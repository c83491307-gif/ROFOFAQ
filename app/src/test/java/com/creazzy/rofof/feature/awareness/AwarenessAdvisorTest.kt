package com.creazzy.rofof.feature.awareness

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.local.TransactionEntity
import com.creazzy.rofof.data.repository.RoomBudgetGoalRepository
import com.creazzy.rofof.data.repository.RoomTransactionRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AwarenessAdvisorTest {
    private lateinit var db: RofofDatabase
    private lateinit var context: Context
    private lateinit var advisor: AwarenessAdvisor

    @Before fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, RofofDatabase::class.java).allowMainThreadQueries().build()
        DatabaseSeeder(db).seedFromAssets(context, 1L)
        RoomTransactionRepository(db.transactionDao(), db.accountDao(), db.categoryDao()).ensureDefaults()
        advisor = AwarenessAdvisor(context, db)
    }

    @After fun close() = db.close()

    @Test fun arabicKeywordResolvesToSeededRoomCategory() = runBlocking {
        val result = advisor.review("قهوة ٥٠", "default-cash", 1_800_000_000_000L, "ar")
        assertEquals("50", result.amountText)
        assertEquals("awareness-expense-coffee", result.categoryId)
        assertEquals("Coffee", result.categoryName)
        assertEquals("قهوة", result.categoryLabel)
        assertTrue(result.categoryKnown)
    }

    @Test fun unknownCategoryIsFlaggedWithoutInventingCategory() = runBlocking {
        val result = advisor.review("مصاريف غير مصنفة 75", "default-cash", 1_800_000_000_000L, "ar")
        assertFalse(result.categoryKnown)
        assertNull(result.categoryId)
        assertTrue(result.ruleMatches.any { it.ruleId == "unknown-category" })
    }

    @Test fun proposedAmountIsIncludedInBudgetThreshold() = runBlocking {
        val id = "awareness-expense-coffee"
        val start = 1_700_000_000_000L
        RoomBudgetGoalRepository(db).createBudget(id, 10_000L, "EGP", 2, start, 1_900_000_000_000L)
        db.transactionDao().insert(TransactionEntity("coffee-old", "Coffee", "EXPENSE", 8_000L, "EGP", 2,
            "default-cash", id, start + 10L, start + 10L, null))
        val result = advisor.review("قهوة ٥", "default-cash", start + 100L, "ar")
        assertTrue(result.ruleMatches.any { it.ruleId == "budget-warning-80" })
    }

    @Test fun analysisNeverWritesFinancialTransactions() = runBlocking {
        val before = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM transactions").use { it.moveToFirst(); it.getInt(0) }
        advisor.review("قهوة ٥٠", "default-cash", 1_800_000_000_000L, "ar")
        val after = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM transactions").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(before, after)
    }
}

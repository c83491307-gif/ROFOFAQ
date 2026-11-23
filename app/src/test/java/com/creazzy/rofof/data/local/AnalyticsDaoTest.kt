package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.creazzy.rofof.data.repository.RoomFinanceRepository
import com.creazzy.rofof.data.repository.RoomTransactionRepository
import com.creazzy.rofof.domain.model.LedgerTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnalyticsDaoTest {
    private lateinit var db: RofofDatabase
    private lateinit var finance: RoomFinanceRepository
    private lateinit var transactions: RoomTransactionRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), RofofDatabase::class.java)
            .allowMainThreadQueries().build()
        finance = RoomFinanceRepository(db)
        transactions = RoomTransactionRepository(db.transactionDao(), db.accountDao(), db.categoryDao())
    }

    @After fun tearDown() = db.close()

    @Test fun categoryReportFiltersIncomeAndOutOfPeriodRows() = runBlocking {
        val account = finance.createAccount("Wallet", "EGP", 2, 0L)
        val food = finance.createCategory("Food", "EXPENSE")
        val salary = finance.createCategory("Salary", "INCOME")
        val start = LocalDate.of(2026, 11, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 12, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        transactions.add(LedgerTransaction("food-1", "Groceries", "EXPENSE", 2500L, "EGP", 2, account, food, start + 10, start + 10, null))
        transactions.add(LedgerTransaction("salary-1", "Salary", "INCOME", 80000L, "EGP", 2, account, salary, start + 20, start + 20, null))
        transactions.add(LedgerTransaction("food-old", "Old groceries", "EXPENSE", 900L, "EGP", 2, account, food, start - 1, start - 1, null))
        val rows = db.transactionDao().observeCategoryExpenses(start, end, "EGP", 2).first()
        assertEquals(1, rows.size)
        assertEquals(food, rows.single().categoryId)
        assertEquals(2500L, rows.single().amountMinor)
    }

    @Test fun searchMatchesTitleAndNotesCaseInsensitively() = runBlocking {
        val account = finance.createAccount("Wallet", "EGP", 2, 0L)
        val food = finance.createCategory("Food", "EXPENSE")
        transactions.add(LedgerTransaction("search-1", "Weekly shopping", "EXPENSE", 1000L, "EGP", 2, account, food, 10L, 10L, "Fresh vegetables"))
        assertEquals("search-1", db.transactionDao().observeSearch("VEGETABLES").first().single().id)
        assertTrue(db.transactionDao().observeSearch("missing").first().isEmpty())
    }
}

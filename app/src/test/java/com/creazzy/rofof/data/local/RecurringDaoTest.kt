package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecurringDaoTest {
    private lateinit var database: RofofDatabase
    private lateinit var dao: RecurringDao

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), RofofDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.recurringDao()
    }

    @After fun tearDown() = database.close()

    @Test fun rulesAndReviewDraftsPersistWithoutPostingTransactions() = runBlocking {
        val rule = RecurringRuleEntity(
            id = "rent-rule", title = "Rent", transactionType = "EXPENSE",
            amountMinor = 25_000L, currencyCode = "EGP", currencyExponent = 2,
            accountId = "default-cash", categoryId = "default-expense-general",
            intervalType = "MONTHLY", intervalCount = 1, nextRunEpochMillis = 100L,
            endEpochMillis = null, isActive = true, createdAtEpochMillis = 1L
        )
        dao.insertRule(rule)
        val draft = RecurringDraftEntity(
            id = "rent-draft-1", ruleId = rule.id, scheduledForEpochMillis = 100L,
            title = rule.title, transactionType = rule.transactionType, amountMinor = rule.amountMinor,
            currencyCode = rule.currencyCode, currencyExponent = rule.currencyExponent,
            accountId = rule.accountId, categoryId = rule.categoryId, status = "DRAFT",
            createdAtEpochMillis = 200L
        )
        assertTrue(dao.insertDraftIfAbsent(draft) > 0L)
        assertEquals(-1L, dao.insertDraftIfAbsent(draft.copy(id = "duplicate-draft")))
        assertEquals("DRAFT", dao.observeDrafts().first().single().status)
        assertTrue(database.transactionDao().observeRecent().first().isEmpty())
    }

    @Test fun draftStatusCanOnlyTransitionFromDraft() = runBlocking {
        val rule = RecurringRuleEntity(
            "utility-rule", "Utilities", "EXPENSE", 5_000L, "EGP", 2,
            "default-cash", "default-expense-general", "MONTHLY", 1,
            1_000L, null, true, 1L
        )
        dao.insertRule(rule)
        val draft = RecurringDraftEntity(
            "utility-draft", rule.id, 1_000L, rule.title, rule.transactionType,
            rule.amountMinor, rule.currencyCode, rule.currencyExponent, rule.accountId,
            rule.categoryId, "DRAFT", 1_000L
        )
        dao.insertDraftIfAbsent(draft)
        assertEquals(1, dao.updateDraftStatus(draft.id, "SKIPPED"))
        assertEquals(0, dao.updateDraftStatus(draft.id, "POSTED"))
        assertEquals("SKIPPED", dao.observeDrafts().first().single().status)
    }
}

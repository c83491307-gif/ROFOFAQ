package com.creazzy.rofof.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.creazzy.rofof.data.local.RecurringDraftEntity
import com.creazzy.rofof.data.local.RecurringRuleEntity
import com.creazzy.rofof.data.local.RofofDatabase
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
class RecurringDraftApprovalRepositoryTest {
    private lateinit var database: RofofDatabase
    private lateinit var finance: RoomFinanceRepository
    private lateinit var approvals: RecurringDraftApprovalRepository
    private lateinit var accountId: String
    private lateinit var categoryId: String

    @Before fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), RofofDatabase::class.java
        ).allowMainThreadQueries().build()
        finance = RoomFinanceRepository(database)
        approvals = RecurringDraftApprovalRepository(database)
        accountId = finance.createAccount("Wallet", "EGP", 2, 0L)
        categoryId = finance.createCategory("Food", "EXPENSE")
        insertPendingDraft("draft-1")
    }

    @After fun tearDown() { database.close() }

    private suspend fun insertPendingDraft(id: String) {
        val ruleId = "rule-$id"
        database.recurringDao().insertRule(
            RecurringRuleEntity(ruleId, "Groceries", "EXPENSE", 1250L, "EGP", 2,
                accountId, categoryId, "MONTHLY", 1, 100L, null, true, 1L)
        )
        database.recurringDao().insertDraftIfAbsent(
            RecurringDraftEntity(id, ruleId, 100L, "Groceries", "EXPENSE", 1250L,
                "EGP", 2, accountId, categoryId, "DRAFT", 200L)
        )
    }

    @Test fun approvalAtomicallyPostsLedgerTransactionAndChangesDraftStatus() = runBlocking {
        val id = approvals.approve("draft-1", 300L)
        val transaction = database.transactionDao().observeRecent().first().single()
        assertEquals("recurring-draft:draft-1", id)
        assertEquals(id, transaction.id)
        assertEquals(1250L, transaction.amountMinor)
        assertEquals(100L, transaction.occurredAtEpochMillis)
        assertEquals("POSTED", database.recurringDao().findDraftById("draft-1")?.status)
    }

    @Test fun approvalIsRejectedAfterFirstPost() = runBlocking {
        approvals.approve("draft-1", 300L)
        assertTrue(runCatching { approvals.approve("draft-1", 400L) }.isFailure)
        assertEquals(1, database.transactionDao().observeRecent().first().size)
    }

    @Test fun archivedAccountPreventsPostingAndLeavesDraftPending() = runBlocking {
        finance.archiveAccount(accountId)
        assertTrue(runCatching { approvals.approve("draft-1", 300L) }.isFailure)
        assertTrue(database.transactionDao().observeRecent().first().isEmpty())
        assertEquals("DRAFT", database.recurringDao().findDraftById("draft-1")?.status)
    }

    @Test fun skippedDraftCannotBeApproved() = runBlocking {
        assertTrue(approvals.skip("draft-1"))
        assertTrue(runCatching { approvals.approve("draft-1", 300L) }.isFailure)
        assertTrue(database.transactionDao().observeRecent().first().isEmpty())
        assertEquals("SKIPPED", database.recurringDao().findDraftById("draft-1")?.status)
    }
}

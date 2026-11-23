package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.creazzy.rofof.data.repository.RoomPurchaseRepository
import com.creazzy.rofof.data.repository.RoomTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PurchaseDaoTest {
    private lateinit var database: RofofDatabase
    private lateinit var purchases: RoomPurchaseRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), RofofDatabase::class.java
        ).allowMainThreadQueries().build()
        purchases = RoomPurchaseRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test fun itemAllocationsAndPaymentsProduceParticipantBalances() = runBlocking {
        val session = purchases.createSession("Household groceries", "EGP", 2)
        val a = purchases.addParticipant(session, "A")
        val b = purchases.addParticipant(session, "B")
        val item = purchases.addItem(session, "Rice", 1_000L, 10_000L)
        purchases.allocateItem(item, mapOf(a to 6_000L, b to 4_000L))
        purchases.recordPayment(session, a, 5_000L, "CASH", "payment-a-1")

        val rows = purchases.observeSettlements(session).first().associateBy { it.participantId }
        assertEquals(6_000L, rows[a]?.allocatedMinor)
        assertEquals(5_000L, rows[a]?.paidMinor)
        assertEquals(1_000L, rows[a]?.balanceMinor)
        assertEquals(4_000L, rows[b]?.balanceMinor)
    }

    @Test fun duplicatePaymentIdempotencyKeyIsRejected() = runBlocking {
        val session = purchases.createSession("Shared bill", "EGP", 2)
        val participant = purchases.addParticipant(session, "A")
        purchases.recordPayment(session, participant, 100L, "CASH", "same-key")
        val duplicate = runCatching {
            purchases.recordPayment(session, participant, 100L, "CASH", "same-key")
        }
        assertTrue(duplicate.isFailure)
        assertEquals(1, database.purchaseDao().settlementSnapshot(session).size)
        assertEquals(100L, database.purchaseDao().settlementSnapshot(session).single().paidMinor)
    }

    @Test fun allocationMustBeExactAndBelongToSamePurchase() = runBlocking {
        val session = purchases.createSession("Shared purchase", "EGP", 2)
        val other = purchases.createSession("Other purchase", "EGP", 2)
        val participant = purchases.addParticipant(session, "A")
        val outsider = purchases.addParticipant(other, "B")
        val item = purchases.addItem(session, "Item", 1_000L, 500L)

        assertTrue(runCatching { purchases.allocateItem(item, mapOf(participant to 499L)) }.isFailure)
        assertTrue(runCatching { purchases.allocateItem(item, mapOf(outsider to 500L)) }.isFailure)
        purchases.allocateItem(item, mapOf(participant to 500L))
        assertEquals(500L, database.purchaseDao().settlementSnapshot(session).single().allocatedMinor)
    }

    @Test fun explicitAllocationConversionCreatesOneLinkedPersonalExpense() = runBlocking {
        val transactions = RoomTransactionRepository(
            database.transactionDao(), database.accountDao(), database.categoryDao()
        )
        transactions.ensureDefaults()
        val finance = com.creazzy.rofof.data.repository.RoomFinanceRepository(database)
        val accountId = finance.createAccount("Wallet", "EGP", 2, 0L)
        val categoryId = finance.createCategory("Shared purchases", "EXPENSE")
        val session = purchases.createSession("Electricity", "EGP", 2)
        val participant = purchases.addParticipant(session, "Me")
        val item = purchases.addItem(session, "Electricity bill", 1_000L, 1_250L)
        purchases.allocateItem(item, mapOf(participant to 1_250L))
        val allocation = database.purchaseDao().allocationsForItem(item).single()

        val transactionId = purchases.recordAllocationAsPersonalExpense(allocation.id, accountId, categoryId)
        assertEquals(1, database.transactionDao().observeRecent().first().size)
        assertEquals(transactionId, database.purchaseDao().findLinkByTransaction(transactionId)?.transactionId)
        assertTrue(runCatching {
            purchases.recordAllocationAsPersonalExpense(allocation.id, accountId, categoryId)
        }.isFailure)
    }

    @Test fun closingRequiresAllItemsAllocatedAndAllPositiveBalancesPaid() = runBlocking {
        val session = purchases.createSession("Trip", "EGP", 2)
        val participant = purchases.addParticipant(session, "A")
        val item = purchases.addItem(session, "Ticket", 1_000L, 1_000L)
        assertTrue(runCatching { purchases.closeAsSettled(session) }.isFailure)
        purchases.allocateItem(item, mapOf(participant to 1_000L))
        assertTrue(runCatching { purchases.closeAsSettled(session) }.isFailure)
        purchases.recordPayment(session, participant, 1_000L, "CASH", "ticket-paid")
        purchases.closeAsSettled(session)
        assertEquals(RoomPurchaseRepository.SETTLED, database.purchaseDao().findSession(session)?.status)
    }
}

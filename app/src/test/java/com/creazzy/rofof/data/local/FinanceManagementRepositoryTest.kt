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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FinanceManagementRepositoryTest {
    private lateinit var database: RofofDatabase
    private lateinit var finance: RoomFinanceRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            RofofDatabase::class.java
        ).allowMainThreadQueries().build()
        finance = RoomFinanceRepository(database)
    }

    @After fun tearDown() { database.close() }

    @Test fun accountRenameAndArchiveArePersistedAndArchivedAccountsRemainDiscoverable() = runBlocking {
        val id = finance.createAccount("Old wallet", "EGP", 2, 12500L)
        finance.renameAccount(id, "Travel wallet")
        assertEquals("Travel wallet", database.accountDao().findById(id)?.name)

        finance.archiveAccount(id)
        assertTrue(database.accountDao().findById(id)?.isArchived == true)
        assertFalse(finance.observeAccounts().first().any { it.id == id })
        val archived = finance.observeAccountsForManagement().first().single { it.id == id }
        assertTrue(archived.isArchived)
        assertEquals(12500L, archived.balanceMinor)
    }

    @Test fun defaultAndAccountsWithLedgerOrTransferActivityCannotBeArchived() = runBlocking {
        assertTrue(runCatching { finance.archiveAccount("default-cash") }.isFailure)

        val accountId = finance.createAccount("Used wallet", "EGP", 2, 5000L)
        val categoryId = finance.createCategory("Food", "EXPENSE")
        RoomTransactionRepository(database.transactionDao(), database.accountDao(), database.categoryDao()).add(
            LedgerTransaction("tx-1", "Lunch", "EXPENSE", 250L, "EGP", 2, accountId, categoryId, 1L, 1L, null)
        )
        assertTrue(runCatching { finance.archiveAccount(accountId) }.isFailure)
        assertFalse(database.accountDao().findById(accountId)!!.isArchived)
    }

    @Test fun categoryRenameAndArchivePreserveArchivedRecordForManagement() = runBlocking {
        val id = finance.createCategory("Dining", "EXPENSE")
        finance.renameCategory(id, "Restaurants")
        assertEquals("Restaurants", database.categoryDao().findById(id)?.name)

        finance.archiveCategory(id)
        assertTrue(database.categoryDao().findById(id)?.isArchived == true)
        assertFalse(finance.observeCategories().first().any { it.id == id })
        assertTrue(finance.observeCategoriesForManagement().first().any { it.id == id && it.isArchived })
    }

    @Test fun parentCategoryCannotBeArchivedBeforeItsActiveChild() = runBlocking {
        val parent = finance.createCategory("Home", "EXPENSE")
        val child = finance.createCategory("Utilities", "EXPENSE", parent)
        assertTrue(runCatching { finance.archiveCategory(parent) }.isFailure)
        assertFalse(database.categoryDao().findById(parent)!!.isArchived)

        finance.archiveCategory(child)
        finance.archiveCategory(parent)
        assertTrue(database.categoryDao().findById(parent)!!.isArchived)
        assertTrue(database.categoryDao().findById(child)!!.isArchived)
    }

    @Test fun categoryWithTransactionsCannotBeArchived() = runBlocking {
        val accountId = finance.createAccount("Cash", "EGP", 2, 0L)
        val categoryId = finance.createCategory("Transport", "EXPENSE")
        RoomTransactionRepository(database.transactionDao(), database.accountDao(), database.categoryDao()).add(
            LedgerTransaction("tx-transport", "Taxi", "EXPENSE", 900L, "EGP", 2, accountId, categoryId, 2L, 2L, null)
        )
        assertTrue(runCatching { finance.archiveCategory(categoryId) }.isFailure)
        assertFalse(database.categoryDao().findById(categoryId)!!.isArchived)
    }
}

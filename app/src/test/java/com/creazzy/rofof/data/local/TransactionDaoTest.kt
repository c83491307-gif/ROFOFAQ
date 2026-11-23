package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TransactionDaoTest {
    private lateinit var database: RofofDatabase
    @Before fun createDatabase() { database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), RofofDatabase::class.java).allowMainThreadQueries().build() }
    @After fun closeDatabase() { database.close() }

    @Test fun incomeAndExpenseArePersistedInChronologicalOrder() = runBlocking {
        val dao = database.transactionDao()
        dao.insert(TransactionEntity("expense", "Groceries", "EXPENSE", 12500, "EGP", 2, "cash", "food", 100L, 100L, null))
        dao.insert(TransactionEntity("income", "Salary", "INCOME", 100000, "EGP", 2, "cash", "salary", 200L, 200L, null))
        val records = dao.observeRecent().first()
        assertEquals(listOf("income", "expense"), records.map { it.id })
        assertEquals("INCOME", records.first().transactionType)
    }

    @Test fun totalsAreScopedToCurrencyAndType() = runBlocking {
        val dao = database.transactionDao()
        dao.insert(TransactionEntity("e1", "Food", "EXPENSE", 5000, "EGP", 2, "cash", "food", 1L, 1L, null))
        dao.insert(TransactionEntity("e2", "Fuel", "EXPENSE", 2500, "USD", 2, "card", "fuel", 2L, 2L, null))
        dao.insert(TransactionEntity("i1", "Pay", "INCOME", 20000, "EGP", 2, "cash", "salary", 3L, 3L, null))
        assertEquals(5000L, dao.observeExpensesSince(0L, "EGP").first())
        assertEquals(20000L, dao.observeIncomeSince(0L, "EGP").first())
        assertEquals(2500L, dao.observeExpensesSince(0L, "USD").first())
    }

    @Test fun repositorySeedsOneLocalCashAccountAndTypedGeneralCategories() = runBlocking {
        val repository = com.creazzy.rofof.data.repository.RoomTransactionRepository(database.transactionDao(), database.accountDao(), database.categoryDao())
        repository.ensureDefaults()
        assertEquals("Cash", database.accountDao().findById("default-cash")?.name)
        assertEquals("EXPENSE", database.categoryDao().findById("default-expense-general")?.transactionType)
        assertEquals("INCOME", database.categoryDao().findById("default-income-general")?.transactionType)
    }

    @Test fun transferChangesBothAccountBalancesWithoutCreatingIncomeOrExpense() = runBlocking {
        val finance = com.creazzy.rofof.data.repository.RoomFinanceRepository(database)
        val sourceId = finance.createAccount("Wallet A", "EGP", 2, 10000L)
        val destinationId = finance.createAccount("Wallet B", "EGP", 2, 500L)
        finance.transfer(sourceId, destinationId, 2500L, "Cash movement")
        val balances = finance.observeAccounts().first().associateBy { it.id }
        assertEquals(7500L, balances[sourceId]?.balanceMinor)
        assertEquals(3000L, balances[destinationId]?.balanceMinor)
        assertEquals(0L, database.transactionDao().observeIncomeSince(0L, "EGP").first())
        assertEquals(0L, database.transactionDao().observeExpensesSince(0L, "EGP").first())
        assertEquals(1, finance.observeTransfers().first().size)
    }

    @Test fun transferRejectsDifferentCurrenciesAndLeavesBalancesUntouched() = runBlocking {
        val finance = com.creazzy.rofof.data.repository.RoomFinanceRepository(database)
        val egp = finance.createAccount("EGP wallet", "EGP", 2, 5000L)
        val usd = finance.createAccount("USD wallet", "USD", 2, 5000L)
        val failure = runCatching { finance.transfer(egp, usd, 100L) }
        assertEquals(true, failure.isFailure)
        val balances = finance.observeAccounts().first().associateBy { it.id }
        assertEquals(5000L, balances[egp]?.balanceMinor)
        assertEquals(5000L, balances[usd]?.balanceMinor)
        assertEquals(0, finance.observeTransfers().first().size)
    }

    @Test fun categoryHierarchyIsLimitedToTwoLevelsAndMatchingTypes() = runBlocking {
        val finance = com.creazzy.rofof.data.repository.RoomFinanceRepository(database)
        val parent = finance.createCategory("Food", "EXPENSE")
        finance.createCategory("Groceries", "EXPENSE", parent)
        assertEquals(true, runCatching { finance.createCategory("Salary child", "INCOME", parent) }.isFailure)
        val child = database.categoryDao().findByNameAndType("Groceries", "EXPENSE")!!
        assertEquals(true, runCatching { finance.createCategory("Fresh food", "EXPENSE", child.id) }.isFailure)
    }

    @Test fun accountWithActivityCannotBeArchived() = runBlocking {
        val finance = com.creazzy.rofof.data.repository.RoomFinanceRepository(database)
        val accountId = finance.createAccount("Daily cash", "EGP", 2, 1000L)
        database.transactionDao().insert(TransactionEntity("tx-archive", "Lunch", "EXPENSE", 100L, "EGP", 2, accountId, "food", 1L, 1L, null))
        assertEquals(true, runCatching { finance.archiveAccount(accountId) }.isFailure)
        assertEquals(false, database.accountDao().findById(accountId)?.isArchived)
    }

    @Test fun dashboardTotalsIncludeFullHistoryAndDoNotMixCurrencies() = runBlocking {
        val dao = database.transactionDao()
        dao.insert(TransactionEntity("egp-i", "Salary", "INCOME", 10000L, "EGP", 2, "cash", "salary", 1L, 1L, null))
        dao.insert(TransactionEntity("egp-e", "Rent", "EXPENSE", 3000L, "EGP", 2, "cash", "rent", 2L, 2L, null))
        dao.insert(TransactionEntity("usd-i", "USD income", "INCOME", 90000L, "USD", 2, "usd", "salary", 3L, 3L, null))
        val totals = dao.observeTotals("EGP", 2).first()
        assertEquals(10000L, totals.incomeMinor)
        assertEquals(3000L, totals.expenseMinor)
    }

    @Test fun transactionRepositoryRejectsCurrencyOrCategoryMismatch() = runBlocking {
        val finance = com.creazzy.rofof.data.repository.RoomFinanceRepository(database)
        val accountId = finance.createAccount("Cash", "EGP", 2, 0L)
        val repository = com.creazzy.rofof.data.repository.RoomTransactionRepository(database.transactionDao(), database.accountDao(), database.categoryDao())
        val incomeCategoryId = finance.createCategory("Salary", "INCOME")
        val expenseCategoryId = finance.createCategory("General", "EXPENSE")
        val wrongCurrency = runCatching { repository.add(com.creazzy.rofof.domain.model.LedgerTransaction("bad-currency", "Wrong", "EXPENSE", 100L, "USD", 2, accountId, expenseCategoryId, 1L, 1L, null)) }
        assertEquals(true, wrongCurrency.isFailure)
        val wrongCategory = runCatching { repository.add(com.creazzy.rofof.domain.model.LedgerTransaction("bad-category", "Wrong", "EXPENSE", 100L, "EGP", 2, accountId, incomeCategoryId, 1L, 1L, null)) }
        assertEquals(true, wrongCategory.isFailure)
    }

    @Test fun deletingOneTransactionDoesNotDeleteOthers() = runBlocking {
        val dao = database.transactionDao()
        dao.insert(TransactionEntity("one", "A", "EXPENSE", 100, "EGP", 2, "cash", "general", 1L, 1L, null))
        dao.insert(TransactionEntity("two", "B", "INCOME", 200, "EGP", 2, "cash", "general", 2L, 2L, null))
        dao.delete("one")
        assertEquals(listOf("two"), dao.observeRecent().first().map { it.id })
    }
}

package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.creazzy.rofof.data.repository.RoomBudgetGoalRepository
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
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BudgetGoalRepositoryTest {
    private lateinit var database: RofofDatabase
    private lateinit var finance: RoomFinanceRepository
    private lateinit var plans: RoomBudgetGoalRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), RofofDatabase::class.java
        ).allowMainThreadQueries().build()
        finance = RoomFinanceRepository(database)
        plans = RoomBudgetGoalRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test fun monthlyBudgetTracksOnlyMatchingCategoryCurrencyAndPeriodExpenses() = runBlocking {
        val account = finance.createAccount("Main", "EGP", 2, 0L)
        val food = finance.createCategory("Food", "EXPENSE")
        val travel = finance.createCategory("Travel", "EXPENSE")
        val income = finance.createCategory("Salary", "INCOME")
        val start = LocalDate.of(2026, 11, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 12, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val transactions = RoomTransactionRepository(database.transactionDao(), database.accountDao(), database.categoryDao())
        transactions.add(LedgerTransaction("food-in-period", "Groceries", "EXPENSE", 2500L, "EGP", 2, account, food, start + 1000L, start + 1000L, null))
        transactions.add(LedgerTransaction("travel-in-period", "Taxi", "EXPENSE", 900L, "EGP", 2, account, travel, start + 2000L, start + 2000L, null))
        transactions.add(LedgerTransaction("food-before", "Old groceries", "EXPENSE", 700L, "EGP", 2, account, food, start - 1000L, start - 1000L, null))
        transactions.add(LedgerTransaction("food-income", "Refund", "INCOME", 500L, "EGP", 2, account, income, start + 3000L, start + 3000L, null))

        plans.createBudget(food, 5000L, "EGP", 2, start, end)
        val row = plans.observeBudgets(start).first().single()
        assertEquals(2500L, row.spentMinor)
        assertEquals(5000L, row.amountMinor)
    }

    @Test fun duplicateBudgetForCategoryCurrencyAndMonthIsRejected() = runBlocking {
        val category = finance.createCategory("Food", "EXPENSE")
        val start = 1_700_000_000_000L
        val end = start + 30L * 24 * 60 * 60 * 1000
        plans.createBudget(category, 10_000L, "EGP", 2, start, end)
        assertTrue(runCatching {
            plans.createBudget(category, 12_000L, "EGP", 2, start, end)
        }.isFailure)
    }

    @Test fun budgetRejectsIncomeOrArchivedCategoriesAndNonPositiveLimits() = runBlocking {
        val income = finance.createCategory("Salary", "INCOME")
        assertTrue(runCatching { plans.createBudget(income, 1000L, "EGP", 2, 1L, 2L) }.isFailure)
        val expense = finance.createCategory("Bills", "EXPENSE")
        assertTrue(runCatching { plans.createBudget(expense, 0L, "EGP", 2, 1L, 2L) }.isFailure)
        finance.archiveCategory(expense)
        assertTrue(runCatching { plans.createBudget(expense, 1000L, "EGP", 2, 1L, 2L) }.isFailure)
    }

    @Test fun financialGoalProgressCanBeUpdatedAndArchivedWithoutLosingRecord() = runBlocking {
        val id = plans.createGoal("Emergency fund", 100_000L, "EGP", 2, null)
        plans.updateGoalProgress(id, 25_000L)
        assertEquals(25_000L, database.budgetGoalDao().findGoal(id)?.currentAmountMinor)
        assertEquals(1, plans.observeGoals().first().size)
        plans.archiveGoal(id)
        assertTrue(plans.observeGoals().first().isEmpty())
        assertTrue(database.budgetGoalDao().findGoal(id)?.isArchived == true)
    }

    @Test fun goalsRejectInvalidTargetsAndNegativeProgress() = runBlocking {
        assertTrue(runCatching { plans.createGoal(" ", 100L, "EGP", 2, null) }.isFailure)
        assertTrue(runCatching { plans.createGoal("Trip", 0L, "EGP", 2, null) }.isFailure)
        val id = plans.createGoal("Trip", 10_000L, "EGP", 2, null)
        assertTrue(runCatching { plans.updateGoalProgress(id, -1L) }.isFailure)
    }

    @Test fun schemaVersionSevenCreatesBudgetGoalAndRecurringTables() = runBlocking {
        val tables = database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('budgets', 'financial_goals', 'recurring_drafts', 'recurring_rules') ORDER BY name"
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        assertEquals(listOf("budgets", "financial_goals", "recurring_drafts", "recurring_rules"), tables)
        val version = database.openHelper.readableDatabase.query("PRAGMA user_version").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        assertEquals(7, version)
    }
    @Test fun budgetAlertSnapshotUsesOnlyMatchingCategoryCurrencyAndPeriod() = runBlocking {
        val account = finance.createAccount("Main", "EGP", 2, 0L)
        val food = finance.createCategory("Food", "EXPENSE")
        val travel = finance.createCategory("Travel", "EXPENSE")
        val start = LocalDate.of(2026, 11, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 12, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        plans.createBudget(food, 10_000L, "EGP", 2, start, end)
        database.transactionDao().insert(
            TransactionEntity("alert-food", "Groceries", "EXPENSE", 10_001L, "EGP", 2, account, food, start + 1_000L, start + 1_000L, null)
        )
        database.transactionDao().insert(
            TransactionEntity("alert-travel", "Taxi", "EXPENSE", 90_000L, "EGP", 2, account, travel, start + 2_000L, start + 2_000L, null)
        )
        database.transactionDao().insert(
            TransactionEntity("alert-old", "Old groceries", "EXPENSE", 70_000L, "EGP", 2, account, food, start - 1L, start - 1L, null)
        )

        val matching = database.budgetGoalDao().findBudgetAlertsForExpense(food, "EGP", 2, start + 3_000L)
        assertEquals(1, matching.size)
        assertEquals(10_001L, matching.single().spentMinor)
        assertTrue(database.budgetGoalDao().findBudgetAlertsForExpense(food, "EGP", 2, end).isEmpty())
        assertTrue(database.budgetGoalDao().findBudgetAlertsForExpense(food, "USD", 2, start + 3_000L).isEmpty())
    }

}

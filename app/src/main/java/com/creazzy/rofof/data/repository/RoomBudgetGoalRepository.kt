package com.creazzy.rofof.data.repository

import androidx.room.withTransaction
import com.creazzy.rofof.data.local.BudgetEntity
import com.creazzy.rofof.data.local.BudgetGoalDao
import com.creazzy.rofof.data.local.FinancialGoalEntity
import com.creazzy.rofof.data.local.RofofDatabase
import kotlinx.coroutines.flow.Flow
import java.util.Currency
import java.util.UUID

class RoomBudgetGoalRepository(
    private val database: RofofDatabase,
    private val dao: BudgetGoalDao = database.budgetGoalDao()
) {
    fun observeBudgets(periodStartEpochMillis: Long): Flow<List<com.creazzy.rofof.data.local.BudgetProgress>> =
        dao.observeBudgetsForPeriod(periodStartEpochMillis)

    fun observeGoals(): Flow<List<FinancialGoalEntity>> = dao.observeActiveGoals()

    suspend fun createBudget(
        categoryId: String,
        amountMinor: Long,
        currencyCode: String,
        currencyExponent: Int,
        periodStartEpochMillis: Long,
        periodEndEpochMillis: Long
    ): String = database.withTransaction {
        require(amountMinor > 0L) { "Budget amount must be positive" }
        require(periodEndEpochMillis > periodStartEpochMillis) { "Budget period is invalid" }
        validateCurrency(currencyCode, currencyExponent)
        val category = database.categoryDao().findById(categoryId) ?: error("Category not found")
        require(!category.isArchived && category.transactionType == "EXPENSE") {
            "Budgets require an active expense category"
        }
        val id = UUID.randomUUID().toString()
        dao.insertBudget(
            BudgetEntity(
                id = id,
                categoryId = categoryId,
                amountMinor = amountMinor,
                currencyCode = currencyCode,
                currencyExponent = currencyExponent,
                periodStartEpochMillis = periodStartEpochMillis,
                periodEndEpochMillis = periodEndEpochMillis,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
        id
    }

    suspend fun updateBudget(id: String, amountMinor: Long) = database.withTransaction {
        require(amountMinor > 0L) { "Budget amount must be positive" }
        val current = dao.findBudget(id) ?: error("Budget not found")
        require(dao.updateBudget(current.copy(amountMinor = amountMinor)) == 1) { "Budget could not be updated" }
    }

    suspend fun deleteBudget(id: String) = database.withTransaction {
        require(dao.deleteBudget(id) == 1) { "Budget not found" }
    }

    suspend fun createGoal(
        title: String,
        targetAmountMinor: Long,
        currencyCode: String,
        currencyExponent: Int,
        deadlineEpochMillis: Long?
    ): String {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Goal title is required" }
        require(targetAmountMinor > 0L) { "Goal target must be positive" }
        validateCurrency(currencyCode, currencyExponent)
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        dao.insertGoal(
            FinancialGoalEntity(
                id = id,
                title = cleanTitle,
                targetAmountMinor = targetAmountMinor,
                currentAmountMinor = 0L,
                currencyCode = currencyCode,
                currencyExponent = currencyExponent,
                deadlineEpochMillis = deadlineEpochMillis,
                isArchived = false,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        )
        return id
    }

    suspend fun updateGoalProgress(id: String, currentAmountMinor: Long) = database.withTransaction {
        require(currentAmountMinor >= 0L) { "Saved amount cannot be negative" }
        val goal = dao.findGoal(id) ?: error("Goal not found")
        require(!goal.isArchived) { "Archived goals cannot be changed" }
        require(dao.updateGoal(goal.copy(
            currentAmountMinor = currentAmountMinor,
            updatedAtEpochMillis = System.currentTimeMillis()
        )) == 1) { "Goal could not be updated" }
    }

    suspend fun archiveGoal(id: String) = database.withTransaction {
        require(dao.archiveGoal(id, System.currentTimeMillis()) == 1) { "Active goal not found" }
    }

    private fun validateCurrency(code: String, exponent: Int) {
        val clean = code.trim().uppercase()
        require(clean.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter ISO code" }
        val currency = runCatching { Currency.getInstance(clean) }.getOrNull()
            ?: error("Unknown ISO currency code")
        require(currency.defaultFractionDigits == exponent) { "Currency exponent does not match ISO currency metadata" }
    }
}

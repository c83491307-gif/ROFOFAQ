package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetGoalDao {
    @Query("""
        SELECT b.id, b.categoryId, b.amountMinor, b.currencyCode, b.currencyExponent,
               b.periodStartEpochMillis, b.periodEndEpochMillis, b.createdAtEpochMillis,
               COALESCE((
                   SELECT SUM(t.amountMinor) FROM transactions t
                   WHERE t.categoryId = b.categoryId
                     AND t.transactionType = 'EXPENSE'
                     AND t.currencyCode = b.currencyCode
                     AND t.currencyExponent = b.currencyExponent
                     AND t.occurredAtEpochMillis >= b.periodStartEpochMillis
                     AND t.occurredAtEpochMillis < b.periodEndEpochMillis
               ), 0) AS spentMinor
        FROM budgets b
        WHERE b.periodStartEpochMillis = :periodStartEpochMillis
        ORDER BY b.createdAtEpochMillis DESC
    """)
    fun observeBudgetsForPeriod(periodStartEpochMillis: Long): Flow<List<BudgetProgress>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun findBudget(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudget(value: BudgetEntity)

    @Update
    suspend fun updateBudget(value: BudgetEntity): Int

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: String): Int

    @Query("SELECT * FROM financial_goals WHERE isArchived = 0 ORDER BY createdAtEpochMillis DESC")
    fun observeActiveGoals(): Flow<List<FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals WHERE id = :id LIMIT 1")
    suspend fun findGoal(id: String): FinancialGoalEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoal(value: FinancialGoalEntity)

    @Update
    suspend fun updateGoal(value: FinancialGoalEntity): Int

    @Query("UPDATE financial_goals SET isArchived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id AND isArchived = 0")
    suspend fun archiveGoal(id: String, updatedAt: Long): Int
}

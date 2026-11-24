package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Transaction
    @Query("""
        SELECT debts.*,
            COALESCE((SELECT SUM(amountMinor) FROM debt_repayments WHERE debtId = debts.id), 0) AS repaidMinor,
            debts.principalMinor - COALESCE((SELECT SUM(amountMinor) FROM debt_repayments WHERE debtId = debts.id), 0) AS outstandingMinor
        FROM debts
        ORDER BY CASE WHEN debts.status = 'OPEN' THEN 0 ELSE 1 END,
                 debts.dueAtEpochMillis, debts.createdAtEpochMillis DESC
    """)
    fun observeBalances(): Flow<List<DebtBalance>>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DebtEntity?

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM debt_repayments WHERE debtId = :debtId")
    suspend fun sumRepayments(debtId: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepayment(repayment: DebtRepaymentEntity)

    @Query("UPDATE debts SET status = :status, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long): Int

    @Query("SELECT COUNT(*) FROM debt_repayments WHERE debtId = :debtId")
    suspend fun countRepayments(debtId: String): Int
}

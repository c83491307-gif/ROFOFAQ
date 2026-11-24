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

    @Query("""
        SELECT debts.*,
            COALESCE((SELECT SUM(amountMinor) FROM debt_repayments WHERE debtId = debts.id), 0) AS repaidMinor,
            debts.principalMinor - COALESCE((SELECT SUM(amountMinor) FROM debt_repayments WHERE debtId = debts.id), 0) AS outstandingMinor
        FROM debts WHERE debts.id = :id LIMIT 1
    """)
    suspend fun getBalance(id: String): DebtBalance?

    @Transaction
    suspend fun recordRepaymentWithLedger(
        repayment: DebtRepaymentEntity,
        transaction: TransactionEntity,
        transactionDao: TransactionDao
    ): DebtBalance {
        require(repayment.amountMinor > 0L) { "Repayment must be positive" }
        val debt = requireNotNull(findById(repayment.debtId)) { "Debt not found" }
        require(debt.status == "OPEN") { "Debt is not open" }
        val expectedType = if (debt.direction == "OWED_TO_ME") "INCOME" else "EXPENSE"
        require(transaction.transactionType == expectedType) { "Repayment ledger direction mismatch" }
        require(transaction.amountMinor == repayment.amountMinor) { "Repayment amount mismatch" }
        require(transaction.currencyCode == debt.currencyCode &&
            transaction.currencyExponent == debt.currencyExponent) { "Repayment currency mismatch" }
        val outstanding = Math.subtractExact(debt.principalMinor, sumRepayments(debt.id))
        require(outstanding > 0L && repayment.amountMinor <= outstanding) {
            "Repayment exceeds outstanding balance"
        }
        insertRepayment(repayment)
        transactionDao.insert(transaction)
        if (repayment.amountMinor == outstanding) {
            updateStatus(debt.id, "CLOSED", repayment.createdAtEpochMillis)
        }
        return requireNotNull(getBalance(debt.id))
    }

    @Transaction
    suspend fun recordRepayment(repayment: DebtRepaymentEntity): DebtBalance {
        require(repayment.amountMinor > 0L) { "Repayment must be positive" }
        val debt = requireNotNull(findById(repayment.debtId)) { "Debt not found" }
        require(debt.status == "OPEN") { "Debt is not open" }
        val repaid = sumRepayments(debt.id)
        val outstanding = Math.subtractExact(debt.principalMinor, repaid)
        require(outstanding > 0L) { "Debt is already settled" }
        require(repayment.amountMinor <= outstanding) { "Repayment exceeds outstanding balance" }
        insertRepayment(repayment)
        val newOutstanding = Math.subtractExact(outstanding, repayment.amountMinor)
        if (newOutstanding == 0L) updateStatus(debt.id, "CLOSED", repayment.createdAtEpochMillis)
        return requireNotNull(getBalance(debt.id))
    }
}

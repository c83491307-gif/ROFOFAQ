package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("""
        SELECT currencyCode, currencyExponent,
            COALESCE(SUM(CASE WHEN transactionType = 'INCOME' THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
            COALESCE(SUM(CASE WHEN transactionType = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor,
            COUNT(*) AS transactionCount
        FROM transactions
        GROUP BY currencyCode, currencyExponent
        ORDER BY currencyCode, currencyExponent
    """)
    fun observeTotalsByCurrency(): Flow<List<CurrencyTransactionTotals>>

    @Query("""
        SELECT strftime('%Y-%m', occurredAtEpochMillis / 1000, 'unixepoch') AS month,
            currencyCode, currencyExponent,
            COALESCE(SUM(CASE WHEN transactionType = 'INCOME' THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
            COALESCE(SUM(CASE WHEN transactionType = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor
        FROM transactions
        WHERE occurredAtEpochMillis >= :fromEpochMillis AND occurredAtEpochMillis < :toEpochMillis
        GROUP BY month, currencyCode, currencyExponent
        ORDER BY month, currencyCode, currencyExponent
    """)
    fun observeMonthlyCashFlow(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<MonthlyCashFlow>>

    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC LIMIT 250")
    fun observeRecent(): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE transactionType = 'INCOME' AND currencyCode = :currencyCode AND occurredAtEpochMillis >= :fromEpochMillis")
    fun observeIncomeSince(fromEpochMillis: Long, currencyCode: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE transactionType = 'EXPENSE' AND currencyCode = :currencyCode AND occurredAtEpochMillis >= :fromEpochMillis")
    fun observeExpensesSince(fromEpochMillis: Long, currencyCode: String): Flow<Long>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN transactionType = 'INCOME' THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
            COALESCE(SUM(CASE WHEN transactionType = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor
        FROM transactions
        WHERE currencyCode = :currencyCode AND currencyExponent = :currencyExponent
    """)
    fun observeTotals(currencyCode: String, currencyExponent: Int): Flow<TransactionTotals>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) > 0 FROM transactions WHERE accountId = :accountId")
    suspend fun hasTransactionsForAccount(accountId: String): Boolean

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: String): Int
}

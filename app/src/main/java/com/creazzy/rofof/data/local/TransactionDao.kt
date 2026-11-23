package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
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

    @Query("""
        SELECT t.* FROM transactions t
        WHERE (:query = '' OR lower(t.title) LIKE '%' || lower(:query) || '%' OR lower(COALESCE(t.notes, '')) LIKE '%' || lower(:query) || '%')
        ORDER BY t.occurredAtEpochMillis DESC LIMIT 200
    """)
    fun observeSearch(query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT t.categoryId AS categoryId, c.name AS categoryName,
               t.currencyCode AS currencyCode, t.currencyExponent AS currencyExponent,
               SUM(t.amountMinor) AS amountMinor
        FROM transactions t INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.transactionType = 'EXPENSE'
          AND t.occurredAtEpochMillis >= :startEpochMillis
          AND t.occurredAtEpochMillis < :endEpochMillis
          AND t.currencyCode = :currencyCode AND t.currencyExponent = :currencyExponent
        GROUP BY t.categoryId, c.name, t.currencyCode, t.currencyExponent
        ORDER BY amountMinor DESC
    """)
    fun observeCategoryExpenses(startEpochMillis: Long, endEpochMillis: Long, currencyCode: String, currencyExponent: Int): Flow<List<CategoryExpenseTotal>>

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

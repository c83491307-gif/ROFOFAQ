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
        FROM transactions WHERE isDeleted = 0
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
        WHERE isDeleted = 0 AND occurredAtEpochMillis >= :fromEpochMillis AND occurredAtEpochMillis < :toEpochMillis
        GROUP BY month, currencyCode, currencyExponent
        ORDER BY month, currencyCode, currencyExponent
    """)
    fun observeMonthlyCashFlow(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<MonthlyCashFlow>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY occurredAtEpochMillis DESC LIMIT 250")
    fun observeRecent(): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE isDeleted = 0 AND transactionType = 'INCOME' AND currencyCode = :currencyCode AND occurredAtEpochMillis >= :fromEpochMillis")
    fun observeIncomeSince(fromEpochMillis: Long, currencyCode: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE isDeleted = 0 AND transactionType = 'EXPENSE' AND currencyCode = :currencyCode AND occurredAtEpochMillis >= :fromEpochMillis")
    fun observeExpensesSince(fromEpochMillis: Long, currencyCode: String): Flow<Long>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN transactionType = 'INCOME' THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
            COALESCE(SUM(CASE WHEN transactionType = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor
        FROM transactions
        WHERE isDeleted = 0 AND currencyCode = :currencyCode AND currencyExponent = :currencyExponent
    """)
    fun observeTotals(currencyCode: String, currencyExponent: Int): Flow<TransactionTotals>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    /** Unique occurrence key prevents duplicate automatic charges after retries or reboot. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecurringIfAbsent(transaction: TransactionEntity): Long

    /** Room wraps a multi-row insert in one transaction; any conflict aborts the batch. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Query("UPDATE transactions SET isDeleted = 1, deletedAtEpochMillis = :deletedAt WHERE id = :id AND isDeleted = 0")
    suspend fun moveToTrash(id: String, deletedAt: Long): Int

    @Query("UPDATE transactions SET isDeleted = 0, deletedAtEpochMillis = NULL WHERE id = :id AND isDeleted = 1 AND deletedAtEpochMillis >= :restoreAfter")
    suspend fun restoreFromTrash(id: String, restoreAfter: Long): Int

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY deletedAtEpochMillis DESC")
    fun observeTrash(): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE isDeleted = 1 AND deletedAtEpochMillis < :cutoff")
    suspend fun purgeExpiredTrash(cutoff: Long): Int

    @Query("DELETE FROM transactions WHERE id = :id AND isDeleted = 1")
    suspend fun deletePermanently(id: String): Int

    @Query("""
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
        WHERE isDeleted = 0 AND transactionType = 'EXPENSE' AND categoryId = :categoryId
          AND currencyCode = :currencyCode AND currencyExponent = :currencyExponent
          AND occurredAtEpochMillis >= :fromEpochMillis AND occurredAtEpochMillis < :toEpochMillis
    """)
    suspend fun sumExpensesForCategoryBetween(categoryId: String, currencyCode: String, currencyExponent: Int, fromEpochMillis: Long, toEpochMillis: Long): Long

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE isDeleted = 0 AND transactionType = 'EXPENSE' AND categoryId = :categoryId
          AND currencyCode = :currencyCode AND currencyExponent = :currencyExponent
          AND occurredAtEpochMillis >= :fromEpochMillis AND occurredAtEpochMillis < :toEpochMillis
    """)
    suspend fun countExpensesForCategoryBetween(categoryId: String, currencyCode: String, currencyExponent: Int, fromEpochMillis: Long, toEpochMillis: Long): Int

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE isDeleted = 0 AND transactionType = 'EXPENSE' AND categoryId = :categoryId
          AND currencyCode = :currencyCode AND currencyExponent = :currencyExponent
          AND amountMinor = :amountMinor
          AND occurredAtEpochMillis >= :fromEpochMillis AND occurredAtEpochMillis < :toEpochMillis
    """)
    suspend fun countMatchingExpenses(categoryId: String, currencyCode: String, currencyExponent: Int, amountMinor: Long, fromEpochMillis: Long, toEpochMillis: Long): Int

    @Query("SELECT COUNT(*) > 0 FROM transactions WHERE accountId = :accountId")
    suspend fun hasTransactionsForAccount(accountId: String): Boolean

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: String): Int

    @Query("""
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
        WHERE isDeleted = 0 AND transactionType = 'EXPENSE'
          AND currencyCode = :currencyCode AND currencyExponent = :currencyExponent
          AND occurredAtEpochMillis >= :fromEpochMillis
          AND occurredAtEpochMillis < :toEpochMillis
    """)
    fun observeMonthlyExpenseTotal(
        currencyCode: String,
        currencyExponent: Int,
        fromEpochMillis: Long,
        toEpochMillis: Long
    ): Flow<Long>

    /** Monthly local-time behavior aggregates for the offline insights engine (Friday/Saturday weekend). */
    @Query("""
        SELECT
            COALESCE(SUM(CASE
                WHEN strftime('%w', occurredAtEpochMillis / 1000, 'unixepoch', 'localtime') IN ('5', '6')
                THEN amountMinor ELSE 0 END), 0) AS weekendExpenseMinor,
            COUNT(DISTINCT CASE
                WHEN strftime('%w', occurredAtEpochMillis / 1000, 'unixepoch', 'localtime') IN ('5', '6')
                THEN date(occurredAtEpochMillis / 1000, 'unixepoch', 'localtime') END) AS weekendDays,
            COALESCE(SUM(CASE
                WHEN strftime('%w', occurredAtEpochMillis / 1000, 'unixepoch', 'localtime') NOT IN ('5', '6')
                THEN amountMinor ELSE 0 END), 0) AS weekdayExpenseMinor,
            COUNT(DISTINCT CASE
                WHEN strftime('%w', occurredAtEpochMillis / 1000, 'unixepoch', 'localtime') NOT IN ('5', '6')
                THEN date(occurredAtEpochMillis / 1000, 'unixepoch', 'localtime') END) AS weekdayDays,
            COALESCE(SUM(amountMinor), 0) AS totalExpenseMinor,
            COALESCE(SUM(CASE
                WHEN lower(categories.name) LIKE '%coffee%' OR lower(categories.name) LIKE '%قهوة%'
                THEN transactions.amountMinor ELSE 0 END), 0) AS coffeeExpenseMinor,
            COALESCE((
                SELECT MAX(history.occurredAtEpochMillis)
                FROM transactions AS history
                WHERE history.isDeleted = 0 AND history.transactionType = 'EXPENSE'
                  AND history.currencyCode = :currencyCode
                  AND history.currencyExponent = :currencyExponent
            ), 0) AS lastExpenseAtEpochMillis
        FROM transactions
        LEFT JOIN categories ON categories.id = transactions.categoryId
        WHERE transactions.isDeleted = 0 AND transactions.transactionType = 'EXPENSE'
          AND transactions.currencyCode = :currencyCode
          AND transactions.currencyExponent = :currencyExponent
          AND transactions.occurredAtEpochMillis >= :fromEpochMillis
          AND transactions.occurredAtEpochMillis < :toEpochMillis
    """)
    fun observeMonthlyInsightsSnapshot(
        currencyCode: String,
        currencyExponent: Int,
        fromEpochMillis: Long,
        toEpochMillis: Long
    ): Flow<FinancialInsightsSnapshot>

    /** Lifetime expense totals, kept separate by currency and exponent. */
    @Query("""
        SELECT categoryId, currencyCode, currencyExponent,
            COALESCE(SUM(amountMinor), 0) AS totalMinor
        FROM transactions
        WHERE isDeleted = 0 AND transactionType = 'EXPENSE'
        GROUP BY categoryId, currencyCode, currencyExponent
        ORDER BY totalMinor DESC
    """)
    fun observeExpenseSumsByCategory(): Flow<List<CategoryExpenseSum>>

    @Query("""
        SELECT transactions.occurredAtEpochMillis AS occurredAtEpochMillis,
            COALESCE(categories.name, '') AS categoryName,
            transactions.amountMinor AS amountMinor,
            transactions.currencyCode AS currencyCode,
            transactions.currencyExponent AS currencyExponent,
            transactions.notes AS note
        FROM transactions
        LEFT JOIN categories ON categories.id = transactions.categoryId
        WHERE transactions.isDeleted = 0
        ORDER BY transactions.occurredAtEpochMillis DESC
    """)
    suspend fun getAllForCsvExport(): List<TransactionCsvRow>

    /**
     * Local transaction search. Amount matching supports both the stored minor-unit
     * integer and its decimal display representation; date upper bound is exclusive.
     */
    @Query("""
        SELECT * FROM transactions
        WHERE isDeleted = 0 AND (
            :query = '' OR
            title LIKE '%' || :query || '%' COLLATE NOCASE OR
            COALESCE(notes, '') LIKE '%' || :query || '%' COLLATE NOCASE OR
            CAST(amountMinor AS TEXT) LIKE '%' || :query || '%' OR
            printf('%.*f', currencyExponent,
                amountMinor / CASE currencyExponent
                    WHEN 0 THEN 1.0
                    WHEN 1 THEN 10.0
                    WHEN 2 THEN 100.0
                    WHEN 3 THEN 1000.0
                    ELSE 100.0
                END
            ) LIKE '%' || :query || '%'
        )
        AND (:fromEpochMillis IS NULL OR occurredAtEpochMillis >= :fromEpochMillis)
        AND (:toEpochMillis IS NULL OR occurredAtEpochMillis < :toEpochMillis)
        AND (:transactionType IS NULL OR transactionType = :transactionType)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:currencyCode IS NULL OR currencyCode = :currencyCode)
        ORDER BY occurredAtEpochMillis DESC, createdAtEpochMillis DESC
    """)
    fun searchTransactions(
        query: String,
        fromEpochMillis: Long? = null,
        toEpochMillis: Long? = null,
        transactionType: String? = null,
        categoryId: String? = null,
        currencyCode: String? = null
    ): Flow<List<TransactionEntity>>

}

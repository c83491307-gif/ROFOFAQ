package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC LIMIT 250")
    fun observeRecent(): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE transactionType = 'INCOME' AND currencyCode = :currencyCode AND occurredAtEpochMillis >= :fromEpochMillis")
    fun observeIncomeSince(fromEpochMillis: Long, currencyCode: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE transactionType = 'EXPENSE' AND currencyCode = :currencyCode AND occurredAtEpochMillis >= :fromEpochMillis")
    fun observeExpensesSince(fromEpochMillis: Long, currencyCode: String): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)
}

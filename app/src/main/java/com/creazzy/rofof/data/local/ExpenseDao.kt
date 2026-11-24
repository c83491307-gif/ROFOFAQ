package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY occurredAtEpochMillis DESC LIMIT 100")
    fun observeRecent(): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM expenses WHERE occurredAtEpochMillis >= :fromEpochMillis")
    fun observeTotalSince(fromEpochMillis: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: String)
}

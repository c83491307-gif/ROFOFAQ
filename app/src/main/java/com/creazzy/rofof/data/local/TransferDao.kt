package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY occurredAtEpochMillis DESC LIMIT 250")
    fun observeRecent(): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transfer: TransferEntity)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) > 0 FROM transfers WHERE fromAccountId = :accountId OR toAccountId = :accountId")
    suspend fun hasTransfersForAccount(accountId: String): Boolean
}

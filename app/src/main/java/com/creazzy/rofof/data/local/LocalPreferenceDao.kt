package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalPreferenceDao {
    @Query("SELECT * FROM local_preferences WHERE `key` = :key LIMIT 1")
    fun observe(key: String): Flow<LocalPreferenceEntity?>

    @Upsert
    suspend fun put(preference: LocalPreferenceEntity)

    @Query("DELETE FROM local_preferences WHERE `key` = :key")
    suspend fun delete(key: String)
}

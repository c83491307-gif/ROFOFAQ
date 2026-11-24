package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedKeywordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keyword: LearnedKeyword)

    @Query("SELECT * FROM learned_keywords ORDER BY keyword COLLATE NOCASE")
    fun observeAll(): Flow<List<LearnedKeyword>>

    @Query("SELECT * FROM learned_keywords ORDER BY keyword COLLATE NOCASE")
    suspend fun getAll(): List<LearnedKeyword>

    @Query("DELETE FROM learned_keywords WHERE keyword = :keyword")
    suspend fun delete(keyword: String)
}

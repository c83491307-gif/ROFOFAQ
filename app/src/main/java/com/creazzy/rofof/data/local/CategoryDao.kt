package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND transactionType = :type LIMIT 1")
    suspend fun findByNameAndType(name: String, type: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE categories SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("SELECT COUNT(*) FROM categories WHERE parentId = :parentId AND isArchived = 0")
    suspend fun countChildren(parentId: String): Int
}

package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "transactionType"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val transactionType: String,
    val parentId: String?,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long
)

package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-confirmed local correction for the smart-entry category matcher.
 * The keyword is normalized by the learning service before it is stored.
 */
@Entity(
    tableName = "learned_keywords",
    indices = [Index(value = ["categoryId"])]
)
data class LearnedKeyword(
    @PrimaryKey val keyword: String,
    val categoryId: String
)

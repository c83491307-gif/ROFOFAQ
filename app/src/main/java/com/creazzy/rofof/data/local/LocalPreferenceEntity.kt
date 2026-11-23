package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_preferences")
data class LocalPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAtEpochMillis: Long
)

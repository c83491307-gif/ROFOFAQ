package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["occurredAtEpochMillis"])]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val category: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long
)

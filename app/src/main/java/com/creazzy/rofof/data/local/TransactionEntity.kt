package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredAtEpochMillis"]),
        Index(value = ["accountId", "occurredAtEpochMillis"]),
        Index(value = ["categoryId", "occurredAtEpochMillis"]),
        Index(value = ["recurringOccurrenceKey"], unique = true),
        Index(value = ["isDeleted", "occurredAtEpochMillis"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val transactionType: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val accountId: String,
    val categoryId: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val notes: String?,
    val recurringOccurrenceKey: String? = null,
    val isDeleted: Boolean = false,
    val deletedAtEpochMillis: Long? = null
)

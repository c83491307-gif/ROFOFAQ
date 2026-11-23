package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transfers",
    indices = [
        Index(value = ["fromAccountId", "occurredAtEpochMillis"]),
        Index(value = ["toAccountId", "occurredAtEpochMillis"])
    ]
)
data class TransferEntity(
    @PrimaryKey val id: String,
    val fromAccountId: String,
    val toAccountId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val note: String?
)

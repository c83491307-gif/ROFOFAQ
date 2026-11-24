package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currencyCode: String,
    val currencyExponent: Int,
    val openingBalanceMinor: Long,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long
)

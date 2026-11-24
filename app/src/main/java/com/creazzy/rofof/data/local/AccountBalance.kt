package com.creazzy.rofof.data.local

data class AccountBalance(
    val id: String,
    val name: String,
    val currencyCode: String,
    val currencyExponent: Int,
    val openingBalanceMinor: Long,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long,
    val balanceMinor: Long
)

package com.creazzy.rofof.domain.model

data class Expense(
    val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val category: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long
)

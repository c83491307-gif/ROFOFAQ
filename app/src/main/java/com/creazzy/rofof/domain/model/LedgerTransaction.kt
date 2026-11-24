package com.creazzy.rofof.domain.model

data class LedgerTransaction(
    val id: String,
    val title: String,
    val transactionType: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val accountId: String,
    val categoryId: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val notes: String?
)

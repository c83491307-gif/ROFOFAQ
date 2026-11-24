package com.creazzy.rofof.data.local

data class CurrencyTransactionTotals(
    val currencyCode: String,
    val currencyExponent: Int,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val transactionCount: Long
)

data class MonthlyCashFlow(
    val month: String,
    val currencyCode: String,
    val currencyExponent: Int,
    val incomeMinor: Long,
    val expenseMinor: Long
)

data class TransactionCsvRow(
    val occurredAtEpochMillis: Long,
    val categoryName: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val note: String?
)

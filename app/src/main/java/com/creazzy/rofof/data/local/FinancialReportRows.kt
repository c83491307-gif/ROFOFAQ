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

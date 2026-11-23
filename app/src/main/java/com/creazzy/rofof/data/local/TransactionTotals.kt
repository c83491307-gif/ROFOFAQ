package com.creazzy.rofof.data.local

data class TransactionTotals(
    val incomeMinor: Long,
    val expenseMinor: Long
)

data class CategoryExpenseTotal(
    val categoryId: String,
    val categoryName: String,
    val currencyCode: String,
    val currencyExponent: Int,
    val amountMinor: Long
)

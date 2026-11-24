package com.creazzy.rofof.data.local

/**
 * Expense aggregate for one category and one currency.
 * Amounts stay in minor units; currencies are never combined.
 */
data class CategoryExpenseSum(
    val categoryId: String,
    val currencyCode: String,
    val currencyExponent: Int,
    val totalMinor: Long
)

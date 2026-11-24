package com.creazzy.rofof.data.local

data class FinancialInsightsSnapshot(
    val weekendExpenseMinor: Long,
    val weekendDays: Int,
    val weekdayExpenseMinor: Long,
    val weekdayDays: Int,
    val totalExpenseMinor: Long,
    val coffeeExpenseMinor: Long,
    val lastExpenseAtEpochMillis: Long
)

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

data class WeekdayExpense(
    /** SQLite local-time weekday: Sunday=0 through Saturday=6. */
    val dayOfWeek: Int,
    val totalExpenseMinor: Long,
    val occurrenceDays: Int
)

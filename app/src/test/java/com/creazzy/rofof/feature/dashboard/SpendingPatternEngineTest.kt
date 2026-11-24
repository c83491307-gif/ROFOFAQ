package com.creazzy.rofof.feature.dashboard

import com.creazzy.rofof.data.local.WeekdayExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendingPatternEngineTest {
    @Test fun detectsWeekdayAnomalyAndBudgetOverrunForecast() {
        val rows = listOf(
            WeekdayExpense(dayOfWeek = 1, totalExpenseMinor = 9_000L, occurrenceDays = 3),
            WeekdayExpense(dayOfWeek = 2, totalExpenseMinor = 3_000L, occurrenceDays = 3),
            WeekdayExpense(dayOfWeek = 3, totalExpenseMinor = 3_000L, occurrenceDays = 3),
            WeekdayExpense(dayOfWeek = 4, totalExpenseMinor = 3_000L, occurrenceDays = 3),
            WeekdayExpense(dayOfWeek = 5, totalExpenseMinor = 3_000L, occurrenceDays = 3),
            WeekdayExpense(dayOfWeek = 6, totalExpenseMinor = 3_000L, occurrenceDays = 3)
        )

        val insights = SpendingPatternEngine.analyze(
            weekdayRows = rows,
            monthExpenseMinor = 12_000L,
            monthlyBudgetMinor = 20_000L,
            monthStartDate = java.time.LocalDate.of(2026, 11, 1),
            monthStartDate = java.time.LocalDate.of(2026, 11, 1),
            elapsedDays = 10,
            daysInMonth = 30,
            language = "ar"
        )

        assertEquals(2, insights.size)
        assertTrue(insights.any { it.contains("الاثنين") })
        assertTrue(insights.any { it.contains("تتجاوز") })
    }

    @Test fun doesNotForecastAnOverrunWhenProjectionEqualsBudget() {
        val insights = SpendingPatternEngine.analyze(
            weekdayRows = emptyList(),
            monthExpenseMinor = 10_000L,
            monthlyBudgetMinor = 30_000L,
            elapsedDays = 10,
            daysInMonth = 30,
            language = "en"
        )
        assertTrue(insights.isEmpty())
    }

    @Test fun doesNotCompareDifferentCurrenciesOrInvalidWeekdays() {
        val insights = SpendingPatternEngine.analyze(
            weekdayRows = listOf(WeekdayExpense(8, 50_000L, 1)),
            monthExpenseMinor = 0L,
            monthlyBudgetMinor = 0L,
            monthStartDate = java.time.LocalDate.of(2026, 11, 1),
            elapsedDays = 1,
            daysInMonth = 30,
            language = "en"
        )
        assertTrue(insights.isEmpty())
    }
}

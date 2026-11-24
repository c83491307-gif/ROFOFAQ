package com.creazzy.rofof.feature.dashboard

import com.creazzy.rofof.data.local.FinancialInsightsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LocalInsightsEngineTest {
    private val zone = ZoneId.of("UTC")
    private val now = LocalDate.of(2026, 11, 24).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test fun detectsWeekendPerDayIncreaseCoffeeShareAndThreeDayGap() {
        val lastExpense = LocalDate.of(2026, 11, 20).atStartOfDay(zone).toInstant().toEpochMilli()
        val snapshot = FinancialInsightsSnapshot(
            weekendExpenseMinor = 6_000L,
            weekendDays = 2,
            weekdayExpenseMinor = 8_000L,
            weekdayDays = 8,
            totalExpenseMinor = 10_000L,
            coffeeExpenseMinor = 2_000L,
            lastExpenseAtEpochMillis = lastExpense
        )

        val insights = LocalInsightsEngine.generate(snapshot, now, "ar", zone)

        assertEquals(3, insights.size)
        assertTrue(insights.any { it.contains("عطلة نهاية الأسبوع") })
        assertTrue(insights.any { it.contains("القهوة") })
        assertTrue(insights.any { it.contains("3 أيام") })
    }

    @Test fun comparesAverageDailySpendInsteadOfRawWeekendTotals() {
        val snapshot = FinancialInsightsSnapshot(
            weekendExpenseMinor = 4_000L,
            weekendDays = 4,
            weekdayExpenseMinor = 8_000L,
            weekdayDays = 8,
            totalExpenseMinor = 12_000L,
            coffeeExpenseMinor = 0L,
            lastExpenseAtEpochMillis = now
        )

        val insights = LocalInsightsEngine.generate(snapshot, now, "en", zone)

        assertEquals(0, insights.size)
    }

    @Test fun ignoresCoffeeRatioAtExactlyFifteenPercent() {
        val snapshot = FinancialInsightsSnapshot(
            weekendExpenseMinor = 0L,
            weekendDays = 0,
            weekdayExpenseMinor = 10_000L,
            weekdayDays = 10,
            totalExpenseMinor = 10_000L,
            coffeeExpenseMinor = 1_500L,
            lastExpenseAtEpochMillis = now
        )

        assertTrue(LocalInsightsEngine.generate(snapshot, now, "en", zone).isEmpty())
    }
}

package com.creazzy.rofof.feature.analytics

import com.creazzy.rofof.data.local.MonthlyCashFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CashFlowForecastTest {
    @Test fun projectsAverageSeparatelyForEachCurrency() {
        val rows = listOf(
            MonthlyCashFlow("2026-01", "EGP", 2, 100_00, 60_00),
            MonthlyCashFlow("2026-02", "EGP", 2, 120_00, 80_00),
            MonthlyCashFlow("2026-01", "USD", 2, 50_00, 20_00)
        )
        val result = CashFlowForecast.project(rows)
        assertEquals(2, result.size)
        assertEquals(110_00L, result.first { it.currencyCode == "EGP" }.averageIncomeMinor)
        assertEquals(70_00L, result.first { it.currencyCode == "EGP" }.averageExpenseMinor)
        assertEquals(30_00L, result.first { it.currencyCode == "EGP" }.projectedNetMinor)
        assertEquals(3_000L, result.first { it.currencyCode == "USD" }.projectedNetMinor)
    }

    @Test fun rejectsOverflowRatherThanReturningWrappedTotals() {
        val rows = listOf(
            MonthlyCashFlow("2026-01", "EGP", 2, Long.MAX_VALUE, 0),
            MonthlyCashFlow("2026-02", "EGP", 2, 1, 0)
        )
        org.junit.Assert.assertThrows(ArithmeticException::class.java) {
            CashFlowForecast.project(rows)
        }
    }
}

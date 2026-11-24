package com.creazzy.rofof.feature.insights

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class FinancialIntelligenceTest {
    private val egp = MoneyKey("EGP", 2)
    private fun sample(id: String, amount: Long, day: Int, category: String = "food", title: String = "Groceries") =
        ExpenseSample(id, title, category, amount, egp, LocalDate.of(2026, 1, 1).plusDays((day - 1).toLong()))

    @Test fun anomalyDetectionRequiresComparableHistoryAndCurrency() {
        val history = (1..5).map { sample("h$it", 1000L, it) }
        assertEquals(1, FinancialAnomalyDetector.detect(history, listOf(sample("new", 5000L, 10))).size)
        assertTrue(FinancialAnomalyDetector.detect(history.take(4), listOf(sample("new", 5000L, 10))).isEmpty())
    }

    @Test fun subscriptionsRequireStableAmountAndRecurringCadence() {
        val rows = listOf(1, 31, 60).mapIndexed { i, day -> sample("s$i", 999L, day, title = "  Stream  Plan ") }
        assertEquals(1, SubscriptionIntelligence.detect(rows).size)
        assertTrue(SubscriptionIntelligence.detect(rows.mapIndexed { i, row -> row.copy(id = "x$i", amountMinor = if (i == 2) 5000L else row.amountMinor) }).isEmpty())
    }

    @Test fun scenarioSimulatorUsesExactIntegerArithmetic() {
        val result = FinancialScenarioSimulator.simulate(ScenarioInput(10_000L, 5_000L, 3_000L, 2_000L, 3))
        assertEquals(14_000L, result.endingBalanceMinor)
        assertTrue(result.affordable)
    }

    @Test fun healthScoreIsBoundedAndPenalizesOverdueDebt() {
        val healthy = FinancialHealthScorer.score(HealthInputs(600_000L, 100_000L, 50_000L, 0L, 20_000L))
        val overdue = FinancialHealthScorer.score(HealthInputs(600_000L, 100_000L, 50_000L, 10_000L, 20_000L))
        assertTrue(healthy.score in 0..100)
        assertTrue(overdue.score < healthy.score)
    }

    @Test fun reconciliationKeepsCurrenciesAtAccountBoundaries() {
        val results = AccountReconciliation.compare(listOf(
            AccountBalanceSnapshot("cash", egp, 1000L, 1000L),
            AccountBalanceSnapshot("card", MoneyKey("USD", 2), 500L, 450L)
        ))
        assertTrue(results.first().reconciled)
        assertEquals(-50L, results.last().differenceMinor)
    }

    @Test fun billCalendarUsesInclusiveDateRangeAndExcludesPaid() {
        val rows = listOf(
            BillEvent("a", "Rent", LocalDate.of(2026, 1, 10), 1000L, egp, false),
            BillEvent("b", "Internet", LocalDate.of(2026, 1, 11), 500L, egp, true)
        )
        assertEquals(listOf("a"), BillsCenter.upcoming(rows, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10)).map { it.id })
    }

    @Test fun receiptExtractionRemainsDraftUnlessRequiredFieldsAreTrusted() {
        val digest = "a".repeat(64)
        assertTrue(ReceiptIntelligence.isReadyForReview(ReceiptReview("Shop", 1200L, egp, LocalDate.now(), .92, digest)))
        assertFalse(ReceiptIntelligence.isReadyForReview(ReceiptReview("Shop", 1200L, egp, LocalDate.now(), .45, digest)))
    }

    @Test fun netWorthIsSeparatedByCurrencyAndIncludesLiabilitiesAsNegative() {
        val totals = NetWorthCalculator.totals(listOf(
            AssetLiability("cash", egp, 10_000L, true),
            AssetLiability("loan", egp, 3_000L, false),
            AssetLiability("usd", MoneyKey("USD", 2), 500L, true)
        ))
        assertEquals(7_000L, totals[egp])
        assertEquals(500L, totals[MoneyKey("USD", 2)])
    }

    @Test fun cashFlowCalendarGroupsByDateAndCurrency() {
        val rows = listOf(
            CashFlowDay(LocalDate.of(2026, 1, 1), 1000L, 300L, egp),
            CashFlowDay(LocalDate.of(2026, 1, 1), 0L, 200L, egp),
            CashFlowDay(LocalDate.of(2026, 1, 1), 100L, 0L, MoneyKey("USD", 2))
        )
        val totals = CashFlowCalendar.dailyNet(rows)
        assertEquals(500L, totals[LocalDate.of(2026, 1, 1) to egp])
        assertEquals(100L, totals[LocalDate.of(2026, 1, 1) to MoneyKey("USD", 2)])
    }

    @Test fun deterministicGeneratedScenariosNeverProduceNegativeScoreOrOverflow() {
        val random = java.util.Random(731_991L)
        repeat(2_000) {
            val balance = random.nextInt(1_000_000).toLong()
            val income = random.nextInt(100_000).toLong()
            val expenses = random.nextInt(100_000).toLong()
            val cost = random.nextInt(200_000).toLong()
            val months = random.nextInt(24) + 1
            val result = FinancialScenarioSimulator.simulate(ScenarioInput(balance, income, expenses, cost, months))
            val expected = balance + (income - expenses) * months - cost
            assertEquals(expected, result.endingBalanceMinor)
            assertEquals(expected >= 0L, result.affordable)
        }
    }

    @Test fun behavioralRankingIsStableAndBounded() {
        val result = BehavioralFinanceEngine.topConcentration(listOf(
            CategoryBehavior("a", 300L, 2), CategoryBehavior("b", 900L, 1), CategoryBehavior("c", 100L, 4)
        ), 2)
        assertEquals(listOf("b", "a"), result.map { it.categoryId })
    }
}

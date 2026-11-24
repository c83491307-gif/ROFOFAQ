package com.creazzy.rofof.feature.insights

import java.math.BigInteger
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class MoneyKey(val currencyCode: String, val currencyExponent: Int)
data class ExpenseSample(
    val id: String, val title: String, val categoryId: String, val amountMinor: Long,
    val currency: MoneyKey, val occurredOn: LocalDate
) {
    init { require(amountMinor > 0L); require(currency.currencyCode.matches(Regex("[A-Z]{3}"))); require(currency.currencyExponent in 0..4) }
}

data class Anomaly(val sampleId: String, val baselineMedianMinor: Long, val multiple: Int)
object FinancialAnomalyDetector {
    /** Requires five prior comparable observations; never compares unlike currencies/categories. */
    fun detect(history: List<ExpenseSample>, candidates: List<ExpenseSample>, multiplier: Int = 3): List<Anomaly> {
        require(multiplier >= 2)
        return candidates.mapNotNull { candidate ->
            val comparable = history.asSequence()
                .filter { it.id != candidate.id && it.categoryId == candidate.categoryId && it.currency == candidate.currency && it.occurredOn < candidate.occurredOn }
                .map { it.amountMinor }.sorted().toList()
            if (comparable.size < 5) return@mapNotNull null
            val median = comparable[comparable.size / 2]
            if (median > 0L && BigInteger.valueOf(candidate.amountMinor) >= BigInteger.valueOf(median).multiply(BigInteger.valueOf(multiplier.toLong()))) {
                Anomaly(candidate.id, median, multiplier)
            } else null
        }
    }
}

data class SubscriptionCandidate(val normalizedTitle: String, val currency: MoneyKey, val amountMinor: Long, val intervalDays: Long, val confidence: Int, val occurrenceCount: Int)
object SubscriptionIntelligence {
    /** Groups exact normalized titles and currency; cadence is inferred from consecutive dates. */
    fun detect(samples: List<ExpenseSample>): List<SubscriptionCandidate> =
        samples.groupBy { it.title.trim().lowercase().replace(Regex("\\s+"), " ") to it.currency }
            .mapNotNull { (key, rows) ->
                val ordered = rows.sortedBy { it.occurredOn }
                if (ordered.size < 3) return@mapNotNull null
                val amounts = ordered.map { it.amountMinor }.sorted()
                val medianAmount = amounts[amounts.size / 2]
                val stable = amounts.all {
                    BigInteger.valueOf(it).subtract(BigInteger.valueOf(medianAmount)).abs().multiply(BigInteger.TEN) <=
                        BigInteger.valueOf(medianAmount.coerceAtLeast(1L))
                }
                val gaps = ordered.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.occurredOn, b.occurredOn) }
                val medianGap = gaps.sorted()[gaps.size / 2]
                val cadence = medianGap in 25..35 || medianGap in 6..8 || medianGap in 360..370
                if (!stable || !cadence) null else SubscriptionCandidate(key.first, key.second, medianAmount, medianGap, if (gaps.all { kotlin.math.abs(it - medianGap) <= 3L }) 95 else 75, ordered.size)
            }.sortedByDescending { it.confidence }
}

data class CategoryBehavior(val categoryId: String, val totalMinor: Long, val transactionCount: Int)
object BehavioralFinanceEngine {
    /** Deterministic ranking only; callers must supply one currency and one analysis window. */
    fun topConcentration(categories: List<CategoryBehavior>, topCount: Int = 3): List<CategoryBehavior> {
        require(topCount > 0)
        return categories.filter { it.totalMinor >= 0L && it.transactionCount >= 0 }.sortedByDescending { it.totalMinor }.take(topCount)
    }
}

data class ScenarioInput(val currentBalanceMinor: Long, val monthlyIncomeMinor: Long, val monthlyExpenseMinor: Long, val oneTimeCostMinor: Long, val months: Int) {
    init { require(currentBalanceMinor >= 0 && monthlyIncomeMinor >= 0 && monthlyExpenseMinor >= 0 && oneTimeCostMinor >= 0 && months in 1..120) }
}
data class ScenarioResult(val endingBalanceMinor: Long, val monthlyNetMinor: Long, val affordable: Boolean)
object FinancialScenarioSimulator {
    fun simulate(input: ScenarioInput): ScenarioResult {
        val net = BigInteger.valueOf(input.monthlyIncomeMinor).subtract(BigInteger.valueOf(input.monthlyExpenseMinor))
        val ending = BigInteger.valueOf(input.currentBalanceMinor).add(net.multiply(BigInteger.valueOf(input.months.toLong()))).subtract(BigInteger.valueOf(input.oneTimeCostMinor))
        require(ending.bitLength() < 63) { "Scenario exceeds supported monetary range" }
        require(net.bitLength() < 63) { "Monthly net exceeds supported monetary range" }
        return ScenarioResult(ending.toLong(), net.toLong(), ending.signum() >= 0)
    }
}

data class HealthInputs(val liquidAssetsMinor: Long, val monthlyIncomeMinor: Long, val monthlyEssentialExpensesMinor: Long, val overdueLiabilitiesMinor: Long, val totalLiabilitiesMinor: Long)
data class FinancialHealthScore(val score: Int, val emergencyCoverageMonths: Int, val debtToIncomeBasisPoints: Int, val factors: List<String>)
object FinancialHealthScorer {
    fun score(input: HealthInputs): FinancialHealthScore {
        require(listOf(input.liquidAssetsMinor, input.monthlyIncomeMinor, input.monthlyEssentialExpensesMinor, input.overdueLiabilitiesMinor, input.totalLiabilitiesMinor).all { it >= 0L })
        val coverage = if (input.monthlyEssentialExpensesMinor == 0L) 12 else (input.liquidAssetsMinor / input.monthlyEssentialExpensesMinor).coerceAtMost(12).toInt()
        val dti = if (input.monthlyIncomeMinor == 0L) { if (input.totalLiabilitiesMinor == 0L) 0 else 10_000 } else
            BigInteger.valueOf(input.totalLiabilitiesMinor).multiply(BigInteger.valueOf(10_000L)).divide(BigInteger.valueOf(input.monthlyIncomeMinor)).coerceAtMost(BigInteger.valueOf(10_000L)).toInt()
        val coveragePoints = (coverage * 5).coerceAtMost(40)
        val debtPoints = when { dti <= 1000 -> 30; dti <= 3000 -> 24; dti <= 5000 -> 15; else -> 5 }
        val overduePoints = if (input.overdueLiabilitiesMinor == 0L) 30 else 0
        val factors = buildList {
            if (coverage < 3) add("Emergency savings below three months")
            if (dti > 3000) add("Debt burden exceeds 30% of monthly income")
            if (input.overdueLiabilitiesMinor > 0L) add("Overdue liabilities exist")
        }
        return FinancialHealthScore((coveragePoints + debtPoints + overduePoints).coerceIn(0, 100), coverage, dti, factors)
    }
}

data class AccountBalanceSnapshot(val accountId: String, val currency: MoneyKey, val expectedMinor: Long, val statementMinor: Long)
data class ReconciliationResult(val accountId: String, val differenceMinor: Long, val reconciled: Boolean)
object AccountReconciliation {
    fun compare(snapshots: List<AccountBalanceSnapshot>): List<ReconciliationResult> {
        require(snapshots.map { it.accountId }.distinct().size == snapshots.size) { "Duplicate account snapshot" }
        return snapshots.map {
            require(it.accountId.isNotBlank())
            val difference = BigInteger.valueOf(it.statementMinor).subtract(BigInteger.valueOf(it.expectedMinor))
            require(difference.bitLength() < 63) { "Balance difference overflow" }
            ReconciliationResult(it.accountId, difference.toLong(), difference.signum() == 0)
        }
    }
}

data class BillEvent(val id: String, val title: String, val dueDate: LocalDate, val amountMinor: Long, val currency: MoneyKey, val isPaid: Boolean)
object BillsCenter {
    fun upcoming(events: List<BillEvent>, from: LocalDate, through: LocalDate): List<BillEvent> {
        require(!through.isBefore(from))
        return events.filter { !it.isPaid && it.amountMinor > 0L && !it.dueDate.isBefore(from) && !it.dueDate.isAfter(through) }
            .sortedWith(compareBy(BillEvent::dueDate, BillEvent::title))
    }
}

data class ReceiptReview(val merchant: String?, val amountMinor: Long?, val currency: MoneyKey?, val date: LocalDate?, val confidence: Double, val sourceDigest: String)
object ReceiptIntelligence {
    /** OCR is draft-only: incomplete/low-confidence extraction cannot post a transaction. */
    fun isReadyForReview(value: ReceiptReview): Boolean =
        !value.merchant.isNullOrBlank() && value.amountMinor != null && value.amountMinor > 0L &&
            value.currency != null && value.date != null && value.confidence in 0.80..1.0 &&
            value.sourceDigest.matches(Regex("[a-fA-F0-9]{64}"))
}

data class AssetLiability(val id: String, val currency: MoneyKey, val amountMinor: Long, val isAsset: Boolean)
object NetWorthCalculator {
    fun totals(items: List<AssetLiability>): Map<MoneyKey, Long> = items.groupBy { it.currency }.mapValues { (_, rows) ->
        rows.fold(BigInteger.ZERO) { total, row ->
            val amount = BigInteger.valueOf(row.amountMinor)
            total + if (row.isAsset) amount else amount.negate()
        }.also { require(it.bitLength() < 63) { "Net worth overflow" } }.toLong()
    }
}

data class CashFlowDay(val date: LocalDate, val expectedIncomeMinor: Long, val expectedExpenseMinor: Long, val currency: MoneyKey)
object CashFlowCalendar {
    fun dailyNet(rows: List<CashFlowDay>): Map<Pair<LocalDate, MoneyKey>, Long> =
        rows.groupBy { it.date to it.currency }.mapValues { (_, days) ->
            days.fold(BigInteger.ZERO) { total, row ->
                require(row.expectedIncomeMinor >= 0L && row.expectedExpenseMinor >= 0L)
                total + BigInteger.valueOf(row.expectedIncomeMinor) - BigInteger.valueOf(row.expectedExpenseMinor)
            }.also { require(it.bitLength() < 63) { "Daily cash flow overflow" } }.toLong()
        }
}

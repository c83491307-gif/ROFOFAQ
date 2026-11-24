package com.creazzy.rofof.feature.dashboard

import com.creazzy.rofof.data.local.FinancialInsightsSnapshot
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Deterministic, offline-only insights. Monetary comparisons stay within one currency. */
object LocalInsightsEngine {
    fun generate(
        snapshot: FinancialInsightsSnapshot,
        nowEpochMillis: Long,
        language: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<String> {
        val arabic = language == "ar"
        val insights = mutableListOf<String>()

        if (snapshot.weekendDays > 0 && snapshot.weekdayDays > 0 &&
            snapshot.weekdayExpenseMinor > 0L
        ) {
            val weekendScaled = BigInteger.valueOf(snapshot.weekendExpenseMinor)
                .multiply(BigInteger.valueOf(snapshot.weekdayDays.toLong()))
                .multiply(BigInteger.valueOf(2L))
            val weekdayThreshold = BigInteger.valueOf(snapshot.weekdayExpenseMinor)
                .multiply(BigInteger.valueOf(snapshot.weekendDays.toLong()))
                .multiply(BigInteger.valueOf(3L))
            if (weekendScaled > weekdayThreshold) {
                insights += if (arabic) {
                    "إنفاقك اليومي في عطلة نهاية الأسبوع أعلى بأكثر من 50%. جرّب تخصيص ميزانية للترفيه."
                } else {
                    "Your daily weekend spending is over 50% higher. Try setting an entertainment budget."
                }
            }
        }

        if (snapshot.totalExpenseMinor > 0L &&
            BigInteger.valueOf(snapshot.coffeeExpenseMinor).multiply(BigInteger.valueOf(100L)) >
                BigInteger.valueOf(snapshot.totalExpenseMinor).multiply(BigInteger.valueOf(15L))
        ) {
            insights += if (arabic) {
                "القهوة تتجاوز 15% من مصروفاتك هذا الشهر. تحضيرها في المنزل قد يوفر لك."
            } else {
                "Coffee accounts for over 15% of this month's expenses. Brewing at home may help you save."
            }
        }

        if (snapshot.lastExpenseAtEpochMillis > 0L) {
            val lastDate = Instant.ofEpochMilli(snapshot.lastExpenseAtEpochMillis).atZone(zone).toLocalDate()
            val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
            if (ChronoUnit.DAYS.between(lastDate, today) >= 3L) {
                insights += if (arabic) {
                    "لم تسجل مصروفاً منذ 3 أيام. هل نسيت تسجيل بعض معاملاتك؟"
                } else {
                    "You haven't recorded an expense for 3 days. Did you miss any transactions?"
                }
            }
        }
        return insights
    }
}

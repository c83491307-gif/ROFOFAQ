package com.creazzy.rofof.feature.dashboard

import com.creazzy.rofof.data.local.WeekdayExpense
import java.math.BigInteger

/** Deterministic local-only weekday anomaly detection and month-end run-rate forecast. */
object SpendingPatternEngine {
    fun analyze(
        weekdayRows: List<WeekdayExpense>,
        monthExpenseMinor: Long,
        monthlyBudgetMinor: Long,
        elapsedDays: Int,
        daysInMonth: Int,
        language: String
    ): List<String> {
        require(monthExpenseMinor >= 0L && monthlyBudgetMinor >= 0L)
        require(elapsedDays >= 1 && daysInMonth >= elapsedDays)
        val arabic = language == "ar"
        val results = mutableListOf<String>()

        val observedAverages = weekdayRows
            .filter { it.dayOfWeek in 0..6 && it.occurrenceDays > 0 && it.totalExpenseMinor >= 0L }
            .associate { it.dayOfWeek to (it.totalExpenseMinor.toDouble() / it.occurrenceDays) }
        // A weekday with no recorded expenses contributes zero, not a missing comparison.
        val dailyAverageByWeekday = (0..6).associateWith { observedAverages[it] ?: 0.0 }

        val weekdayNamesAr = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val weekdayNamesEn = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        for ((day, average) in dailyAverageByWeekday) {
            val otherAverages = (0..6).filter { it != day }.map { dailyAverageByWeekday.getValue(it) }
            if (otherAverages.isEmpty()) continue
            val baseline = otherAverages.average()
            if (baseline > 0.0 && average > baseline * 1.5) {
                val dayName = if (arabic) weekdayNamesAr[day] else weekdayNamesEn[day]
                results += if (arabic) {
                    "إنفاقك يوم $dayName أعلى بأكثر من 50% من متوسط بقية أيام الأسبوع."
                } else {
                    "Your spending on $dayName is over 50% higher than the average for other weekdays."
                }
            }
        }

        if (monthlyBudgetMinor > 0L && monthExpenseMinor > 0L) {
            val projected = BigInteger.valueOf(monthExpenseMinor)
                .multiply(BigInteger.valueOf(daysInMonth.toLong()))
                .divide(BigInteger.valueOf(elapsedDays.toLong()))
            if (projected > BigInteger.valueOf(monthlyBudgetMinor)) {
                results += if (arabic) {
                    "بهذا المعدل، يُتوقع أن تتجاوز مصروفاتك ميزانية الشهر قبل نهايته."
                } else {
                    "At this rate, your projected monthly spending will exceed your budget."
                }
            }
        }
        return results
    }
}

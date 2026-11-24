package com.creazzy.rofof.feature.analytics

import com.creazzy.rofof.data.local.MonthlyCashFlow

/** Conservative baseline forecast: arithmetic mean of supplied complete months per currency. */
object CashFlowForecast {
    data class Projection(
        val currencyCode: String,
        val currencyExponent: Int,
        val averageIncomeMinor: Long,
        val averageExpenseMinor: Long,
        val projectedNetMinor: Long,
        val monthsObserved: Int
    )

    fun project(months: List<MonthlyCashFlow>): List<Projection> =
        months.groupBy { it.currencyCode to it.currencyExponent }
            .map { (currency, rows) ->
                require(rows.isNotEmpty())
                val income = rows.fold(0L) { total, row -> Math.addExact(total, row.incomeMinor) }
                val expense = rows.fold(0L) { total, row -> Math.addExact(total, row.expenseMinor) }
                val count = rows.size.toLong()
                val averageIncome = income / count
                val averageExpense = expense / count
                Projection(
                    currencyCode = currency.first,
                    currencyExponent = currency.second,
                    averageIncomeMinor = averageIncome,
                    averageExpenseMinor = averageExpense,
                    projectedNetMinor = Math.subtractExact(averageIncome, averageExpense),
                    monthsObserved = rows.size
                )
            }.sortedWith(compareBy(Projection::currencyCode, Projection::currencyExponent))
}

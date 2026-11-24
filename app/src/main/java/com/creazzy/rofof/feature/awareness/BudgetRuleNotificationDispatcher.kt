package com.creazzy.rofof.feature.awareness

import android.content.Context
import com.creazzy.rofof.data.local.BudgetAlertSnapshot
import com.creazzy.rofof.data.local.RofofDatabase
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId

/**
 * Evaluates the same bundled RuleEngine rules for saved budget snapshots.
 * The database remains the source of truth; notifications are only side effects.
 */
class BudgetRuleNotificationDispatcher(
    context: Context,
    private val database: RofofDatabase
) {
    private val appContext = context.applicationContext
    private val rules = RuleLoader().load(appContext).rules
    private val engine = RuleEngine()
    private val notifications = NotificationHelper(appContext)

    suspend fun dispatchForExpense(
        categoryId: String,
        currencyCode: String,
        currencyExponent: Int,
        occurredAtEpochMillis: Long,
        language: String
    ) {
        val snapshots = database.budgetGoalDao().findBudgetAlertsForExpense(
            categoryId, currencyCode, currencyExponent, occurredAtEpochMillis
        )
        snapshots.forEach { dispatch(it, language) }
    }

    suspend fun dispatchCurrentMonth() {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val language = database.localPreferenceDao().observe("language").first()?.value
            ?.takeIf { it == "ar" || it == "en" } ?: "ar"
        database.budgetGoalDao().observeBudgetsForPeriod(start).first().forEach { budget ->
            dispatch(
                BudgetAlertSnapshot(
                    id = budget.id,
                    categoryId = budget.categoryId,
                    amountMinor = budget.amountMinor,
                    currencyCode = budget.currencyCode,
                    currencyExponent = budget.currencyExponent,
                    periodStartEpochMillis = budget.periodStartEpochMillis,
                    periodEndEpochMillis = budget.periodEndEpochMillis,
                    spentMinor = budget.spentMinor
                ),
                language
            )
        }
    }

    private fun dispatch(snapshot: BudgetAlertSnapshot, language: String) {
        val key = "budget-rule:${snapshot.id}:${snapshot.periodStartEpochMillis}"
        if (snapshot.amountMinor <= 0L || snapshot.spentMinor <= 0L) {
            notifications.resetBudgetRule(key)
            return
        }
        val percent = BigDecimal.valueOf(snapshot.spentMinor)
            .multiply(BigDecimal("100"))
            .divide(BigDecimal.valueOf(snapshot.amountMinor), 2, RoundingMode.HALF_UP)
        val matches = engine.evaluate(rules, mapOf("budgetPercent" to percent))
        val match = matches.firstOrNull {
            it.action.type == "BUDGET_WARNING" || it.action.type == "BUDGET_EXCEEDED"
        }
        if (match == null) {
            notifications.resetBudgetRule(key)
            return
        }
        val exceeded = match.action.type == "BUDGET_EXCEEDED"
        val title = if (language == "ar") {
            if (exceeded) "تم استهلاك الميزانية بالكامل" else "اقتربت من حد الميزانية"
        } else {
            if (exceeded) "Budget limit reached" else "Budget warning"
        }
        val message = if (language == "ar") match.action.messageAr else match.action.messageEn
        notifications.showBudgetRule(key, if (exceeded) 2 else 1, title, message, language)
    }
}

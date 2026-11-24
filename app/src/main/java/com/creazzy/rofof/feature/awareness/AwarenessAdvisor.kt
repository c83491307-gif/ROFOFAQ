package com.creazzy.rofof.feature.awareness

import android.content.Context
import com.creazzy.rofof.data.local.LocalPreferenceEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.feature.smartentry.SmartEntryParser
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.util.Calendar

data class AwarenessEntryReview(
    val sourceText: String,
    val amountText: String?,
    val transactionType: String,
    val categoryId: String?,
    val categoryName: String?,
    val categoryLabel: String?,
    val categoryKnown: Boolean,
    val ruleMatches: List<AwarenessMatch>
)

/** Read-only advisor using bundled rules and the existing Room v8 ledger. */
class AwarenessAdvisor(context: Context, private val database: RofofDatabase) {
    private val config = RuleLoader().load(context.applicationContext)
    private val parser = SmartEntryParser()

    suspend fun review(text: String, accountId: String, occurredAt: Long, language: String): AwarenessEntryReview {
        val parsed = parser.parse(text)
        val account = database.accountDao().findById(accountId)?.takeIf { !it.isArchived }
            ?: error("Select an active account before analyzing this entry.")
        val type = parsed.transactionType
        val normalized = normalize(text)
        val mapping = config.keywords.asSequence()
            .filter { it.transactionType == type && normalized.contains(normalize(it.keyword)) }
            .maxByOrNull { normalize(it.keyword).length }
        val seed = mapping?.let { map -> config.categories.firstOrNull { it.id == map.categoryId } }
        val category = seed?.let { database.categoryDao().findById(it.id) }
            ?.takeIf { !it.isArchived && it.transactionType == type }
        val amountMinor = parsed.amountText?.let {
            BigDecimal(it.replace(',', '.')).setScale(account.currencyExponent, RoundingMode.UNNECESSARY)
                .movePointRight(account.currencyExponent).longValueExact()
        }?.takeIf { it > 0L }

        val calendar = Calendar.getInstance().apply { timeInMillis = occurredAt }
        val facts = mutableMapOf<String, Any?>(
            "categoryKnown" to (category != null),
            "transactionType" to type,
            "localHour" to calendar.get(Calendar.HOUR_OF_DAY),
            "isWeekend" to (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY),
            "isMonthEnd" to (calendar.get(Calendar.DAY_OF_MONTH) == calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        )
        category?.let { facts["categoryId"] = it.id }
        val balance = database.accountDao().observeAllBalances().first().firstOrNull { it.id == accountId }?.balanceMinor
            ?: account.openingBalanceMinor
        facts["balanceMinor"] = if (amountMinor == null) balance else
            BigDecimal.valueOf(balance).add(if (type == "EXPENSE") BigDecimal.valueOf(amountMinor).negate() else BigDecimal.valueOf(amountMinor)).longValueExact()

        if (category != null && amountMinor != null && type == "EXPENSE") {
            val budget = database.budgetGoalDao().findBudgetAlertsForExpense(category.id, account.currencyCode, account.currencyExponent, occurredAt).firstOrNull()
            if (budget != null && budget.amountMinor > 0L) {
                facts["budgetPercent"] = BigDecimal.valueOf(budget.spentMinor).add(BigDecimal.valueOf(amountMinor))
                    .multiply(BigDecimal("100")).divide(BigDecimal.valueOf(budget.amountMinor), 2, RoundingMode.HALF_UP)
            }
        }
        database.localPreferenceDao().observe("awareness.rent_due_epoch_millis").first()?.value?.toLongOrNull()?.let { due ->
            facts["daysUntilRent"] = Math.floorDiv(due - occurredAt, 24L * 60 * 60 * 1000)
        }
        val firstUse = database.localPreferenceDao().observe(FIRST_USE_KEY).first() == null
        facts["isFirstLaunch"] = firstUse
        val matches = RuleEngine().evaluate(config.rules, facts)
        if (firstUse) database.localPreferenceDao().put(LocalPreferenceEntity(FIRST_USE_KEY, "seen", System.currentTimeMillis()))

        val arabicLabels = mapOf(
            "Food" to "طعام", "Coffee" to "قهوة", "Transport" to "مواصلات", "Fuel" to "وقود",
            "Bills" to "فواتير", "Rent" to "إيجار", "Health" to "صحة", "Education" to "تعليم",
            "Shopping" to "تسوق", "Entertainment" to "ترفيه", "Family" to "عائلة",
            "Charity" to "صدقة", "Work" to "عمل", "Salary" to "راتب", "Freelance" to "عمل حر", "Gift" to "هدية"
        )
        return AwarenessEntryReview(text, parsed.amountText, type, category?.id, category?.name,
            category?.name?.let { if (language == "ar") arabicLabels[it] ?: it else it },
            category != null, matches)
    }

    private fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").replace('أ','ا').replace('إ','ا').replace('آ','ا')
        .replace('ى','ي').replace('ة','ه').replace('ـ',' ').lowercase()

    companion object { const val FIRST_USE_KEY = "awareness.first_use" }
}

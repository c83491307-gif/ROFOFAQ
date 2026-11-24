package com.creazzy.rofof.feature.awareness

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RuleEngineTest {
    private val engine = RuleEngine()
    private val budgetRule = AwarenessRule(
        id = "budget-80", enabled = true, priority = 10,
        conditions = listOf(
            AwarenessCondition("budgetPercent", "gte", 80),
            AwarenessCondition("budgetPercent", "lt", 100)
        ),
        actions = listOf(AwarenessAction("BUDGET_WARNING", "WARNING", "تنبيه", "Warning"))
    )

    @Test fun budgetWarningMatchesAtEightyButNotAtOneHundred() {
        assertEquals("budget-80", engine.evaluate(listOf(budgetRule), mapOf("budgetPercent" to 80)).single().ruleId)
        assertTrue(engine.evaluate(listOf(budgetRule), mapOf("budgetPercent" to 100)).isEmpty())
    }

    @Test fun disabledRulesAndMissingFactsDoNotFire() {
        val disabled = budgetRule.copy(enabled = false)
        assertTrue(engine.evaluate(listOf(disabled), mapOf("budgetPercent" to 90)).isEmpty())
        assertTrue(engine.evaluate(listOf(budgetRule), emptyMap()).isEmpty())
    }

    @Test fun rulesAreEvaluatedByPriorityAndEveryConditionMustMatch() {
        val high = budgetRule.copy(id = "high", priority = 20)
        val low = budgetRule.copy(id = "low", priority = 1)
        val results = engine.evaluate(listOf(low, high), mapOf("budgetPercent" to 90))
        assertEquals(listOf("high", "low"), results.map { it.ruleId })
        assertTrue(engine.evaluate(listOf(budgetRule), mapOf("budgetPercent" to 90, "accountActive" to false)).isNotEmpty())
    }

    @Test fun numericStringFactsAndBetweenOperatorAreSupported() {
        val rule = budgetRule.copy(
            conditions = listOf(AwarenessCondition("hour", "between", listOf(21, 23)))
        )
        assertEquals(1, engine.evaluate(listOf(rule), mapOf("hour" to "22")).size)
        assertTrue(engine.evaluate(listOf(rule), mapOf("hour" to 24)).isEmpty())
    }

    @Test fun unknownCategoryAndNegativeBalanceRulesWorkFromBundledRules() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rules = RuleLoader().load(context).rules
        val unknown = engine.evaluate(rules, mapOf("categoryKnown" to false))
        assertTrue(unknown.any { it.ruleId == "unknown-category" })
        val negative = engine.evaluate(rules, mapOf("balanceMinor" to -1L))
        assertTrue(negative.any { it.ruleId == "negative-balance" })
    }
}

package com.creazzy.rofof.feature.awareness

/**
 * Deterministic, offline rule evaluator. It only reads supplied facts and
 * returns suggestions; it never writes transactions or changes budgets.
 */
class RuleEngine {
    fun evaluate(
        rules: List<AwarenessRule>,
        facts: Map<String, Any?>
    ): List<AwarenessMatch> =
        rules.asSequence()
            .filter { it.enabled }
            .sortedWith(compareByDescending<AwarenessRule> { it.priority }.thenBy { it.id })
            .filter { rule -> rule.conditions.all { condition -> matches(condition, facts) } }
            .flatMap { rule -> rule.actions.asSequence().map { AwarenessMatch(rule.id, rule.priority, it) } }
            .toList()

    private fun matches(condition: AwarenessCondition, facts: Map<String, Any?>): Boolean {
        if (!facts.containsKey(condition.fact)) return condition.operator == "exists" && condition.value == false
        val actual = facts[condition.fact]
        val expected = condition.value
        return when (condition.operator) {
            "exists" -> (actual != null) == (expected != false)
            "eq" -> equivalent(actual, expected)
            "neq" -> !equivalent(actual, expected)
            "gt" -> compare(actual, expected)?.let { it > 0 } ?: false
            "gte" -> compare(actual, expected)?.let { it >= 0 } ?: false
            "lt" -> compare(actual, expected)?.let { it < 0 } ?: false
            "lte" -> compare(actual, expected)?.let { it <= 0 } ?: false
            "contains" -> actual?.toString()?.contains(expected?.toString().orEmpty(), ignoreCase = true) == true
            "between" -> {
                val bounds = expected as? List<*> ?: return false
                if (bounds.size != 2) return false
                val lower = compare(actual, bounds[0]) ?: return false
                val upper = compare(actual, bounds[1]) ?: return false
                lower >= 0 && upper <= 0
            }
            else -> false
        }
    }

    private fun equivalent(left: Any?, right: Any?): Boolean {
        val numeric = compare(left, right)
        if (numeric != null) return numeric == 0
        return left?.toString() == right?.toString()
    }

    private fun compare(left: Any?, right: Any?): Int? {
        val leftNumber = left.asFiniteDouble() ?: return null
        val rightNumber = right.asFiniteDouble() ?: return null
        return leftNumber.compareTo(rightNumber)
    }

    private fun Any?.asFiniteDouble(): Double? = when (this) {
        is Number -> toDouble().takeIf { it.isFinite() }
        is String -> trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        else -> null
    }
}

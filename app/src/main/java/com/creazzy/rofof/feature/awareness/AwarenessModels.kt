package com.creazzy.rofof.feature.awareness

data class SeedCategory(
    val id: String,
    val name: String,
    val transactionType: String
)

data class KeywordMapping(
    val keyword: String,
    val categoryId: String,
    val transactionType: String
)

data class AwarenessCondition(
    val fact: String,
    val operator: String,
    val value: Any? = null
)

data class AwarenessAction(
    val type: String,
    val severity: String,
    val messageAr: String,
    val messageEn: String
)

data class AwarenessRule(
    val id: String,
    val enabled: Boolean,
    val priority: Int,
    val conditions: List<AwarenessCondition>,
    val actions: List<AwarenessAction>
)

data class AwarenessConfiguration(
    val categories: List<SeedCategory>,
    val keywords: List<KeywordMapping>,
    val rules: List<AwarenessRule>
)

data class AwarenessMatch(
    val ruleId: String,
    val priority: Int,
    val action: AwarenessAction
)

package com.creazzy.rofof.feature.awareness

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class RuleLoader {
    fun load(context: Context): AwarenessConfiguration {
        val assets = context.assets
        return parse(
            categoriesJson = assets.open("awareness/categories.json").bufferedReader().use { it.readText() },
            keywordsJson = assets.open("awareness/keywords_ar.json").bufferedReader().use { it.readText() },
            rulesJson = assets.open("awareness/rules.json").bufferedReader().use { it.readText() }
        )
    }

    fun parse(
        categoriesJson: String,
        keywordsJson: String,
        rulesJson: String
    ): AwarenessConfiguration {
        try {
            val categories = parseCategories(JSONObject(categoriesJson).getJSONArray("categories"))
            val keywords = parseKeywords(JSONObject(keywordsJson).getJSONArray("keywords"))
            val rules = parseRules(JSONObject(rulesJson).getJSONArray("rules"))
            require(categories.map { it.id }.distinct().size == categories.size) {
                "Duplicate category ID in categories.json."
            }
            require(categories.map { it.name.lowercase() to it.transactionType }.distinct().size == categories.size) {
                "Duplicate category name/type in categories.json."
            }
            require(keywords.map { it.keyword }.distinct().size == keywords.size) {
                "Duplicate keyword in keywords_ar.json."
            }
            keywords.forEach { keyword ->
                val category = categories.firstOrNull { it.id == keyword.categoryId }
                    ?: error("Unknown category ID for keyword: ${keyword.keyword}")
                require(category.transactionType == keyword.transactionType) {
                    "Keyword type does not match its category: ${keyword.keyword}"
                }
            }
            require(rules.map { it.id }.distinct().size == rules.size) { "Duplicate rule ID in rules.json." }
            return AwarenessConfiguration(categories, keywords, rules.sortedWith(compareByDescending<AwarenessRule> { it.priority }.thenBy { it.id }))
        } catch (error: JSONException) {
            throw IllegalArgumentException("Invalid awareness JSON: ${error.message}", error)
        }
    }

    private fun parseCategories(array: JSONArray): List<SeedCategory> =
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val type = item.getString("transactionType")
            require(type == DatabaseSeeder.EXPENSE || type == DatabaseSeeder.INCOME) {
                "Invalid category transactionType at index $index."
            }
            SeedCategory(
                id = item.getString("id").trim().also { require(it.isNotEmpty()) },
                name = item.getString("name").trim().also { require(it.isNotEmpty()) },
                transactionType = type
            )
        }

    private fun parseKeywords(array: JSONArray): List<KeywordMapping> =
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val type = item.getString("transactionType")
            require(type == DatabaseSeeder.EXPENSE || type == DatabaseSeeder.INCOME) {
                "Invalid keyword transactionType at index $index."
            }
            KeywordMapping(
                keyword = item.getString("keyword").trim().also { require(it.isNotEmpty()) },
                categoryId = item.getString("categoryId").trim().also { require(it.isNotEmpty()) },
                transactionType = type
            )
        }

    private fun parseRules(array: JSONArray): List<AwarenessRule> =
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val conditions = item.getJSONArray("conditions").let { values ->
                (0 until values.length()).map { conditionIndex ->
                    val condition = values.getJSONObject(conditionIndex)
                    val operator = condition.getString("operator")
                    require(operator in SUPPORTED_OPERATORS) { "Unsupported rule operator: $operator" }
                    AwarenessCondition(
                        fact = condition.getString("fact").trim().also { require(it.isNotEmpty()) },
                        operator = operator,
                        value = parseJsonValue(condition.opt("value"))
                    )
                }
            }
            val actions = item.getJSONArray("actions").let { values ->
                (0 until values.length()).map { actionIndex ->
                    val action = values.getJSONObject(actionIndex)
                    val severity = action.getString("severity")
                    require(severity in SUPPORTED_SEVERITIES) { "Unsupported action severity: $severity" }
                    AwarenessAction(
                        type = action.getString("type").trim().also { require(it.isNotEmpty()) },
                        severity = severity,
                        messageAr = action.getString("messageAr").trim(),
                        messageEn = action.getString("messageEn").trim()
                    )
                }
            }
            require(conditions.isNotEmpty()) { "Rule at index $index has no conditions." }
            require(actions.isNotEmpty()) { "Rule at index $index has no actions." }
            AwarenessRule(
                id = item.getString("id").trim().also { require(it.isNotEmpty()) },
                enabled = item.optBoolean("enabled", true),
                priority = item.optInt("priority", 0),
                conditions = conditions,
                actions = actions
            )
        }

    private fun parseJsonValue(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONArray -> (0 until value.length()).map { parseJsonValue(value.opt(it)) }
        is JSONObject -> value.keys().asSequence().associateWith { key -> parseJsonValue(value.opt(key)) }
        else -> value
    }

    companion object {
        val SUPPORTED_OPERATORS = setOf("eq", "neq", "gt", "gte", "lt", "lte", "contains", "exists", "between")
        val SUPPORTED_SEVERITIES = setOf("INFO", "NOTICE", "WARNING", "CRITICAL")
    }
}

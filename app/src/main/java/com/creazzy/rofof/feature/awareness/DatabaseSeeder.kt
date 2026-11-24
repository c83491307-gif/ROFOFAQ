package com.creazzy.rofof.feature.awareness

import android.content.Context
import androidx.room.withTransaction
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.RofofDatabase

data class DatabaseSeedResult(
    val inserted: Int,
    val alreadyPresent: Int
)

/**
 * Seeds only the category catalog. It never inserts accounts, transactions,
 * budgets, goals, or other financial activity.
 */
class DatabaseSeeder(
    private val database: RofofDatabase
) {
    suspend fun seedFromAssets(context: Context, nowEpochMillis: Long = System.currentTimeMillis()): DatabaseSeedResult {
        val categories = RuleLoader().load(context).categories
        return seed(categories, nowEpochMillis)
    }

    suspend fun seed(categories: List<SeedCategory>, nowEpochMillis: Long): DatabaseSeedResult {
        require(categories.map { it.id }.distinct().size == categories.size) {
            "Seed category IDs must be unique."
        }
        require(categories.map { it.name.lowercase() to it.transactionType }.distinct().size == categories.size) {
            "Seed category names must be unique per transaction type."
        }
        categories.forEach {
            require(it.id.isNotBlank() && it.name.isNotBlank()) { "Seed category ID and name are required." }
            require(it.transactionType == EXPENSE || it.transactionType == INCOME) {
                "Unsupported category transaction type: ${it.transactionType}"
            }
        }

        var inserted = 0
        var alreadyPresent = 0
        database.withTransaction {
            val dao = database.categoryDao()
            categories.forEach { seed ->
                if (dao.findByNameAndType(seed.name, seed.transactionType) != null) {
                    alreadyPresent++
                } else {
                    dao.insertIfAbsent(
                        CategoryEntity(
                            id = seed.id,
                            name = seed.name,
                            transactionType = seed.transactionType,
                            parentId = null,
                            isArchived = false,
                            createdAtEpochMillis = nowEpochMillis
                        )
                    )
                    if (dao.findByNameAndType(seed.name, seed.transactionType) != null) inserted++
                    else alreadyPresent++
                }
            }
        }
        return DatabaseSeedResult(inserted, alreadyPresent)
    }

    companion object {
        const val EXPENSE = "EXPENSE"
        const val INCOME = "INCOME"
    }
}

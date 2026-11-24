package com.creazzy.rofof.feature.awareness

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
                val existing = dao.findByNameAndType(seed.name, seed.transactionType)
                if (existing != null) {
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

        val DEFAULT_CATEGORIES = listOf(
            SeedCategory("awareness-expense-food", "Food", EXPENSE),
            SeedCategory("awareness-expense-coffee", "Coffee", EXPENSE),
            SeedCategory("awareness-expense-transport", "Transport", EXPENSE),
            SeedCategory("awareness-expense-fuel", "Fuel", EXPENSE),
            SeedCategory("awareness-expense-bills", "Bills", EXPENSE),
            SeedCategory("awareness-expense-rent", "Rent", EXPENSE),
            SeedCategory("awareness-expense-health", "Health", EXPENSE),
            SeedCategory("awareness-expense-education", "Education", EXPENSE),
            SeedCategory("awareness-expense-shopping", "Shopping", EXPENSE),
            SeedCategory("awareness-expense-entertainment", "Entertainment", EXPENSE),
            SeedCategory("awareness-expense-family", "Family", EXPENSE),
            SeedCategory("awareness-expense-charity", "Charity", EXPENSE),
            SeedCategory("awareness-expense-work", "Work", EXPENSE),
            SeedCategory("awareness-income-salary", "Salary", INCOME),
            SeedCategory("awareness-income-freelance", "Freelance", INCOME),
            SeedCategory("awareness-income-gift", "Gift", INCOME)
        )
    }
}

package com.creazzy.rofof.data.importing

import com.creazzy.rofof.data.local.AccountDao
import com.creazzy.rofof.data.local.CategoryDao
import com.creazzy.rofof.data.local.TransactionDao

/**
 * Validates every foreign reference before the single atomic Room batch insert.
 * Call only after the user has reviewed and explicitly confirmed the preview.
 */
class TransactionCsvImportService(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) {
    data class Result(val importedCount: Int, val errors: List<String>) {
        val succeeded: Boolean get() = errors.isEmpty()
    }

    suspend fun importConfirmed(csv: String): Result {
        val parsed = TransactionCsvImporter.parse(csv)
        if (!parsed.isValid) {
            return Result(0, parsed.errors.map { "Row ${it.row}: ${it.message}" })
        }
        val validationErrors = mutableListOf<String>()
        parsed.transactions.forEachIndexed { index, transaction ->
            val row = index + 2
            if (transactionDao.findById(transaction.id) != null) {
                validationErrors += "Row $row: transaction ID already exists"
            }
            val account = accountDao.findById(transaction.accountId)
            if (account == null || account.isArchived ||
                account.currencyCode != transaction.currencyCode ||
                account.currencyExponent != transaction.currencyExponent
            ) {
                validationErrors += "Row $row: account is missing, archived, or has a different currency"
            }
            val category = categoryDao.findById(transaction.categoryId)
            if (category == null || category.isArchived ||
                category.transactionType != transaction.transactionType
            ) {
                validationErrors += "Row $row: category is missing, archived, or has a different transaction type"
            }
        }
        if (validationErrors.isNotEmpty()) return Result(0, validationErrors)
        return runCatching {
            transactionDao.insertAll(parsed.transactions)
            Result(parsed.transactions.size, emptyList())
        }.getOrElse {
            Result(0, listOf("Import was rolled back: ${it.message ?: "database constraint failed"}"))
        }
    }
}

package com.creazzy.rofof.data.repository

import androidx.room.withTransaction
import com.creazzy.rofof.data.local.AccountDao
import com.creazzy.rofof.data.local.AccountEntity
import com.creazzy.rofof.data.local.CategoryDao
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.local.TransferDao
import com.creazzy.rofof.data.local.TransferEntity
import kotlinx.coroutines.flow.Flow
import java.util.Currency
import java.util.UUID

class RoomFinanceRepository(
    private val database: RofofDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val categoryDao: CategoryDao = database.categoryDao(),
    private val transferDao: TransferDao = database.transferDao()
) {
    fun observeAccounts() = accountDao.observeBalances()
    fun observeCategories() = categoryDao.observeActive()
    fun observeTransfers(): Flow<List<TransferEntity>> = transferDao.observeRecent()

    suspend fun createAccount(
        name: String,
        currencyCode: String,
        currencyExponent: Int,
        openingBalanceMinor: Long
    ): String {
        val cleanName = name.trim()
        val code = currencyCode.trim().uppercase()
        require(cleanName.isNotEmpty()) { "Account name is required" }
        require(code.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter ISO code" }
        require(currencyExponent in 0..4) { "Unsupported currency exponent" }
        val isoCurrency = runCatching { Currency.getInstance(code) }.getOrNull()
            ?: error("Unknown ISO currency code")
        require(isoCurrency.defaultFractionDigits == currencyExponent) {
            "Currency exponent does not match ISO currency metadata"
        }
        val id = UUID.randomUUID().toString()
        accountDao.insert(
            AccountEntity(
                id = id,
                name = cleanName,
                currencyCode = code,
                currencyExponent = currencyExponent,
                openingBalanceMinor = openingBalanceMinor,
                isArchived = false,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun renameAccount(id: String, name: String) {
        require(name.isNotBlank()) { "Account name is required" }
        require(accountDao.findById(id)?.isArchived == false) { "Active account not found" }
        accountDao.rename(id, name.trim())
    }

    suspend fun archiveAccount(id: String) {
        require(id != RoomTransactionRepository.DEFAULT_ACCOUNT_ID) { "Default account cannot be archived" }
        val account = accountDao.findById(id) ?: error("Account not found")
        require(!account.isArchived) { "Account already archived" }
        val hasTransactions = database.transactionDao().hasTransactionsForAccount(id)
        val hasTransfers = database.transferDao().hasTransfersForAccount(id)
        require(!hasTransactions && !hasTransfers) {
            "Move or remove account activity before archiving this account"
        }
        accountDao.archive(id)
    }

    suspend fun createCategory(name: String, type: String, parentId: String? = null): String {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Category name is required" }
        require(type == "INCOME" || type == "EXPENSE") { "Invalid category type" }
        if (parentId != null) {
            val parent = categoryDao.findById(parentId) ?: error("Parent category not found")
            require(!parent.isArchived) { "Parent category is archived" }
            require(parent.parentId == null) { "Only two category levels are supported" }
            require(parent.transactionType == type) { "Parent and child category types must match" }
        }
        val id = UUID.randomUUID().toString()
        categoryDao.insert(
            CategoryEntity(
                id = id,
                name = cleanName,
                transactionType = type,
                parentId = parentId,
                isArchived = false,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun renameCategory(id: String, name: String) {
        require(name.isNotBlank()) { "Category name is required" }
        require(categoryDao.findById(id)?.isArchived == false) { "Active category not found" }
        categoryDao.rename(id, name.trim())
    }

    suspend fun archiveCategory(id: String) {
        val category = categoryDao.findById(id) ?: error("Category not found")
        require(!category.isArchived) { "Category already archived" }
        require(categoryDao.countChildren(id) == 0) { "Archive child categories first" }
        require(database.transactionDao().countForCategory(id) == 0) {
            "Category has transactions; reassign them before archiving"
        }
        categoryDao.archive(id)
    }

    suspend fun transfer(
        fromAccountId: String,
        toAccountId: String,
        amountMinor: Long,
        note: String? = null
    ): String = database.withTransaction {
        require(fromAccountId != toAccountId) { "Choose two different accounts" }
        require(amountMinor > 0L) { "Transfer amount must be positive" }
        val source = accountDao.findById(fromAccountId) ?: error("Source account not found")
        val destination = accountDao.findById(toAccountId) ?: error("Destination account not found")
        require(!source.isArchived && !destination.isArchived) { "Archived accounts cannot transfer" }
        require(
            source.currencyCode == destination.currencyCode &&
                source.currencyExponent == destination.currencyExponent
        ) { "Transfers require matching currencies; exchange is not supported" }
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        transferDao.insert(
            TransferEntity(
                id = id,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amountMinor = amountMinor,
                currencyCode = source.currencyCode,
                currencyExponent = source.currencyExponent,
                occurredAtEpochMillis = now,
                createdAtEpochMillis = now,
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
        id
    }
}

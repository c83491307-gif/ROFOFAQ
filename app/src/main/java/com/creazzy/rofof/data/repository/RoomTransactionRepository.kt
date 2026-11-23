package com.creazzy.rofof.data.repository

import com.creazzy.rofof.data.local.AccountDao
import com.creazzy.rofof.data.local.AccountEntity
import com.creazzy.rofof.data.local.CategoryDao
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.TransactionDao
import com.creazzy.rofof.data.local.TransactionEntity
import com.creazzy.rofof.domain.model.LedgerTransaction
import com.creazzy.rofof.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {
    override fun observeRecent(): Flow<List<LedgerTransaction>> =
        transactionDao.observeRecent().map { records -> records.map { it.toDomain() } }

    override suspend fun add(transaction: LedgerTransaction) {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun delete(id: String) = transactionDao.delete(id)

    suspend fun ensureDefaults() {
        accountDao.insertIfAbsent(
            AccountEntity(
                id = DEFAULT_ACCOUNT_ID,
                name = "Cash",
                currencyCode = "EGP",
                currencyExponent = 2,
                openingBalanceMinor = 0L,
                isArchived = false,
                createdAtEpochMillis = 0L
            )
        )
        categoryDao.insertIfAbsent(
            CategoryEntity(
                id = DEFAULT_EXPENSE_CATEGORY_ID,
                name = "General",
                transactionType = EXPENSE,
                parentId = null,
                isArchived = false,
                createdAtEpochMillis = 0L
            )
        )
        categoryDao.insertIfAbsent(
            CategoryEntity(
                id = DEFAULT_INCOME_CATEGORY_ID,
                name = "General",
                transactionType = INCOME,
                parentId = null,
                isArchived = false,
                createdAtEpochMillis = 0L
            )
        )
    }

    private fun TransactionEntity.toDomain() = LedgerTransaction(
        id, title, transactionType, amountMinor, currencyCode, currencyExponent,
        accountId, categoryId, occurredAtEpochMillis, createdAtEpochMillis, notes
    )

    private fun LedgerTransaction.toEntity() = TransactionEntity(
        id, title, transactionType, amountMinor, currencyCode, currencyExponent,
        accountId, categoryId, occurredAtEpochMillis, createdAtEpochMillis, notes
    )

    companion object {
        const val DEFAULT_ACCOUNT_ID = "default-cash"
        const val DEFAULT_EXPENSE_CATEGORY_ID = "default-expense-general"
        const val DEFAULT_INCOME_CATEGORY_ID = "default-income-general"
        const val EXPENSE = "EXPENSE"
        const val INCOME = "INCOME"
    }
}

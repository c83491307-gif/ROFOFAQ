package com.creazzy.rofof.data.repository

import com.creazzy.rofof.data.local.DebtBalance
import com.creazzy.rofof.data.local.DebtEntity
import com.creazzy.rofof.data.local.DebtRepaymentEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DebtManagementRepository(private val database: RofofDatabase) {
    private val debtDao = database.debtDao()

    fun observeBalances(): Flow<List<DebtBalance>> = debtDao.observeBalances()

    suspend fun createDebt(
        title: String,
        direction: String,
        amountMinor: Long,
        currencyCode: String,
        currencyExponent: Int,
        dueAtEpochMillis: Long? = null
    ): String {
        require(title.isNotBlank()) { "Person or debt title is required" }
        require(direction == "OWED_TO_ME" || direction == "OWED_BY_ME") { "Invalid debt direction" }
        require(amountMinor > 0L) { "Debt amount must be positive" }
        require(currencyCode.matches(Regex("[A-Z]{3}")) && currencyExponent in 0..4) {
            "Invalid debt currency"
        }
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        debtDao.insertDebt(
            DebtEntity(
                id = id,
                title = title.trim(),
                direction = direction,
                principalMinor = amountMinor,
                currencyCode = currencyCode,
                currencyExponent = currencyExponent,
                annualRateBasisPoints = 0,
                dueAtEpochMillis = dueAtEpochMillis,
                status = "OPEN",
                note = null,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        )
        return id
    }

    suspend fun settle(debtId: String): DebtBalance {
        val debt = requireNotNull(debtDao.findById(debtId)) { "Debt not found" }
        require(debt.status == "OPEN") { "Debt is already settled" }
        val current = requireNotNull(debtDao.getBalance(debtId)) { "Debt balance not found" }
        require(current.outstandingMinor > 0L) { "Debt is already settled" }

        val account = database.accountDao().findActiveAccountForCurrency(
            debt.currencyCode, debt.currencyExponent
        ) ?: error("No active account matches this debt currency")
        val transactionType = if (debt.direction == "OWED_TO_ME") "INCOME" else "EXPENSE"
        if (transactionType == "EXPENSE") {
            val balance = database.accountDao().findBalance(account.id)?.balanceMinor
                ?: error("Account balance could not be read")
            require(balance >= current.outstandingMinor) { "Insufficient account balance to settle this debt" }
        }
        val categoryName = if (transactionType == "INCOME") "Debt repayment received" else "Debt repayment paid"
        val categoryId = database.categoryDao().findByNameAndType(categoryName, transactionType)?.id
            ?: UUID.randomUUID().toString().also { id ->
                database.categoryDao().insertIfAbsent(
                    com.creazzy.rofof.data.local.CategoryEntity(
                        id = id,
                        name = categoryName,
                        transactionType = transactionType,
                        parentId = null,
                        isArchived = false,
                        createdAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }.let {
                database.categoryDao().findByNameAndType(categoryName, transactionType)?.id ?: it
            }
        val now = System.currentTimeMillis()
        val amount = current.outstandingMinor
        val transaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            title = if (transactionType == "INCOME") "Debt received: ${debt.title}" else "Debt paid: ${debt.title}",
            transactionType = transactionType,
            amountMinor = amount,
            currencyCode = debt.currencyCode,
            currencyExponent = debt.currencyExponent,
            accountId = account.id,
            categoryId = categoryId,
            occurredAtEpochMillis = now,
            createdAtEpochMillis = now,
            notes = "Debt settlement · ${debt.id}"
        )
        val repayment = DebtRepaymentEntity(
            id = UUID.randomUUID().toString(),
            debtId = debt.id,
            amountMinor = amount,
            paidAtEpochMillis = now,
            note = "Full settlement",
            createdAtEpochMillis = now
        )
        return debtDao.recordRepaymentWithLedger(repayment, transaction)
    }
}

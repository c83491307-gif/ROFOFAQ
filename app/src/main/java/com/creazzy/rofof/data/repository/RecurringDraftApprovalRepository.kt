package com.creazzy.rofof.data.repository

import androidx.room.withTransaction
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.local.TransactionEntity

class RecurringDraftApprovalRepository(private val database: RofofDatabase) {
    suspend fun approve(draftId: String, approvedAtEpochMillis: Long): String =
        database.withTransaction {
            val draft = database.recurringDao().findDraftById(draftId)
                ?: error("Recurring draft not found")
            require(draft.status == "DRAFT") { "Only pending drafts can be approved" }
            require(draft.amountMinor > 0L) { "Draft amount must be positive" }
            require(draft.transactionType == "INCOME" || draft.transactionType == "EXPENSE") {
                "Unsupported recurring transaction type"
            }

            val account = database.accountDao().findById(draft.accountId)
                ?: error("Draft account no longer exists")
            require(!account.isArchived) { "Draft account is archived" }
            require(account.currencyCode == draft.currencyCode &&
                account.currencyExponent == draft.currencyExponent) {
                "Draft currency no longer matches its account"
            }

            val category = database.categoryDao().findById(draft.categoryId)
                ?: error("Draft category no longer exists")
            require(!category.isArchived && category.transactionType == draft.transactionType) {
                "Draft category is archived or has a different type"
            }

            val transactionId = "recurring-draft:$draftId"
            database.transactionDao().insert(
                TransactionEntity(
                    id = transactionId,
                    title = draft.title,
                    transactionType = draft.transactionType,
                    amountMinor = draft.amountMinor,
                    currencyCode = draft.currencyCode,
                    currencyExponent = draft.currencyExponent,
                    accountId = draft.accountId,
                    categoryId = draft.categoryId,
                    occurredAtEpochMillis = draft.scheduledForEpochMillis,
                    createdAtEpochMillis = approvedAtEpochMillis,
                    notes = "Created from recurring rule draft $draftId"
                )
            )
            check(database.recurringDao().updateDraftStatus(draftId, "POSTED") == 1) {
                "Draft status changed during approval"
            }
            transactionId
        }

    suspend fun skip(draftId: String): Boolean =
        database.recurringDao().updateDraftStatus(draftId, "SKIPPED") == 1
}

package com.creazzy.rofof.data.repository

import androidx.room.withTransaction
import com.creazzy.rofof.core.money.PurchaseMath
import com.creazzy.rofof.data.local.*
import java.util.UUID

class RoomPurchaseRepository(private val database: RofofDatabase) {
    private val dao = database.purchaseDao()

    fun observeSessions() = dao.observeSessions()
    fun observeParticipants(sessionId: String) = dao.observeParticipants(sessionId)
    fun observeItems(sessionId: String) = dao.observeItems(sessionId)
    fun observeSettlements(sessionId: String) = dao.observeSettlements(sessionId)

    suspend fun createSession(title: String, currencyCode: String, currencyExponent: Int, notes: String? = null): String {
        val cleanTitle = title.trim()
        val code = currencyCode.trim().uppercase()
        require(cleanTitle.isNotEmpty()) { "Purchase title is required" }
        require(code.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter ISO code" }
        require(currencyExponent in 0..4) { "Unsupported currency exponent" }
        val currency = runCatching { java.util.Currency.getInstance(code) }.getOrNull()
            ?: error("Unknown ISO currency code")
        require(currency.defaultFractionDigits == currencyExponent) { "Currency exponent does not match ISO metadata" }
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        dao.insertSession(PurchaseSessionEntity(id, cleanTitle, notes?.trim()?.takeIf { it.isNotEmpty() },
            code, currencyExponent, OPEN, now, now))
        return id
    }

    suspend fun addParticipant(sessionId: String, displayName: String, note: String? = null): String =
        database.withTransaction {
            val session = dao.findSession(sessionId) ?: error("Purchase session not found")
            require(session.status == OPEN) { "Closed purchase sessions cannot be changed" }
            val cleanName = displayName.trim()
            require(cleanName.isNotEmpty()) { "Participant name is required" }
            val id = UUID.randomUUID().toString()
            dao.insertParticipant(PurchaseParticipantEntity(id, sessionId, cleanName,
                note?.trim()?.takeIf { it.isNotEmpty() }, false, System.currentTimeMillis()))
            id
        }

    suspend fun renameParticipant(sessionId: String, participantId: String, displayName: String) = database.withTransaction {
        val session = dao.findSession(sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Closed purchase sessions cannot be changed" }
        val participant = dao.findParticipant(participantId) ?: error("Participant not found")
        require(participant.sessionId == sessionId && !participant.isArchived) { "Participant does not belong to this active purchase" }
        val cleanName = displayName.trim()
        require(cleanName.isNotEmpty()) { "Participant name is required" }
        require(dao.renameParticipant(sessionId, participantId, cleanName) == 1) { "Participant could not be renamed" }
    }

    suspend fun archiveParticipant(sessionId: String, participantId: String) = database.withTransaction {
        val session = dao.findSession(sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Closed purchase sessions cannot be changed" }
        val participant = dao.findParticipant(participantId) ?: error("Participant not found")
        require(participant.sessionId == sessionId && !participant.isArchived) { "Participant does not belong to this active purchase" }
        require(dao.allocationCount(participantId) == 0 && dao.paymentCount(participantId) == 0) {
            "A participant with allocations or payments cannot be archived; financial history must remain visible"
        }
        require(dao.archiveParticipant(sessionId, participantId) == 1) { "Participant could not be archived" }
    }

    suspend fun updateItem(
        itemId: String,
        title: String,
        quantityMilli: Long,
        unitPriceMinor: Long,
        note: String? = null
    ) = database.withTransaction {
        val item = dao.findItem(itemId) ?: error("Purchase item not found")
        val session = dao.findSession(item.sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Closed purchase sessions cannot be changed" }
        require(dao.allocationCountForItem(itemId) == 0) { "Allocated items cannot be edited; allocation history must remain intact" }
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Item title is required" }
        val total = PurchaseMath.lineTotalMinor(quantityMilli, unitPriceMinor)
        require(dao.updateItem(item.copy(
            title = cleanTitle,
            quantityMilli = quantityMilli,
            unitPriceMinor = unitPriceMinor,
            totalMinor = total,
            note = note?.trim()?.takeIf { it.isNotEmpty() }
        )) == 1) { "Purchase item could not be updated" }
    }

    suspend fun deleteUnallocatedItem(itemId: String) = database.withTransaction {
        val item = dao.findItem(itemId) ?: error("Purchase item not found")
        val session = dao.findSession(item.sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Closed purchase sessions cannot be changed" }
        require(dao.allocationCountForItem(itemId) == 0) { "Allocated items cannot be deleted; financial history must remain intact" }
        require(dao.deleteItem(itemId) == 1) { "Purchase item could not be deleted" }
    }

    suspend fun addItem(
        sessionId: String,
        title: String,
        quantityMilli: Long,
        unitPriceMinor: Long,
        note: String? = null
    ): String = database.withTransaction {
        val session = dao.findSession(sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Closed purchase sessions cannot be changed" }
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Item title is required" }
        val total = PurchaseMath.lineTotalMinor(quantityMilli, unitPriceMinor)
        val id = UUID.randomUUID().toString()
        dao.insertItem(PurchaseItemEntity(id, sessionId, cleanTitle, quantityMilli,
            unitPriceMinor, total, note?.trim()?.takeIf { it.isNotEmpty() }, System.currentTimeMillis()))
        id
    }

    suspend fun allocateItem(itemId: String, sharesMinor: Map<String, Long>) = database.withTransaction {
        val item = dao.findItem(itemId) ?: error("Purchase item not found")
        val session = dao.findSession(item.sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Closed purchase sessions cannot be changed" }
        require(dao.allocationsForItem(itemId).isEmpty()) { "Item has already been allocated" }
        PurchaseMath.requireExactAllocation(item.totalMinor, sharesMinor)
        val participants = dao.participants(session.id).associateBy { it.id }
        require(sharesMinor.keys.all { participants.containsKey(it) }) {
            "Every allocation must belong to an active participant in this purchase"
        }
        val now = System.currentTimeMillis()
        dao.insertAllocations(sharesMinor.map { (participantId, amount) ->
            PurchaseAllocationEntity(UUID.randomUUID().toString(), itemId, participantId, amount, now)
        })
    }

    suspend fun recordPayment(
        sessionId: String,
        participantId: String,
        amountMinor: Long,
        paymentMethod: String,
        idempotencyKey: String,
        note: String? = null,
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ): String = database.withTransaction {
        val session = dao.findSession(sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Closed purchase sessions cannot receive payments" }
        require(amountMinor > 0L) { "Payment must be positive" }
        require(paymentMethod.isNotBlank()) { "Payment method is required" }
        require(idempotencyKey.isNotBlank()) { "Payment idempotency key is required" }
        require(dao.findPaymentByIdempotencyKey(idempotencyKey) == null) {
            "This payment has already been registered"
        }
        val participant = dao.findParticipant(participantId) ?: error("Participant not found")
        require(participant.sessionId == sessionId && !participant.isArchived) {
            "Participant does not belong to this active purchase"
        }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.insertPayment(PurchasePaymentEntity(id, sessionId, participantId, amountMinor,
            paymentMethod.trim(), idempotencyKey.trim(), note?.trim()?.takeIf { it.isNotEmpty() },
            occurredAtEpochMillis, now))
        id
    }

    suspend fun closeAsSettled(sessionId: String) = database.withTransaction {
        val session = dao.findSession(sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Purchase session is already closed" }
        val items = dao.items(sessionId)
        require(items.isNotEmpty()) { "A purchase needs at least one item" }
        for (item in items) {
            val allocations = dao.allocationsForItem(item.id)
            PurchaseMath.requireExactAllocation(item.totalMinor, allocations.associate { it.participantId to it.amountMinor })
        }
        // Read a consistent snapshot before changing the session state.
        val rows = dao.settlementSnapshot(sessionId)
        require(rows.all { it.allocatedMinor <= it.paidMinor }) {
            "All participant balances must be settled before closing"
        }
        dao.updateStatus(sessionId, SETTLED, System.currentTimeMillis())
    }

    /**
     * Records the current user's share as an expense and converts every other participant's
     * unpaid share into one receivable debt. All ledger writes and links are atomic.
     */
    suspend fun recordPayerSplitAsPersonalExpense(
        sessionId: String,
        payerParticipantId: String,
        accountId: String,
        categoryId: String
    ): List<String> = database.withTransaction {
        val session = dao.findSession(sessionId) ?: error("Purchase session not found")
        require(session.status == OPEN) { "Closed purchase sessions cannot be changed" }
        require(dao.debtLinkCountForSession(sessionId) == 0) { "This purchase was already converted to debts" }
        val payer = dao.findParticipant(payerParticipantId) ?: error("Payer participant not found")
        require(payer.sessionId == sessionId && !payer.isArchived) { "Payer must be an active participant in this purchase" }

        val account = database.accountDao().findById(accountId) ?: error("Account not found")
        require(!account.isArchived) { "Archived accounts cannot be used" }
        require(account.currencyCode == session.currencyCode && account.currencyExponent == session.currencyExponent) {
            "Personal expense account currency must match the shared purchase currency"
        }
        val category = database.categoryDao().findById(categoryId) ?: error("Expense category not found")
        require(!category.isArchived && category.transactionType == RoomTransactionRepository.EXPENSE) {
            "Choose an active expense category"
        }

        val items = dao.items(sessionId)
        require(items.isNotEmpty()) { "A shared purchase needs at least one item" }
        val allAllocations = items.flatMap { item ->
            val allocations = dao.allocationsForItem(item.id)
            PurchaseMath.requireExactAllocation(item.totalMinor, allocations.associate { it.participantId to it.amountMinor })
            allocations
        }
        require(allAllocations.isNotEmpty()) { "Allocate the bill before converting it" }
        require(allAllocations.none { dao.findExpenseLink(it.id) != null || dao.findDebtLink(it.id) != null }) {
            "One or more shares have already been converted"
        }

        val participantRows = dao.settlementSnapshot(sessionId).associateBy { it.participantId }
        require(participantRows.values.all { it.balanceMinor >= 0L }) {
            "Recorded payments exceed one or more participant shares"
        }
        val payerAllocations = allAllocations.filter { it.participantId == payerParticipantId }
        require(payerAllocations.isNotEmpty()) { "The payer must have an allocated share" }
        require(participantRows[payerParticipantId]?.paidMinor == 0L) {
            "The payer already has recorded payments; resolve them before converting the bill"
        }

        val now = System.currentTimeMillis()
        val createdTransactions = mutableListOf<String>()
        for (allocation in payerAllocations) {
            val item = requireNotNull(dao.findItem(allocation.itemId))
            val transactionId = UUID.randomUUID().toString()
            database.transactionDao().insert(
                TransactionEntity(
                    id = transactionId,
                    title = item.title,
                    transactionType = RoomTransactionRepository.EXPENSE,
                    amountMinor = allocation.amountMinor,
                    currencyCode = session.currencyCode,
                    currencyExponent = session.currencyExponent,
                    accountId = accountId,
                    categoryId = categoryId,
                    occurredAtEpochMillis = now,
                    createdAtEpochMillis = now,
                    notes = "Shared purchase payer share · ${session.title}"
                )
            )
            dao.insertExpenseLink(AllocationPersonalExpenseLinkEntity(
                UUID.randomUUID().toString(), allocation.id, transactionId, now
            ))
            createdTransactions += transactionId
        }

        // Record the payer's own allocated share as internally settled; the ledger expense
        // above is the only cash-flow entry created for that share.
        val payerShareMinor = payerAllocations.fold(0L) { sum, row -> Math.addExact(sum, row.amountMinor) }
        if (payerShareMinor > 0L) {
            dao.insertPayment(PurchasePaymentEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                participantId = payerParticipantId,
                amountMinor = payerShareMinor,
                paymentMethod = "SPLIT_SELF_SHARE",
                idempotencyKey = "split-self-share:$sessionId",
                note = "Payer's own allocated share",
                occurredAtEpochMillis = now,
                createdAtEpochMillis = now
            ))
        }

        val debtLinks = mutableListOf<PurchaseAllocationDebtLinkEntity>()
        val participants = dao.participants(sessionId)
        for (participant in participants.filter { it.id != payerParticipantId }) {
            val row = participantRows[participant.id] ?: continue
            val outstanding = row.balanceMinor
            if (outstanding <= 0L) continue
            val debtId = UUID.randomUUID().toString()
            dao.insertDebtRecord(
                DebtEntity(
                    id = debtId,
                    title = "${participant.displayName} · ${session.title}",
                    direction = "OWED_TO_ME",
                    principalMinor = outstanding,
                    currencyCode = session.currencyCode,
                    currencyExponent = session.currencyExponent,
                    annualRateBasisPoints = 0,
                    dueAtEpochMillis = null,
                    status = "OPEN",
                    note = "Shared purchase receivable · session:$sessionId",
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now
                )
            )
            allAllocations.filter { it.participantId == participant.id }.forEach { allocation ->
                debtLinks += PurchaseAllocationDebtLinkEntity(
                    id = UUID.randomUUID().toString(),
                    allocationId = allocation.id,
                    debtId = debtId,
                    createdAtEpochMillis = now
                )
            }
        }
        if (debtLinks.isNotEmpty()) dao.insertDebtLinks(debtLinks)
        createdTransactions
    }

    suspend fun recordAllocationAsPersonalExpense(
        allocationId: String,
        accountId: String,
        categoryId: String,
        title: String? = null
    ): String = database.withTransaction {
        val allocation = dao.findAllocation(allocationId) ?: error("Allocation not found")
        require(dao.findExpenseLink(allocationId) == null) { "This allocation is already in personal expenses" }
        val item = dao.findItem(allocation.itemId) ?: error("Purchase item not found")
        val session = dao.findSession(item.sessionId) ?: error("Purchase session not found")
        val account = database.accountDao().findById(accountId) ?: error("Account not found")
        require(!account.isArchived) { "Archived accounts cannot be used" }
        require(account.currencyCode == session.currencyCode && account.currencyExponent == session.currencyExponent) {
            "Personal expense account currency must match the shared purchase currency"
        }
        val category = database.categoryDao().findById(categoryId) ?: error("Expense category not found")
        require(!category.isArchived && category.transactionType == RoomTransactionRepository.EXPENSE) {
            "Choose an active expense category"
        }
        require(allocation.amountMinor > 0L) { "Zero-value allocations cannot become personal expenses" }
        val now = System.currentTimeMillis()
        val transactionId = UUID.randomUUID().toString()
        database.transactionDao().insert(
            TransactionEntity(
                id = transactionId,
                title = title?.trim()?.takeIf { it.isNotEmpty() } ?: item.title,
                transactionType = RoomTransactionRepository.EXPENSE,
                amountMinor = allocation.amountMinor,
                currencyCode = session.currencyCode,
                currencyExponent = session.currencyExponent,
                accountId = accountId,
                categoryId = categoryId,
                occurredAtEpochMillis = now,
                createdAtEpochMillis = now,
                notes = "Shared purchase allocation: ${session.title}"
            )
        )
        dao.insertExpenseLink(AllocationPersonalExpenseLinkEntity(
            UUID.randomUUID().toString(), allocationId, transactionId, now
        ))
        transactionId
    }

    companion object {
        const val OPEN = "OPEN"
        const val SETTLED = "SETTLED"
    }
}

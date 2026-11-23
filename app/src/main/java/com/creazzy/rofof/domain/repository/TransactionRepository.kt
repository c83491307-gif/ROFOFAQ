package com.creazzy.rofof.domain.repository

import com.creazzy.rofof.domain.model.LedgerTransaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeRecent(): Flow<List<LedgerTransaction>>
    suspend fun add(transaction: LedgerTransaction)
    suspend fun delete(id: String)
}

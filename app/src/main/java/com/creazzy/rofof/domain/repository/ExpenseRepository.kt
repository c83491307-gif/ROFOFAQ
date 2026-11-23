package com.creazzy.rofof.domain.repository

import com.creazzy.rofof.data.local.ExpenseEntity
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeRecent(): Flow<List<ExpenseEntity>>
    suspend fun add(expense: ExpenseEntity)
    suspend fun delete(id: String)
}

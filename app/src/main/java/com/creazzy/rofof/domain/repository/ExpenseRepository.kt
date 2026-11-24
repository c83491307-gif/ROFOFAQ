package com.creazzy.rofof.domain.repository

import com.creazzy.rofof.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeRecent(): Flow<List<Expense>>
    suspend fun add(expense: Expense)
    suspend fun delete(id: String)
}

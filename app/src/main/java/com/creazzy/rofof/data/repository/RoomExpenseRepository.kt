package com.creazzy.rofof.data.repository

import com.creazzy.rofof.data.local.ExpenseDao
import com.creazzy.rofof.data.local.ExpenseEntity
import com.creazzy.rofof.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class RoomExpenseRepository(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    override fun observeRecent(): Flow<List<ExpenseEntity>> = expenseDao.observeRecent()
    override suspend fun add(expense: ExpenseEntity) = expenseDao.insert(expense)
    override suspend fun delete(id: String) = expenseDao.delete(id)
}

package com.creazzy.rofof.data.repository

import com.creazzy.rofof.data.local.ExpenseDao
import com.creazzy.rofof.data.local.ExpenseEntity
import com.creazzy.rofof.domain.model.Expense
import com.creazzy.rofof.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExpenseRepository(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    override fun observeRecent(): Flow<List<Expense>> =
        expenseDao.observeRecent().map { records -> records.map { it.toDomain() } }

    override suspend fun add(expense: Expense) {
        expenseDao.insert(expense.toEntity())
    }

    override suspend fun delete(id: String) = expenseDao.delete(id)

    private fun ExpenseEntity.toDomain() = Expense(
        id = id,
        title = title,
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        currencyExponent = currencyExponent,
        category = category,
        occurredAtEpochMillis = occurredAtEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis
    )

    private fun Expense.toEntity() = ExpenseEntity(
        id = id,
        title = title,
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        currencyExponent = currencyExponent,
        category = category,
        occurredAtEpochMillis = occurredAtEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis
    )
}

package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryId", "currencyCode", "periodStartEpochMillis"], unique = true),
        Index(value = ["periodStartEpochMillis"])
    ]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val periodStartEpochMillis: Long,
    val periodEndEpochMillis: Long,
    val createdAtEpochMillis: Long
)

data class BudgetProgress(
    val id: String,
    val categoryId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val periodStartEpochMillis: Long,
    val periodEndEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val spentMinor: Long
)

@Entity(
    tableName = "financial_goals",
    indices = [Index(value = ["isArchived", "createdAtEpochMillis"])]
)
data class FinancialGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val deadlineEpochMillis: Long?,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

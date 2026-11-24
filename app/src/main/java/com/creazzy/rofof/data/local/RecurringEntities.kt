package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_rules", indices = [Index(value = ["isActive", "nextRunEpochMillis"])])
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val transactionType: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val accountId: String,
    val categoryId: String,
    val intervalType: String,
    val intervalCount: Int,
    val nextRunEpochMillis: Long,
    val endEpochMillis: Long?,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val dayOfMonth: Int = 1
)

@Entity(
    tableName = "recurring_drafts",
    indices = [
        Index(value = ["ruleId", "scheduledForEpochMillis"], unique = true),
        Index(value = ["status", "scheduledForEpochMillis"])
    ]
)
data class RecurringDraftEntity(
    @PrimaryKey val id: String,
    val ruleId: String,
    val scheduledForEpochMillis: Long,
    val title: String,
    val transactionType: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val accountId: String,
    val categoryId: String,
    val status: String,
    val createdAtEpochMillis: Long
)

package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debts",
    indices = [Index(value = ["status", "dueAtEpochMillis"]), Index(value = ["currencyCode", "currencyExponent"])]
)
data class DebtEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** OWED_TO_ME or OWED_BY_ME */
    val direction: String,
    val principalMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    /** Annual nominal rate in basis points; informational until an explicit accrual policy is enabled. */
    val annualRateBasisPoints: Int,
    val dueAtEpochMillis: Long?,
    val status: String,
    val note: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "debt_repayments",
    foreignKeys = [ForeignKey(
        entity = DebtEntity::class,
        parentColumns = ["id"],
        childColumns = ["debtId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index(value = ["debtId", "paidAtEpochMillis"])]
)
data class DebtRepaymentEntity(
    @PrimaryKey val id: String,
    val debtId: String,
    val amountMinor: Long,
    val paidAtEpochMillis: Long,
    val note: String?,
    val createdAtEpochMillis: Long
)

data class DebtBalance(
    val id: String,
    val title: String,
    val direction: String,
    val principalMinor: Long,
    val currencyCode: String,
    val currencyExponent: Int,
    val annualRateBasisPoints: Int,
    val dueAtEpochMillis: Long?,
    val status: String,
    val note: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val repaidMinor: Long,
    val outstandingMinor: Long
)

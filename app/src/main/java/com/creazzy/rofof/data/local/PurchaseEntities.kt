package com.creazzy.rofof.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_sessions", indices = [Index("createdAtEpochMillis")])
data class PurchaseSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    val currencyCode: String,
    val currencyExponent: Int,
    val status: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "purchase_participants",
    foreignKeys = [ForeignKey(entity = PurchaseSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("sessionId"), Index(value = ["sessionId", "displayName"], unique = true)]
)
data class PurchaseParticipantEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val displayName: String,
    val note: String?,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [ForeignKey(entity = PurchaseSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("sessionId")]
)
data class PurchaseItemEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val title: String,
    val quantityMilli: Long,
    val unitPriceMinor: Long,
    val totalMinor: Long,
    val note: String?,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "purchase_allocations",
    foreignKeys = [
        ForeignKey(entity = PurchaseItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = PurchaseParticipantEntity::class, parentColumns = ["id"], childColumns = ["participantId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("itemId"), Index("participantId"), Index(value = ["itemId", "participantId"], unique = true)]
)
data class PurchaseAllocationEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val participantId: String,
    val amountMinor: Long,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "purchase_payments",
    foreignKeys = [
        ForeignKey(entity = PurchaseSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = PurchaseParticipantEntity::class, parentColumns = ["id"], childColumns = ["participantId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("sessionId"), Index("participantId"), Index("occurredAtEpochMillis"), Index("idempotencyKey", unique = true)]
)
data class PurchasePaymentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val participantId: String,
    val amountMinor: Long,
    val paymentMethod: String,
    val idempotencyKey: String,
    val note: String?,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "allocation_personal_expense_links",
    foreignKeys = [
        ForeignKey(entity = PurchaseAllocationEntity::class, parentColumns = ["id"], childColumns = ["allocationId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("allocationId", unique = true), Index("transactionId", unique = true)]
)
data class AllocationPersonalExpenseLinkEntity(
    @PrimaryKey val id: String,
    val allocationId: String,
    val transactionId: String,
    val createdAtEpochMillis: Long
)

data class ParticipantSettlementRow(
    val participantId: String,
    val displayName: String,
    val allocatedMinor: Long,
    val paidMinor: Long
) {
    val balanceMinor: Long get() = Math.subtractExact(allocatedMinor, paidMinor)
}

package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(value: PurchaseSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertParticipant(value: PurchaseParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(value: PurchaseItemEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllocations(values: List<PurchaseAllocationEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(value: PurchasePaymentEntity)

    @Update
    suspend fun updateItem(value: PurchaseItemEntity): Int

    @Query("UPDATE purchase_participants SET displayName = :displayName WHERE id = :participantId AND sessionId = :sessionId AND isArchived = 0")
    suspend fun renameParticipant(sessionId: String, participantId: String, displayName: String): Int

    @Query("UPDATE purchase_participants SET isArchived = 1 WHERE id = :participantId AND sessionId = :sessionId AND isArchived = 0")
    suspend fun archiveParticipant(sessionId: String, participantId: String): Int

    @Query("SELECT COUNT(*) FROM purchase_allocations WHERE participantId = :participantId")
    suspend fun allocationCount(participantId: String): Int

    @Query("SELECT COUNT(*) FROM purchase_payments WHERE participantId = :participantId")
    suspend fun paymentCount(participantId: String): Int

    @Query("SELECT COUNT(*) FROM purchase_allocations WHERE itemId = :itemId")
    suspend fun allocationCountForItem(itemId: String): Int

    @Query("DELETE FROM purchase_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpenseLink(value: AllocationPersonalExpenseLinkEntity)

    @Query("SELECT * FROM purchase_sessions ORDER BY createdAtEpochMillis DESC")
    fun observeSessions(): Flow<List<PurchaseSessionEntity>>

    @Query("SELECT * FROM purchase_sessions WHERE id = :id")
    suspend fun findSession(id: String): PurchaseSessionEntity?

    @Query("SELECT * FROM purchase_participants WHERE sessionId = :sessionId AND isArchived = 0 ORDER BY createdAtEpochMillis")
    suspend fun participants(sessionId: String): List<PurchaseParticipantEntity>

    @Query("SELECT * FROM purchase_participants WHERE sessionId = :sessionId AND isArchived = 0 ORDER BY createdAtEpochMillis")
    fun observeParticipants(sessionId: String): Flow<List<PurchaseParticipantEntity>>

    @Query("SELECT * FROM purchase_participants WHERE id = :id")
    suspend fun findParticipant(id: String): PurchaseParticipantEntity?

    @Query("SELECT * FROM purchase_items WHERE sessionId = :sessionId ORDER BY createdAtEpochMillis")
    suspend fun items(sessionId: String): List<PurchaseItemEntity>

    @Query("SELECT * FROM purchase_items WHERE sessionId = :sessionId ORDER BY createdAtEpochMillis")
    fun observeItems(sessionId: String): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE id = :id")
    suspend fun findItem(id: String): PurchaseItemEntity?

    @Query("SELECT * FROM purchase_allocations WHERE itemId = :itemId")
    suspend fun allocationsForItem(itemId: String): List<PurchaseAllocationEntity>

    @Query("""
        SELECT a.id AS allocationId, i.id AS itemId, i.title AS itemTitle, p.id AS participantId,
          p.displayName AS participantName, a.amountMinor AS amountMinor,
          l.transactionId AS personalTransactionId
        FROM purchase_allocations a
        INNER JOIN purchase_items i ON i.id = a.itemId
        INNER JOIN purchase_participants p ON p.id = a.participantId
        LEFT JOIN allocation_personal_expense_links l ON l.allocationId = a.id
        WHERE i.sessionId = :sessionId
        ORDER BY i.createdAtEpochMillis, p.createdAtEpochMillis
    """)
    fun observeAllocationViews(sessionId: String): Flow<List<PurchaseAllocationView>>

    @Query("""
        SELECT p.id AS participantId, p.displayName AS displayName,
          COALESCE((SELECT SUM(a.amountMinor) FROM purchase_allocations a
            INNER JOIN purchase_items i ON i.id = a.itemId
            WHERE i.sessionId = :sessionId AND a.participantId = p.id), 0) AS allocatedMinor,
          COALESCE((SELECT SUM(pay.amountMinor) FROM purchase_payments pay
            WHERE pay.sessionId = :sessionId AND pay.participantId = p.id), 0) AS paidMinor
        FROM purchase_participants p
        WHERE p.sessionId = :sessionId AND p.isArchived = 0
        ORDER BY p.createdAtEpochMillis
    """)
    fun observeSettlements(sessionId: String): Flow<List<ParticipantSettlementRow>>

    @Query("""
        SELECT p.id AS participantId, p.displayName AS displayName,
          COALESCE((SELECT SUM(a.amountMinor) FROM purchase_allocations a
            INNER JOIN purchase_items i ON i.id = a.itemId
            WHERE i.sessionId = :sessionId AND a.participantId = p.id), 0) AS allocatedMinor,
          COALESCE((SELECT SUM(pay.amountMinor) FROM purchase_payments pay
            WHERE pay.sessionId = :sessionId AND pay.participantId = p.id), 0) AS paidMinor
        FROM purchase_participants p
        WHERE p.sessionId = :sessionId AND p.isArchived = 0
        ORDER BY p.createdAtEpochMillis
    """)
    suspend fun settlementSnapshot(sessionId: String): List<ParticipantSettlementRow>

    @Query("SELECT * FROM purchase_payments WHERE idempotencyKey = :key LIMIT 1")
    suspend fun findPaymentByIdempotencyKey(key: String): PurchasePaymentEntity?

    @Query("SELECT * FROM purchase_allocations WHERE id = :id")
    suspend fun findAllocation(id: String): PurchaseAllocationEntity?

    @Query("SELECT * FROM allocation_personal_expense_links WHERE allocationId = :allocationId LIMIT 1")
    suspend fun findExpenseLink(allocationId: String): AllocationPersonalExpenseLinkEntity?

    @Query("SELECT * FROM allocation_personal_expense_links WHERE transactionId = :transactionId LIMIT 1")
    suspend fun findLinkByTransaction(transactionId: String): AllocationPersonalExpenseLinkEntity?

    @Query("UPDATE purchase_sessions SET status = :status, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("SELECT COALESCE(SUM(totalMinor), 0) FROM purchase_items WHERE sessionId = :sessionId")
    suspend fun totalMinor(sessionId: String): Long
}

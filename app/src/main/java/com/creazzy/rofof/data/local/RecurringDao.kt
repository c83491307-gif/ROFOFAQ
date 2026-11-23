package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules ORDER BY isActive DESC, nextRunEpochMillis ASC")
    fun observeRules(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_drafts ORDER BY scheduledForEpochMillis DESC")
    fun observeDrafts(): Flow<List<RecurringDraftEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRule(rule: RecurringRuleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDraftIfAbsent(draft: RecurringDraftEntity): Long

    @Update
    suspend fun updateRule(rule: RecurringRuleEntity): Int

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1 AND nextRunEpochMillis <= :nowEpochMillis ORDER BY nextRunEpochMillis ASC")
    suspend fun findDueRules(nowEpochMillis: Long): List<RecurringRuleEntity>

    @Query("UPDATE recurring_drafts SET status = :status WHERE id = :draftId AND status = 'DRAFT'")
    suspend fun updateDraftStatus(draftId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM recurring_drafts WHERE ruleId = :ruleId")
    suspend fun countDrafts(ruleId: String): Int
}

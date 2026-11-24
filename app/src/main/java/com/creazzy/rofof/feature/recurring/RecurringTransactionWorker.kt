package com.creazzy.rofof.feature.recurring

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.creazzy.rofof.data.local.RecurringRuleEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.local.TransactionEntity
import com.creazzy.rofof.feature.awareness.BudgetRuleNotificationDispatcher
import com.creazzy.rofof.widget.ExpenseWidgetUpdater
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * Executes due recurring rules locally. Each occurrence is keyed by rule + scheduled instant,
 * and both the transaction insert and next-run update are committed atomically.
 */
class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = runCatching {
        val database = RofofDatabase.getInstance(applicationContext)
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        var insertedExpense = false

        database.withTransaction {
            database.recurringDao().findDueRules(now).forEach { rule ->
                val account = database.accountDao().findById(rule.accountId)
                val category = database.categoryDao().findById(rule.categoryId)
                if (account == null || account.isArchived ||
                    category == null || category.isArchived ||
                    category.transactionType != rule.transactionType ||
                    account.currencyCode != rule.currencyCode ||
                    account.currencyExponent != rule.currencyExponent
                ) {
                    database.recurringDao().updateRule(rule.copy(isActive = false))
                    return@forEach
                }

                val anchorDay = Instant.ofEpochMilli(rule.nextRunEpochMillis).atZone(zone).dayOfMonth
                var scheduled = rule.nextRunEpochMillis
                var processed = 0
                while (scheduled <= now && processed < MAX_CATCH_UP_OCCURRENCES) {
                    if (rule.endEpochMillis == null || scheduled <= rule.endEpochMillis) {
                        val occurrenceKey = "${rule.id}@$scheduled"
                        val rowId = database.transactionDao().insertRecurringIfAbsent(
                            TransactionEntity(
                                id = UUID.nameUUIDFromBytes(occurrenceKey.toByteArray()).toString(),
                                title = rule.title,
                                transactionType = rule.transactionType,
                                amountMinor = rule.amountMinor,
                                currencyCode = rule.currencyCode,
                                currencyExponent = rule.currencyExponent,
                                accountId = rule.accountId,
                                categoryId = rule.categoryId,
                                occurredAtEpochMillis = scheduled,
                                createdAtEpochMillis = now,
                                notes = "Automatically generated from recurring rule",
                                recurringOccurrenceKey = occurrenceKey
                            )
                        )
                        if (rowId != -1L && rule.transactionType == "EXPENSE") insertedExpense = true
                    }
                    scheduled = nextMonthly(scheduled, rule.intervalCount, anchorDay, zone)
                    processed++
                }

                val stillActive = rule.endEpochMillis == null || scheduled <= rule.endEpochMillis
                database.recurringDao().updateRule(
                    rule.copy(nextRunEpochMillis = scheduled, isActive = stillActive)
                )
            }
        }

        if (insertedExpense) {
            runCatching {
                BudgetRuleNotificationDispatcher(applicationContext, database).dispatchCurrentMonth()
            }
            ExpenseWidgetUpdater.refresh(applicationContext)
        }
        Result.success()
    }.getOrElse { Result.retry() }

    private fun nextMonthly(
        scheduledEpochMillis: Long,
        intervalCount: Int,
        anchorDay: Int,
        zone: ZoneId
    ): Long {
        val current = Instant.ofEpochMilli(scheduledEpochMillis).atZone(zone)
        val targetMonth = YearMonth.from(current).plusMonths(intervalCount.toLong())
        val targetDay = minOf(anchorDay, targetMonth.lengthOfMonth())
        return targetMonth.atDay(targetDay)
            .atTime(current.toLocalTime())
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    companion object {
        private const val MAX_CATCH_UP_OCCURRENCES = 1200
    }
}

package com.creazzy.rofof.feature.awareness

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.creazzy.rofof.data.local.RofofDatabase

/** Periodic local reevaluation survives process death and device restarts via WorkManager. */
class BudgetNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        val database = RofofDatabase.getInstance(applicationContext)
        BudgetRuleNotificationDispatcher(applicationContext, database).dispatchCurrentMonth()
        Result.success()
    }.getOrElse { Result.retry() }
}

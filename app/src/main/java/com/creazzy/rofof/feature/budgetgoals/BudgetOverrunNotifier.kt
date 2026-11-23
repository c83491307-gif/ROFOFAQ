package com.creazzy.rofof.feature.budgetgoals

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.creazzy.rofof.app.MainActivity
import com.creazzy.rofof.data.local.BudgetGoalDao
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class BudgetOverrunNotifier(
    context: Context,
    private val budgetDao: BudgetGoalDao
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val notificationManager = NotificationManagerCompat.from(appContext)

    suspend fun refreshForExpense(
        categoryId: String,
        currencyCode: String,
        currencyExponent: Int,
        occurredAtEpochMillis: Long,
        language: String
    ) {
        val snapshots = budgetDao.findBudgetAlertsForExpense(
            categoryId = categoryId,
            currencyCode = currencyCode,
            currencyExponent = currencyExponent,
            occurredAtEpochMillis = occurredAtEpochMillis
        )

        snapshots.forEach { budget ->
            val stateKey = "\${budget.id}:\${budget.periodStartEpochMillis}"
            val alreadyNotified = preferences.getBoolean(stateKey, false)

            if (BudgetOverrunPolicy.shouldRearm(budget.spentMinor, budget.amountMinor)) {
                if (alreadyNotified) preferences.edit().remove(stateKey).apply()
                return@forEach
            }

            if (!BudgetOverrunPolicy.shouldNotify(budget.spentMinor, budget.amountMinor, alreadyNotified)) {
                return@forEach
            }

            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return@forEach

            if (!notificationManager.areNotificationsEnabled()) return@forEach

            ensureChannel()
            val ar = language == "ar"
            val locale = if (ar) Locale("ar", "EG") else Locale.US
            val spent = formatMoney(budget.spentMinor, budget.currencyCode, budget.currencyExponent, locale)
            val limit = formatMoney(budget.amountMinor, budget.currencyCode, budget.currencyExponent, locale)
            val title = if (ar) "تم تجاوز الميزانية" else "Budget limit exceeded"
            val message = if (ar) {
                "وصل إنفاقك إلى $spent، متجاوزاً الحد المحدد $limit."
            } else {
                "Your spending reached $spent, above the budget limit of $limit."
            }
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                stateKey.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            runCatching {
                notificationManager.notify("budget-overrun:$stateKey", stateKey.hashCode(), notification)
                preferences.edit().putBoolean(stateKey, true).apply()
            }
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Budget alerts · تنبيهات الميزانية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when category spending exceeds its monthly budget"
            }
        )
    }

    private fun formatMoney(minor: Long, code: String, exponent: Int, locale: Locale): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(code)
            minimumFractionDigits = exponent
            maximumFractionDigits = exponent
        }.format(BigDecimal.valueOf(minor, exponent))

    companion object {
        private const val CHANNEL_ID = "budget_overrun_alerts"
        private const val PREFERENCES = "budget_overrun_alert_state"
    }
}

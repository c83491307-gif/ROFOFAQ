package com.creazzy.rofof.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.creazzy.rofof.feature.awareness.BudgetNotificationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var quickAddRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        quickAddRequested = intent?.action == ACTION_QUICK_ADD
        scheduleBudgetNotifications()
        window.statusBarColor = android.graphics.Color.rgb(244, 247, 245)
        window.navigationBarColor = android.graphics.Color.rgb(244, 247, 245)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF147D64),
                    onPrimary = Color.White,
                    background = Color(0xFFF4F7F5),
                    surface = Color.White
                )
            ) {
                ROFOFRootScreen(
                    openQuickAdd = quickAddRequested,
                    onQuickAddConsumed = { quickAddRequested = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_QUICK_ADD) quickAddRequested = true
    }

    private fun scheduleBudgetNotifications() {
        val request = PeriodicWorkRequestBuilder<BudgetNotificationWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            BUDGET_NOTIFICATION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val ACTION_QUICK_ADD = "com.creazzy.rofof.action.QUICK_ADD"
        const val ACTION_OPEN_DASHBOARD = "com.creazzy.rofof.action.OPEN_DASHBOARD"
        private const val BUDGET_NOTIFICATION_WORK = "rofof-budget-rule-notifications"
    }
}

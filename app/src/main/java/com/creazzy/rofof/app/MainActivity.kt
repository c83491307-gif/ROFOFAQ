package com.creazzy.rofof.app

import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.creazzy.rofof.feature.awareness.BudgetNotificationWorker
import com.creazzy.rofof.feature.recurring.RecurringTransactionWorker
import com.creazzy.rofof.widget.ExpenseWidgetUpdater
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
    private var appLockEnabled by mutableStateOf(false)
    private var appUnlocked by mutableStateOf(true)
    private var authenticationInProgress = false
    private var promptAttempted = false

    private var quickAddRequested by mutableStateOf(false)
    private var openDashboardRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        appLockEnabled = getSharedPreferences(SECURITY_PREFS, MODE_PRIVATE).getBoolean(APP_LOCK_KEY, false)
        appUnlocked = !appLockEnabled
        quickAddRequested = intent?.action == ACTION_QUICK_ADD
        openDashboardRequested = intent?.action == ACTION_OPEN_DASHBOARD
        scheduleBudgetNotifications()
        ExpenseWidgetUpdater.refresh(applicationContext)
        setContent {
            val systemDark = isSystemInDarkTheme()
            SideEffect {
                val barColor = if (systemDark) android.graphics.Color.rgb(16, 24, 22)
                    else android.graphics.Color.rgb(244, 247, 245)
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
                window.decorView.systemUiVisibility = if (systemDark) 0 else
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            MaterialTheme(
                colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (systemDark) dynamicDarkColorScheme(this@MainActivity) else dynamicLightColorScheme(this@MainActivity)
                } else if (systemDark) darkColorScheme(
                    primary = Color(0xFF79D6B5),
                    onPrimary = Color(0xFF00382B),
                    background = Color(0xFF101816),
                    surface = Color(0xFF18211E)
                ) else lightColorScheme(
                    primary = Color(0xFF147D64),
                    onPrimary = Color.White,
                    background = Color(0xFFF4F7F5),
                    surface = Color.White
                )
            ) {
                if (appLockEnabled && !appUnlocked) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ROFOF · التطبيق مقفل")
                            Button(onClick = { promptAttempted = false; authenticateApp() }) { Text("التحقق بالبصمة أو قفل الجهاز") }
                        }
                    }
                } else {
                    ROFOFRootScreen(
                        openQuickAdd = quickAddRequested,
                        onQuickAddConsumed = { quickAddRequested = false },
                        openDashboard = openDashboardRequested,
                        onDashboardConsumed = { openDashboardRequested = false },
                        appLockEnabled = appLockEnabled,
                        onAppLockChange = ::setAppLockEnabled
                    )
                }
            }
        }
        if (appLockEnabled) window.decorView.post { authenticateApp() }
    }

    override fun onResume() {
        super.onResume()
        ExpenseWidgetUpdater.refresh(applicationContext)
        if (appLockEnabled && !appUnlocked && !authenticationInProgress && !promptAttempted) authenticateApp()
    }

    override fun onPause() {
        if (appLockEnabled && !authenticationInProgress) { appUnlocked = false; promptAttempted = false }
        super.onPause()
    }

    private fun setAppLockEnabled(enabled: Boolean) {
        if (enabled) {
            val authenticators = if (Build.VERSION.SDK_INT >= 30) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else BiometricManager.Authenticators.BIOMETRIC_STRONG
            if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return
        }
        appLockEnabled = enabled
        appUnlocked = !enabled
        getSharedPreferences(SECURITY_PREFS, MODE_PRIVATE).edit().putBoolean(APP_LOCK_KEY, enabled).apply()
        if (enabled) authenticateApp()
    }

    private fun authenticateApp() {
        if (!appLockEnabled || appUnlocked || authenticationInProgress || promptAttempted || isFinishing) return
        val authenticators = if (Build.VERSION.SDK_INT >= 30) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ROFOF · قفل التطبيق")
            .setSubtitle("تحقق من هويتك للوصول إلى بياناتك المالية")
            .setAllowedAuthenticators(authenticators)
        if (Build.VERSION.SDK_INT < 30) builder.setNegativeButtonText("إلغاء")
        authenticationInProgress = true
        promptAttempted = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticationInProgress = false
                    appUnlocked = true
                    promptAttempted = false
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticationInProgress = false
                    appUnlocked = false
                }
                override fun onAuthenticationFailed() {
                    appUnlocked = false
                }
            }
        )
        prompt.authenticate(builder.build())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when (intent.action) {
            ACTION_QUICK_ADD -> quickAddRequested = true
            ACTION_OPEN_DASHBOARD -> openDashboardRequested = true
        }
    }

    private fun scheduleBudgetNotifications() {
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "rofof-budget-rule-check-now",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BudgetNotificationWorker>().build()
        )
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "rofof-recurring-check-now",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RecurringTransactionWorker>().build()
        )
        val request = PeriodicWorkRequestBuilder<BudgetNotificationWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            BUDGET_NOTIFICATION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        val recurringRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            RECURRING_TRANSACTION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            recurringRequest
        )
    }

    companion object {
        private const val SECURITY_PREFS = "rofof-security"
        private const val APP_LOCK_KEY = "app_lock_enabled"
        const val ACTION_QUICK_ADD = "com.creazzy.rofof.action.QUICK_ADD"
        const val ACTION_OPEN_DASHBOARD = "com.creazzy.rofof.action.OPEN_DASHBOARD"
        private const val BUDGET_NOTIFICATION_WORK = "rofof-budget-rule-notifications"
        private const val RECURRING_TRANSACTION_WORK = "rofof-recurring-transactions"
    }
}

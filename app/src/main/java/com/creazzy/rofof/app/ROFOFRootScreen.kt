package com.creazzy.rofof.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.creazzy.rofof.feature.budgetgoals.BudgetGoalsScreen
import com.creazzy.rofof.feature.dashboard.DashboardScreen
import com.creazzy.rofof.feature.dashboard.ExpenseViewModel
import com.creazzy.rofof.feature.debts.DebtManagementScreen
import com.creazzy.rofof.feature.purchases.PurchaseManagementScreen
import com.creazzy.rofof.feature.recurring.RecurringManagementScreen
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private data class RootDestination(
    val route: String,
    val labelAr: String,
    val labelEn: String,
    val symbol: String
)

private val rootDestinations = listOf(
    RootDestination("dashboard", "الرئيسية", "Home", "⌂"),
    RootDestination("transactions", "المعاملات", "Activity", "↕"),
    RootDestination("budget", "الميزانية", "Budget", "◷"),
    RootDestination("settings", "الإعدادات", "Settings", "⚙")
)

@Composable
fun ROFOFRootScreen(
    openQuickAdd: Boolean = false,
    onQuickAddConsumed: () -> Unit = {},
    openDashboard: Boolean = false,
    onDashboardConsumed: () -> Unit = {},
    appLockEnabled: Boolean = false,
    onAppLockChange: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "dashboard"
    val expenseViewModel: ExpenseViewModel = viewModel()
    val language by expenseViewModel.language.collectAsState()
    val isArabic = language == "ar"
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(expenseViewModel::exportTransactions) }
    val requestCsvExport: () -> Unit = { csvExportLauncher.launch("ROFOF-transactions.csv") }
    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(openQuickAdd, openDashboard) {
        if (openQuickAdd || openDashboard) {
            navController.navigate("dashboard") { launchSingleTop = true }
            if (openDashboard) onDashboardConsumed()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF4F7F5),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                rootDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(destination.symbol, style = MaterialTheme.typography.titleMedium) },
                        label = { Text(if (isArabic) destination.labelAr else destination.labelEn) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    expenseViewModel,
                    openAddInitially = openQuickAdd,
                    onQuickAddConsumed = onQuickAddConsumed
                )
            }
            composable("transactions") {
                TransactionsRoute(expenseViewModel)
            }
            composable("budget") {
                BudgetGoalsScreen(onBack = { navController.navigate("dashboard") })
            }
            composable("settings") {
                SettingsRoute(
                    language = language,
                    onLanguageChange = expenseViewModel::setLanguage,
                    onPurchases = { navController.navigate("purchases") },
                    onRecurring = { navController.navigate("recurring") },
                    onRequestNotifications = requestNotificationPermission,
                    onExportCsv = requestCsvExport,
                    onTrash = { navController.navigate("trash") },
                    onDebts = { navController.navigate("debts") },
                    appLockEnabled = appLockEnabled,
                    onAppLockChange = onAppLockChange
                )
            }
            composable("purchases") {
                PurchaseManagementScreen(onBack = { navController.popBackStack() })
            }
            composable("recurring") {
                RecurringManagementScreen(onBack = { navController.popBackStack() })
            }
            composable("trash") {
                TrashRoute(expenseViewModel, onBack = { navController.popBackStack() })
            }
            composable("debts") {
                DebtManagementScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun TransactionsRoute(viewModel: ExpenseViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val language by viewModel.language.collectAsState()
    val ar = language == "ar"
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Text(
            if (ar) "كل المعاملات" else "All transactions",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF172B2A)
        )
        Text(
            if (ar) "${transactions.size} عملية محفوظة على هذا الجهاز" else "${transactions.size} transactions stored on this device",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF71817D)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(item.title, color = Color(0xFF172B2A), style = MaterialTheme.typography.titleMedium)
                        Text(
                            moneyLabel(item.amountMinor, item.currencyCode, item.currencyExponent, ar),
                            color = if (item.transactionType == "INCOME") Color(0xFF147D64) else Color(0xFFB45B52),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            if (ar) if (item.transactionType == "INCOME") "دخل" else "مصروف"
                            else if (item.transactionType == "INCOME") "Income" else "Expense",
                            color = Color(0xFF71817D),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRoute(
    language: String,
    onLanguageChange: (String) -> Unit,
    onPurchases: () -> Unit,
    onRecurring: () -> Unit,
    onRequestNotifications: () -> Unit,
    onExportCsv: () -> Unit,
    onTrash: () -> Unit,
    onDebts: () -> Unit,
    appLockEnabled: Boolean,
    onAppLockChange: (Boolean) -> Unit
) {
    val ar = language == "ar"
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (ar) "الإعدادات" else "Settings", style = MaterialTheme.typography.headlineSmall)
        Text(if (ar) "اللغة" else "Language", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { onLanguageChange(if (ar) "en" else "ar") }) {
            Text(if (ar) "English · LTR" else "العربية · RTL")
        }
        TextButton(onClick = onRequestNotifications) {
            Text(if (ar) "تفعيل إشعارات الميزانية" else "Enable budget notifications")
        }
        TextButton(onClick = onExportCsv) {
            Text(if (ar) "تصدير المعاملات إلى Excel / CSV" else "Export transactions to Excel / CSV")
        }
        TextButton(onClick = onTrash) {
            Text(if (ar) "سلة المحذوفات · 30 يوماً" else "Recycle bin · 30 days")
        }
        TextButton(onClick = onDebts) {
            Text(if (ar) "الديون والسلف" else "Debts and loans")
        }
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (ar) "قفل التطبيق بالبصمة / قفل الجهاز" else "Biometric / device app lock")
            Switch(checked = appLockEnabled, onCheckedChange = onAppLockChange)
        }
        Text(if (ar) "الخصوصية" else "Privacy", style = MaterialTheme.typography.titleMedium)
        Text(
            if (ar) "تُحفظ بياناتك محلياً على الجهاز. لا يحتاج التطبيق إلى اتصال بالإنترنت للوظائف المالية الأساسية."
            else "Your financial data stays on this device. Core finance features do not require an internet connection.",
            color = Color(0xFF71817D)
        )
        TextButton(onClick = onPurchases) { Text(if (ar) "المشتريات المشتركة" else "Shared purchases") }
        TextButton(onClick = onRecurring) { Text(if (ar) "العمليات المتكررة" else "Recurring transactions") }
    }
}

@Composable
private fun TrashRoute(viewModel: ExpenseViewModel, onBack: () -> Unit) {
    val items by viewModel.trash.collectAsState()
    val language by viewModel.language.collectAsState()
    val ar = language == "ar"
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        TextButton(onClick = onBack) { Text(if (ar) "رجوع" else "Back") }
        Text(if (ar) "سلة المحذوفات" else "Recycle bin", style = MaterialTheme.typography.headlineSmall)
        Text(if (ar) "تُحذف نهائياً بعد 30 يوماً" else "Items are permanently removed after 30 days")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(moneyLabel(item.amountMinor, item.currencyCode, item.currencyExponent, ar))
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.restoreTransaction(item.id) }) {
                                Text(if (ar) "استعادة" else "Restore")
                            }
                            TextButton(onClick = { viewModel.permanentlyDeleteTransaction(item.id) }) {
                                Text(if (ar) "حذف نهائي" else "Delete permanently")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun moneyLabel(minor: Long, code: String, exponent: Int, ar: Boolean): String =
    NumberFormat.getCurrencyInstance(if (ar) Locale("ar", "EG") else Locale.US).apply {
        currency = Currency.getInstance(code)
        minimumFractionDigits = exponent
        maximumFractionDigits = exponent
    }.format(BigDecimal.valueOf(minor, exponent))

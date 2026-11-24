package com.creazzy.rofof.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.creazzy.rofof.core.money.AmountInputParser
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.creazzy.rofof.domain.model.LedgerTransaction
import com.creazzy.rofof.feature.smartentry.SmartEntryParser
import com.creazzy.rofof.feature.smartentry.FrequentCategoryChips
import com.creazzy.rofof.feature.smartentry.LocalVoiceInputButton
import com.creazzy.rofof.feature.smartentry.SmartPasteAction
import com.creazzy.rofof.feature.smartentry.rememberSaveHaptic
import com.creazzy.rofof.feature.awareness.AwarenessEntryReview
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale


@Composable
fun DashboardScreen(
    expenseViewModel: ExpenseViewModel = viewModel(),
    openAddInitially: Boolean = false,
    onQuickAddConsumed: () -> Unit = {}
) {
    val transactions by expenseViewModel.transactions.collectAsState()
    val transactionsLoaded by expenseViewModel.transactionsLoaded.collectAsState()
    val transactionsLoadError by expenseViewModel.transactionsLoadError.collectAsState()
    val categories by expenseViewModel.categories.collectAsState()
    val learnedKeywordSuggestions by expenseViewModel.learnedKeywordSuggestions.collectAsState()
    val accounts by expenseViewModel.accounts.collectAsState()
    val frequentCategories by expenseViewModel.frequentCategories.collectAsState()
    val hapticOnSaved = rememberSaveHaptic()
    val managedAccounts by expenseViewModel.managedAccounts.collectAsState()
    val managedCategories by expenseViewModel.managedCategories.collectAsState()
    val totals by expenseViewModel.totals.collectAsState()
    val safeToSpend by expenseViewModel.safeToSpend.collectAsState()
    val insights by expenseViewModel.insights.collectAsState()
    val language by expenseViewModel.language.collectAsState()
    val financeActionMessage by expenseViewModel.financeActionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(financeActionMessage) {
        financeActionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            expenseViewModel.clearFinanceActionMessage()
        }
    }
    val isSavingTransaction by expenseViewModel.isSavingTransaction.collectAsState()
    val awarenessReview by expenseViewModel.awarenessReview.collectAsState()
    val isArabic = language == "ar"
    val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val words = if (isArabic) Arabic else English
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showAccounts by rememberSaveable { mutableStateOf(false) }
    var showTransfer by rememberSaveable { mutableStateOf(false) }
    var showCategories by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<LedgerTransaction?>(null) }
    var pendingEdit by remember { mutableStateOf<LedgerTransaction?>(null) }

    LaunchedEffect(openAddInitially) {
        if (openAddInitially) {
            showAdd = true
            onQuickAddConsumed()
        }
    }

    val income = totals.incomeMinor
    val expenses = totals.expenseMinor
    val net = accounts.asSequence()
        .filter { it.currencyCode == "EGP" && it.currencyExponent == 2 }
        .fold(0L) { total, account -> Math.addExact(total, account.balanceMinor) }
    val categoryNames = categories.associate { it.id to it.name }

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(words.greeting, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                        Text("ROFOF", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                    FilledTonalButton(
                        onClick = { expenseViewModel.setLanguage(if (isArabic) "en" else "ar") },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) { Text(if (isArabic) "EN  ·  LTR" else "عربي  ·  RTL") }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = { showAccounts = true }, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "الحسابات" else "Accounts")
                    }
                    FilledTonalButton(onClick = { showTransfer = true }, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "تحويل" else "Transfer")
                    }
                    FilledTonalButton(onClick = { showCategories = true }, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "الفئات" else "Categories")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)))
                            .padding(20.dp)
                    ) {
                        Text(words.netBalance, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(formatEgp(net, language), color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BalanceMetric(words.income, income, MaterialTheme.colorScheme.primary, language, Modifier.weight(1f))
                            BalanceMetric(words.expenses, expenses, MaterialTheme.colorScheme.error, language, Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            if (isArabic) "المتبقي الآمن للإنفاق اليومي" else "Safe to spend today",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        if (safeToSpend.hasBudget) {
                            Text(
                                formatEgp(safeToSpend.dailySafeMinor, language),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isArabic) "بناءً على الميزانية والمصروفات المسجلة · ${safeToSpend.daysRemaining} يومًا متبقيًا"
                                else "Based on recorded budget and expenses · ${safeToSpend.daysRemaining} days remaining",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                if (isArabic) "حدد ميزانيتك الشهرية أولاً لحساب المبلغ الآمن."
                                else "Set a monthly budget to calculate your safe daily amount.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (insights.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text(
                                if (isArabic) "رؤى مالية ذكية" else "Smart financial insights",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            insights.forEach { insight ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("✦", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                                    Text(insight, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(words.recent, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(words.savedEntries(transactions.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { showAdd = true },
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text(words.addTransaction) }
                }

                Spacer(Modifier.height(12.dp))
                if (!transactionsLoaded) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(3) {
                            Card(
                                modifier = Modifier.fillMaxWidth().height(76.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(18.dp)
                            ) { Box(Modifier.fillMaxSize()) }
                        }
                    }
                } else if (transactionsLoadError != null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                if (isArabic) "تعذر تحميل العمليات المالية." else "Could not load transactions.",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                transactionsLoadError.orEmpty(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(onClick = expenseViewModel::retryTransactions) {
                                Text(if (isArabic) "إعادة المحاولة" else "Retry")
                            }
                        }
                    }
                } else if (transactions.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(66.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text("＋", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge) }
                            Spacer(Modifier.height(14.dp))
                            Text(words.emptyTitle, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(words.emptyBody, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(transactions, key = { it.id }) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                categoryName = categoryNames[transaction.categoryId] ?: words.general,
                                language = language,
                                deleteLabel = words.delete,
                                editLabel = words.edit,
                                onEdit = { pendingEdit = transaction },
                                onDelete = { pendingDelete = transaction }
                            )
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
            }
        }
    }

    if (showAccounts) {
        AccountManagerDialog(
            accounts = managedAccounts,
            language = language,
            onCreate = expenseViewModel::createAccount,
            onRename = expenseViewModel::renameAccount,
            onArchive = expenseViewModel::archiveAccount,
            onDismiss = { showAccounts = false }
        )
    }
    if (showTransfer) {
        TransferDialog(
            accounts = accounts,
            language = language,
            onTransfer = { from, to, amount, note ->
                expenseViewModel.transfer(from, to, amount, note)
                showTransfer = false
            },
            onDismiss = { showTransfer = false }
        )
    }
    if (showCategories) {
        CategoryManagerDialog(
            categories = managedCategories,
            language = language,
            onCreate = expenseViewModel::createCategory,
            onRename = expenseViewModel::renameCategory,
            onArchive = expenseViewModel::archiveCategory,
            onDismiss = { showCategories = false }
        )
    }

    if (showAdd) {
        AddTransactionDialog(
            words = words,
            language = language,
            accounts = accounts,
            learnedKeywords = learnedKeywordSuggestions,
            frequentCategories = frequentCategories,
            onDismiss = { showAdd = false },
            onAnalyze = { command, accountId, occurredAt -> expenseViewModel.analyzeSmartEntry(command, accountId, occurredAt) },
            awarenessReview = awarenessReview,
            onClearReview = expenseViewModel::clearAwarenessReview,
            isSaving = isSavingTransaction,
            onSave = { title, amount, category, type, accountId, occurredAt ->
                expenseViewModel.addTransaction(title, amount, category, type, accountId, occurredAt, onSaved = hapticOnSaved)
                showAdd = false
            }
        )
    }

    pendingEdit?.let { transaction ->
        AddTransactionDialog(
            words = words,
            language = language,
            accounts = accounts,
            learnedKeywords = learnedKeywordSuggestions,
            initialTransaction = transaction,
            isSaving = isSavingTransaction,
            initialCategoryName = categoryNames[transaction.categoryId] ?: words.general,
            onDismiss = { pendingEdit = null },
            onSave = { title, amount, category, type, accountId, occurredAt ->
                expenseViewModel.updateTransaction(transaction, title, amount, category, type, accountId, occurredAt)
                pendingEdit = null
            }
        )
    }

    pendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(words.confirmDelete, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text(words.confirmDeleteBody) },
            confirmButton = {
                Button(
                    onClick = {
                        expenseViewModel.deleteTransaction(transaction.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(words.delete) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(words.cancel) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

}

@Composable
private fun BalanceMetric(label: String, amount: Long, color: Color, language: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(formatEgp(amount, language), color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TransactionRow(
    transaction: LedgerTransaction,
    categoryName: String,
    language: String,
    deleteLabel: String,
    editLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.transactionType == "INCOME"
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Text(if (isIncome) "＋" else "−", color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "$categoryName  ·  ${dateLabel(transaction.occurredAtEpochMillis, language)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (isIncome) "+" else "−") + formatMoney(
                        transaction.amountMinor, transaction.currencyCode, transaction.currencyExponent, language
                    ),
                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onEdit, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(editLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onDelete, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(deleteLabel, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    words: Words,
    language: String,
    accounts: List<com.creazzy.rofof.data.local.AccountBalance>,
    learnedKeywords: Map<String, SmartEntryParser.CategorySuggestion> = emptyMap(),
    frequentCategories: List<com.creazzy.rofof.data.local.FrequentCategory> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Long) -> Unit,
    onAnalyze: (String, String, Long) -> Unit = { _, _, _ -> },
    awarenessReview: AwarenessEntryReview? = null,
    onClearReview: () -> Unit = {},
    initialTransaction: LedgerTransaction? = null,
    initialCategoryName: String = "",
    isSaving: Boolean = false
) {
    var title by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialTransaction?.title.orEmpty()) }
    var amount by rememberSaveable(initialTransaction?.id) {
        mutableStateOf(initialTransaction?.let { BigDecimal.valueOf(it.amountMinor, it.currencyExponent).toPlainString() }.orEmpty())
    }
    var category by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialCategoryName) }
    var smartCommand by rememberSaveable(initialTransaction?.id) { mutableStateOf("") }
    var smartSuggestion by rememberSaveable(initialTransaction?.id) { mutableStateOf<String?>(null) }
    var appliedReviewText by rememberSaveable(initialTransaction?.id) { mutableStateOf<String?>(null) }
    val smartParser = remember(learnedKeywords) { SmartEntryParser(learnedKeywords = learnedKeywords) }
    var type by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialTransaction?.transactionType ?: "EXPENSE") }
    var accountId by rememberSaveable(accounts, initialTransaction?.id) { mutableStateOf(initialTransaction?.accountId ?: accounts.firstOrNull()?.id.orEmpty()) }
    var accountMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTransaction?.occurredAtEpochMillis ?: System.currentTimeMillis())
    val selectedCurrencyExponent = accounts.firstOrNull { it.id == accountId }?.currencyExponent ?: 2
    val amountValid = AmountInputParser.toMinorUnits(amount, selectedCurrencyExponent) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTransaction == null) words.newTransaction else words.edit, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { type = "EXPENSE" },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (type == "EXPENSE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == "EXPENSE") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text(words.expenses) }
                    FilledTonalButton(
                        onClick = { type = "INCOME" },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == "INCOME") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text(words.income) }
                }
                if (initialTransaction == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        LocalVoiceInputButton(onRecognized = { spoken ->
                            smartCommand = spoken
                            val parsed = smartParser.parse(spoken)
                            parsed.amountText?.let { amount = it }
                            parsed.category?.let { suggestion ->
                                category = if (language == "ar") suggestion.categoryNameArabic else suggestion.categoryNameEnglish
                                type = suggestion.transactionType
                            }
                        })
                        SmartPasteAction(onAccept = { pasted ->
                            smartCommand = pasted.sourcePreview
                            pasted.amountText.let { amount = it }
                            pasted.parsedEntry.category?.let { suggestion ->
                                category = if (language == "ar") suggestion.categoryNameArabic else suggestion.categoryNameEnglish
                                type = suggestion.transactionType
                            }
                            appliedReviewText = null
                        })
                    }
                    FrequentCategoryChips(categories = frequentCategories, onSelect = { selectedId ->
                        frequentCategories.firstOrNull { it.categoryId == selectedId }?.let { category = it.categoryName }
                    }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = smartCommand,
                        onValueChange = {
                            smartCommand = it
                            val parsed = smartParser.parse(it)
                            smartSuggestion = parsed.category?.categoryNameArabic
                            parsed.category?.let { suggestion ->
                                category = if (language == "ar") suggestion.categoryNameArabic else suggestion.categoryNameEnglish
                                type = suggestion.transactionType
                            }
                            appliedReviewText = null
                            onClearReview()
                        },
                        label = { Text(if (language == "ar") "اكتب العملية بلغتك" else "Describe the transaction") },
                        placeholder = { Text(if (language == "ar") "مثال: قهوة ٥٠ أو استلمت راتب ١٥٠٠٠" else "Example: coffee 50 or received salary 15000") },
                        supportingText = { Text(if (language == "ar") "تحليل محلي على الجهاز — لا يتم الحفظ قبل مراجعتك" else "Parsed locally on this device — nothing is saved until you review") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    smartSuggestion?.let { suggested ->
                        Text(
                            (if (language == "ar") "تصنيف مقترح من تعلمك السابق: " else "Learned category suggestion: ") + suggested,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(
                        onClick = { appliedReviewText = null; onAnalyze(smartCommand, accountId, datePickerState.selectedDateMillis ?: System.currentTimeMillis()) },
                        enabled = smartCommand.isNotBlank() && accountId.isNotBlank()
                    ) { Text(if (language == "ar") "حلّل وراجع" else "Analyze & review") }
                    awarenessReview?.takeIf { it.sourceText == smartCommand }?.let { review ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (language == "ar") "مراجعة الاقتراح المحلي" else "Local suggestion review", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text((if (language == "ar") "النوع: " else "Type: ") + if (review.transactionType == "INCOME") words.income else words.expenses)
                                Text((if (language == "ar") "المبلغ: " else "Amount: ") + (review.amountText ?: "—"))
                                Text((if (language == "ar") "الفئة: " else "Category: ") + (review.categoryLabel ?: if (language == "ar") "غير معروفة — اختر فئة يدويًا" else "Unknown — choose manually"))
                                review.ruleMatches.forEach { match ->
                                    Text(if (language == "ar") match.action.messageAr else match.action.messageEn,
                                        color = if (match.action.severity == "CRITICAL" || match.action.severity == "WARNING") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = {
                                    review.amountText?.let { amount = it }
                                    type = review.transactionType
                                    review.categoryName?.let { category = it }
                                    val cleaned = smartCommand.replace(Regex("""[٠-٩۰-۹0-9]+([٫.,][٠-٩۰-۹0-9]+)?"""), "").trim()
                                    if (cleaned.isNotBlank()) title = cleaned
                                    appliedReviewText = smartCommand
                                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
                                    Text(if (language == "ar") "اعتماد المراجعة وتطبيق الاقتراح" else "Accept review and apply suggestion")
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(words.transactionName) }, singleLine = true)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        amount = value.filter { char -> char in '0'..'9' || char in '٠'..'٩' || char in '۰'..'۹' || char in "٫.,٬" || char.isWhitespace() }
                    },
                    label = { Text(words.amount) },
                    supportingText = {
                        Text(
                            if (amount.isNotBlank() && !amountValid) {
                                if (language == "ar") "أدخل مبلغاً أكبر من صفر وبعدد منازل عشرية صحيح." else "Enter an amount greater than zero with valid decimal precision."
                            } else words.amountHint
                        )
                    },
                    isError = amount.isNotBlank() && !amountValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(words.category) }, singleLine = true)
                TextButton(onClick = { showDatePicker = true }) {
                    Text("${words.transactionDate}: ${dateLabel(datePickerState.selectedDateMillis ?: System.currentTimeMillis(), language)}")
                }
                TextButton(onClick = { accountMenu = true }) {
                    val selected = accounts.firstOrNull { it.id == accountId }
                    Text(if (selected == null) words.chooseAccount else selected.name + " · " + selected.currencyCode)
                }
                androidx.compose.material3.DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                    accounts.forEach { account ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(account.name + " · " + account.currencyCode) },
                            onClick = { accountId = account.id; accountMenu = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, amount, category, type, accountId, datePickerState.selectedDateMillis ?: System.currentTimeMillis()) },
                enabled = !isSaving && title.isNotBlank() && amountValid && accountId.isNotBlank() &&
                    (smartCommand.isBlank() || appliedReviewText == smartCommand) &&
                    (awarenessReview?.takeIf { it.sourceText == smartCommand }?.categoryKnown != false || category.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (isSaving) { if (language == "ar") "جارٍ الحفظ…" else "Saving…" } else words.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(words.cancel) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(words.save) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(words.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatEgp(minor: Long, language: String): String =
    formatMoney(minor, "EGP", 2, language)

private fun formatMoney(minor: Long, currencyCode: String, exponent: Int, language: String): String {
    val locale = if (language == "ar") Locale("ar", "EG") else Locale.US
    return NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(currencyCode)
        minimumFractionDigits = exponent
        maximumFractionDigits = exponent
    }.format(BigDecimal.valueOf(minor, exponent))
}

private fun dateLabel(epoch: Long, language: String): String =
    SimpleDateFormat("dd MMM yyyy", if (language == "ar") Locale("ar") else Locale.US).format(Date(epoch))

private data class Words(
    val greeting: String,
    val netBalance: String,
    val income: String,
    val expenses: String,
    val recent: String,
    val savedEntries: (Int) -> String,
    val addTransaction: String,
    val emptyTitle: String,
    val emptyBody: String,
    val newTransaction: String,
    val transactionName: String,
    val amount: String,
    val amountHint: String,
    val category: String,
    val general: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val confirmDelete: String,
    val confirmDeleteBody: String,
    val chooseAccount: String,
    val edit: String,
    val transactionDate: String
)

private val Arabic = Words(
    "مساء الخير", "صافي الرصيد (EGP)", "الدخل (EGP)", "المصروفات (EGP)", "أحدث العمليات",
    { "عدد العمليات: $it" }, "＋ إضافة عملية", "لا توجد عمليات بعد",
    "أضف أول دخل أو مصروف ليظهر هنا", "عملية مالية جديدة", "اسم العملية",
    "المبلغ بالجنيه", "مثال: 125.50", "التصنيف", "عام", "حفظ العملية",
    "إلغاء", "حذف", "تأكيد الحذف",
    "سيتم حذف هذه العملية نهائيًا من سجل الجهاز.", "اختر الحساب", "تعديل العملية", "تاريخ العملية"
)
private val English = Words(
    "Good evening", "Net balance (EGP)", "Income (EGP)", "Expenses (EGP)", "Recent transactions",
    { "\$it saved transactions" }, "＋ Add transaction", "No transactions yet",
    "Add your first income or expense", "New transaction", "Transaction name",
    "Amount in EGP", "Example: 125.50", "Category", "General", "Save transaction",
    "Cancel", "Delete", "Confirm deletion",
    "This transaction will be permanently deleted from this device.", "Choose account", "Edit transaction", "Transaction date"
)

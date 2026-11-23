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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.creazzy.rofof.domain.model.LedgerTransaction
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF172B2A)
private val Muted = Color(0xFF71817D)
private val Canvas = Color(0xFFF4F7F5)
private val Green = Color(0xFF147D64)
private val Red = Color(0xFFB45B52)

@Composable
fun DashboardScreen(expenseViewModel: ExpenseViewModel = viewModel()) {
    val transactions by expenseViewModel.transactions.collectAsState()
    val categories by expenseViewModel.categories.collectAsState()
    val accounts by expenseViewModel.accounts.collectAsState()
    val managedAccounts by expenseViewModel.managedAccounts.collectAsState()
    val managedCategories by expenseViewModel.managedCategories.collectAsState()
    val totals by expenseViewModel.totals.collectAsState()
    val language by expenseViewModel.language.collectAsState()
    val financeActionMessage by expenseViewModel.financeActionMessage.collectAsState()
    val isArabic = language == "ar"
    val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val words = if (isArabic) Arabic else English
    var showAdd by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<LedgerTransaction?>(null) }

    val income = totals.incomeMinor
    val expenses = totals.expenseMinor
    val net = accounts.asSequence()
        .filter { it.currencyCode == "EGP" && it.currencyExponent == 2 }
        .fold(0L) { total, account -> Math.addExact(total, account.balanceMinor) }
    val categoryNames = categories.associate { it.id to it.name }

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Surface(color = Canvas, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(words.greeting, color = Muted, style = MaterialTheme.typography.labelLarge)
                        Text("ROFOF", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                    FilledTonalButton(
                        onClick = { expenseViewModel.setLanguage(if (isArabic) "en" else "ar") },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White, contentColor = Ink)
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
                    colors = CardDefaults.cardColors(containerColor = Ink)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(Color(0xFF183C36), Color(0xFF126B57))))
                            .padding(20.dp)
                    ) {
                        Text(words.netBalance, color = Color(0xFFB9D6CC), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(formatEgp(net, language), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BalanceMetric(words.income, income, Green, language, Modifier.weight(1f))
                            BalanceMetric(words.expenses, expenses, Red, language, Modifier.weight(1f))
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
                        Text(words.recent, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(words.savedEntries(transactions.size), color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { showAdd = true },
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) { Text(words.addTransaction) }
                }

                Spacer(Modifier.height(12.dp))
                if (transactions.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(66.dp).background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text("＋", color = Green, style = MaterialTheme.typography.headlineLarge) }
                            Spacer(Modifier.height(14.dp))
                            Text(words.emptyTitle, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(words.emptyBody, color = Muted, style = MaterialTheme.typography.bodyMedium)
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
                                onDelete = { pendingDelete = transaction }
                            )
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
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
            onDismiss = { showAdd = false },
            accounts = accounts,
            onSave = { title, amount, category, type, accountId ->
                expenseViewModel.addTransaction(title, amount, category, type, accountId)
                showAdd = false
            }
        )
    }

    pendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(words.confirmDelete, color = Ink, fontWeight = FontWeight.Bold) },
            text = { Text(words.confirmDeleteBody) },
            confirmButton = {
                Button(
                    onClick = {
                        expenseViewModel.deleteTransaction(transaction.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text(words.delete) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(words.cancel) } },
            containerColor = Color.White
        )
    }

    financeActionMessage?.let { message ->
        AlertDialog(
            onDismissRequest = expenseViewModel::clearFinanceActionMessage,
            title = { Text(if (isArabic) "إدارة الحسابات والفئات" else "Account & category management") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = expenseViewModel::clearFinanceActionMessage) {
                    Text(if (isArabic) "حسناً" else "OK")
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun BalanceMetric(label: String, amount: Long, color: Color, language: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(label, color = Color(0xFFD0E5DE), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(formatEgp(amount, language), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TransactionRow(
    transaction: LedgerTransaction,
    categoryName: String,
    language: String,
    deleteLabel: String,
    onDelete: () -> Unit
) {
    val isIncome = transaction.transactionType == "INCOME"
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFE8F3EF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Text(if (isIncome) "＋" else "−", color = if (isIncome) Green else Red, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "$categoryName  ·  ${dateLabel(transaction.occurredAtEpochMillis, language)}",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (isIncome) "+" else "−") + formatMoney(
                        transaction.amountMinor, transaction.currencyCode, transaction.currencyExponent, language
                    ),
                    color = if (isIncome) Green else Red,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDelete, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(deleteLabel, color = Red, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    words: Words,
    accounts: List<com.creazzy.rofof.data.local.AccountBalance>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var accountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var accountMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(words.newTransaction, color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { type = "EXPENSE" },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (type == "EXPENSE") Green else Color(0xFFEAF0ED),
                            contentColor = if (type == "EXPENSE") Color.White else Ink
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text(words.expenses) }
                    FilledTonalButton(
                        onClick = { type = "INCOME" },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (type == "INCOME") Green else Color(0xFFEAF0ED),
                            contentColor = if (type == "INCOME") Color.White else Ink
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text(words.income) }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(words.transactionName) }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(words.amount) }, supportingText = { Text(words.amountHint) }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(words.category) }, singleLine = true)
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
                onClick = { onSave(title, amount, category, type, accountId) },
                enabled = title.isNotBlank() && amount.isNotBlank() && accountId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) { Text(words.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(words.cancel) } },
        containerColor = Color.White
    )
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
    val chooseAccount: String
)

private val Arabic = Words(
    "مساء الخير", "صافي الرصيد (EGP)", "الدخل (EGP)", "المصروفات (EGP)", "أحدث العمليات",
    { "عدد العمليات: $it" }, "＋ إضافة عملية", "لا توجد عمليات بعد",
    "أضف أول دخل أو مصروف ليظهر هنا", "عملية مالية جديدة", "اسم العملية",
    "المبلغ بالجنيه", "مثال: 125.50", "التصنيف", "عام", "حفظ العملية",
    "إلغاء", "حذف", "تأكيد الحذف",
    "سيتم حذف هذه العملية نهائيًا من سجل الجهاز.", "اختر الحساب"
)
private val English = Words(
    "Good evening", "Net balance (EGP)", "Income (EGP)", "Expenses (EGP)", "Recent transactions",
    { "\$it saved transactions" }, "＋ Add transaction", "No transactions yet",
    "Add your first income or expense", "New transaction", "Transaction name",
    "Amount in EGP", "Example: 125.50", "Category", "General", "Save transaction",
    "Cancel", "Delete", "Confirm deletion",
    "This transaction will be permanently deleted from this device.", "Choose account"
)

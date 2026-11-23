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
import androidx.compose.foundation.layout.weight
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
import com.creazzy.rofof.domain.model.Expense
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

@Composable
fun DashboardScreen(expenseViewModel: ExpenseViewModel = viewModel()) {
    val expenses by expenseViewModel.expenses.collectAsState()
    val language by expenseViewModel.language.collectAsState()
    val isArabic = language == "ar"
    val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val words = if (isArabic) Arabic else English
    var showAdd by remember { mutableStateOf(false) }

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

                Spacer(Modifier.height(22.dp))
                val total = expenses.fold(0L) { accumulated, expense -> Math.addExact(accumulated, expense.amountMinor) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Ink)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(Color(0xFF183C36), Color(0xFF126B57))))
                            .padding(22.dp)
                    ) {
                        Column {
                            Text(words.totalExpenses, color = Color(0xFFB9D6CC), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(10.dp))
                            Text(formatEgp(total, language), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(words.localData, color = Color(0xFFD0E5DE), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(words.recent, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(words.savedEntries(expenses.size), color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { showAdd = true },
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) { Text(words.addExpense) }
                }

                Spacer(Modifier.height(12.dp))
                if (expenses.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        items(expenses, key = { it.id }) { expense ->
                            ExpenseRow(expense, language, words.delete) {
                                expenseViewModel.deleteExpense(expense.id)
                            }
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddExpenseDialog(
            words = words,
            onDismiss = { showAdd = false },
            onSave = { title, amount, category ->
                expenseViewModel.addExpense(title, amount, category)
                showAdd = false
            }
        )
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    language: String,
    deleteLabel: String,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFE8F3EF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Text("↗", color = Green, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    expense.category + "  ·  " + dateLabel(expense.occurredAtEpochMillis, language),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatEgp(expense.amountMinor, language), color = Ink, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDelete, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(deleteLabel, color = Color(0xFFB45B52), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(
    words: Words,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(words.newExpense, color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(words.expenseName) }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(words.amount) }, supportingText = { Text(words.amountHint) }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(words.category) }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, amount, category) },
                enabled = title.isNotBlank() && amount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) { Text(words.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(words.cancel) } },
        containerColor = Color.White
    )
}

private fun formatEgp(minor: Long, language: String): String {
    val locale = if (language == "ar") Locale("ar", "EG") else Locale.US
    return NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance("EGP")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(minor, 2))
}

private fun dateLabel(epoch: Long, language: String): String =
    SimpleDateFormat("dd MMM yyyy", if (language == "ar") Locale("ar") else Locale.US).format(Date(epoch))

private data class Words(
    val greeting: String, val totalExpenses: String, val localData: String,
    val recent: String, val savedEntries: (Int) -> String, val addExpense: String,
    val emptyTitle: String, val emptyBody: String, val newExpense: String,
    val expenseName: String, val amount: String, val amountHint: String,
    val category: String, val save: String, val cancel: String, val delete: String
)

private val Arabic = Words(
    "مساء الخير", "إجمالي المصروفات", "بياناتك محفوظة على هذا الجهاز فقط",
    "أحدث المصروفات", { "عدد العمليات: $it" }, "＋ إضافة مصروف",
    "لا توجد مصروفات بعد", "أضف أول عملية لتظهر هنا تلقائياً",
    "مصروف جديد", "اسم المصروف", "المبلغ بالجنيه", "مثال: 125.50",
    "التصنيف", "حفظ العملية", "إلغاء", "حذف"
)
private val English = Words(
    "Good evening", "Total expenses", "Your data stays on this device",
    "Recent expenses", { "$it saved entries" }, "＋ Add expense",
    "No expenses yet", "Add your first entry and it will appear here",
    "New expense", "Expense name", "Amount in EGP", "Example: 125.50",
    "Category", "Save expense", "Cancel", "Delete"
)

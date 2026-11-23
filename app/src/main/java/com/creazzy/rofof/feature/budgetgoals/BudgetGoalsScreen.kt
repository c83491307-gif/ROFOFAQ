package com.creazzy.rofof.feature.budgetgoals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.creazzy.rofof.data.local.BudgetProgress
import com.creazzy.rofof.data.local.FinancialGoalEntity
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.AccountBalance
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val PlanInk = Color(0xFF172B2A)
private val PlanGreen = Color(0xFF147D64)
private val PlanMuted = Color(0xFF71817D)
private val PlanCanvas = Color(0xFFF4F7F5)

@Composable
fun BudgetGoalsScreen(
    onBack: () -> Unit,
    model: BudgetGoalViewModel = viewModel()
) {
    val budgets by model.budgets.collectAsState()
    val goals by model.goals.collectAsState()
    val categories by model.categories.collectAsState()
    val accounts by model.accounts.collectAsState()
    val language by model.language.collectAsState()
    val error by model.error.collectAsState()
    val busy by model.busy.collectAsState()
    val ar = language == "ar"
    var showBudget by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetProgress?>(null) }
    var showGoal by remember { mutableStateOf(false) }
    var progressGoal by remember { mutableStateOf<FinancialGoalEntity?>(null) }
    val categoryNames = categories.associate { it.id to it.name }

    CompositionLocalProvider(LocalLayoutDirection provides if (ar) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Surface(color = PlanCanvas, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text(if (ar) "رجوع" else "Back") }
                    Column(Modifier.weight(1f)) {
                        Text(if (ar) "التخطيط المالي" else "Financial planning", color = PlanMuted)
                        Text(if (ar) "الميزانيات والأهداف" else "Budgets & goals", style = MaterialTheme.typography.headlineSmall, color = PlanInk, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { showBudget = true; editingBudget = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PlanGreen)) {
                        Text(if (ar) "＋ ميزانية" else "＋ Budget")
                    }
                    Button(onClick = { showGoal = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PlanInk)) {
                        Text(if (ar) "＋ هدف مالي" else "＋ Goal")
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    item {
                        Text(if (ar) "ميزانيات الشهر الحالي" else "Current-month budgets", color = PlanInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    if (budgets.isEmpty()) item {
                        Text(if (ar) "لا توجد ميزانيات لهذا الشهر. أضف ميزانية حسب فئة المصروفات." else "No budgets for this month. Add one for an expense category.", color = PlanMuted)
                    }
                    items(budgets, key = { it.id }) { budget ->
                        BudgetCard(
                            budget = budget,
                            categoryName = categoryNames[budget.categoryId] ?: if (ar) "فئة مؤرشفة" else "Archived category",
                            language = language,
                            onEdit = { editingBudget = budget; showBudget = true },
                            onDelete = { model.deleteBudget(budget.id) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(if (ar) "الأهداف المالية" else "Financial goals", color = PlanInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    if (goals.isEmpty()) item {
                        Text(if (ar) "لا توجد أهداف بعد. أنشئ هدف ادخار وتابع تقدمه." else "No goals yet. Create a savings goal and track progress.", color = PlanMuted)
                    }
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            language = language,
                            onProgress = { progressGoal = goal },
                            onArchive = { model.archiveGoal(goal.id) }
                        )
                    }
                    item { Spacer(Modifier.height(18.dp)) }
                }
            }
        }
    }

    if (showBudget) BudgetDialog(
        categories = categories,
        accounts = accounts,
        language = language,
        initial = editingBudget,
        busy = busy,
        onDismiss = { showBudget = false; editingBudget = null },
        onSave = { id, categoryId, amount, currency, exponent ->
            model.saveBudget(id, categoryId, amount, currency, exponent)
            showBudget = false
            editingBudget = null
        }
    )
    if (showGoal) GoalDialog(
        accounts = accounts,
        language = language,
        busy = busy,
        onDismiss = { showGoal = false },
        onSave = { title, target, currency, exponent, deadline ->
            model.createGoal(title, target, currency, exponent, deadline)
            showGoal = false
        }
    )
    progressGoal?.let { goal ->
        ProgressDialog(
            goal = goal,
            language = language,
            busy = busy,
            onDismiss = { progressGoal = null },
            onSave = { amount ->
                model.updateGoalProgress(goal.id, amount, goal.currencyExponent)
                progressGoal = null
            }
        )
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = model::clearError,
            title = { Text(if (ar) "تعذر تنفيذ العملية" else "Action could not be completed") },
            text = { Text(localizeError(message, ar)) },
            confirmButton = { TextButton(onClick = model::clearError) { Text(if (ar) "حسناً" else "OK") } }
        )
    }
}

@Composable
private fun BudgetCard(
    budget: BudgetProgress,
    categoryName: String,
    language: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ar = language == "ar"
    val ratio = if (budget.amountMinor <= 0L) 0f else (budget.spentMinor.toDouble() / budget.amountMinor).toFloat().coerceIn(0f, 1f)
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(categoryName, color = PlanInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text(if (ar) "تعديل" else "Edit") }
                TextButton(onClick = onDelete) { Text(if (ar) "حذف" else "Delete", color = Color(0xFFB45B52)) }
            }
            Text(
                (if (ar) "المصروف " else "Spent ") + money(budget.spentMinor, budget.currencyCode, budget.currencyExponent, language) +
                    " / " + money(budget.amountMinor, budget.currencyCode, budget.currencyExponent, language),
                color = PlanMuted
            )
            LinearProgressIndicator(progress = ratio, modifier = Modifier.fillMaxWidth(), color = if (budget.spentMinor > budget.amountMinor) Color(0xFFB45B52) else PlanGreen)
            val remaining = budget.amountMinor - budget.spentMinor
            Text(
                if (remaining >= 0L) (if (ar) "المتبقي " else "Remaining ") + money(remaining, budget.currencyCode, budget.currencyExponent, language)
                else (if (ar) "تجاوزت الميزانية بمقدار " else "Over budget by ") + money(-remaining, budget.currencyCode, budget.currencyExponent, language),
                color = if (remaining < 0L) Color(0xFFB45B52) else PlanGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: FinancialGoalEntity,
    language: String,
    onProgress: () -> Unit,
    onArchive: () -> Unit
) {
    val ar = language == "ar"
    val ratio = (goal.currentAmountMinor.toDouble() / goal.targetAmountMinor.toDouble()).toFloat().coerceIn(0f, 1f)
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(goal.title, color = PlanInk, fontWeight = FontWeight.SemiBold)
            Text(
                money(goal.currentAmountMinor, goal.currencyCode, goal.currencyExponent, language) + " / " +
                    money(goal.targetAmountMinor, goal.currencyCode, goal.currencyExponent, language),
                color = PlanMuted
            )
            LinearProgressIndicator(progress = ratio, modifier = Modifier.fillMaxWidth(), color = PlanGreen)
            if (goal.deadlineEpochMillis != null) Text(
                (if (ar) "الموعد المستهدف: " else "Target date: ") + java.text.SimpleDateFormat("dd MMM yyyy", if (ar) Locale("ar") else Locale.US).format(java.util.Date(goal.deadlineEpochMillis)),
                color = PlanMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onProgress) { Text(if (ar) "تحديث المدخرات" else "Update saved") }
                TextButton(onClick = onArchive) { Text(if (ar) "أرشفة" else "Archive", color = Color(0xFFB45B52)) }
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    categories: List<CategoryEntity>,
    accounts: List<AccountBalance>,
    language: String,
    initial: BudgetProgress?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, Int) -> Unit
) {
    val ar = language == "ar"
    var categoryId by remember(initial?.id, categories) { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id.orEmpty()) }
    var amount by remember(initial?.id) { mutableStateOf(initial?.let { BigDecimal.valueOf(it.amountMinor, it.currencyExponent).toPlainString() }.orEmpty()) }
    var currency by remember(initial?.id, accounts) { mutableStateOf(initial?.currencyCode ?: accounts.firstOrNull()?.currencyCode ?: "EGP") }
    var categoryMenu by remember { mutableStateOf(false) }
    var currencyMenu by remember { mutableStateOf(false) }
    val exponent = initial?.currencyExponent ?: accounts.firstOrNull { it.currencyCode == currency }?.currencyExponent ?: 2
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) { if (ar) "ميزانية شهرية جديدة" else "New monthly budget" } else { if (ar) "تعديل الميزانية" else "Edit budget" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { categoryMenu = true }, enabled = initial == null) {
                    Text(categories.firstOrNull { it.id == categoryId }?.name ?: if (ar) "اختر فئة مصروف" else "Choose expense category")
                }
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    categories.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { categoryId = item.id; categoryMenu = false }) }
                }
                TextButton(onClick = { currencyMenu = true }, enabled = initial == null) { Text(currency) }
                DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                    accounts.distinctBy { it.currencyCode }.forEach { account ->
                        DropdownMenuItem(text = { Text(account.currencyCode) }, onClick = { currency = account.currencyCode; currencyMenu = false })
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(if (ar) "حد الميزانية" else "Budget limit") }, singleLine = true)
                Text(if (ar) "يحسب الإنفاق من عمليات المصروفات المسجلة لهذه الفئة خلال الشهر الحالي فقط." else "Spending is calculated from recorded expenses in this category for the current month.", color = PlanMuted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onSave(initial?.id, categoryId, amount, currency, exponent) }, enabled = !busy && categoryId.isNotBlank() && amount.isNotBlank()) { Text(if (ar) "حفظ" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

@Composable
private fun GoalDialog(
    accounts: List<AccountBalance>,
    language: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String) -> Unit
) {
    val ar = language == "ar"
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var currency by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.currencyCode ?: "EGP") }
    var currencyMenu by remember { mutableStateOf(false) }
    val exponent = accounts.firstOrNull { it.currencyCode == currency }?.currencyExponent ?: 2
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "هدف مالي جديد" else "New financial goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (ar) "اسم الهدف" else "Goal name") }, singleLine = true)
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text(if (ar) "المبلغ المستهدف" else "Target amount") }, singleLine = true)
                TextButton(onClick = { currencyMenu = true }) { Text(currency) }
                DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                    accounts.distinctBy { it.currencyCode }.forEach { account ->
                        DropdownMenuItem(text = { Text(account.currencyCode) }, onClick = { currency = account.currencyCode; currencyMenu = false })
                    }
                }
                OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text(if (ar) "الموعد (YYYY-MM-DD، اختياري)" else "Deadline (YYYY-MM-DD, optional)") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(title, target, currency, exponent, deadline) }, enabled = !busy && title.isNotBlank() && target.isNotBlank()) { Text(if (ar) "إنشاء" else "Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

@Composable
private fun ProgressDialog(
    goal: FinancialGoalEntity,
    language: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val ar = language == "ar"
    var amount by remember(goal.id) { mutableStateOf(BigDecimal.valueOf(goal.currentAmountMinor, goal.currencyExponent).toPlainString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "تحديث المدخرات" else "Update saved amount") },
        text = { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(if (ar) "المبلغ المدخر حالياً" else "Current saved amount") }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(amount) }, enabled = !busy && amount.isNotBlank()) { Text(if (ar) "حفظ" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

private fun money(minor: Long, code: String, exponent: Int, language: String): String =
    NumberFormat.getCurrencyInstance(if (language == "ar") Locale("ar", "EG") else Locale.US).apply {
        currency = Currency.getInstance(code)
        minimumFractionDigits = exponent
        maximumFractionDigits = exponent
    }.format(BigDecimal.valueOf(minor, exponent))

private fun localizeError(message: String, ar: Boolean): String {
    if (!ar) return message
    return when {
        message.contains("Budget amount must be positive") -> "يجب أن يكون حد الميزانية أكبر من صفر."
        message.contains("Budget period is invalid") -> "فترة الميزانية غير صحيحة."
        message.contains("active expense category") -> "اختر فئة مصروفات نشطة."
        message.contains("UNIQUE constraint failed") -> "توجد ميزانية بالفعل لهذه الفئة والعملة في هذا الشهر."
        message.contains("Goal title is required") -> "اسم الهدف مطلوب."
        message.contains("Goal target must be positive") -> "المبلغ المستهدف يجب أن يكون أكبر من صفر."
        message.contains("Invalid") || message.contains("Unknown") || message.contains("Currency") -> "تحقق من العملة وعدد الخانات العشرية."
        else -> "تعذر تنفيذ العملية: $message"
    }
}

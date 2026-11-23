package com.creazzy.rofof.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.creazzy.rofof.data.local.AccountBalance
import com.creazzy.rofof.data.local.CategoryEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun AccountManagerDialog(
    accounts: List<AccountBalance>,
    language: String,
    onCreate: (String, String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val ar = language == "ar"
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("EGP") }
    var exponent by remember { mutableStateOf("2") }
    var opening by remember { mutableStateOf("0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "الحسابات والأرصدة" else "Accounts & balances") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                LazyColumn(modifier = Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(accounts, key = { it.id }) { account ->
                        Column {
                            Text(account.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                formatAccountMoney(account.balanceMinor, account.currencyCode, account.currencyExponent, language),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text(if (ar) "اسم الحساب" else "Account name") }, singleLine = true)
                OutlinedTextField(currency, { currency = it.uppercase(Locale.ROOT) }, label = { Text(if (ar) "رمز العملة" else "Currency code") }, singleLine = true)
                OutlinedTextField(exponent, { exponent = it.filter(Char::isDigit).take(1) }, label = { Text(if (ar) "الخانات العشرية (0–4)" else "Currency exponent (0–4)") }, singleLine = true)
                OutlinedTextField(opening, { opening = it }, label = { Text(if (ar) "الرصيد الافتتاحي" else "Opening balance") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, currency, exponent.toIntOrNull() ?: 2, opening) },
                enabled = name.isNotBlank() && currency.matches(Regex("[A-Z]{3}")) && ((exponent.toIntOrNull() ?: -1) in 0..4),
            ) { Text(if (ar) "إضافة الحساب" else "Add account") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إغلاق" else "Close") } }
    )
}

@Composable
fun TransferDialog(
    accounts: List<AccountBalance>,
    language: String,
    onTransfer: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val ar = language == "ar"
    var sourceId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var destinationId by remember(accounts) {
        val first = accounts.firstOrNull()
        mutableStateOf(first?.let { source ->
            accounts.firstOrNull {
                it.id != source.id && it.currencyCode == source.currencyCode &&
                    it.currencyExponent == source.currencyExponent
            }?.id.orEmpty()
        }.orEmpty())
    }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var sourceExpanded by remember { mutableStateOf(false) }
    var destinationExpanded by remember { mutableStateOf(false) }
    val source = accounts.firstOrNull { it.id == sourceId }
    val compatible = accounts.filter {
        it.id != sourceId && source != null &&
            it.currencyCode == source.currencyCode && it.currencyExponent == source.currencyExponent
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "تحويل بين الحسابات" else "Transfer between accounts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(if (ar) "من الحساب" else "From account")
                TextButton(onClick = { sourceExpanded = true }) { Text(source?.name ?: if (ar) "اختر الحساب" else "Choose account") }
                DropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name + " · " + account.currencyCode) },
                            onClick = {
                                sourceId = account.id
                                destinationId = accounts.firstOrNull {
                                    it.id != account.id && it.currencyCode == account.currencyCode &&
                                        it.currencyExponent == account.currencyExponent
                                }?.id.orEmpty()
                                sourceExpanded = false
                            }
                        )
                    }
                }
                Text(if (ar) "إلى الحساب" else "To account")
                TextButton(onClick = { destinationExpanded = true }, enabled = compatible.isNotEmpty()) {
                    Text(accounts.firstOrNull { it.id == destinationId }?.name ?: if (ar) "اختر حسابًا بنفس العملة" else "Choose same-currency account")
                }
                DropdownMenu(expanded = destinationExpanded, onDismissRequest = { destinationExpanded = false }) {
                    compatible.forEach { account ->
                        DropdownMenuItem(text = { Text(account.name) }, onClick = {
                            destinationId = account.id
                            destinationExpanded = false
                        })
                    }
                }
                OutlinedTextField(amount, { amount = it }, label = { Text(if (ar) "المبلغ" else "Amount") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text(if (ar) "ملاحظة (اختياري)" else "Note (optional)") }, singleLine = true)
                if (compatible.isEmpty()) {
                    Text(if (ar) "أنشئ حسابًا آخر بنفس العملة لإجراء التحويل." else "Create another account in the same currency to transfer.")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onTransfer(sourceId, destinationId, amount, note) },
                enabled = sourceId.isNotBlank() && destinationId.isNotBlank() && amount.isNotBlank() && compatible.any { it.id == destinationId }
            ) { Text(if (ar) "تأكيد التحويل" else "Confirm transfer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    language: String,
    onCreate: (String, String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val ar = language == "ar"
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var parentExpanded by remember { mutableStateOf(false) }
    val parents = categories.filter { it.parentId == null && it.transactionType == type }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "الفئات والفئات الفرعية" else "Categories & subcategories") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(categories, key = { it.id }) { category ->
                        Text(
                            (if (category.parentId == null) "• " else "   ↳ ") + category.name +
                                if (ar) (if (category.transactionType == "INCOME") " · دخل" else " · مصروف") else
                                    if (category.transactionType == "INCOME") " · Income" else " · Expense",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { type = "EXPENSE"; parentId = null },
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) { Text(if (ar) "مصروف" else "Expense") }
                    FilledTonalButton(
                        onClick = { type = "INCOME"; parentId = null },
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) { Text(if (ar) "دخل" else "Income") }
                }
                OutlinedTextField(name, { name = it }, label = { Text(if (ar) "اسم الفئة الجديدة" else "New category name") }, singleLine = true)
                TextButton(onClick = { parentExpanded = true }) {
                    Text(
                        if (parentId == null) {
                            if (ar) "فئة رئيسية" else "Top-level category"
                        } else {
                            parents.firstOrNull { it.id == parentId }?.name.orEmpty()
                        }
                    )
                }
                DropdownMenu(expanded = parentExpanded, onDismissRequest = { parentExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (ar) "فئة رئيسية" else "Top-level category") },
                        onClick = { parentId = null; parentExpanded = false }
                    )
                    parents.forEach { parent ->
                        DropdownMenuItem(text = { Text(parent.name) }, onClick = {
                            parentId = parent.id
                            parentExpanded = false
                        })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, type, parentId) }, enabled = name.isNotBlank()) {
                Text(if (ar) "إضافة الفئة" else "Add category")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إغلاق" else "Close") } }
    )
}

private fun formatAccountMoney(minor: Long, code: String, exponent: Int, language: String): String {
    val locale = if (language == "ar") Locale("ar", "EG") else Locale.US
    return NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(code)
        minimumFractionDigits = exponent
        maximumFractionDigits = exponent
    }.format(BigDecimal.valueOf(minor, exponent))
}

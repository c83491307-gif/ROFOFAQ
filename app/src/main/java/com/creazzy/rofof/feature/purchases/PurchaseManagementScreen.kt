package com.creazzy.rofof.feature.purchases

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.creazzy.rofof.data.local.PurchaseItemEntity
import com.creazzy.rofof.data.local.PurchaseSessionEntity
import java.math.BigDecimal
import java.math.RoundingMode

private val PurchaseGreen = Color(0xFF147D64)
private val PurchaseInk = Color(0xFF172B2A)

@Composable
fun PurchaseManagementScreen(
    onBack: () -> Unit,
    viewModel: PurchaseViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedId by viewModel.selectedSessionId.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val items by viewModel.items.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val allocations by viewModel.allocationViews.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val language by viewModel.language.collectAsState()
    val error by viewModel.error.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val ar = language == "ar"
    val session = sessions.firstOrNull { it.id == selectedId }
    var showCreate by remember { mutableStateOf(false) }
    var showParticipant by remember { mutableStateOf(false) }
    var showItem by remember { mutableStateOf(false) }
    var allocateItem by remember { mutableStateOf<PurchaseItemEntity?>(null) }
    var paymentParticipant by remember { mutableStateOf<String?>(null) }
    var expenseAllocation by remember { mutableStateOf<String?>(null) }

    Surface(color = Color(0xFFF4F7F5), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (selectedId != null) viewModel.selectSession(null) else onBack() }) {
                    Text(if (ar) "رجوع" else "Back")
                }
                Text("ROFOF", style = MaterialTheme.typography.titleLarge, color = PurchaseInk)
            }
            Text(if (ar) "المشتريات المشتركة" else "Shared purchases",
                style = MaterialTheme.typography.headlineSmall, color = PurchaseInk)
            Spacer(Modifier.height(10.dp))

            if (session == null) {
                Button(onClick = { showCreate = true }, enabled = !busy) {
                    Text(if (ar) "إنشاء مجموعة شراء" else "Create purchase")
                }
                Spacer(Modifier.height(10.dp))
                if (sessions.isEmpty()) {
                    Text(if (ar) "لا توجد مجموعات شراء بعد." else "No shared purchases yet.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sessions, key = { it.id }) { value ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(value.title, color = PurchaseInk, style = MaterialTheme.typography.titleMedium)
                                        Text("${value.currencyCode} · ${value.status}", color = Color.Gray)
                                    }
                                    TextButton(onClick = { viewModel.selectSession(value.id) }) {
                                        Text(if (ar) "فتح" else "Open")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                PurchaseDetail(
                    session = session,
                    participants = participants,
                    items = items,
                    settlements = settlements,
                    allocations = allocations,
                    language = language,
                    busy = busy,
                    onAddParticipant = { showParticipant = true },
                    onAddItem = { showItem = true },
                    onAllocate = { allocateItem = it },
                    onPayment = { paymentParticipant = it },
                    onConvert = { expenseAllocation = it },
                    onClose = viewModel::closeAsSettled
                )
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error ?: "", color = Color(0xFFB45B52), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = viewModel::clearError) { Text(if (ar) "إخفاء" else "Dismiss") }
            }
        }
    }

    if (showCreate) SimpleEntryDialog(
        title = if (ar) "مجموعة شراء جديدة" else "New purchase",
        label = if (ar) "اسم المجموعة" else "Purchase name",
        confirm = if (ar) "إنشاء" else "Create",
        onDismiss = { showCreate = false },
        onConfirm = { viewModel.createSession(it); showCreate = false }
    )
    if (showParticipant) SimpleEntryDialog(
        title = if (ar) "إضافة مشارك" else "Add participant",
        label = if (ar) "اسم المشارك" else "Participant name",
        confirm = if (ar) "إضافة" else "Add",
        onDismiss = { showParticipant = false },
        onConfirm = { viewModel.addParticipant(it); showParticipant = false }
    )
    if (showItem) ItemEntryDialog(ar, onDismiss = { showItem = false }) { title, quantity, price ->
        viewModel.addItem(title, quantity, price)
        showItem = false
    }
    allocateItem?.let { item ->
        AllocationEntryDialog(item, participants, session?.currencyExponent ?: 2, ar,
            onDismiss = { allocateItem = null },
            onConfirm = { shares -> viewModel.allocate(item.id, shares); allocateItem = null })
    }
    paymentParticipant?.let { participantId ->
        SimpleEntryDialog(
            title = if (ar) "تسجيل دفعة فعلية" else "Record actual payment",
            label = if (ar) "المبلغ" else "Amount",
            confirm = if (ar) "تسجيل" else "Record",
            onDismiss = { paymentParticipant = null },
            onConfirm = { viewModel.recordPayment(participantId, it, "CASH"); paymentParticipant = null }
        )
    }
    expenseAllocation?.let { allocationId ->
        PersonalExpenseDialog(
            accounts = accounts.filter { !it.isArchived },
            categories = categories.filter { !it.isArchived && it.transactionType == "EXPENSE" },
            language = language,
            onDismiss = { expenseAllocation = null },
            onConfirm = { accountId, categoryId ->
                viewModel.recordAsPersonalExpense(allocationId, accountId, categoryId)
                expenseAllocation = null
            }
        )
    }
}

@Composable
private fun PurchaseDetail(
    session: PurchaseSessionEntity,
    participants: List<com.creazzy.rofof.data.local.PurchaseParticipantEntity>,
    items: List<PurchaseItemEntity>,
    settlements: List<com.creazzy.rofof.data.local.ParticipantSettlementRow>,
    allocations: List<com.creazzy.rofof.data.local.PurchaseAllocationView>,
    language: String,
    busy: Boolean,
    onAddParticipant: () -> Unit,
    onAddItem: () -> Unit,
    onAllocate: (PurchaseItemEntity) -> Unit,
    onPayment: (String) -> Unit,
    onConvert: (String) -> Unit,
    onClose: () -> Unit
) {
    val ar = language == "ar"
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(session.title, style = MaterialTheme.typography.titleLarge, color = PurchaseInk)
            Text("${session.currencyCode} · ${if (session.status == "OPEN") if (ar) "مفتوحة" else "Open" else if (ar) "مغلقة" else "Settled"}",
                color = Color.Gray)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddParticipant, enabled = session.status == "OPEN" && !busy) {
                    Text(if (ar) "مشارك +" else "Participant +")
                }
                Button(onClick = onAddItem, enabled = session.status == "OPEN" && !busy) {
                    Text(if (ar) "إضافة صنف" else "Add item")
                }
            }
        }
        item { Text(if (ar) "المشاركون" else "Participants", style = MaterialTheme.typography.titleMedium) }
        items(participants, key = { "p-${it.id}" }) { p ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.displayName, color = PurchaseInk)
                    TextButton(onClick = { onPayment(p.id) }, enabled = session.status == "OPEN") {
                        Text(if (ar) "تسجيل دفعة" else "Add payment")
                    }
                }
            }
        }
        item { Text(if (ar) "الأصناف والتوزيع" else "Items & allocations", style = MaterialTheme.typography.titleMedium) }
        items(items, key = { "i-${it.id}" }) { item ->
            val allocated = allocations.filter { it.itemTitle == item.title }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(item.title, color = PurchaseInk, style = MaterialTheme.typography.titleSmall)
                    Text("${money(item.totalMinor, session.currencyExponent)} ${session.currencyCode}",
                        color = PurchaseGreen)
                    if (allocated.isNotEmpty()) {
                        allocated.forEach { a ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${a.participantName}: ${money(a.amountMinor, session.currencyExponent)}")
                                if (a.personalTransactionId == null) {
                                    TextButton(onClick = { onConvert(a.allocationId) }) {
                                        Text(if (ar) "إضافة لمصروفاتي" else "Add to My Expenses")
                                    }
                                } else {
                                    Text(if (ar) "مضاف للمصروفات" else "In My Expenses", color = PurchaseGreen)
                                }
                            }
                        }
                    }
                    if (session.status == "OPEN" && allocated.isEmpty()) {
                        TextButton(onClick = { onAllocate(item) }, enabled = participants.isNotEmpty()) {
                            Text(if (ar) "توزيع الحصة" else "Allocate shares")
                        }
                    }
                }
            }
        }
        item { Text(if (ar) "التسويات والأرصدة" else "Settlements & balances", style = MaterialTheme.typography.titleMedium) }
        items(settlements, key = { "s-${it.participantId}" }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(row.displayName, color = PurchaseInk)
                        Text(if (ar) "المستحق: ${money(row.allocatedMinor, session.currencyExponent)} · المدفوع: ${money(row.paidMinor, session.currencyExponent)}"
                            else "Share: ${money(row.allocatedMinor, session.currencyExponent)} · Paid: ${money(row.paidMinor, session.currencyExponent)}",
                            color = Color.Gray)
                    }
                    Text(money(row.balanceMinor, session.currencyExponent), color = if (row.balanceMinor > 0) Color(0xFFB45B52) else PurchaseGreen)
                }
            }
        }
        if (session.status == "OPEN") item {
            Button(onClick = onClose, enabled = !busy && items.isNotEmpty() && items.all { item -> allocations.any { it.itemTitle == item.title } }) {
                Text(if (ar) "إغلاق بعد التسوية" else "Close as settled")
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SimpleEntryDialog(
    title: String, label: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { Button(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ItemEntryDialog(
    ar: Boolean, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "إضافة صنف" else "Add item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(if (ar) "الصنف" else "Item") })
                OutlinedTextField(quantity, { quantity = it }, label = { Text(if (ar) "الكمية" else "Quantity") })
                OutlinedTextField(price, { price = it }, label = { Text(if (ar) "سعر الوحدة" else "Unit price") })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(title, quantity, price) }, enabled = title.isNotBlank() && quantity.isNotBlank() && price.isNotBlank()) { Text(if (ar) "حفظ" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

@Composable
private fun AllocationEntryDialog(
    item: PurchaseItemEntity,
    participants: List<com.creazzy.rofof.data.local.PurchaseParticipantEntity>,
    exponent: Int,
    ar: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    val values = remember(item.id, participants) { mutableStateMapOf<String, String>().apply { participants.forEach { put(it.id, "") } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "توزيع ${item.title}" else "Allocate ${item.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (ar) "أدخل نصيب كل مشارك. يجب أن يساوي المجموع إجمالي الصنف بدقة."
                    else "Enter each participant share. The sum must exactly match the item total.")
                participants.forEach { p ->
                    OutlinedTextField(
                        value = values[p.id].orEmpty(),
                        onValueChange = { values[p.id] = it },
                        label = { Text(p.displayName) },
                        singleLine = true
                    )
                }
                Text("${money(item.totalMinor, exponent)}", color = PurchaseGreen)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(values.toMap()) }, enabled = values.values.any { it.isNotBlank() }) { Text(if (ar) "حفظ التوزيع" else "Save allocation") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

@Composable
private fun PersonalExpenseDialog(
    accounts: List<com.creazzy.rofof.data.local.AccountBalance>,
    categories: List<com.creazzy.rofof.data.local.CategoryEntity>,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val ar = language == "ar"
    var accountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var categoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id.orEmpty()) }
    var accountMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "تأكيد إضافة الحصة للمصروفات" else "Confirm personal expense") },
        text = {
            Column {
                Text(if (ar) "سيُنشأ مصروف شخصي مرتبط بهذه الحصة دون تكرارها تلقائيًا."
                    else "A personal ledger expense will be created and linked to this share.")
                TextButton(onClick = { accountMenu = true }) {
                    Text(accounts.firstOrNull { it.id == accountId }?.name ?: if (ar) "اختر الحساب" else "Choose account")
                }
                DropdownMenu(accountMenu, { accountMenu = false }) {
                    accounts.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { accountId = a.id; accountMenu = false }) }
                }
                TextButton(onClick = { categoryMenu = true }) {
                    Text(categories.firstOrNull { it.id == categoryId }?.name ?: if (ar) "اختر الفئة" else "Choose category")
                }
                DropdownMenu(categoryMenu, { categoryMenu = false }) {
                    categories.forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { categoryId = c.id; categoryMenu = false }) }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(accountId, categoryId) }, enabled = accountId.isNotBlank() && categoryId.isNotBlank()) { Text(if (ar) "تأكيد" else "Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

private fun money(minor: Long, exponent: Int): String =
    BigDecimal.valueOf(minor, exponent).setScale(exponent, RoundingMode.UNNECESSARY).toPlainString()

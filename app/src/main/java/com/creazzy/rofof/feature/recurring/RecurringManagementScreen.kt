package com.creazzy.rofof.feature.recurring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.creazzy.rofof.data.local.RecurringDraftEntity
import com.creazzy.rofof.data.local.RecurringRuleEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.repository.RecurringDraftApprovalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.util.UUID

class RecurringViewModel(application: Application) : AndroidViewModel(application) {
    private val db = RofofDatabase.getInstance(application)
    private val dao = db.recurringDao()
    private val approvalRepository = RecurringDraftApprovalRepository(db)
    val actionMessage = MutableStateFlow("")
    val rules = dao.observeRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val drafts = dao.observeDrafts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun approveDraft(draftId: String) {
        viewModelScope.launch {
            actionMessage.value = runCatching {
                approvalRepository.approve(draftId, System.currentTimeMillis())
                "تم اعتماد المسودة وتسجيل المعاملة · Draft approved and posted"
            }.getOrElse { "تعذر الاعتماد: " + (it.message ?: "خطأ غير معروف") }
        }
    }

    fun skipDraft(draftId: String) {
        viewModelScope.launch {
            actionMessage.value = if (approvalRepository.skip(draftId)) {
                "تم تخطي المسودة · Draft skipped"
            } else {
                "المسودة لم تعد معلقة · Draft is no longer pending"
            }
        }
    }

    fun addMonthlyExpense(title: String, amount: String, dayOfMonth: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        val requestedDay = dayOfMonth.toIntOrNull()?.takeIf { it in 1..31 } ?: return
        viewModelScope.launch {
            val account = db.accountDao().findById("default-cash") ?: return@launch
            val category = db.categoryDao().findById("default-expense-general") ?: return@launch
            val minor = runCatching {
                BigDecimal(amount.trim().replace(',', '.'))
                    .setScale(account.currencyExponent, RoundingMode.UNNECESSARY)
                    .movePointRight(account.currencyExponent).longValueExact()
            }.getOrNull()?.takeIf { it > 0L } ?: return@launch
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val targetMonth = if (today.dayOfMonth <= requestedDay) YearMonth.from(today) else YearMonth.from(today).plusMonths(1)
            val firstRun = targetMonth.atDay(minOf(requestedDay, targetMonth.lengthOfMonth()))
                .atStartOfDay(zone).toInstant().toEpochMilli()
            dao.insertRule(
                RecurringRuleEntity(
                    id = UUID.randomUUID().toString(), title = cleanTitle,
                    transactionType = "EXPENSE", amountMinor = minor,
                    currencyCode = account.currencyCode, currencyExponent = account.currencyExponent,
                    accountId = account.id, categoryId = category.id,
                    intervalType = "MONTHLY", intervalCount = 1,
                    nextRunEpochMillis = firstRun, endEpochMillis = null, isActive = true,
                    createdAtEpochMillis = now,
                    dayOfMonth = requestedDay
                )
            )
        }
    }

    private fun nextMonthly(epochMillis: Long, count: Int): Long {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val targetMonth = YearMonth.from(current).plusMonths(count.toLong())
        val targetDay = minOf(current.dayOfMonth, targetMonth.lengthOfMonth())
        return targetMonth.atDay(targetDay).atTime(current.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
    }

    fun generateDueDrafts(nowEpochMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            db.withTransaction {
                dao.findDueRules(nowEpochMillis).forEach { rule ->
                    var scheduled = rule.nextRunEpochMillis
                    var next = scheduled
                    var generated = 0
                    while (next <= nowEpochMillis && generated < 1200) {
                        if (rule.endEpochMillis == null || scheduled <= rule.endEpochMillis) {
                            dao.insertDraftIfAbsent(
                                RecurringDraftEntity(
                                    id = UUID.randomUUID().toString(), ruleId = rule.id,
                                    scheduledForEpochMillis = scheduled, title = rule.title,
                                    transactionType = rule.transactionType, amountMinor = rule.amountMinor,
                                    currencyCode = rule.currencyCode, currencyExponent = rule.currencyExponent,
                                    accountId = rule.accountId, categoryId = rule.categoryId,
                                    status = "DRAFT", createdAtEpochMillis = nowEpochMillis
                                )
                            )
                        }
                        next = nextMonthly(scheduled, rule.intervalCount)
                        scheduled = next
                        generated++
                    }
                    val active = (rule.endEpochMillis == null || next <= rule.endEpochMillis)
                    dao.updateRule(rule.copy(nextRunEpochMillis = next, isActive = active))
                }
            }
        }
    }
}

@Composable
fun RecurringManagementScreen(onBack: () -> Unit) {
    val vm: RecurringViewModel = viewModel()
    val rules by vm.rules.collectAsState()
    val drafts by vm.drafts.collectAsState()
    val actionMessage by vm.actionMessage.collectAsState()
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dayOfMonth by remember { mutableStateOf("1") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("المعاملات المتكررة · Recurring")
            Button(onClick = onBack) { Text("رجوع · Back") }
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("الاسم · Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("المبلغ · Amount (EGP)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = dayOfMonth, onValueChange = { dayOfMonth = it.filter(Char::isDigit).take(2) }, label = { Text("يوم الشهر · Day (1–31)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.addMonthlyExpense(title, amount, dayOfMonth); title = ""; amount = "" }, enabled = title.isNotBlank() && amount.isNotBlank() && (dayOfMonth.toIntOrNull()?.let { it in 1..31 } == true)) {
            Text("إضافة قاعدة شهرية · Add monthly rule")
        }
        Text(
            "تُنشأ مسودات للمراجعة قبل التسجيل · Due items are drafted for review before posting",
            modifier = Modifier.fillMaxWidth()
        )
        if (actionMessage.isNotBlank()) Text(actionMessage)
        Text("القواعد · Rules (${rules.size})")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rules, key = { it.id }) { rule ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(rule.title)
                        Text("EGP ${BigDecimal.valueOf(rule.amountMinor, rule.currencyExponent).toPlainString()} · ${rule.intervalType} · Day ${rule.dayOfMonth}")
                        Text(if (rule.isActive) "Active" else "Paused")
                    }
                }
            }
            item { Text("المسودات للمراجعة · Review drafts (${drafts.size})") }
            items(drafts, key = { it.id }) { draft ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(draft.title)
                        Text("EGP ${BigDecimal.valueOf(draft.amountMinor, draft.currencyExponent).toPlainString()}")
                        Text("Status: ${draft.status}")
                        if (draft.status == "DRAFT") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { vm.approveDraft(draft.id) }) {
                                    Text("اعتماد · Approve")
                                }
                                TextButton(onClick = { vm.skipDraft(draft.id) }) {
                                    Text("تخطي · Skip")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

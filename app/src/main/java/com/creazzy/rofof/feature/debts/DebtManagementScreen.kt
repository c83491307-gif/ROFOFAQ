package com.creazzy.rofof.feature.debts

import android.app.Application
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
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.creazzy.rofof.data.local.DebtBalance
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.repository.DebtManagementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RofofDatabase.getInstance(application)
    private val repository = DebtManagementRepository(database)
    val debts = repository.observeBalances().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val accounts = database.accountDao().observeBalances().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val message = MutableStateFlow<String?>(null)

    fun addDebt(title: String, amount: String, direction: String) {
        viewModelScope.launch {
            runCatching {
                val account = accounts.value.firstOrNull() ?: error("Create an account first")
                val minor = BigDecimal(amount.trim().replace(',', '.'))
                    .setScale(account.currencyExponent, RoundingMode.UNNECESSARY)
                    .movePointRight(account.currencyExponent).longValueExact()
                repository.createDebt(title, direction, minor, account.currencyCode, account.currencyExponent)
            }.onSuccess { message.value = "Saved" }
                .onFailure { message.value = it.message ?: "Unable to save debt" }
        }
    }

    fun settle(id: String) {
        viewModelScope.launch {
            runCatching { repository.settle(id) }
                .onSuccess { message.value = "Debt settled and ledger updated" }
                .onFailure { message.value = it.message ?: "Unable to settle debt" }
        }
    }

    fun clearMessage() { message.value = null }
}

@Composable
fun DebtManagementScreen(onBack: () -> Unit = {}, vm: DebtViewModel = viewModel()) {
    val debts by vm.debts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val message by vm.message.collectAsState()
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("OWED_TO_ME") }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) { Text("رجوع") }
        Text("الديون والسلف", style = MaterialTheme.typography.headlineSmall)
        Text("لي عند الآخرين · وعليّ للآخرين", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { direction = "OWED_TO_ME" }) {
                Text(if (direction == "OWED_TO_ME") "✓ لي" else "لي")
            }
            TextButton(onClick = { direction = "OWED_BY_ME" }) {
                Text(if (direction == "OWED_BY_ME") "✓ عليّ" else "عليّ")
            }
        }
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("الشخص أو وصف الدين") }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = amount, onValueChange = { amount = it },
            label = { Text("المبلغ (${accounts.firstOrNull()?.currencyCode ?: "EGP"})") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                vm.addDebt(title, amount, direction)
                title = ""
                amount = ""
            },
            enabled = title.isNotBlank() && amount.isNotBlank() && accounts.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("إضافة دين") }
        message?.let {
            Text(it)
            TextButton(onClick = vm::clearMessage) { Text("إغلاق") }
        }
        Text("السجلات", style = MaterialTheme.typography.titleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(debts, key = { it.id }) { debt ->
                DebtCard(debt = debt, onSettle = { vm.settle(debt.id) })
            }
        }
    }
}

@Composable
private fun DebtCard(debt: DebtBalance, onSettle: () -> Unit) {
    val amount = NumberFormat.getCurrencyInstance(Locale("ar", "EG")).apply {
        currency = Currency.getInstance(debt.currencyCode)
        minimumFractionDigits = debt.currencyExponent
        maximumFractionDigits = debt.currencyExponent
    }.format(BigDecimal.valueOf(debt.outstandingMinor, debt.currencyExponent))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(debt.title, style = MaterialTheme.typography.titleMedium)
            Text(if (debt.direction == "OWED_TO_ME") "لي عنده" else "عليّ له")
            Text("المتبقي: $amount")
            Text(if (debt.status == "OPEN") "مفتوح" else "تمت التسوية")
            if (debt.status == "OPEN") {
                TextButton(onClick = onSettle) { Text("تسوية كاملة") }
            }
        }
    }
}

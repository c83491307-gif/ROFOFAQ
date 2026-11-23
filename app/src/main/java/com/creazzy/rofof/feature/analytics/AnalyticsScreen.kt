package com.creazzy.rofof.feature.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.creazzy.rofof.data.local.RofofDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = RofofDatabase.getInstance(application)
    private val query = MutableStateFlow("")
    private val monthStart: Long
    private val nextMonthStart: Long

    init {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        nextMonthStart = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults = query.flatMapLatest { db.transactionDao().observeSearch(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoryExpenses = db.transactionDao()
        .observeCategoryExpenses(monthStart, nextMonthStart, "EGP", 2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(value: String) { query.value = value }
}

@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    val vm: AnalyticsViewModel = viewModel()
    val results by vm.searchResults.collectAsState()
    val categories by vm.categoryExpenses.collectAsState()
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("التقارير والبحث · Reports & Search")
            TextButton(onClick = onBack) { Text("رجوع · Back") }
        }
        Text("مصروفات الشهر حسب الفئة · Monthly expenses by category (EGP)")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.categoryId }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.categoryName)
                        Text("EGP " + BigDecimal.valueOf(item.amountMinor, item.currencyExponent).toPlainString())
                    }
                }
            }
            item { Text("بحث العمليات · Search transactions") }
            item {
                OutlinedTextField(value = query, onValueChange = { query = it; vm.search(it) },
                    label = { Text("العنوان أو الملاحظات · Title or notes") }, modifier = Modifier.fillMaxWidth())
            }
            items(results, key = { it.id }) { transaction ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(transaction.title)
                        Text(transaction.transactionType + " · " + transaction.currencyCode + " " +
                            BigDecimal.valueOf(transaction.amountMinor, transaction.currencyExponent).toPlainString())
                    }
                }
            }
        }
    }
}

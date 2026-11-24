package com.creazzy.rofof.feature.budgetgoals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.repository.RoomBudgetGoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class BudgetGoalViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RofofDatabase.getInstance(application)
    private val repository = RoomBudgetGoalRepository(database)
    private val budgetOverrunNotifier = BudgetOverrunNotifier(application, database.budgetGoalDao())
    private val month = YearMonth.now()
    private val zone = ZoneId.systemDefault()
    val periodStartEpochMillis = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val periodEndEpochMillis = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val budgets = repository.observeBudgets(periodStartEpochMillis)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = repository.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = database.categoryDao().observeActive()
        .map { values -> values.filter { it.transactionType == "EXPENSE" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accounts = database.accountDao().observeBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val language = database.localPreferenceDao().observe("language")
        .map { it?.value?.takeIf { value -> value == "en" || value == "ar" } ?: "ar" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "ar")
    val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val busy = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun saveBudget(id: String?, categoryId: String, amount: String, currencyCode: String, exponent: Int) {
        execute {
            val minor = parseMinor(amount, exponent)
            if (id == null) repository.createBudget(
                categoryId, minor, currencyCode, exponent, periodStartEpochMillis, periodEndEpochMillis
            ) else repository.updateBudget(id, minor)
            budgetOverrunNotifier.refreshForExpense(
                categoryId, currencyCode, exponent, periodStartEpochMillis, language.value
            )
        }
    }

    fun deleteBudget(id: String) = execute { repository.deleteBudget(id) }

    fun createGoal(title: String, target: String, currencyCode: String, exponent: Int, deadline: String) {
        execute {
            val minor = parseMinor(target, exponent)
            val deadlineMillis = deadline.trim().takeIf { it.isNotEmpty() }?.let {
                LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli()
            }
            repository.createGoal(title, minor, currencyCode, exponent, deadlineMillis)
        }
    }

    fun updateGoalProgress(id: String, amount: String, exponent: Int) =
        execute { repository.updateGoalProgress(id, parseMinor(amount, exponent)) }

    fun archiveGoal(id: String) = execute { repository.archiveGoal(id) }

    fun clearError() { error.value = null }

    private fun execute(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { block() }.onFailure { error.value = it.message ?: "Operation failed" }
            busy.value = false
        }
    }

    private fun parseMinor(value: String, exponent: Int): Long =
        BigDecimal(value.trim().replace(',', '.'))
            .setScale(exponent, RoundingMode.UNNECESSARY)
            .movePointRight(exponent)
            .longValueExact()
}

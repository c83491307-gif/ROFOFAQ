package com.creazzy.rofof.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creazzy.rofof.data.local.LocalPreferenceEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.repository.RoomExpenseRepository
import com.creazzy.rofof.domain.model.Expense
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RofofDatabase.getInstance(application)
    private val repository = RoomExpenseRepository(database.expenseDao())

    val expenses = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val language = database.localPreferenceDao().observe("language")
        .map { it?.value?.takeIf { value -> value == "en" || value == "ar" } ?: "ar" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "ar")

    fun setLanguage(value: String) {
        if (value != "ar" && value != "en") return
        viewModelScope.launch {
            database.localPreferenceDao().put(
                LocalPreferenceEntity(
                    key = "language",
                    value = value,
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun addExpense(title: String, amount: String, category: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val minorUnits = runCatching {
            BigDecimal(amount.trim().replace(',', '.'))
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull() ?: return
        if (minorUnits <= 0L) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.add(
                Expense(
                    id = UUID.randomUUID().toString(),
                    title = cleanTitle,
                    amountMinor = minorUnits,
                    currencyCode = "EGP",
                    currencyExponent = 2,
                    category = category.trim().ifEmpty { "general" },
                    occurredAtEpochMillis = now,
                    createdAtEpochMillis = now
                )
            )
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}

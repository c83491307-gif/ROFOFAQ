package com.creazzy.rofof.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.LocalPreferenceEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.repository.RoomTransactionRepository
import com.creazzy.rofof.domain.model.LedgerTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RofofDatabase.getInstance(application)
    private val repository = RoomTransactionRepository(
        database.transactionDao(),
        database.accountDao(),
        database.categoryDao()
    )

    val transactions = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories = database.categoryDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val language = database.localPreferenceDao().observe("language")
        .map { it?.value?.takeIf { value -> value == "en" || value == "ar" } ?: "ar" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "ar")

    init {
        viewModelScope.launch { repository.ensureDefaults() }
    }

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

    fun addTransaction(title: String, amount: String, category: String, type: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || (type != "INCOME" && type != "EXPENSE")) return
        val minorUnits = runCatching {
            BigDecimal(amount.trim().replace(',', '.'))
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull() ?: return
        if (minorUnits <= 0L) return

        viewModelScope.launch {
            repository.ensureDefaults()
            val categoryName = category.trim().ifEmpty { "General" }
            val categoryId = database.categoryDao().findByNameAndType(categoryName, type)?.id
                ?: UUID.randomUUID().toString().also { id ->
                    database.categoryDao().insertIfAbsent(
                        CategoryEntity(
                            id = id,
                            name = categoryName,
                            transactionType = type,
                            parentId = null,
                            isArchived = false,
                            createdAtEpochMillis = System.currentTimeMillis()
                        )
                    )
                }.let {
                    database.categoryDao().findByNameAndType(categoryName, type)?.id ?: it
                }

            val now = System.currentTimeMillis()
            repository.add(
                LedgerTransaction(
                    id = UUID.randomUUID().toString(),
                    title = cleanTitle,
                    transactionType = type,
                    amountMinor = minorUnits,
                    currencyCode = "EGP",
                    currencyExponent = 2,
                    accountId = RoomTransactionRepository.DEFAULT_ACCOUNT_ID,
                    categoryId = categoryId,
                    occurredAtEpochMillis = now,
                    createdAtEpochMillis = now,
                    notes = null
                )
            )
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}

package com.creazzy.rofof.feature.purchases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.repository.RoomPurchaseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RofofDatabase.getInstance(application)
    private val repository = RoomPurchaseRepository(database)
    private val selectedId = MutableStateFlow<String?>(null)

    val sessions = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedSessionId = selectedId
    val participants = selectedId.filterNotNull().flatMapLatest { repository.observeParticipants(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val items = selectedId.filterNotNull().flatMapLatest { repository.observeItems(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settlements = selectedId.filterNotNull().flatMapLatest { repository.observeSettlements(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allocationViews = selectedId.filterNotNull().flatMapLatest { database.purchaseDao().observeAllocationViews(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accounts = database.accountDao().observeBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = database.categoryDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val language = database.localPreferenceDao().observe("language")
        .map { it?.value?.takeIf { v -> v == "en" || v == "ar" } ?: "ar" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "ar")
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)

    fun selectSession(id: String?) { selectedId.value = id }
    fun clearError() { error.value = null }

    fun createSession(title: String, currencyCode: String = "EGP", exponent: Int = 2) {
        execute {
            val id = repository.createSession(title, currencyCode, exponent)
            selectedId.value = id
        }
    }

    fun addParticipant(name: String) {
        val sessionId = selectedId.value ?: return
        execute { repository.addParticipant(sessionId, name) }
    }

    fun addItem(title: String, quantity: String, unitPrice: String) {
        val sessionId = selectedId.value ?: return
        val session = sessions.value.firstOrNull { it.id == sessionId } ?: return
        val quantityMilli = BigDecimal(quantity.trim().replace(',', '.'))
            .setScale(3, RoundingMode.UNNECESSARY).movePointRight(3).longValueExact()
        val unitPriceMinor = toMinor(unitPrice, session.currencyExponent)
        execute { repository.addItem(sessionId, title, quantityMilli, unitPriceMinor) }
    }

    fun allocate(itemId: String, amountByParticipant: Map<String, String>) {
        val sessionId = selectedId.value ?: return
        val session = sessions.value.firstOrNull { it.id == sessionId } ?: return
        execute {
            val shares = amountByParticipant.filterValues { it.isNotBlank() }
                .mapValues { (_, amount) -> toMinor(amount, session.currencyExponent) }
            repository.allocateItem(itemId, shares)
        }
    }

    fun recordPayment(participantId: String, amount: String, method: String) {
        val sessionId = selectedId.value ?: return
        val session = sessions.value.firstOrNull { it.id == sessionId } ?: return
        execute {
            repository.recordPayment(
                sessionId = sessionId,
                participantId = participantId,
                amountMinor = toMinor(amount, session.currencyExponent),
                paymentMethod = method,
                idempotencyKey = UUID.randomUUID().toString()
            )
        }
    }

    fun closeAsSettled() {
        val sessionId = selectedId.value ?: return
        execute { repository.closeAsSettled(sessionId) }
    }

    fun recordAsPersonalExpense(allocationId: String, accountId: String, categoryId: String) {
        execute { repository.recordAllocationAsPersonalExpense(allocationId, accountId, categoryId) }
    }

    private fun execute(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { block() }.onFailure { error.value = it.message ?: "Operation failed" }
            busy.value = false
        }
    }

    private fun toMinor(value: String, exponent: Int): Long =
        BigDecimal(value.trim().replace(',', '.'))
            .setScale(exponent, RoundingMode.UNNECESSARY)
            .movePointRight(exponent)
            .longValueExact()
}

package com.creazzy.rofof.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.repository.RoomFinanceRepository
import com.creazzy.rofof.data.local.LocalPreferenceEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.repository.RoomTransactionRepository
import com.creazzy.rofof.domain.model.LedgerTransaction
import com.creazzy.rofof.feature.budgetgoals.BudgetOverrunNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RofofDatabase.getInstance(application)
    private val financeRepository = RoomFinanceRepository(database)
    private val budgetOverrunNotifier = BudgetOverrunNotifier(application, database.budgetGoalDao())
    private val repository = RoomTransactionRepository(
        database.transactionDao(),
        database.accountDao(),
        database.categoryDao()
    )

    private val _financeActionMessage = MutableStateFlow<String?>(null)
    val financeActionMessage = _financeActionMessage.asStateFlow()

    fun clearFinanceActionMessage() { _financeActionMessage.value = null }

    private fun runFinanceAction(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _financeActionMessage.value = runCatching { action() }
                .fold({ successMessage }, { localizeFinanceError(it.message.orEmpty()) })
        }
    }

    private fun localizeFinanceError(message: String): String {
        if (language.value != "ar") return message.ifBlank { "The operation could not be completed." }
        return when {
            message.contains("Move or remove account activity") -> "لا يمكن أرشفة الحساب لوجود عمليات أو تحويلات مرتبطة به. انقلها أو عالجها أولاً."
            message.contains("Default account cannot be archived") -> "لا يمكن أرشفة الحساب النقدي الافتراضي."
            message.contains("Archive child categories first") -> "أرشف الفئات الفرعية أولاً قبل أرشفة الفئة الرئيسية."
            message.contains("Category has transactions") -> "لا يمكن أرشفة الفئة لارتباطها بعمليات مالية. أعد إسناد العمليات أولاً."
            message.contains("already archived") -> "هذا العنصر مؤرشف بالفعل."
            message.contains("not found") -> "العنصر المطلوب غير موجود أو لم يعد نشطاً."
            message.contains("name is required") -> "الاسم مطلوب."
            else -> "تعذر تنفيذ العملية: $message"
        }
    }

    val transactions = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories = database.categoryDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val managedCategories = financeRepository.observeCategoriesForManagement()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts = financeRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val managedAccounts = financeRepository.observeAccountsForManagement()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transfers = financeRepository.observeTransfers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals = database.transactionDao().observeTotals("EGP", 2)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            com.creazzy.rofof.data.local.TransactionTotals(0L, 0L)
        )

    fun createAccount(name: String, currencyCode: String, exponent: Int, openingBalance: String) {
        val minor = runCatching {
            BigDecimal(openingBalance.trim().replace(',', '.'))
                .setScale(exponent, RoundingMode.UNNECESSARY)
                .movePointRight(exponent).longValueExact()
        }.getOrNull() ?: return
        viewModelScope.launch {
            runCatching { financeRepository.createAccount(name, currencyCode, exponent, minor) }
        }
    }

    fun transfer(fromAccountId: String, toAccountId: String, amount: String, note: String = "") {
        val source = accounts.value.firstOrNull { it.id == fromAccountId } ?: return
        val minor = runCatching {
            BigDecimal(amount.trim().replace(',', '.'))
                .setScale(source.currencyExponent, RoundingMode.UNNECESSARY)
                .movePointRight(source.currencyExponent).longValueExact()
        }.getOrNull() ?: return
        viewModelScope.launch {
            runCatching { financeRepository.transfer(fromAccountId, toAccountId, minor, note) }
        }
    }

    fun createCategory(name: String, type: String, parentId: String? = null) {
        viewModelScope.launch { runCatching { financeRepository.createCategory(name, type, parentId) } }
    }

    fun renameAccount(id: String, name: String) =
        runFinanceAction(if (language.value == "ar") "تم تعديل اسم الحساب." else "Account renamed.") {
            financeRepository.renameAccount(id, name)
        }

    fun archiveAccount(id: String) =
        runFinanceAction(if (language.value == "ar") "تمت أرشفة الحساب مع الاحتفاظ بسجلاته." else "Account archived; its history was kept.") {
            financeRepository.archiveAccount(id)
        }

    fun renameCategory(id: String, name: String) =
        runFinanceAction(if (language.value == "ar") "تم تعديل اسم الفئة." else "Category renamed.") {
            financeRepository.renameCategory(id, name)
        }

    fun archiveCategory(id: String) =
        runFinanceAction(if (language.value == "ar") "تمت أرشفة الفئة مع الاحتفاظ بالعمليات التاريخية." else "Category archived; historical transactions were kept.") {
            financeRepository.archiveCategory(id)
        }

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

    fun addTransaction(title: String, amount: String, category: String, type: String, accountId: String = RoomTransactionRepository.DEFAULT_ACCOUNT_ID, occurredAtEpochMillis: Long = System.currentTimeMillis()) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || (type != "INCOME" && type != "EXPENSE")) return
        viewModelScope.launch {
            repository.ensureDefaults()
            val account = database.accountDao().findById(accountId)?.takeIf { !it.isArchived } ?: return@launch
            val minorUnits = runCatching {
                BigDecimal(amount.trim().replace(',', '.'))
                    .setScale(account.currencyExponent, RoundingMode.UNNECESSARY)
                    .movePointRight(account.currencyExponent)
                    .longValueExact()
            }.getOrNull() ?: return@launch
            if (minorUnits <= 0L) return@launch
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
                    currencyCode = account.currencyCode,
                    currencyExponent = account.currencyExponent,
                    accountId = account.id,
                    categoryId = categoryId,
                    occurredAtEpochMillis = occurredAtEpochMillis,
                    createdAtEpochMillis = now,
                    notes = null
                )
            )
            if (type == "EXPENSE") {
                budgetOverrunNotifier.refreshForExpense(
                    categoryId, account.currencyCode, account.currencyExponent,
                    occurredAtEpochMillis, language.value
                )
            }
        }
    }


    fun updateTransaction(
        original: LedgerTransaction,
        title: String,
        amount: String,
        category: String,
        type: String,
        accountId: String,
        occurredAtEpochMillis: Long
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || (type != "INCOME" && type != "EXPENSE")) return
        viewModelScope.launch {
            val account = database.accountDao().findById(accountId)?.takeIf { !it.isArchived } ?: return@launch
            val minorUnits = runCatching {
                BigDecimal(amount.trim().replace(',', '.'))
                    .setScale(account.currencyExponent, RoundingMode.UNNECESSARY)
                    .movePointRight(account.currencyExponent)
                    .longValueExact()
            }.getOrNull() ?: return@launch
            if (minorUnits <= 0L) return@launch
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
                }.let { database.categoryDao().findByNameAndType(categoryName, type)?.id ?: it }
            runCatching {
                repository.update(
                    original.copy(
                        title = cleanTitle,
                        transactionType = type,
                        amountMinor = minorUnits,
                        currencyCode = account.currencyCode,
                        currencyExponent = account.currencyExponent,
                        accountId = account.id,
                        categoryId = categoryId,
                        occurredAtEpochMillis = occurredAtEpochMillis
                    )
                )
            }.onFailure {
                _financeActionMessage.value = localizeFinanceError(it.message.orEmpty())
            }.onSuccess {
                if (original.transactionType == "EXPENSE") {
                    budgetOverrunNotifier.refreshForExpense(
                        original.categoryId, original.currencyCode, original.currencyExponent,
                        original.occurredAtEpochMillis, language.value
                    )
                }
                if (type == "EXPENSE") {
                    budgetOverrunNotifier.refreshForExpense(
                        categoryId, account.currencyCode, account.currencyExponent,
                        occurredAtEpochMillis, language.value
                    )
                }
                _financeActionMessage.value = if (language.value == "ar") "تم تحديث العملية المالية." else "Transaction updated."
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val previous = database.transactionDao().findById(id)
            repository.delete(id)
            if (previous?.transactionType == "EXPENSE") {
                budgetOverrunNotifier.refreshForExpense(
                    previous.categoryId, previous.currencyCode, previous.currencyExponent,
                    previous.occurredAtEpochMillis, language.value
                )
            }
        }
    }
}

package com.creazzy.rofof.feature.dashboard

import android.app.Application
import android.net.Uri
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneId
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.LearnedKeyword
import com.creazzy.rofof.feature.smartentry.SmartEntryParser
import com.creazzy.rofof.widget.ExpenseWidgetUpdater
import com.creazzy.rofof.data.repository.RoomFinanceRepository
import com.creazzy.rofof.data.local.LocalPreferenceEntity
import com.creazzy.rofof.data.local.RofofDatabase
import com.creazzy.rofof.data.repository.RoomTransactionRepository
import com.creazzy.rofof.domain.model.LedgerTransaction
import com.creazzy.rofof.core.money.AmountInputParser
import com.creazzy.rofof.feature.awareness.AwarenessAdvisor
import com.creazzy.rofof.feature.awareness.BudgetRuleNotificationDispatcher
import com.creazzy.rofof.feature.awareness.AwarenessEntryReview
import com.creazzy.rofof.feature.awareness.DatabaseSeeder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.time.LocalDate

data class SafeToSpendState(
    val monthlyBudgetMinor: Long,
    val monthlyExpensesMinor: Long,
    val daysRemaining: Int,
    val dailySafeMinor: Long,
    val hasBudget: Boolean
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionWriteLock = AtomicBoolean(false)
    private val _isSavingTransaction = MutableStateFlow(false)
    val isSavingTransaction = _isSavingTransaction.asStateFlow()
    private val database = RofofDatabase.getInstance(application)
    private val financeRepository = RoomFinanceRepository(database)
    private val budgetRuleNotifications = BudgetRuleNotificationDispatcher(application, database)
    private val awarenessAdvisor = AwarenessAdvisor(application, database)
    private val _awarenessReview = MutableStateFlow<AwarenessEntryReview?>(null)
    val awarenessReview = _awarenessReview.asStateFlow()
    private val repository = RoomTransactionRepository(
        database.transactionDao(),
        database.accountDao(),
        database.categoryDao()
    )

    private val _financeActionMessage = MutableStateFlow<String?>(null)
    val financeActionMessage = _financeActionMessage.asStateFlow()

    fun clearFinanceActionMessage() { _financeActionMessage.value = null }

    fun analyzeSmartEntry(text: String, accountId: String, occurredAt: Long) {
        viewModelScope.launch {
            _awarenessReview.value = runCatching { awarenessAdvisor.review(text, accountId, occurredAt, language.value) }.getOrNull()
        }
    }
    fun clearAwarenessReview() { _awarenessReview.value = null }

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

    val trash = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val transactionRefresh = MutableStateFlow(0)
    private val _transactionsLoaded = MutableStateFlow(false)
    val transactionsLoaded = _transactionsLoaded.asStateFlow()
    private val _transactionsLoadError = MutableStateFlow<String?>(null)
    val transactionsLoadError = _transactionsLoadError.asStateFlow()

    val transactions = transactionRefresh
        .flatMapLatest {
            repository.observeRecent()
                .onStart { _transactionsLoaded.value = false; _transactionsLoadError.value = null }
                .onEach { _transactionsLoaded.value = true; _transactionsLoadError.value = null }
                .catch { error ->
                    _transactionsLoaded.value = true
                    _transactionsLoadError.value = localizeFinanceError(error.message.orEmpty())
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun retryTransactions() {
        transactionRefresh.value = transactionRefresh.value + 1
    }

    val categories = database.categoryDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val learnedKeywordSuggestions = combine(
        database.learnedKeywordDao().observeAll(),
        database.categoryDao().observeActive()
    ) { learned, activeCategories ->
        val categoriesById = activeCategories.associateBy { it.id }
        learned.mapNotNull { item ->
            categoriesById[item.categoryId]?.let { category ->
                item.keyword to SmartEntryParser.CategorySuggestion(
                    categoryNameArabic = category.name,
                    categoryNameEnglish = category.name,
                    transactionType = category.transactionType
                )
            }
        }.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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

    private val currentMonth = LocalDate.now()
    private val currentMonthStart = currentMonth.withDayOfMonth(1)
    private val nextMonthStart = currentMonthStart.plusMonths(1)
    private val monthStartMillis = currentMonthStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val nextMonthStartMillis = nextMonthStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val daysRemainingInMonth = (currentMonth.lengthOfMonth() - currentMonth.dayOfMonth + 1).coerceAtLeast(1)

    val safeToSpend = combine(
        database.budgetGoalDao().observeMonthlyBudgetTotal("EGP", 2, monthStartMillis, nextMonthStartMillis),
        database.transactionDao().observeMonthlyExpenseTotal("EGP", 2, monthStartMillis, nextMonthStartMillis)
    ) { budget, expenses ->
        val remaining = (budget - expenses).coerceAtLeast(0L)
        SafeToSpendState(
            monthlyBudgetMinor = budget,
            monthlyExpensesMinor = expenses,
            daysRemaining = daysRemainingInMonth,
            dailySafeMinor = if (budget > 0L) remaining / daysRemainingInMonth else 0L,
            hasBudget = budget > 0L
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SafeToSpendState(0L, 0L, daysRemainingInMonth, 0L, false)
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

    val insights = combine(
        database.transactionDao().observeMonthlyInsightsSnapshot(
            "EGP", 2, monthStartMillis, nextMonthStartMillis
        ),
        language,
        database.transactionDao().observeMonthlyExpenseByWeekday(
            "EGP", 2, monthStartMillis, nextMonthStartMillis
        ),
        safeToSpend
    ) { snapshot, selectedLanguage, weekdayRows, budgetState ->
        LocalInsightsEngine.generate(snapshot, System.currentTimeMillis(), selectedLanguage) +
            SpendingPatternEngine.analyze(
                weekdayRows = weekdayRows,
                monthExpenseMinor = snapshot.totalExpenseMinor,
                monthlyBudgetMinor = budgetState.monthlyBudgetMinor,
                monthStartDate = currentMonthStart,
                elapsedDays = currentMonth.dayOfMonth,
                daysInMonth = currentMonth.lengthOfMonth(),
                language = selectedLanguage
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            runCatching { DatabaseSeeder(database).seedFromAssets(getApplication()) }
            repository.ensureDefaults()
            repository.purgeExpiredTrash()
        }
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

    fun exportTransactions(uri: Uri) {
        viewModelScope.launch {
            _financeActionMessage.value = runCatching {
                val rows = database.transactionDao().getAllForCsvExport()
                val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                    ?: error("Unable to open the selected export destination")
                stream.use { output ->
                    OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                        writer.write("\uFEFF")
                        writer.write("Date,Category,Amount,Currency,Note\r\n")
                        rows.forEach { row ->
                            val date = Instant.ofEpochMilli(row.occurredAtEpochMillis)
                                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                            val amount = BigDecimal.valueOf(row.amountMinor, row.currencyExponent).toPlainString()
                            fun csv(value: String?): String = "\"" + (value ?: "").replace("\"", "\"\"") + "\""
                            writer.write(listOf(date, csv(row.categoryName), amount, row.currencyCode, csv(row.note))
                                .joinToString(","))
                            writer.write("\r\n")
                        }
                    }
                }
                if (language.value == "ar") "تم تصدير المعاملات إلى CSV." else "Transactions exported to CSV."
            }.getOrElse { localizeFinanceError(it.message.orEmpty()) }
        }
    }

    fun restoreTransaction(id: String) {
        viewModelScope.launch { runCatching { repository.restoreFromTrash(id) } }
    }

    fun permanentlyDeleteTransaction(id: String) {
        viewModelScope.launch { runCatching { repository.permanentlyDeleteFromTrash(id) } }
    }

    fun addTransaction(
        title: String,
        amount: String,
        category: String,
        type: String,
        accountId: String = RoomTransactionRepository.DEFAULT_ACCOUNT_ID,
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || (type != "INCOME" && type != "EXPENSE")) {
            _financeActionMessage.value = if (language.value == "ar") "أدخل اسم العملية واختر نوعاً صحيحاً." else "Enter a transaction name and a valid type."
            return
        }
        if (!transactionWriteLock.compareAndSet(false, true)) return
        _isSavingTransaction.value = true
        viewModelScope.launch {
            try {
                repository.ensureDefaults()
                val account = database.accountDao().findById(accountId)?.takeIf { !it.isArchived }
                    ?: error(if (language.value == "ar") "الحساب المحدد غير متاح." else "The selected account is unavailable.")
                val minorUnits = AmountInputParser.toMinorUnits(amount, account.currencyExponent)
                    ?: error(if (language.value == "ar") "أدخل مبلغاً أكبر من صفر وبعدد منازل عشرية صحيح." else "Enter an amount greater than zero with valid decimal precision.")
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
                val learningKey = cleanTitle
                    .replace(Regex("""[٠-٩۰-۹0-9]+([٫.,][٠-٩۰-۹0-9]+)?"""), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .lowercase()
                if (learningKey.length >= 2) {
                    database.learnedKeywordDao().insert(LearnedKeyword(learningKey, categoryId))
                }
                if (type == "EXPENSE") {
                    runCatching {
                        budgetRuleNotifications.dispatchForExpense(
                            categoryId, account.currencyCode, account.currencyExponent,
                            occurredAtEpochMillis, language.value
                        )
                    }
                }
                ExpenseWidgetUpdater.refresh(getApplication())
                _financeActionMessage.value = if (language.value == "ar") "تم حفظ العملية بنجاح." else "Transaction saved."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _financeActionMessage.value = localizeFinanceError(error.message.orEmpty())
            } finally {
                transactionWriteLock.set(false)
                _isSavingTransaction.value = false
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
        if (cleanTitle.isEmpty() || (type != "INCOME" && type != "EXPENSE")) {
            _financeActionMessage.value = if (language.value == "ar") "أدخل اسم العملية واختر نوعاً صحيحاً." else "Enter a transaction name and a valid type."
            return
        }
        if (!transactionWriteLock.compareAndSet(false, true)) return
        _isSavingTransaction.value = true
        viewModelScope.launch {
            try {
                val account = database.accountDao().findById(accountId)?.takeIf { !it.isArchived }
                    ?: error(if (language.value == "ar") "الحساب المحدد غير متاح." else "The selected account is unavailable.")
                val minorUnits = AmountInputParser.toMinorUnits(amount, account.currencyExponent)
                    ?: error(if (language.value == "ar") "أدخل مبلغاً أكبر من صفر وبعدد منازل عشرية صحيح." else "Enter an amount greater than zero with valid decimal precision.")
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
                if (original.transactionType == "EXPENSE") {
                    runCatching {
                        budgetRuleNotifications.dispatchForExpense(
                            original.categoryId, original.currencyCode, original.currencyExponent,
                            original.occurredAtEpochMillis, language.value
                        )
                    }
                }
                if (type == "EXPENSE") {
                    runCatching {
                        budgetRuleNotifications.dispatchForExpense(
                            categoryId, account.currencyCode, account.currencyExponent,
                            occurredAtEpochMillis, language.value
                        )
                    }
                }
                ExpenseWidgetUpdater.refresh(getApplication())
                _financeActionMessage.value = if (language.value == "ar") "تم تحديث العملية المالية." else "Transaction updated."
            } catch (error: Throwable) {
                _financeActionMessage.value = localizeFinanceError(error.message.orEmpty())
            } finally {
                transactionWriteLock.set(false)
                _isSavingTransaction.value = false
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val previous = database.transactionDao().findById(id)
            repository.delete(id)
            if (previous?.transactionType == "EXPENSE") {
                runCatching {
                    budgetRuleNotifications.dispatchForExpense(
                        previous.categoryId, previous.currencyCode, previous.currencyExponent,
                        previous.occurredAtEpochMillis, language.value
                    )
                }
            }
            ExpenseWidgetUpdater.refresh(getApplication())
        }
    }
}

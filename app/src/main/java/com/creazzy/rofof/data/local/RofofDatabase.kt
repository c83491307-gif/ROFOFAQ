package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LocalPreferenceEntity::class,
        ExpenseEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        PurchaseSessionEntity::class,
        PurchaseParticipantEntity::class,
        PurchaseItemEntity::class,
        PurchaseAllocationEntity::class,
        PurchasePaymentEntity::class,
        AllocationPersonalExpenseLinkEntity::class,
        BudgetEntity::class,
        FinancialGoalEntity::class,
        RecurringRuleEntity::class,
        RecurringDraftEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class RofofDatabase : RoomDatabase() {
    abstract fun localPreferenceDao(): LocalPreferenceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        @Volatile
        private var instance: RofofDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        amountMinor INTEGER NOT NULL,
                        currencyCode TEXT NOT NULL,
                        currencyExponent INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        occurredAtEpochMillis INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_expenses_occurredAtEpochMillis ON expenses (occurredAtEpochMillis)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        currencyCode TEXT NOT NULL,
                        currencyExponent INTEGER NOT NULL,
                        openingBalanceMinor INTEGER NOT NULL,
                        isArchived INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        transactionType TEXT NOT NULL,
                        parentId TEXT,
                        isArchived INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name_transactionType ON categories (name, transactionType)"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transactions (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        transactionType TEXT NOT NULL,
                        amountMinor INTEGER NOT NULL,
                        currencyCode TEXT NOT NULL,
                        currencyExponent INTEGER NOT NULL,
                        accountId TEXT NOT NULL,
                        categoryId TEXT NOT NULL,
                        occurredAtEpochMillis INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        notes TEXT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_occurredAtEpochMillis ON transactions (occurredAtEpochMillis)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_accountId_occurredAtEpochMillis ON transactions (accountId, occurredAtEpochMillis)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_categoryId_occurredAtEpochMillis ON transactions (categoryId, occurredAtEpochMillis)"
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO accounts
                    (id, name, currencyCode, currencyExponent, openingBalanceMinor, isArchived, createdAtEpochMillis)
                    VALUES ('default-cash', 'Cash', 'EGP', 2, 0, 0, 0)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO accounts
                    (id, name, currencyCode, currencyExponent, openingBalanceMinor, isArchived, createdAtEpochMillis)
                    SELECT
                        CASE
                            WHEN currencyCode = 'EGP' AND currencyExponent = 2 THEN 'default-cash'
                            ELSE 'legacy-cash-' || currencyCode || '-' || currencyExponent
                        END,
                        'Cash (' || currencyCode || ')',
                        currencyCode, currencyExponent, 0, 0, 0
                    FROM expenses
                    GROUP BY currencyCode, currencyExponent
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories
                    (id, name, transactionType, parentId, isArchived, createdAtEpochMillis)
                    VALUES ('default-expense-general', 'General', 'EXPENSE', NULL, 0, 0)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories
                    (id, name, transactionType, parentId, isArchived, createdAtEpochMillis)
                    VALUES ('default-income-general', 'General', 'INCOME', NULL, 0, 0)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories
                    (id, name, transactionType, parentId, isArchived, createdAtEpochMillis)
                    SELECT 'legacy-expense-' || lower(trim(category)), trim(category),
                           'EXPENSE', NULL, 0, 0
                    FROM expenses
                    WHERE trim(category) <> '' AND lower(trim(category)) <> 'general'
                    GROUP BY lower(trim(category))
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO transactions
                    (id, title, transactionType, amountMinor, currencyCode, currencyExponent,
                     accountId, categoryId, occurredAtEpochMillis, createdAtEpochMillis, notes)
                    SELECT id, title, 'EXPENSE', amountMinor, currencyCode, currencyExponent,
                           CASE
                               WHEN currencyCode = 'EGP' AND currencyExponent = 2 THEN 'default-cash'
                               ELSE 'legacy-cash-' || currencyCode || '-' || currencyExponent
                           END,
                           CASE
                               WHEN trim(category) = '' OR lower(trim(category)) = 'general'
                                   THEN 'default-expense-general'
                               ELSE 'legacy-expense-' || lower(trim(category))
                           END,
                           occurredAtEpochMillis, createdAtEpochMillis, NULL
                    FROM expenses
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transfers (
                        id TEXT NOT NULL PRIMARY KEY,
                        fromAccountId TEXT NOT NULL,
                        toAccountId TEXT NOT NULL,
                        amountMinor INTEGER NOT NULL,
                        currencyCode TEXT NOT NULL,
                        currencyExponent INTEGER NOT NULL,
                        occurredAtEpochMillis INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        note TEXT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transfers_fromAccountId_occurredAtEpochMillis ON transfers (fromAccountId, occurredAtEpochMillis)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transfers_toAccountId_occurredAtEpochMillis ON transfers (toAccountId, occurredAtEpochMillis)"
                )
            }
        }


        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""CREATE TABLE IF NOT EXISTS purchase_sessions (
                    id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, notes TEXT,
                    currencyCode TEXT NOT NULL, currencyExponent INTEGER NOT NULL,
                    status TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL)""")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_sessions_createdAtEpochMillis ON purchase_sessions (createdAtEpochMillis)")
                database.execSQL("""CREATE TABLE IF NOT EXISTS purchase_participants (
                    id TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, displayName TEXT NOT NULL,
                    note TEXT, isArchived INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL,
                    FOREIGN KEY(sessionId) REFERENCES purchase_sessions(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_participants_sessionId ON purchase_participants (sessionId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchase_participants_sessionId_displayName ON purchase_participants (sessionId, displayName)")
                database.execSQL("""CREATE TABLE IF NOT EXISTS purchase_items (
                    id TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, title TEXT NOT NULL,
                    quantityMilli INTEGER NOT NULL, unitPriceMinor INTEGER NOT NULL, totalMinor INTEGER NOT NULL,
                    note TEXT, createdAtEpochMillis INTEGER NOT NULL,
                    FOREIGN KEY(sessionId) REFERENCES purchase_sessions(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_items_sessionId ON purchase_items (sessionId)")
                database.execSQL("""CREATE TABLE IF NOT EXISTS purchase_allocations (
                    id TEXT NOT NULL PRIMARY KEY, itemId TEXT NOT NULL, participantId TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL,
                    FOREIGN KEY(itemId) REFERENCES purchase_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(participantId) REFERENCES purchase_participants(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_allocations_itemId ON purchase_allocations (itemId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_allocations_participantId ON purchase_allocations (participantId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchase_allocations_itemId_participantId ON purchase_allocations (itemId, participantId)")
                database.execSQL("""CREATE TABLE IF NOT EXISTS purchase_payments (
                    id TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, participantId TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL, paymentMethod TEXT NOT NULL, idempotencyKey TEXT NOT NULL,
                    note TEXT, occurredAtEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL,
                    FOREIGN KEY(sessionId) REFERENCES purchase_sessions(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(participantId) REFERENCES purchase_participants(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_payments_sessionId ON purchase_payments (sessionId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_payments_participantId ON purchase_payments (participantId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_payments_occurredAtEpochMillis ON purchase_payments (occurredAtEpochMillis)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchase_payments_idempotencyKey ON purchase_payments (idempotencyKey)")
                database.execSQL("""CREATE TABLE IF NOT EXISTS allocation_personal_expense_links (
                    id TEXT NOT NULL PRIMARY KEY, allocationId TEXT NOT NULL, transactionId TEXT NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    FOREIGN KEY(allocationId) REFERENCES purchase_allocations(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_allocation_personal_expense_links_allocationId ON allocation_personal_expense_links (allocationId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_allocation_personal_expense_links_transactionId ON allocation_personal_expense_links (transactionId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""CREATE TABLE IF NOT EXISTS budgets (
                    id TEXT NOT NULL PRIMARY KEY,
                    categoryId TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    currencyCode TEXT NOT NULL,
                    currencyExponent INTEGER NOT NULL,
                    periodStartEpochMillis INTEGER NOT NULL,
                    periodEndEpochMillis INTEGER NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL
                )""")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_categoryId_currencyCode_periodStartEpochMillis ON budgets (categoryId, currencyCode, periodStartEpochMillis)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_periodStartEpochMillis ON budgets (periodStartEpochMillis)")
                database.execSQL("""CREATE TABLE IF NOT EXISTS financial_goals (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    targetAmountMinor INTEGER NOT NULL,
                    currentAmountMinor INTEGER NOT NULL,
                    currencyCode TEXT NOT NULL,
                    currencyExponent INTEGER NOT NULL,
                    deadlineEpochMillis INTEGER,
                    isArchived INTEGER NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL
                )""")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_isArchived_createdAtEpochMillis ON financial_goals (isArchived, createdAtEpochMillis)")
            }
        }


        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""CREATE TABLE IF NOT EXISTS recurring_rules (
                    id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, transactionType TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL, currencyCode TEXT NOT NULL, currencyExponent INTEGER NOT NULL,
                    accountId TEXT NOT NULL, categoryId TEXT NOT NULL, intervalType TEXT NOT NULL,
                    intervalCount INTEGER NOT NULL, nextRunEpochMillis INTEGER NOT NULL, endEpochMillis INTEGER,
                    isActive INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL
                )""")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_isActive_nextRunEpochMillis ON recurring_rules (isActive, nextRunEpochMillis)")
                database.execSQL("""CREATE TABLE IF NOT EXISTS recurring_drafts (
                    id TEXT NOT NULL PRIMARY KEY, ruleId TEXT NOT NULL, scheduledForEpochMillis INTEGER NOT NULL,
                    title TEXT NOT NULL, transactionType TEXT NOT NULL, amountMinor INTEGER NOT NULL,
                    currencyCode TEXT NOT NULL, currencyExponent INTEGER NOT NULL, accountId TEXT NOT NULL,
                    categoryId TEXT NOT NULL, status TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL
                )""")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recurring_drafts_ruleId_scheduledForEpochMillis ON recurring_drafts (ruleId, scheduledForEpochMillis)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_drafts_status_scheduledForEpochMillis ON recurring_drafts (status, scheduledForEpochMillis)")
            }
        }

        fun getInstance(context: Context): RofofDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RofofDatabase::class.java,
                    "rofof-local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { instance = it }
            }
    }
}

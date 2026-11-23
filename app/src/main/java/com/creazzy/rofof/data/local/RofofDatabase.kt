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
        TransactionEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class RofofDatabase : RoomDatabase() {
    abstract fun localPreferenceDao(): LocalPreferenceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

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
                    WHERE trim(category) <> ''
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
                               WHEN trim(category) = '' THEN 'default-expense-general'
                               ELSE 'legacy-expense-' || lower(trim(category))
                           END,
                           occurredAtEpochMillis, createdAtEpochMillis, NULL
                    FROM expenses
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): RofofDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RofofDatabase::class.java,
                    "rofof-local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}

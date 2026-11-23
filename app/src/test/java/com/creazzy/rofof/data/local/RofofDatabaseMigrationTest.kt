package com.creazzy.rofof.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RofofDatabaseMigrationTest {
    @Test
    fun migratesPhysicalV4DatabaseAndPreservesRowsTablesIndexesAndConstraints() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "rofof-v4-to-v5-migration-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        createV4File(file)

        val room = Room.databaseBuilder(context, RofofDatabase::class.java, name)
            .addMigrations(RofofDatabase.MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()
        try {
            val db = room.openHelper.writableDatabase
            assertEquals(5, db.version)
            assertEquals("dark", db.query("SELECT value FROM local_preferences WHERE key='theme'").use {
                assertTrue(it.moveToFirst()); it.getString(0)
            })
            assertEquals(12345L, db.query("SELECT amountMinor FROM expenses WHERE id='legacy-expense-1'").use {
                assertTrue(it.moveToFirst()); it.getLong(0)
            })
            assertEquals("legacy-cash-EGP-2", db.query("SELECT id FROM accounts WHERE id='legacy-cash-EGP-2'").use {
                assertTrue(it.moveToFirst()); it.getString(0)
            })
            assertEquals("legacy-expense-food", db.query("SELECT id FROM categories WHERE id='legacy-expense-food'").use {
                assertTrue(it.moveToFirst()); it.getString(0)
            })
            assertEquals(12345L, db.query("SELECT amountMinor FROM transactions WHERE id='legacy-expense-1'").use {
                assertTrue(it.moveToFirst()); it.getLong(0)
            })
            assertEquals("transfer-1", db.query("SELECT id FROM transfers WHERE id='transfer-1'").use {
                assertTrue(it.moveToFirst()); it.getString(0)
            })

            val tables = mutableSetOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").use { c ->
                while (c.moveToNext()) tables += c.getString(0)
            }
            val expectedTables = setOf(
                "local_preferences", "expenses", "accounts", "categories", "transactions", "transfers",
                "purchase_sessions", "purchase_participants", "purchase_items", "purchase_allocations",
                "purchase_payments", "allocation_personal_expense_links"
            )
            assertTrue("Missing tables: " + (expectedTables - tables), tables.containsAll(expectedTables))

            val indexes = mutableSetOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'").use { c ->
                while (c.moveToNext()) indexes += c.getString(0)
            }
            val expectedIndexes = setOf(
                "index_expenses_occurredAtEpochMillis", "index_categories_name_transactionType",
                "index_transactions_occurredAtEpochMillis", "index_transactions_accountId_occurredAtEpochMillis",
                "index_transactions_categoryId_occurredAtEpochMillis", "index_transfers_fromAccountId_occurredAtEpochMillis",
                "index_transfers_toAccountId_occurredAtEpochMillis", "index_purchase_sessions_createdAtEpochMillis",
                "index_purchase_participants_sessionId", "index_purchase_participants_sessionId_displayName",
                "index_purchase_items_sessionId", "index_purchase_allocations_itemId",
                "index_purchase_allocations_participantId", "index_purchase_allocations_itemId_participantId",
                "index_purchase_payments_sessionId", "index_purchase_payments_participantId",
                "index_purchase_payments_occurredAtEpochMillis", "index_purchase_payments_idempotencyKey",
                "index_allocation_personal_expense_links_allocationId",
                "index_allocation_personal_expense_links_transactionId"
            )
            assertTrue("Missing indexes: " + (expectedIndexes - indexes), indexes.containsAll(expectedIndexes))
            assertEquals(1, db.query("PRAGMA foreign_key_list('purchase_participants')").use { it.count })
            assertEquals(2, db.query("PRAGMA foreign_key_list('purchase_allocations')").use { it.count })
            assertEquals(2, db.query("PRAGMA foreign_key_list('purchase_payments')").use { it.count })
            assertEquals(2, db.query("PRAGMA foreign_key_list('allocation_personal_expense_links')").use { it.count })
            assertFalse("Foreign-key violations", db.query("PRAGMA foreign_key_check").use { it.moveToFirst() })

            db.execSQL("""INSERT INTO purchase_sessions
                (id,title,notes,currencyCode,currencyExponent,status,createdAtEpochMillis,updatedAtEpochMillis)
                VALUES ('s1','test',NULL,'EGP',2,'OPEN',1,1)""")
            db.execSQL("""INSERT INTO purchase_participants
                (id,sessionId,displayName,note,isArchived,createdAtEpochMillis)
                VALUES ('p1','s1','A',NULL,0,1)""")
            assertTrue(runCatching {
                db.execSQL("""INSERT INTO purchase_participants
                    (id,sessionId,displayName,note,isArchived,createdAtEpochMillis)
                    VALUES ('p2','s1','A',NULL,0,1)""")
            }.isFailure)
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }

    private fun createV4File(file: java.io.File) {
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            db.execSQL("CREATE TABLE local_preferences (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE expenses (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, amountMinor INTEGER NOT NULL, currencyCode TEXT NOT NULL, currencyExponent INTEGER NOT NULL, category TEXT NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX index_expenses_occurredAtEpochMillis ON expenses (occurredAtEpochMillis)")
            db.execSQL("CREATE TABLE accounts (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, currencyCode TEXT NOT NULL, currencyExponent INTEGER NOT NULL, openingBalanceMinor INTEGER NOT NULL, isArchived INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE categories (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, transactionType TEXT NOT NULL, parentId TEXT, isArchived INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX index_categories_name_transactionType ON categories (name, transactionType)")
            db.execSQL("CREATE TABLE transactions (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, transactionType TEXT NOT NULL, amountMinor INTEGER NOT NULL, currencyCode TEXT NOT NULL, currencyExponent INTEGER NOT NULL, accountId TEXT NOT NULL, categoryId TEXT NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, notes TEXT)")
            db.execSQL("CREATE INDEX index_transactions_occurredAtEpochMillis ON transactions (occurredAtEpochMillis)")
            db.execSQL("CREATE INDEX index_transactions_accountId_occurredAtEpochMillis ON transactions (accountId, occurredAtEpochMillis)")
            db.execSQL("CREATE INDEX index_transactions_categoryId_occurredAtEpochMillis ON transactions (categoryId, occurredAtEpochMillis)")
            db.execSQL("CREATE TABLE transfers (id TEXT NOT NULL PRIMARY KEY, fromAccountId TEXT NOT NULL, toAccountId TEXT NOT NULL, amountMinor INTEGER NOT NULL, currencyCode TEXT NOT NULL, currencyExponent INTEGER NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, note TEXT)")
            db.execSQL("CREATE INDEX index_transfers_fromAccountId_occurredAtEpochMillis ON transfers (fromAccountId, occurredAtEpochMillis)")
            db.execSQL("CREATE INDEX index_transfers_toAccountId_occurredAtEpochMillis ON transfers (toAccountId, occurredAtEpochMillis)")
            db.execSQL("INSERT INTO local_preferences VALUES ('theme','dark',10)")
            db.execSQL("INSERT INTO expenses VALUES ('legacy-expense-1','Groceries',12345,'EGP',2,'Food',100,90)")
            db.execSQL("INSERT INTO accounts VALUES ('legacy-cash-EGP-2','Cash','EGP',2,0,0,90)")
            db.execSQL("INSERT INTO categories VALUES ('legacy-expense-food','Food','EXPENSE',NULL,0,90)")
            db.execSQL("""INSERT INTO transactions VALUES
                ('legacy-expense-1','Groceries','EXPENSE',12345,'EGP',2,'legacy-cash-EGP-2','legacy-expense-food',100,90,NULL)""")
            db.execSQL("""INSERT INTO transfers VALUES
                ('transfer-1','legacy-cash-EGP-2','legacy-cash-EGP-2',500,'EGP',2,110,100,'test')""")
            db.version = 4
        } finally {
            db.close()
        }
    }
}

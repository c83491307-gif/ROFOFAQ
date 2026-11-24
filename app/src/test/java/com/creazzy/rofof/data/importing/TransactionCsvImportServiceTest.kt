package com.creazzy.rofof.data.importing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.creazzy.rofof.data.local.AccountEntity
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.RofofDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TransactionCsvImportServiceTest {
    private lateinit var database: RofofDatabase
    private lateinit var service: TransactionCsvImportService

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            RofofDatabase::class.java
        ).allowMainThreadQueries().build()
        service = TransactionCsvImportService(
            database.transactionDao(),
            database.accountDao(),
            database.categoryDao()
        )
    }

    @After fun tearDown() {
        database.close()
    }

    @Test fun confirmedImportPersistsAllValidatedRowsAtomically() = runBlocking {
        seedReferences()
        val result = service.importConfirmed(csv(row("tx-1"), row("tx-2")))

        assertTrue(result.errors.toString(), result.succeeded)
        assertEquals(2, result.importedCount)
        assertEquals("tx-1", database.transactionDao().findById("tx-1")?.id)
        assertEquals(2500L, database.transactionDao().findById("tx-2")?.amountMinor)
    }

    @Test fun missingReferenceRejectsWholeFileWithoutPartialInsert() = runBlocking {
        seedReferences()
        val valid = row("tx-valid")
        val missingCategory = row("tx-invalid", category = "missing-category")
        val result = service.importConfirmed(csv(valid, missingCategory))

        assertFalse(result.succeeded)
        assertEquals(0, result.importedCount)
        assertTrue(result.errors.any { it.contains("category is missing") })
        assertEquals(null, database.transactionDao().findById("tx-valid"))
        assertEquals(null, database.transactionDao().findById("tx-invalid"))
    }

    @Test fun existingTransactionIdIsRejectedWithoutChangingExistingRecord() = runBlocking {
        seedReferences()
        val first = service.importConfirmed(csv(row("same-id", title = "Original")))
        assertTrue(first.succeeded)

        val duplicate = service.importConfirmed(csv(row("same-id", title = "Replacement")))
        assertFalse(duplicate.succeeded)
        assertEquals(0, duplicate.importedCount)
        assertEquals("Original", database.transactionDao().findById("same-id")?.title)
    }

    private suspend fun seedReferences() {
        database.accountDao().insert(
            AccountEntity("cash", "Cash", "EGP", 2, 0L, false, 1L)
        )
        database.categoryDao().insert(
            CategoryEntity("food", "Food", "EXPENSE", null, false, 1L)
        )
    }

    private fun csv(vararg rows: String): String =
        "id,title,type,amount,currency,currency_exponent,account_id,category_id,occurred_at_utc,created_at_utc,notes\n" +
            rows.joinToString("\n")

    private fun row(
        id: String,
        title: String = "Lunch",
        category: String = "food"
    ): String =
        "$id,$title,EXPENSE,25.00,EGP,2,cash,$category,2026-01-01T10:00:00Z,2026-01-01T10:01:00Z,Imported"
}

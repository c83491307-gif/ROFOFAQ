package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransactionDaoTest {
    private lateinit var database: RofofDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            RofofDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun incomeAndExpenseArePersistedInChronologicalOrder() = runBlocking {
        val dao = database.transactionDao()
        dao.insert(
            TransactionEntity(
                "expense", "Groceries", "EXPENSE", 12500, "EGP", 2,
                "cash", "food", 100L, 100L, null
            )
        )
        dao.insert(
            TransactionEntity(
                "income", "Salary", "INCOME", 100000, "EGP", 2,
                "cash", "salary", 200L, 200L, null
            )
        )

        val records = dao.observeRecent().first()
        assertEquals(listOf("income", "expense"), records.map { it.id })
        assertEquals("INCOME", records.first().transactionType)
    }

    @Test
    fun totalsAreScopedToCurrencyAndType() = runBlocking {
        val dao = database.transactionDao()
        dao.insert(TransactionEntity("e1", "Food", "EXPENSE", 5000, "EGP", 2, "cash", "food", 1L, 1L, null))
        dao.insert(TransactionEntity("e2", "Fuel", "EXPENSE", 2500, "USD", 2, "card", "fuel", 2L, 2L, null))
        dao.insert(TransactionEntity("i1", "Pay", "INCOME", 20000, "EGP", 2, "cash", "salary", 3L, 3L, null))

        assertEquals(5000L, dao.observeExpensesSince(0L, "EGP").first())
        assertEquals(20000L, dao.observeIncomeSince(0L, "EGP").first())
        assertEquals(2500L, dao.observeExpensesSince(0L, "USD").first())
    }

    @Test
    fun deletingOneTransactionDoesNotDeleteOthers() = runBlocking {
        val dao = database.transactionDao()
        dao.insert(TransactionEntity("one", "A", "EXPENSE", 100, "EGP", 2, "cash", "general", 1L, 1L, null))
        dao.insert(TransactionEntity("two", "B", "INCOME", 200, "EGP", 2, "cash", "general", 2L, 2L, null))
        dao.delete("one")
        assertEquals(listOf("two"), dao.observeRecent().first().map { it.id })
    }
}

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

class ExpenseDaoTest {
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
    fun expenseIsPersistedAndObservedInNewestFirstOrder() = runBlocking {
        val dao = database.expenseDao()
        dao.insert(
            ExpenseEntity("older", "Groceries", 12500, "EGP", 2, "food", 100L, 100L)
        )
        dao.insert(
            ExpenseEntity("newer", "Transport", 3500, "EGP", 2, "transport", 200L, 200L)
        )

        val observed = dao.observeRecent().first()
        assertEquals(listOf("newer", "older"), observed.map { it.id })
        assertEquals(16000L, observed.sumOf { it.amountMinor })
    }

    @Test
    fun deleteRemovesOnlySelectedExpense() = runBlocking {
        val dao = database.expenseDao()
        dao.insert(ExpenseEntity("one", "A", 100, "EGP", 2, "general", 1L, 1L))
        dao.insert(ExpenseEntity("two", "B", 200, "EGP", 2, "general", 2L, 2L))
        dao.delete("one")
        assertEquals(listOf("two"), dao.observeRecent().first().map { it.id })
    }
}

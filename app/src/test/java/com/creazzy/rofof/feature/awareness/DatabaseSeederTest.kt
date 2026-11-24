package com.creazzy.rofof.feature.awareness

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.RofofDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseSeederTest {
    private lateinit var database: RofofDatabase

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            RofofDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() = database.close()

    @Test fun seedIsIdempotentAndOnlyCreatesCategories() = runBlocking {
        val seeder = DatabaseSeeder(database)
        val first = seeder.seed(DatabaseSeeder.DEFAULT_CATEGORIES, 123L)
        val second = seeder.seed(DatabaseSeeder.DEFAULT_CATEGORIES, 456L)

        assertEquals(DatabaseSeeder.DEFAULT_CATEGORIES.size, first.inserted)
        assertEquals(0, first.alreadyPresent)
        assertEquals(0, second.inserted)
        assertEquals(DatabaseSeeder.DEFAULT_CATEGORIES.size, second.alreadyPresent)
        assertNotNull(database.categoryDao().findById("awareness-expense-food"))
        assertEquals(null, database.accountDao().findById("awareness-default-account"))
    }

    @Test fun existingMatchingCategoryIsPreservedInsteadOfOverwritten() = runBlocking {
        database.categoryDao().insert(
            CategoryEntity(
                id = "user-food", name = "Food", transactionType = "EXPENSE",
                parentId = null, isArchived = true, createdAtEpochMillis = 7L
            )
        )
        val result = DatabaseSeeder(database).seed(
            listOf(SeedCategory("seed-food", "Food", "EXPENSE")), 99L
        )
        assertEquals(0, result.inserted)
        assertEquals(1, result.alreadyPresent)
        val existing = database.categoryDao().findById("user-food")
        assertNotNull(existing)
        assertFalse(database.categoryDao().findById("seed-food") != null)
        assertEquals(true, existing?.isArchived)
        assertEquals("user-food", database.categoryDao().findByNameAndType("Food", "EXPENSE")?.id)
    }

    @Test fun invalidCategoryTypeFailsBeforeWriting() = runBlocking {
        val invalid = listOf(SeedCategory("bad", "Bad", "TRANSFER"))
        org.junit.Assert.assertTrue(runCatching { DatabaseSeeder(database).seed(invalid, 1L) }.isFailure)
        assertEquals(null, database.categoryDao().findById("bad"))
    }
}

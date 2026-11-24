package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalPreferenceDaoTest {
    private lateinit var database: RofofDatabase
    @Before fun createDatabase() { database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), RofofDatabase::class.java).allowMainThreadQueries().build() }
    @After fun closeDatabase() { database.close() }

    @Test fun upsertAndObservePersistsLocalValue() = runBlocking {
        val dao = database.localPreferenceDao()
        dao.put(LocalPreferenceEntity("locale", "ar", 123L))
        assertEquals("ar", dao.observe("locale").first()?.value)
    }

    @Test fun deleteRemovesLocalValue() = runBlocking {
        val dao = database.localPreferenceDao()
        dao.put(LocalPreferenceEntity("theme", "light", 123L))
        dao.delete("theme")
        assertNull(dao.observe("theme").first())
    }
}

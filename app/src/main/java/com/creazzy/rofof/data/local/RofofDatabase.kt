package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LocalPreferenceEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RofofDatabase : RoomDatabase() {
    abstract fun localPreferenceDao(): LocalPreferenceDao

    companion object {
        @Volatile
        private var instance: RofofDatabase? = null

        fun getInstance(context: Context): RofofDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RofofDatabase::class.java,
                    "rofof-local.db"
                ).build().also { instance = it }
            }
    }
}

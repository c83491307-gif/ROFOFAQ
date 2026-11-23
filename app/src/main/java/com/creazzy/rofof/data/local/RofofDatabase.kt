package com.creazzy.rofof.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LocalPreferenceEntity::class, ExpenseEntity::class],
    version = 2,
    exportSchema = true
)
abstract class RofofDatabase : RoomDatabase() {
    abstract fun localPreferenceDao(): LocalPreferenceDao
    abstract fun expenseDao(): ExpenseDao

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

        fun getInstance(context: Context): RofofDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RofofDatabase::class.java,
                    "rofof-local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}

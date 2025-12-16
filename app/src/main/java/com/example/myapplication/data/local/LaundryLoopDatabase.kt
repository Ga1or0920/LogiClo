package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.local.dao.ClothingItemDao
import com.example.myapplication.data.local.dao.UserPreferencesDao
import com.example.myapplication.data.local.dao.WearFeedbackDao
import com.example.myapplication.data.local.entity.ClothingItemEntity
import com.example.myapplication.data.local.entity.UserPreferencesEntity
import com.example.myapplication.data.local.entity.WearFeedbackEntity

@Database(
    entities = [
        ClothingItemEntity::class,
        UserPreferencesEntity::class,
        WearFeedbackEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class LaundryLoopDatabase : RoomDatabase() {
    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun wearFeedbackDao(): WearFeedbackDao

    companion object {
        const val NAME: String = "laundry_loop.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN lastSelectedEnvironment TEXT NOT NULL DEFAULT 'outdoor'"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN defaultMaxWearsJson TEXT NOT NULL DEFAULT '{}'"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE clothing_items ADD COLUMN brand TEXT"
                )
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS wear_feedback_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        wornAtEpochMillis INTEGER NOT NULL,
                        topItemId TEXT,
                        bottomItemId TEXT,
                        rating TEXT,
                        notes TEXT,
                        submittedAtEpochMillis INTEGER
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_wear_feedback_pending ON wear_feedback_entries(rating, wornAtEpochMillis)"
                )
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE clothing_items ADD COLUMN comfortMinCelsius REAL")
                database.execSQL("ALTER TABLE clothing_items ADD COLUMN comfortMaxCelsius REAL")
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN weatherLocationLabel TEXT")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN weatherLocationLatitude REAL")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN weatherLocationLongitude REAL")
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN indoorTemperatureCelsius REAL")
            }
        }
        
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wear_feedback_entries ADD COLUMN topRating TEXT")
                database.execSQL("ALTER TABLE wear_feedback_entries ADD COLUMN bottomRating TEXT")
            }
        }

        @Volatile
        private var instance: LaundryLoopDatabase? = null

        fun getInstance(context: Context): LaundryLoopDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): LaundryLoopDatabase {
            return Room.databaseBuilder(
                context,
                LaundryLoopDatabase::class.java,
                NAME
            ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
            ).build()
        }
    }
}

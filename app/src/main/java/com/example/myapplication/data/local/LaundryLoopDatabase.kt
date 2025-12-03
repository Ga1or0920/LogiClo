package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.local.dao.ClothingItemDao
import com.example.myapplication.data.local.dao.UserPreferencesDao
import com.example.myapplication.data.local.entity.ClothingItemEntity
import com.example.myapplication.data.local.entity.UserPreferencesEntity

@Database(
    entities = [ClothingItemEntity::class, UserPreferencesEntity::class],
    version = 3,
    exportSchema = false
)
abstract class LaundryLoopDatabase : RoomDatabase() {
    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun userPreferencesDao(): UserPreferencesDao

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
    }
}

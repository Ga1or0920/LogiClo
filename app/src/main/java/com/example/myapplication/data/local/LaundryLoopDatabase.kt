package com.example.myapplication.data.local

import android.content.Context
import android.graphics.Color
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
    version = 11,
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

        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // カラムが存在しない場合のみ追加（重複エラー回避）
                addColumnIfNotExists(database, "clothing_items", "imageUrl", "TEXT")
                addColumnIfNotExists(database, "clothing_items", "lastWornEpochMillis", "INTEGER")
            }
        }

        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Data backfill: normalize legacy enum-name strings and backfill colorGroup from colorHex.
                database.beginTransaction()
                try {
                    val cursor = database.query(
                        "SELECT id, category, colorHex, colorGroup, pattern FROM clothing_items"
                    )
                    cursor.use { c ->
                        val idIndex = c.getColumnIndex("id")
                        val categoryIndex = c.getColumnIndex("category")
                        val colorHexIndex = c.getColumnIndex("colorHex")
                        val colorGroupIndex = c.getColumnIndex("colorGroup")
                        val patternIndex = c.getColumnIndex("pattern")

                        while (c.moveToNext()) {
                            val id = c.getString(idIndex)
                            val rawCategory = c.getString(categoryIndex)
                            val rawColorHex = c.getString(colorHexIndex)
                            val rawColorGroup = c.getString(colorGroupIndex)
                            val rawPattern = c.getString(patternIndex)

                            val normalizedCategory = normalizeCategory(rawCategory)
                            val normalizedPattern = normalizePattern(rawPattern)
                            val normalizedColorGroup = normalizeOrInferColorGroup(rawColorGroup, rawColorHex)

                            // Only write when something changes to reduce IO.
                            val needsUpdate =
                                normalizedCategory != safeLowerTrim(rawCategory) ||
                                    normalizedPattern != safeLowerTrim(rawPattern) ||
                                    normalizedColorGroup != safeLowerTrim(rawColorGroup)

                            if (needsUpdate) {
                                database.execSQL(
                                    "UPDATE clothing_items SET category = ?, colorGroup = ?, pattern = ? WHERE id = ?",
                                    arrayOf(normalizedCategory, normalizedColorGroup, normalizedPattern, id)
                                )
                            }
                        }
                    }
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }

        private fun addColumnIfNotExists(
            database: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnType: String
        ) {
            val cursor = database.query("PRAGMA table_info($tableName)")
            val columnExists = cursor.use {
                val nameIndex = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    if (it.getString(nameIndex) == columnName) {
                        return@use true
                    }
                }
                false
            }
            if (!columnExists) {
                database.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnType")
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
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11
            ).build()
        }

        private fun safeLowerTrim(value: String?): String {
            return value?.trim()?.lowercase(java.util.Locale.ROOT) ?: ""
        }

        private fun normalizeCategory(raw: String?): String {
            val value = raw?.trim().orEmpty()
            if (value.isBlank()) return "unknown"
            val lower = value.lowercase(java.util.Locale.ROOT).replace('-', '_')
            // If already backend value, keep.
            if (
                lower in setOf(
                    "t_shirt",
                    "polo",
                    "dress_shirt",
                    "knit",
                    "sweatshirt",
                    "denim",
                    "slacks",
                    "chino",
                    "outer_light",
                    "down",
                    "coat",
                    "inner",
                    "jacket",
                    "fleece",
                    "windbreaker",
                    "unknown"
                )
            ) {
                return lower
            }

            return when (value.uppercase(java.util.Locale.ROOT)) {
                "T_SHIRT" -> "t_shirt"
                "POLO" -> "polo"
                "DRESS_SHIRT" -> "dress_shirt"
                "KNIT" -> "knit"
                "SWEATSHIRT" -> "sweatshirt"
                "DENIM" -> "denim"
                "SLACKS" -> "slacks"
                "CHINO" -> "chino"
                "OUTER_LIGHT" -> "outer_light"
                "DOWN" -> "down"
                "COAT" -> "coat"
                "INNER" -> "inner"
                "JACKET" -> "jacket"
                "FLEECE" -> "fleece"
                "WINDBREAKER" -> "windbreaker"
                else -> "unknown"
            }
        }

        private fun normalizePattern(raw: String?): String {
            val value = raw?.trim().orEmpty()
            if (value.isBlank()) return "unknown"
            val lower = value.lowercase(java.util.Locale.ROOT)
            if (lower in setOf("solid", "stripe", "graphic", "unknown")) return lower

            return when (value.uppercase(java.util.Locale.ROOT)) {
                "SOLID" -> "solid"
                "STRIPE" -> "stripe"
                "GRAPHIC" -> "graphic"
                else -> "unknown"
            }
        }

        private fun normalizeOrInferColorGroup(rawColorGroup: String?, rawColorHex: String?): String {
            val value = rawColorGroup?.trim().orEmpty()
            if (value.isBlank() || value.equals("unknown", ignoreCase = true)) {
                return inferColorGroupFromHex(rawColorHex)
            }

            val lower = value.lowercase(java.util.Locale.ROOT)
            if (lower in setOf("monotone", "navy_blue", "vivid", "earth_tone", "pastel", "other", "unknown")) {
                return lower
            }

            // Legacy enum-name strings
            return when (value.uppercase(java.util.Locale.ROOT)) {
                "MONOTONE" -> "monotone"
                "NAVY_BLUE" -> "navy_blue"
                "VIVID" -> "vivid"
                "EARTH_TONE" -> "earth_tone"
                "PASTEL" -> "pastel"
                "OTHER" -> "other"
                else -> inferColorGroupFromHex(rawColorHex)
            }
        }

        private fun inferColorGroupFromHex(rawColorHex: String?): String {
            val hex = rawColorHex?.trim().orEmpty()
            if (hex.isBlank()) return "unknown"

            return try {
                val colorInt = Color.parseColor(hex)
                val r = (colorInt shr 16 and 0xFF) / 255.0
                val g = (colorInt shr 8 and 0xFF) / 255.0
                val b = (colorInt and 0xFF) / 255.0

                val maxc = maxOf(r, g, b)
                val minc = minOf(r, g, b)
                val l = (maxc + minc) / 2.0
                val delta = maxc - minc
                val s = if (delta == 0.0) 0.0 else delta / (1.0 - kotlin.math.abs(2.0 * l - 1.0))

                var h = 0.0
                if (delta != 0.0) {
                    h = when (maxc) {
                        r -> ((g - b) / delta) % 6.0
                        g -> ((b - r) / delta) + 2.0
                        else -> ((r - g) / delta) + 4.0
                    }
                    h *= 60.0
                    if (h < 0) h += 360.0
                }

                if (s < 0.12) return "monotone"
                if (h in 210.0..260.0 && l < 0.45) return "navy_blue"
                if (l > 0.75 && s in 0.15..0.55) return "pastel"
                if (h in 20.0..70.0 && s < 0.45) return "earth_tone"
                if (s >= 0.55) return "vivid"
                "other"
            } catch (e: Exception) {
                "unknown"
            }
        }
    }
}

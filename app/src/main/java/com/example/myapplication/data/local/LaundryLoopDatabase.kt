package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class LaundryLoopDatabase : RoomDatabase() {
    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun wearFeedbackDao(): WearFeedbackDao

    companion object {
        @Volatile
        private var Instance: LaundryLoopDatabase? = null

        fun getInstance(context: Context): LaundryLoopDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context = context,
                    klass = LaundryLoopDatabase::class.java,
                    name = "laundry_loop_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

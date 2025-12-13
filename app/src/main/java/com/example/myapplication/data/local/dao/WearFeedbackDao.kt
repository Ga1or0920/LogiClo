package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.example.myapplication.data.local.entity.WearFeedbackEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

class DateConverter {
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Dao
@TypeConverters(DateConverter::class)
interface WearFeedbackDao {
    @Query("SELECT * FROM wear_feedback ORDER BY wornAt DESC")
    fun observeAll(): Flow<List<WearFeedbackEntity>>

    @Query("SELECT * FROM wear_feedback WHERE isComfortable IS NULL ORDER BY wornAt DESC LIMIT 1")
    fun observeLatestPending(): Flow<WearFeedbackEntity?>

    @Upsert
    suspend fun upsert(entity: WearFeedbackEntity)

    @Query("DELETE FROM wear_feedback WHERE wornAt < :thresholdEpochMillis")
    suspend fun pruneHistory(thresholdEpochMillis: Long)
}

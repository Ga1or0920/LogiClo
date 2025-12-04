package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.myapplication.data.local.entity.WearFeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WearFeedbackDao {
    @Upsert
    suspend fun upsert(entity: WearFeedbackEntity)

    @Query("SELECT * FROM wear_feedback_entries WHERE id = :id")
    suspend fun getById(id: String): WearFeedbackEntity?

    @Query("SELECT * FROM wear_feedback_entries WHERE rating IS NULL ORDER BY wornAtEpochMillis DESC LIMIT 1")
    fun observeLatestPending(): Flow<WearFeedbackEntity?>

    @Query("SELECT * FROM wear_feedback_entries WHERE rating IS NULL ORDER BY wornAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestPending(): WearFeedbackEntity?

    @Query(
        "UPDATE wear_feedback_entries SET rating = :rating, notes = :notes, submittedAtEpochMillis = :submittedAtEpochMillis WHERE id = :id"
    )
    suspend fun updateSubmission(
        id: String,
        rating: String?,
        notes: String?,
        submittedAtEpochMillis: Long?
    )

    @Query("DELETE FROM wear_feedback_entries WHERE rating IS NOT NULL AND wornAtEpochMillis < :thresholdEpochMillis")
    suspend fun pruneSubmittedBefore(thresholdEpochMillis: Long)
}

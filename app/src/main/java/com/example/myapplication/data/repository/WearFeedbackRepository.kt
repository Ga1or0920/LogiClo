package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.WearFeedbackEntry
import com.example.myapplication.domain.model.WearFeedbackRating
import kotlinx.coroutines.flow.Flow

interface WearFeedbackRepository {
    fun observeLatestPending(): Flow<WearFeedbackEntry?>
    suspend fun getLatestPending(): WearFeedbackEntry?
    suspend fun recordWear(topItemId: String?, bottomItemId: String?)
    suspend fun submitFeedback(entryId: String, rating: WearFeedbackRating, notes: String?)
    suspend fun pruneHistory(beforeEpochMillis: Long)
}

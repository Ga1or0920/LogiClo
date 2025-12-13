package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.WearFeedback
import kotlinx.coroutines.flow.Flow

interface WearFeedbackRepository {
    fun observeAll(): Flow<List<WearFeedback>>
    fun observeLatestPending(): Flow<WearFeedback?>
    suspend fun recordWear(items: List<ClothingItem>)
    suspend fun update(feedback: WearFeedback)
    suspend fun pruneHistory(threshold: Long)
}

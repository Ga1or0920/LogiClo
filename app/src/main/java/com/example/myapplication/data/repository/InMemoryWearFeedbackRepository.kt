package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.WearFeedback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date

class InMemoryWearFeedbackRepository : WearFeedbackRepository {

    private val feedbacks = MutableStateFlow<Map<String, WearFeedback>>(emptyMap())

    override fun observeAll(): Flow<List<WearFeedback>> = feedbacks.map { it.values.toList() }

    override fun observeLatestPending(): Flow<WearFeedback?> = feedbacks.map {
        it.values.filter { it.isComfortable == null }.maxByOrNull { it.wornAt }
    }

    override suspend fun recordWear(items: List<ClothingItem>) {
        if (items.isEmpty()) return
        val newFeedback = WearFeedback(
            id = items.joinToString("-") { it.id },
            wornAt = Date(),
            itemIds = items.map { it.id }
        )
        feedbacks.value += (newFeedback.id to newFeedback)
    }

    override suspend fun update(feedback: WearFeedback) {
        feedbacks.value += (feedback.id to feedback)
    }

    override suspend fun pruneHistory(threshold: Long) {
        feedbacks.value = feedbacks.value.filterValues { it.wornAt.time >= threshold }
    }
}

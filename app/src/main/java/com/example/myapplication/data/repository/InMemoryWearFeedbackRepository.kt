package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.WearFeedbackEntry
import com.example.myapplication.domain.model.WearFeedbackRating
import com.example.myapplication.util.time.InstantCompat
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryWearFeedbackRepository : WearFeedbackRepository {

    private val entriesState = MutableStateFlow<Map<String, WearFeedbackEntry>>(emptyMap())

    override fun observeLatestPending(): Flow<WearFeedbackEntry?> =
        entriesState.map { state -> state.values.filter { it.isPending }.maxByOrNull { it.pendingTimestamp() } }

    override suspend fun getLatestPending(): WearFeedbackEntry? {
        return entriesState.value.values.filter { it.isPending }.maxByOrNull { it.pendingTimestamp() }
    }

    override suspend fun recordWear(topItemId: String?, bottomItemId: String?) {
        val now = InstantCompat.nowOrNull()
        val entry = WearFeedbackEntry(
            id = UUID.randomUUID().toString(),
            wornAt = now,
            topItemId = topItemId,
            bottomItemId = bottomItemId,
            rating = null,
            notes = null,
            submittedAt = null
        )
        entriesState.update { current -> current + (entry.id to entry) }
    }

    override suspend fun submitFeedback(entryId: String, rating: WearFeedbackRating, notes: String?) {
        val submittedAt = InstantCompat.nowOrNull()
        entriesState.update { current ->
            val existing = current[entryId] ?: return@update current
            current + (entryId to existing.copy(
                rating = rating,
                notes = notes?.takeIf { it.isNotBlank() },
                submittedAt = submittedAt
            ))
        }
    }

    override suspend fun pruneHistory(beforeEpochMillis: Long) {
        entriesState.update { current ->
            current.filterValues { entry ->
                val wornAtMillis = InstantCompat.toEpochMilliOrNull(entry.wornAt) ?: Long.MAX_VALUE
                entry.rating == null || wornAtMillis >= beforeEpochMillis
            }
        }
    }
}

private fun WearFeedbackEntry.pendingTimestamp(): Long {
    return InstantCompat.toEpochMilliOrNull(wornAt) ?: Long.MIN_VALUE
}

package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.WearFeedbackDao
import com.example.myapplication.data.local.entity.WearFeedbackEntity
import com.example.myapplication.domain.model.WearFeedbackEntry
import com.example.myapplication.domain.model.WearFeedbackRating
import com.example.myapplication.util.time.InstantCompat
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWearFeedbackRepository(
    private val dao: WearFeedbackDao
) : WearFeedbackRepository {

    override fun observeLatestPending(): Flow<WearFeedbackEntry?> =
        dao.observeLatestPending().map { entity -> entity?.toDomain() }

    override suspend fun getLatestPending(): WearFeedbackEntry? {
        return dao.getLatestPending()?.toDomain()
    }

    override suspend fun recordWear(topItemId: String?, bottomItemId: String?) {
        val nowMillis = InstantCompat.nowOrNull()?.let(InstantCompat::toEpochMilliOrNull)
            ?: System.currentTimeMillis()
        val entity = WearFeedbackEntity(
            id = UUID.randomUUID().toString(),
            wornAtEpochMillis = nowMillis,
            topItemId = topItemId,
            bottomItemId = bottomItemId,
            rating = null,
            notes = null,
            submittedAtEpochMillis = null
        )
        dao.upsert(entity)
    }

    override suspend fun submitFeedback(entryId: String, rating: WearFeedbackRating, notes: String?) {
        val submittedMillis = InstantCompat.nowOrNull()?.let(InstantCompat::toEpochMilliOrNull)
            ?: System.currentTimeMillis()
        dao.updateSubmission(
            id = entryId,
            rating = rating.backendValue,
            notes = notes?.takeIf { it.isNotBlank() },
            submittedAtEpochMillis = submittedMillis
        )
    }

    override suspend fun pruneHistory(beforeEpochMillis: Long) {
        dao.pruneSubmittedBefore(beforeEpochMillis)
    }
}

private fun WearFeedbackEntity.toDomain(): WearFeedbackEntry {
    return WearFeedbackEntry(
        id = id,
        wornAt = InstantCompat.ofEpochMilliOrNull(wornAtEpochMillis),
        topItemId = topItemId,
        bottomItemId = bottomItemId,
        rating = WearFeedbackRating.fromBackend(rating),
        notes = notes,
        submittedAt = InstantCompat.ofEpochMilliOrNull(submittedAtEpochMillis)
    )
}

package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.WearFeedbackDao
import com.example.myapplication.data.local.entity.toDomain
import com.example.myapplication.data.local.entity.toEntity
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.WearFeedback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class RoomWearFeedbackRepository(
    private val wearFeedbackDao: WearFeedbackDao
) : WearFeedbackRepository {

    override fun observeAll(): Flow<List<WearFeedback>> = wearFeedbackDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }

    override fun observeLatestPending(): Flow<WearFeedback?> = wearFeedbackDao.observeLatestPending()
        .map { entity -> entity?.toDomain() }

    override suspend fun recordWear(items: List<ClothingItem>) {
        if (items.isEmpty()) return
        val entity = WearFeedback(
            id = items.joinToString("-") { it.id },
            wornAt = Date(),
            itemIds = items.map { it.id }
        ).toEntity()
        wearFeedbackDao.upsert(entity)
    }

    override suspend fun update(feedback: WearFeedback) {
        wearFeedbackDao.upsert(feedback.toEntity())
    }

    override suspend fun pruneHistory(threshold: Long) {
        wearFeedbackDao.pruneHistory(threshold)
    }
}

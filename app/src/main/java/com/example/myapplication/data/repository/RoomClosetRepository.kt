package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.ClothingItemDao
import com.example.myapplication.data.local.entity.toDomain
import com.example.myapplication.data.local.entity.toEntity
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.LaundryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomClosetRepository(
    private val clothingItemDao: ClothingItemDao
) : ClosetRepository {

    override fun observeAll(): Flow<List<ClothingItem>> = clothingItemDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }

    override fun observeByStatus(status: LaundryStatus): Flow<List<ClothingItem>> =
        clothingItemDao.observeByStatus(status.backendValue)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsert(item: ClothingItem) {
        clothingItemDao.upsertItem(item.toEntity())
    }

    override suspend fun upsert(items: List<ClothingItem>) {
        clothingItemDao.upsertItems(items.map { it.toEntity() })
    }

    override suspend fun delete(id: String) {
        clothingItemDao.deleteItem(id)
    }

    override suspend fun getItem(id: String): ClothingItem? {
        return clothingItemDao.getById(id)?.toDomain()
    }

    override suspend fun getItems(ids: Collection<String>): List<ClothingItem> {
        if (ids.isEmpty()) return emptyList()
        return clothingItemDao.getByIds(ids.toList()).map { it.toDomain() }
    }
}

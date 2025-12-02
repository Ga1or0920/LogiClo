package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.LaundryStatus
import kotlinx.coroutines.flow.Flow

interface ClosetRepository {
    fun observeAll(): Flow<List<ClothingItem>>
    fun observeByStatus(status: LaundryStatus): Flow<List<ClothingItem>>
    suspend fun upsert(item: ClothingItem)
    suspend fun upsert(items: List<ClothingItem>)
    suspend fun delete(id: String)
}

package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.myapplication.data.local.entity.ClothingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingItemDao {
    @Query("SELECT * FROM clothing_items")
    fun observeAll(): Flow<List<ClothingItemEntity>>

    @Query("SELECT * FROM clothing_items WHERE status = :status")
    fun observeByStatus(status: String): Flow<List<ClothingItemEntity>>

    @Query("SELECT COUNT(*) FROM clothing_items")
    suspend fun countItems(): Int

    @Upsert
    suspend fun upsertItem(entity: ClothingItemEntity)

    @Upsert
    suspend fun upsertItems(entities: List<ClothingItemEntity>)

    @Query("DELETE FROM clothing_items WHERE id = :id")
    suspend fun deleteItem(id: String)
}

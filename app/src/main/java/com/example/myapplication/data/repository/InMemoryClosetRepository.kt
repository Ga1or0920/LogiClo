package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.LaundryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Room 実装に先立ってモックデータを扱うための簡易リポジトリ。
 */
class InMemoryClosetRepository(
    initialItems: List<ClothingItem> = emptyList()
) : ClosetRepository {

    private val itemsState = MutableStateFlow(initialItems.associateBy { it.id })

    override fun observeAll(): Flow<List<ClothingItem>> = itemsState.map { it.values.sortedBy(ClothingItem::name) }

    override fun observeByStatus(status: LaundryStatus): Flow<List<ClothingItem>> = itemsState.map { state ->
        state.values.filter { it.status == status }.sortedBy(ClothingItem::name)
    }

    override suspend fun upsert(item: ClothingItem) {
        itemsState.update { current -> current + (item.id to item) }
    }

    override suspend fun upsert(items: List<ClothingItem>) {
        if (items.isEmpty()) return
        itemsState.update { current -> current + items.associateBy { it.id } }
    }

    override suspend fun delete(id: String) {
        itemsState.update { current -> current - id }
    }
}

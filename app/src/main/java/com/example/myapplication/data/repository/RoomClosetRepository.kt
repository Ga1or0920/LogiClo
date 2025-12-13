package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.ClothingItemDao
import com.example.myapplication.data.local.entity.toDomain
import com.example.myapplication.data.local.entity.toEntity
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.LaundryStatus
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RoomClosetRepository(
    private val clothingItemDao: ClothingItemDao
) : ClosetRepository {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    override fun observeAll(): Flow<List<ClothingItem>> = clothingItemDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }

    override fun observeByStatus(status: LaundryStatus): Flow<List<ClothingItem>> =
        clothingItemDao.observeByStatus(status.backendValue)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsert(item: ClothingItem) {
        clothingItemDao.upsertItem(item.toEntity())
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId)
                .collection("clothing_items").document(item.id)
                .set(item).await()
        }
    }

    override suspend fun upsert(items: List<ClothingItem>) {
        clothingItemDao.upsertItems(items.map { it.toEntity() })
        auth.currentUser?.uid?.let { userId ->
            val userDoc = firestore.collection("users").document(userId)
            val batch = firestore.batch()
            items.forEach { item ->
                val docRef = userDoc.collection("clothing_items").document(item.id)
                batch.set(docRef, item)
            }
            batch.commit().await()
        }
    }

    override suspend fun delete(id: String) {
        clothingItemDao.deleteItem(id)
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId)
                .collection("clothing_items").document(id)
                .delete().await()
        }
    }

    override suspend fun getItem(id: String): ClothingItem? {
        return clothingItemDao.getById(id)?.toDomain()
    }

    override suspend fun getItems(ids: Collection<String>): List<ClothingItem> {
        if (ids.isEmpty()) return emptyList()
        return clothingItemDao.getByIds(ids.toList()).map { it.toDomain() }
    }

    override suspend fun syncUp(userId: String) {
        val items = observeAll().first()
        val userDoc = firestore.collection("users").document(userId)
        val batch = firestore.batch()
        items.forEach { item ->
            val docRef = userDoc.collection("clothing_items").document(item.id)
            batch.set(docRef, item)
        }
        batch.commit().await()
    }

    override suspend fun syncDown(userId: String) {
        val userDoc = firestore.collection("users").document(userId)
        val remoteItems = userDoc.collection("clothing_items").get().await().toObjects<ClothingItem>()
        clearAll()
        clothingItemDao.upsertItems(remoteItems.map { it.toEntity() })
    }

    override suspend fun clearAll() {
        clothingItemDao.deleteAll()
    }

    override suspend fun seedSampleData() {
        if (clothingItemDao.countItems() == 0) {
            clothingItemDao.upsertItems(SampleData.closetItems.map { it.toEntity() })
        }
    }
}

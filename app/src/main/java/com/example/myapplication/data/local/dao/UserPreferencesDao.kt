package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.myapplication.data.local.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = :id LIMIT 1")
    fun observe(id: Int = UserPreferencesEntity.SINGLETON_ID): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = UserPreferencesEntity.SINGLETON_ID): UserPreferencesEntity?

    @Upsert
    suspend fun upsert(entity: UserPreferencesEntity)
}

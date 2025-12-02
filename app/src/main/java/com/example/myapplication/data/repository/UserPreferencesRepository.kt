package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun observe(): Flow<UserPreferences>
    suspend fun upsert(preferences: UserPreferences)
    suspend fun update(transform: (UserPreferences) -> UserPreferences)
    suspend fun updateLastSelectedMode(mode: TpoMode)
    suspend fun updateLastSelectedEnvironment(mode: EnvironmentMode)
}

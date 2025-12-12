package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.UserPreferencesDao
import com.example.myapplication.data.local.entity.toDomain
import com.example.myapplication.data.local.entity.toEntity
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUserPreferencesRepository(
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {

    override fun observe(): Flow<UserPreferences> = userPreferencesDao.observe()
        .map { entity -> entity?.toDomain() ?: UserPreferences() }

    override suspend fun upsert(preferences: UserPreferences) {
        userPreferencesDao.upsert(preferences.toEntity())
    }

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        val current = userPreferencesDao.get()?.toDomain() ?: UserPreferences()
        userPreferencesDao.upsert(transform(current).toEntity())
    }

    override suspend fun updateLastSelectedMode(mode: TpoMode) {
        val current = userPreferencesDao.get()?.toDomain() ?: UserPreferences()
        val updated = current.copy(lastSelectedMode = mode)
        userPreferencesDao.upsert(updated.toEntity())
    }

    override suspend fun updateLastSelectedEnvironment(mode: EnvironmentMode) {
        val current = userPreferencesDao.get()?.toDomain() ?: UserPreferences()
        val updated = current.copy(lastSelectedEnvironment = mode)
        userPreferencesDao.upsert(updated.toEntity())
    }

    override suspend fun getEmailForSignIn(): String? {
        return userPreferencesDao.get()?.emailForSignIn
    }

    override suspend fun setEmailForSignIn(email: String) {
        val current = userPreferencesDao.get()?.toDomain() ?: UserPreferences()
        val updated = current.copy(emailForSignIn = email)
        userPreferencesDao.upsert(updated.toEntity())
    }
}

package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class InMemoryUserPreferencesRepository(
    initialPreferences: UserPreferences
) : UserPreferencesRepository {

    private val state = MutableStateFlow(initialPreferences)

    override fun observe(): Flow<UserPreferences> = state

    override suspend fun upsert(preferences: UserPreferences) {
        state.value = preferences
    }

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        state.update(transform)
    }

    override suspend fun updateLastSelectedMode(mode: TpoMode) {
        state.update { current -> current.copy(lastSelectedMode = mode) }
    }

    override suspend fun updateLastSelectedEnvironment(mode: EnvironmentMode) {
        state.update { current -> current.copy(lastSelectedEnvironment = mode) }
    }
}

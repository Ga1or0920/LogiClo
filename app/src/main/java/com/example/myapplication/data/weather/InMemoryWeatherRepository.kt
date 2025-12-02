package com.example.myapplication.data.weather

import com.example.myapplication.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryWeatherRepository(initialWeather: WeatherSnapshot) : WeatherRepository {

    private val state = MutableStateFlow(initialWeather)

    override fun observeCurrentWeather(): Flow<WeatherSnapshot> = state

    override suspend fun refresh() {
        // No-op for in-memory repository.
    }

    fun update(snapshot: WeatherSnapshot) {
        state.value = snapshot
    }
}

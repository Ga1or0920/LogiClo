package com.example.myapplication.data.weather

import com.example.myapplication.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class OpenMeteoWeatherRepository(
    private var coordinates: Coordinates,
    initialSnapshot: WeatherSnapshot
) : WeatherRepository {

    private val weatherState = MutableStateFlow(initialSnapshot)

    override fun observeCurrentWeather(): Flow<WeatherSnapshot> = weatherState

    override suspend fun refresh() {
        // No-op
    }

    fun updateCoordinates(coordinates: Coordinates) {
        this.coordinates = coordinates
    }

    data class Coordinates(
        val latitude: Double,
        val longitude: Double
    )
}

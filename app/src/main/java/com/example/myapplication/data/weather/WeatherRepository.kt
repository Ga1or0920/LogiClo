package com.example.myapplication.data.weather

import com.example.myapplication.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun observeCurrentWeather(): Flow<WeatherSnapshot>
    suspend fun refresh()
}

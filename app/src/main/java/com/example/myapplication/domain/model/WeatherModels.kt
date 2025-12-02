package com.example.myapplication.domain.model

import java.time.Instant

data class WeatherSnapshot(
    val minTemperatureCelsius: Double,
    val maxTemperatureCelsius: Double,
    val humidityPercent: Int,
    val updatedAt: Instant? = null
)

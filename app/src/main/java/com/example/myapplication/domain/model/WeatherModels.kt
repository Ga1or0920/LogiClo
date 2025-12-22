package com.example.myapplication.domain.model

import java.time.Instant

data class WeatherSnapshot(
    val minTemperatureCelsius: Double,
    val maxTemperatureCelsius: Double,
    val apparentTemperatureCelsius: Double,
    val humidityPercent: Int,
    val updatedAt: Instant? = null,
    val casualSegmentSummaries: List<CasualForecastSegmentSummary> = emptyList()
)

enum class CasualForecastDay {
    TODAY,
    TOMORROW
}

enum class CasualForecastSegment {
    MORNING,
    AFTERNOON,
    EVENING
}

data class CasualForecastSegmentSummary(
    val day: CasualForecastDay,
    val segment: CasualForecastSegment,
    val minTemperatureCelsius: Double,
    val maxTemperatureCelsius: Double,
    val averageApparentTemperatureCelsius: Double
)

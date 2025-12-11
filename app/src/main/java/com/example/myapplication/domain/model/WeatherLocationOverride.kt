package com.example.myapplication.domain.model

/**
 * ユーザーが任意に指定した天気参照地点。
 */
data class WeatherLocationOverride(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

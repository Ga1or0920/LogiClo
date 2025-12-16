package com.example.myapplication.domain.model

import java.time.Instant

/**
 * ユーザー設定やカラールールなど、Firestore の users コレクションを想定したモデル。
 */

data class ColorRules(
    val allowBlackNavy: Boolean = false,
    val disallowVividPair: Boolean = true
)

data class UserPreferences(
    val lastLogin: Instant? = null,
    val lastSelectedMode: TpoMode = TpoMode.CASUAL,
    val lastSelectedEnvironment: EnvironmentMode = EnvironmentMode.OUTDOOR,
    val tempOffsets: Map<Thickness, Int> = emptyMap(),
    val colorRules: ColorRules = ColorRules(),
    val defaultMaxWears: Map<ClothingCategory, Int> = emptyMap(),
    val weatherLocationOverride: WeatherLocationOverride? = null,
    val indoorTemperatureCelsius: Double? = null
)

enum class TpoMode(val backendValue: String) {
    CASUAL("casual"),
    OFFICE("office"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): TpoMode = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

enum class EnvironmentMode(val backendValue: String) {
    OUTDOOR("outdoor"),
    INDOOR("indoor"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): EnvironmentMode = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

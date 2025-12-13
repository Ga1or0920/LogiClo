package com.example.myapplication.domain.model

import java.util.Date

enum class ThemeOption(val backendValue: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromBackend(value: String): ThemeOption = entries.firstOrNull { it.backendValue == value }
            ?: SYSTEM
    }
}

data class ColorRules(
    val allowBlackNavy: Boolean = false,
    val disallowVividPair: Boolean = true
)

data class UserPreferences(
    val lastLogin: Date? = null,
    val lastSelectedMode: TpoMode = TpoMode.CASUAL,
    val lastSelectedEnvironment: EnvironmentMode = EnvironmentMode.OUTDOOR,
    val tempOffsets: Map<Thickness, Int> = emptyMap(),
    val colorRules: ColorRules = ColorRules(),
    val defaultMaxWears: Map<ClothingCategory, Int> = emptyMap(),
    val weatherLocationOverride: WeatherLocationOverride? = null,
    val emailForSignIn: String? = null,
    val theme: ThemeOption = ThemeOption.SYSTEM
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

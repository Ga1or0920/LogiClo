package com.example.myapplication.ui.dashboard.model

import com.example.myapplication.data.weather.WeatherDebugOverride
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.domain.model.WearFeedback
import com.example.myapplication.ui.common.UiMessage
import com.example.myapplication.util.time.ManualTimeOverride
import java.util.Date

data class DashboardState(
    val closet: List<ClothingItem> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val wearFeedback: List<WearFeedback> = emptyList(),
    val weather: WeatherSnapshot? = null,
    val recommendations: List<UiMessage> = emptyList(),
    val weatherDebugOverride: WeatherDebugOverride? = null,
    val clockNextDayEnabled: Boolean = false,
    val clockManualOverride: ManualTimeOverride? = null
)

fun DashboardState.toUi(stringResolver: (Int) -> String): DashboardUiState {
    val weatherUi = weather?.let {
        WeatherUiState(
            lastUpdatedAt = it.updatedAt,
            location = preferences.weatherLocationOverride?.label,
            minTemperature = it.minTemperatureCelsius,
            maxTemperature = it.maxTemperatureCelsius,
            humidity = it.humidityPercent,
            isRefreshing = false // This should be handled by the repository
        )
    }

    return DashboardUiState(
        isLoading = false,
        weather = weatherUi,
        suggestions = emptyList(), // TODO: Populate this with actual suggestions
        recommendations = recommendations,
        alerts = emptyList(), // TODO: Populate this with actual alerts
        weatherDebug = null, // TODO: Map debug states
        clockDebug = null,
        wearFeedbackDebug = null
    )
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val weather: WeatherUiState? = null,
    val suggestions: List<ClothingItem> = emptyList(),
    val recommendations: List<UiMessage> = emptyList(),
    val alerts: List<UiMessage> = emptyList(),
    val weatherDebug: WeatherDebugUiState? = null,
    val clockDebug: ClockDebugUiState? = null,
    val wearFeedbackDebug: WearFeedbackDebugUiState? = null
)

data class WeatherUiState(
    val lastUpdatedAt: Date? = null,
    val location: String? = null,
    val minTemperature: Double? = null,
    val maxTemperature: Double? = null,
    val humidity: Int? = null,
    val isRefreshing: Boolean = false
)

data class WeatherDebugUiState(
    val isEnabled: Boolean,
    val minTemperatureInput: String,
    val maxTemperatureInput: String,
    val humidityInput: String,
    val isOverrideActive: Boolean,
    val lastAppliedAt: Date?,
    val errorMessage: String?
)

data class ClockDebugUiState(
    val isEnabled: Boolean,
    val isNextDayEnabled: Boolean,
    val manualOverrideInput: String,
    val isManualOverrideActive: Boolean,
    val manualOverrideLabel: String?,
    val lastAppliedAt: Date?,
    val errorMessage: String?
)

data class WearFeedbackDebugUiState(
    val isEnabled: Boolean,
    val lastUpdatedAt: Date?,
    val messages: List<UiMessage>
)

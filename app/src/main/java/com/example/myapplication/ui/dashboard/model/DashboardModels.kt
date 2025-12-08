package com.example.myapplication.ui.dashboard.model

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.ui.common.UiMessage
import java.time.Instant

enum class AlertSeverity {
    NONE,
    WARNING,
    ERROR
}

data class InventoryAlert(
    val severity: AlertSeverity,
    val message: UiMessage
)

data class OutfitSuggestion(
    val top: ClothingItem,
    val bottom: ClothingItem,
    val outer: ClothingItem? = null,
    val totalScore: Int
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val mode: TpoMode = TpoMode.CASUAL,
    val environment: EnvironmentMode = EnvironmentMode.OUTDOOR,
    val suggestions: List<OutfitSuggestion> = emptyList(),
    val totalSuggestionCount: Int = 0,
    val selectionInsights: List<UiMessage> = emptyList(),
    val inventoryReviewMessages: List<UiMessage> = emptyList(),
    val isInventoryReviewVisible: Boolean = false,
    val alert: InventoryAlert? = null,
    val weather: WeatherSnapshot? = null,
    val selectedSuggestion: OutfitSuggestion? = null,
    val isRefreshingWeather: Boolean = false,
    val lastWeatherUpdatedAt: Instant? = null,
    val weatherErrorMessage: UiMessage? = null,
    val purchaseRecommendations: List<UiMessage> = emptyList(),
    val weatherDebug: WeatherDebugUiState? = null,
    val clockDebug: ClockDebugUiState? = null,
    val wearFeedbackDebug: WearFeedbackDebugUiState? = null
)

data class WeatherDebugUiState(
    val minTemperatureInput: String = "",
    val maxTemperatureInput: String = "",
    val humidityInput: String = "",
    val isOverrideActive: Boolean = false,
    val errorMessage: String? = null,
    val lastAppliedAt: Instant? = null
)

data class ClockDebugUiState(
    val isNextDayEnabled: Boolean = false,
    val manualOverrideInput: String = "",
    val manualOverrideLabel: String? = null,
    val isManualOverrideActive: Boolean = false,
    val lastAppliedAt: Instant? = null,
    val errorMessage: String? = null
)

data class WearFeedbackDebugUiState(
    val messages: List<UiMessage> = emptyList(),
    val lastUpdatedAt: Instant? = null
)

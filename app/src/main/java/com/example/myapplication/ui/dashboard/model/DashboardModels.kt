package com.example.myapplication.ui.dashboard.model

import com.example.myapplication.domain.model.CasualForecastDay
import com.example.myapplication.domain.model.CasualForecastSegment
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
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
    val indoorTemperatureCelsius: Double? = null,
    val suggestions: List<OutfitSuggestion> = emptyList(),
    val totalSuggestionCount: Int = 0,
    val selectionInsights: List<UiMessage> = emptyList(),
    val inventoryReviewMessages: List<UiMessage> = emptyList(),
    val isInventoryReviewVisible: Boolean = false,
    val alert: InventoryAlert? = null,
    val weather: WeatherSnapshot? = null,
    val weatherLocation: WeatherLocationUiState = WeatherLocationUiState(),
    val locationSearch: LocationSearchUiState = LocationSearchUiState(),
    val mapPicker: MapPickerUiState = MapPickerUiState(),
    val selectedSuggestion: OutfitSuggestion? = null,
    val isRefreshingWeather: Boolean = false,
    val lastWeatherUpdatedAt: Instant? = null,
    val weatherErrorMessage: UiMessage? = null,
    val purchaseRecommendations: List<UiMessage> = emptyList(),
    val weatherDebug: WeatherDebugUiState? = null,
    val clockDebug: ClockDebugUiState? = null,
    val wearFeedbackDebug: WearFeedbackDebugUiState? = null,
    val casualForecast: CasualForecastUiState? = null,
    val comebackDialogMessage: UiMessage? = null,
    val colorWish: ColorWishUiState = ColorWishUiState()
)

data class ColorWishUiState(
    val isFeatureAvailable: Boolean = false,
    val activePreference: ColorWishPreferenceUi? = null,
    val isDialogVisible: Boolean = false,
    val typeOptions: List<ColorWishTypeOption> = emptyList(),
    val selectedType: ClothingType? = null,
    val colorOptions: List<ColorWishColorOption> = emptyList(),
    val selectedColorHex: String? = null,
    val isConfirmEnabled: Boolean = false,
    val emptyStateMessage: String? = null
)

data class ColorWishPreferenceUi(
    val type: ClothingType,
    val colorHex: String,
    val typeLabel: String,
    val colorLabel: String
)

data class ColorWishTypeOption(
    val type: ClothingType,
    val label: String
)

data class ColorWishColorOption(
    val colorHex: String,
    val label: String
)

data class WeatherLocationUiState(
    val displayLabel: String = "",
    val description: String? = null,
    val isOverrideActive: Boolean = false,
    val isDialogVisible: Boolean = false,
    val labelInput: String = "",
    val latitudeInput: String = "",
    val longitudeInput: String = "",
    val errorMessage: String? = null
)

data class LocationSearchUiState(
    val isVisible: Boolean = false,
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<LocationSearchResultUiState> = emptyList(),
    val errorMessage: String? = null
)

data class LocationSearchResultUiState(
    val id: String,
    val title: String,
    val subtitle: String?
)

data class MapPickerUiState(
    val isVisible: Boolean = false,
    val labelInput: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val hasLocationSelection: Boolean = false,
    val errorMessage: String? = null,
    val isConfirmEnabled: Boolean = false,
    val zoom: Float = 0f
)

data class CasualForecastUiState(
    val dayOptions: List<CasualForecastDay>,
    val segmentOptions: List<CasualForecastSegmentOption>,
    val selectedDay: CasualForecastDay,
    val selectedSegment: CasualForecastSegment,
    val summary: CasualForecastSummary
)

data class CasualForecastSegmentOption(
    val segment: CasualForecastSegment,
    val isEnabled: Boolean
)

data class CasualForecastSummary(
    val minTemperatureCelsius: Double,
    val maxTemperatureCelsius: Double,
    val averageApparentTemperatureCelsius: Double
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

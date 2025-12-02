package com.example.myapplication.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.example.myapplication.data.weather.WeatherRepository
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.domain.model.formatClothingDisplayLabel
import com.example.myapplication.domain.usecase.ApplyWearUseCase
import com.example.myapplication.domain.usecase.ApplyWearUseCase.WearReason
import com.example.myapplication.domain.usecase.FormalScoreCalculator
import com.example.myapplication.domain.usecase.WeatherSuitabilityEvaluator
import java.time.Instant
import com.example.myapplication.ui.dashboard.model.AlertSeverity
import com.example.myapplication.ui.dashboard.model.DashboardUiState
import com.example.myapplication.ui.dashboard.model.InventoryAlert
import com.example.myapplication.ui.dashboard.model.OutfitSuggestion
import com.example.myapplication.ui.common.UiMessage
import com.example.myapplication.ui.common.UiMessageArg
import com.example.myapplication.ui.common.labelResId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val closetRepository: ClosetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val weatherRepository: WeatherRepository,
    private val formalScoreCalculator: FormalScoreCalculator,
    private val weatherSuitabilityEvaluator: WeatherSuitabilityEvaluator,
    private val applyWearUseCase: ApplyWearUseCase,
    private val stringResolver: (Int) -> String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val selectionState = MutableStateFlow<Pair<String, String>?>(null)
    private val weatherStatus = MutableStateFlow(WeatherRefreshStatus())
    private var suggestionCache: List<OutfitSuggestion> = emptyList()

    private val _toastEvents = MutableSharedFlow<List<UiMessage>>(extraBufferCapacity = 1)
    val toastEvents = _toastEvents.asSharedFlow()

    init {
        observeData()
        refreshWeather()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.observe(),
                closetRepository.observeAll(),
                weatherRepository.observeCurrentWeather(),
                weatherStatus
            ) { preferences, items, weather, weatherStatus ->
                val mode = preferences.lastSelectedMode.takeUnless { it == TpoMode.UNKNOWN } ?: TpoMode.CASUAL
                val environment = preferences.lastSelectedEnvironment.takeUnless { it == EnvironmentMode.UNKNOWN }
                    ?: EnvironmentMode.OUTDOOR
                val closetItems = items.filter { it.status == LaundryStatus.CLOSET }
                val (targetMinTemp, targetMaxTemp) = resolveComfortRange(environment, weather)
                val suggestionResult = buildSuggestions(
                    mode = mode,
                    environment = environment,
                    items = closetItems,
                    disallowVividPair = preferences.colorRules.disallowVividPair,
                    allowBlackNavy = preferences.colorRules.allowBlackNavy,
                    temperatureMin = targetMinTemp,
                    temperatureMax = targetMaxTemp
                )
                val suggestions = suggestionResult.suggestions
                suggestionCache = suggestions
                val totalSuggestionCount = suggestions.size
                val alert = buildInventoryAlert(mode, totalSuggestionCount)
                val selectedPair = selectionState.value
                val resolvedSelectionPair = when {
                    selectedPair != null && suggestions.any { it.pairIds == selectedPair } -> selectedPair
                    suggestions.isNotEmpty() -> suggestions.first().pairIds
                    else -> null
                }
                if (resolvedSelectionPair != selectedPair) {
                    selectionState.value = resolvedSelectionPair
                }

                val selectedSuggestion = suggestions.firstOrNull { it.pairIds == resolvedSelectionPair }
                    ?: suggestions.firstOrNull()
                val visibleSuggestions = selectedSuggestion?.let { listOf(it) } ?: emptyList()

                DashboardUiState(
                    isLoading = false,
                    mode = mode,
                    environment = environment,
                    suggestions = visibleSuggestions,
                    alert = alert,
                    weather = weather,
                    selectedSuggestion = selectedSuggestion,
                    isRefreshingWeather = weatherStatus.isRefreshing,
                    lastWeatherUpdatedAt = weather.updatedAt ?: weatherStatus.lastUpdated,
                    weatherErrorMessage = weatherStatus.errorMessage,
                    purchaseRecommendations = suggestionResult.recommendations,
                    totalSuggestionCount = totalSuggestionCount
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refreshWeather() {
        if (weatherStatus.value.isRefreshing) return
        viewModelScope.launch {
            weatherStatus.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                weatherRepository.refresh()
                weatherStatus.value = WeatherRefreshStatus(
                    isRefreshing = false,
                    lastUpdated = Instant.now(),
                    errorMessage = null
                )
            } catch (t: Throwable) {
                weatherStatus.update { current ->
                    current.copy(
                        isRefreshing = false,
                        errorMessage = UiMessage(R.string.dashboard_weather_error)
                    )
                }
            }
        }
    }

    private fun buildSuggestions(
        mode: TpoMode,
        environment: EnvironmentMode,
        items: List<ClothingItem>,
        disallowVividPair: Boolean,
        allowBlackNavy: Boolean,
        temperatureMin: Double,
        temperatureMax: Double
    ): SuggestionResult {
        val recommendations = mutableListOf<UiMessage>()

        if (items.isEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_missing_top_inventory, mode, ClothingType.TOP)
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_missing_bottom_inventory, mode, ClothingType.BOTTOM)
            return SuggestionResult(emptyList(), recommendations.distinct())
        }

        val topsInCloset = items.filter { it.type == ClothingType.TOP }
        val bottomsInCloset = items.filter { it.type == ClothingType.BOTTOM }

        if (topsInCloset.isEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_missing_top_inventory, mode, ClothingType.TOP)
        }
        if (bottomsInCloset.isEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_missing_bottom_inventory, mode, ClothingType.BOTTOM)
        }

        val tops = topsInCloset.filter { item ->
            weatherSuitabilityEvaluator.isSuitable(
                item = item,
                minTemperature = temperatureMin,
                maxTemperature = temperatureMax
            )
        }
        if (tops.isEmpty() && topsInCloset.isNotEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_weather_top, mode, ClothingType.TOP)
        }

        val bottoms = bottomsInCloset.filter { item ->
            weatherSuitabilityEvaluator.isSuitable(
                item = item,
                minTemperature = temperatureMin,
                maxTemperature = temperatureMax
            )
        }
        if (bottoms.isEmpty() && bottomsInCloset.isNotEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_weather_bottom, mode, ClothingType.BOTTOM)
        }

        val candidateTops = if (tops.isNotEmpty()) tops else topsInCloset
        val candidateBottoms = if (bottoms.isNotEmpty()) bottoms else bottomsInCloset

        if (candidateTops.isEmpty() || candidateBottoms.isEmpty()) {
            if (recommendations.isEmpty()) {
                val fallbackType = if (candidateTops.isEmpty()) ClothingType.TOP else ClothingType.BOTTOM
                recommendations += buildGenericRecommendation(mode, fallbackType)
            }
            return SuggestionResult(emptyList(), recommendations.distinct())
        }

        val scoreRange = when (mode) {
            TpoMode.CASUAL -> 0..6
            TpoMode.OFFICE -> 7..14
            else -> 0..10
        }

        val suggestions = mutableListOf<OutfitSuggestion>()
        var filteredByColor = false
        var filteredByScore = false
        for (top in candidateTops) {
            val topScore = formalScoreCalculator.calculate(top)
            for (bottom in candidateBottoms) {
                val bottomScore = formalScoreCalculator.calculate(bottom)
                val totalScore = topScore + bottomScore
                if (totalScore !in scoreRange) {
                    filteredByScore = true
                    continue
                }
                if (!isColorCompatible(top, bottom, disallowVividPair, allowBlackNavy)) {
                    filteredByColor = true
                    continue
                }
                suggestions += OutfitSuggestion(
                    top = top,
                    bottom = bottom,
                    totalScore = totalScore
                )
            }
        }

        val sortedSuggestions = suggestions
            .sortedWith(
                compareByDescending<OutfitSuggestion> { it.totalScore }
                    .thenBy { it.top.name }
                    .thenBy { it.bottom.name }
            )
            .take(20)

        if (sortedSuggestions.isNotEmpty()) {
            return SuggestionResult(sortedSuggestions, recommendations.distinct())
        }

        val relaxedSuggestions = mutableListOf<OutfitSuggestion>()
        for (top in candidateTops) {
            val topScore = formalScoreCalculator.calculate(top)
            for (bottom in candidateBottoms) {
                val bottomScore = formalScoreCalculator.calculate(bottom)
                if (!isColorCompatible(top, bottom, disallowVividPair, allowBlackNavy)) continue
                relaxedSuggestions += OutfitSuggestion(
                    top = top,
                    bottom = bottom,
                    totalScore = topScore + bottomScore
                )
            }
        }

        if (relaxedSuggestions.isNotEmpty()) {
            if (filteredByScore) {
                recommendations += buildGenericRecommendation(mode, if (candidateTops.size <= candidateBottoms.size) ClothingType.TOP else ClothingType.BOTTOM)
            }
            return SuggestionResult(
                suggestions = relaxedSuggestions
                    .sortedWith(
                        compareByDescending<OutfitSuggestion> { it.totalScore }
                            .thenBy { it.top.name }
                            .thenBy { it.bottom.name }
                    )
                    .take(20),
                recommendations = recommendations.distinct()
            )
        }

        val fallbackType = if (candidateTops.size <= candidateBottoms.size) ClothingType.TOP else ClothingType.BOTTOM
        if (filteredByColor) {
            recommendations += buildGenericRecommendation(mode, fallbackType)
        }
        if (recommendations.isEmpty()) {
            recommendations += buildGenericRecommendation(mode, ClothingType.TOP)
            recommendations += buildGenericRecommendation(mode, ClothingType.BOTTOM)
        }

        return SuggestionResult(emptyList(), recommendations.distinct())
    }

    private fun resolveComfortRange(
        environment: EnvironmentMode,
        weather: WeatherSnapshot
    ): Pair<Double, Double> {
        val minTemperature = minOf(weather.minTemperatureCelsius, weather.maxTemperatureCelsius)
        val maxTemperature = maxOf(weather.minTemperatureCelsius, weather.maxTemperatureCelsius)

        if (environment != EnvironmentMode.INDOOR) {
            return minTemperature to maxTemperature
        }

        // Indoor days stay mostly in climate-controlled spaces, so narrow the envelope while
        // still respecting the actual outdoor extremes for short trips.
        val averageTemperature = (minTemperature + maxTemperature) / 2.0
        val candidateMin = (averageTemperature - INDOOR_HALF_RANGE).coerceAtLeast(minTemperature)
        val candidateMax = (averageTemperature + INDOOR_HALF_RANGE).coerceAtMost(maxTemperature)

        return if (candidateMin <= candidateMax) {
            candidateMin to candidateMax
        } else {
            minTemperature to maxTemperature
        }
    }

    private fun isColorCompatible(
        top: ClothingItem,
        bottom: ClothingItem,
        disallowVividPair: Boolean,
        allowBlackNavy: Boolean
    ): Boolean {
        if (disallowVividPair && top.colorGroup == ColorGroup.VIVID && bottom.colorGroup == ColorGroup.VIVID) {
            return false
        }
        if (!allowBlackNavy) {
            val navyWithMonotone = (top.colorGroup == ColorGroup.NAVY_BLUE && bottom.colorGroup == ColorGroup.MONOTONE) ||
                (top.colorGroup == ColorGroup.MONOTONE && bottom.colorGroup == ColorGroup.NAVY_BLUE)
            if (navyWithMonotone) return false
        }
        return true
    }

    private fun buildInventoryAlert(mode: TpoMode, suggestionCount: Int): InventoryAlert? {
        if (suggestionCount == 0) {
            return InventoryAlert(
                severity = AlertSeverity.ERROR,
                message = UiMessage(R.string.dashboard_alert_no_outfit)
            )
        }
        if (suggestionCount <= 2) {
            return InventoryAlert(
                severity = AlertSeverity.WARNING,
                message = UiMessage(
                    resId = R.string.dashboard_alert_low_inventory,
                    args = listOf(UiMessageArg.Resource(mode.toLabelResId()))
                )
            )
        }
        return null
    }

    private fun buildRecommendationMessage(
        @androidx.annotation.StringRes resId: Int,
        mode: TpoMode,
        type: ClothingType
    ): UiMessage {
        val categories = recommendedCategoriesFor(mode, type)
        val label = formatCategoryList(categories)
        return UiMessage(
            resId = resId,
            args = listOf(UiMessageArg.Raw(label))
        )
    }

    private fun buildGenericRecommendation(mode: TpoMode, type: ClothingType): UiMessage {
        val resId = if (type == ClothingType.BOTTOM) {
            R.string.dashboard_recommendation_generic_bottom
        } else {
            R.string.dashboard_recommendation_generic_top
        }
        return buildRecommendationMessage(resId, mode, type)
    }

    private fun recommendedCategoriesFor(mode: TpoMode, type: ClothingType): List<ClothingCategory> = when (type) {
        ClothingType.TOP -> when (mode) {
            TpoMode.OFFICE -> listOf(ClothingCategory.DRESS_SHIRT, ClothingCategory.KNIT)
            TpoMode.CASUAL -> listOf(ClothingCategory.T_SHIRT, ClothingCategory.POLO, ClothingCategory.KNIT)
            else -> listOf(ClothingCategory.T_SHIRT, ClothingCategory.KNIT)
        }
        ClothingType.BOTTOM -> when (mode) {
            TpoMode.OFFICE -> listOf(ClothingCategory.SLACKS, ClothingCategory.CHINO)
            TpoMode.CASUAL -> listOf(ClothingCategory.CHINO, ClothingCategory.DENIM)
            else -> listOf(ClothingCategory.CHINO)
        }
        else -> listOf(ClothingCategory.OUTER_LIGHT)
    }

    private fun formatCategoryList(categories: List<ClothingCategory>): String {
        val distinct = categories.distinct()
        if (distinct.isEmpty()) {
            return stringResolver(ClothingCategory.T_SHIRT.labelResId())
        }
        return distinct.joinToString(separator = " / ") { category ->
            stringResolver(category.labelResId())
        }
    }

    fun onModeSelected(mode: TpoMode) {
        if (_uiState.value.mode == mode) return
        viewModelScope.launch {
            userPreferencesRepository.updateLastSelectedMode(mode)
        }
    }

    fun onSuggestionSelected(outfit: OutfitSuggestion) {
        selectionState.value = outfit.pairIds
        _uiState.update { current ->
            current.copy(
                selectedSuggestion = outfit,
                suggestions = listOf(outfit)
            )
        }
    }

    fun rerollSuggestion() {
        val suggestions = suggestionCache
        if (suggestions.isEmpty()) return
        val currentPair = selectionState.value
        val nextSuggestion = suggestions
            .filter { it.pairIds != currentPair }
            .randomOrNull()
            ?: suggestions.first()
        selectionState.value = nextSuggestion.pairIds
        _uiState.update { current ->
            current.copy(
                selectedSuggestion = nextSuggestion,
                suggestions = listOf(nextSuggestion)
            )
        }
    }

    fun onWearSelected() {
        val suggestion = _uiState.value.selectedSuggestion ?: return
        viewModelScope.launch {
            val weather = _uiState.value.weather ?: weatherRepository.observeCurrentWeather().first()
            val uniqueItems = listOf(suggestion.top, suggestion.bottom).distinctBy { it.id }
            val outcomes = uniqueItems.map { applyWearUseCase.execute(it, weather) }
            outcomes.forEach { outcome -> closetRepository.upsert(outcome.updatedItem) }
            val messages = outcomes.map { outcome -> outcome.toUiMessage(stringResolver) }
            _toastEvents.emit(messages)
        }
    }

    private companion object {
        const val INDOOR_HALF_RANGE = 2.5
    }

    class Factory(
        private val closetRepository: ClosetRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val weatherRepository: WeatherRepository,
        private val formalScoreCalculator: FormalScoreCalculator = FormalScoreCalculator(),
        private val weatherSuitabilityEvaluator: WeatherSuitabilityEvaluator = WeatherSuitabilityEvaluator(),
        private val applyWearUseCase: ApplyWearUseCase = ApplyWearUseCase(),
        private val stringResolver: (Int) -> String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(
                    closetRepository = closetRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    weatherRepository = weatherRepository,
                    formalScoreCalculator = formalScoreCalculator,
                    weatherSuitabilityEvaluator = weatherSuitabilityEvaluator,
                    applyWearUseCase = applyWearUseCase,
                    stringResolver = stringResolver
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private data class SuggestionResult(
    val suggestions: List<OutfitSuggestion>,
    val recommendations: List<UiMessage>
)

private val OutfitSuggestion.pairIds: Pair<String, String>
    get() = top.id to bottom.id

private data class WeatherRefreshStatus(
    val isRefreshing: Boolean = false,
    val lastUpdated: Instant? = null,
    val errorMessage: UiMessage? = null
)

@androidx.annotation.StringRes
private fun TpoMode.toLabelResId(): Int = when (this) {
    TpoMode.CASUAL -> R.string.dashboard_mode_casual
    TpoMode.OFFICE -> R.string.dashboard_mode_office
    TpoMode.UNKNOWN -> R.string.dashboard_mode_unknown
}

private fun ApplyWearUseCase.WearOutcome.toUiMessage(
    stringResolver: (Int) -> String
): UiMessage {
    val categoryLabel = stringResolver(updatedItem.category.labelResId())
    val label = formatClothingDisplayLabel(
        categoryLabel = categoryLabel,
        name = updatedItem.name,
        colorHex = updatedItem.colorHex
    )
    return if (movedToDirty) {
        val args = mutableListOf<UiMessageArg>(UiMessageArg.Raw(label))
        val reasonArg = reason?.toUiMessageArg()
        val resId = if (reasonArg != null) {
            args += reasonArg
            R.string.wear_message_dirty_with_reason
        } else {
            R.string.wear_message_dirty
        }
        UiMessage(resId = resId, args = args)
    } else {
        UiMessage(
            resId = R.string.wear_message_remaining,
            args = listOf(
                UiMessageArg.Raw(label),
                UiMessageArg.Raw(remainingUses)
            )
        )
    }
}

private fun WearReason.toUiMessageArg(): UiMessageArg.Resource = when (this) {
    WearReason.FORCE_SETTING -> UiMessageArg.Resource(R.string.wear_reason_setting)
    WearReason.HEAT_SWEAT -> UiMessageArg.Resource(R.string.wear_reason_heat)
}

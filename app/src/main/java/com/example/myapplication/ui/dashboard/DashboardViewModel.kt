package com.example.myapplication.ui.dashboard

import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.example.myapplication.data.repository.WearFeedbackRepository
import com.example.myapplication.data.weather.NoOpWeatherDebugController
import com.example.myapplication.data.weather.WeatherRepository
import com.example.myapplication.data.weather.WeatherDebugController
import com.example.myapplication.data.weather.WeatherDebugOverride
import com.example.myapplication.domain.model.CasualForecastDay
import com.example.myapplication.domain.model.CasualForecastSegment
import com.example.myapplication.domain.model.CasualForecastSegmentSummary
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.domain.model.formatClothingDisplayLabel
import com.example.myapplication.domain.usecase.ApplyWearUseCase
import com.example.myapplication.domain.usecase.ApplyWearUseCase.WearReason
import com.example.myapplication.domain.usecase.FormalScoreCalculator
import com.example.myapplication.domain.usecase.WeatherSuitabilityEvaluator
import com.example.myapplication.util.time.DebugClockController
import com.example.myapplication.util.time.InstantCompat
import com.example.myapplication.util.time.NoOpDebugClockController
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import com.example.myapplication.ui.dashboard.model.AlertSeverity
import com.example.myapplication.ui.dashboard.model.ClockDebugUiState
import com.example.myapplication.ui.dashboard.model.DashboardUiState
import com.example.myapplication.ui.dashboard.model.InventoryAlert
import com.example.myapplication.ui.dashboard.model.OutfitSuggestion
import com.example.myapplication.ui.dashboard.model.CasualForecastSegmentOption
import com.example.myapplication.ui.dashboard.model.CasualForecastSummary
import com.example.myapplication.ui.dashboard.model.CasualForecastUiState
import com.example.myapplication.ui.dashboard.model.WeatherDebugUiState
import com.example.myapplication.ui.dashboard.model.WearFeedbackDebugUiState
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
import kotlin.collections.buildList

class DashboardViewModel(
    private val closetRepository: ClosetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val weatherRepository: WeatherRepository,
    private val wearFeedbackRepository: WearFeedbackRepository,
    private val weatherDebugController: WeatherDebugController,
    private val clockDebugController: DebugClockController,
    private val formalScoreCalculator: FormalScoreCalculator,
    private val weatherSuitabilityEvaluator: WeatherSuitabilityEvaluator,
    private val applyWearUseCase: ApplyWearUseCase,
    private val stringResolver: (Int) -> String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val selectionState = MutableStateFlow<SuggestionSelectionKey?>(null)
    private val weatherStatus = MutableStateFlow(WeatherRefreshStatus())
    private var suggestionCache: List<OutfitSuggestion> = emptyList()
    private val casualForecastSelection = MutableStateFlow(CasualForecastSelection())
    private val reviewDialogState = MutableStateFlow(false)
    private val weatherDebugState = MutableStateFlow(
        if (weatherDebugController.isSupported) WeatherDebugUiState() else null
    )
    private val clockDebugState = MutableStateFlow(
        if (clockDebugController.isSupported) ClockDebugUiState() else null
    )
    private val wearFeedbackDebugState = MutableStateFlow(
        if (clockDebugController.isSupported || weatherDebugController.isSupported) WearFeedbackDebugUiState() else null
    )
    private val comebackDialogState = MutableStateFlow<UiMessage?>(null)

    private val _wearNotificationEvents = MutableSharedFlow<List<UiMessage>>(extraBufferCapacity = 1)
    val wearNotificationEvents = _wearNotificationEvents.asSharedFlow()

    init {
        observeData()
        observeWeatherDebug()
        observeWeatherDebugDefaults()
        observeClockDebug()
        trackLastLogin()
        refreshWeather()
    }

    private fun observeData() {
        viewModelScope.launch {
            val baseFlow = userPreferencesRepository.observe()
                .combine(closetRepository.observeAll()) { preferences, items ->
                    BaseInputsBuilder(preferences = preferences, items = items)
                }
                .combine(weatherRepository.observeCurrentWeather()) { builder, weather ->
                    builder.copy(weather = weather)
                }
                .combine(weatherStatus) { builder, status ->
                    builder.copy(weatherStatus = status)
                }
                .combine(selectionState) { builder, selectedKey ->
                    builder.copy(selectedKey = selectedKey)
                }
                .combine(casualForecastSelection) { builder, casualSelection ->
                    val weatherSnapshot = builder.weather
                        ?: throw IllegalStateException("Weather snapshot missing")
                    CombinedInputs(
                        preferences = builder.preferences,
                        items = builder.items,
                        weather = weatherSnapshot,
                        weatherStatus = builder.weatherStatus,
                        selectedKey = builder.selectedKey,
                        casualSelection = casualSelection
                    )
                }

            val debugFlow = weatherDebugState
                .combine(clockDebugState) { weatherDebug, clockDebug ->
                    DebugInputs(weatherDebug = weatherDebug, clockDebug = clockDebug)
                }
                .combine(wearFeedbackDebugState) { debugInputs, wearFeedback ->
                    debugInputs.copy(wearFeedback = wearFeedback)
                }

            baseFlow
                .combine(debugFlow) { base, debug ->
                    CombinedInputsWithDebug(
                        base = base,
                        weatherDebug = debug.weatherDebug,
                        clockDebug = debug.clockDebug,
                        wearFeedback = debug.wearFeedback
                    )
                }
                .combine(reviewDialogState) { combined, reviewVisible ->
                    combined to reviewVisible
                }
                .combine(comebackDialogState) { (combined, reviewVisible), comebackMessage ->
                    val inputs = combined.base
                    val weatherDebug = combined.weatherDebug
                    val clockDebug = combined.clockDebug
                    val wearFeedback = combined.wearFeedback
                    val preferences = inputs.preferences
                    val items = inputs.items
                    val weather = inputs.weather
                    val weatherStatus = inputs.weatherStatus
                    val selectedKey = inputs.selectedKey

                    val mode = preferences.lastSelectedMode.takeUnless { it == TpoMode.UNKNOWN } ?: TpoMode.CASUAL
                    val environment = preferences.lastSelectedEnvironment.takeUnless { it == EnvironmentMode.UNKNOWN }
                        ?: EnvironmentMode.OUTDOOR
                    val closetItems = items.filter { it.status == LaundryStatus.CLOSET }
                    val casualComputation = if (mode == TpoMode.CASUAL) {
                        buildCasualForecastState(weather, inputs.casualSelection)
                    } else {
                        CasualForecastComputation()
                    }
                    val resolvedCasualSelection = casualComputation.resolvedSelection
                    if (resolvedCasualSelection != null && resolvedCasualSelection != inputs.casualSelection) {
                        casualForecastSelection.value = resolvedCasualSelection
                    }
                    val selectedCasualSummary = casualComputation.uiState?.summary
                    val (targetMinTemp, targetMaxTemp) = if (selectedCasualSummary != null) {
                        resolveComfortRange(
                            environment = environment,
                            weather = weather,
                            minOverride = selectedCasualSummary.minTemperatureCelsius,
                            maxOverride = selectedCasualSummary.maxTemperatureCelsius
                        )
                    } else {
                        resolveComfortRange(environment, weather)
                    }
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
                    val resolvedSelectionKey = when {
                        selectedKey != null && suggestions.any { it.selectionKey == selectedKey } -> selectedKey
                        suggestions.isNotEmpty() -> suggestions.first().selectionKey
                        else -> null
                    }
                    if (resolvedSelectionKey != selectedKey) {
                        selectionState.value = resolvedSelectionKey
                    }

                    val selectedSuggestion = suggestions.firstOrNull { it.selectionKey == resolvedSelectionKey }
                        ?: suggestions.firstOrNull()
                    val previewSuggestions = selectedSuggestion?.let { listOf(it) } ?: emptyList()
                    val insights = selectedSuggestion?.let { suggestion ->
                        buildSelectionInsights(
                            suggestion = suggestion,
                            mode = mode,
                            environment = environment,
                            comfortMin = targetMinTemp,
                            comfortMax = targetMaxTemp,
                            weather = weather,
                            disallowVividPair = preferences.colorRules.disallowVividPair,
                            allowBlackNavy = preferences.colorRules.allowBlackNavy
                        )
                    } ?: emptyList()

                    buildInventoryReviewMessages(
                        alert,
                        suggestionResult.recommendations
                    ).let { reviewMessages ->
                        DashboardUiState(
                            isLoading = false,
                            mode = mode,
                            environment = environment,
                            suggestions = previewSuggestions,
                            alert = alert,
                            weather = weather,
                            selectedSuggestion = selectedSuggestion,
                            isRefreshingWeather = weatherStatus.isRefreshing,
                            lastWeatherUpdatedAt = weather.updatedAt ?: weatherStatus.lastUpdated,
                            weatherErrorMessage = weatherStatus.errorMessage,
                            purchaseRecommendations = suggestionResult.recommendations,
                            totalSuggestionCount = totalSuggestionCount,
                            selectionInsights = insights,
                            inventoryReviewMessages = reviewMessages,
                            isInventoryReviewVisible = reviewVisible && reviewMessages.isNotEmpty(),
                            weatherDebug = weatherDebug,
                            clockDebug = clockDebug,
                            wearFeedbackDebug = wearFeedback,
                            casualForecast = casualComputation.uiState,
                            comebackDialogMessage = comebackMessage
                        )
                    }
                }
                .collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun observeWeatherDebug() {
        if (!weatherDebugController.isSupported) return
        viewModelScope.launch {
            weatherDebugController.override.collect { override ->
                weatherDebugState.update { current ->
                    current?.let { state ->
                        if (override != null) {
                            val appliedAt = state.lastAppliedAt ?: InstantCompat.nowOrNull()
                            state.copy(
                                minTemperatureInput = formatTemperature(override.minTemperatureCelsius),
                                maxTemperatureInput = formatTemperature(override.maxTemperatureCelsius),
                                humidityInput = override.humidityPercent.toString(),
                                isOverrideActive = true,
                                errorMessage = null,
                                lastAppliedAt = appliedAt
                            )
                        } else {
                            state.copy(
                                isOverrideActive = false,
                                errorMessage = null,
                                lastAppliedAt = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeWeatherDebugDefaults() {
        if (!weatherDebugController.isSupported) return
        viewModelScope.launch {
            weatherRepository.observeCurrentWeather().collect { snapshot ->
                weatherDebugState.update { current ->
                    current?.let { state ->
                        if (state.isOverrideActive) {
                            state
                        } else {
                            val minText = formatTemperature(snapshot.minTemperatureCelsius)
                            val maxText = formatTemperature(snapshot.maxTemperatureCelsius)
                            val humidityText = snapshot.humidityPercent.toString()
                            if (
                                state.minTemperatureInput == minText &&
                                state.maxTemperatureInput == maxText &&
                                state.humidityInput == humidityText
                            ) {
                                state
                            } else {
                                state.copy(
                                    minTemperatureInput = minText,
                                    maxTemperatureInput = maxText,
                                    humidityInput = humidityText
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeClockDebug() {
        if (!clockDebugController.isSupported) return
        viewModelScope.launch {
            clockDebugController.nextDayEnabled.collect { enabled ->
                clockDebugState.update { current ->
                    current?.let { state ->
                        val updatedLastApplied = if (!state.isNextDayEnabled && enabled) {
                            InstantCompat.nowOrNull()
                        } else {
                            state.lastAppliedAt
                        }
                        state.copy(
                            isNextDayEnabled = enabled,
                            lastAppliedAt = updatedLastApplied
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            clockDebugController.manualOverride.collect { override ->
                clockDebugState.update { current ->
                    current?.let { state ->
                        if (override != null) {
                            val label = formatManualOverrideLabel(override.targetEpochMillis)
                            val normalizedInput = formatManualOverrideInput(override.targetEpochMillis)
                            val lastApplied = when {
                                state.isManualOverrideActive && state.manualOverrideLabel == label -> state.lastAppliedAt
                                state.lastAppliedAt != null -> state.lastAppliedAt
                                else -> InstantCompat.nowOrNull()
                            }
                            state.copy(
                                isManualOverrideActive = true,
                                manualOverrideLabel = label,
                                manualOverrideInput = normalizedInput,
                                errorMessage = null,
                                lastAppliedAt = lastApplied
                            )
                        } else {
                            state.copy(
                                isManualOverrideActive = false,
                                manualOverrideLabel = null,
                                manualOverrideInput = "",
                                errorMessage = null,
                                lastAppliedAt = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun trackLastLogin() {
        viewModelScope.launch {
            val now = InstantCompat.nowOrNull() ?: return@launch
            val preferences = userPreferencesRepository.observe().first()
            val lastLogin = preferences.lastLogin
            if (lastLogin != null) {
                val daysSince = Duration.between(lastLogin, now).toDays()
                if (daysSince >= INACTIVITY_THRESHOLD_DAYS && comebackDialogState.value == null) {
                    val clampedDays = daysSince.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    comebackDialogState.value = UiMessage(
                        resId = R.string.dashboard_comeback_dialog_message,
                        args = listOf(UiMessageArg.Raw(clampedDays))
                    )
                }
            }
            userPreferencesRepository.update { current ->
                current.copy(lastLogin = now)
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
                    lastUpdated = InstantCompat.nowOrNull(),
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

    fun onClockDebugNextDayChanged(enabled: Boolean) {
        if (!clockDebugController.isSupported) return
        clockDebugController.setNextDayEnabled(enabled)
        clockDebugState.update { current ->
            current?.copy(errorMessage = null)
        }
    }

    fun onClockDebugManualOverrideInputChanged(value: String) {
        clockDebugState.update { current ->
            current?.copy(manualOverrideInput = value, errorMessage = null)
        }
    }

    fun applyClockDebugManualOverride() {
        if (!clockDebugController.isSupported) return
        val currentState = clockDebugState.value ?: return
        val parseResult = parseManualOverrideInput(currentState.manualOverrideInput)
        if (parseResult == null) {
            setClockDebugError(R.string.debug_clock_manual_error_format)
            return
        }
        clockDebugController.setManualOverride(parseResult.epochMillis)
        val appliedAt = InstantCompat.nowOrNull()
        val label = formatManualOverrideLabel(parseResult.epochMillis)
        clockDebugState.update { state ->
            state?.copy(
                isManualOverrideActive = true,
                manualOverrideLabel = label,
                manualOverrideInput = parseResult.normalizedInput,
                isNextDayEnabled = false,
                lastAppliedAt = appliedAt,
                errorMessage = null
            )
        }
    }

    fun clearClockDebugManualOverride() {
        if (!clockDebugController.isSupported) return
        clockDebugController.clearManualOverride()
        clockDebugState.update { state ->
            state?.copy(
                isManualOverrideActive = false,
                manualOverrideLabel = null,
                manualOverrideInput = "",
                lastAppliedAt = null,
                errorMessage = null
            )
        }
    }

    fun onDebugMinTemperatureChanged(value: String) {
        weatherDebugState.update { current ->
            current?.copy(minTemperatureInput = value, errorMessage = null)
        }
    }

    fun onDebugMaxTemperatureChanged(value: String) {
        weatherDebugState.update { current ->
            current?.copy(maxTemperatureInput = value, errorMessage = null)
        }
    }

    fun onDebugHumidityChanged(value: String) {
        weatherDebugState.update { current ->
            current?.copy(humidityInput = value, errorMessage = null)
        }
    }

    fun onCasualForecastDaySelected(day: CasualForecastDay) {
        casualForecastSelection.update { current ->
            if (current.day == day) current else current.copy(day = day)
        }
    }

    fun onCasualForecastSegmentSelected(segment: CasualForecastSegment) {
        casualForecastSelection.update { current ->
            if (current.segment == segment) current else current.copy(segment = segment)
        }
    }

    fun applyWeatherDebugOverride() {
        val current = weatherDebugState.value ?: return
        val minTemp = current.minTemperatureInput.trim().toDoubleOrNull()
        val maxTemp = current.maxTemperatureInput.trim().toDoubleOrNull()
        val humidity = current.humidityInput.trim().toIntOrNull()
        if (minTemp == null || maxTemp == null || humidity == null) {
            setWeatherDebugError(R.string.debug_weather_error_invalid_number)
            return
        }
        if (humidity !in 0..100) {
            setWeatherDebugError(R.string.debug_weather_error_humidity_range)
            return
        }
        if (minTemp > maxTemp) {
            setWeatherDebugError(R.string.debug_weather_error_min_greater_than_max)
            return
        }
        val sanitizedHumidity = humidity.coerceIn(0, 100)
        weatherDebugController.applyOverride(
            WeatherDebugOverride(
                minTemperatureCelsius = minTemp,
                maxTemperatureCelsius = maxTemp,
                humidityPercent = sanitizedHumidity
            )
        )
        val appliedAt = InstantCompat.nowOrNull()
        weatherDebugState.update { state ->
            state?.copy(
                errorMessage = null,
                isOverrideActive = true,
                lastAppliedAt = appliedAt,
                minTemperatureInput = formatTemperature(minTemp),
                maxTemperatureInput = formatTemperature(maxTemp),
                humidityInput = sanitizedHumidity.toString()
            )
        }
    }

    fun clearWeatherDebugOverride() {
        if (!weatherDebugController.isSupported) return
        weatherDebugController.clearOverride()
        weatherDebugState.update { state ->
            state?.copy(
                isOverrideActive = false,
                errorMessage = null,
                lastAppliedAt = null
            )
        }
    }

    private fun setWeatherDebugError(@StringRes resId: Int) {
        val message = stringResolver(resId)
        weatherDebugState.update { state ->
            state?.copy(errorMessage = message)
        }
    }

    private fun setClockDebugError(@StringRes resId: Int) {
        val message = stringResolver(resId)
        clockDebugState.update { state ->
            state?.copy(errorMessage = message)
        }
    }

    private fun parseManualOverrideInput(raw: String): ManualOverrideParseResult? {
        val sanitized = raw.trim().replace("T", " ")
        if (sanitized.isEmpty()) return null
        val locale = Locale.getDefault()
        val patterns = listOf(MANUAL_OVERRIDE_INPUT_PATTERN, MANUAL_OVERRIDE_DATE_ONLY_PATTERN)
        for (pattern in patterns) {
            val formatter = SimpleDateFormat(pattern, locale).apply { isLenient = false }
            val date = runCatching { formatter.parse(sanitized) }.getOrNull() ?: continue
            val epochMillis = date.time
            val normalizedInput = formatManualOverrideInput(epochMillis)
            return ManualOverrideParseResult(epochMillis = epochMillis, normalizedInput = normalizedInput)
        }
        return null
    }

    private fun formatManualOverrideInput(epochMillis: Long): String {
        val locale = Locale.getDefault()
        return SimpleDateFormat(MANUAL_OVERRIDE_INPUT_PATTERN, locale).format(Date(epochMillis))
    }

    private fun formatManualOverrideLabel(epochMillis: Long): String {
        val instant = InstantCompat.ofEpochMilliOrNull(epochMillis)
        return if (instant != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern(MANUAL_OVERRIDE_LABEL_PATTERN)
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } else {
            SimpleDateFormat(MANUAL_OVERRIDE_LABEL_PATTERN, Locale.getDefault()).format(Date(epochMillis))
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
        val outersInCloset = items.filter { it.type == ClothingType.OUTER }

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
        val outerCandidates = resolveOuterCandidates(
            outers = outersInCloset,
            temperatureMin = temperatureMin,
            temperatureMax = temperatureMax
        )

        if (candidateTops.isEmpty() || candidateBottoms.isEmpty()) {
            if (recommendations.isEmpty()) {
                val fallbackType = if (candidateTops.isEmpty()) ClothingType.TOP else ClothingType.BOTTOM
                recommendations += buildGenericRecommendation(mode, fallbackType)
            }
            return SuggestionResult(emptyList(), recommendations.distinct())
        }

        val scoreRange = resolveScoreRange(mode)

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
                val outer = pickOuterCandidate(outerCandidates, suggestions.size)
                suggestions += OutfitSuggestion(
                    top = top,
                    bottom = bottom,
                    outer = outer,
                    totalScore = totalScore
                )
            }
        }

        val sortedSuggestions = suggestions
            .sortedWith(
                compareByDescending<OutfitSuggestion> { it.totalScore }
                    .thenBy { it.top.name }
                    .thenBy { it.bottom.name }
                    .thenBy { it.outer?.name ?: "" }
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
                val outer = pickOuterCandidate(outerCandidates, relaxedSuggestions.size)
                relaxedSuggestions += OutfitSuggestion(
                    top = top,
                    bottom = bottom,
                    outer = outer,
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
                            .thenBy { it.outer?.name ?: "" }
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

    private fun buildSelectionInsights(
        suggestion: OutfitSuggestion,
        mode: TpoMode,
        environment: EnvironmentMode,
        comfortMin: Double,
        comfortMax: Double,
        weather: WeatherSnapshot,
        disallowVividPair: Boolean,
        allowBlackNavy: Boolean
    ): List<UiMessage> {
        val insights = mutableListOf<UiMessage>()
        val modeLabel = stringResolver(mode.toLabelResId())
        val scoreRangeLabel = formatScoreRange(resolveScoreRange(mode))
        insights += UiMessage(
            resId = R.string.dashboard_insight_mode,
            args = listOf(
                UiMessageArg.Raw(modeLabel),
                UiMessageArg.Raw(scoreRangeLabel),
                UiMessageArg.Raw(suggestion.totalScore)
            )
        )

        val environmentLabel = stringResolver(environment.toLabelResId())
        insights += UiMessage(
            resId = R.string.dashboard_insight_weather,
            args = listOf(
                UiMessageArg.Raw(formatTemperature(comfortMin)),
                UiMessageArg.Raw(formatTemperature(comfortMax)),
                UiMessageArg.Raw(formatTemperature(weather.minTemperatureCelsius)),
                UiMessageArg.Raw(formatTemperature(weather.maxTemperatureCelsius)),
                UiMessageArg.Raw(weather.humidityPercent),
                UiMessageArg.Raw(environmentLabel)
            )
        )

        val colorRuleLabels = mutableListOf<String>()
        if (disallowVividPair) {
            colorRuleLabels += stringResolver(R.string.dashboard_insight_color_rule_disallow_vivid)
        }
        if (allowBlackNavy) {
            colorRuleLabels += stringResolver(R.string.dashboard_insight_color_rule_allow_black_navy)
        }
        val colorRulesText = colorRuleLabels.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " / ")
            ?: stringResolver(R.string.dashboard_insight_color_rules_none)
        insights += UiMessage(
            resId = R.string.dashboard_insight_color_rules,
            args = listOf(UiMessageArg.Raw(colorRulesText))
        )

        return insights
    }

    private fun resolveOuterCandidates(
        outers: List<ClothingItem>,
        temperatureMin: Double,
        temperatureMax: Double
    ): List<ClothingItem> {
        if (outers.isEmpty()) return emptyList()
        val suitable = outers.filter { outer ->
            weatherSuitabilityEvaluator.isSuitable(
                item = outer,
                minTemperature = temperatureMin,
                maxTemperature = temperatureMax
            )
        }
        val pool = if (suitable.isNotEmpty()) suitable else outers
        return pool.sortedWith(
            compareByDescending<ClothingItem> { formalScoreCalculator.calculate(it) }
                .thenBy { it.name }
        )
    }

    private fun pickOuterCandidate(
        candidates: List<ClothingItem>,
        position: Int
    ): ClothingItem? {
        if (candidates.isEmpty()) return null
        val index = position % candidates.size
        return candidates[index]
    }

    private fun buildInventoryReviewMessages(
        alert: InventoryAlert?,
        recommendations: List<UiMessage>
    ): List<UiMessage> {
        val messages = mutableListOf<UiMessage>()
        alert?.message?.let(messages::add)
        messages += recommendations
        return messages.distinct()
    }

    private fun resolveScoreRange(mode: TpoMode): IntRange = when (mode) {
        TpoMode.CASUAL -> 5..12
        TpoMode.OFFICE -> 13..18
        else -> 5..15
    }

    private fun formatScoreRange(range: IntRange): String = "${range.first}~${range.last}"

    private fun formatTemperature(value: Double): String = String.format(Locale.JAPAN, "%.1f", value)

    private fun resolveComfortRange(
        environment: EnvironmentMode,
        weather: WeatherSnapshot,
        minOverride: Double? = null,
        maxOverride: Double? = null
    ): Pair<Double, Double> {
        val rawMin = minOf(weather.minTemperatureCelsius, weather.maxTemperatureCelsius)
        val rawMax = maxOf(weather.minTemperatureCelsius, weather.maxTemperatureCelsius)
        val minTemperature = minOverride ?: rawMin
        val maxTemperature = maxOverride ?: rawMax

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

    private fun buildCasualForecastState(
        weather: WeatherSnapshot,
        selection: CasualForecastSelection
    ): CasualForecastComputation {
        val summaries = weather.casualSegmentSummaries
        if (summaries.isEmpty()) {
            return CasualForecastComputation()
        }
        val groupedByDay = summaries.groupBy { it.day }
        val dayOptions = groupedByDay.keys.sortedBy { it.ordinal }
        if (dayOptions.isEmpty()) {
            return CasualForecastComputation()
        }
        val resolvedDay = selection.day.takeIf { dayOptions.contains(it) } ?: dayOptions.first()
        val summariesForDay = groupedByDay[resolvedDay].orEmpty()
        if (summariesForDay.isEmpty()) {
            return CasualForecastComputation()
        }
        val availableSegmentsForDay = summariesForDay.map { it.segment }.distinct().sortedBy { it.ordinal }
        val resolvedSegment = selection.segment.takeIf { availableSegmentsForDay.contains(it) }
            ?: availableSegmentsForDay.firstOrNull()
            ?: return CasualForecastComputation()
        val summaryForSelection = summariesForDay.firstOrNull { it.segment == resolvedSegment }
            ?: return CasualForecastComputation()
        val availableSegmentsOverall = CasualForecastSegment.values()
            .filter { segment -> summaries.any { it.segment == segment } }
        if (availableSegmentsOverall.isEmpty()) {
            return CasualForecastComputation()
        }
        val segmentOptions = availableSegmentsOverall.map { segment ->
            CasualForecastSegmentOption(
                segment = segment,
                isEnabled = availableSegmentsForDay.contains(segment)
            )
        }
        val summary = summaryForSelection.toUiSummary()
        val uiState = CasualForecastUiState(
            dayOptions = dayOptions,
            segmentOptions = segmentOptions,
            selectedDay = resolvedDay,
            selectedSegment = resolvedSegment,
            summary = summary
        )
        return CasualForecastComputation(
            uiState = uiState,
            resolvedSelection = CasualForecastSelection(resolvedDay, resolvedSegment)
        )
    }

    private fun CasualForecastSegmentSummary.toUiSummary(): CasualForecastSummary {
        return CasualForecastSummary(
            minTemperatureCelsius = minTemperatureCelsius,
            maxTemperatureCelsius = maxTemperatureCelsius,
            averageApparentTemperatureCelsius = averageApparentTemperatureCelsius
        )
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

    fun onEnvironmentSelected(environment: EnvironmentMode) {
        if (_uiState.value.environment == environment) return
        viewModelScope.launch {
            userPreferencesRepository.updateLastSelectedEnvironment(environment)
        }
    }

    fun onSuggestionSelected(outfit: OutfitSuggestion) {
        selectionState.value = outfit.selectionKey
    }

    fun onReviewInventoryRequested() {
        if (_uiState.value.inventoryReviewMessages.isEmpty()) return
        reviewDialogState.value = true
    }

    fun onReviewInventoryDismissed() {
        reviewDialogState.value = false
    }

    fun onComebackDialogDismissed() {
        comebackDialogState.value = null
    }

    fun rerollSuggestion() {
        val suggestions = suggestionCache
        if (suggestions.isEmpty()) return
        val currentKey = selectionState.value
        val nextSuggestion = suggestions
            .filter { it.selectionKey != currentKey }
            .randomOrNull()
            ?: suggestions.first()
        selectionState.value = nextSuggestion.selectionKey
    }

    fun onWearSelected() {
        val suggestion = _uiState.value.selectedSuggestion ?: return
        viewModelScope.launch {
            val weather = _uiState.value.weather ?: weatherRepository.observeCurrentWeather().first()
            val uniqueItems = listOfNotNull(suggestion.top, suggestion.bottom, suggestion.outer).distinctBy { it.id }
            val outcomes = uniqueItems.map { applyWearUseCase.execute(it, weather) }
            outcomes.forEach { outcome -> closetRepository.upsert(outcome.updatedItem) }
            val messages = outcomes.map { outcome -> outcome.toUiMessage(stringResolver) }
            wearFeedbackDebugState.update { current ->
                current?.copy(
                    messages = messages,
                    lastUpdatedAt = InstantCompat.nowOrNull()
                )
            }
            _wearNotificationEvents.emit(messages)
        }
    }

    private companion object {
        const val INDOOR_HALF_RANGE = 2.5
        private const val INACTIVITY_THRESHOLD_DAYS = 7L
        private const val MANUAL_OVERRIDE_INPUT_PATTERN = "yyyy-MM-dd HH:mm"
        private const val MANUAL_OVERRIDE_DATE_ONLY_PATTERN = "yyyy-MM-dd"
        private const val MANUAL_OVERRIDE_LABEL_PATTERN = "M/d HH:mm"
    }

    class Factory(
        private val closetRepository: ClosetRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val weatherRepository: WeatherRepository,
        private val wearFeedbackRepository: WearFeedbackRepository,
        private val weatherDebugController: WeatherDebugController = NoOpWeatherDebugController,
        private val clockDebugController: DebugClockController = NoOpDebugClockController,
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
                    wearFeedbackRepository = wearFeedbackRepository,
                    weatherDebugController = weatherDebugController,
                    clockDebugController = clockDebugController,
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

private data class SuggestionSelectionKey(
    val topId: String,
    val bottomId: String,
    val outerId: String?
)

private data class CasualForecastSelection(
    val day: CasualForecastDay = CasualForecastDay.TODAY,
    val segment: CasualForecastSegment = CasualForecastSegment.MORNING
)

private data class CombinedInputs(
    val preferences: UserPreferences,
    val items: List<ClothingItem>,
    val weather: WeatherSnapshot,
    val weatherStatus: WeatherRefreshStatus,
    val selectedKey: SuggestionSelectionKey?,
    val casualSelection: CasualForecastSelection
)

private data class CasualForecastComputation(
    val uiState: CasualForecastUiState? = null,
    val resolvedSelection: CasualForecastSelection? = null
)

private data class BaseInputsBuilder(
    val preferences: UserPreferences,
    val items: List<ClothingItem>,
    val weather: WeatherSnapshot? = null,
    val weatherStatus: WeatherRefreshStatus = WeatherRefreshStatus(),
    val selectedKey: SuggestionSelectionKey? = null
)

private data class CombinedInputsWithDebug(
    val base: CombinedInputs,
    val weatherDebug: WeatherDebugUiState?,
    val clockDebug: ClockDebugUiState?,
    val wearFeedback: WearFeedbackDebugUiState?
)

private data class DebugInputs(
    val weatherDebug: WeatherDebugUiState?,
    val clockDebug: ClockDebugUiState?,
    val wearFeedback: WearFeedbackDebugUiState? = null
)

private data class ManualOverrideParseResult(
    val epochMillis: Long,
    val normalizedInput: String
)

private val OutfitSuggestion.selectionKey: SuggestionSelectionKey
    get() = SuggestionSelectionKey(
        topId = top.id,
        bottomId = bottom.id,
        outerId = outer?.id
    )

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

private fun EnvironmentMode.toLabelResId(): Int = when (this) {
    EnvironmentMode.OUTDOOR -> R.string.dashboard_environment_outdoor
    EnvironmentMode.INDOOR -> R.string.dashboard_environment_indoor
    EnvironmentMode.UNKNOWN -> R.string.dashboard_environment_outdoor
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

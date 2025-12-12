package com.example.myapplication.ui.dashboard

import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.LocationSearchRepository
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
import com.example.myapplication.domain.model.WeatherLocationOverride
import com.example.myapplication.domain.model.LocationSearchResult
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
import com.example.myapplication.ui.dashboard.model.ColorWishColorOption
import com.example.myapplication.ui.dashboard.model.ColorWishPreferenceUi
import com.example.myapplication.ui.dashboard.model.ColorWishTypeOption
import com.example.myapplication.ui.dashboard.model.ColorWishUiState
import com.example.myapplication.ui.dashboard.model.WeatherLocationUiState
import com.example.myapplication.ui.dashboard.model.LocationSearchUiState
import com.example.myapplication.ui.dashboard.model.LocationSearchResultUiState
import com.example.myapplication.ui.dashboard.model.MapPickerUiState
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlin.collections.buildList
import kotlin.text.RegexOption

private const val LOCATION_SEARCH_MIN_QUERY = 2
private const val LOCATION_SEARCH_DEBOUNCE_MILLIS = 400L
private const val DEFAULT_MAP_LATITUDE = 35.681236
private const val DEFAULT_MAP_LONGITUDE = 139.767125
private const val DEFAULT_MAP_ZOOM = 9.5f
private val COLOR_WISH_SUPPORTED_TYPES = listOf(ClothingType.OUTER, ClothingType.TOP, ClothingType.BOTTOM)

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
    private val locationSearchRepository: LocationSearchRepository,
    private val stringResolver: (Int) -> String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val selectionState = MutableStateFlow<SuggestionSelectionKey?>(null)
    private val weatherStatus = MutableStateFlow(WeatherRefreshStatus())
    private var suggestionCache: List<OutfitSuggestion> = emptyList()
    private val casualForecastSelection = MutableStateFlow(CasualForecastSelection())
    private val locationEditorState = MutableStateFlow(LocationEditorState())
    private val locationSearchState = MutableStateFlow(LocationSearchState())
    private val mapPickerState = MutableStateFlow(MapPickerState())
    private val reviewDialogState = MutableStateFlow(false)
    private val colorWishPreference = MutableStateFlow<ColorWishPreference?>(null)
    private val colorWishDialogState = MutableStateFlow(ColorWishDialogState())
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
    private var latestPreferences: UserPreferences = UserPreferences()
    private var latestClosetItems: List<ClothingItem> = emptyList()
    private var locationSearchJob: Job? = null

    private val _wearNotificationEvents = MutableSharedFlow<List<UiMessage>>(extraBufferCapacity = 1)
    val wearNotificationEvents = _wearNotificationEvents.asSharedFlow()
    private val _wearUndoEvents = MutableSharedFlow<WearUndoEvent>(extraBufferCapacity = 1)
    val wearUndoEvents = _wearUndoEvents.asSharedFlow()

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
                    Triple(combined, reviewVisible, comebackMessage)
                }
                .combine(locationEditorState) { (combined, reviewVisible, comebackMessage), locationEditor ->
                    Triple(combined, reviewVisible, comebackMessage) to locationEditor
                }
                .combine(locationSearchState) { (combined, locationEditor), locationSearch ->
                    Triple(combined.first, combined.second, combined.third) to Pair(locationEditor, locationSearch)
                }
                .combine(colorWishPreference) { (combined, editorAndSearch), colorWishPref ->
                    Triple(combined, editorAndSearch, colorWishPref)
                }
                .combine(colorWishDialogState) { (combined, editorAndSearch, colorWishPref), dialogState ->
                    Pair(Triple(combined, editorAndSearch, colorWishPref), dialogState)
                }
                .combine(mapPickerState) { (combinedBundle, dialogState), mapPicker ->
                    val (combined, editorAndSearch, colorWishPref) = combinedBundle
                    val (inputsWithDebug, reviewVisible, comebackMessage) = combined
                    val (locationEditor, locationSearch) = editorAndSearch
                    val inputs = inputsWithDebug.base
                    val weatherDebug = inputsWithDebug.weatherDebug
                    val clockDebug = inputsWithDebug.clockDebug
                    val wearFeedback = inputsWithDebug.wearFeedback
                    val preferences = inputs.preferences
                    latestPreferences = preferences
                    val items = inputs.items
                    val weather = inputs.weather
                    val weatherStatus = inputs.weatherStatus
                    val selectedKey = inputs.selectedKey

                    val mode = preferences.lastSelectedMode.takeUnless { it == TpoMode.UNKNOWN } ?: TpoMode.CASUAL
                    val environment = preferences.lastSelectedEnvironment.takeUnless { it == EnvironmentMode.UNKNOWN }
                        ?: EnvironmentMode.OUTDOOR
                    val closetItems = items.filter { it.status == LaundryStatus.CLOSET }
                    latestClosetItems = closetItems
                    val colorWishUiState = buildColorWishUiState(
                        closetItems = closetItems,
                        currentPreference = colorWishPref,
                        dialogState = dialogState
                    )
                    val activeColorWish = colorWishPreference.value
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
                        temperatureMax = targetMaxTemp,
                        colorWish = activeColorWish
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

                    val locationOverride = preferences.weatherLocationOverride
                    val locationDisplayLabel = locationOverride?.let {
                        val template = stringResolver(R.string.dashboard_weather_location_override_label)
                        String.format(Locale.getDefault(), template, it.label)
                    } ?: stringResolver(R.string.dashboard_weather_location_default_label)
                    val locationDescription = locationOverride?.let {
                        val template = stringResolver(R.string.dashboard_weather_location_coordinates)
                        String.format(Locale.getDefault(), template, it.latitude, it.longitude)
                    }
                    val weatherLocationUiState = WeatherLocationUiState(
                        displayLabel = locationDisplayLabel,
                        description = locationDescription,
                        isOverrideActive = locationOverride != null,
                        isDialogVisible = locationEditor.isVisible,
                        labelInput = locationEditor.labelInput,
                        latitudeInput = locationEditor.latitudeInput,
                        longitudeInput = locationEditor.longitudeInput,
                        errorMessage = locationEditor.errorMessage
                    )

                    val locationSearchUiState = locationSearch.toUiState()
                    val mapPickerUiState = mapPicker.toUiState()

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
                            weatherLocation = weatherLocationUiState,
                            locationSearch = locationSearchUiState,
                            mapPicker = mapPickerUiState,
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
                            comebackDialogMessage = comebackMessage,
                            colorWish = colorWishUiState
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
        temperatureMax: Double,
        colorWish: ColorWishPreference?
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

        var candidateTops = if (tops.isNotEmpty()) tops else topsInCloset
        var candidateBottoms = if (bottoms.isNotEmpty()) bottoms else bottomsInCloset
        var outerCandidates = resolveOuterCandidates(
            outers = outersInCloset,
            temperatureMin = temperatureMin,
            temperatureMax = temperatureMax
        )

        var colorWishApplied = false
        if (colorWish != null && colorWish.type in COLOR_WISH_SUPPORTED_TYPES) {
            colorWishApplied = true
            when (colorWish.type) {
                ClothingType.TOP -> {
                    candidateTops = candidateTops.filter { matchesColor(it, colorWish.colorHex) }
                    if (candidateTops.isEmpty()) {
                        recommendations += buildColorWishMissingMessage(colorWish)
                        return SuggestionResult(emptyList(), recommendations.distinct())
                    }
                }

                ClothingType.BOTTOM -> {
                    candidateBottoms = candidateBottoms.filter { matchesColor(it, colorWish.colorHex) }
                    if (candidateBottoms.isEmpty()) {
                        recommendations += buildColorWishMissingMessage(colorWish)
                        return SuggestionResult(emptyList(), recommendations.distinct())
                    }
                }

                ClothingType.OUTER -> {
                    outerCandidates = outerCandidates.filter { matchesColor(it, colorWish.colorHex) }
                    if (outerCandidates.isEmpty()) {
                        recommendations += buildColorWishMissingMessage(colorWish)
                        return SuggestionResult(emptyList(), recommendations.distinct())
                    }
                }

                else -> Unit
            }
        }

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
        if (colorWishApplied && colorWish != null) {
            recommendations += UiMessage(
                resId = R.string.dashboard_color_wish_no_match,
                args = listOf(
                    UiMessageArg.Raw(stringResolver(colorWish.type.labelResId())),
                    UiMessageArg.Raw(colorWish.colorLabel)
                )
            )
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

    private fun buildColorWishUiState(
        closetItems: List<ClothingItem>,
        currentPreference: ColorWishPreference?,
        dialogState: ColorWishDialogState
    ): ColorWishUiState {
        val groupedByType = closetItems
            .filter { it.type in COLOR_WISH_SUPPORTED_TYPES }
            .groupBy { it.type }

        val typeOptions = COLOR_WISH_SUPPORTED_TYPES.mapNotNull { type ->
            val items = groupedByType[type].orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                ColorWishTypeOption(type = type, label = stringResolver(type.labelResId()))
            }
        }

        var resolvedPreference = currentPreference
        val preferenceMatch = resolvedPreference?.let { pref ->
            groupedByType[pref.type]?.firstOrNull { matchesColor(it, pref.colorHex) }
        }
        if (resolvedPreference != null && preferenceMatch == null) {
            if (colorWishPreference.value != null) {
                colorWishPreference.value = null
            }
            resolvedPreference = null
        } else if (resolvedPreference != null && preferenceMatch != null) {
            val normalizedHex = normalizeColorHex(preferenceMatch.colorHex)
            val refreshedLabel = resolveColorLabel(preferenceMatch, normalizedHex)
            if (
                resolvedPreference.colorHex != normalizedHex ||
                resolvedPreference.colorLabel != refreshedLabel
            ) {
                val updated = resolvedPreference.copy(colorHex = normalizedHex, colorLabel = refreshedLabel)
                if (colorWishPreference.value != updated) {
                    colorWishPreference.value = updated
                }
                resolvedPreference = updated
            }
        }

        val resolvedType = listOfNotNull(
            dialogState.selectedType,
            resolvedPreference?.type,
            typeOptions.firstOrNull()?.type
        ).firstOrNull { type -> typeOptions.any { it.type == type } }

        val colorOptions = resolvedType?.let { type ->
            groupedByType[type]
                ?.groupBy { normalizeColorHex(it.colorHex) }
                ?.map { (hex, items) ->
                    ColorWishColorOption(colorHex = hex, label = resolveColorLabel(items.first(), hex))
                }
                ?.sortedBy { it.label }
                ?: emptyList()
        } ?: emptyList()

        val resolvedColor = listOfNotNull(
            dialogState.selectedColorHex?.let(::normalizeColorHex),
            resolvedPreference?.takeIf { it.type == resolvedType }?.colorHex,
            colorOptions.firstOrNull()?.colorHex
        ).firstOrNull { candidate -> colorOptions.any { it.colorHex == candidate } }

        if (dialogState.isVisible) {
            val normalizedDialog = dialogState.copy(
                selectedType = resolvedType,
                selectedColorHex = resolvedColor
            )
            if (normalizedDialog != dialogState) {
                colorWishDialogState.value = normalizedDialog
            }
        } else if (dialogState.selectedType != null || dialogState.selectedColorHex != null) {
            colorWishDialogState.value = ColorWishDialogState()
        }

        val activePreferenceUi = resolvedPreference?.let { pref ->
            ColorWishPreferenceUi(
                type = pref.type,
                colorHex = pref.colorHex,
                typeLabel = stringResolver(pref.type.labelResId()),
                colorLabel = pref.colorLabel
            )
        }

        val emptyMessage = when {
            typeOptions.isEmpty() -> stringResolver(R.string.dashboard_color_wish_unavailable)
            resolvedType != null && colorOptions.isEmpty() -> stringResolver(R.string.dashboard_color_wish_no_colors_for_type)
            else -> null
        }

        return ColorWishUiState(
            isFeatureAvailable = typeOptions.isNotEmpty(),
            activePreference = activePreferenceUi,
            isDialogVisible = dialogState.isVisible,
            typeOptions = typeOptions,
            selectedType = resolvedType,
            colorOptions = colorOptions,
            selectedColorHex = resolvedColor,
            isConfirmEnabled = resolvedType != null && resolvedColor != null,
            emptyStateMessage = emptyMessage
        )
    }

    private fun resolveColorLabel(item: ClothingItem, normalizedHex: String): String {
        val groupLabel = item.colorGroup.takeUnless { it == ColorGroup.UNKNOWN }?.let { group ->
            stringResolver(group.labelResId())
        }
        return when {
            groupLabel.isNullOrBlank() -> normalizedHex
            groupLabel.equals(normalizedHex, ignoreCase = true) -> normalizedHex
            else -> "$groupLabel ($normalizedHex)"
        }
    }

    private fun normalizeColorHex(raw: String): String {
        val trimmed = raw.trim()
        val withoutHash = trimmed.removePrefix("#")
        return "#" + withoutHash.uppercase(Locale.ROOT)
    }

    private fun matchesColor(item: ClothingItem, targetHex: String): Boolean {
        return normalizeColorHex(item.colorHex) == targetHex
    }

    private fun buildColorWishMissingMessage(preference: ColorWishPreference): UiMessage {
        val typeLabel = stringResolver(preference.type.labelResId())
        return UiMessage(
            resId = R.string.dashboard_color_wish_recommendation_missing,
            args = listOf(
                UiMessageArg.Raw(typeLabel),
                UiMessageArg.Raw(preference.colorLabel)
            )
        )
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

    private fun formatCoordinateInput(value: Double): String {
        return String.format(Locale.US, "%.4f", value)
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

    fun onWeatherLocationEditRequested() {
        val override = latestPreferences.weatherLocationOverride
        locationEditorState.update { current ->
            if (current.isVisible) {
                current.copy(errorMessage = null)
            } else {
                current.copy(
                    isVisible = true,
                    labelInput = override?.label.orEmpty(),
                    latitudeInput = override?.let { formatCoordinateInput(it.latitude) } ?: "",
                    longitudeInput = override?.let { formatCoordinateInput(it.longitude) } ?: "",
                    errorMessage = null
                )
            }
        }
    }

    fun onWeatherLocationDialogDismissed() {
        locationEditorState.value = LocationEditorState()
    }

    fun onWeatherLocationLabelChanged(value: String) {
        locationEditorState.update { current -> current.copy(labelInput = value, errorMessage = null) }
    }

    fun onWeatherLocationLatitudeChanged(value: String) {
        locationEditorState.update { current -> current.copy(latitudeInput = value, errorMessage = null) }
    }

    fun onWeatherLocationLongitudeChanged(value: String) {
        locationEditorState.update { current -> current.copy(longitudeInput = value, errorMessage = null) }
    }

    fun onApplyWeatherLocationOverride() {
        val editorState = locationEditorState.value
        val label = editorState.labelInput.trim()
        if (label.isEmpty()) {
            val message = stringResolver(R.string.dashboard_weather_location_error_label_blank)
            locationEditorState.update { current -> current.copy(errorMessage = message) }
            return
        }
        val latitude = editorState.latitudeInput.toDoubleOrNull()
        if (latitude == null || latitude !in -90.0..90.0) {
            val message = stringResolver(R.string.dashboard_weather_location_error_latitude)
            locationEditorState.update { current -> current.copy(errorMessage = message) }
            return
        }
        val longitude = editorState.longitudeInput.toDoubleOrNull()
        if (longitude == null || longitude !in -180.0..180.0) {
            val message = stringResolver(R.string.dashboard_weather_location_error_longitude)
            locationEditorState.update { current -> current.copy(errorMessage = message) }
            return
        }
        viewModelScope.launch {
            userPreferencesRepository.update { current ->
                current.copy(
                    weatherLocationOverride = WeatherLocationOverride(
                        label = label,
                        latitude = latitude,
                        longitude = longitude
                    )
                )
            }
            locationEditorState.value = LocationEditorState()
        }
    }

    fun onUseDeviceLocationSelected() {
        viewModelScope.launch {
            userPreferencesRepository.update { current ->
                if (current.weatherLocationOverride == null) {
                    current
                } else {
                    current.copy(weatherLocationOverride = null)
                }
            }
            locationEditorState.value = LocationEditorState()
        }
    }

    fun onLocationSearchRequested(initialQuery: String) {
        locationSearchJob?.cancel()
        locationSearchJob = null
        locationSearchState.value = LocationSearchState(
            isVisible = true,
            query = initialQuery,
            isLoading = false,
            results = emptyList(),
            errorMessage = null
        )
        scheduleLocationSearch(initialQuery, skipDebounce = true)
    }

    fun onLocationSearchDismissed() {
        locationSearchJob?.cancel()
        locationSearchJob = null
        locationSearchState.value = LocationSearchState()
    }

    fun onLocationSearchQueryChanged(query: String) {
        scheduleLocationSearch(query, skipDebounce = false)
    }

    fun onLocationSearchResultSelected(resultId: String) {
        if (!locationSearchState.value.isVisible) return
        val index = resultId.toIntOrNull() ?: return
        val result = locationSearchState.value.results.getOrNull(index) ?: return
        viewModelScope.launch {
            userPreferencesRepository.update { current ->
                current.copy(
                    weatherLocationOverride = WeatherLocationOverride(
                        label = result.title,
                        latitude = result.latitude,
                        longitude = result.longitude
                    )
                )
            }
            locationSearchState.value = LocationSearchState()
        }
    }

    private fun scheduleLocationSearch(query: String, skipDebounce: Boolean) {
        if (!locationSearchState.value.isVisible) return
        val trimmed = query.trim()
        locationSearchState.update { current ->
            if (!current.isVisible) {
                current
            } else {
                current.copy(
                    query = query,
                    errorMessage = null,
                    isLoading = trimmed.length >= LOCATION_SEARCH_MIN_QUERY,
                    results = emptyList()
                )
            }
        }
        locationSearchJob?.cancel()
        if (trimmed.length < LOCATION_SEARCH_MIN_QUERY) {
            return
        }
        locationSearchJob = viewModelScope.launch {
            if (!skipDebounce) {
                delay(LOCATION_SEARCH_DEBOUNCE_MILLIS)
            }
            try {
                val results = locationSearchRepository.searchByName(trimmed)
                val prioritized = prioritizeJapanResults(results)
                locationSearchState.update { current ->
                    if (!current.isVisible) {
                        current
                    } else {
                        current.copy(
                            isLoading = false,
                            results = prioritized,
                            errorMessage = if (prioritized.isEmpty()) {
                                stringResolver(R.string.dashboard_weather_location_search_empty)
                            } else {
                                null
                            }
                        )
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }
                locationSearchState.update { current ->
                    if (!current.isVisible) {
                        current
                    } else {
                        current.copy(
                            isLoading = false,
                            results = emptyList(),
                            errorMessage = stringResolver(R.string.dashboard_weather_location_search_error)
                        )
                    }
                }
            } finally {
                locationSearchJob = null
            }
        }
    }

    private fun prioritizeJapanResults(results: List<LocationSearchResult>): List<LocationSearchResult> {
        if (results.isEmpty()) return results
        val (japan, others) = results.partition { result ->
            isWithinJapanBounds(result.latitude, result.longitude) || subtitleIndicatesJapan(result.subtitle)
        }
        if (japan.isEmpty()) return results
        return buildList {
            addAll(japan)
            addAll(others)
        }
    }

    private fun subtitleIndicatesJapan(subtitle: String?): Boolean {
        if (subtitle.isNullOrBlank()) return false
        if (subtitle.contains("日本")) return true
        if (subtitle.contains("japan", ignoreCase = true)) return true
        return JAPAN_CODE_REGEX.containsMatchIn(subtitle)
    }

    private fun isWithinJapanBounds(latitude: Double, longitude: Double): Boolean {
        return latitude in JAPAN_LATITUDE_MIN..JAPAN_LATITUDE_MAX &&
            longitude in JAPAN_LONGITUDE_MIN..JAPAN_LONGITUDE_MAX
    }

    fun onMapPickerRequested() {
        if (mapPickerState.value.isVisible) return
        val override = latestPreferences.weatherLocationOverride
        val label = override?.label.orEmpty()
        val latitude = override?.latitude ?: DEFAULT_MAP_LATITUDE
        val longitude = override?.longitude ?: DEFAULT_MAP_LONGITUDE
        val zoom = if (override != null) 11f else DEFAULT_MAP_ZOOM
        mapPickerState.value = MapPickerState(
            isVisible = true,
            labelInput = label,
            latitude = latitude,
            longitude = longitude,
            hasLocationSelection = override != null,
            errorMessage = null,
            zoom = zoom
        )
    }

    fun onMapPickerDismissed() {
        mapPickerState.value = MapPickerState()
    }

    fun onMapPickerLabelChanged(value: String) {
        mapPickerState.update { current ->
            if (!current.isVisible) current else current.copy(labelInput = value, errorMessage = null)
        }
    }

    fun onMapPickerLocationChanged(latitude: Double, longitude: Double) {
        mapPickerState.update { current ->
            if (!current.isVisible) {
                current
            } else {
                current.copy(
                    latitude = latitude,
                    longitude = longitude,
                    hasLocationSelection = true,
                    errorMessage = null,
                    zoom = if (current.hasLocationSelection) current.zoom else 12f
                )
            }
        }
    }

    fun onMapPickerConfirmed() {
        val state = mapPickerState.value
        if (!state.isVisible) return
        val label = state.labelInput.trim()
        if (label.isEmpty()) {
            val message = stringResolver(R.string.dashboard_weather_location_map_error_label)
            mapPickerState.update { current -> current.copy(errorMessage = message) }
            return
        }
        if (!state.hasLocationSelection) {
            val message = stringResolver(R.string.dashboard_weather_location_map_error_location)
            mapPickerState.update { current -> current.copy(errorMessage = message) }
            return
        }
        val latitude = state.latitude
        if (latitude !in -90.0..90.0) {
            val message = stringResolver(R.string.dashboard_weather_location_error_latitude)
            mapPickerState.update { current -> current.copy(errorMessage = message) }
            return
        }
        val longitude = state.longitude
        if (longitude !in -180.0..180.0) {
            val message = stringResolver(R.string.dashboard_weather_location_error_longitude)
            mapPickerState.update { current -> current.copy(errorMessage = message) }
            return
        }
        viewModelScope.launch {
            userPreferencesRepository.update { current ->
                current.copy(
                    weatherLocationOverride = WeatherLocationOverride(
                        label = label,
                        latitude = latitude,
                        longitude = longitude
                    )
                )
            }
            mapPickerState.value = MapPickerState()
        }
    }

    fun onColorWishButtonClicked() {
        colorWishDialogState.update { current ->
            current.copy(
                isVisible = true,
                selectedType = current.selectedType ?: colorWishPreference.value?.type,
                selectedColorHex = current.selectedColorHex ?: colorWishPreference.value?.colorHex
            )
        }
    }

    fun onColorWishDialogDismissed() {
        colorWishDialogState.value = ColorWishDialogState()
    }

    fun onColorWishTypeSelected(type: ClothingType) {
        colorWishDialogState.update { current ->
            if (!current.isVisible) {
                current
            } else {
                current.copy(selectedType = type, selectedColorHex = null)
            }
        }
    }

    fun onColorWishColorSelected(colorHex: String) {
        val normalized = normalizeColorHex(colorHex)
        colorWishDialogState.update { current ->
            if (!current.isVisible) current else current.copy(selectedColorHex = normalized)
        }
    }

    fun onColorWishConfirm() {
        val dialog = colorWishDialogState.value
        val type = dialog.selectedType ?: return
        val selectedColor = dialog.selectedColorHex ?: return
        val normalizedColor = normalizeColorHex(selectedColor)
        val match = latestClosetItems.firstOrNull { it.type == type && matchesColor(it, normalizedColor) }
        if (match == null) {
            colorWishDialogState.value = ColorWishDialogState()
            colorWishPreference.value = null
            return
        }
        val label = resolveColorLabel(match, normalizedColor)
        colorWishPreference.value = ColorWishPreference(
            type = type,
            colorHex = normalizedColor,
            colorLabel = label
        )
        colorWishDialogState.value = ColorWishDialogState()
    }

    fun onColorWishClear() {
        colorWishPreference.value = null
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
            val originalItems = uniqueItems.map { it.copy() }
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
            _wearUndoEvents.emit(
                WearUndoEvent(
                    message = UiMessage(R.string.dashboard_wear_toast_message),
                    allowUndo = true,
                    snapshot = originalItems
                )
            )
        }
    }

    fun onWearUndo(snapshot: List<ClothingItem>) {
        viewModelScope.launch {
            if (snapshot.isNotEmpty()) {
                closetRepository.upsert(snapshot)
            }
            _wearUndoEvents.emit(
                WearUndoEvent(
                    message = UiMessage(R.string.dashboard_wear_toast_undone),
                    allowUndo = false
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationSearchJob?.cancel()
    }

    private companion object {
        const val INDOOR_HALF_RANGE = 2.5
        private const val INACTIVITY_THRESHOLD_DAYS = 7L
        private const val MANUAL_OVERRIDE_INPUT_PATTERN = "yyyy-MM-dd HH:mm"
        private const val MANUAL_OVERRIDE_DATE_ONLY_PATTERN = "yyyy-MM-dd"
        private const val MANUAL_OVERRIDE_LABEL_PATTERN = "M/d HH:mm"
        private const val JAPAN_LATITUDE_MIN = 20.0
        private const val JAPAN_LATITUDE_MAX = 46.8
        private const val JAPAN_LONGITUDE_MIN = 122.0
        private const val JAPAN_LONGITUDE_MAX = 154.0
        private val JAPAN_CODE_REGEX = Regex("\\bjp(?:n)?\\b", RegexOption.IGNORE_CASE)
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
        private val locationSearchRepository: LocationSearchRepository,
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
                    locationSearchRepository = locationSearchRepository,
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

data class WearUndoEvent(
    val message: UiMessage,
    val allowUndo: Boolean,
    val snapshot: List<ClothingItem> = emptyList()
)

private data class ColorWishPreference(
    val type: ClothingType,
    val colorHex: String,
    val colorLabel: String
)

private data class ColorWishDialogState(
    val isVisible: Boolean = false,
    val selectedType: ClothingType? = null,
    val selectedColorHex: String? = null
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

private data class LocationSearchState(
    val isVisible: Boolean = false,
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<LocationSearchResult> = emptyList(),
    val errorMessage: String? = null
)

private data class MapPickerState(
    val isVisible: Boolean = false,
    val labelInput: String = "",
    val latitude: Double = DEFAULT_MAP_LATITUDE,
    val longitude: Double = DEFAULT_MAP_LONGITUDE,
    val hasLocationSelection: Boolean = false,
    val errorMessage: String? = null,
    val zoom: Float = DEFAULT_MAP_ZOOM
)

private data class LocationEditorState(
    val isVisible: Boolean = false,
    val labelInput: String = "",
    val latitudeInput: String = "",
    val longitudeInput: String = "",
    val errorMessage: String? = null
)

private data class WeatherRefreshStatus(
    val isRefreshing: Boolean = false,
    val lastUpdated: Instant? = null,
    val errorMessage: UiMessage? = null
)

private fun LocationSearchState.toUiState(): LocationSearchUiState = LocationSearchUiState(
    isVisible = isVisible,
    query = query,
    isSearching = isLoading,
    results = results.mapIndexed { index, result ->
        LocationSearchResultUiState(
            id = index.toString(),
            title = result.title,
            subtitle = result.subtitle
        )
    },
    errorMessage = errorMessage
)

private fun MapPickerState.toUiState(): MapPickerUiState = MapPickerUiState(
    isVisible = isVisible,
    labelInput = labelInput,
    latitude = latitude,
    longitude = longitude,
    hasLocationSelection = hasLocationSelection,
    errorMessage = errorMessage,
    isConfirmEnabled = labelInput.isNotBlank() && hasLocationSelection,
    zoom = zoom
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

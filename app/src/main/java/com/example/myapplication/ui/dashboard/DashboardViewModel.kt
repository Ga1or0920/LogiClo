package com.example.myapplication.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.example.myapplication.data.repository.LocationSearchRepository
import com.example.myapplication.data.repository.WearFeedbackRepository
import com.example.myapplication.data.weather.WeatherDebugController
import com.example.myapplication.data.weather.WeatherDebugOverride
import com.example.myapplication.data.weather.WeatherRepository
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.domain.model.WearFeedback
import com.example.myapplication.domain.usecase.ApplyWearUseCase
import com.example.myapplication.domain.usecase.GetRecommendationsUseCase
import com.example.myapplication.ui.dashboard.model.DashboardState
import com.example.myapplication.ui.dashboard.model.DashboardUiState
import com.example.myapplication.ui.dashboard.model.toUi
import com.example.myapplication.util.time.DebugClockController
import com.example.myapplication.util.time.ManualTimeOverride
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val closetRepository: ClosetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val wearFeedbackRepository: WearFeedbackRepository,
    private val weatherRepository: WeatherRepository,
    private val weatherDebugController: WeatherDebugController,
    private val clockDebugController: DebugClockController,
    private val locationSearchRepository: LocationSearchRepository,
    private val getRecommendations: GetRecommendationsUseCase,
    private val applyWear: ApplyWearUseCase,
    private val stringResolver: (Int) -> String
) : ViewModel() {

    private val dashboardState: StateFlow<DashboardState> = combine(
        closetRepository.observeAll(),
        userPreferencesRepository.observe(),
        wearFeedbackRepository.observeAll(),
        weatherRepository.observeCurrentWeather(),
        weatherDebugController.override,
        clockDebugController.nextDayEnabled,
        clockDebugController.manualOverride
    ) { values ->
        val closet = values[0] as List<ClothingItem>
        val preferences = values[1] as UserPreferences
        val wearFeedback = values[2] as List<WearFeedback>
        val weather = values[3] as com.example.myapplication.domain.model.WeatherSnapshot?
        val weatherDebug = values[4] as WeatherDebugOverride?
        val nextDay = values[5] as Boolean
        val manualOverride = values[6] as ManualTimeOverride?

        val recommendations = getRecommendations(
            closet = closet,
            preferences = preferences
        )
        DashboardState(
            closet = closet,
            preferences = preferences,
            wearFeedback = wearFeedback,
            weather = weather,
            recommendations = recommendations,
            weatherDebugOverride = weatherDebug,
            clockNextDayEnabled = nextDay,
            clockManualOverride = manualOverride
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState()
    )

    val uiState: StateFlow<DashboardUiState> = combine(dashboardState) { (state) ->
        state.toUi(stringResolver)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun onWearClothingItems(items: List<ClothingItem>) {
        viewModelScope.launch {
            val weather = weatherRepository.observeCurrentWeather().first()
            val outcomes = items.map { applyWear.execute(it, weather) }
            closetRepository.upsert(outcomes.map { it.updatedItem })
            wearFeedbackRepository.recordWear(items)
        }
    }

    fun onClockDebugNextDayChanged(isNextDay: Boolean) {
        clockDebugController.setNextDayEnabled(isNextDay)
    }

    fun onClockDebugManualOverrideInputChanged(input: String) {
        // This needs a proper implementation based on the new controller
    }

    fun applyClockDebugManualOverride() {
        // This needs a proper implementation based on the new controller
    }

    fun clearClockDebugManualOverride() {
        clockDebugController.clearManualOverride()
    }

    fun onDebugMinTemperatureChanged(value: String) {
        // This needs a proper implementation based on the new controller
    }

    fun onDebugMaxTemperatureChanged(value: String) {
        // This needs a proper implementation based on the new controller
    }

    fun onDebugHumidityChanged(value: String) {
        // This needs a proper implementation based on the new controller
    }

    fun applyWeatherDebugOverride() {
        // This needs a proper implementation based on the new controller
    }

    fun clearWeatherDebugOverride() {
        weatherDebugController.clearOverride()
    }

    class Factory(
        private val closetRepository: ClosetRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val wearFeedbackRepository: WearFeedbackRepository,
        private val weatherRepository: WeatherRepository,
        private val weatherDebugController: WeatherDebugController,
        private val clockDebugController: DebugClockController,
        private val locationSearchRepository: LocationSearchRepository,
        private val stringResolver: (Int) -> String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                val getRecommendations = GetRecommendationsUseCase()
                val applyWear = ApplyWearUseCase()
                return DashboardViewModel(
                    closetRepository = closetRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    wearFeedbackRepository = wearFeedbackRepository,
                    weatherRepository = weatherRepository,
                    weatherDebugController = weatherDebugController,
                    clockDebugController = clockDebugController,
                    locationSearchRepository = locationSearchRepository,
                    getRecommendations = getRecommendations,
                    applyWear = applyWear,
                    stringResolver = stringResolver
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

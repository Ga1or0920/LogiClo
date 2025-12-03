package com.example.myapplication.data.weather

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Allows developers to override weather values at runtime for debugging.
 */
interface WeatherDebugController {
    val override: StateFlow<WeatherDebugOverride?>
    val isSupported: Boolean

    fun applyOverride(override: WeatherDebugOverride)
    fun clearOverride()
}

data class WeatherDebugOverride(
    val minTemperatureCelsius: Double,
    val maxTemperatureCelsius: Double,
    val humidityPercent: Int
)

class WeatherDebugControllerImpl : WeatherDebugController {
    private val _override = MutableStateFlow<WeatherDebugOverride?>(null)
    override val override: StateFlow<WeatherDebugOverride?> = _override.asStateFlow()
    override val isSupported: Boolean = true

    override fun applyOverride(override: WeatherDebugOverride) {
        _override.value = override
    }

    override fun clearOverride() {
        _override.value = null
    }
}

object NoOpWeatherDebugController : WeatherDebugController {
    private val state = MutableStateFlow<WeatherDebugOverride?>(null)
    override val override: StateFlow<WeatherDebugOverride?> = state
    override val isSupported: Boolean = false

    override fun applyOverride(override: WeatherDebugOverride) {
        // No-op
    }

    override fun clearOverride() {
        // No-op
    }
}

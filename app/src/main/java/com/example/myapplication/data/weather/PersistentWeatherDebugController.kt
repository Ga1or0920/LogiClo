package com.example.myapplication.data.weather

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists debug weather overrides so they survive process deaths during testing.
 */
class PersistentWeatherDebugController(
    context: Context
) : WeatherDebugController {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _override = MutableStateFlow(readOverride())
    override val override: StateFlow<WeatherDebugOverride?> = _override.asStateFlow()
    override val isSupported: Boolean = true

    override fun applyOverride(override: WeatherDebugOverride) {
        preferences.edit()
            .putLong(KEY_MIN, override.minTemperatureCelsius.toRawBits())
            .putLong(KEY_MAX, override.maxTemperatureCelsius.toRawBits())
            .putInt(KEY_HUMIDITY, override.humidityPercent)
            .apply()
        _override.value = override
    }

    override fun clearOverride() {
        preferences.edit()
            .remove(KEY_MIN)
            .remove(KEY_MAX)
            .remove(KEY_HUMIDITY)
            .apply()
        _override.value = null
    }

    private fun readOverride(): WeatherDebugOverride? {
        if (!preferences.contains(KEY_MIN) ||
            !preferences.contains(KEY_MAX) ||
            !preferences.contains(KEY_HUMIDITY)
        ) {
            return null
        }
        val minBits = preferences.getLong(KEY_MIN, DEFAULT_DOUBLE_BITS)
        val maxBits = preferences.getLong(KEY_MAX, DEFAULT_DOUBLE_BITS)
        val humidity = preferences.getInt(KEY_HUMIDITY, DEFAULT_HUMIDITY)
        return WeatherDebugOverride(
            minTemperatureCelsius = minBits.toDoubleFromBits(),
            maxTemperatureCelsius = maxBits.toDoubleFromBits(),
            humidityPercent = humidity
        )
    }

    companion object {
        private const val PREFS_NAME = "weather_debug_overrides"
        private const val KEY_MIN = "min_temp_bits"
        private const val KEY_MAX = "max_temp_bits"
        private const val KEY_HUMIDITY = "humidity"
        private const val DEFAULT_HUMIDITY = 0
        private val DEFAULT_DOUBLE_BITS = java.lang.Double.doubleToRawLongBits(0.0)
    }
}

private fun Double.toRawBits(): Long = java.lang.Double.doubleToRawLongBits(this)

private fun Long.toDoubleFromBits(): Double = java.lang.Double.longBitsToDouble(this)

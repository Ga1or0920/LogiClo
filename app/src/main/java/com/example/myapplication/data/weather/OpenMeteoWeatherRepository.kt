package com.example.myapplication.data.weather

import android.util.Log
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.util.time.InstantCompat
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OpenMeteoWeatherRepository(
    private val coordinates: Coordinates,
    initialSnapshot: WeatherSnapshot,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    private val state = MutableStateFlow(initialSnapshot)

    override fun observeCurrentWeather(): Flow<WeatherSnapshot> = state

    override suspend fun refresh() {
        try {
            val snapshot = fetchSnapshot()
            state.value = snapshot
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to fetch weather", t)
        }
    }

    private suspend fun fetchSnapshot(): WeatherSnapshot = withContext(ioDispatcher) {
        val url = buildUrl()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Unexpected response code: $code")
            }
            connection.inputStream.use { stream ->
                val response = stream.bufferedReader().use { it.readText() }
                parseWeather(response)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(): String {
        return "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${coordinates.latitude}" +
            "&longitude=${coordinates.longitude}" +
            "&daily=temperature_2m_max,temperature_2m_min" +
            "&hourly=relative_humidity_2m" +
            "&current_weather=true" +
            "&timezone=auto"
    }

    private fun parseWeather(response: String): WeatherSnapshot {
        val json = JSONObject(response)
        val daily = json.optJSONObject("daily")
        val hourly = json.optJSONObject("hourly")
        val minTemp = daily?.optJSONArray("temperature_2m_min")?.optDouble(0) ?: state.value.minTemperatureCelsius
        val maxTemp = daily?.optJSONArray("temperature_2m_max")?.optDouble(0) ?: state.value.maxTemperatureCelsius
        val humidityArray = hourly?.optJSONArray("relative_humidity_2m")
        val humidity = humidityArray?.optDouble(0)?.toInt() ?: state.value.humidityPercent
        val updatedAt = parseUpdatedAt(json) ?: InstantCompat.nowOrNull()
        return WeatherSnapshot(
            minTemperatureCelsius = minTemp,
            maxTemperatureCelsius = maxTemp,
            humidityPercent = humidity,
            updatedAt = updatedAt
        )
    }

    private fun parseUpdatedAt(json: JSONObject): Instant? {
        val currentWeather = json.optJSONObject("current_weather") ?: return null
        val timeString = currentWeather.optString("time").takeIf { it.isNotBlank() } ?: return null
        val timezoneId = json.optString("timezone", "").takeIf { it.isNotBlank() }
        val zone: ZoneId? = timezoneId?.let { id ->
            runCatching { ZoneId.of(id) }.getOrNull()
        } ?: if (json.has("utc_offset_seconds")) {
            val offsetSeconds = json.optInt("utc_offset_seconds", 0)
            runCatching { ZoneOffset.ofTotalSeconds(offsetSeconds) }.getOrNull()
        } else {
            null
        }
        return runCatching {
            val localDateTime = LocalDateTime.parse(timeString)
            val resolvedZone = zone ?: ZoneId.systemDefault()
            localDateTime.atZone(resolvedZone).toInstant()
        }.getOrNull()
    }

    data class Coordinates(
        val latitude: Double,
        val longitude: Double
    )

    companion object {
        private const val TIMEOUT_MILLIS = 10_000
        private const val TAG = "OpenMeteoRepo"
    }
}

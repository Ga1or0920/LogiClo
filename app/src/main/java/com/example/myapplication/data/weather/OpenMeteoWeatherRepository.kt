package com.example.myapplication.data.weather

import android.os.Build
import android.util.Log
import com.example.myapplication.domain.model.CasualForecastDay
import com.example.myapplication.domain.model.CasualForecastSegment
import com.example.myapplication.domain.model.CasualForecastSegmentSummary
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.util.time.InstantCompat
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
class OpenMeteoWeatherRepository(
    coordinates: Coordinates,
    initialSnapshot: WeatherSnapshot,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    private val state = MutableStateFlow(initialSnapshot)
    private val coordinatesState = MutableStateFlow(coordinates)

    override fun observeCurrentWeather(): Flow<WeatherSnapshot> = state

    override suspend fun refresh() {
        try {
            val snapshot = fetchSnapshot(coordinatesState.value)
            state.value = snapshot
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to fetch weather", t)
        }
    }

    fun updateCoordinates(newCoordinates: Coordinates) {
        if (coordinatesState.value != newCoordinates) {
            coordinatesState.value = newCoordinates
        }
    }

    private suspend fun fetchSnapshot(coordinates: Coordinates): WeatherSnapshot = withContext(ioDispatcher) {
        val url = buildUrl(coordinates)
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

    private fun buildUrl(coordinates: Coordinates): String {
        return "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${coordinates.latitude}" +
            "&longitude=${coordinates.longitude}" +
            "&daily=temperature_2m_max,temperature_2m_min" +
            "&hourly=relative_humidity_2m,apparent_temperature" +
            "&current=apparent_temperature,relative_humidity_2m,temperature_2m,weather_code" +
            "&timezone=auto"
    }

    private fun parseWeather(response: String): WeatherSnapshot {
        val json = JSONObject(response)
        val daily = json.optJSONObject("daily")
        val hourly = json.optJSONObject("hourly")
        val current = json.optJSONObject("current")
        val minTemp = daily?.optJSONArray("temperature_2m_min")?.optDouble(0, Double.NaN)
            ?.takeIf { !it.isNaN() }
            ?: state.value.minTemperatureCelsius
        val maxTemp = daily?.optJSONArray("temperature_2m_max")?.optDouble(0, Double.NaN)
            ?.takeIf { !it.isNaN() }
            ?: state.value.maxTemperatureCelsius
        // 体感温度と湿度は "current" オブジェクトから取得
        // has()でキーの存在を確認してから取得（キーがない場合の0.0を防ぐ）
        val apparentTemp = if (current?.has("apparent_temperature") == true) {
            current.optDouble("apparent_temperature", Double.NaN).takeIf { !it.isNaN() }
        } else {
            null
        } ?: state.value.apparentTemperatureCelsius
        val humidity = current?.optInt("relative_humidity_2m", -1)?.takeIf { it >= 0 }
            ?: hourly?.optJSONArray("relative_humidity_2m")?.optDouble(0)?.toInt()
            ?: state.value.humidityPercent
        val weatherCode = if (current?.has("weather_code") == true) {
            current.optInt("weather_code", -1).takeIf { it >= 0 } ?: 0
        } else { 0 }
        val zone = resolveZone(json)
        val updatedAt = parseUpdatedAt(current, zone) ?: InstantCompat.nowOrNull()
        val casualSummaries = parseCasualSummaries(hourly, zone)
        return WeatherSnapshot(
            minTemperatureCelsius = minTemp,
            maxTemperatureCelsius = maxTemp,
            apparentTemperatureCelsius = apparentTemp,
            humidityPercent = humidity,
            weatherCode = weatherCode,
            updatedAt = updatedAt,
            casualSegmentSummaries = casualSummaries
        )
    }

    private fun parseUpdatedAt(current: JSONObject?, zone: ZoneId?): Instant? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || current == null) {
            return null
        }
        val timeString = current.optString("time").takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val localDateTime = LocalDateTime.parse(timeString)
            val resolvedZone = zone ?: ZoneId.systemDefault()
            localDateTime.atZone(resolvedZone).toInstant()
        }.getOrNull()
    }

    private fun resolveZone(json: JSONObject): ZoneId? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null
        }
        val timezoneId = json.optString("timezone", "").takeIf { it.isNotBlank() }
        val zoneFromId = timezoneId?.let { id ->
            runCatching { ZoneId.of(id) }.getOrNull()
        }
        if (zoneFromId != null) {
            return zoneFromId
        }
        if (json.has("utc_offset_seconds")) {
            val offsetSeconds = json.optInt("utc_offset_seconds", 0)
            return runCatching { ZoneOffset.ofTotalSeconds(offsetSeconds) }.getOrNull()
        }
        return null
    }

    private fun parseCasualSummaries(hourly: JSONObject?, zone: ZoneId?): List<CasualForecastSegmentSummary> {
        if (hourly == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return emptyList()
        }
        val timeArray = hourly.optJSONArray("time") ?: return emptyList()
        val apparentArray = hourly.optJSONArray("apparent_temperature") ?: return emptyList()
        val length = minOf(timeArray.length(), apparentArray.length())
        if (length == 0) {
            return emptyList()
        }
        val resolvedZone = zone ?: ZoneId.systemDefault()
        val currentDate = LocalDate.now(resolvedZone)
        val stats = mutableMapOf<Pair<CasualForecastDay, CasualForecastSegment>, SegmentAccumulator>()
        for (index in 0 until length) {
            val timeString = timeArray.optString(index)
            if (timeString.isNullOrBlank()) continue
            val apparent = apparentArray.optDouble(index, Double.NaN)
            if (java.lang.Double.isNaN(apparent)) continue
            val localDateTime = runCatching { LocalDateTime.parse(timeString) }.getOrNull() ?: continue
            val localDate = localDateTime.toLocalDate()
            val dayOffset = ChronoUnit.DAYS.between(currentDate, localDate).toInt()
            val day = when (dayOffset) {
                0 -> CasualForecastDay.TODAY
                1 -> CasualForecastDay.TOMORROW
                else -> continue
            }
            val segment = resolveSegment(localDateTime.hour) ?: continue
            val accumulator = stats.getOrPut(day to segment) { SegmentAccumulator() }
            accumulator.add(apparent)
        }
        return stats.mapNotNull { (key, accumulator) ->
            if (!accumulator.isValid()) return@mapNotNull null
            val (day, segment) = key
            CasualForecastSegmentSummary(
                day = day,
                segment = segment,
                minTemperatureCelsius = accumulator.min,
                maxTemperatureCelsius = accumulator.max,
                averageApparentTemperatureCelsius = accumulator.average
            )
        }.sortedWith(
            compareBy<CasualForecastSegmentSummary> { it.day.ordinal }
                .thenBy { it.segment.ordinal }
        )
    }

    private fun resolveSegment(hour: Int): CasualForecastSegment? = when (hour) {
        in 6..11 -> CasualForecastSegment.MORNING
        in 12..17 -> CasualForecastSegment.AFTERNOON
        in 18..23 -> CasualForecastSegment.EVENING
        else -> null
    }

    private class SegmentAccumulator {
        private var sum = 0.0
        private var count = 0
        var min: Double = Double.POSITIVE_INFINITY
            private set
        var max: Double = Double.NEGATIVE_INFINITY
            private set

        fun add(value: Double) {
            sum += value
            count += 1
            if (value < min) min = value
            if (value > max) max = value
        }

        fun isValid(): Boolean = count > 0 && min.isFiniteValue() && max.isFiniteValue() && sum.isFiniteValue()

        val average: Double
            get() = if (count == 0) Double.NaN else sum / count
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

private fun Double.isFiniteValue(): Boolean =
    !java.lang.Double.isNaN(this) && !java.lang.Double.isInfinite(this)

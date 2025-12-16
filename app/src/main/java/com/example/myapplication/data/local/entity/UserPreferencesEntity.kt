package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myapplication.domain.model.ColorRules
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.domain.model.WeatherLocationOverride
import com.example.myapplication.util.time.InstantCompat
import java.time.Instant

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastLoginEpochMillis: Long?,
    val lastSelectedMode: String,
    @ColumnInfo(defaultValue = "outdoor") val lastSelectedEnvironment: String,
    val allowBlackNavy: Boolean,
    val disallowVividPair: Boolean,
    @ColumnInfo(defaultValue = "{}") val defaultMaxWearsJson: String,
    val indoorTemperatureCelsius: Double? = null,
    val weatherLocationLabel: String? = null,
    val weatherLocationLatitude: Double? = null,
    val weatherLocationLongitude: Double? = null
) {
    companion object {
        const val SINGLETON_ID: Int = 0
    }
}

fun UserPreferencesEntity.toDomain(): UserPreferences = UserPreferences(
    lastLogin = InstantCompat.ofEpochMilliOrNull(lastLoginEpochMillis),
    lastSelectedMode = TpoMode.fromBackend(lastSelectedMode),
    lastSelectedEnvironment = EnvironmentMode.fromBackend(lastSelectedEnvironment),
    tempOffsets = emptyMap(),
    colorRules = ColorRules(
        allowBlackNavy = allowBlackNavy,
        disallowVividPair = disallowVividPair
    ),
    defaultMaxWears = parseDefaultMaxWears(defaultMaxWearsJson),
    weatherLocationOverride = buildWeatherLocationOverride(
        label = weatherLocationLabel,
        latitude = weatherLocationLatitude,
        longitude = weatherLocationLongitude
    ),
    indoorTemperatureCelsius = indoorTemperatureCelsius
)

fun UserPreferences.toEntity(): UserPreferencesEntity = UserPreferencesEntity(
    lastLoginEpochMillis = InstantCompat.toEpochMilliOrNull(lastLogin),
    lastSelectedMode = lastSelectedMode.backendValue,
    lastSelectedEnvironment = lastSelectedEnvironment.backendValue,
    allowBlackNavy = colorRules.allowBlackNavy,
    disallowVividPair = colorRules.disallowVividPair,
    defaultMaxWearsJson = encodeDefaultMaxWears(defaultMaxWears),
    indoorTemperatureCelsius = indoorTemperatureCelsius,
    weatherLocationLabel = weatherLocationOverride?.label,
    weatherLocationLatitude = weatherLocationOverride?.latitude,
    weatherLocationLongitude = weatherLocationOverride?.longitude
)

private fun parseDefaultMaxWears(raw: String?): Map<ClothingCategory, Int> {
    if (raw.isNullOrBlank() || raw == "{}") return emptyMap()
    return raw.split(ENTRY_DELIMITER)
        .mapNotNull { entry ->
            val parts = entry.split(KEY_VALUE_DELIMITER)
            if (parts.size != 2) return@mapNotNull null
            val category = ClothingCategory.fromBackend(parts[0])
            if (category == ClothingCategory.UNKNOWN) return@mapNotNull null
            val value = parts[1].toIntOrNull() ?: return@mapNotNull null
            category to value
        }.toMap()
}

private fun encodeDefaultMaxWears(values: Map<ClothingCategory, Int>): String {
    if (values.isEmpty()) return "{}"
    return values.entries.joinToString(separator = ENTRY_DELIMITER) { (category, max) ->
        "${category.backendValue}$KEY_VALUE_DELIMITER$max"
    }
}

private fun buildWeatherLocationOverride(
    label: String?,
    latitude: Double?,
    longitude: Double?
): WeatherLocationOverride? {
    val normalizedLabel = label?.takeIf { it.isNotBlank() } ?: return null
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return WeatherLocationOverride(
        label = normalizedLabel,
        latitude = lat,
        longitude = lon
    )
}

private const val ENTRY_DELIMITER = ";"
private const val KEY_VALUE_DELIMITER = ":"

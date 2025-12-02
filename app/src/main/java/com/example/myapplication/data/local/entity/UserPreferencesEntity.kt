package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myapplication.domain.model.ColorRules
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.UserPreferences
import java.time.Instant

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastLoginEpochMillis: Long?,
    val lastSelectedMode: String,
    @ColumnInfo(defaultValue = "outdoor") val lastSelectedEnvironment: String,
    val allowBlackNavy: Boolean,
    val disallowVividPair: Boolean
) {
    companion object {
        const val SINGLETON_ID: Int = 0
    }
}

fun UserPreferencesEntity.toDomain(): UserPreferences = UserPreferences(
    lastLogin = lastLoginEpochMillis?.let(Instant::ofEpochMilli),
    lastSelectedMode = TpoMode.fromBackend(lastSelectedMode),
    lastSelectedEnvironment = EnvironmentMode.fromBackend(lastSelectedEnvironment),
    tempOffsets = emptyMap(),
    colorRules = ColorRules(
        allowBlackNavy = allowBlackNavy,
        disallowVividPair = disallowVividPair
    ),
    defaultMaxWears = emptyMap()
)

fun UserPreferences.toEntity(): UserPreferencesEntity = UserPreferencesEntity(
    lastLoginEpochMillis = lastLogin?.toEpochMilli(),
    lastSelectedMode = lastSelectedMode.backendValue,
    lastSelectedEnvironment = lastSelectedEnvironment.backendValue,
    allowBlackNavy = colorRules.allowBlackNavy,
    disallowVividPair = colorRules.disallowVividPair
)

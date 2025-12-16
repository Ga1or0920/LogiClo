package com.example.myapplication.ui.theme

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM
}

val LocalThemePreference: androidx.compose.runtime.ProvidableCompositionLocal<MutableState<ThemePreference>> =
    staticCompositionLocalOf { mutableStateOf(ThemePreference.SYSTEM) }

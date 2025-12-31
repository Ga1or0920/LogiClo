package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Custom Colors from Flutter App
val BgGrey = Color(0xFFF2F4F7)
val CardWhite = Color.White
val TextBlack = Color(0xFF2D3436)
val TextGreyLight = Color(0xFF636E72)
val TextGreyDark = Color(0xFFB0B8BC)
val AccentBlue = Color(0xFF0984E3)
val AccentRed = Color(0xFFD63031)
val Navy = Color(0xFF2C3E50)

// テーマ連動のTextGrey（MaterialTheme.colorScheme.onSurfaceVariantを使用）
val TextGrey: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

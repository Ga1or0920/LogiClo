package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.theme.AccentBlue
import com.example.myapplication.ui.theme.BgGrey
import com.example.myapplication.ui.theme.CardWhite
import com.example.myapplication.ui.theme.Pink40
import com.example.myapplication.ui.theme.Pink80
import com.example.myapplication.ui.theme.TextBlack
import com.example.myapplication.ui.theme.TextGrey // <-- ADDED THIS IMPORT
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TextBlack,
    secondary = AccentBlue,
    tertiary = Pink80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFEAEAEA),
    onSurface = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFF1E1E1E),
    outline = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TextBlack,
    secondary = AccentBlue,
    tertiary = Pink40,
    background = BgGrey,
    surface = CardWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextBlack,
    onSurface = TextBlack,
    surfaceVariant = BgGrey,
    outline = TextGrey
)

@Composable
fun LogiCloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
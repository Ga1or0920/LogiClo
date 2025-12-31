package com.example.myapplication.ui.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.LaundryLoopApplication
import com.example.myapplication.data.AppContainer
import com.example.myapplication.data.InMemoryAppContainer

val LocalAppContainer: ProvidableCompositionLocal<AppContainer> = staticCompositionLocalOf {
    error("AppContainer is not provided")
}

@Composable
fun rememberAppContainer(): AppContainer {
    val appContext = LocalContext.current.applicationContext
    return if (appContext is LaundryLoopApplication) {
        appContext.container
    } else {
        remember { InMemoryAppContainer() }
    }
}

@Composable
fun ProvideAppContainer(
    appContainer: AppContainer,
    content: @Composable () -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalAppContainer provides appContainer) {
        content()
    }
}

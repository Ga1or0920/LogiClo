package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.ui.navigation.AppNavGraph
import com.example.myapplication.ui.providers.LocalAppContainer
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun LaundryLoopApp() {
    val appContainer = LocalAppContainer.current
    val userPreferences by appContainer.userPreferencesRepository.observe().collectAsStateWithLifecycle(
        initialValue = null
    )

    userPreferences?.let { preferences ->
        MyApplicationTheme(themeOption = preferences.theme) {
            AppNavGraph()
        }
    }
}

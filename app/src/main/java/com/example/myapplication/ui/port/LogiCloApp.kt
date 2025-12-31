package com.example.myapplication.ui.port

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LogiCloApp(viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val isDarkTheme by viewModel.isDarkTheme

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        MainScreen(viewModel)
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(if (selectedIndex == 0) Icons.Default.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(if (selectedIndex == 1) Icons.Default.Checkroom else Icons.Outlined.Checkroom, contentDescription = "Closet") },
                    label = { Text("Closet") },
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(if (selectedIndex == 2) Icons.Default.LocalLaundryService else Icons.Outlined.LocalLaundryService, contentDescription = "Laundry") },
                    label = { Text("Laundry") },
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(if (selectedIndex == 3) Icons.Default.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedIndex == 3,
                    onClick = { selectedIndex = 3 }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedIndex) {
                0 -> DashboardScreen(viewModel)
                1 -> ClosetScreen(viewModel)
                2 -> LaundryScreen(viewModel)
                3 -> SettingsScreen(viewModel)
            }
        }
    }
}

/*
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LogiCloApp()
}
*/

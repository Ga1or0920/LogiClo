package com.example.myapplication.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myapplication.ui.closet.ClosetDestinations
import com.example.myapplication.ui.closet.ClosetEditorScreen
import com.example.myapplication.ui.closet.ClosetScreen
import com.example.myapplication.ui.closet.navigateToAddClosetItem
import com.example.myapplication.ui.dashboard.DashboardScreen
import com.example.myapplication.ui.laundry.LaundryScreen
import com.example.myapplication.ui.navigation.AppDestination
import com.example.myapplication.ui.navigation.rememberLaundryLoopAppState
import com.example.myapplication.ui.providers.ProvideAppContainer
import com.example.myapplication.ui.providers.rememberAppContainer

/**
 * アプリ全体のナビゲーションと共通 UI フレームを司るコンポーザブル。
 */
@Composable
fun LaundryLoopApp() {
    val appContainer = rememberAppContainer()
    val appState = rememberLaundryLoopAppState()
    val navController = appState.navController
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestinationRoute = currentBackStackEntry?.destination?.route

    ProvideAppContainer(appContainer = appContainer) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    appState.destinations.forEach { destination ->
                        val selected = currentDestinationRoute?.startsWith(destination.route) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { if (!selected) appState.navigateTo(destination) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(text = getStringResource(destination))
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestination.Dashboard.route) {
                    DashboardScreen()
                }
                composable(AppDestination.Closet.route) {
                    ClosetScreen(
                        onAddItem = { navController.navigateToAddClosetItem() }
                    )
                }
                composable(ClosetDestinations.AddItem) {
                    ClosetEditorScreen(
                        onClose = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
                composable(AppDestination.Laundry.route) {
                    LaundryScreen()
                }
            }
        }
    }
}

@Composable
private fun getStringResource(destination: AppDestination): String {
    return androidx.compose.ui.res.stringResource(id = destination.labelRes)
}

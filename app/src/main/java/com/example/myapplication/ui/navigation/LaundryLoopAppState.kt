package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

class LaundryLoopAppState(
    val navController: NavHostController
) {
    val destinations: List<AppDestination> = AppDestination.entries

    fun navigateTo(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun rememberLaundryLoopAppState(
    navController: NavHostController = rememberNavController()
): LaundryLoopAppState = remember(navController) {
    LaundryLoopAppState(navController)
}

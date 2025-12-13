package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.closet.ClosetRoute
import com.example.myapplication.ui.dashboard.DashboardRoute
import com.example.myapplication.ui.laundry.LaundryRoute
import com.example.myapplication.ui.settings.SettingsRoute

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppDestination.Dashboard.route) {
        composable(AppDestination.Dashboard.route) { DashboardRoute(navController) }
        composable(AppDestination.Closet.route) { ClosetRoute() }
        composable(AppDestination.Laundry.route) { LaundryRoute() }
        composable(AppDestination.Settings.route) { SettingsRoute(navController) }
    }
}

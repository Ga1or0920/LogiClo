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
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.myapplication.ui.closet.ClosetDestinations
import com.example.myapplication.ui.closet.ClosetEditorScreen
import com.example.myapplication.ui.closet.ClosetScreen
import com.example.myapplication.ui.closet.navigateToAddClosetItem
import com.example.myapplication.ui.closet.navigateToEditClosetItem
import com.example.myapplication.ui.dashboard.DashboardScreen
import com.example.myapplication.ui.laundry.LaundryScreen
import com.example.myapplication.ui.navigation.AppDestination
import com.example.myapplication.ui.navigation.rememberLaundryLoopAppState
import com.example.myapplication.ui.providers.ProvideAppContainer
import com.example.myapplication.ui.providers.rememberAppContainer
import com.example.myapplication.ui.settings.SettingsRoute

/**
 * アプリ全体のナビゲーションと共通 UI フレームを司るコンポーザブル。
 */
@Composable
fun LaundryLoopApp(navController: NavHostController, startDestination: String? = null) {
    val appContainer = rememberAppContainer()
    val appState = rememberLaundryLoopAppState(navController)
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
            val effectiveStartDestination = startDestination ?: AppDestination.Dashboard.route
            NavHost(
                navController = navController,
                startDestination = effectiveStartDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestination.Dashboard.route) {
                    DashboardScreen()
                }
                composable(AppDestination.Closet.route) {
                    ClosetScreen(
                        onAddItem = { navController.navigateToAddClosetItem() },
                        onEditItem = { itemId -> navController.navigateToEditClosetItem(itemId) }
                    )
                }
                composable(ClosetDestinations.AddItem) {
                    ClosetEditorScreen(
                        existingItemId = null,
                        onClose = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
                composable(AppDestination.Laundry.route) {
                    LaundryScreen()
                }
                composable(AppDestination.Settings.route) {
                    SettingsRoute(navController)
                }
                composable(
                    route = ClosetDestinations.EditItem,
                    arguments = listOf(navArgument(ClosetDestinations.EditItemArg) { type = NavType.StringType })
                ) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getString(ClosetDestinations.EditItemArg)
                    ClosetEditorScreen(
                        existingItemId = itemId,
                        onClose = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
                // --- 追加: フィードバック遷移先 ---
                composable(
                    route = "feedback/pending"
                ) {
                    // TODO: PendingFeedbackScreen等、該当画面を呼び出す
                    // ここに画面が未実装の場合は仮のTextを表示
                    androidx.compose.material3.Text("着用フィードバック画面（仮）")
                }
            }
        }
    }
}

@Composable
private fun getStringResource(destination: AppDestination): String {
    return androidx.compose.ui.res.stringResource(id = destination.labelRes)
}

package com.example.myapplication.ui.logiclo

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.LogiCloTheme

sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", "ホーム", Icons.Filled.Home, Icons.Outlined.Home)
    object Closet : Screen("closet", "クローゼット", Icons.Filled.Checkroom, Icons.Outlined.Checkroom)
    object Laundry : Screen("laundry", "洗濯", Icons.Filled.LocalLaundryService, Icons.Outlined.LocalLaundryService)
    object Settings : Screen("settings", "設定", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Closet,
    Screen.Laundry,
    Screen.Settings,
)

@Composable
fun LogiCloApp(viewModel: LogiCloViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val useDarkTheme = when (uiState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    LogiCloTheme(darkTheme = useDarkTheme) {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Dashboard.route, Modifier.padding(innerPadding)) {
                composable(Screen.Dashboard.route) { DashboardScreen(viewModel) }
                composable(Screen.Closet.route) { ClosetScreen(viewModel) }
                composable(Screen.Laundry.route) { LaundryScreen(viewModel) }
                composable(Screen.Settings.route) { SettingsScreen(viewModel) }
            }
        }
    }
}

/**
 * ## Flutter to Compose Migration Analysis
 *
 * This section briefly explains how the UI components from the original Flutter
 * application were mapped to Jetpack Compose.
 *
 * ### 1. Core Structure
 * - **`MaterialApp`**: The root of the Flutter app is replaced by `LogiCloTheme`, a custom Material 3 theme wrapper, which handles light/dark mode.
 * - **`MainScreen (StatefulWidget)`**: The Flutter widget holding the `BottomNavigationBar` is now the `LogiCloApp` composable, which contains a `Scaffold`.
 * - **`Scaffold`**: Both frameworks have a `Scaffold`. In Compose, it's used with slots for `bottomBar`, `floatingActionButton`, etc.
 * - **`IndexedStack` & `BottomNavigationBar`**: This navigation pattern is replaced by the official `androidx.navigation.compose` library. `NavHost` handles switching between screen composables, and `NavigationBar` with `NavigationBarItem` creates the bottom navigation UI. State is managed by a `rememberNavController()`.
 *
 * ### 2. State Management
 * - **`AppController (ChangeNotifier)`**: All application state and business logic were moved from this class into a `LogiCloViewModel` (an `androidx.lifecycle.ViewModel`).
 * - **`setState` & `AnimatedBuilder`**: These are replaced by Compose's reactive state system. The `ViewModel` exposes state via `StateFlow`, which is collected in the UI using `.collectAsState()`. When the state changes, only the composables that use that state are automatically recomposed.
 *
 * ### 3. UI Widgets to Composables
 * - **`Column` / `Row`**: Directly map to `Column` and `Row` composables.
 * - **`Container`**: This versatile Flutter widget is replaced by `Box`, `Surface`, or `Modifier` chains in Compose. For example, `Container(decoration: ...)` becomes `Surface(shape = ..., color = ..., modifier = ...)`. Padding is applied via `Modifier.padding()`.
 * - **`ListView.builder`**: Replaced by `LazyColumn` for efficiently displaying scrollable lists.
 * - **`Card`**: Maps directly to the `Card` composable in Compose.
 * - **`ListTile`**: There's no direct equivalent. A custom layout using `Row` with `Icon`, `Column` (for title/subtitle), and `IconButton` is used to achieve a similar result.
 * - **`FloatingActionButton`**: Maps directly to `FloatingActionButton` or `ExtendedFloatingActionButton`.
 * - **`showModalBottomSheet` / `DraggableScrollableSheet`**: This functionality is implemented using the `ModalBottomSheet` composable from `androidx.compose.material3`. Its visibility is controlled by a mutable state variable (e.g., `remember { mutableStateOf(false) }`).
 * - **`TabBar` / `TabBarView`**: Replaced with `TabRow` and `HorizontalPager` (or simple conditional logic) to display different content based on the selected tab.
 * - **`Text` / `Icon` / `Button` / `Slider` / `Switch`**: All have direct equivalents in Compose (`Text`, `Icon`, `Button`, `Slider`, `Switch`).
 * - **Custom Widgets (`_SlidingToggle`, `_FilterChip`)**: These are rebuilt as new, self-contained `@Composable` functions, using basic building blocks like `Box`, `Row`, `Surface`, and `Modifier.clickable {}`.
 *
 * ### 4. Previews
 * - To facilitate development and testing, `@Preview` annotations are added to composables. This allows developers to see how a UI component looks in Android Studio without running the app on a device or emulator.
 */

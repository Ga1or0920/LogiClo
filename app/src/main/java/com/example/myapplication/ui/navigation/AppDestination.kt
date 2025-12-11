package com.example.myapplication.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.R

/**
 * アプリ内の主要画面を表す定義。ルートとアイコンをまとめて管理してナビゲーションの重複を防ぐ。
 */
enum class AppDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    Dashboard(
        route = "dashboard",
        labelRes = R.string.navigation_dashboard,
        icon = Icons.Outlined.Dashboard
    ),
    Closet(
        route = "closet",
        labelRes = R.string.navigation_closet,
        icon = Icons.Outlined.Checkroom
    ),
    Laundry(
        route = "laundry",
        labelRes = R.string.navigation_laundry,
        icon = Icons.Outlined.LocalLaundryService
    ),
    Settings(
        route = "settings",
        labelRes = R.string.navigation_settings,
        icon = Icons.Outlined.Settings
    );
}

package com.example.myapplication.ui.closet

import androidx.navigation.NavController

/**
 * クローゼット関連サブ画面のルート定義をまとめる。呼び出し側のルート文字列直書きを防止する。
 */
object ClosetDestinations {
    const val AddItem = "closet/add"
}

fun NavController.navigateToAddClosetItem() {
    navigate(ClosetDestinations.AddItem) {
        launchSingleTop = true
    }
}

package com.example.myapplication.ui.closet

import androidx.navigation.NavController

/**
 * クローゼット関連サブ画面のルート定義をまとめる。呼び出し側のルート文字列直書きを防止する。
 */
object ClosetDestinations {
    const val AddItem = "closet/add"
    const val EditItem = "closet/edit/{itemId}"
    const val EditItemArg = "itemId"
}

fun NavController.navigateToAddClosetItem() {
    navigate(ClosetDestinations.AddItem) {
        launchSingleTop = true
    }
}

fun NavController.navigateToEditClosetItem(itemId: String) {
    navigate("closet/edit/$itemId") {
        launchSingleTop = true
    }
}

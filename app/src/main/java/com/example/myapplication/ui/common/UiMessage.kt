package com.example.myapplication.ui.common

import androidx.annotation.StringRes

data class UiMessage(
    @param:StringRes val resId: Int,
    val args: List<UiMessageArg> = emptyList()
)

sealed class UiMessageArg {
    data class Raw(val value: Any) : UiMessageArg()
    data class Resource(@param:StringRes val resId: Int) : UiMessageArg()
}

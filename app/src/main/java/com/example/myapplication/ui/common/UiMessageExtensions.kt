package com.example.myapplication.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

fun UiMessage.resolve(context: Context): String {
    val resolvedArgs = args.map { arg ->
        when (arg) {
            is UiMessageArg.Raw -> arg.value
            is UiMessageArg.Resource -> context.getString(arg.resId)
        }
    }.toTypedArray()
    return context.getString(resId, *resolvedArgs)
}

@Composable
fun UiMessage.resolve(): String {
    val context = LocalContext.current
    return resolve(context)
}

package com.example.myapplication.ui.common

import android.os.Build
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

fun formatWeatherTimestamp(date: Date?): String? {
    if (date == null) return null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
    val formatter = DateTimeFormatter.ofPattern("M/d H:mm").withZone(ZoneId.systemDefault())
    return formatter.format(date.toInstant())
}

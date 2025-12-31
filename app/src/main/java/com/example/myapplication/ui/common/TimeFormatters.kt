package com.example.myapplication.ui.common

import android.os.Build
import com.example.myapplication.util.time.InstantCompat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

fun formatWeatherTimestamp(instant: Instant): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        java.time.format.DateTimeFormatter.ofPattern("M/d HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } else {
        val epochMillis = InstantCompat.toEpochMilliOrNull(instant)
            ?: return SimpleDateFormat("M/d HH:mm", Locale.getDefault()).format(Date())
        SimpleDateFormat("M/d HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }
}

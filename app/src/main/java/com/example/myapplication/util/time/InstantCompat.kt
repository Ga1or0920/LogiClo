package com.example.myapplication.util.time

import android.os.Build
import java.time.Instant

/**
 * Provides a safe way to obtain [Instant] values while respecting the minSdk requirement.
 * Returns null on devices that do not support java.time APIs.
 */
object InstantCompat {
    fun nowOrNull(): Instant? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Instant.now()
    } else {
        null
    }

    fun toEpochMilliOrNull(instant: Instant?): Long? {
        if (instant == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            instant.toEpochMilli()
        } else {
            null
        }
    }

    fun ofEpochMilliOrNull(value: Long?): Instant? {
        if (value == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.ofEpochMilli(value)
        } else {
            null
        }
    }
}

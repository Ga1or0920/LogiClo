package com.example.myapplication.util.time

import java.util.Date

object InstantCompat {

    private var debugOffsetMillis: Long = 0L

    fun registerDebugOffsetProvider(provider: () -> Long) {
        debugOffsetMillis = provider()
    }

    fun nowOrNull(): Date? {
        if (debugOffsetMillis == 0L) return Date()
        return Date(System.currentTimeMillis() + debugOffsetMillis)
    }

    fun toEpochMilliOrNull(date: Date?): Long? {
        return date?.time
    }

    fun ofEpochMilliOrNull(epochMilli: Long?): Date? {
        return epochMilli?.let { Date(it) }
    }
}

package com.example.myapplication.util.time

import java.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controls debug-only time overrides such as advancing the current date.
 */
interface DebugClockController {
    val isSupported: Boolean
    val nextDayEnabled: StateFlow<Boolean>

    fun setNextDayEnabled(enabled: Boolean)
    fun toggleNextDay() = setNextDayEnabled(!nextDayEnabled.value)
    fun clear() = setNextDayEnabled(false)
    fun currentOffsetMillis(): Long
}

class DebugClockControllerImpl : DebugClockController {
    private val _nextDayEnabled = MutableStateFlow(false)
    override val nextDayEnabled: StateFlow<Boolean> = _nextDayEnabled.asStateFlow()
    override val isSupported: Boolean = true

    override fun setNextDayEnabled(enabled: Boolean) {
        _nextDayEnabled.value = enabled
    }

    override fun currentOffsetMillis(): Long {
        return if (_nextDayEnabled.value) Duration.ofDays(1).toMillis() else 0L
    }
}

object NoOpDebugClockController : DebugClockController {
    private val state = MutableStateFlow(false)
    override val isSupported: Boolean = false
    override val nextDayEnabled: StateFlow<Boolean> = state

    override fun setNextDayEnabled(enabled: Boolean) {
        // No-op
    }

    override fun currentOffsetMillis(): Long = 0L
}

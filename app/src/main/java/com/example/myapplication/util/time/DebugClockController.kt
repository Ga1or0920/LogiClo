package com.example.myapplication.util.time

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Controls debug-only time overrides such as advancing the current date.
 */
interface DebugClockController {
    val isSupported: Boolean
    val nextDayEnabled: StateFlow<Boolean>
    val manualOverride: StateFlow<ManualTimeOverride?>

    fun setNextDayEnabled(enabled: Boolean)
    fun toggleNextDay() = setNextDayEnabled(!nextDayEnabled.value)
    fun setManualOverride(epochMillis: Long)
    fun clearManualOverride()
    fun clear() {
        setNextDayEnabled(false)
        clearManualOverride()
    }
    fun currentOffsetMillis(): Long
}

class DebugClockControllerImpl : DebugClockController {
    private val _nextDayEnabled = MutableStateFlow(false)
    override val nextDayEnabled: StateFlow<Boolean> = _nextDayEnabled.asStateFlow()
    private val _manualOverride = MutableStateFlow<ManualTimeOverride?>(null)
    override val manualOverride: StateFlow<ManualTimeOverride?> = _manualOverride.asStateFlow()
    private val manualOffsetMillis = MutableStateFlow<Long?>(null)
    override val isSupported: Boolean = true

    override fun setNextDayEnabled(enabled: Boolean) {
        _nextDayEnabled.value = enabled
        if (enabled) {
            clearManualOverride()
        }
    }

    override fun setManualOverride(epochMillis: Long) {
        val offset = epochMillis - System.currentTimeMillis()
        manualOffsetMillis.value = offset
        _manualOverride.value = ManualTimeOverride(epochMillis)
        _nextDayEnabled.value = false
    }

    override fun clearManualOverride() {
        manualOffsetMillis.value = null
        _manualOverride.value = null
    }

    override fun currentOffsetMillis(): Long {
        val manual = manualOffsetMillis.value
        if (manual != null) {
            return manual
        }
        return if (_nextDayEnabled.value) TimeUnit.DAYS.toMillis(1) else 0L
    }
}

object NoOpDebugClockController : DebugClockController {
    private val state = MutableStateFlow(false)
    override val isSupported: Boolean = false
    override val nextDayEnabled: StateFlow<Boolean> = state
    override val manualOverride: StateFlow<ManualTimeOverride?> = MutableStateFlow(null)

    override fun setNextDayEnabled(enabled: Boolean) {
        // No-op
    }

    override fun setManualOverride(epochMillis: Long) {
        // No-op
    }

    override fun clearManualOverride() {
        // No-op
    }

    override fun currentOffsetMillis(): Long = 0L
}

data class ManualTimeOverride(val targetEpochMillis: Long)

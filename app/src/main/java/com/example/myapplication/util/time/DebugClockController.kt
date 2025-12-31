package com.example.myapplication.util.time

import android.content.Context
import androidx.core.content.edit
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

class DebugClockControllerImpl(context: Context?) : DebugClockController {
    private val prefs = context?.getSharedPreferences("debug_clock_prefs", Context.MODE_PRIVATE)

    private val _nextDayEnabled = MutableStateFlow(false)
    override val nextDayEnabled: StateFlow<Boolean> = _nextDayEnabled.asStateFlow()
    private val _manualOverride = MutableStateFlow<ManualTimeOverride?>(null)
    override val manualOverride: StateFlow<ManualTimeOverride?> = _manualOverride.asStateFlow()
    private val manualOffsetMillis = MutableStateFlow<Long?>(null)
    override val isSupported: Boolean = true

    init {
        // Load persistent state
        if (prefs != null) {
            val nextDay = prefs.getBoolean("next_day_enabled", false)
            _nextDayEnabled.value = nextDay

            val manualTarget = prefs.getLong("manual_target_epoch", -1L)
            if (manualTarget != -1L) {
                // Restore manual override
                // Note: The offset needs to be recalculated relative to CURRENT system time
                // But since we are overriding the absolute time, the offset changes as real time passes.
                // Wait, manual override is "Set to X date/time".
                // If I set "2024-01-01 12:00", and restart app 1 hour later, should it be "2024-01-01 12:00" or "13:00"?
                // Usually for debugging specific dates, we want the target time to be fixed or the offset to be fixed.
                // The current implementation:
                // setManualOverride(epochMillis) -> calculates offset = epochMillis - System.currentTimeMillis()
                // currentOffsetMillis -> uses that fixed offset.
                // So the *offset* is what should be persisted if we want the "time to flow" from the override point.
                // OR we persist the target epoch if we want to snap back to that exact time.
                // The prompt says "date/time", usually implying a specific test scenario.
                // If I persist the *offset*, then if I set it to "7 days ago", it remains "7 days ago" even after restart.
                // If I persist the *target*, then if I restart, it jumps back to that old target.
                
                // Let's persist the *offset* for consistency with "time passing".
                // Actually, looking at `setManualOverride` implementation:
                // `manualOffsetMillis.value = offset`
                // `_manualOverride.value = ManualTimeOverride(epochMillis)`
                // The UI uses `manualOverride` to show the "Set date". 
                // `currentOffsetMillis` uses `manualOffsetMillis`.
                
                // Let's persist the offset.
                val savedOffset = prefs.getLong("manual_offset_millis", 0L)
                // If target is present, we assume offset is also valid (or 0 if not set but target is set - which shouldn't happen if we save both)
                // But wait, if I save offset, I can reconstruct target for UI: target = System.currentTimeMillis() + offset
                
                if (savedOffset != 0L || manualTarget != -1L) {
                    manualOffsetMillis.value = savedOffset
                    // Reconstruct the "target" for UI display purposes. 
                    // Ideally we should have saved the target too if we want to show exactly what was set, 
                    // but showing current overridden time is also fine.
                    // Let's use the saved target for UI if available, but offset for logic.
                     _manualOverride.value = ManualTimeOverride(manualTarget)
                }
            }
        }
    }

    override fun setNextDayEnabled(enabled: Boolean) {
        _nextDayEnabled.value = enabled
        prefs?.edit { putBoolean("next_day_enabled", enabled) }
        if (enabled) {
            clearManualOverride()
        }
    }

    override fun setManualOverride(epochMillis: Long) {
        val offset = epochMillis - System.currentTimeMillis()
        manualOffsetMillis.value = offset
        _manualOverride.value = ManualTimeOverride(epochMillis)
        _nextDayEnabled.value = false
        
        prefs?.edit {
            putLong("manual_target_epoch", epochMillis)
            putLong("manual_offset_millis", offset)
            putBoolean("next_day_enabled", false)
        }
    }

    override fun clearManualOverride() {
        manualOffsetMillis.value = null
        _manualOverride.value = null
        
        prefs?.edit {
            remove("manual_target_epoch")
            remove("manual_offset_millis")
        }
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

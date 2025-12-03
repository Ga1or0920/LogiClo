package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.util.time.InstantCompat
import java.time.Instant

class ApplyWearUseCase(
    private val clock: () -> Instant? = { InstantCompat.nowOrNull() }
) {

    fun execute(item: ClothingItem, weather: WeatherSnapshot): WearOutcome {
        val damageResult = calculateDamage(item, weather)
        val damage = damageResult.damage
        val reason = damageResult.reason

        val forcedDirty = damage >= FORCE_DIRTY_DAMAGE || item.isAlwaysWash
        val updatedCurrent = when {
            forcedDirty -> item.maxWears
            else -> (item.currentWears + damage).coerceAtLeast(0)
        }
        val shouldMoveToDirty = forcedDirty || updatedCurrent >= item.maxWears

        val updatedStatus = if (shouldMoveToDirty) LaundryStatus.DIRTY else item.status
        val updatedItem = item.copy(
            currentWears = updatedCurrent.coerceAtMost(item.maxWears),
            status = updatedStatus,
            lastWornDate = clock()
        )

        val remainingUses = (item.maxWears - updatedItem.currentWears).coerceAtLeast(0)
        return WearOutcome(
            updatedItem = updatedItem,
            movedToDirty = updatedStatus == LaundryStatus.DIRTY,
            remainingUses = remainingUses,
            reason = if (updatedStatus == LaundryStatus.DIRTY) reason else null
        )
    }

    private fun calculateDamage(item: ClothingItem, weather: WeatherSnapshot): DamageResult {
        if (item.isAlwaysWash) {
            return DamageResult(FORCE_DIRTY_DAMAGE, WearReason.FORCE_SETTING)
        }

        val highHeat = weather.maxTemperatureCelsius >= 26.0 || weather.humidityPercent >= 70
        val weatherSensitiveCategory = item.category != ClothingCategory.INNER && item.category != ClothingCategory.T_SHIRT
        if (highHeat && weatherSensitiveCategory) {
            return DamageResult(2, WearReason.HEAT_SWEAT)
        }

        return DamageResult(1, null)
    }

    data class WearOutcome(
        val updatedItem: ClothingItem,
        val movedToDirty: Boolean,
        val remainingUses: Int,
        val reason: WearReason?
    )

    private data class DamageResult(
        val damage: Int,
        val reason: WearReason?
    )

    enum class WearReason {
        FORCE_SETTING,
        HEAT_SWEAT
    }

    companion object {
        private const val FORCE_DIRTY_DAMAGE = 999
    }
}

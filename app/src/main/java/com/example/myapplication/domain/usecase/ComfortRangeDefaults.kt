package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import kotlin.math.max
import kotlin.math.min

/**
 * Provides recommended comfort temperature ranges per garment attributes.
 */
object ComfortRangeDefaults {
    private val DEFAULT_RANGE: Pair<Double, Double> = 10.0 to 30.0
    const val MIN_LIMIT: Double = -5.0
    const val MAX_LIMIT: Double = 40.0

    fun forItem(item: ClothingItem): Pair<Double, Double> =
        forAttributes(item.type, item.thickness, item.sleeveLength)

    fun forAttributes(
        type: ClothingType,
        thickness: Thickness,
        sleeveLength: SleeveLength
    ): Pair<Double, Double> {
        return when (type) {
            ClothingType.TOP, ClothingType.OUTER ->
                adjustRange(resolveUpperBase(thickness), resolveSleeveAdjustment(sleeveLength))
            ClothingType.BOTTOM -> resolveLowerBase(thickness)
            ClothingType.INNER -> resolveInnerBase(thickness)
            else -> DEFAULT_RANGE
        }
    }

    fun fallback(): Pair<Double, Double> = DEFAULT_RANGE

    private fun resolveUpperBase(thickness: Thickness): Pair<Double, Double> = when (thickness) {
        Thickness.THIN -> 18.0 to 33.0
        Thickness.NORMAL -> 12.0 to 28.0
        Thickness.THICK -> 5.0 to 20.0
        Thickness.UNKNOWN -> DEFAULT_RANGE
    }

    private fun resolveLowerBase(thickness: Thickness): Pair<Double, Double> = when (thickness) {
        Thickness.THIN -> 20.0 to 34.0
        Thickness.NORMAL -> 12.0 to 30.0
        Thickness.THICK -> 5.0 to 20.0
        Thickness.UNKNOWN -> DEFAULT_RANGE
    }

    private fun resolveInnerBase(thickness: Thickness): Pair<Double, Double> = when (thickness) {
        Thickness.THIN -> 15.0 to 28.0
        Thickness.NORMAL -> 8.0 to 20.0
        Thickness.THICK -> 0.0 to 15.0
        Thickness.UNKNOWN -> DEFAULT_RANGE
    }

    private fun resolveSleeveAdjustment(sleeveLength: SleeveLength): Pair<Double, Double> = when (sleeveLength) {
        SleeveLength.SHORT -> 2.0 to 2.0
        SleeveLength.NONE -> 0.0 to 3.0
        SleeveLength.LONG -> -2.0 to -1.0
        SleeveLength.UNKNOWN -> 0.0 to 0.0
    }

    private fun adjustRange(
        base: Pair<Double, Double>,
        adjustment: Pair<Double, Double>
    ): Pair<Double, Double> {
        val minAdjusted = base.first + adjustment.first
        val maxAdjusted = base.second + adjustment.second
        return max(minAdjusted, MIN_LIMIT) to min(maxAdjusted, MAX_LIMIT)
    }
}

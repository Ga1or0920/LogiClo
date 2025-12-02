package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import kotlin.math.max
import kotlin.math.min

/**
 * アイテムが現在の気象条件に適しているか簡易判定する。
 * 要件のマトリクスに合わせて袖丈＋厚さから快適気温レンジを推定する。
 */
class WeatherSuitabilityEvaluator {

    fun isSuitable(
        item: ClothingItem,
        minTemperature: Double,
        maxTemperature: Double
    ): Boolean {
        val (comfortableMin, comfortableMax) = when (item.type) {
            ClothingType.TOP, ClothingType.OUTER -> evaluateUpper(item)
            ClothingType.BOTTOM -> evaluateLower(item)
            ClothingType.INNER -> evaluateInner(item)
            else -> DEFAULT_RANGE
        }

        return minTemperature >= comfortableMin - TOLERANCE_LOWER &&
            maxTemperature <= comfortableMax + TOLERANCE_UPPER
    }

    private fun evaluateUpper(item: ClothingItem): Pair<Double, Double> {
        val baseRange = when (item.thickness) {
            Thickness.THIN -> 18.0 to 33.0
            Thickness.NORMAL -> 12.0 to 28.0
            Thickness.THICK -> 5.0 to 20.0
            Thickness.UNKNOWN -> DEFAULT_RANGE
        }
        val sleeveAdjustment = when (item.sleeveLength) {
            SleeveLength.SHORT -> 2.0 to 2.0
            SleeveLength.NONE -> 0.0 to 3.0
            SleeveLength.LONG -> -2.0 to -1.0
            SleeveLength.UNKNOWN -> 0.0 to 0.0
        }
        return adjustRange(baseRange, sleeveAdjustment)
    }

    private fun evaluateLower(item: ClothingItem): Pair<Double, Double> {
        val baseRange = when (item.thickness) {
            Thickness.THIN -> 20.0 to 34.0
            Thickness.NORMAL -> 12.0 to 30.0
            Thickness.THICK -> 5.0 to 20.0
            Thickness.UNKNOWN -> DEFAULT_RANGE
        }
        return baseRange
    }

    private fun evaluateInner(item: ClothingItem): Pair<Double, Double> {
        val baseRange = when (item.thickness) {
            Thickness.THIN -> 15.0 to 28.0
            Thickness.NORMAL -> 8.0 to 20.0
            Thickness.THICK -> 0.0 to 15.0
            Thickness.UNKNOWN -> DEFAULT_RANGE
        }
        return baseRange
    }

    private fun adjustRange(
        base: Pair<Double, Double>,
        adjustment: Pair<Double, Double>
    ): Pair<Double, Double> {
        val minAdjusted = base.first + adjustment.first
        val maxAdjusted = base.second + adjustment.second
        return max(minAdjusted, MIN_LIMIT) to min(maxAdjusted, MAX_LIMIT)
    }

    companion object {
        private val DEFAULT_RANGE = 10.0 to 30.0
        private const val MIN_LIMIT = -5.0
        private const val MAX_LIMIT = 40.0
        private const val TOLERANCE_LOWER = 3.0
        private const val TOLERANCE_UPPER = 3.0
    }
}

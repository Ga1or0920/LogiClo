package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.ClothingItem

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
        val (defaultMin, defaultMax) = ComfortRangeDefaults.forItem(item)
        val comfortableMin = item.comfortMinCelsius ?: defaultMin
        val comfortableMax = item.comfortMaxCelsius ?: defaultMax

        return minTemperature >= comfortableMin - TOLERANCE_LOWER &&
            maxTemperature <= comfortableMax + TOLERANCE_UPPER
    }

    companion object {
        private const val TOLERANCE_LOWER = 3.0
        private const val TOLERANCE_UPPER = 3.0
    }
}

package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.Pattern

/**
 * アイテムのフォーマル度 (F-Score) を算出するユースケース。
 */
class FormalScoreCalculator {

    fun calculate(item: ClothingItem): Int {
        val baseScore = when (item.category) {
            ClothingCategory.SWEATSHIRT,
            ClothingCategory.T_SHIRT,
            ClothingCategory.DENIM -> 3

            ClothingCategory.KNIT,
            ClothingCategory.POLO,
            ClothingCategory.CHINO -> 5

            ClothingCategory.DRESS_SHIRT,
            ClothingCategory.SLACKS,
            ClothingCategory.JACKET -> 8

            else -> 4 // その他カテゴリはバランス程度の点数で初期化
        }

        val patternBonus = when (item.pattern) {
            Pattern.SOLID -> 1
            Pattern.GRAPHIC -> -2
            else -> 0
        }

        val colorBonus = when (item.colorGroup) {
            ColorGroup.MONOTONE, ColorGroup.NAVY_BLUE -> 1
            ColorGroup.VIVID -> -1
            else -> 0
        }

        return (baseScore + patternBonus + colorBonus).coerceIn(0, 10)
    }
}

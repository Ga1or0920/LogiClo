package com.example.myapplication.ui.dashboard

import com.example.myapplication.R
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.ui.common.UiMessage
import com.example.myapplication.ui.common.UiMessageArg
import com.example.myapplication.ui.dashboard.model.OutfitSuggestion
import com.example.myapplication.domain.usecase.FormalScoreCalculator
import com.example.myapplication.domain.usecase.WeatherSuitabilityEvaluator
import com.example.myapplication.ui.dashboard.model.ColorWishPreference
import com.example.myapplication.ui.common.labelResId

class SuggestionEngine(
    private val formalScoreCalculator: FormalScoreCalculator,
    private val weatherSuitabilityEvaluator: WeatherSuitabilityEvaluator,
    private val stringResolver: (Int) -> String
) {

    data class SuggestionResult(
        val suggestions: List<OutfitSuggestion>,
        val recommendations: List<UiMessage>
    )

    fun buildSuggestions(
        mode: TpoMode,
        items: List<ClothingItem>,
        disallowVividPair: Boolean,
        allowBlackNavy: Boolean,
        temperatureMin: Double,
        temperatureMax: Double,
        colorWish: ColorWishPreference?
    ): SuggestionResult {
        val recommendations = mutableListOf<UiMessage>()

        if (items.isEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_missing_top_inventory, mode, ClothingType.TOP)
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_missing_bottom_inventory, mode, ClothingType.BOTTOM)
            return SuggestionResult(emptyList(), recommendations.distinct())
        }

        val topsInCloset = items.filter { it.type == ClothingType.TOP }
        val bottomsInCloset = items.filter { it.type == ClothingType.BOTTOM }
        val outersInCloset = items.filter { it.type == ClothingType.OUTER }

        if (topsInCloset.isEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_missing_top_inventory, mode, ClothingType.TOP)
        }
        if (bottomsInCloset.isEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_missing_bottom_inventory, mode, ClothingType.BOTTOM)
        }

        val tops = topsInCloset.filter { item ->
            weatherSuitabilityEvaluator.isSuitable(
                item = item,
                minTemperature = temperatureMin,
                maxTemperature = temperatureMax
            )
        }
        if (tops.isEmpty() && topsInCloset.isNotEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_weather_top, mode, ClothingType.TOP)
        }

        val bottoms = bottomsInCloset.filter { item ->
            weatherSuitabilityEvaluator.isSuitable(
                item = item,
                minTemperature = temperatureMin,
                maxTemperature = temperatureMax
            )
        }
        if (bottoms.isEmpty() && bottomsInCloset.isNotEmpty()) {
            recommendations += buildRecommendationMessage(R.string.dashboard_recommendation_weather_bottom, mode, ClothingType.BOTTOM)
        }

        var candidateTops = if (tops.isNotEmpty()) tops else topsInCloset
        var candidateBottoms = if (bottoms.isNotEmpty()) bottoms else bottomsInCloset
        var outerCandidates = resolveOuterCandidates(
            outers = outersInCloset,
            temperatureMin = temperatureMin,
            temperatureMax = temperatureMax
        )

        var colorWishApplied = false
        if (colorWish != null && colorWish.type in listOf(ClothingType.OUTER, ClothingType.TOP, ClothingType.BOTTOM)) {
            colorWishApplied = true
            when (colorWish.type) {
                ClothingType.TOP -> {
                    candidateTops = candidateTops.filter { matchesColor(it, colorWish.colorHex) }
                    if (candidateTops.isEmpty()) {
                        recommendations += buildColorWishMissingMessage(colorWish)
                        return SuggestionResult(emptyList(), recommendations.distinct())
                    }
                }

                ClothingType.BOTTOM -> {
                    candidateBottoms = candidateBottoms.filter { matchesColor(it, colorWish.colorHex) }
                    if (candidateBottoms.isEmpty()) {
                        recommendations += buildColorWishMissingMessage(colorWish)
                        return SuggestionResult(emptyList(), recommendations.distinct())
                    }
                }

                ClothingType.OUTER -> {
                    outerCandidates = outerCandidates.filter { matchesColor(it, colorWish.colorHex) }
                    if (outerCandidates.isEmpty()) {
                        recommendations += buildColorWishMissingMessage(colorWish)
                        return SuggestionResult(emptyList(), recommendations.distinct())
                    }
                }

                else -> Unit
            }
        }

        if (candidateTops.isEmpty() || candidateBottoms.isEmpty()) {
            if (recommendations.isEmpty()) {
                val fallbackType = if (candidateTops.isEmpty()) ClothingType.TOP else ClothingType.BOTTOM
                recommendations += buildGenericRecommendation(mode, fallbackType)
            }
            return SuggestionResult(emptyList(), recommendations.distinct())
        }

        val scoreRange = resolveScoreRange(mode)

        val suggestions = mutableListOf<OutfitSuggestion>()
        var filteredByColor = false
        var filteredByScore = false
        for (top in candidateTops) {
            val topScore = formalScoreCalculator.calculate(top)
            for (bottom in candidateBottoms) {
                val bottomScore = formalScoreCalculator.calculate(bottom)
                val totalScore = topScore + bottomScore
                if (totalScore !in scoreRange) {
                    filteredByScore = true
                    continue
                }
                if (!isColorCompatible(top, bottom, disallowVividPair, allowBlackNavy)) {
                    filteredByColor = true
                    continue
                }
                val outer = pickOuterCandidate(outerCandidates, suggestions.size)
                suggestions += OutfitSuggestion(
                    top = top,
                    bottom = bottom,
                    outer = outer,
                    totalScore = totalScore
                )
            }
        }

        val sortedSuggestions = suggestions
            .sortedWith(
                compareByDescending<OutfitSuggestion> { it.totalScore }
                    .thenBy { it.top.name }
                    .thenBy { it.bottom.name }
                    .thenBy { it.outer?.name ?: "" }
            )
            .take(20)

        if (sortedSuggestions.isNotEmpty()) {
            return SuggestionResult(sortedSuggestions, recommendations.distinct())
        }

        val relaxedSuggestions = mutableListOf<OutfitSuggestion>()
        for (top in candidateTops) {
            val topScore = formalScoreCalculator.calculate(top)
            for (bottom in candidateBottoms) {
                val bottomScore = formalScoreCalculator.calculate(bottom)
                if (!isColorCompatible(top, bottom, disallowVividPair, allowBlackNavy)) continue
                val outer = pickOuterCandidate(outerCandidates, relaxedSuggestions.size)
                relaxedSuggestions += OutfitSuggestion(
                    top = top,
                    bottom = bottom,
                    outer = outer,
                    totalScore = topScore + bottomScore
                )
            }
        }

        if (relaxedSuggestions.isNotEmpty()) {
            if (filteredByScore) {
                recommendations += buildGenericRecommendation(mode, if (candidateTops.size <= candidateBottoms.size) ClothingType.TOP else ClothingType.BOTTOM)
            }
            return SuggestionResult(
                suggestions = relaxedSuggestions
                    .sortedWith(
                        compareByDescending<OutfitSuggestion> { it.totalScore }
                            .thenBy { it.top.name }
                            .thenBy { it.bottom.name }
                            .thenBy { it.outer?.name ?: "" }
                    )
                    .take(20),
                recommendations = recommendations.distinct()
            )
        }

        val fallbackType = if (candidateTops.size <= candidateBottoms.size) ClothingType.TOP else ClothingType.BOTTOM
        if (filteredByColor) {
            recommendations += buildGenericRecommendation(mode, fallbackType)
        }
        if (colorWishApplied && colorWish != null) {
            recommendations += UiMessage(
                resId = R.string.dashboard_color_wish_no_match,
                args = listOf(
                    UiMessageArg.Raw(stringResolver(colorWish.type.labelResId())),
                    UiMessageArg.Raw(colorWish.colorLabel)
                )
            )
        }
        if (recommendations.isEmpty()) {
            recommendations += buildGenericRecommendation(mode, ClothingType.TOP)
            recommendations += buildGenericRecommendation(mode, ClothingType.BOTTOM)
        }

        return SuggestionResult(emptyList(), recommendations.distinct())
    }

    fun resolveScoreRange(mode: TpoMode): IntRange = when (mode) {
        TpoMode.CASUAL -> 5..12
        TpoMode.OFFICE -> 13..18
        else -> 5..15
    }

    fun formatScoreRange(range: IntRange): String = "${range.first}~${range.last}"

    private fun buildRecommendationMessage(
        resId: Int,
        mode: TpoMode,
        type: ClothingType
    ): UiMessage {
        val categories = recommendedCategoriesFor(mode, type)
        val label = formatCategoryList(categories)
        return UiMessage(
            resId = resId,
            args = listOf(UiMessageArg.Raw(label))
        )
    }

    private fun buildGenericRecommendation(mode: TpoMode, type: ClothingType): UiMessage {
        val resId = if (type == ClothingType.BOTTOM) {
            R.string.dashboard_recommendation_generic_bottom
        } else {
            R.string.dashboard_recommendation_generic_top
        }
        return buildRecommendationMessage(resId, mode, type)
    }

    private fun recommendedCategoriesFor(mode: TpoMode, type: ClothingType): List<ClothingCategory> = when (type) {
        ClothingType.TOP -> when (mode) {
            TpoMode.OFFICE -> listOf(ClothingCategory.DRESS_SHIRT, ClothingCategory.KNIT)
            TpoMode.CASUAL -> listOf(ClothingCategory.T_SHIRT, ClothingCategory.POLO, ClothingCategory.KNIT)
            else -> listOf(ClothingCategory.T_SHIRT, ClothingCategory.KNIT)
        }
        ClothingType.BOTTOM -> when (mode) {
            TpoMode.OFFICE -> listOf(ClothingCategory.SLACKS, ClothingCategory.CHINO)
            TpoMode.CASUAL -> listOf(ClothingCategory.CHINO, ClothingCategory.DENIM)
            else -> listOf(ClothingCategory.CHINO)
        }
        else -> listOf(ClothingCategory.OUTER_LIGHT)
    }

    private fun formatCategoryList(categories: List<ClothingCategory>): String {
        val distinct = categories.distinct()
        if (distinct.isEmpty()) {
            return stringResolver(ClothingCategory.T_SHIRT.labelResId())
        }
        return distinct.joinToString(separator = " / ") { category ->
            stringResolver(category.labelResId())
        }
    }

    private fun resolveOuterCandidates(
        outers: List<ClothingItem>,
        temperatureMin: Double,
        temperatureMax: Double
    ): List<ClothingItem> {
        if (outers.isEmpty()) return emptyList()
        val suitable = outers.filter { outer ->
            weatherSuitabilityEvaluator.isSuitable(
                item = outer,
                minTemperature = temperatureMin,
                maxTemperature = temperatureMax
            )
        }
        val pool = if (suitable.isNotEmpty()) suitable else outers
        return pool.sortedWith(
            compareByDescending<ClothingItem> { formalScoreCalculator.calculate(it) }
                .thenBy { it.name }
        )
    }

    private fun pickOuterCandidate(
        candidates: List<ClothingItem>,
        position: Int
    ): ClothingItem? {
        if (candidates.isEmpty()) return null
        val index = position % candidates.size
        return candidates[index]
    }

    private fun isColorCompatible(
        top: ClothingItem,
        bottom: ClothingItem,
        disallowVividPair: Boolean,
        allowBlackNavy: Boolean
    ): Boolean {
        if (disallowVividPair && top.colorGroup == com.example.myapplication.domain.model.ColorGroup.VIVID && bottom.colorGroup == com.example.myapplication.domain.model.ColorGroup.VIVID) {
            return false
        }
        if (!allowBlackNavy) {
            val navyWithMonotone = (top.colorGroup == com.example.myapplication.domain.model.ColorGroup.NAVY_BLUE && bottom.colorGroup == com.example.myapplication.domain.model.ColorGroup.MONOTONE) ||
                (top.colorGroup == com.example.myapplication.domain.model.ColorGroup.MONOTONE && bottom.colorGroup == com.example.myapplication.domain.model.ColorGroup.NAVY_BLUE)
            if (navyWithMonotone) return false
        }
        return true
    }

    private fun buildColorWishMissingMessage(preference: ColorWishPreference): UiMessage {
        val typeLabel = stringResolver(preference.type.labelResId())
        return UiMessage(
            resId = R.string.dashboard_color_wish_recommendation_missing,
            args = listOf(
                UiMessageArg.Raw(typeLabel),
                UiMessageArg.Raw(preference.colorLabel)
            )
        )
    }

    private fun normalizeColorHex(raw: String): String {
        val trimmed = raw.trim()
        val withoutHash = trimmed.removePrefix("#")
        return "#" + withoutHash.uppercase(java.util.Locale.ROOT)
    }

    private fun matchesColor(item: ClothingItem, targetHex: String): Boolean {
        return normalizeColorHex(item.colorHex) == targetHex
    }
}

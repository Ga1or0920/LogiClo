package com.example.myapplication.ui.dashboard

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.R
import com.example.myapplication.ui.common.UiMessage
import com.example.myapplication.ui.common.UiMessageArg
import com.example.myapplication.ui.common.labelResId
import com.example.myapplication.ui.dashboard.model.*
import java.util.Locale

data class BuildColorWishResult(
    val uiState: ColorWishUiState,
    val updatedPreference: ColorWishPreference?
)

object ColorWishHelper {
    private val SUPPORTED_TYPES = listOf(ClothingType.OUTER, ClothingType.TOP, ClothingType.BOTTOM)

    fun buildColorWishUiState(
        closetItems: List<ClothingItem>,
        currentPreference: ColorWishPreference?,
        dialogState: ColorWishDialogState,
        currentPreferenceValue: ColorWishPreference?,
        stringResolver: (Int) -> String
    ): BuildColorWishResult {
        val groupedByType = closetItems.filter { it.type in SUPPORTED_TYPES }.groupBy { it.type }

        val typeOptions = SUPPORTED_TYPES.mapNotNull { type ->
            val items = groupedByType[type].orEmpty()
            if (items.isEmpty()) null else ColorWishTypeOption(type = type, label = stringResolver(type.labelResId()))
        }

        var resolvedPreference = currentPreference
        val preferenceMatch = resolvedPreference?.let { pref -> groupedByType[pref.type]?.firstOrNull { normalizeColorHex(it.colorHex) == pref.colorHex } }
        var updatedPref: ColorWishPreference? = resolvedPreference
        if (resolvedPreference != null && preferenceMatch == null) {
            if (currentPreferenceValue != null) updatedPref = null
            resolvedPreference = null
        } else if (resolvedPreference != null && preferenceMatch != null) {
            val normalizedHex = normalizeColorHex(preferenceMatch.colorHex)
            val refreshedLabel = resolveColorLabel(preferenceMatch, normalizedHex, stringResolver)
            if (resolvedPreference.colorHex != normalizedHex || resolvedPreference.colorLabel != refreshedLabel) {
                val updated = resolvedPreference.copy(colorHex = normalizedHex, colorLabel = refreshedLabel)
                if (currentPreferenceValue != updated) updatedPref = updated
                resolvedPreference = updated
            }
        }

        val resolvedType = listOfNotNull(dialogState.selectedType, resolvedPreference?.type, typeOptions.firstOrNull()?.type)
            .firstOrNull { type -> typeOptions.any { it.type == type } }

        val colorOptions = resolvedType?.let { type ->
            groupedByType[type]
                ?.groupBy { normalizeColorHex(it.colorHex) }
                ?.map { (hex, items) -> ColorWishColorOption(colorHex = hex, label = resolveColorLabel(items.first(), hex, stringResolver)) }
                ?.sortedBy { it.label }
                ?: emptyList()
        } ?: emptyList()

        val resolvedColor = listOfNotNull(dialogState.selectedColorHex?.let(::normalizeColorHex), resolvedPreference?.takeIf { it.type == resolvedType }?.colorHex, colorOptions.firstOrNull()?.colorHex)
            .firstOrNull { candidate -> colorOptions.any { it.colorHex == candidate } }

        val normalizedDialog = if (dialogState.isVisible) dialogState.copy(selectedType = resolvedType, selectedColorHex = resolvedColor) else dialogState

        val activePreferenceUi = resolvedPreference?.let { pref ->
            ColorWishPreferenceUi(type = pref.type, colorHex = pref.colorHex, typeLabel = stringResolver(pref.type.labelResId()), colorLabel = pref.colorLabel)
        }

        val emptyMessage = when {
            typeOptions.isEmpty() -> stringResolver(R.string.dashboard_color_wish_unavailable)
            resolvedType != null && colorOptions.isEmpty() -> stringResolver(R.string.dashboard_color_wish_no_colors_for_type)
            else -> null
        }

        val uiState = ColorWishUiState(
            isFeatureAvailable = typeOptions.isNotEmpty(),
            activePreference = activePreferenceUi,
            isDialogVisible = normalizedDialog.isVisible,
            typeOptions = typeOptions,
            selectedType = normalizedDialog.selectedType,
            colorOptions = colorOptions,
            selectedColorHex = normalizedDialog.selectedColorHex,
            isConfirmEnabled = normalizedDialog.selectedType != null && normalizedDialog.selectedColorHex != null,
            emptyStateMessage = emptyMessage
        )

        return BuildColorWishResult(uiState = uiState, updatedPreference = updatedPref)
    }

    fun resolveColorLabel(item: ClothingItem, normalizedHex: String, stringResolver: (Int) -> String): String {
        val groupLabel = item.colorGroup.takeUnless { it == ColorGroup.UNKNOWN }?.let { group -> stringResolver(group.labelResId()) }
        return when {
            groupLabel.isNullOrBlank() -> normalizedHex
            groupLabel.equals(normalizedHex, ignoreCase = true) -> normalizedHex
            else -> "$groupLabel ($normalizedHex)"
        }
    }

    fun normalizeColorHex(raw: String): String {
        val trimmed = raw.trim()
        val withoutHash = trimmed.removePrefix("#")
        return "#" + withoutHash.uppercase(Locale.ROOT)
    }

    fun matchesColor(item: ClothingItem, targetHex: String): Boolean {
        return normalizeColorHex(item.colorHex) == targetHex
    }

    fun buildColorWishMissingMessage(preference: ColorWishPreference, stringResolver: (Int) -> String): UiMessage {
        val typeLabel = stringResolver(preference.type.labelResId())
        return UiMessage(resId = R.string.dashboard_color_wish_recommendation_missing, args = listOf(UiMessageArg.Raw(typeLabel), UiMessageArg.Raw(preference.colorLabel)))
    }
}

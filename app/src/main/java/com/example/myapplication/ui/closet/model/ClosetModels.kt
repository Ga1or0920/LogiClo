package com.example.myapplication.ui.closet.model

import androidx.annotation.StringRes
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.Pattern
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import com.example.myapplication.domain.usecase.ComfortRangeDefaults

private val DEFAULT_COMFORT_RANGE = ComfortRangeDefaults.fallback()

data class ClosetItemUi(
    val id: String,
    val name: String,
    val brand: String? = null,
    val category: ClothingCategory,
    @param:StringRes val categoryLabelResId: Int,
    val colorHex: String,
    val status: LaundryStatus,
    val currentWears: Int,
    val maxWears: Int,
    val isAlwaysWash: Boolean,
    val cleaningType: CleaningType
)

data class ClosetFilters(
    val category: ClothingCategory? = null,
    val type: ClothingType? = null,
    val colorGroup: ColorGroup? = null
) {
    val isActive: Boolean
        get() = category != null || type != null || colorGroup != null

    val activeCount: Int
        get() = listOfNotNull(category, type, colorGroup).size
}

data class ClosetUiState(
    val isLoading: Boolean = true,
    val filter: LaundryStatus = LaundryStatus.CLOSET,
    val items: List<ClosetItemUi> = emptyList(),
    val statusCounts: Map<LaundryStatus, Int> = emptyMap(),
    val filters: ClosetFilters = ClosetFilters(),
    val availableCategories: List<ClothingCategory> = emptyList(),
    val availableTypes: List<ClothingType> = emptyList(),
    val availableColorGroups: List<ColorGroup> = emptyList(),
    val isFilterDialogVisible: Boolean = false
) {
    val activeFilterCount: Int
        get() = filters.activeCount

    val hasActiveFilters: Boolean
        get() = filters.isActive
}

data class ClosetEditorUiState(
    val name: String = "",
    val brand: String = "",
    val selectedCategory: CategoryOption? = null,
    val selectedColor: ColorOption? = null,
    val isAlwaysWash: Boolean = false,
    val maxWears: Int = 3,
    val comfortMinCelsius: Double = DEFAULT_COMFORT_RANGE.first,
    val comfortMaxCelsius: Double = DEFAULT_COMFORT_RANGE.second,
    val isComfortRangeCustomized: Boolean = false,
    val cleaningType: CleaningType = CleaningType.HOME,
    val type: ClothingType = ClothingType.TOP,
    val sleeveLength: SleeveLength = SleeveLength.UNKNOWN,
    val thickness: Thickness = Thickness.UNKNOWN,
    val pattern: Pattern = Pattern.SOLID,
    val status: LaundryStatus = LaundryStatus.CLOSET,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val availableCategories: List<CategoryOption> = emptyList(),
    val availableColors: List<ColorOption> = emptyList(),
    val isEditMode: Boolean = false
) {
    val canSave: Boolean
        get() = name.isNotBlank() && selectedCategory != null && selectedColor != null && !isSaving
    val showMaxWearSlider: Boolean
        get() = !isAlwaysWash
}

data class CategoryOption(
    val category: ClothingCategory,
    @param:StringRes val labelResId: Int,
    val type: ClothingType,
    val defaultSleeve: SleeveLength,
    val defaultThickness: Thickness,
    val defaultCleaning: CleaningType,
    val defaultMaxWears: Int,
    val defaultAlwaysWash: Boolean
)

data class ColorOption(
    val colorHex: String,
    @param:StringRes val labelResId: Int,
    val group: ColorGroup
)

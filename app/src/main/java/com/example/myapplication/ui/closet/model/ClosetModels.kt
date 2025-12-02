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

data class ClosetItemUi(
    val id: String,
    val name: String,
    val category: ClothingCategory,
    @StringRes val categoryLabelResId: Int,
    val colorHex: String,
    val status: LaundryStatus,
    val currentWears: Int,
    val maxWears: Int,
    val isAlwaysWash: Boolean,
    val cleaningType: CleaningType
)

data class ClosetUiState(
    val isLoading: Boolean = true,
    val filter: LaundryStatus = LaundryStatus.CLOSET,
    val items: List<ClosetItemUi> = emptyList(),
    val statusCounts: Map<LaundryStatus, Int> = emptyMap()
)

data class ClosetEditorUiState(
    val name: String = "",
    val selectedCategory: CategoryOption? = null,
    val selectedColor: ColorOption? = null,
    val isAlwaysWash: Boolean = false,
    val maxWears: Int = 3,
    val cleaningType: CleaningType = CleaningType.HOME,
    val type: ClothingType = ClothingType.TOP,
    val sleeveLength: SleeveLength = SleeveLength.UNKNOWN,
    val thickness: Thickness = Thickness.NORMAL,
    val pattern: Pattern = Pattern.SOLID,
    val status: LaundryStatus = LaundryStatus.CLOSET,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val availableCategories: List<CategoryOption> = emptyList(),
    val availableColors: List<ColorOption> = emptyList()
) {
    val canSave: Boolean
        get() = name.isNotBlank() && selectedCategory != null && selectedColor != null && !isSaving
    val showMaxWearSlider: Boolean
        get() = !isAlwaysWash
}

data class CategoryOption(
    val category: ClothingCategory,
    @StringRes val labelResId: Int,
    val type: ClothingType,
    val defaultSleeve: SleeveLength,
    val defaultThickness: Thickness,
    val defaultCleaning: CleaningType,
    val defaultMaxWears: Int,
    val defaultAlwaysWash: Boolean
)

data class ColorOption(
    val colorHex: String,
    @StringRes val labelResId: Int,
    val group: ColorGroup
)

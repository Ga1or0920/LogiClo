package com.example.myapplication.ui.closet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.Pattern
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import com.example.myapplication.ui.closet.model.CategoryOption
import com.example.myapplication.ui.closet.model.ClosetEditorUiState
import com.example.myapplication.ui.closet.model.ColorOption
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val MIN_WEAR_COUNT = 2
private const val MAX_WEAR_COUNT = 30

class ClosetEditorViewModel(
    private val closetRepository: ClosetRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val categoryOptions = closetCategoryOptions()
    private val colorOptions = closetColorOptions()
    private var learnedDefaultMaxWears: Map<ClothingCategory, Int> = emptyMap()

    private val _uiState = MutableStateFlow(
        ClosetEditorUiState(
            availableCategories = categoryOptions,
            availableColors = colorOptions
        )
    )
    val uiState: StateFlow<ClosetEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.observe().collect { preferences ->
                learnedDefaultMaxWears = preferences.defaultMaxWears
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { state ->
            state.copy(name = value)
        }
    }

    fun onCategorySelected(option: CategoryOption) {
        _uiState.update { state ->
            val initialAlwaysWash = resolveInitialAlwaysWash(option)
            val initialMaxWears = if (initialAlwaysWash) 1 else resolveBaseMaxWears(option)
            state.copy(
                selectedCategory = option,
                type = option.type,
                sleeveLength = option.defaultSleeve,
                thickness = option.defaultThickness,
                cleaningType = option.defaultCleaning,
                isAlwaysWash = initialAlwaysWash,
                maxWears = initialMaxWears
            )
        }
    }

    fun onColorSelected(option: ColorOption) {
        _uiState.update { state ->
            state.copy(selectedColor = option)
        }
    }

    fun onAlwaysWashChanged(isAlwaysWash: Boolean) {
        _uiState.update { state ->
            state.copy(
                isAlwaysWash = isAlwaysWash,
                maxWears = if (isAlwaysWash) 1 else resolveBaseMaxWears(state.selectedCategory)
            )
        }
    }

    fun onMaxWearsChanged(value: Int) {
        val clamped = value.coerceIn(MIN_WEAR_COUNT, MAX_WEAR_COUNT)
        _uiState.update { state ->
            state.copy(maxWears = clamped)
        }
    }

    fun onCleaningTypeChanged(type: CleaningType) {
        _uiState.update { state ->
            state.copy(cleaningType = type)
        }
    }

    fun onSleeveLengthSelected(length: SleeveLength) {
        _uiState.update { state ->
            state.copy(sleeveLength = length)
        }
    }

    fun onThicknessSelected(thickness: Thickness) {
        _uiState.update { state ->
            state.copy(thickness = thickness)
        }
    }

    fun onSave() {
        val current = _uiState.value
        if (!current.canSave || current.isSaving) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val item = buildClothingItem(current)
            closetRepository.upsert(item)
            userPreferencesRepository.update { preferences ->
                val updatedDefaults = preferences.defaultMaxWears.toMutableMap()
                updatedDefaults[item.category] = if (item.isAlwaysWash) 1 else item.maxWears
                preferences.copy(defaultMaxWears = updatedDefaults)
            }
            _uiState.update { state ->
                state.copy(isSaving = false, saveCompleted = true)
            }
        }
    }

    fun onSaveHandled() {
        _uiState.update { state ->
            if (state.saveCompleted) state.copy(saveCompleted = false) else state
        }
    }

    private fun resolveInitialAlwaysWash(option: CategoryOption): Boolean {
        val learned = learnedDefaultMaxWears[option.category]
        return option.defaultAlwaysWash || (learned ?: option.defaultMaxWears) <= 1
    }

    private fun resolveBaseMaxWears(option: CategoryOption?): Int {
        if (option == null) return MIN_WEAR_COUNT
        val learned = learnedDefaultMaxWears[option.category]
        val fallback = option.defaultMaxWears
        val target = learned ?: fallback
        return target.coerceIn(MIN_WEAR_COUNT, MAX_WEAR_COUNT)
    }

    private fun buildClothingItem(state: ClosetEditorUiState): ClothingItem {
        val categoryOption = state.selectedCategory
            ?: error("Category must be selected before saving")
        val colorOption = state.selectedColor
            ?: error("Color must be selected before saving")

        val maxWear = if (state.isAlwaysWash) 1 else state.maxWears

        return ClothingItem(
            id = UUID.randomUUID().toString(),
            name = state.name.trim(),
            category = categoryOption.category,
            type = state.type,
            sleeveLength = state.sleeveLength,
            thickness = state.thickness,
            colorHex = colorOption.colorHex,
            colorGroup = colorOption.group,
            pattern = state.pattern,
            maxWears = maxWear,
            currentWears = 0,
            isAlwaysWash = state.isAlwaysWash,
            cleaningType = state.cleaningType,
            status = LaundryStatus.CLOSET,
            imageUrl = null,
            lastWornDate = null
        )
    }

    class Factory(
        private val closetRepository: ClosetRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClosetEditorViewModel::class.java)) {
                return ClosetEditorViewModel(
                    closetRepository = closetRepository,
                    userPreferencesRepository = userPreferencesRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

internal fun closetCategoryOptions(): List<CategoryOption> = listOf(
    CategoryOption(
        category = ClothingCategory.T_SHIRT,
        labelResId = R.string.clothing_category_t_shirt,
        type = ClothingType.TOP,
        defaultSleeve = SleeveLength.SHORT,
        defaultThickness = Thickness.THIN,
        defaultCleaning = CleaningType.HOME,
        defaultMaxWears = 1,
        defaultAlwaysWash = true
    ),
    CategoryOption(
        category = ClothingCategory.POLO,
        labelResId = R.string.clothing_category_polo,
        type = ClothingType.TOP,
        defaultSleeve = SleeveLength.SHORT,
        defaultThickness = Thickness.THIN,
        defaultCleaning = CleaningType.HOME,
        defaultMaxWears = 2,
        defaultAlwaysWash = false
    ),
    CategoryOption(
        category = ClothingCategory.DRESS_SHIRT,
        labelResId = R.string.clothing_category_dress_shirt,
        type = ClothingType.TOP,
        defaultSleeve = SleeveLength.LONG,
        defaultThickness = Thickness.THIN,
        defaultCleaning = CleaningType.HOME,
        defaultMaxWears = 1,
        defaultAlwaysWash = true
    ),
    CategoryOption(
        category = ClothingCategory.KNIT,
        labelResId = R.string.clothing_category_knit,
        type = ClothingType.TOP,
        defaultSleeve = SleeveLength.LONG,
        defaultThickness = Thickness.NORMAL,
        defaultCleaning = CleaningType.HOME,
        defaultMaxWears = 5,
        defaultAlwaysWash = false
    ),
    CategoryOption(
        category = ClothingCategory.SWEATSHIRT,
        labelResId = R.string.clothing_category_sweatshirt,
        type = ClothingType.TOP,
        defaultSleeve = SleeveLength.LONG,
        defaultThickness = Thickness.THICK,
        defaultCleaning = CleaningType.HOME,
        defaultMaxWears = 3,
        defaultAlwaysWash = false
    ),
    CategoryOption(
        category = ClothingCategory.DENIM,
        labelResId = R.string.clothing_category_denim,
        type = ClothingType.BOTTOM,
        defaultSleeve = SleeveLength.NONE,
        defaultThickness = Thickness.THICK,
        defaultCleaning = CleaningType.HOME,
        defaultMaxWears = 10,
        defaultAlwaysWash = false
    ),
    CategoryOption(
        category = ClothingCategory.SLACKS,
        labelResId = R.string.clothing_category_slacks,
        type = ClothingType.BOTTOM,
        defaultSleeve = SleeveLength.NONE,
        defaultThickness = Thickness.NORMAL,
        defaultCleaning = CleaningType.HOME,
        defaultMaxWears = 4,
        defaultAlwaysWash = false
    ),
    CategoryOption(
        category = ClothingCategory.CHINO,
        labelResId = R.string.clothing_category_chino,
        type = ClothingType.BOTTOM,
        defaultSleeve = SleeveLength.NONE,
        defaultThickness = Thickness.NORMAL,
        defaultCleaning = CleaningType.HOME,
        defaultMaxWears = 6,
        defaultAlwaysWash = false
    ),
    CategoryOption(
        category = ClothingCategory.OUTER_LIGHT,
        labelResId = R.string.clothing_category_outer_light,
        type = ClothingType.OUTER,
        defaultSleeve = SleeveLength.LONG,
        defaultThickness = Thickness.NORMAL,
        defaultCleaning = CleaningType.DRY,
        defaultMaxWears = 8,
        defaultAlwaysWash = false
    ),
    CategoryOption(
        category = ClothingCategory.COAT,
        labelResId = R.string.clothing_category_coat,
        type = ClothingType.OUTER,
        defaultSleeve = SleeveLength.LONG,
        defaultThickness = Thickness.THICK,
        defaultCleaning = CleaningType.DRY,
        defaultMaxWears = 6,
        defaultAlwaysWash = false
    ),
    CategoryOption(
        category = ClothingCategory.JACKET,
        labelResId = R.string.clothing_category_jacket,
        type = ClothingType.OUTER,
        defaultSleeve = SleeveLength.LONG,
        defaultThickness = Thickness.THICK,
        defaultCleaning = CleaningType.DRY,
        defaultMaxWears = 5,
        defaultAlwaysWash = false
    )
)

internal fun closetColorOptions(): List<ColorOption> = listOf(
    ColorOption(colorHex = "#000000", labelResId = R.string.closet_color_black, group = ColorGroup.MONOTONE),
    ColorOption(colorHex = "#1F2933", labelResId = R.string.closet_color_charcoal, group = ColorGroup.MONOTONE),
    ColorOption(colorHex = "#FFFFFF", labelResId = R.string.closet_color_white, group = ColorGroup.MONOTONE),
    ColorOption(colorHex = "#0D3B66", labelResId = R.string.closet_color_navy, group = ColorGroup.NAVY_BLUE),
    ColorOption(colorHex = "#1C3F95", labelResId = R.string.closet_color_blue, group = ColorGroup.NAVY_BLUE),
    ColorOption(colorHex = "#D2B48C", labelResId = R.string.closet_color_beige, group = ColorGroup.EARTH_TONE),
    ColorOption(colorHex = "#8B5E3C", labelResId = R.string.closet_color_brown, group = ColorGroup.EARTH_TONE),
    ColorOption(colorHex = "#556B2F", labelResId = R.string.closet_color_olive, group = ColorGroup.EARTH_TONE),
    ColorOption(colorHex = "#B22222", labelResId = R.string.closet_color_red, group = ColorGroup.VIVID),
    ColorOption(colorHex = "#FFA500", labelResId = R.string.closet_color_orange, group = ColorGroup.VIVID),
    ColorOption(colorHex = "#32CD32", labelResId = R.string.closet_color_lime, group = ColorGroup.VIVID),
    ColorOption(colorHex = "#87CEEB", labelResId = R.string.closet_color_sky, group = ColorGroup.PASTEL),
    ColorOption(colorHex = "#E6E6FA", labelResId = R.string.closet_color_lavender, group = ColorGroup.PASTEL),
    ColorOption(colorHex = "#808080", labelResId = R.string.closet_color_gray, group = ColorGroup.MONOTONE)
)

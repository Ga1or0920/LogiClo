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
import com.example.myapplication.domain.usecase.ComfortRangeDefaults
import com.example.myapplication.ui.closet.model.CategoryOption
import com.example.myapplication.ui.closet.model.ClosetEditorUiState
import com.example.myapplication.ui.closet.model.ColorOption
import com.example.myapplication.ui.common.labelResId
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlin.ranges.ClosedFloatingPointRange
import kotlin.math.abs

private const val MIN_WEAR_COUNT = 2
private const val MAX_WEAR_COUNT = 30

class ClosetEditorViewModel(
    private val closetRepository: ClosetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val existingItemId: String?
) : ViewModel() {

    private val categoryOptions = closetCategoryOptions()
    private val colorOptions = closetColorOptions()
    private var learnedDefaultMaxWears: Map<ClothingCategory, Int> = emptyMap()
    private var editingItem: ClothingItem? = null

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
        existingItemId?.let { id ->
            viewModelScope.launch {
                loadExistingItem(id)
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { state ->
            state.copy(name = value)
        }
    }

    fun onBrandChanged(value: String) {
        _uiState.update { state ->
            state.copy(brand = value)
        }
    }

    fun onCategorySelected(option: CategoryOption) {
        _uiState.update { state ->
            val initialAlwaysWash = resolveInitialAlwaysWash(option)
            val initialMaxWears = if (initialAlwaysWash) 1 else resolveBaseMaxWears(option)
            val recommendedRange = ComfortRangeDefaults.forAttributes(
                type = option.type,
                thickness = option.defaultThickness,
                sleeveLength = option.defaultSleeve
            )
            state.copy(
                selectedCategory = option,
                type = option.type,
                sleeveLength = option.defaultSleeve,
                thickness = option.defaultThickness,
                cleaningType = option.defaultCleaning,
                isAlwaysWash = initialAlwaysWash,
                maxWears = initialMaxWears,
                comfortMinCelsius = recommendedRange.first,
                comfortMaxCelsius = recommendedRange.second,
                isComfortRangeCustomized = false
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

    fun onComfortRangeChanged(range: ClosedFloatingPointRange<Float>) {
        val minValue = normalizeComfortValue(range.start)
        val maxValue = normalizeComfortValue(range.endInclusive)
        val resolvedMin = min(minValue, maxValue)
        val resolvedMax = max(minValue, maxValue)
        _uiState.update { state ->
            state.copy(
                comfortMinCelsius = resolvedMin,
                comfortMaxCelsius = resolvedMax,
                isComfortRangeCustomized = true
            )
        }
    }

    fun onComfortRangeReset() {
        _uiState.update { state ->
            val recommended = ComfortRangeDefaults.forAttributes(
                type = state.type,
                thickness = state.thickness,
                sleeveLength = state.sleeveLength
            )
            state.copy(
                comfortMinCelsius = recommended.first,
                comfortMaxCelsius = recommended.second,
                isComfortRangeCustomized = false
            )
        }
    }

    fun onCleaningTypeChanged(type: CleaningType) {
        _uiState.update { state ->
            state.copy(cleaningType = type)
        }
    }

    fun onSleeveLengthSelected(length: SleeveLength) {
        _uiState.update { state ->
            val recommended = if (state.isComfortRangeCustomized) null else ComfortRangeDefaults.forAttributes(
                type = state.type,
                thickness = state.thickness,
                sleeveLength = length
            )
            state.copy(
                sleeveLength = length,
                comfortMinCelsius = recommended?.first ?: state.comfortMinCelsius,
                comfortMaxCelsius = recommended?.second ?: state.comfortMaxCelsius
            )
        }
    }

    fun onThicknessSelected(thickness: Thickness) {
        _uiState.update { state ->
            val recommended = if (state.isComfortRangeCustomized) null else ComfortRangeDefaults.forAttributes(
                type = state.type,
                thickness = thickness,
                sleeveLength = state.sleeveLength
            )
            state.copy(
                thickness = thickness,
                comfortMinCelsius = recommended?.first ?: state.comfortMinCelsius,
                comfortMaxCelsius = recommended?.second ?: state.comfortMaxCelsius
            )
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
        val brand = state.brand.trim().takeIf { it.isNotEmpty() }
        val comfortMin = min(state.comfortMinCelsius, state.comfortMaxCelsius)
        val comfortMax = max(state.comfortMinCelsius, state.comfortMaxCelsius)
        val existing = editingItem
        return if (existing != null) {
            val adjustedCurrent = existing.currentWears.coerceAtMost(maxWear)
            val updated = existing.copy(
                name = state.name.trim(),
                category = categoryOption.category,
                type = state.type,
                sleeveLength = state.sleeveLength,
                thickness = state.thickness,
                comfortMinCelsius = comfortMin,
                comfortMaxCelsius = comfortMax,
                colorHex = colorOption.colorHex,
                colorGroup = colorOption.group,
                pattern = state.pattern,
                maxWears = maxWear,
                currentWears = adjustedCurrent,
                isAlwaysWash = state.isAlwaysWash,
                cleaningType = state.cleaningType,
                brand = brand
            )
            editingItem = updated
            updated
        } else {
            ClothingItem(
                id = UUID.randomUUID().toString(),
                name = state.name.trim(),
                category = categoryOption.category,
                type = state.type,
                sleeveLength = state.sleeveLength,
                thickness = state.thickness,
                comfortMinCelsius = comfortMin,
                comfortMaxCelsius = comfortMax,
                colorHex = colorOption.colorHex,
                colorGroup = colorOption.group,
                pattern = state.pattern,
                maxWears = maxWear,
                currentWears = 0,
                isAlwaysWash = state.isAlwaysWash,
                cleaningType = state.cleaningType,
                status = LaundryStatus.CLOSET,
                brand = brand,
                imageUrl = null,
                lastWornDate = null
            )
        }
    }

    private fun normalizeComfortValue(value: Float): Double {
        return (value * 10f).roundToInt() / 10.0
    }

    private suspend fun loadExistingItem(itemId: String) {
        val item = closetRepository.getItem(itemId) ?: return
        editingItem = item

        val recommendedRange = ComfortRangeDefaults.forItem(item)
        val categoryOption = findOrCreateCategoryOption(item)
        val categoryChoices = ensureCategoryAvailable(categoryOption)
        val (colorOption, colorChoices) = resolveColorOption(item)
        val comfortMin = item.comfortMinCelsius ?: recommendedRange.first
        val comfortMax = item.comfortMaxCelsius ?: recommendedRange.second

        _uiState.update { state ->
            state.copy(
                name = item.name,
                brand = item.brand.orEmpty(),
                selectedCategory = categoryOption,
                selectedColor = colorOption,
                availableCategories = categoryChoices,
                availableColors = colorChoices,
                isAlwaysWash = item.isAlwaysWash,
                maxWears = if (item.isAlwaysWash) 1 else item.maxWears,
                cleaningType = item.cleaningType,
                type = item.type,
                sleeveLength = item.sleeveLength,
                thickness = item.thickness,
                pattern = item.pattern,
                status = item.status,
                comfortMinCelsius = comfortMin,
                comfortMaxCelsius = comfortMax,
                isComfortRangeCustomized = isCustomComfortRange(item, recommendedRange),
                isEditMode = true
            )
        }
    }

    private fun findOrCreateCategoryOption(item: ClothingItem): CategoryOption {
        val existing = categoryOptions.firstOrNull { it.category == item.category }
        if (existing != null) return existing
        val fallbackMaxWears = item.maxWears.coerceAtLeast(MIN_WEAR_COUNT)
        return CategoryOption(
            category = item.category,
            labelResId = item.category.labelResId(),
            type = item.type,
            defaultSleeve = item.sleeveLength,
            defaultThickness = item.thickness,
            defaultCleaning = item.cleaningType,
            defaultMaxWears = fallbackMaxWears,
            defaultAlwaysWash = item.isAlwaysWash
        )
    }

    private fun ensureCategoryAvailable(option: CategoryOption): List<CategoryOption> {
        return if (categoryOptions.any { it.category == option.category }) {
            categoryOptions
        } else {
            categoryOptions + option
        }
    }

    private fun resolveColorOption(item: ClothingItem): Pair<ColorOption, List<ColorOption>> {
        val match = colorOptions.firstOrNull { it.colorHex.equals(item.colorHex, ignoreCase = true) }
        if (match != null) return match to colorOptions
        val option = ColorOption(
            colorHex = item.colorHex,
            labelResId = R.string.closet_color_custom,
            group = item.colorGroup
        )
        return option to (colorOptions + option)
    }

    private fun isCustomComfortRange(item: ClothingItem, recommendedRange: Pair<Double, Double>): Boolean {
        val minValue = item.comfortMinCelsius
        val maxValue = item.comfortMaxCelsius
        if (minValue == null && maxValue == null) return false
        val minDiffers = minValue != null && !approxEquals(minValue, recommendedRange.first)
        val maxDiffers = maxValue != null && !approxEquals(maxValue, recommendedRange.second)
        val missingValue = minValue == null || maxValue == null
        return minDiffers || maxDiffers || missingValue
    }

    private fun approxEquals(a: Double, b: Double, tolerance: Double = 0.1): Boolean {
        return abs(a - b) <= tolerance
    }

    class Factory(
        private val closetRepository: ClosetRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val existingItemId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClosetEditorViewModel::class.java)) {
                return ClosetEditorViewModel(
                    closetRepository = closetRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    existingItemId = existingItemId
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
        category = ClothingCategory.DOWN,
        labelResId = R.string.clothing_category_down,
        type = ClothingType.OUTER,
        defaultSleeve = SleeveLength.LONG,
        defaultThickness = Thickness.THICK,
        defaultCleaning = CleaningType.DRY,
        defaultMaxWears = 6,
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

package com.example.myapplication.ui.logiclo

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.Pattern
import com.example.myapplication.domain.model.ClothingItem as DomainClothingItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

// =============================================================================
// 2. Logic Controller (ViewModel)
// =============================================================================
class LogiCloViewModel(
    private val closetRepository: ClosetRepository
) : ViewModel() {

    // --- UI State ---
    private val _uiState = MutableStateFlow(LogiCloUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            closetRepository.observeAll().collect { domainItems ->
                val uiItems = domainItems.map { it.toUiModel() }
                _uiState.update { it.copy(inventory = uiItems) }
                _refreshSuggestion()
            }
        }
    }

    // --- Getters ---
    val closetItems: List<UiClothingItem>
        get() = _uiState.value.inventory.filter { !it.isDirty }

    val dirtyHomeItems: List<UiClothingItem>
        get() = _uiState.value.inventory.filter { it.isDirty && it.cleaningType == CleaningType.HOME }

    val dirtyDryItems: List<UiClothingItem>
        get() = _uiState.value.inventory.filter { it.isDirty && it.cleaningType == CleaningType.DRY }

    // --- Dashboard Actions ---
    fun setDate(tomorrow: Boolean) {
        _uiState.update { it.copy(isTomorrow = tomorrow) }
        _resetTimeSelection()
        _refreshSuggestion()
    }

    fun setMode(mode: AppMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        _resetTimeSelection()
        _refreshSuggestion()
    }

    fun setEnv(env: EnvMode) {
        _uiState.update { it.copy(selectedEnv = env) }
        _refreshSuggestion()
    }

    fun setTimeSelection(id: String, label: String) {
        _uiState.update { it.copy(selectedTimeId = id, selectedTimeLabel = label) }
        _refreshSuggestion()
    }

    fun setIndoorTemp(temp: Float) {
        _uiState.update { it.copy(indoorTargetTemp = temp) }
        _refreshSuggestion()
    }

    fun setLocation(name: String, isCustom: Boolean) {
        _uiState.update { it.copy(currentLocationName = name, isLocationCustom = isCustom) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    private fun _resetTimeSelection() {
        val (newId, newLabel) = if (_uiState.value.selectedMode == AppMode.CASUAL) {
            if (_uiState.value.isTomorrow) "daytime" to "☀️ 日中 (10-17)"
            else "spot" to "⏱️ スポット (+3h)"
        } else {
            "day" to "☀️ 日勤 (9-18)"
        }
        _uiState.update { it.copy(selectedTimeId = newId, selectedTimeLabel = newLabel) }
    }

    private fun _refreshSuggestion() {
        val cleanItems = _uiState.value.inventory.filter { !it.isDirty }

        val tops = cleanItems.filter { it.type == ItemType.TOP }.toMutableList()
        val bottoms = cleanItems.filter { it.type == ItemType.BOTTOM }.toMutableList()
        val outers = cleanItems.filter { it.type == ItemType.OUTER }.toMutableList()

        // Mock Filtering
        if (_uiState.value.selectedMode == AppMode.OFFICE) {
            tops.removeAll { it.name.contains("パーカー") || it.name.contains("Tシャツ") }
            bottoms.removeAll { it.name.contains("デニム") }
        } else {
            tops.removeAll { it.name.contains("シャツ") }
        }

        val suggestedTop = if (tops.isNotEmpty()) tops.random(Random) else null
        val suggestedBottom = if (bottoms.isNotEmpty()) bottoms.random(Random) else null

        val suggestedOuter = if (_uiState.value.selectedEnv == EnvMode.INDOOR || (_uiState.value.selectedMode == AppMode.CASUAL && _uiState.value.selectedTimeId == "spot")) {
            null
        } else {
            if (outers.isNotEmpty()) outers.random(Random) else null
        }

        _uiState.update {
            it.copy(
                suggestedTop = suggestedTop,
                suggestedBottom = suggestedBottom,
                suggestedOuter = suggestedOuter
            )
        }
    }

    fun markAsActuallyDirty(item: UiClothingItem) {
        viewModelScope.launch {
            val domainItem = item.toDomainModel().copy(status = LaundryStatus.DIRTY)
            closetRepository.upsert(domainItem)
        }
    }

    fun wearCurrentOutfit(): String {
        val isHotDay = _uiState.value.selectedEnv == EnvMode.OUTDOOR || (_uiState.value.selectedEnv == EnvMode.INDOOR && _uiState.value.indoorTargetTemp > 25)
        val damage = if (isHotDay) 2 else 1
        val logs = mutableListOf<String>()

        _uiState.value.suggestedTop?.let { top ->
             viewModelScope.launch {
                 val newWears = top.currentWears + damage
                 val isDirty = newWears >= top.maxWears
                 if (isDirty) {
                     logs.add("${top.name}: 洗濯カゴへ")
                 }
                 val status = if (isDirty) LaundryStatus.DIRTY else LaundryStatus.CLOSET
                 val currentWears = if (isDirty) 0 else newWears
                 
                 val domainItem = top.toDomainModel().copy(currentWears = currentWears, status = status)
                 closetRepository.upsert(domainItem)
             }
        }
        
        // Note: Ideally we should update bottom and outer as well, but keeping it simple as per original logic for now.
        // Also, logs update is tricky with async. For now, we return a message based on local prediction.
        
        return if (isHotDay) "☀️ 暑いため +2カウント (処理中)" else "記録しました (残り回数を更新)"
    }

    // --- Closet & Laundry Actions ---
    fun incrementWearCount(item: UiClothingItem) {
        viewModelScope.launch {
            val newWears = item.currentWears + 1
            val isDirty = newWears >= item.maxWears
            val status = if (isDirty) LaundryStatus.DIRTY else LaundryStatus.CLOSET
            val currentWears = if (isDirty) 0 else newWears
            val domainItem = item.toDomainModel().copy(currentWears = currentWears, status = status)
            closetRepository.upsert(domainItem)
        }
    }

    fun moveToLaundry(item: UiClothingItem) {
        viewModelScope.launch {
            val domainItem = item.toDomainModel().copy(status = LaundryStatus.DIRTY, currentWears = 0)
            closetRepository.upsert(domainItem)
        }
    }

    fun deleteItem(item: UiClothingItem) {
        viewModelScope.launch {
            closetRepository.delete(item.id)
        }
    }

    fun washSelectedItems(itemIds: List<String>) {
        viewModelScope.launch {
            val itemsToWash = _uiState.value.inventory.filter { itemIds.contains(it.id) }
            val updatedItems = itemsToWash.map { 
                it.toDomainModel().copy(status = LaundryStatus.CLOSET, currentWears = 0)
            }
            if (updatedItems.isNotEmpty()) {
                closetRepository.upsert(updatedItems)
            }
            _uiState.update { currentState ->
                val newInventory = currentState.inventory.map { 
                    if (itemIds.contains(it.id)) {
                        it.copy(isDirty = false, currentWears = 0)
                    } else {
                        it
                    }
                }
                currentState.copy(inventory = newInventory)
            }
        }
    }

    fun toggleItemStatus(item: UiClothingItem) {
        viewModelScope.launch {
            val newStatus = if (item.isDirty) LaundryStatus.CLOSET else LaundryStatus.DIRTY
            val currentWears = if (newStatus == LaundryStatus.CLOSET) 0 else item.currentWears
            val domainItem = item.toDomainModel().copy(status = newStatus, currentWears = currentWears)
            closetRepository.upsert(domainItem)
        }
    }

    fun washAllHomeItems() {
        val homeItems = dirtyHomeItems.map { it.id }
        washSelectedItems(homeItems)
    }

    fun resetAllData() {
         viewModelScope.launch {
            val allItems = _uiState.value.inventory.map { 
                it.toDomainModel().copy(status = LaundryStatus.CLOSET, currentWears = 0)
            }
            if (allItems.isNotEmpty()) {
                closetRepository.upsert(allItems)
            }
        }
    }

    // --- Add Item Logic ---
    fun addItem(item: UiClothingItem) {
        viewModelScope.launch {
            closetRepository.upsert(item.toDomainModel())
        }
    }
    
    fun getSmartDefaults(categoryKey: String): Map<String, Any> {
        return when (categoryKey) {
            "t_shirt" -> mapOf("max" to 1, "always" to true, "type" to ItemType.TOP, "sleeve" to SleeveLength.SHORT, "thickness" to Thickness.NORMAL)
            "polo" -> mapOf("max" to 1, "always" to true, "type" to ItemType.TOP, "sleeve" to SleeveLength.SHORT, "thickness" to Thickness.NORMAL)
            "shirt" -> mapOf("max" to 2, "always" to false, "type" to ItemType.TOP, "sleeve" to SleeveLength.LONG, "thickness" to Thickness.THIN)
            "knit" -> mapOf("max" to 5, "always" to false, "type" to ItemType.TOP, "sleeve" to SleeveLength.LONG, "thickness" to Thickness.THICK)
            "hoodie" -> mapOf("max" to 3, "always" to false, "type" to ItemType.TOP, "sleeve" to SleeveLength.LONG, "thickness" to Thickness.THICK)
            "denim" -> mapOf("max" to 10, "always" to false, "type" to ItemType.BOTTOM, "sleeve" to SleeveLength.LONG, "thickness" to Thickness.THICK)
            "slacks" -> mapOf("max" to 3, "always" to false, "type" to ItemType.BOTTOM, "sleeve" to SleeveLength.LONG, "thickness" to Thickness.NORMAL)
            "chino" -> mapOf("max" to 5, "always" to false, "type" to ItemType.BOTTOM, "sleeve" to SleeveLength.LONG, "thickness" to Thickness.NORMAL)
            "jacket" -> mapOf("max" to 5, "always" to false, "type" to ItemType.OUTER, "sleeve" to SleeveLength.LONG, "thickness" to Thickness.NORMAL)
            "coat" -> mapOf("max" to 10, "always" to false, "type" to ItemType.OUTER, "sleeve" to SleeveLength.LONG, "thickness" to Thickness.THICK)
            else -> mapOf("max" to 1, "always" to true, "type" to ItemType.TOP, "sleeve" to SleeveLength.SHORT, "thickness" to Thickness.NORMAL)
        }
    }

    // --- Mappers ---
    
    private fun DomainClothingItem.toUiModel(): UiClothingItem {
        val uiType = when (this.type) {
            ClothingType.TOP, ClothingType.INNER -> ItemType.TOP
            ClothingType.BOTTOM -> ItemType.BOTTOM
            ClothingType.OUTER -> ItemType.OUTER
            ClothingType.UNKNOWN -> ItemType.TOP
        }
        
        val uiIcon = when (this.type) {
            ClothingType.TOP -> R.drawable.ic_clothing_top
            ClothingType.INNER -> R.drawable.ic_clothing_inner
            ClothingType.OUTER -> R.drawable.ic_clothing_outer
            ClothingType.BOTTOM -> R.drawable.ic_clothing_bottom
            else -> R.drawable.ic_clothing_top
        }
        
        val uiCleaningType = when (this.cleaningType) {
            com.example.myapplication.domain.model.CleaningType.HOME -> CleaningType.HOME
            com.example.myapplication.domain.model.CleaningType.DRY -> CleaningType.DRY
            else -> CleaningType.HOME
        }
        
        // Parse hex color safely
        val uiColor = try {
            Color(android.graphics.Color.parseColor(this.colorHex))
        } catch (e: Exception) {
            Color.Gray
        }

        return UiClothingItem(
            id = this.id,
            name = this.name,
            brand = this.brand ?: "",
            type = uiType,
            categoryKey = this.category.name.lowercase(Locale.ROOT),
            sleeveLength = SleeveLength.values().find { it.name.equals(this.sleeveLength.name, true) } ?: SleeveLength.SHORT,
            thickness = Thickness.values().find { it.name.equals(this.thickness.name, true) } ?: Thickness.NORMAL,
            color = uiColor,
            icon = uiIcon,
            maxWears = this.maxWears,
            currentWears = this.currentWears,
            isDirty = this.status == LaundryStatus.DIRTY,
            cleaningType = uiCleaningType,
            fit = FitType.REGULAR // Domain doesn't have FitType yet
        )
    }

    private fun UiClothingItem.toDomainModel(): DomainClothingItem {
        val domainType = when (this.type) {
            ItemType.TOP -> ClothingType.TOP
            ItemType.BOTTOM -> ClothingType.BOTTOM
            ItemType.OUTER -> ClothingType.OUTER
        }
        
        val domainCategory = ClothingCategory.values().find { it.name.equals(this.categoryKey, ignoreCase = true) } ?: ClothingCategory.UNKNOWN

        val domainStatus = if (this.isDirty) LaundryStatus.DIRTY else LaundryStatus.CLOSET
        
        val domainCleaningType = when (this.cleaningType) {
            CleaningType.HOME -> com.example.myapplication.domain.model.CleaningType.HOME
            CleaningType.DRY -> com.example.myapplication.domain.model.CleaningType.DRY
        }
        
        // Convert Color to Hex string
        // Note: Color.value is ULong, we need ARGB hex
        val argb = this.color.value.toLong()
        val hex = String.format("#%08X", argb)

        return DomainClothingItem(
            id = this.id,
            name = this.name,
            category = domainCategory,
            type = domainType,
            sleeveLength = com.example.myapplication.domain.model.SleeveLength.values().find { it.name.equals(this.sleeveLength.name, true) } ?: com.example.myapplication.domain.model.SleeveLength.NONE,
            thickness = com.example.myapplication.domain.model.Thickness.values().find { it.name.equals(this.thickness.name, true) } ?: com.example.myapplication.domain.model.Thickness.NORMAL,
            colorHex = hex,
            colorGroup = ColorGroup.UNKNOWN,
            pattern = Pattern.UNKNOWN,
            maxWears = this.maxWears,
            currentWears = this.currentWears,
            isAlwaysWash = this.maxWears == 1,
            cleaningType = domainCleaningType,
            status = domainStatus,
            brand = this.brand
        )
    }

    companion object {
        class Factory(private val closetRepository: ClosetRepository) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LogiCloViewModel::class.java)) {
                    return LogiCloViewModel(closetRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}


// --- UI State Holder ---
data class LogiCloUiState(
    // Dashboard State
    val isTomorrow: Boolean = false,
    val selectedMode: AppMode = AppMode.CASUAL,
    val selectedEnv: EnvMode = EnvMode.OUTDOOR,
    var selectedTimeLabel: String = "⏱️ スポット (+3h)",
    var selectedTimeId: String = "spot",
    val indoorTargetTemp: Float = 22.0f,
    val currentLocationName: String = "神戸市 (現在地)",
    val isLocationCustom: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    // Inventory State
    val inventory: List<UiClothingItem> = emptyList(), // Changed from generateMockItems()

    // Suggestion State
    val suggestedOuter: UiClothingItem? = null,
    val suggestedTop: UiClothingItem? = null,
    val suggestedBottom: UiClothingItem? = null
)

// Represents system theme options
enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

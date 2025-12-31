package com.example.myapplication.ui.port

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlin.random.Random

class AppViewModel : ViewModel() {
    // --- Dashboard State ---
    var isTomorrow = mutableStateOf(false)
        private set
    var selectedMode = mutableStateOf(AppMode.Casual)
        private set
    var selectedEnv = mutableStateOf(EnvMode.Outdoor)
        private set
    var selectedTimeLabel = mutableStateOf("⏱️ スポット (+3h)")
        private set
    var selectedTimeId = mutableStateOf("spot")
        private set
    var indoorTargetTemp = mutableStateOf(22.0)
        private set

    // Location
    var currentLocationName = mutableStateOf("神戸市 (現在地)")
        private set
    var isLocationCustom = mutableStateOf(false)
        private set

    // Theme
    var isDarkTheme = mutableStateOf(false)

    // --- Inventory State ---
    private val _inventory = generateMockItems()
    val inventory = mutableStateListOf<ClothingItem>().apply { addAll(_inventory) }

    // --- Suggestion State ---
    var suggestedOuter = mutableStateOf<ClothingItem?>(null)
        private set
    var suggestedTop = mutableStateOf<ClothingItem?>(null)
        private set
    var suggestedBottom = mutableStateOf<ClothingItem?>(null)
        private set

    init {
        refreshSuggestion()
    }

    // --- Getters ---
    fun getClosetItems(): List<ClothingItem> = inventory.filter { !it.isDirty }
    fun getDirtyHomeItems(): List<ClothingItem> = inventory.filter { it.isDirty && it.cleaningType == CleaningType.Home }
    fun getDirtyDryItems(): List<ClothingItem> = inventory.filter { it.isDirty && it.cleaningType == CleaningType.Dry }

    // --- Dashboard Actions ---

    fun setDate(tomorrow: Boolean) {
        isTomorrow.value = tomorrow
        resetTimeSelection()
        refreshSuggestion()
    }

    fun setMode(mode: AppMode) {
        selectedMode.value = mode
        resetTimeSelection()
        refreshSuggestion()
    }

    fun setEnv(env: EnvMode) {
        selectedEnv.value = env
        refreshSuggestion()
    }

    fun setTimeSelection(id: String, label: String) {
        selectedTimeId.value = id
        selectedTimeLabel.value = label
        refreshSuggestion()
    }

    fun setIndoorTemp(temp: Double) {
        indoorTargetTemp.value = temp
        refreshSuggestion()
    }

    fun setLocation(name: String, isCustom: Boolean) {
        currentLocationName.value = name
        isLocationCustom.value = isCustom
    }
    
    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }
    
    fun setTheme(isDark: Boolean) {
        isDarkTheme.value = isDark
    }

    private fun resetTimeSelection() {
        if (selectedMode.value == AppMode.Casual) {
            if (isTomorrow.value) {
                selectedTimeId.value = "daytime"
                selectedTimeLabel.value = "☀️ 日中 (10-17)"
            } else {
                selectedTimeId.value = "spot"
                selectedTimeLabel.value = "⏱️ スポット (+3h)"
            }
        } else {
            selectedTimeId.value = "day"
            selectedTimeLabel.value = "☀️ 日勤 (9-18)"
        }
    }

    private fun refreshSuggestion() {
        val rand = Random
        val cleanItems = inventory.filter { !it.isDirty }

        val tops = cleanItems.filter { it.type == ItemType.Top }.toMutableList()
        val bottoms = cleanItems.filter { it.type == ItemType.Bottom }.toMutableList()
        val outers = cleanItems.filter { it.type == ItemType.Outer }.toMutableList()

        if (selectedMode.value == AppMode.Office) {
            tops.removeAll { it.name.contains("パーカー") || it.name.contains("Tシャツ") }
            bottoms.removeAll { it.name.contains("デニム") }
        } else {
            tops.removeAll { it.name.contains("シャツ") && !it.name.contains("Tシャツ") }
        }

        suggestedTop.value = if (tops.isNotEmpty()) tops[rand.nextInt(tops.size)] else null
        suggestedBottom.value = if (bottoms.isNotEmpty()) bottoms[rand.nextInt(bottoms.size)] else null

        if (selectedEnv.value == EnvMode.Indoor || (selectedMode.value == AppMode.Casual && selectedTimeId.value == "spot")) {
            suggestedOuter.value = null
        } else {
            suggestedOuter.value = if (outers.isNotEmpty()) outers[rand.nextInt(outers.size)] else null
        }
    }

    fun markAsActuallyDirty(item: ClothingItem) {
        val index = inventory.indexOfFirst { it.id == item.id }
        if (index != -1) {
            val newItem = inventory[index].copy(isDirty = true)
            inventory[index] = newItem
            refreshSuggestion()
        }
    }

    fun wearCurrentOutfit(): String {
        val isHotDay = selectedEnv.value == EnvMode.Outdoor || (selectedEnv.value == EnvMode.Indoor && indoorTargetTemp.value > 25)
        val damage = if (isHotDay) 2 else 1
        val logs = mutableListOf<String>()

        suggestedTop.value?.let { top ->
            val index = inventory.indexOfFirst { it.id == top.id }
            if (index != -1) {
                var currentWears = top.currentWears + damage
                var isDirty = top.isDirty
                if (currentWears >= top.maxWears) {
                    isDirty = true
                    currentWears = 0
                    logs.add("${top.name}: 洗濯カゴへ")
                }
                val newItem = top.copy(currentWears = currentWears, isDirty = isDirty)
                inventory[index] = newItem
            }
        }

        refreshSuggestion()

        if (isHotDay) {
            return "☀️ 暑いため +2カウント → ${logs.joinToString(", ")}"
        }
        return "記録しました (残り回数を更新)"
    }

    // --- Closet & Laundry Actions ---

    fun toggleItemStatus(item: ClothingItem) {
        val index = inventory.indexOfFirst { it.id == item.id }
        if (index != -1) {
            val isDirty = !item.isDirty
            val currentWears = if (!isDirty) 0 else item.currentWears
            val newItem = item.copy(isDirty = isDirty, currentWears = currentWears)
            inventory[index] = newItem
            refreshSuggestion()
        }
    }

    fun washAllHomeItems() {
        val iterator = inventory.listIterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.isDirty && item.cleaningType == CleaningType.Home) {
                iterator.set(item.copy(isDirty = false, currentWears = 0))
            }
        }
        refreshSuggestion()
    }

    fun resetAllData() {
        val iterator = inventory.listIterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            iterator.set(item.copy(isDirty = false, currentWears = 0))
        }
        refreshSuggestion()
    }

    // --- Add Item Logic ---
    fun addItem(item: ClothingItem) {
        inventory.add(item)
        refreshSuggestion()
    }

    fun getSmartDefaults(categoryKey: String): Map<String, Any> {
        return when (categoryKey) {
            "t_shirt" -> mapOf("max" to 1, "always" to true, "type" to ItemType.Top, "sleeve" to SleeveLength.Short, "thickness" to Thickness.Normal)
            "polo" -> mapOf("max" to 1, "always" to true, "type" to ItemType.Top, "sleeve" to SleeveLength.Short, "thickness" to Thickness.Normal)
            "shirt" -> mapOf("max" to 2, "always" to false, "type" to ItemType.Top, "sleeve" to SleeveLength.Long, "thickness" to Thickness.Thin)
            "knit" -> mapOf("max" to 5, "always" to false, "type" to ItemType.Top, "sleeve" to SleeveLength.Long, "thickness" to Thickness.Thick)
            "hoodie" -> mapOf("max" to 3, "always" to false, "type" to ItemType.Top, "sleeve" to SleeveLength.Long, "thickness" to Thickness.Thick)
            "denim" -> mapOf("max" to 10, "always" to false, "type" to ItemType.Bottom, "sleeve" to SleeveLength.Long, "thickness" to Thickness.Thick)
            "slacks" -> mapOf("max" to 3, "always" to false, "type" to ItemType.Bottom, "sleeve" to SleeveLength.Long, "thickness" to Thickness.Normal)
            "chino" -> mapOf("max" to 5, "always" to false, "type" to ItemType.Bottom, "sleeve" to SleeveLength.Long, "thickness" to Thickness.Normal)
            "jacket" -> mapOf("max" to 5, "always" to false, "type" to ItemType.Outer, "sleeve" to SleeveLength.Long, "thickness" to Thickness.Normal)
            "coat" -> mapOf("max" to 10, "always" to false, "type" to ItemType.Outer, "sleeve" to SleeveLength.Long, "thickness" to Thickness.Thick)
            else -> mapOf("max" to 1, "always" to true, "type" to ItemType.Top, "sleeve" to SleeveLength.Short, "thickness" to Thickness.Normal)
        }
    }
}

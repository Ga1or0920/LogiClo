package com.example.myapplication.ui.logiclo

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.LocationSearchRepository
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.example.myapplication.data.weather.WeatherRepository
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LocationSearchResult
import com.example.myapplication.domain.model.Pattern
import com.example.myapplication.domain.model.WeatherLocationOverride
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.domain.model.CasualForecastDay
import com.example.myapplication.domain.model.CasualForecastSegment
import com.example.myapplication.domain.model.ClothingItem as DomainClothingItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

private const val LOCATION_SEARCH_MIN_QUERY = 2
private const val LOCATION_SEARCH_DEBOUNCE_MILLIS = 400L

// =============================================================================
// 2. Logic Controller (ViewModel)
// =============================================================================
class LogiCloViewModel(
    private val closetRepository: ClosetRepository,
    private val locationSearchRepository: LocationSearchRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    // --- UI State ---
    private val _uiState = MutableStateFlow(LogiCloUiState())
    val uiState = _uiState.asStateFlow()

    // --- Location Search State ---
    private val _locationSearchState = MutableStateFlow(LocationSearchState())
    val locationSearchState = _locationSearchState.asStateFlow()
    private var locationSearchJob: Job? = null

    init {
        viewModelScope.launch {
            closetRepository.observeAll().collect { domainItems ->
                val uiItems = domainItems.map { it.toUiModel() }
                _uiState.update { it.copy(inventory = uiItems) }
                _refreshSuggestion()
            }
        }
        // Observe weather changes and update UI
        viewModelScope.launch {
            weatherRepository.observeCurrentWeather().collect { weather ->
                _uiState.update { it.copy(weather = weather) }
                _refreshSuggestion()
            }
        }
        // Observe user preferences for location override
        viewModelScope.launch {
            userPreferencesRepository.observe().collect { preferences ->
                val override = preferences.weatherLocationOverride
                val locationName = override?.label ?: "神戸市 (現在地)"
                val isCustom = override != null
                _uiState.update { it.copy(currentLocationName = locationName, isLocationCustom = isCustom) }
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

    // --- Location Search Actions ---
    fun openLocationSearch() {
        _locationSearchState.value = LocationSearchState(isVisible = true)
    }

    fun closeLocationSearch() {
        locationSearchJob?.cancel()
        locationSearchJob = null
        _locationSearchState.value = LocationSearchState()
    }

    fun onLocationSearchQueryChanged(query: String) {
        scheduleLocationSearch(query)
    }

    fun onLocationSearchResultSelected(result: LocationSearchResult) {
        viewModelScope.launch {
            userPreferencesRepository.update { current ->
                current.copy(
                    weatherLocationOverride = WeatherLocationOverride(
                        label = result.title,
                        latitude = result.latitude,
                        longitude = result.longitude
                    )
                )
            }
            closeLocationSearch()
        }
    }

    fun onUseCurrentLocation() {
        viewModelScope.launch {
            userPreferencesRepository.update { current ->
                current.copy(weatherLocationOverride = null)
            }
            closeLocationSearch()
        }
    }

    private fun scheduleLocationSearch(query: String) {
        val trimmed = query.trim()
        _locationSearchState.update { current ->
            current.copy(
                query = query,
                errorMessage = null,
                isLoading = trimmed.length >= LOCATION_SEARCH_MIN_QUERY,
                results = emptyList()
            )
        }
        locationSearchJob?.cancel()
        if (trimmed.length < LOCATION_SEARCH_MIN_QUERY) {
            return
        }
        locationSearchJob = viewModelScope.launch {
            delay(LOCATION_SEARCH_DEBOUNCE_MILLIS)
            try {
                val results = locationSearchRepository.searchByName(trimmed)
                _locationSearchState.update { current ->
                    current.copy(
                        isLoading = false,
                        results = results,
                        errorMessage = if (results.isEmpty()) "検索結果がありません" else null
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _locationSearchState.update { current ->
                    current.copy(
                        isLoading = false,
                        results = emptyList(),
                        errorMessage = "検索に失敗しました"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationSearchJob?.cancel()
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    private fun _resetTimeSelection() {
        val (newId, newLabel) = if (_uiState.value.selectedMode == AppMode.CASUAL) {
            if (_uiState.value.isTomorrow) "daytime" to "☀️ 日中 (10-17)"
            else "spot" to "⏱️ 短時間 (+3h)"
        } else {
            "day" to "☀️ 日勤 (9-18)"
        }
        _uiState.update { it.copy(selectedTimeId = newId, selectedTimeLabel = newLabel) }
    }

    /**
     * 時間帯IDに応じたセグメントを取得
     */
    private fun getSegmentsForTimeId(timeId: String): List<CasualForecastSegment> {
        return when (timeId) {
            // 今日カジュアル
            "spot" -> listOf(CasualForecastSegment.AFTERNOON)
            "half" -> listOf(CasualForecastSegment.AFTERNOON, CasualForecastSegment.EVENING)
            "full" -> listOf(CasualForecastSegment.MORNING, CasualForecastSegment.AFTERNOON, CasualForecastSegment.EVENING)
            // 明日カジュアル
            "daytime" -> listOf(CasualForecastSegment.MORNING, CasualForecastSegment.AFTERNOON)
            "night" -> listOf(CasualForecastSegment.EVENING)
            "allday" -> listOf(CasualForecastSegment.MORNING, CasualForecastSegment.AFTERNOON, CasualForecastSegment.EVENING)
            // オフィス
            "day" -> listOf(CasualForecastSegment.MORNING, CasualForecastSegment.AFTERNOON)
            "evening" -> listOf(CasualForecastSegment.EVENING)
            else -> listOf(CasualForecastSegment.MORNING, CasualForecastSegment.AFTERNOON, CasualForecastSegment.EVENING)
        }
    }

    /**
     * 今日/明日・時間帯に応じた体感温度を取得
     */
    private fun getEffectiveTempForTimeSlot(
        weather: WeatherSnapshot?,
        isTomorrow: Boolean,
        timeId: String
    ): Double {
        if (weather == null) return 20.0

        // 今日で短時間の場合は現在の天気を使用（より正確）
        if (!isTomorrow && timeId in listOf("spot", "half")) {
            return weather.apparentTemperatureCelsius
        }

        // それ以外はcasualSegmentSummariesから計算
        val targetDay = if (isTomorrow) CasualForecastDay.TOMORROW else CasualForecastDay.TODAY
        val targetSegments = getSegmentsForTimeId(timeId)

        val matchingSummaries = weather.casualSegmentSummaries
            .filter { it.day == targetDay && it.segment in targetSegments }

        return if (matchingSummaries.isNotEmpty()) {
            matchingSummaries
                .map { it.averageApparentTemperatureCelsius }
                .average()
                .takeIf { !it.isNaN() } ?: 20.0
        } else if (!isTomorrow) {
            // 今日でセグメントデータがない場合は現在値にフォールバック
            weather.apparentTemperatureCelsius
        } else {
            20.0
        }
    }

    private fun _refreshSuggestion() {
        val cleanItems = _uiState.value.inventory.filter { !it.isDirty }
        val state = _uiState.value

        val tops = cleanItems.filter { it.type == ItemType.TOP }.toMutableList()
        val bottoms = cleanItems.filter { it.type == ItemType.BOTTOM }.toMutableList()
        val outers = cleanItems.filter { it.type == ItemType.OUTER }.toMutableList()

        // 体感温度を取得（室内の場合は室内設定温度、屋外の場合は今日/明日・時間帯に応じた体感温度）
        val effectiveTemp = if (state.selectedEnv == EnvMode.INDOOR) {
            state.indoorTargetTemp.toDouble()
        } else {
            getEffectiveTempForTimeSlot(
                weather = state.weather,
                isTomorrow = state.isTomorrow,
                timeId = state.selectedTimeId
            )
        }

        // 体感温度に基づくフィルタリング
        when {
            effectiveTemp < 15.0 -> {
                // 寒い: 厚手・長袖を優先
                tops.sortByDescending { it.thickness == Thickness.THICK }
                tops.removeAll { it.sleeveLength == SleeveLength.SHORT && tops.any { t -> t.sleeveLength == SleeveLength.LONG } }
            }
            effectiveTemp < 20.0 -> {
                // 涼しい: 普通の服、長袖を優先
                tops.removeAll { it.sleeveLength == SleeveLength.SHORT && tops.any { t -> t.sleeveLength == SleeveLength.LONG } }
            }
            effectiveTemp > 25.0 -> {
                // 暑い: 薄手・半袖を優先
                tops.sortByDescending { it.thickness == Thickness.THIN || it.sleeveLength == SleeveLength.SHORT }
                tops.removeAll { it.thickness == Thickness.THICK && tops.any { t -> t.thickness != Thickness.THICK } }
            }
            // 20-25℃は快適: フィルタリングなし
        }

        // モードによるフィルタリング
        if (state.selectedMode == AppMode.OFFICE) {
            tops.removeAll { it.name.contains("パーカー") || it.name.contains("Tシャツ") }
            bottoms.removeAll { it.name.contains("デニム") }
        } else {
            tops.removeAll { it.name.contains("シャツ") }
        }

        val suggestedTop = if (tops.isNotEmpty()) tops.random(Random) else null
        val suggestedBottom = if (bottoms.isNotEmpty()) bottoms.random(Random) else null

        // アウターの判定: 寒い場合（<20℃）は推奨、暑い場合（>25℃）は不要
        val needsOuter = effectiveTemp < 20.0 && state.selectedEnv != EnvMode.INDOOR
        val suggestedOuter = when {
            state.selectedEnv == EnvMode.INDOOR -> null
            state.selectedMode == AppMode.CASUAL && state.selectedTimeId == "spot" && effectiveTemp > 15.0 -> null
            needsOuter && outers.isNotEmpty() -> {
                // 寒さに応じてアウターを選択
                if (effectiveTemp < 10.0) {
                    outers.filter { it.thickness == Thickness.THICK }.randomOrNull() ?: outers.random(Random)
                } else {
                    outers.random(Random)
                }
            }
            effectiveTemp > 25.0 -> null
            outers.isNotEmpty() -> outers.random(Random)
            else -> null
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
        class Factory(
            private val closetRepository: ClosetRepository,
            private val locationSearchRepository: LocationSearchRepository,
            private val userPreferencesRepository: UserPreferencesRepository,
            private val weatherRepository: WeatherRepository
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LogiCloViewModel::class.java)) {
                    return LogiCloViewModel(
                        closetRepository,
                        locationSearchRepository,
                        userPreferencesRepository,
                        weatherRepository
                    ) as T
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
    var selectedTimeLabel: String = "⏱️ 短時間 (+3h)",
    var selectedTimeId: String = "spot",
    val indoorTargetTemp: Float = 22.0f,
    val currentLocationName: String = "神戸市 (現在地)",
    val isLocationCustom: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    // Weather State
    val weather: WeatherSnapshot? = null,

    // Inventory State
    val inventory: List<UiClothingItem> = emptyList(), // Changed from generateMockItems()

    // Suggestion State
    val suggestedOuter: UiClothingItem? = null,
    val suggestedTop: UiClothingItem? = null,
    val suggestedBottom: UiClothingItem? = null
)

// --- Location Search State ---
data class LocationSearchState(
    val isVisible: Boolean = false,
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<LocationSearchResult> = emptyList(),
    val errorMessage: String? = null
)

// Represents system theme options
enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

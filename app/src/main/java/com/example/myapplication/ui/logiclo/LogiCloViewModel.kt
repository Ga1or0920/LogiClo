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
    private val weatherRepository: WeatherRepository,
    private val wearFeedbackRepository: com.example.myapplication.data.repository.WearFeedbackRepository? = null
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
     * デバッグオーバーライドが設定されている場合はそれを優先
     */
    private fun getEffectiveTempForTimeSlot(
        weather: WeatherSnapshot?,
        isTomorrow: Boolean,
        timeId: String
    ): Double {
        // デバッグオーバーライドがある場合は常にそれを使用
        _uiState.value.debugTemperatureOverride?.let { return it }

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

    // --- Debug Override Functions ---
    fun setDebugTemperatureOverride(temp: Double?) {
        _uiState.update { it.copy(debugTemperatureOverride = temp) }
        _refreshSuggestion()
    }

    fun setDebugWeatherCodeOverride(code: Int?) {
        _uiState.update { it.copy(debugWeatherCodeOverride = code) }
        _refreshSuggestion()
    }

    fun getEffectiveWeatherCode(): Int {
        return _uiState.value.debugWeatherCodeOverride
            ?: _uiState.value.weather?.weatherCode
            ?: 0
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

        // アウターの判定:
        // - 既存の仕様では外気温を基準にしていたが、
        //   UI の「室内設定」スライダーがあるため、
        //   ユーザーが室内モードを選択している場合は室内設定温度を優先して判定する。
        val outdoorTemp = if (state.selectedEnv == EnvMode.INDOOR) {
            state.indoorTargetTemp.toDouble()
        } else {
            getEffectiveTempForTimeSlot(
                weather = state.weather,
                isTomorrow = state.isTomorrow,
                timeId = state.selectedTimeId
            )
        }
        val needsOuter = outdoorTemp < 20.0
        val suggestedOuter = when {
            state.selectedMode == AppMode.CASUAL && state.selectedTimeId == "spot" && outdoorTemp > 15.0 -> null
            needsOuter && outers.isNotEmpty() -> {
                // 寒さに応じてアウターを選択
                if (outdoorTemp < 10.0) {
                    outers.filter { it.thickness == Thickness.THICK }.randomOrNull() ?: outers.random(Random)
                } else {
                    outers.random(Random)
                }
            }
            outdoorTemp > 25.0 -> null
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

    /**
     * 指定したタイプの服を別の候補に変更する
     */
    fun changeOutfitItem(type: ItemType) {
        val state = _uiState.value
        val cleanItems = state.inventory.filter { !it.isDirty }

        when (type) {
            ItemType.OUTER -> {
                val currentOuter = state.suggestedOuter
                val outers = cleanItems.filter { it.type == ItemType.OUTER && it.id != currentOuter?.id }
                if (outers.isNotEmpty()) {
                    _uiState.update { it.copy(suggestedOuter = outers.random(Random)) }
                }
            }
            ItemType.TOP -> {
                val currentTop = state.suggestedTop
                val tops = cleanItems.filter { it.type == ItemType.TOP && it.id != currentTop?.id }
                if (tops.isNotEmpty()) {
                    _uiState.update { it.copy(suggestedTop = tops.random(Random)) }
                }
            }
            ItemType.BOTTOM -> {
                val currentBottom = state.suggestedBottom
                val bottoms = cleanItems.filter { it.type == ItemType.BOTTOM && it.id != currentBottom?.id }
                if (bottoms.isNotEmpty()) {
                    _uiState.update { it.copy(suggestedBottom = bottoms.random(Random)) }
                }
            }
        }
    }

    // Undo用に前回の状態を保存
    private var lastWornItems: List<UiClothingItem> = emptyList()
    // フィードバック用に着用時の基準温度を保存
    private var lastWornBasisTemp: Double = 20.0

    fun wearCurrentOutfit(): String {
        val state = _uiState.value
        // 体感気温が高い日（25°C以上）のみカウント+2
        val basisTemp = when (state.selectedEnv) {
            EnvMode.OUTDOOR -> getEffectiveTempForTimeSlot(
                weather = state.weather,
                isTomorrow = state.isTomorrow,
                timeId = state.selectedTimeId
            )
            EnvMode.INDOOR -> state.indoorTargetTemp.toDouble()
        }
        val isHotDay = basisTemp >= 25.0
        val damage = if (isHotDay) 2 else 1

        // Undo用に現在の状態を保存
        lastWornItems = listOfNotNull(
            state.suggestedTop,
            state.suggestedBottom,
            state.suggestedOuter
        )
        lastWornBasisTemp = basisTemp

        _uiState.value.suggestedTop?.let { top ->
             viewModelScope.launch {
                 val newWears = top.currentWears + damage
                 val isDirty = newWears >= top.maxWears
                 val status = if (isDirty) LaundryStatus.DIRTY else LaundryStatus.CLOSET
                 val currentWears = if (isDirty) 0 else newWears

                 val domainItem = top.toDomainModel().copy(currentWears = currentWears, status = status)
                 closetRepository.upsert(domainItem)
             }
        }

        // フィードバック用に着用記録（通知は21時に送信される）
        viewModelScope.launch {
            wearFeedbackRepository?.recordWear(
                topItemId = state.suggestedTop?.id,
                bottomItemId = state.suggestedBottom?.id
            )
        }

        // 基準温度を保存（後でフィードバック時に使用）
        lastWornBasisTemp = basisTemp

        return if (isHotDay) "☀️ 暑いため +2カウント" else "記録しました"
    }

    fun undoWearOutfit() {
        if (lastWornItems.isEmpty()) return
        viewModelScope.launch {
            lastWornItems.forEach { item ->
                val domainItem = item.toDomainModel()
                closetRepository.upsert(domainItem)
            }
            lastWornItems = emptyList()
        }
        _uiState.update { it.copy(showFeedbackDialog = false) }
    }

    fun canUndoWear(): Boolean = lastWornItems.isNotEmpty()

    // --- Feedback & Learning ---
    fun dismissFeedbackDialog() {
        _uiState.update { it.copy(showFeedbackDialog = false) }
    }

    fun submitFeedback(itemId: String, rating: com.example.myapplication.domain.model.WearFeedbackRating) {
        viewModelScope.launch {
            val item = _uiState.value.inventory.find { it.id == itemId } ?: return@launch
            val basisTemp = _uiState.value.feedbackBasisTemp

            // 学習アルゴリズム: 温度範囲を更新
            val currentMin = item.comfortMinCelsius ?: 10.0
            val currentMax = item.comfortMaxCelsius ?: 30.0

            val (newMin, newMax) = when (rating) {
                com.example.myapplication.domain.model.WearFeedbackRating.TOO_COLD -> {
                    // 寒かった → 下限を引き上げ
                    val updatedMin = maxOf(currentMin + 3.0, basisTemp + 1.0)
                    Pair(updatedMin, currentMax)
                }
                com.example.myapplication.domain.model.WearFeedbackRating.TOO_WARM -> {
                    // 暑かった → 上限を引き下げ
                    val updatedMax = minOf(currentMax - 3.0, basisTemp - 1.0)
                    Pair(currentMin, updatedMax)
                }
                com.example.myapplication.domain.model.WearFeedbackRating.JUST_RIGHT -> {
                    // ちょうど良い → 変更なし
                    Pair(currentMin, currentMax)
                }
            }

            // 温度範囲を更新
            val domainItem = item.toDomainModel().copy(
                comfortMinCelsius = newMin,
                comfortMaxCelsius = newMax
            )
            closetRepository.upsert(domainItem)
        }
    }

    /**
     * カスタム温度値付きでフィードバックを送信
     * ユーザーがスライダーで調整した値を直接使用
     */
    fun submitFeedbackWithCustomTemp(
        itemId: String,
        rating: com.example.myapplication.domain.model.WearFeedbackRating,
        customMinTemp: Double,
        customMaxTemp: Double
    ) {
        viewModelScope.launch {
            val item = _uiState.value.inventory.find { it.id == itemId } ?: return@launch

            // ユーザーが指定したカスタム温度値を使用
            val domainItem = item.toDomainModel().copy(
                comfortMinCelsius = customMinTemp,
                comfortMaxCelsius = customMaxTemp
            )
            closetRepository.upsert(domainItem)
        }
    }

    fun getLastWornItems(): List<UiClothingItem> = lastWornItems

    /**
     * デバッグ用：フィードバックダイアログを手動で表示
     * lastWornItemsが空の場合は現在の提案アイテムを使用
     */
    fun triggerFeedbackDialog() {
        val state = _uiState.value
        val items = if (lastWornItems.isNotEmpty()) {
            lastWornItems
        } else {
            // lastWornItemsが空の場合、現在の提案アイテムを使用
            listOfNotNull(state.suggestedTop, state.suggestedBottom, state.suggestedOuter)
        }

        if (items.isNotEmpty()) {
            // フィードバック用にアイテムを設定
            lastWornItems = items
            val basisTemp = when (state.selectedEnv) {
                EnvMode.OUTDOOR -> getEffectiveTempForTimeSlot(
                    weather = state.weather,
                    isTomorrow = state.isTomorrow,
                    timeId = state.selectedTimeId
                )
                EnvMode.INDOOR -> state.indoorTargetTemp.toDouble()
            }
            _uiState.update { it.copy(showFeedbackDialog = true, feedbackBasisTemp = basisTemp) }
        }
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

    fun addItemWithStatus(item: UiClothingItem, status: LaundryStatus) {
        viewModelScope.launch {
            val domainItem = item.toDomainModel().copy(status = status)
            closetRepository.upsert(domainItem)
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

        // Resolve color name from hex if possible, otherwise fall back to colorGroup
        val resolvedFromHex = try {
            ColorNameResolver.resolve(this.colorHex)
        } catch (e: Exception) {
            ""
        }

        val colorDisplayName = if (resolvedFromHex.isNotBlank()) {
            resolvedFromHex
        } else {
            when (this.colorGroup) {
                com.example.myapplication.domain.model.ColorGroup.MONOTONE -> "モノトーン"
                com.example.myapplication.domain.model.ColorGroup.NAVY_BLUE -> "ネイビー/ブルー"
                com.example.myapplication.domain.model.ColorGroup.VIVID -> "ビビッド"
                com.example.myapplication.domain.model.ColorGroup.EARTH_TONE -> "アースカラー"
                com.example.myapplication.domain.model.ColorGroup.PASTEL -> "パステル"
                com.example.myapplication.domain.model.ColorGroup.OTHER -> "その他"
                com.example.myapplication.domain.model.ColorGroup.UNKNOWN -> ""
            }
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
            colorName = colorDisplayName,
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
            private val weatherRepository: WeatherRepository,
            private val wearFeedbackRepository: com.example.myapplication.data.repository.WearFeedbackRepository? = null
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LogiCloViewModel::class.java)) {
                    return LogiCloViewModel(
                        closetRepository,
                        locationSearchRepository,
                        userPreferencesRepository,
                        weatherRepository,
                        wearFeedbackRepository
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
    val suggestedBottom: UiClothingItem? = null,

    // Debug Override State
    val debugTemperatureOverride: Double? = null,  // nullの場合は通常の気温を使用
    val debugWeatherCodeOverride: Int? = null,     // nullの場合は通常の天気を使用

    // Feedback Dialog State
    val showFeedbackDialog: Boolean = false,
    val feedbackBasisTemp: Double = 20.0
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

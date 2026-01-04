package com.example.myapplication.ui.logiclo

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.domain.model.CasualForecastDay
import com.example.myapplication.domain.model.CasualForecastSegment
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.ui.logiclo.components.*
import com.example.myapplication.ui.theme.LogiCloTheme
import com.example.myapplication.ui.theme.TextGrey
import com.example.myapplication.domain.model.WearFeedbackRating

// =============================================================================
// Screen 1: Dashboard
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: LogiCloViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locationSearchState by viewModel.locationSearchState.collectAsState()
    var showTempSheet by remember { mutableStateOf(false) }

    // A simple way to show a snackbar message
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wearOutfitMessage by remember { derivedStateOf { viewModel.wearCurrentOutfit() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Header(
                uiState = uiState,
                onDateChange = { viewModel.setDate(it) },
                onLocationClick = { viewModel.openLocationSearch() },
                onModeChange = { viewModel.setMode(it) },
                onTimeChange = { id, label -> viewModel.setTimeSelection(id, label) },
                onEnvChange = { viewModel.setEnv(it) }
            )
        },
        bottomBar = {
            BottomAction(
                onClick = {
                    val msg = viewModel.wearCurrentOutfit()
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = msg,
                            actionLabel = "元に戻す",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoWearOutfit()
                            snackbarHostState.showSnackbar("元に戻しました")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // 今日/明日・時間帯に応じた天気データを取得
                val weatherData = getWeatherDataForTimeSlot(
                    weather = uiState.weather,
                    isTomorrow = uiState.isTomorrow,
                    timeId = uiState.selectedTimeId
                )

                // デバッグオーバーライドを適用
                val effectiveTemp = uiState.debugTemperatureOverride ?: weatherData.first
                val effectiveWeatherCode = uiState.debugWeatherCodeOverride ?: weatherData.third
                val isDebugMode = uiState.debugTemperatureOverride != null || uiState.debugWeatherCodeOverride != null

                WeatherInfo(
                    isTomorrow = uiState.isTomorrow,
                    selectedEnv = uiState.selectedEnv,
                    indoorTargetTemp = uiState.indoorTargetTemp,
                    locationName = uiState.currentLocationName.replace(" (現在地)", ""),
                    apparentTemp = effectiveTemp,
                    humidity = weatherData.second,
                    weatherCode = effectiveWeatherCode,
                    isDebugMode = isDebugMode,
                    onIndoorClick = { showTempSheet = true }
                )
            }

            if (uiState.suggestedTop == null || uiState.suggestedBottom == null) {
                item {
                    Column(
                        modifier = Modifier.fillParentMaxHeight(0.7f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = TextGrey, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("コーデが組めません", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        val hasAnyClothes = uiState.inventory.isNotEmpty()
                        val message = if (hasAnyClothes) {
                            "現在の温度・モードに合う服がありません"
                        } else {
                            "クローゼットに服を追加してください"
                        }
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextGrey)
                    }
                }
            } else {
                uiState.suggestedOuter?.let {
                    item { OutfitCardItem(item = it, label = "アウター", onChangeItem = { viewModel.changeOutfitItem(ItemType.OUTER) }) }
                }
                uiState.suggestedTop?.let {
                    item { OutfitCardItem(item = it, label = "トップス", onChangeItem = { viewModel.changeOutfitItem(ItemType.TOP) }) }
                }
                uiState.suggestedBottom?.let {
                    item { OutfitCardItem(item = it, label = "ボトムス", onChangeItem = { viewModel.changeOutfitItem(ItemType.BOTTOM) }) }
                }
            }
        }
    }

    if (locationSearchState.isVisible) {
        LocationSearchSheet(
            searchState = locationSearchState,
            onDismiss = { viewModel.closeLocationSearch() },
            onQueryChanged = { viewModel.onLocationSearchQueryChanged(it) },
            onResultSelected = { viewModel.onLocationSearchResultSelected(it) },
            onUseCurrentLocation = { viewModel.onUseCurrentLocation() }
        )
    }

    if (showTempSheet) {
        IndoorTempSheet(
            initialTemp = uiState.indoorTargetTemp,
            onDismiss = { showTempSheet = false },
            onTempChanged = { viewModel.setIndoorTemp(it) }
        )
    }

    // フィードバックダイアログ
    if (uiState.showFeedbackDialog) {
        FeedbackDialog(
            wornItems = viewModel.getLastWornItems(),
            basisTemp = uiState.feedbackBasisTemp,
            onSubmit = { itemId, rating, minTemp, maxTemp ->
                viewModel.submitFeedbackWithCustomTemp(itemId, rating, minTemp, maxTemp)
            },
            onDismiss = { viewModel.dismissFeedbackDialog() }
        )
    }
}

@Composable
private fun Header(
    uiState: LogiCloUiState,
    onDateChange: (Boolean) -> Unit,
    onLocationClick: () -> Unit,
    onModeChange: (AppMode) -> Unit,
    onTimeChange: (String, String) -> Unit,
    onEnvChange: (EnvMode) -> Unit
) {
    fun getTimeOptions(): List<Pair<String, String>> {
        return if (uiState.selectedMode == AppMode.CASUAL) {
            if (uiState.isTomorrow) {
                listOf(
                    "daytime" to "☀️ 日中 (10-17)",
                    "night" to "🌙 夜間 (17-23)",
                    "allday" to "📅 終日 (08-22)"
                )
            } else {
                listOf(
                    "spot" to "⏱️ 短時間 (+3h)",
                    "half" to "🌤️ 半日 (+6h)",
                    "full" to "📅 終日 (〜22時)"
                )
            }
        } else {
            listOf(
                "day" to "☀️ 日勤 (9-18)",
                "evening" to "🌆 夕勤 (17-22)",
                "night" to "🌙 夜勤 (22-07)"
            )
        }
    }
    
    Surface(
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlidingToggle(
                    labels = listOf("今日", "明日"),
                    selectedIndex = if (uiState.isTomorrow) 1 else 0,
                    onChanged = { onDateChange(it == 1) },
                    modifier = Modifier.width(140.dp)
                )

                TextButton(onClick = onLocationClick) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(16.dp),
                        tint = if (uiState.isLocationCustom) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = uiState.currentLocationName,
                        color = if (uiState.isLocationCustom) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SlidingToggle(
                    labels = listOf("仕事", "休日"),
                    icons = listOf(Icons.Default.BusinessCenter, Icons.Default.Home),
                    selectedIndex = if (uiState.selectedMode == AppMode.OFFICE) 0 else 1,
                    onChanged = { onModeChange(if (it == 0) AppMode.OFFICE else AppMode.CASUAL) },
                    modifier = Modifier.weight(1f)
                )

                TimeMenuButton(
                    label = uiState.selectedTimeLabel,
                    selectedId = uiState.selectedTimeId,
                    options = getTimeOptions(),
                    onSelected = onTimeChange
                )

                IconToggleGroup(
                    selectedIndex = uiState.selectedEnv.ordinal,
                    onChanged = { onEnvChange(EnvMode.values()[it]) },
                    icons = listOf(Icons.Default.Park, Icons.Default.Chair)
                )
            }
        }
    }
}

@Composable
private fun WeatherInfo(
    isTomorrow: Boolean,
    selectedEnv: EnvMode,
    indoorTargetTemp: Float,
    locationName: String,
    apparentTemp: Double?,
    humidity: Int?,
    weatherCode: Int?,
    isDebugMode: Boolean = false,
    onIndoorClick: () -> Unit
) {
    val isIndoor = selectedEnv == EnvMode.INDOOR
    val displayTemp = if (isIndoor) {
        "${indoorTargetTemp.toInt()}℃"
    } else {
        apparentTemp?.let { "${it.toInt()}℃" } ?: "--℃"
    }

    // WMO Weather Code に基づいてアイコンを選択
    val weatherIcon = if (isIndoor) {
        Icons.Default.Thermostat
    } else {
        when (weatherCode) {
            0 -> Icons.Default.WbSunny                      // 快晴
            1, 2 -> Icons.Default.WbSunny                   // 晴れ〜やや曇り
            3 -> Icons.Default.WbCloudy                     // 曇り
            in 45..48 -> Icons.Default.Cloud                // 霧
            in 51..57 -> Icons.Default.Grain                // 霧雨
            in 61..67 -> Icons.Default.WaterDrop            // 雨
            in 71..77 -> Icons.Default.AcUnit               // 雪
            in 80..82 -> Icons.Default.WaterDrop            // にわか雨
            in 85..86 -> Icons.Default.AcUnit               // にわか雪
            in 95..99 -> Icons.Default.Bolt                 // 雷雨
            else -> Icons.Default.WbCloudy
        }
    }

    val modifier = if (isIndoor) Modifier.clickable(onClick = onIndoorClick) else Modifier

    Column(
        modifier = modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isTomorrow) "明日の$locationName" else "今日の$locationName",
            style = MaterialTheme.typography.bodySmall,
            color = TextGrey
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // デバッグモードインジケーター
            if (isDebugMode) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = "Debug Mode",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                weatherIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${if (isIndoor) "室内設定" else "体感"} $displayTemp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (isIndoor) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextGrey, modifier = Modifier.size(14.dp))
            }
        }
    }
}


@Composable
private fun BottomAction(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("これを着る", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSearchSheet(
    searchState: LocationSearchState,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onResultSelected: (com.example.myapplication.domain.model.LocationSearchResult) -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("場所を検索", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Search Field
            OutlinedTextField(
                value = searchState.query,
                onValueChange = onQueryChanged,
                label = { Text("地名・施設名 (例: ユニバ)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Error or empty message
            searchState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Options
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text("現在地を使う (デフォルト)") },
                        leadingContent = { Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.clickable { onUseCurrentLocation() }
                    )
                }
                items(searchState.results.size) { index ->
                    val result = searchState.results[index]
                    ListItem(
                        headlineContent = { Text(result.title) },
                        supportingContent = result.subtitle?.let { { Text(it) } },
                        leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                        modifier = Modifier.clickable { onResultSelected(result) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndoorTempSheet(
    initialTemp: Float,
    onDismiss: () -> Unit,
    onTempChanged: (Float) -> Unit
) {
    var temp by remember { mutableStateOf(initialTemp) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("室内基準温度の調整", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${temp.toInt()}℃", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AcUnit, contentDescription = "Cooler", tint = Color.Blue)
                Slider(
                    value = temp,
                    onValueChange = { temp = it },
                    onValueChangeFinished = { onTempChanged(temp) },
                    valueRange = 15f..30f,
                    steps = 14,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.WbSunny, contentDescription = "Warmer", tint = Color(0xFFFFA500))
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("完了")
            }
        }
    }
}

/**
 * 時間帯IDに応じたセグメントを取得
 */
private fun getSegmentsForTimeId(timeId: String): List<CasualForecastSegment> {
    return when (timeId) {
        // 今日カジュアル
        "spot" -> listOf(CasualForecastSegment.AFTERNOON) // 現在時刻付近
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
 * 今日/明日・時間帯に応じた天気データを取得
 */
private fun getWeatherDataForTimeSlot(
    weather: WeatherSnapshot?,
    isTomorrow: Boolean,
    timeId: String
): Triple<Double?, Int?, Int?> {
    if (weather == null) return Triple(null, null, null)

    // 今日で現在の天気がある場合は現在値を使用（より正確）
    if (!isTomorrow && timeId in listOf("spot", "half")) {
        return Triple(
            weather.apparentTemperatureCelsius,
            weather.humidityPercent,
            weather.weatherCode
        )
    }

    // それ以外はcasualSegmentSummariesから計算
    val targetDay = if (isTomorrow) CasualForecastDay.TOMORROW else CasualForecastDay.TODAY
    val targetSegments = getSegmentsForTimeId(timeId)

    val matchingSummaries = weather.casualSegmentSummaries
        .filter { it.day == targetDay && it.segment in targetSegments }

    return if (matchingSummaries.isNotEmpty()) {
        val avgTemp = matchingSummaries
            .map { it.averageApparentTemperatureCelsius }
            .average()
            .takeIf { !it.isNaN() }
        Triple(avgTemp, null, null) // 予報は湿度・天気コード未対応
    } else if (!isTomorrow) {
        // 今日でセグメントデータがない場合は現在値にフォールバック
        Triple(weather.apparentTemperatureCelsius, weather.humidityPercent, weather.weatherCode)
    } else {
        Triple(null, null, null)
    }
}

// --- Feedback Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackDialog(
    wornItems: List<UiClothingItem>,
    basisTemp: Double,
    onSubmit: (itemId: String, rating: WearFeedbackRating, minTemp: Double, maxTemp: Double) -> Unit,
    onDismiss: () -> Unit
) {
    // 各アイテムの状態を管理
    data class ItemFeedbackState(
        val rating: WearFeedbackRating?,
        val minTemp: Float,
        val maxTemp: Float,
        val originalMin: Float,
        val originalMax: Float
    )

    val itemStates = remember {
        mutableStateMapOf<String, ItemFeedbackState>().apply {
            wornItems.forEach { item ->
                val min = (item.comfortMinCelsius ?: 10.0).toFloat()
                val max = (item.comfortMaxCelsius ?: 30.0).toFloat()
                this[item.id] = ItemFeedbackState(null, min, max, min, max)
            }
        }
    }

    // アイテムタイプのラベル
    fun getItemLabel(type: ItemType): String = when (type) {
        ItemType.OUTER -> "アウター"
        ItemType.TOP -> "トップス"
        ItemType.BOTTOM -> "ボトムス"
    }

    // 評価に基づいて推奨温度を計算
    fun calculateSuggestedTemp(item: UiClothingItem, rating: WearFeedbackRating): Pair<Float, Float> {
        val currentMin = (item.comfortMinCelsius ?: 10.0).toFloat()
        val currentMax = (item.comfortMaxCelsius ?: 30.0).toFloat()

        return when (rating) {
            WearFeedbackRating.TOO_COLD -> {
                val newMin = maxOf(currentMin + 3f, basisTemp.toFloat() + 1f)
                Pair(newMin, currentMax)
            }
            WearFeedbackRating.TOO_WARM -> {
                val newMax = minOf(currentMax - 3f, basisTemp.toFloat() - 1f)
                Pair(currentMin, newMax)
            }
            WearFeedbackRating.JUST_RIGHT -> Pair(currentMin, currentMax)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ヘッダー
            Text(
                text = "今日の服装はどうでしたか？",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Text(
                text = "体感気温: ${basisTemp.toInt()}℃",
                style = MaterialTheme.typography.bodySmall,
                color = TextGrey,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // 各アイテムの評価カード
            wornItems.forEach { item ->
                val state = itemStates[item.id] ?: return@forEach

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // アイテム名
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = item.displayIcon),
                                contentDescription = null,
                                tint = item.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = getItemLabel(item.type),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGrey
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 3つの評価ボタン
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 寒かった
                            FeedbackRatingButton(
                                icon = Icons.Default.AcUnit,
                                label = "寒かった",
                                isSelected = state.rating == WearFeedbackRating.TOO_COLD,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val (newMin, newMax) = calculateSuggestedTemp(item, WearFeedbackRating.TOO_COLD)
                                    itemStates[item.id] = state.copy(
                                        rating = WearFeedbackRating.TOO_COLD,
                                        minTemp = newMin,
                                        maxTemp = newMax
                                    )
                                }
                            )

                            // 快適
                            FeedbackRatingButton(
                                icon = Icons.Default.ThumbUp,
                                label = "快適",
                                isSelected = state.rating == WearFeedbackRating.JUST_RIGHT,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    itemStates[item.id] = state.copy(
                                        rating = WearFeedbackRating.JUST_RIGHT,
                                        minTemp = state.originalMin,
                                        maxTemp = state.originalMax
                                    )
                                }
                            )

                            // 暑かった
                            FeedbackRatingButton(
                                icon = Icons.Default.WbSunny,
                                label = "暑かった",
                                isSelected = state.rating == WearFeedbackRating.TOO_WARM,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val (newMin, newMax) = calculateSuggestedTemp(item, WearFeedbackRating.TOO_WARM)
                                    itemStates[item.id] = state.copy(
                                        rating = WearFeedbackRating.TOO_WARM,
                                        minTemp = newMin,
                                        maxTemp = newMax
                                    )
                                }
                            )
                        }

                        // 温度調整スライダー（快適以外を選択時に表示）
                        if (state.rating != null && state.rating != WearFeedbackRating.JUST_RIGHT) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "適正温度を調整",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${state.minTemp.toInt()}℃ 〜 ${state.maxTemp.toInt()}℃",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // 変更前後の表示
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${state.originalMin.toInt()}℃〜${state.originalMax.toInt()}℃",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGrey
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(16.dp),
                                        tint = TextGrey
                                    )
                                    Text(
                                        text = "${state.minTemp.toInt()}℃〜${state.maxTemp.toInt()}℃",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // 下限温度スライダー
                                if (state.rating == WearFeedbackRating.TOO_COLD) {
                                    Column {
                                        Text(
                                            text = "下限温度: ${state.minTemp.toInt()}℃",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGrey
                                        )
                                        Slider(
                                            value = state.minTemp,
                                            onValueChange = { newMin ->
                                                if (newMin < state.maxTemp) {
                                                    itemStates[item.id] = state.copy(minTemp = newMin)
                                                }
                                            },
                                            valueRange = 0f..40f,
                                            steps = 39,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF2196F3),
                                                activeTrackColor = Color(0xFF2196F3)
                                            )
                                        )
                                    }
                                }

                                // 上限温度スライダー
                                if (state.rating == WearFeedbackRating.TOO_WARM) {
                                    Column {
                                        Text(
                                            text = "上限温度: ${state.maxTemp.toInt()}℃",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGrey
                                        )
                                        Slider(
                                            value = state.maxTemp,
                                            onValueChange = { newMax ->
                                                if (newMax > state.minTemp) {
                                                    itemStates[item.id] = state.copy(maxTemp = newMax)
                                                }
                                            },
                                            valueRange = 0f..40f,
                                            steps = 39,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFFFF9800),
                                                activeTrackColor = Color(0xFFFF9800)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ボタン行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("後で")
                }

                Button(
                    onClick = {
                        // 評価を送信（カスタム温度範囲付き）
                        itemStates.forEach { (itemId, state) ->
                            state.rating?.let { rating ->
                                onSubmit(
                                    itemId,
                                    rating,
                                    state.minTemp.toDouble(),
                                    state.maxTemp.toDouble()
                                )
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = itemStates.values.any { it.rating != null }
                ) {
                    Text("送信")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeedbackRatingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) tint.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (isSelected) tint else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) tint else TextGrey,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) tint else TextGrey
        )
    }
}

// --- Preview ---
/*
@Preview(showBackground = true, name = "Dashboard Light")
@Composable
fun DashboardScreenPreview() {
    LogiCloTheme(darkTheme = false) {
        DashboardScreen(viewModel = viewModel())
    }
}

@Preview(showBackground = true, name = "Dashboard Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardScreenDarkPreview() {
    LogiCloTheme(darkTheme = true) {
        DashboardScreen(viewModel = viewModel())
    }
}
*/

@Preview(showBackground = true)
@Composable
private fun LocationSheetPreview(){
    LogiCloTheme {
        LocationSearchSheet(
            searchState = LocationSearchState(),
            onDismiss = {},
            onQueryChanged = {},
            onResultSelected = {},
            onUseCurrentLocation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TempSheetPreview(){
    LogiCloTheme {
        IndoorTempSheet(initialTemp = 23f, onDismiss = {}, onTempChanged = {})
    }
}

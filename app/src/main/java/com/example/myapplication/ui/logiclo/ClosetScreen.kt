package com.example.myapplication.ui.logiclo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.logiclo.components.ClothingItemCard
import com.example.myapplication.ui.theme.LogiCloTheme
import com.example.myapplication.ui.theme.TextGrey
import java.util.*

// =============================================================================
// Screen 2: Closet
// =============================================================================

// フィルター状態を保持するデータクラス
data class ClothingFilter(
    val types: Set<ItemType> = emptySet(),
    val categories: Set<String> = emptySet(),
    val sleeveLengths: Set<SleeveLength> = emptySet(),
    val thicknesses: Set<Thickness> = emptySet(),
    val colors: Set<Color> = emptySet(),
    val tempMin: Float? = null,
    val tempMax: Float? = null
) {
    val isActive: Boolean
        get() = types.isNotEmpty() || categories.isNotEmpty() || sleeveLengths.isNotEmpty() ||
                thicknesses.isNotEmpty() || colors.isNotEmpty() || tempMin != null || tempMax != null

    val activeCount: Int
        get() = listOf(
            types.isNotEmpty(),
            categories.isNotEmpty(),
            sleeveLengths.isNotEmpty(),
            thicknesses.isNotEmpty(),
            colors.isNotEmpty(),
            tempMin != null || tempMax != null
        ).count { it }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(viewModel: LogiCloViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ClothingFilter()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<UiClothingItem?>(null) }
    val showAddItemSheet = editingItem != null
    val uiState by viewModel.uiState.collectAsState()

    // クローゼットにある服のみ表示（洗濯中は除外）し、検索クエリとフィルターでフィルタリング
    val filteredItems = remember(uiState.inventory, searchQuery, filter) {
        val closetItems = uiState.inventory.filter { !it.isDirty }
        closetItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() || run {
                val query = searchQuery.lowercase()
                item.name.lowercase().contains(query) ||
                item.brand.lowercase().contains(query) ||
                item.categoryKey.lowercase().contains(query)
            }
            val matchesType = filter.types.isEmpty() || item.type in filter.types
            val matchesCategory = filter.categories.isEmpty() || item.categoryKey in filter.categories
            val matchesSleeve = filter.sleeveLengths.isEmpty() || item.sleeveLength in filter.sleeveLengths
            val matchesThickness = filter.thicknesses.isEmpty() || item.thickness in filter.thicknesses
            val matchesColor = filter.colors.isEmpty() || filter.colors.any { filterColor ->
                item.color.value == filterColor.value
            }
            val matchesTemp = run {
                if (filter.tempMin == null && filter.tempMax == null) true
                else {
                    val itemMin = item.comfortMinCelsius ?: 10.0
                    val itemMax = item.comfortMaxCelsius ?: 30.0
                    val filterMin = filter.tempMin?.toDouble() ?: -10.0
                    val filterMax = filter.tempMax?.toDouble() ?: 45.0
                    // 範囲が重なっているかチェック
                    itemMin <= filterMax && itemMax >= filterMin
                }
            }
            matchesQuery && matchesType && matchesCategory && matchesSleeve &&
                matchesThickness && matchesColor && matchesTemp
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("服を追加") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Item") },
                onClick = { editingItem = UiClothingItem(
                    id = "",
                    name = "",
                    brand = "",
                    type = ItemType.TOP,
                    categoryKey = "t_shirt",
                    color = Color.White,
                    icon = com.example.myapplication.R.drawable.ic_clothing_top,
                    maxWears = 1
                ) },
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // ヘッダー
            Surface(
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "クローゼット",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 検索バーと絞り込みボタン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("服を検索...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // 絞り込みボタン
                Box {
                    FilledTonalButton(
                        onClick = { showFilterSheet = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("絞り込み")
                    }
                    // フィルターがアクティブな場合はバッジを表示
                    if (filter.isActive) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                        ) {
                            Text("${filter.activeCount}")
                        }
                    }
                }
            }

            // フィルターがアクティブな場合、リセットボタンを表示
            if (filter.isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "フィルター適用中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { filter = ClothingFilter() }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("リセット")
                    }
                }
            }

            // アイテム数の表示
            Text(
                text = "${filteredItems.size}件",
                style = MaterialTheme.typography.bodySmall,
                color = TextGrey,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextGrey,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isNotEmpty()) "「$searchQuery」に一致する服がありません"
                            else "クローゼットに服がありません",
                            color = TextGrey
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(filteredItems.size) { index ->
                        ClosetItemRow(
                            item = filteredItems[index],
                            onIncrementWear = { viewModel.incrementWearCount(filteredItems[index]) },
                            onMoveToLaundry = { viewModel.moveToLaundry(filteredItems[index]) },
                            onEditItem = { editingItem = filteredItems[index] },
                            onDeleteItem = { viewModel.deleteItem(filteredItems[index]) }
                        )
                    }
                }
            }
        }
    }

    if (showAddItemSheet) {
        AddItemSheet(
            itemToEdit = editingItem,
            viewModel = viewModel,
            onDismiss = { editingItem = null }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = filter,
            onFilterChanged = { filter = it },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun ClosetItemRow(
    item: UiClothingItem,
    onIncrementWear: () -> Unit,
    onMoveToLaundry: () -> Unit,
    onEditItem: () -> Unit,
    onDeleteItem: () -> Unit
) {
    ClothingItemCard(
        item = item,
        trailingContent = {
            Row {
                IconButton(onClick = onEditItem) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = TextGrey)
                }
                IconButton(onClick = onDeleteItem) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = TextGrey)
                }
            }
        },
        bottomContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onIncrementWear,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("着用+1", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onMoveToLaundry,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.LocalLaundryService,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("洗濯へ", fontSize = 12.sp)
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemSheet(
    itemToEdit: UiClothingItem?,
    viewModel: LogiCloViewModel,
    onDismiss: () -> Unit
) {
    val isEditing = itemToEdit?.id?.isNotEmpty() == true

    val categories = remember { listOf(
        mapOf("key" to "t_shirt", "icon" to com.example.myapplication.R.drawable.ic_clothing_top, "label" to "Tシャツ"),
        mapOf("key" to "polo", "icon" to com.example.myapplication.R.drawable.ic_clothing_top, "label" to "ポロシャツ"),
        mapOf("key" to "shirt", "icon" to com.example.myapplication.R.drawable.ic_clothing_top, "label" to "シャツ"),
        mapOf("key" to "knit", "icon" to com.example.myapplication.R.drawable.ic_clothing_top, "label" to "ニット"),
        mapOf("key" to "hoodie", "icon" to com.example.myapplication.R.drawable.ic_clothing_top, "label" to "パーカー"),
        mapOf("key" to "denim", "icon" to com.example.myapplication.R.drawable.ic_clothing_bottom, "label" to "デニム"),
        mapOf("key" to "slacks", "icon" to com.example.myapplication.R.drawable.ic_clothing_bottom, "label" to "スラックス"),
        mapOf("key" to "chino", "icon" to com.example.myapplication.R.drawable.ic_clothing_bottom, "label" to "チノパン"),
        mapOf("key" to "jacket", "icon" to com.example.myapplication.R.drawable.ic_clothing_outer, "label" to "ジャケット"),
        mapOf("key" to "coat", "icon" to com.example.myapplication.R.drawable.ic_clothing_outer, "label" to "コート"),
    )}
    val colors = remember { listOf(
        Color.White, Color.Black, Color.Gray, Color(0xFF1A237E),
        Color.LightGray, Color(0xFFD7CCC8),
        Color(0xFF558B2F), Color(0xFF795548), Color(0xFFE53935),
    )}

    var categoryKey by remember { mutableStateOf(itemToEdit?.categoryKey ?: "t_shirt") }
    var name by remember { mutableStateOf(itemToEdit?.name ?: "Tシャツ") }
    var brand by remember { mutableStateOf(itemToEdit?.brand ?: "") }
    var color by remember { mutableStateOf(itemToEdit?.color ?: Color.White) }
    var isAlwaysWash by remember { mutableStateOf(itemToEdit?.maxWears == 1) }
    var maxWears by remember { mutableStateOf(itemToEdit?.maxWears?.toFloat() ?: 1f) }
    var sleeve by remember { mutableStateOf(itemToEdit?.sleeveLength ?: SleeveLength.SHORT) }
    var thickness by remember { mutableStateOf(itemToEdit?.thickness ?: Thickness.NORMAL) }
    var customComfortMin by remember { mutableStateOf(itemToEdit?.comfortMinCelsius) }
    var customComfortMax by remember { mutableStateOf(itemToEdit?.comfortMaxCelsius) }
    var showTempRangeDialog by remember { mutableStateOf(false) }

    val onCategorySelected = { cat: Map<String, Any> ->
        categoryKey = cat["key"] as String
        name = cat["label"] as String
        val defaults = viewModel.getSmartDefaults(categoryKey)
        maxWears = (defaults["max"] as Int).toFloat()
        isAlwaysWash = defaults["always"] as Boolean
        sleeve = defaults["sleeve"] as SleeveLength
        thickness = defaults["thickness"] as Thickness
    }

    fun saveItem() {
        val defaults = viewModel.getSmartDefaults(categoryKey)
        val newItem = UiClothingItem(
            id = itemToEdit?.id ?: UUID.randomUUID().toString(),
            name = name,
            brand = brand,
            type = defaults["type"] as ItemType,
            categoryKey = categoryKey,
            color = color,
            icon = categories.first { it["key"] == categoryKey }["icon"] as Int,
            maxWears = if (isAlwaysWash) 1 else maxWears.toInt(),
            isDirty = false,
            cleaningType = CleaningType.HOME,
            sleeveLength = sleeve,
            thickness = thickness,
            comfortMinCelsius = customComfortMin,
            comfortMaxCelsius = customComfortMax,
        )
        viewModel.addItem(newItem)
        onDismiss()
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Scaffold(
            topBar = {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(if (isEditing) "服を編集" else "新しい服を登録", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart))
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Button(onClick = { saveItem() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Text("保存する", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Preview
                // 袖丈に応じたアイコンを計算
                val previewDefaults = viewModel.getSmartDefaults(categoryKey)
                val previewType = previewDefaults["type"] as ItemType
                val previewIcon = when (previewType) {
                    ItemType.TOP -> when (sleeve) {
                        SleeveLength.LONG -> com.example.myapplication.R.drawable.ic_clothing_top_long
                        else -> com.example.myapplication.R.drawable.ic_clothing_top
                    }
                    ItemType.OUTER -> com.example.myapplication.R.drawable.ic_clothing_outer
                    ItemType.BOTTOM -> com.example.myapplication.R.drawable.ic_clothing_bottom
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = previewIcon),
                        contentDescription = name,
                        tint = color,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(200.dp)
                    )
                }
                Spacer(Modifier.height(32.dp))

                Text("ブランド", style = MaterialTheme.typography.titleSmall, color = TextGrey)
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("例: UNIQLO") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(32.dp))

                Text("カテゴリ", style = MaterialTheme.typography.titleSmall, color = TextGrey)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(240.dp) // Adjust height as needed
                ) {
                    items(categories) { cat ->
                        val isSelected = categoryKey == cat["key"]
                        CategoryChip(
                            icon = cat["icon"] as Int,
                            label = cat["label"] as String,
                            isSelected = isSelected,
                            onTap = { onCategorySelected(cat) }
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))

                Text("色", style = MaterialTheme.typography.titleSmall, color = TextGrey)
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { c ->
                        val isSelected = color.value == c.value
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { color = c }
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = if(c == Color.Black) Color.White else Color.Black, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))

                Text("スペック", style = MaterialTheme.typography.titleSmall, color = TextGrey)
                SegmentedControl(
                    label = "袖丈",
                    value = sleeve,
                    options = mapOf(
                        SleeveLength.SHORT to "半袖",
                        SleeveLength.LONG to "長袖",
                        SleeveLength.NONE to "なし"
                    ),
                    onChanged = { sleeve = it }
                )
                SegmentedControl(
                    label = "厚さ",
                    value = thickness,
                    options = mapOf(
                        Thickness.THIN to "薄手",
                        Thickness.NORMAL to "普通",
                        Thickness.THICK to "厚手"
                    ),
                    onChanged = { thickness = it }
                )
                Spacer(Modifier.height(16.dp))

                // 気温パラメーター表示
                val defaults = viewModel.getSmartDefaults(categoryKey)
                val itemType = defaults["type"] as ItemType
                val defaultRange = calculateComfortRange(itemType, thickness, sleeve)
                val displayMin = customComfortMin ?: defaultRange.first
                val displayMax = customComfortMax ?: defaultRange.second
                val isCustomized = customComfortMin != null || customComfortMax != null

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTempRangeDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("適温範囲", style = MaterialTheme.typography.bodyMedium)
                                if (isCustomized) {
                                    Text(
                                        "カスタム設定中",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${displayMin.toInt()}℃ 〜 ${displayMax.toInt()}℃",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "編集",
                                tint = TextGrey,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 温度範囲編集ダイアログ
                if (showTempRangeDialog) {
                    TempRangeEditDialog(
                        currentMin = displayMin,
                        currentMax = displayMax,
                        defaultMin = defaultRange.first,
                        defaultMax = defaultRange.second,
                        onDismiss = { showTempRangeDialog = false },
                        onConfirm = { newMin, newMax ->
                            customComfortMin = newMin
                            customComfortMax = newMax
                            showTempRangeDialog = false
                        },
                        onReset = {
                            customComfortMin = null
                            customComfortMax = null
                            showTempRangeDialog = false
                        }
                    )
                }
                Spacer(Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("毎回洗う", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Switch(checked = isAlwaysWash, onCheckedChange = { isAlwaysWash = it })
                }
                AnimatedVisibility(!isAlwaysWash) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                           Text("洗濯頻度", color = TextGrey)
                           Text("${maxWears.toInt()}回ごと", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = maxWears,
                            onValueChange = { maxWears = it },
                            valueRange = 2f..20f,
                            steps = 18
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun CategoryChip(icon: Int, label: String, isSelected: Boolean, onTap: () -> Unit) {
    val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
    val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)

    Column(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onTap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(id = icon), contentDescription = label, tint = contentColor)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = contentColor)
    }
}

@Composable
private fun <T> SegmentedControl(label: String, value: T, options: Map<T, String>, onChanged: (T) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.entries.forEachIndexed { index, (opt, text) ->
                SegmentedButton(
                    selected = value == opt,
                    onClick = { onChanged(opt) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(text)
                }
            }
        }
    }
}


@Composable
private fun TempRangeEditDialog(
    currentMin: Double,
    currentMax: Double,
    defaultMin: Double,
    defaultMax: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
    onReset: () -> Unit
) {
    var minTemp by remember { mutableStateOf(currentMin.toFloat()) }
    var maxTemp by remember { mutableStateOf(currentMax.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("適温範囲を設定") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "この服に適した気温の範囲を設定してください。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGrey
                )

                // 最低気温
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("最低気温", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${minTemp.toInt()}℃",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = minTemp,
                        onValueChange = {
                            minTemp = it
                            if (maxTemp < minTemp) maxTemp = minTemp
                        },
                        valueRange = -5f..35f,
                        steps = 39
                    )
                }

                // 最高気温
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("最高気温", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${maxTemp.toInt()}℃",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = maxTemp,
                        onValueChange = {
                            maxTemp = it
                            if (minTemp > maxTemp) minTemp = maxTemp
                        },
                        valueRange = 0f..40f,
                        steps = 39
                    )
                }

                // デフォルト値の表示
                Text(
                    "デフォルト: ${defaultMin.toInt()}℃ 〜 ${defaultMax.toInt()}℃",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minTemp.toDouble(), maxTemp.toDouble()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text("リセット", color = MaterialTheme.colorScheme.secondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        }
    )
}

/**
 * 袖丈と厚さに基づいて適温範囲を計算
 */
private fun calculateComfortRange(
    type: ItemType,
    thickness: Thickness,
    sleeveLength: SleeveLength
): Pair<Double, Double> {
    val base = when (type) {
        ItemType.TOP, ItemType.OUTER -> when (thickness) {
            Thickness.THIN -> 18.0 to 33.0
            Thickness.NORMAL -> 12.0 to 28.0
            Thickness.THICK -> 5.0 to 20.0
        }
        ItemType.BOTTOM -> when (thickness) {
            Thickness.THIN -> 20.0 to 34.0
            Thickness.NORMAL -> 12.0 to 30.0
            Thickness.THICK -> 5.0 to 20.0
        }
    }

    val sleeveAdjust = when (sleeveLength) {
        SleeveLength.SHORT -> 2.0 to 2.0
        SleeveLength.NONE -> 0.0 to 3.0
        SleeveLength.LONG -> -2.0 to -1.0
    }

    return if (type == ItemType.BOTTOM) {
        base
    } else {
        (base.first + sleeveAdjust.first).coerceAtLeast(-5.0) to
                (base.second + sleeveAdjust.second).coerceAtMost(40.0)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    currentFilter: ClothingFilter,
    onFilterChanged: (ClothingFilter) -> Unit,
    onDismiss: () -> Unit
) {
    // ローカル状態（適用ボタンを押すまで変更を保持）
    var localFilter by remember { mutableStateOf(currentFilter) }

    val categories = remember { listOf(
        "t_shirt" to "Tシャツ",
        "polo" to "ポロシャツ",
        "shirt" to "シャツ",
        "knit" to "ニット",
        "hoodie" to "パーカー",
        "denim" to "デニム",
        "slacks" to "スラックス",
        "chino" to "チノパン",
        "jacket" to "ジャケット",
        "coat" to "コート"
    )}

    val filterColors = remember { listOf(
        Color.White to "白",
        Color.Black to "黒",
        Color.Gray to "グレー",
        Color(0xFF1A237E) to "ネイビー",
        Color.LightGray to "ライトグレー",
        Color(0xFFD7CCC8) to "ベージュ",
        Color(0xFF558B2F) to "緑",
        Color(0xFF795548) to "茶",
        Color(0xFFE53935) to "赤"
    )}

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("絞り込み", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row {
                        TextButton(onClick = { localFilter = ClothingFilter() }) {
                            Text("リセット")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text("キャンセル")
                        }
                        Button(
                            onClick = {
                                onFilterChanged(localFilter)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text("適用する", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // タイプ
                FilterSection(title = "タイプ") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ItemType.values().forEach { type ->
                            val isSelected = type in localFilter.types
                            val label = when (type) {
                                ItemType.OUTER -> "アウター"
                                ItemType.TOP -> "トップス"
                                ItemType.BOTTOM -> "ボトムス"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    localFilter = if (isSelected) {
                                        localFilter.copy(types = localFilter.types - type)
                                    } else {
                                        localFilter.copy(types = localFilter.types + type)
                                    }
                                },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Done, contentDescription = null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // カテゴリー
                FilterSection(title = "カテゴリー") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (key, label) ->
                            val isSelected = key in localFilter.categories
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    localFilter = if (isSelected) {
                                        localFilter.copy(categories = localFilter.categories - key)
                                    } else {
                                        localFilter.copy(categories = localFilter.categories + key)
                                    }
                                },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Done, contentDescription = null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // 袖丈
                FilterSection(title = "袖丈") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SleeveLength.values().forEach { sleeve ->
                            val isSelected = sleeve in localFilter.sleeveLengths
                            val label = when (sleeve) {
                                SleeveLength.SHORT -> "半袖"
                                SleeveLength.LONG -> "長袖"
                                SleeveLength.NONE -> "なし"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    localFilter = if (isSelected) {
                                        localFilter.copy(sleeveLengths = localFilter.sleeveLengths - sleeve)
                                    } else {
                                        localFilter.copy(sleeveLengths = localFilter.sleeveLengths + sleeve)
                                    }
                                },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Done, contentDescription = null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // 厚さ
                FilterSection(title = "厚さ") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Thickness.values().forEach { thick ->
                            val isSelected = thick in localFilter.thicknesses
                            val label = when (thick) {
                                Thickness.THIN -> "薄手"
                                Thickness.NORMAL -> "普通"
                                Thickness.THICK -> "厚手"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    localFilter = if (isSelected) {
                                        localFilter.copy(thicknesses = localFilter.thicknesses - thick)
                                    } else {
                                        localFilter.copy(thicknesses = localFilter.thicknesses + thick)
                                    }
                                },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Done, contentDescription = null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // 色
                FilterSection(title = "色") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterColors.forEach { (color, label) ->
                            val isSelected = localFilter.colors.any { it.value == color.value }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    localFilter = if (isSelected) {
                                        localFilter.copy(colors = localFilter.colors.filter { it.value != color.value }.toSet())
                                    } else {
                                        localFilter.copy(colors = localFilter.colors + color)
                                    }
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(1.dp, Color.Gray, CircleShape)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(label)
                                    }
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Done, contentDescription = null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // 適温範囲
                FilterSection(title = "適温範囲") {
                    val hasTempFilter = localFilter.tempMin != null || localFilter.tempMax != null
                    var tempEnabled by remember { mutableStateOf(hasTempFilter) }
                    var tempMin by remember { mutableStateOf(localFilter.tempMin ?: 0f) }
                    var tempMax by remember { mutableStateOf(localFilter.tempMax ?: 35f) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("気温で絞り込む")
                        Switch(
                            checked = tempEnabled,
                            onCheckedChange = {
                                tempEnabled = it
                                localFilter = if (it) {
                                    localFilter.copy(tempMin = tempMin, tempMax = tempMax)
                                } else {
                                    localFilter.copy(tempMin = null, tempMax = null)
                                }
                            }
                        )
                    }

                    AnimatedVisibility(tempEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${tempMin.toInt()}℃", fontWeight = FontWeight.Bold)
                                Text("〜")
                                Text("${tempMax.toInt()}℃", fontWeight = FontWeight.Bold)
                            }
                            RangeSlider(
                                value = tempMin..tempMax,
                                onValueChange = { range ->
                                    tempMin = range.start
                                    tempMax = range.endInclusive
                                    localFilter = localFilter.copy(tempMin = tempMin, tempMax = tempMax)
                                },
                                valueRange = -5f..40f,
                                steps = 44
                            )
                            Text(
                                "この気温範囲に適した服を表示",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGrey
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextGrey)
        content()
    }
}

/*
@Preview(showBackground = true, name="Closet Screen")
@Composable
fun ClosetScreenPreview() {
    LogiCloTheme {
        ClosetScreen(viewModel = viewModel())
    }
}

@Preview(showBackground = true, name="Closet Screen Dark")
@Composable
fun ClosetScreenDarkPreview() {
    LogiCloTheme(darkTheme = true) {
        ClosetScreen(viewModel = viewModel())
    }
}
*/
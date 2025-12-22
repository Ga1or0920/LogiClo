package com.example.myapplication.ui.logiclo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.logiclo.components.FilterChip
import com.example.myapplication.ui.theme.LogiCloTheme
import com.example.myapplication.ui.theme.TextGrey
import java.util.*

// =============================================================================
// Screen 2: Closet
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(viewModel: LogiCloViewModel) {
    var filterIndex by remember { mutableStateOf(0) }
    var editingItem by remember { mutableStateOf<UiClothingItem?>(null) }
    val showAddItemSheet = editingItem != null
    val uiState by viewModel.uiState.collectAsState()

    val items = when (filterIndex) {
        1 -> viewModel.closetItems
        2 -> uiState.inventory.filter { it.isDirty }
        else -> uiState.inventory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Closet", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(label = "すべて", isSelected = filterIndex == 0, onTap = { filterIndex = 0 })
                Spacer(Modifier.width(8.dp))
                FilterChip(label = "クローゼット", isSelected = filterIndex == 1, onTap = { filterIndex = 1 })
                Spacer(Modifier.width(8.dp))
                FilterChip(label = "洗濯・店", isSelected = filterIndex == 2, onTap = { filterIndex = 2 })
            }
            if(items.isEmpty()){
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                    Text("アイテムがありません", color = TextGrey)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items.size) { index ->
                        ClosetItemRow(
                            item = items[index],
                            onIncrementWear = { viewModel.incrementWearCount(items[index]) },
                            onMoveToLaundry = { viewModel.moveToLaundry(items[index]) },
                            onEditItem = { editingItem = items[index] },
                            onDeleteItem = { viewModel.deleteItem(items[index]) }
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
}

@Composable
fun ClosetItemRow(
    item: UiClothingItem, 
    onIncrementWear: () -> Unit,
    onMoveToLaundry: () -> Unit,
    onEditItem: () -> Unit,
    onDeleteItem: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = item.name,
                tint = item.color,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                if (item.brand.isNotEmpty()) {
                    Text(item.brand, style = MaterialTheme.typography.bodySmall, color = TextGrey)
                }
                Text(
                    if (item.isDirty) "洗濯待ち" else "残り${item.maxWears - item.currentWears}回",
                    style = MaterialTheme.typography.bodySmall,
                    color = if(item.isDirty) MaterialTheme.colorScheme.error else TextGrey
                )
                Row {
                    TextButton(onClick = onIncrementWear) {
                        Text("着用回数+1")
                    }
                    TextButton(onClick = onMoveToLaundry) {
                        Text("洗濯かごへ移動")
                    }
                }
            }
            IconButton(onClick = onEditItem) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Item")
            }
            IconButton(onClick = onDeleteItem) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Item")
            }
        }
    }
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
    var fit by remember { mutableStateOf(itemToEdit?.fit ?: FitType.REGULAR) }
    var sleeve by remember { mutableStateOf(itemToEdit?.sleeveLength ?: SleeveLength.SHORT) }
    var thickness by remember { mutableStateOf(itemToEdit?.thickness ?: Thickness.NORMAL) }
    var initialStatusIndex by remember { mutableStateOf(if (itemToEdit?.isDirty == true) 1 else 0) }

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
            isDirty = initialStatusIndex != 0,
            cleaningType = if (initialStatusIndex == 2) CleaningType.DRY else CleaningType.HOME,
            fit = fit,
            sleeveLength = sleeve,
            thickness = thickness,
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
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatusOptions(
                            selectedIndex = initialStatusIndex,
                            onTap = { initialStatusIndex = it }
                        )
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = categories.first { it["key"] == categoryKey } ["icon"] as Int),
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
                    label = "フィット",
                    value = fit,
                    options = FitType.values().associateWith { it.name },
                    onChanged = { fit = it }
                )
                SegmentedControl(
                    label = "袖丈",
                    value = sleeve,
                    options = SleeveLength.values().associateWith { it.name },
                    onChanged = { sleeve = it }
                )
                SegmentedControl(
                    label = "厚さ",
                    value = thickness,
                    options = Thickness.values().associateWith { it.name },
                    onChanged = { thickness = it }
                )
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
private fun StatusOptions(selectedIndex: Int, onTap: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val labels = listOf("📂 クローゼット", "🧺 洗濯カゴ", "🏬 店")
        labels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .clickable { onTap(index) },
                contentAlignment = Alignment.Center
            ) {
                val isSelected = selectedIndex == index
                val textStyle = if (isSelected) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) else MaterialTheme.typography.bodySmall
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = textStyle)
                }
            }
        }
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
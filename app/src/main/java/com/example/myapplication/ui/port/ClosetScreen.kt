package com.example.myapplication.ui.port

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(viewModel: AppViewModel) {
    var filterIndex by remember { mutableStateOf(0) }
    var showAddItemSheet by remember { mutableStateOf(false) }

    val items = when (filterIndex) {
        1 -> viewModel.getClosetItems()
        2 -> viewModel.inventory.filter { it.isDirty }
        else -> viewModel.inventory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Closet", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddItemSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("服を追加", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChipCustom("すべて", filterIndex == 0) { filterIndex = 0 }
                FilterChipCustom("クローゼット", filterIndex == 1) { filterIndex = 1 }
                FilterChipCustom("洗濯・店", filterIndex == 2) { filterIndex = 2 }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    ClosetItemCard(item = item, viewModel = viewModel)
                }
            }
        }
    }

    if (showAddItemSheet) {
        AddItemSheet(
            viewModel = viewModel,
            onDismiss = { showAddItemSheet = false }
        )
    }
}

@Composable
fun ClosetItemCard(item: ClothingItem, viewModel: AppViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        ListItem(
            leadingContent = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(item.icon, contentDescription = null, tint = item.color)
                    }
                }
            },
            headlineContent = { Text(item.name, fontWeight = FontWeight.Bold) },
            supportingContent = {
                Column {
                    if (item.brand.isNotEmpty()) {
                        Text(item.brand, fontSize = 10.sp, color = Color.Gray)
                    }
                    Text(if (item.isDirty) "洗濯待ち" else "残り${item.maxWears - item.currentWears}回")
                }
            },
            trailingContent = {
                IconButton(onClick = { viewModel.toggleItemStatus(item) }) {
                    Icon(
                        if (item.isDirty) Icons.Default.LocalLaundryService else Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = if (item.isDirty) AppColors.AccentRed else Color.Green
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemSheet(viewModel: AppViewModel, onDismiss: () -> Unit) {
    var categoryKey by remember { mutableStateOf("t_shirt") }
    var name by remember { mutableStateOf("Tシャツ") }
    var brand by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(Color.White) }
    var isAlwaysWash by remember { mutableStateOf(true) }
    var maxWears by remember { mutableStateOf(1f) }
    var fit by remember { mutableStateOf(FitType.Regular) }
    var sleeve by remember { mutableStateOf(SleeveLength.Short) }
    var thickness by remember { mutableStateOf(Thickness.Normal) }
    var initialStatusIndex by remember { mutableStateOf(0) }

    val categories = listOf(
        mapOf("key" to "t_shirt", "icon" to Icons.Default.Checkroom, "label" to "Tシャツ"),
        mapOf("key" to "polo", "icon" to Icons.Default.Checkroom, "label" to "ポロシャツ"),
        mapOf("key" to "shirt", "icon" to Icons.Default.Checkroom, "label" to "シャツ"),
        mapOf("key" to "knit", "icon" to Icons.Default.Checkroom, "label" to "ニット"),
        mapOf("key" to "hoodie", "icon" to Icons.Default.Checkroom, "label" to "パーカー"),
        mapOf("key" to "denim", "icon" to Icons.Default.AccessibilityNew, "label" to "デニム"),
        mapOf("key" to "slacks", "icon" to Icons.Default.AccessibilityNew, "label" to "スラックス"),
        mapOf("key" to "chino", "icon" to Icons.Default.AccessibilityNew, "label" to "チノパン"),
        mapOf("key" to "jacket", "icon" to Icons.Default.AllOut, "label" to "ジャケット"),
        mapOf("key" to "coat", "icon" to Icons.Default.AllOut, "label" to "コート")
    )

    val colors = listOf(
        Color.White, Color.Black, Color.Gray, Color(0xFF1A237E),
        Color(0xFF81D4FA), Color(0xFFD7CCC8),
        Color(0xFF558B2F), Color(0xFF795548), Color(0xFFE53935)
    )

    // Effect to update defaults when category changes
    LaunchedEffect(categoryKey) {
        val defaults = viewModel.getSmartDefaults(categoryKey)
        maxWears = (defaults["max"] as Int).toFloat()
        isAlwaysWash = defaults["always"] as Boolean
        sleeve = defaults["sleeve"] as SleeveLength
        thickness = defaults["thickness"] as Thickness
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("新しい服を登録", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
            HorizontalDivider()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(24.dp)
            ) {
                // Preview
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val icon = categories.find { it["key"] == categoryKey }?.get("icon") as? ImageVector
                                ?: Icons.Default.Checkroom
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.width(160.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))

                // Brand
                Text("ブランド", fontWeight = FontWeight.Bold, color = AppColors.TextGrey)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    placeholder = { Text("例: UNIQLO") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Categories
                Text("カテゴリ", fontWeight = FontWeight.Bold, color = AppColors.TextGrey)
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = categoryKey == cat["key"]
                        val containerColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.surfaceVariant
                        val contentColor = if (isSelected) Color.White else Color.Gray

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = containerColor,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray) else null,
                            modifier = Modifier
                                .size(64.dp)
                                .clickable {
                                    categoryKey = cat["key"] as String
                                    name = cat["label"] as String
                                }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(cat["icon"] as ImageVector, contentDescription = null, tint = contentColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(cat["label"] as String, fontSize = 10.sp, color = contentColor)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                // Colors
                Text("色", fontWeight = FontWeight.Bold, color = AppColors.TextGrey)
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    colors.forEach { c ->
                        val isSelected = color.value == c.value
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { color = c },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = if (c.luminance() > 0.5) Color.Black else Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                // Specs
                Text("スペック", fontWeight = FontWeight.Bold, color = AppColors.TextGrey)
                Spacer(modifier = Modifier.height(12.dp))
                SegmentedControlRow("フィット", fit, listOf(FitType.Slim, FitType.Regular, FitType.Loose)) { fit = it }
                Spacer(modifier = Modifier.height(12.dp))
                SegmentedControlRow("袖丈", sleeve, listOf(SleeveLength.Short, SleeveLength.Long, SleeveLength.None)) { sleeve = it }
                Spacer(modifier = Modifier.height(12.dp))
                SegmentedControlRow("厚さ", thickness, listOf(Thickness.Thin, Thickness.Normal, Thickness.Thick)) { thickness = it }

                Spacer(modifier = Modifier.height(32.dp))

                // Wash
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("毎回洗う", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Switch(checked = isAlwaysWash, onCheckedChange = { isAlwaysWash = it })
                }
                if (!isAlwaysWash) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("洗濯頻度", color = AppColors.TextGrey)
                        Text("${maxWears.toInt()}回ごと", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = maxWears,
                        onValueChange = { maxWears = it },
                        valueRange = 2f..20f,
                        steps = 18
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Save Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        listOf("📂 クローゼット", "🧺 洗濯カゴ", "🏬 店").forEachIndexed { index, label ->
                            val isSelected = index == initialStatusIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { initialStatusIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val defaults = viewModel.getSmartDefaults(categoryKey)
                        val newItem = ClothingItem(
                            name = name,
                            brand = brand,
                            type = defaults["type"] as ItemType,
                            color = color,
                            icon = categories.find { it["key"] == categoryKey }!!["icon"] as ImageVector,
                            maxWears = if (isAlwaysWash) 1 else maxWears.toInt(),
                            isDirty = initialStatusIndex != 0,
                            cleaningType = if (initialStatusIndex == 2) CleaningType.Dry else CleaningType.Home,
                            fit = fit,
                            sleeveLength = sleeve,
                            thickness = thickness,
                            currentWears = 0 // Initial logic was slightly weird, resetting to 0 makes sense or max if dirty? logic says if dirty, it's dirty.
                        )
                        // If starting as dirty/shop, usually implies it needs wash or is out.
                        if (initialStatusIndex != 0) {
                             newItem.currentWears = newItem.maxWears // simulate used up
                        }
                        
                        viewModel.addItem(newItem)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("保存する", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedControlRow(label: String, selected: T, options: List<T>, onSelect: (T) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, item ->
                 SegmentedButton(
                     shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                     onClick = { onSelect(item) },
                     selected = item == selected
                 ) {
                     Text(item.toString())
                 }
            }
        }
    }
}

package com.example.myapplication.ui.port

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    var showLocationSheet by remember { mutableStateOf(false) }
    var showIndoorTempSheet by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomAction(viewModel)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            DashboardHeader(
                viewModel = viewModel,
                onLocationClick = { showLocationSheet = true }
            )
            WeatherInfo(
                viewModel = viewModel,
                onIndoorTempClick = { showIndoorTempSheet = true }
            )
            Box(modifier = Modifier.weight(1f)) {
                MainContent(viewModel)
            }
        }
    }

    if (showLocationSheet) {
        LocationSearchSheet(
            viewModel = viewModel,
            onDismiss = { showLocationSheet = false }
        )
    }

    if (showIndoorTempSheet) {
        IndoorTempSheet(
            viewModel = viewModel,
            onDismiss = { showIndoorTempSheet = false }
        )
    }
}

@Composable
fun DashboardHeader(viewModel: AppViewModel, onLocationClick: () -> Unit) {
    val isTomorrow by viewModel.isTomorrow
    val selectedMode by viewModel.selectedMode
    val selectedTimeLabel by viewModel.selectedTimeLabel
    val selectedTimeId by viewModel.selectedTimeId
    val selectedEnv by viewModel.selectedEnv
    val currentLocationName by viewModel.currentLocationName
    val isLocationCustom by viewModel.isLocationCustom

    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlidingToggle(
                    height = 36.0,
                    labels = listOf("今日", "明日"),
                    selectedIndex = if (isTomorrow) 1 else 0,
                    onChanged = { viewModel.setDate(it == 1) },
                    modifier = Modifier.width(140.dp)
                )

                TextButton(
                    onClick = onLocationClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isLocationCustom) AppColors.AccentBlue else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(currentLocationName, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(4f)) {
                    SlidingToggle(
                        height = 40.0,
                        labels = listOf("Casual", "Office"),
                        icons = listOf(Icons.Outlined.Home, Icons.Default.BusinessCenter),
                        selectedIndex = if (selectedMode == AppMode.Casual) 0 else 1,
                        onChanged = { viewModel.setMode(if (it == 0) AppMode.Casual else AppMode.Office) }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(3f)) {
                    TimeMenuButton(
                        label = selectedTimeLabel,
                        selectedId = selectedTimeId,
                        options = getTimeOptions(selectedMode, isTomorrow),
                        onSelected = { id, label -> viewModel.setTimeSelection(id, label) }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconToggleGroup(
                    selectedIndex = selectedEnv.ordinal,
                    onChanged = { viewModel.setEnv(EnvMode.values()[it]) },
                    icons = listOf(Icons.Default.Park, Icons.Default.DirectionsCar, Icons.Default.Chair)
                )
            }
        }
    }
}

fun getTimeOptions(mode: AppMode, isTomorrow: Boolean): List<Map<String, String>> {
    return if (mode == AppMode.Casual) {
        if (isTomorrow) {
            listOf(
                mapOf("id" to "daytime", "label" to "☀️ 日中 (10-17)"),
                mapOf("id" to "night", "label" to "🌙 夜間 (17-23)"),
                mapOf("id" to "allday", "label" to "📅 終日 (08-22)")
            )
        } else {
            listOf(
                mapOf("id" to "spot", "label" to "⏱️ スポット (+3h)"),
                mapOf("id" to "half", "label" to "🌤️ 半日 (+6h)"),
                mapOf("id" to "full", "label" to "📅 終日 (〜22時)")
            )
        }
    } else {
        listOf(
            mapOf("id" to "day", "label" to "☀️ 日勤 (9-18)"),
            mapOf("id" to "evening", "label" to "🌆 夕勤 (17-22)"),
            mapOf("id" to "night", "label" to "🌙 夜勤 (22-07)")
        )
    }
}

@Composable
fun WeatherInfo(viewModel: AppViewModel, onIndoorTempClick: () -> Unit) {
    val selectedEnv by viewModel.selectedEnv
    val indoorTargetTemp by viewModel.indoorTargetTemp
    val isTomorrow by viewModel.isTomorrow

    val isIndoor = selectedEnv == EnvMode.Indoor
    val displayTemp = if (isIndoor) "${indoorTargetTemp.toInt()}℃" else "22℃"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isIndoor, onClick = onIndoorTempClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isTomorrow) "明日の神戸" else "今日の神戸",
            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextGrey)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isIndoor) Icons.Default.Thermostat else Icons.Default.Cloud,
                contentDescription = null,
                tint = Color.Magenta, // Using Magenta as close enough to orange in default palette or custom
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${if (isIndoor) "室内設定" else "体感"} $displayTemp ${if (isIndoor) "" else "(湿度 45%)"}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
            )
            if (isIndoor) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: AppViewModel) {
    val suggestedTop by viewModel.suggestedTop
    val suggestedBottom by viewModel.suggestedBottom
    val suggestedOuter by viewModel.suggestedOuter

    if (suggestedTop == null || suggestedBottom == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Magenta)
            Spacer(modifier = Modifier.height(16.dp))
            Text("コーデが組めません", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {}) {
                Text("⚠️ 洗濯カゴから探す")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            suggestedOuter?.let {
                OutfitCardItem(item = it, label = "Outer", onRemove = { viewModel.markAsActuallyDirty(it) })
            }
            OutfitCardItem(item = suggestedTop!!, label = "Top", onRemove = { viewModel.markAsActuallyDirty(suggestedTop!!) })
            OutfitCardItem(item = suggestedBottom!!, label = "Bottom", onRemove = { viewModel.markAsActuallyDirty(suggestedBottom!!) })
        }
    }
}

@Composable
fun BottomAction(viewModel: AppViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // For Snackbars, we usually use ScaffoldState, but here we'll simplify or use Toast for this demo if Scaffold not fully propagated
    // In M3 Scaffold, we pass SnackbarHost. For now, let's use a simple Toast or assume SnackbarHost is in parent.
    // Ideally we should have a SnackbarHostState in top level.
    // Let's just print to console or simple Toast for the prototype action.
    
    Surface(
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    val msg = viewModel.wearCurrentOutfit()
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
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
                Text("これを着る (Wear This)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(viewModel: AppViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(min = 400.dp)
        ) {
            Text(
                "場所を変更",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isEmpty()) {
                        results = emptyList()
                    } else {
                        results = listOf("$it (施設名)", "${it}駅 (駅)", "${it}市 (住所)")
                    }
                },
                placeholder = { Text("地名・施設名 (例: ユニバ)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            ListItem(
                headlineContent = { Text("現在地を使う (GPS)") },
                leadingContent = { Icon(Icons.Default.MyLocation, contentDescription = null, tint = AppColors.AccentBlue) },
                modifier = Modifier.clickable {
                    viewModel.setLocation("神戸市 (現在地)", false)
                    onDismiss()
                }
            )
            HorizontalDivider()
            
            if (results.isEmpty()) {
                ListItem(leadingContent = { Icon(Icons.Default.History, contentDescription = null) }, headlineContent = { Text("ユニバーサル・スタジオ・ジャパン") })
                ListItem(leadingContent = { Icon(Icons.Default.History, contentDescription = null) }, headlineContent = { Text("東京駅") })
            } else {
                results.forEach { res ->
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                        headlineContent = { Text(res) },
                        modifier = Modifier.clickable {
                            viewModel.setLocation(res, true)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndoorTempSheet(viewModel: AppViewModel, onDismiss: () -> Unit) {
    var temp by remember { mutableStateOf(viewModel.indoorTargetTemp.value) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("室内基準温度の調整", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AcUnit, contentDescription = "Cooler", tint = AppColors.AccentBlue)
                Slider(
                    value = temp.toFloat(),
                    onValueChange = { 
                        temp = it.toDouble()
                        viewModel.setIndoorTemp(it.toDouble())
                    },
                    valueRange = 18f..28f,
                    steps = 9,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color.Magenta)
            }
            Text("${temp.toInt()}℃", fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
/*
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LogiCloApp()
}
*/
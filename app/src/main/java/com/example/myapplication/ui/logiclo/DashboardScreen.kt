package com.example.myapplication.ui.logiclo

import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.logiclo.components.*
import com.example.myapplication.ui.theme.LogiCloTheme
import com.example.myapplication.ui.theme.TextGrey

// =============================================================================
// Screen 1: Dashboard
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: LogiCloViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showLocationSheet by remember { mutableStateOf(false) }
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
                onLocationClick = { showLocationSheet = true },
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
                        snackbarHostState.showSnackbar(msg)
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
                WeatherInfo(
                    isTomorrow = uiState.isTomorrow,
                    selectedEnv = uiState.selectedEnv,
                    indoorTargetTemp = uiState.indoorTargetTemp,
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
                        Text("クローゼットに服を追加してください", style = MaterialTheme.typography.bodyMedium, color = TextGrey)
                    }
                }
            } else {
                uiState.suggestedOuter?.let {
                    item { OutfitCardItem(item = it, label = "Outer", onRemove = { viewModel.markAsActuallyDirty(it) }) }
                }
                uiState.suggestedTop?.let {
                    item { OutfitCardItem(item = it, label = "Top", onRemove = { viewModel.markAsActuallyDirty(it) }) }
                }
                uiState.suggestedBottom?.let {
                    item { OutfitCardItem(item = it, label = "Bottom", onRemove = { viewModel.markAsActuallyDirty(it) }) }
                }
            }
        }
    }

    if (showLocationSheet) {
        LocationSearchSheet(
            onDismiss = { showLocationSheet = false },
            onLocationSelected = { name, isCustom ->
                viewModel.setLocation(name, isCustom)
                showLocationSheet = false
            }
        )
    }

    if (showTempSheet) {
        IndoorTempSheet(
            initialTemp = uiState.indoorTargetTemp,
            onDismiss = { showTempSheet = false },
            onTempChanged = { viewModel.setIndoorTemp(it) }
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
                    "spot" to "⏱️ スポット (+3h)",
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
                    labels = listOf("Casual", "Office"),
                    icons = listOf(Icons.Default.Home, Icons.Default.BusinessCenter),
                    selectedIndex = if (uiState.selectedMode == AppMode.CASUAL) 0 else 1,
                    onChanged = { onModeChange(if (it == 0) AppMode.CASUAL else AppMode.OFFICE) },
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
                    icons = listOf(Icons.Default.Park, Icons.Default.DirectionsCar, Icons.Default.Chair)
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
    onIndoorClick: () -> Unit
) {
    val isIndoor = selectedEnv == EnvMode.INDOOR
    val displayTemp = if (isIndoor) "${indoorTargetTemp.toInt()}℃" else "22℃"

    val modifier = if (isIndoor) Modifier.clickable(onClick = onIndoorClick) else Modifier

    Column(
        modifier = modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isTomorrow) "明日の神戸" else "今日の神戸",
            style = MaterialTheme.typography.bodySmall,
            color = TextGrey
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isIndoor) Icons.Default.Thermostat else Icons.Default.WbCloudy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${if (isIndoor) "室内設定" else "体感"} $displayTemp ${if (isIndoor) "" else "(湿度 45%)"}",
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
            Text("これを着る (Wear This)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSearchSheet(
    onDismiss: () -> Unit,
    onLocationSelected: (name: String, isCustom: Boolean) -> Unit
) {
    val mockResults = remember {
        listOf(
            "ユニバーサル・スタジオ・ジャパン",
            "東京駅",
            "神戸市",
            "大阪市"
        )
    }
    var query by remember { mutableStateOf("") }
    val filteredResults = if (query.isEmpty()) mockResults else mockResults.filter { it.contains(query, ignoreCase = true) }

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
                Text("場所を変更", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Search Field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("地名・施設名 (例: ユニバ)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Options
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text("現在地を使う (GPS)") },
                        leadingContent = { Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.clickable { onLocationSelected("神戸市 (現在地)", false) }
                    )
                }
                items(filteredResults.size) { index ->
                    val result = filteredResults[index]
                    ListItem(
                        headlineContent = { Text(result) },
                        leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                        modifier = Modifier.clickable { onLocationSelected(result, true) }
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
                    valueRange = 18f..28f,
                    steps = 9,
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
        LocationSearchSheet(onDismiss = {}, onLocationSelected = {_,_ ->})
    }
}

@Preview(showBackground = true)
@Composable
private fun TempSheetPreview(){
    LogiCloTheme {
        IndoorTempSheet(initialTemp = 23f, onDismiss = {}, onTempChanged = {})
    }
}
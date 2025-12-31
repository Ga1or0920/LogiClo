package com.example.myapplication.ui.logiclo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.domain.usecase.DebugFeedbackAlarmScheduler
import com.example.myapplication.domain.worker.WearFeedbackReminderWorker
import com.example.myapplication.ui.providers.rememberAppContainer
import com.example.myapplication.ui.theme.LogiCloTheme
import com.example.myapplication.ui.theme.TextGrey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// =============================================================================
// Screen 4: Settings
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LogiCloViewModel,
    onNavigateToDashboard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
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
                        "設定",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
            item { SectionHeader("表示") }
            item {
                ThemeSetting(
                    currentTheme = uiState.themeMode,
                    onThemeChange = { viewModel.setThemeMode(it) }
                )
            }

            item { SectionHeader("データ管理") }
            item {
                SettingItem(
                    title = "全データをリセット",
                    icon = Icons.Default.DeleteOutline,
                    isDestructive = true,
                    onClick = { showResetDialog = true }
                )
            }

            item { SectionHeader("デバッグ") }
            item {
                DebugTemperatureOverride(
                    currentOverride = uiState.debugTemperatureOverride,
                    onOverrideChange = { viewModel.setDebugTemperatureOverride(it) }
                )
            }
            item {
                DebugWeatherOverride(
                    currentOverride = uiState.debugWeatherCodeOverride,
                    onOverrideChange = { viewModel.setDebugWeatherCodeOverride(it) }
                )
            }
            item {
                DebugNotificationTrigger()
            }
            item {
                DebugFeedbackTrigger(
                    onTrigger = {
                        viewModel.triggerFeedbackDialog()
                        onNavigateToDashboard()
                    }
                )
            }
            item {
                DebugAlarmSchedulerToggle()
            }
            }
        }
    }

    if (showResetDialog) {
        ResetDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                viewModel.resetAllData()
                showResetDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = TextGrey,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(title, color = color) },
        leadingContent = { Icon(icon, contentDescription = title, tint = color) },
        trailingContent = { if (!isDestructive) Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun ThemeSetting(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    fun ThemeMode.toDisplayName(): String = when (this) {
        ThemeMode.LIGHT -> "ライト"
        ThemeMode.DARK -> "ダーク"
        ThemeMode.SYSTEM -> "システム"
    }

    ListItem(
        headlineContent = { Text("テーマ") },
        leadingContent = { Icon(Icons.Default.Brightness6, contentDescription = "Theme") },
        trailingContent = {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(currentTheme.toDisplayName())
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open theme options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ThemeMode.values().forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.toDisplayName()) },
                            onClick = {
                                onThemeChange(theme)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DebugTemperatureOverride(
    currentOverride: Double?,
    onOverrideChange: (Double?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        null to "オフ (実際の気温を使用)",
        -5.0 to "-5°C (極寒)",
        0.0 to "0°C (厳寒)",
        5.0 to "5°C (寒い)",
        10.0 to "10°C (やや寒い)",
        15.0 to "15°C (涼しい)",
        20.0 to "20°C (快適)",
        25.0 to "25°C (暖かい)",
        30.0 to "30°C (暑い)",
        35.0 to "35°C (猛暑)"
    )

    val currentLabel = options.find { it.first == currentOverride }?.second
        ?: currentOverride?.let { "${it.toInt()}°C" }
        ?: "オフ"

    ListItem(
        headlineContent = { Text("体感気温オーバーライド") },
        supportingContent = {
            if (currentOverride != null) {
                Text("すべての時間帯に適用されます", color = MaterialTheme.colorScheme.primary)
            }
        },
        leadingContent = { Icon(Icons.Default.Thermostat, contentDescription = "Temperature") },
        trailingContent = {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(currentLabel)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onOverrideChange(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DebugWeatherOverride(
    currentOverride: Int?,
    onOverrideChange: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        null to "オフ (実際の天気を使用)",
        0 to "☀️ 晴れ",
        3 to "☁️ 曇り",
        63 to "🌧️ 雨",
        73 to "❄️ 雪"
    )

    val currentLabel = options.find { it.first == currentOverride }?.second
        ?: currentOverride?.let { "コード: $it" }
        ?: "オフ"

    ListItem(
        headlineContent = { Text("天気オーバーライド") },
        supportingContent = {
            if (currentOverride != null) {
                Text("すべての時間帯に適用されます", color = MaterialTheme.colorScheme.primary)
            }
        },
        leadingContent = { Icon(Icons.Default.Cloud, contentDescription = "Weather") },
        trailingContent = {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(currentLabel)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onOverrideChange(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DebugNotificationTrigger() {
    val context = LocalContext.current
    val appContainer = rememberAppContainer()
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var hasPendingEntry by remember { mutableStateOf<Boolean?>(null) }

    // 保留中のエントリがあるか確認
    LaunchedEffect(Unit) {
        val pending = appContainer.wearFeedbackRepository.getLatestPending()
        hasPendingEntry = pending != null
    }

    Column {
        ListItem(
            headlineContent = { Text("通知をテスト送信") },
            supportingContent = {
                Column {
                    Text("フィードバック通知を今すぐ送信します")
                    when (hasPendingEntry) {
                        true -> Text("✓ 保留中の着用記録があります", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        false -> Text("⚠ 着用記録がありません（自動作成されます）", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        null -> Text("確認中...", style = MaterialTheme.typography.bodySmall)
                    }
                    statusMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = "Notification") },
            trailingContent = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            // 保留中のエントリがなければダミーを作成
                            val pending = appContainer.wearFeedbackRepository.getLatestPending()
                            if (pending == null) {
                                // ダミーの着用記録を作成（実際のアイテムIDを使用）
                                val items = appContainer.closetRepository.observeAll().first()
                                val topItem = items.find { it.type.name == "TOP" }
                                val bottomItem = items.find { it.type.name == "BOTTOM" }
                                appContainer.wearFeedbackRepository.recordWear(
                                    topItemId = topItem?.id,
                                    bottomItemId = bottomItem?.id
                                )
                                statusMessage = "テスト用着用記録を作成しました"
                                hasPendingEntry = true
                            }

                            // 通知をトリガー
                            val request = OneTimeWorkRequestBuilder<WearFeedbackReminderWorker>().build()
                            WorkManager.getInstance(context).enqueue(request)
                            statusMessage = "通知を送信しました"
                        } catch (e: Exception) {
                            statusMessage = "エラー: ${e.message}"
                        }
                    }
                }) {
                    Text("送信")
                }
            }
        )
    }
}

@Composable
private fun DebugFeedbackTrigger(
    onTrigger: () -> Unit
) {
    ListItem(
        headlineContent = { Text("フィードバックダイアログを表示") },
        supportingContent = { Text("ホーム画面に移動してダイアログを表示します") },
        leadingContent = { Icon(Icons.Default.RateReview, contentDescription = "Feedback") },
        trailingContent = {
            TextButton(onClick = onTrigger) {
                Text("開く")
            }
        }
    )
}

@Composable
private fun DebugAlarmSchedulerToggle() {
    val context = LocalContext.current
    val scheduler = remember { DebugFeedbackAlarmScheduler(context) }
    var isEnabled by remember { mutableStateOf(scheduler.isScheduled()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    ListItem(
        headlineContent = { Text("システム時刻連動モード") },
        supportingContent = {
            Column {
                Text("ONにするとシステム時刻が21:00になった時点で通知が届きます")
                Text("※システム時刻を手動で変更してテストできます", style = MaterialTheme.typography.bodySmall, color = TextGrey)
                if (isEnabled) {
                    Text("✓ 次の21:00にアラーム設定済み", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        leadingContent = { Icon(Icons.Default.Alarm, contentDescription = "Alarm") },
        trailingContent = {
            Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        scheduler.scheduleAt2100()
                        statusMessage = "21:00にアラームを設定しました"
                    } else {
                        scheduler.cancel()
                        statusMessage = "アラームをキャンセルしました"
                    }
                    isEnabled = enabled
                }
            )
        }
    )
}

@Composable
private fun ResetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("リセットしますか？") },
        text = { Text("全ての服を「クローゼTット」に戻し、着用回数を0にします。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("リセット")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

/*
@Preview(showBackground = true, name = "Settings Screen")
@Composable
fun SettingsScreenPreview() {
    LogiCloTheme {
        SettingsScreen(viewModel = viewModel())
    }
}

@Preview(showBackground = true, name = "Settings Screen Dark")
@Composable
fun SettingsScreenDarkPreview() {
    LogiCloTheme(darkTheme = true) {
        SettingsScreen(viewModel = viewModel())
    }
}
*/
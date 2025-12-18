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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.LogiCloTheme
import com.example.myapplication.ui.theme.TextGrey

// =============================================================================
// Screen 4: Settings
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: LogiCloViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item { SectionHeader("表示") }
            item {
                ThemeSetting(
                    currentTheme = uiState.themeMode,
                    onThemeChange = { viewModel.setThemeMode(it) }
                )
            }

            item { SectionHeader("ユーザー設定") }
            item {
                SettingItem(
                    title = "暑がり/寒がり補正",
                    icon = Icons.Default.Thermostat,
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingItem(
                    title = "色合わせルール",
                    icon = Icons.Default.Palette,
                    onClick = { /* TODO */ }
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

    ListItem(
        headlineContent = { Text("テーマ") },
        leadingContent = { Icon(Icons.Default.Brightness6, contentDescription = "Theme") },
        trailingContent = {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(currentTheme.name)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open theme options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ThemeMode.values().forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.name) },
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
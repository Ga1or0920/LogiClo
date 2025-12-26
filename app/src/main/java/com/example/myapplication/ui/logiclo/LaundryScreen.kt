package com.example.myapplication.ui.logiclo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DirtyLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.LogiCloTheme
import com.example.myapplication.ui.theme.TextGrey
import kotlinx.coroutines.launch

// =============================================================================
// Screen 3: Laundry
// =============================================================================

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LaundryScreen(viewModel: LogiCloViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("🏠 自宅洗い", "🏬 クリーニング")

    val dirtyHomeItems = uiState.inventory.filter { it.isDirty && it.cleaningType == CleaningType.HOME }
    val dirtyDryItems = uiState.inventory.filter { it.isDirty && it.cleaningType == CleaningType.DRY }

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
                        "洗濯",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> LaundryList(
                        items = dirtyHomeItems,
                        actionLabel = "☑を洗濯する",
                        onAction = { ids -> viewModel.washSelectedItems(ids) },
                        emptyMsg = "洗濯カゴは空です✨"
                    )
                    1 -> LaundryList(
                        items = dirtyDryItems,
                        actionLabel = "店に出す / 受け取る",
                        onAction = { /* Implement dry cleaning logic */ },
                        emptyMsg = "クリーニング予定はありません"
                    )
                }
            }
        }
    }
}

@Composable
private fun LaundryList(
    items: List<UiClothingItem>,
    actionLabel: String,
    onAction: (List<String>) -> Unit,
    emptyMsg: String
) {
    val checkedState = remember { mutableStateMapOf<String, Boolean>() }
    items.forEach { item ->
        checkedState.putIfAbsent(item.id, true)
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextGrey)
                Spacer(Modifier.height(16.dp))
                Text(emptyMsg, color = TextGrey)
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // チェックボックス
                        Checkbox(
                            checked = checkedState[item.id] ?: false,
                            onCheckedChange = { checkedState[item.id] = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        // アイコン
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = item.displayIcon),
                                contentDescription = item.name,
                                tint = item.color,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        // テキスト
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.brand.isNotEmpty()) {
                                Text(
                                    item.brand,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGrey
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "洗濯待ち",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = { 
                    val selectedIds = checkedState.filter { it.value }.keys.toList()
                    onAction(selectedIds)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/*
@Preview(showBackground = true, name = "Laundry Screen")
@Composable
fun LaundryScreenPreview() {
    val viewModel = viewModel<LogiCloViewModel>()
    // Force some items into laundry for preview
    viewModel.uiState.value.inventory.take(2).forEach { it.isDirty = true }
    LogiCloTheme {
        LaundryScreen(viewModel = viewModel)
    }
}

@Preview(showBackground = true, name = "Laundry Screen Dark")
@Composable
fun LaundryScreenDarkPreview() {
    val viewModel = viewModel<LogiCloViewModel>()
    viewModel.uiState.value.inventory.take(2).forEach { it.isDirty = true }
    LogiCloTheme(darkTheme = true) {
        LaundryScreen(viewModel = viewModel)
    }
}

@Preview(showBackground = true, name = "Laundry Screen Empty")
@Composable
fun LaundryScreenEmptyPreview() {
    val viewModel = viewModel<LogiCloViewModel>()
    viewModel.uiState.value.inventory.forEach { it.isDirty = false }
    LogiCloTheme {
        LaundryScreen(viewModel = viewModel)
    }
}
*/
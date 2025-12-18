package com.example.myapplication.ui.logiclo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DirtyLens
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("🏠 自宅洗い", "🏬 クリーニング")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laundry", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
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
                        items = viewModel.dirtyHomeItems,
                        actionLabel = "洗濯完了 (Wash All)",
                        onAction = { viewModel.washAllHomeItems() },
                        emptyMsg = "洗濯カゴは空です✨"
                    )
                    1 -> LaundryList(
                        items = viewModel.dirtyDryItems,
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
    onAction: () -> Unit,
    emptyMsg: String
) {
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("洗濯待ち") },
                        leadingContent = { Icon(Icons.Default.DirtyLens, contentDescription = null, tint = TextGrey) }
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = onAction,
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
package com.example.myapplication.ui.port

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DirtyLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaundryScreen(viewModel: AppViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("🏠 自宅洗い", "🏬 クリーニング")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Laundry", fontWeight = FontWeight.Bold) })
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTabIndex) {
                0 -> LaundryList(
                    items = viewModel.getDirtyHomeItems(),
                    actionLabel = "洗濯完了 (Wash All)",
                    onAction = { viewModel.washAllHomeItems() },
                    emptyMsg = "洗濯カゴは空です✨"
                )
                1 -> LaundryList(
                    items = viewModel.getDirtyDryItems(),
                    actionLabel = "店に出す / 受け取る",
                    onAction = { /* No-op in mock */ },
                    emptyMsg = "クリーニング予定はありません"
                )
            }
        }
    }
}

@Composable
fun LaundryList(
    items: List<ClothingItem>,
    actionLabel: String,
    onAction: () -> Unit,
    emptyMsg: String
) {
    if (items.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(emptyMsg, color = Color.Gray)
        }
    } else {
        Column {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.DirtyLens, contentDescription = null, tint = Color.Gray) },
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text("洗濯待ち") }
                        )
                    }
                }
            }
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue)
                    ) {
                        Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

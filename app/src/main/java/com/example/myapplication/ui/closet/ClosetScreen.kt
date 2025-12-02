package com.example.myapplication.ui.closet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.R
import com.example.myapplication.ui.closet.model.ClosetItemUi
import com.example.myapplication.ui.closet.model.ClosetUiState
import com.example.myapplication.ui.components.ClothingIllustrationSwatch
import com.example.myapplication.ui.providers.LocalAppContainer
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.common.labelResId

@Composable
fun ClosetScreen(
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContainer = LocalAppContainer.current
    val viewModel: ClosetViewModel = viewModel(
        factory = ClosetViewModel.Factory(appContainer.closetRepository)
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(id = R.string.closet_add_item)
                )
            }
        }
    ) { innerPadding ->
        ClosetContent(
            state = state,
            onFilterSelected = viewModel::onFilterSelected,
            onDeleteItem = viewModel::onDeleteItem,
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun ClosetContent(
    state: ClosetUiState,
    onFilterSelected: (LaundryStatus) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = listOf(LaundryStatus.CLOSET, LaundryStatus.DIRTY, LaundryStatus.CLEANING)
    val selectedIndex = statuses.indexOf(state.filter).coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.closet_title),
            style = MaterialTheme.typography.titleLarge
        )

        TabRow(selectedTabIndex = selectedIndex) {
            statuses.forEachIndexed { index, status ->
                val count = state.statusCounts[status] ?: 0
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onFilterSelected(status) },
                    text = {
                        val statusLabel = stringResource(id = status.labelResId())
                        Text(
                            text = stringResource(
                                id = R.string.closet_status_tab_label,
                                statusLabel,
                                count
                            )
                        )
                    }
                )
            }
        }

        if (state.isLoading) {
            Text(text = stringResource(id = R.string.closet_loading), style = MaterialTheme.typography.bodyMedium)
        } else if (state.items.isEmpty()) {
            Text(
                text = stringResource(id = R.string.closet_empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            ClosetItemList(items = state.items, onDeleteItem = onDeleteItem)
        }
    }
}

@Composable
private fun ClosetItemList(
    items: List<ClosetItemUi>,
    onDeleteItem: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ClosetItemCard(item = item, onDeleteItem = onDeleteItem)
        }
    }
}

@Composable
private fun ClosetItemCard(
    item: ClosetItemUi,
    onDeleteItem: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClothingIllustrationSwatch(
                    category = item.category,
                    colorHex = item.colorHex
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(id = item.categoryLabelResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onDeleteItem(item.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(id = R.string.closet_delete_item)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val wearCountText = if (item.isAlwaysWash) {
                    stringResource(id = R.string.closet_wear_count_always, item.currentWears)
                } else {
                    stringResource(id = R.string.closet_wear_count_numeric, item.currentWears, item.maxWears)
                }
                Text(text = wearCountText, style = MaterialTheme.typography.bodyMedium)
                val cleaningTypeLabel = stringResource(id = item.cleaningType.labelResId())
                Text(
                    text = stringResource(id = R.string.item_cleaning_type, cleaningTypeLabel),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.isAlwaysWash) {
                    Text(
                        text = stringResource(id = R.string.closet_always_wash),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClosetScreenPreview() {
    MyApplicationTheme {
        val sampleItems = SampleData.closetItems.map {
                ClosetItemUi(
                    id = it.id,
                    name = it.name,
                    category = it.category,
                    categoryLabelResId = it.category.labelResId(),
                    colorHex = it.colorHex,
                    status = it.status,
                    currentWears = it.currentWears,
                    maxWears = it.maxWears,
                    isAlwaysWash = it.isAlwaysWash,
                    cleaningType = it.cleaningType
                )
        }
        ClosetContent(
            state = ClosetUiState(
                isLoading = false,
                filter = LaundryStatus.CLOSET,
                items = sampleItems,
                statusCounts = sampleItems.groupingBy { it.status }.eachCount()
            ),
            onFilterSelected = {},
            onDeleteItem = {}
        )
    }
}

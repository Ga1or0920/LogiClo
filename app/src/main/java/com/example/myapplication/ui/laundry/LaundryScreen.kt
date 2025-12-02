package com.example.myapplication.ui.laundry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.R
import com.example.myapplication.ui.common.UiMessageArg
import com.example.myapplication.ui.common.labelResId
import com.example.myapplication.ui.laundry.model.LaundryItemUi
import com.example.myapplication.ui.laundry.model.LaundryTab
import com.example.myapplication.ui.laundry.model.LaundryUiState
import com.example.myapplication.ui.components.ClothingIllustrationSwatch
import com.example.myapplication.ui.providers.LocalAppContainer
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun LaundryScreen(modifier: Modifier = Modifier) {
    val appContainer = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: LaundryViewModel = viewModel(
        factory = LaundryViewModel.Factory(
            closetRepository = appContainer.closetRepository,
            stringResolver = context::getString,
            fallbackItemName = context.getString(R.string.laundry_fallback_item_name)
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LaundryEvent.ShowMessage -> {
                    val resolvedArgs = event.message.args.map { arg ->
                        when (arg) {
                            is UiMessageArg.Raw -> arg.value
                            is UiMessageArg.Resource -> context.getString(arg.resId)
                        }
                    }.toTypedArray()
                    val text = context.getString(event.message.resId, *resolvedArgs)
                    snackbarHostState.showSnackbar(text)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LaundryContent(
            state = uiState,
            onTabSelected = viewModel::onTabSelected,
            onWashAll = viewModel::onWashAll,
            onSendToDryCleaning = viewModel::onSendToDryCleaning,
            onReceiveFromDryCleaning = viewModel::onReceiveFromDryCleaning,
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun LaundryContent(
    state: LaundryUiState,
    onTabSelected: (LaundryTab) -> Unit,
    onWashAll: () -> Unit,
    onSendToDryCleaning: (String) -> Unit,
    onReceiveFromDryCleaning: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(id = R.string.laundry_title), style = MaterialTheme.typography.titleLarge)

        TabRow(selectedTabIndex = state.activeTab.ordinal) {
            Tab(
                selected = state.activeTab == LaundryTab.HOME,
                onClick = { onTabSelected(LaundryTab.HOME) },
                text = { Text(stringResource(id = R.string.laundry_tab_home)) }
            )
            Tab(
                selected = state.activeTab == LaundryTab.DRY,
                onClick = { onTabSelected(LaundryTab.DRY) },
                text = { Text(stringResource(id = R.string.laundry_tab_dry)) }
            )
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (state.activeTab) {
                LaundryTab.HOME -> HomeLaundrySection(
                    items = state.homeLaundryItems,
                    isProcessing = state.isProcessing,
                    onWashAll = onWashAll
                )
                LaundryTab.DRY -> DryCleaningSection(
                    items = state.dryCleaningItems,
                    isProcessing = state.isProcessing,
                    onSendToDryCleaning = onSendToDryCleaning,
                    onReceiveFromDryCleaning = onReceiveFromDryCleaning
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.HomeLaundrySection(
    items: List<LaundryItemUi>,
    isProcessing: Boolean,
    onWashAll: () -> Unit
) {
    Button(
        onClick = onWashAll,
        enabled = items.isNotEmpty() && !isProcessing
    ) {
        Text(text = stringResource(id = R.string.laundry_wash_all))
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.laundry_home_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                LaundryItemCard(item = item, actions = null)
            }
        }
    }
}

@Composable
private fun ColumnScope.DryCleaningSection(
    items: List<LaundryItemUi>,
    isProcessing: Boolean,
    onSendToDryCleaning: (String) -> Unit,
    onReceiveFromDryCleaning: (String) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.laundry_dry_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val dirtyItems = items.filter { it.status == LaundryStatus.DIRTY }
    val cleaningItems = items.filter { it.status == LaundryStatus.CLEANING }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (dirtyItems.isNotEmpty()) {
            item {
                Text(text = stringResource(id = R.string.laundry_section_dirty), style = MaterialTheme.typography.titleMedium)
            }
            items(dirtyItems, key = { it.id }) { item ->
                LaundryItemCard(
                    item = item,
                    actions = {
                        IconButton(
                            onClick = { onSendToDryCleaning(item.id) },
                            enabled = !isProcessing
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalLaundryService,
                                contentDescription = stringResource(id = R.string.laundry_send_to_cleaning)
                            )
                        }
                    }
                )
            }
        }

        if (dirtyItems.isNotEmpty() && cleaningItems.isNotEmpty()) {
            item { HorizontalDivider() }
        }

        if (cleaningItems.isNotEmpty()) {
            item {
                Text(text = stringResource(id = R.string.laundry_section_cleaning), style = MaterialTheme.typography.titleMedium)
            }
            items(cleaningItems, key = { it.id }) { item ->
                LaundryItemCard(
                    item = item,
                    actions = {
                        IconButton(
                            onClick = { onReceiveFromDryCleaning(item.id) },
                            enabled = !isProcessing
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = stringResource(id = R.string.laundry_receive_item)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LaundryItemCard(
    item: LaundryItemUi,
    actions: (@Composable () -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClothingIllustrationSwatch(
                    category = item.category,
                    colorHex = item.colorHex,
                    swatchSize = 48.dp,
                    iconSize = 32.dp
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(id = item.categoryLabelResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (actions != null) {
                    actions()
                }
            }

            item.lastWornLabel?.let { label ->
                Text(
                    text = stringResource(id = R.string.laundry_last_worn, label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val cleaningLabel = stringResource(id = item.cleaningType.labelResId())
            Text(
                text = stringResource(id = R.string.item_cleaning_type, cleaningLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LaundryScreenPreview() {
    val homeItems = SampleData.closetItems
        .filter { it.status == LaundryStatus.DIRTY && it.cleaningType == CleaningType.HOME }
        .map {
            LaundryItemUi(
                id = it.id,
                name = it.name,
                category = it.category,
                categoryLabelResId = it.category.labelResId(),
                colorHex = it.colorHex,
                status = it.status,
                cleaningType = it.cleaningType,
                lastWornLabel = "11/30 08:30"
            )
        }
    val dryItems = SampleData.closetItems
        .filter { it.cleaningType == CleaningType.DRY }
        .map {
            LaundryItemUi(
                id = it.id,
                name = it.name,
                category = it.category,
                categoryLabelResId = it.category.labelResId(),
                colorHex = it.colorHex,
                status = it.status,
                cleaningType = it.cleaningType,
                lastWornLabel = "11/28 19:10"
            )
        }
    MyApplicationTheme {
        LaundryContent(
            state = LaundryUiState(
                isLoading = false,
                activeTab = LaundryTab.HOME,
                homeLaundryItems = homeItems,
                dryCleaningItems = dryItems
            ),
            onTabSelected = {},
            onWashAll = {},
            onSendToDryCleaning = {},
            onReceiveFromDryCleaning = {}
        )
    }
}

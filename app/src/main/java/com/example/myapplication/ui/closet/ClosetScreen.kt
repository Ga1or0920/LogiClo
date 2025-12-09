package com.example.myapplication.ui.closet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.R
import com.example.myapplication.ui.closet.model.ClosetItemUi
import com.example.myapplication.ui.closet.model.ClosetUiState
import com.example.myapplication.ui.closet.model.ClosetFilters
import com.example.myapplication.ui.components.ClothingIllustrationSwatch
import com.example.myapplication.ui.providers.LocalAppContainer
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.common.labelResId

@Composable
fun ClosetScreen(
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
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
            onFilterButtonClicked = viewModel::onFilterButtonClicked,
            onFilterDialogDismissed = viewModel::onFilterDialogDismissed,
            onFiltersApplied = viewModel::onFiltersApplied,
            onFiltersCleared = viewModel::onFiltersCleared,
            onDeleteItem = viewModel::onDeleteItem,
            onIncrementWear = viewModel::onIncrementWearCount,
            onMarkDirty = viewModel::onMarkItemDirty,
            onResetWear = viewModel::onResetWearCount,
            onEditItem = onEditItem,
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
    onFilterButtonClicked: () -> Unit,
    onFilterDialogDismissed: () -> Unit,
    onFiltersApplied: (ClosetFilters) -> Unit,
    onFiltersCleared: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onIncrementWear: (String) -> Unit,
    onMarkDirty: (String) -> Unit,
    onResetWear: (String) -> Unit,
    onEditItem: (String) -> Unit,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val buttonText = if (state.hasActiveFilters) {
                stringResource(id = R.string.closet_filter_button_active, state.activeFilterCount)
            } else {
                stringResource(id = R.string.closet_filter_button)
            }
            FilledTonalButton(onClick = onFilterButtonClicked) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = buttonText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buttonText)
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
            ClosetItemList(
                items = state.items,
                onDeleteItem = onDeleteItem,
                onEditItem = onEditItem,
                onIncrementWear = onIncrementWear,
                onMarkDirty = onMarkDirty,
                onResetWear = onResetWear
            )
        }

        if (state.isFilterDialogVisible) {
            ClosetFilterDialog(
                filters = state.filters,
                availableCategories = state.availableCategories,
                availableTypes = state.availableTypes,
                availableColorGroups = state.availableColorGroups,
                onDismiss = onFilterDialogDismissed,
                onApply = onFiltersApplied,
                onClear = onFiltersCleared
            )
        }
    }
}

@Composable
private fun ClosetItemList(
    items: List<ClosetItemUi>,
    onDeleteItem: (String) -> Unit,
    onEditItem: (String) -> Unit,
    onIncrementWear: (String) -> Unit,
    onMarkDirty: (String) -> Unit,
    onResetWear: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ClosetItemCard(
                item = item,
                onDeleteItem = onDeleteItem,
                onEditItem = onEditItem,
                onIncrementWear = onIncrementWear,
                onMarkDirty = onMarkDirty,
                onResetWear = onResetWear
            )
        }
    }
}

@Composable
private fun ClosetFilterDialog(
    filters: ClosetFilters,
    availableCategories: List<ClothingCategory>,
    availableTypes: List<ClothingType>,
    availableColorGroups: List<ColorGroup>,
    onDismiss: () -> Unit,
    onApply: (ClosetFilters) -> Unit,
    onClear: () -> Unit
) {
    var selectedCategory by remember(filters) { mutableStateOf(filters.category) }
    var selectedType by remember(filters) { mutableStateOf(filters.type) }
    var selectedColorGroup by remember(filters) { mutableStateOf(filters.colorGroup) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.closet_filter_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val categoryOptions = availableCategories.map { category ->
                    category to stringResource(id = category.labelResId())
                }
                FilterOptionGroup(
                    label = stringResource(id = R.string.closet_filter_category),
                    options = categoryOptions,
                    selectedOption = selectedCategory,
                    onOptionSelected = { selectedCategory = it },
                    modifier = Modifier.fillMaxWidth()
                )

                val typeOptions = availableTypes.map { type ->
                    type to stringResource(id = type.labelResId())
                }
                FilterOptionGroup(
                    label = stringResource(id = R.string.closet_filter_type),
                    options = typeOptions,
                    selectedOption = selectedType,
                    onOptionSelected = { selectedType = it },
                    modifier = Modifier.fillMaxWidth()
                )

                val colorGroupOptions = availableColorGroups.map { colorGroup ->
                    colorGroup to stringResource(id = colorGroup.labelResId())
                }
                FilterOptionGroup(
                    label = stringResource(id = R.string.closet_filter_color_group),
                    options = colorGroupOptions,
                    selectedOption = selectedColorGroup,
                    onOptionSelected = { selectedColorGroup = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        ClosetFilters(
                            category = selectedCategory,
                            type = selectedType,
                            colorGroup = selectedColorGroup
                        )
                    )
                }
            ) {
                Text(text = stringResource(id = R.string.closet_filter_apply))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        selectedCategory = null
                        selectedType = null
                        selectedColorGroup = null
                        onClear()
                    }
                ) {
                    Text(text = stringResource(id = R.string.closet_filter_clear))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        }
    )
}

@Composable
private fun <T> FilterOptionGroup(
    label: String,
    options: List<Pair<T, String>>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterOptionRow(
                text = stringResource(id = R.string.closet_filter_all),
                selected = selectedOption == null,
                onClick = { onOptionSelected(null) }
            )
            options.forEach { (option, labelText) ->
                FilterOptionRow(
                    text = labelText,
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun FilterOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClosetItemCard(
    item: ClosetItemUi,
    onDeleteItem: (String) -> Unit,
    onEditItem: (String) -> Unit,
    onIncrementWear: (String) -> Unit,
    onMarkDirty: (String) -> Unit,
    onResetWear: (String) -> Unit
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
                    item.brand?.takeIf { it.isNotBlank() }?.let { brand ->
                        Text(
                            text = stringResource(id = R.string.closet_item_brand_label, brand),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(id = item.categoryLabelResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onEditItem(item.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.closet_edit_item)
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

            val wearCountText = if (item.isAlwaysWash) {
                stringResource(id = R.string.closet_wear_count_always, item.currentWears)
            } else {
                stringResource(id = R.string.closet_wear_count_numeric, item.currentWears, item.maxWears)
            }
            val cleaningTypeLabel = stringResource(id = item.cleaningType.labelResId())
            val statusLabel = stringResource(id = item.status.labelResId())

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = wearCountText, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(id = R.string.item_cleaning_type, cleaningTypeLabel),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(id = R.string.closet_item_status_label, statusLabel),
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

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { onIncrementWear(item.id) }) {
                    Text(text = stringResource(id = R.string.closet_action_increment_wear))
                }
                TextButton(onClick = { onMarkDirty(item.id) }) {
                    Text(text = stringResource(id = R.string.closet_action_mark_dirty))
                }
                TextButton(
                    onClick = { onResetWear(item.id) },
                    enabled = item.currentWears > 0 || item.status != LaundryStatus.CLOSET
                ) {
                    Text(text = stringResource(id = R.string.closet_action_reset_wear))
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
                    brand = it.brand,
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
            onFilterButtonClicked = {},
            onFilterDialogDismissed = {},
            onFiltersApplied = {},
            onFiltersCleared = {},
            onDeleteItem = {},
            onIncrementWear = {},
            onMarkDirty = {},
            onResetWear = {},
            onEditItem = {}
        )
    }
}

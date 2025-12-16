package com.example.myapplication.ui.closet

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import com.example.myapplication.domain.usecase.ComfortRangeDefaults
import com.example.myapplication.ui.common.labelResId
import com.example.myapplication.ui.components.ClothingIllustrationSwatch
import com.example.myapplication.ui.closet.closetCategoryOptions
import com.example.myapplication.ui.closet.closetColorOptions
import com.example.myapplication.ui.closet.model.CategoryOption
import com.example.myapplication.ui.closet.model.ClosetEditorUiState
import com.example.myapplication.ui.closet.model.ColorOption
import com.example.myapplication.ui.providers.LocalAppContainer
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlin.math.roundToInt
import kotlin.ranges.ClosedFloatingPointRange
import java.util.Locale

@Composable
fun ClosetEditorScreen(
    existingItemId: String? = null,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContainer = LocalAppContainer.current
    val viewModel: ClosetEditorViewModel = viewModel(
        factory = ClosetEditorViewModel.Factory(
            closetRepository = appContainer.closetRepository,
            userPreferencesRepository = appContainer.userPreferencesRepository,
            existingItemId = existingItemId
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            onSaved()
            viewModel.onSaveHandled()
        }
    }

    ClosetEditorContent(
        state = uiState,
        onClose = onClose,
        onNameChange = viewModel::onNameChanged,
        onBrandChange = viewModel::onBrandChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onColorSelected = viewModel::onColorSelected,
        onAlwaysWashChanged = viewModel::onAlwaysWashChanged,
        onMaxWearsChanged = viewModel::onMaxWearsChanged,
        onComfortRangeChanged = viewModel::onComfortRangeChanged,
        onComfortRangeReset = viewModel::onComfortRangeReset,
        onCleaningTypeChanged = viewModel::onCleaningTypeChanged,
        onSleeveLengthSelected = viewModel::onSleeveLengthSelected,
        onThicknessSelected = viewModel::onThicknessSelected,
        onStatusSelected = viewModel::onStatusSelected,
        onSave = viewModel::onSave,
        onImageSelected = viewModel::onImageSelected,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ClosetEditorContent(
    state: ClosetEditorUiState,
    onClose: () -> Unit,
    onNameChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onCategorySelected: (CategoryOption) -> Unit,
    onColorSelected: (ColorOption) -> Unit,
    onAlwaysWashChanged: (Boolean) -> Unit,
    onMaxWearsChanged: (Int) -> Unit,
    onComfortRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onComfortRangeReset: () -> Unit,
    onCleaningTypeChanged: (CleaningType) -> Unit,
    onSleeveLengthSelected: (SleeveLength) -> Unit,
    onThicknessSelected: (Thickness) -> Unit,
    onStatusSelected: (LaundryStatus) -> Unit,
    onSave: () -> Unit,
    onImageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val titleRes = if (state.isEditMode) {
                        R.string.closet_editor_title_edit
                    } else {
                        R.string.closet_editor_title
                    }
                    Text(text = stringResource(id = titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(id = R.string.closet_editor_close)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = state.canSave) {
                        val textRes = when {
                            state.isSaving -> R.string.closet_editor_saving
                            state.isEditMode -> R.string.closet_editor_save_edit
                            else -> R.string.closet_editor_save
                        }
                        Text(text = stringResource(id = textRes))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (state.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(id = R.string.closet_editor_section_name))
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(id = R.string.closet_editor_field_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(id = R.string.closet_editor_section_brand))
                OutlinedTextField(
                    value = state.brand,
                    onValueChange = onBrandChange,
                    label = { Text(stringResource(id = R.string.closet_editor_field_brand_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(text = stringResource(id = R.string.closet_editor_field_brand_support))
                    }
                )
            }

            if (!state.isEditMode) {
                SectionHeader(text = stringResource(id = R.string.closet_editor_section_initial_status))
                StatusOptionRow(
                    options = statusOptions(),
                    selected = state.status,
                    onSelect = onStatusSelected,
                    enabled = !state.isSaving
                )
                Text(
                    text = stringResource(id = R.string.closet_editor_status_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionHeader(text = stringResource(id = R.string.closet_editor_section_category))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.availableCategories.forEach { option ->
                    val selected = state.selectedCategory?.category == option.category
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelected(option) },
                        label = { Text(stringResource(id = option.labelResId)) }
                    )
                }
            }

            SectionHeader(text = stringResource(id = R.string.closet_editor_section_color))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.availableColors.forEach { option ->
                    val selected = state.selectedColor?.colorHex == option.colorHex
                    ColorOptionChip(
                        option = option,
                        selected = selected,
                        onClick = { onColorSelected(option) }
                    )
                }
            }

            if (state.selectedCategory != null && state.selectedColor != null) {
                SectionHeader(text = stringResource(id = R.string.closet_editor_section_preview))
                            // Image import: show dialog on tap and launch picker
                            val showImportDialog = remember { mutableStateOf(false) }
                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri: Uri? ->
                                // pass URI string back via provided callback
                                onImageSelected(uri?.toString())
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                ClothingIllustrationSwatch(
                                    category = state.selectedCategory.category,
                                    colorHex = state.selectedColor.colorHex,
                                    swatchSize = 96.dp,
                                    iconSize = 64.dp,
                                    modifier = Modifier
                                        .clickable(onClick = { showImportDialog.value = true })
                                )
                            }

                            if (showImportDialog.value) {
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { showImportDialog.value = false },
                                    title = { Text(text = stringResource(id = R.string.closet_editor_import_dialog_title)) },
                                    text = { Text(text = stringResource(id = R.string.closet_editor_import_dialog_message)) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showImportDialog.value = false
                                            launcher.launch("image/*")
                                        }) { Text(text = stringResource(id = R.string.common_yes)) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showImportDialog.value = false }) { Text(text = stringResource(id = R.string.common_cancel)) }
                                    }
                                )
                            }
            }

            if (state.selectedCategory != null) {
                SectionHeader(text = stringResource(id = R.string.closet_editor_section_comfort))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.closet_editor_comfort_range_label,
                            formatComfortTemperature(state.comfortMinCelsius),
                            formatComfortTemperature(state.comfortMaxCelsius)
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (state.isComfortRangeCustomized) {
                        TextButton(onClick = onComfortRangeReset) {
                            Text(text = stringResource(id = R.string.closet_editor_comfort_reset))
                        }
                    }
                }
                Text(
                    text = stringResource(id = R.string.closet_editor_comfort_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // スライダーを1℃刻みにするため、stepsを範囲幅（ceil）から1引いた値に設定する
                val comfortSteps = (ComfortRangeDefaults.MAX_LIMIT - ComfortRangeDefaults.MIN_LIMIT).toInt() - 1
                RangeSlider(
                    value = state.comfortMinCelsius.toFloat()..state.comfortMaxCelsius.toFloat(),
                    onValueChange = onComfortRangeChanged,
                    valueRange = ComfortRangeDefaults.MIN_LIMIT.toFloat()..ComfortRangeDefaults.MAX_LIMIT.toFloat(),
                    steps = if (comfortSteps > 0) comfortSteps else 0
                )
            }

            SectionHeader(text = stringResource(id = R.string.closet_editor_section_laundry))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = stringResource(id = R.string.closet_always_wash), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(id = R.string.closet_editor_always_wash_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.isAlwaysWash,
                    onCheckedChange = onAlwaysWashChanged
                )
            }

            if (state.showMaxWearSlider) {
                Text(
                    text = stringResource(id = R.string.closet_editor_wear_count, state.maxWears),
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = state.maxWears.toFloat(),
                    onValueChange = { onMaxWearsChanged(it.roundToInt()) },
                    valueRange = 2f..30f,
                    steps = 28
                )
            } else {
                Text(
                    text = stringResource(id = R.string.closet_editor_always_wash_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionHeader(text = stringResource(id = R.string.closet_editor_section_cleaning))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cleaningOptions().forEach { (type, labelResId) ->
                    val selected = state.cleaningType == type
                    FilterChip(
                        selected = selected,
                        onClick = { onCleaningTypeChanged(type) },
                        label = { Text(stringResource(id = labelResId)) }
                    )
                }
            }

            SectionHeader(text = stringResource(id = R.string.closet_editor_section_attributes))
            AttributeRow(
                label = stringResource(id = R.string.closet_editor_attribute_classification),
                value = stringResource(id = state.type.labelResId())
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.closet_editor_attribute_sleeve),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    SleeveLength.entries
                        .filterNot { it == SleeveLength.UNKNOWN }
                        .forEach { length ->
                            val selected = state.sleeveLength == length
                            FilterChip(
                                selected = selected,
                                onClick = { onSleeveLengthSelected(length) },
                                label = { Text(stringResource(id = length.labelResId())) }
                            )
                        }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.closet_editor_attribute_thickness),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Thickness.entries
                        .filterNot { it == Thickness.UNKNOWN }
                        .forEach { thickness ->
                            val selected = state.thickness == thickness
                            FilterChip(
                                selected = selected,
                                onClick = { onThicknessSelected(thickness) },
                                label = { Text(stringResource(id = thickness.labelResId())) }
                            )
                        }
                }
            }
            AttributeRow(
                label = stringResource(id = R.string.closet_editor_attribute_color_group),
                value = state.selectedColor?.group?.let { group ->
                    stringResource(id = group.labelResId())
                } ?: stringResource(id = R.string.closet_editor_value_unselected)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun AttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorOptionChip(
    option: ColorOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(id = option.labelResId)) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(android.graphics.Color.parseColor(option.colorHex)), CircleShape)
            )
        }
    )
}

private fun cleaningOptions(): List<Pair<CleaningType, Int>> = listOf(
    CleaningType.HOME,
    CleaningType.DRY
).map { it to it.labelResId() }

@Composable
private fun StatusOptionRow(
    options: List<StatusOption>,
    selected: LaundryStatus,
    onSelect: (LaundryStatus) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            StatusOptionCard(
                option = option,
                selected = selected == option.status,
                onClick = { onSelect(option.status) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusOptionCard(
    option: StatusOption,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        contentColor = contentColor,
        border = border,
        tonalElevation = if (selected) 6.dp else 0.dp,
        onClick = onClick,
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${option.emoji} ${stringResource(id = option.titleResId)}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null
                    )
                }
            }
            Text(
                text = stringResource(id = option.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private data class StatusOption(
    val status: LaundryStatus,
    val emoji: String,
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int
)

private fun statusOptions(): List<StatusOption> = listOf(
    StatusOption(
        status = LaundryStatus.CLOSET,
        emoji = "📂",
        titleResId = R.string.closet_editor_status_option_closet_title,
        descriptionResId = R.string.closet_editor_status_option_closet_description
    ),
    StatusOption(
        status = LaundryStatus.DIRTY,
        emoji = "🧺",
        titleResId = R.string.closet_editor_status_option_dirty_title,
        descriptionResId = R.string.closet_editor_status_option_dirty_description
    ),
    StatusOption(
        status = LaundryStatus.CLEANING,
        emoji = "🏬",
        titleResId = R.string.closet_editor_status_option_cleaning_title,
        descriptionResId = R.string.closet_editor_status_option_cleaning_description
    )
)

private fun formatComfortTemperature(value: Double): String =
    String.format(Locale.JAPAN, "%.1f", value)

@Preview(showBackground = true)
@Composable
private fun ClosetEditorPreview() {
    val sampleState = ClosetEditorUiState(
        name = "ネイビーポロ",
        brand = "Sample Brand",
        selectedCategory = closetCategoryOptions().first { it.category == ClothingCategory.POLO },
        selectedColor = closetColorOptions().first { it.colorHex == "#0D3B66" },
        isAlwaysWash = false,
        maxWears = 3,
        comfortMinCelsius = 20.0,
        comfortMaxCelsius = 30.0,
        cleaningType = CleaningType.HOME,
        sleeveLength = SleeveLength.SHORT,
        thickness = Thickness.THIN,
        availableCategories = closetCategoryOptions(),
        availableColors = closetColorOptions()
    )
    MyApplicationTheme {
        ClosetEditorContent(
            state = sampleState,
            onClose = {},
            onNameChange = {},
            onBrandChange = {},
            onCategorySelected = {},
            onColorSelected = {},
            onAlwaysWashChanged = {},
            onMaxWearsChanged = {},
            onComfortRangeChanged = {},
            onComfortRangeReset = {},
            onCleaningTypeChanged = {},
            onSleeveLengthSelected = {},
            onThicknessSelected = {},
            onStatusSelected = {},
            onSave = {},
            onImageSelected = {}
        )
    }
}

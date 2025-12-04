package com.example.myapplication.ui.closet

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
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

@Composable
fun ClosetEditorScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContainer = LocalAppContainer.current
    val viewModel: ClosetEditorViewModel = viewModel(
        factory = ClosetEditorViewModel.Factory(
            closetRepository = appContainer.closetRepository,
            userPreferencesRepository = appContainer.userPreferencesRepository
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
        onCleaningTypeChanged = viewModel::onCleaningTypeChanged,
        onSleeveLengthSelected = viewModel::onSleeveLengthSelected,
        onThicknessSelected = viewModel::onThicknessSelected,
        onSave = viewModel::onSave,
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
    onCleaningTypeChanged: (CleaningType) -> Unit,
    onSleeveLengthSelected: (SleeveLength) -> Unit,
    onThicknessSelected: (Thickness) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.closet_editor_title)) },
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
                        val textRes = if (state.isSaving) {
                            R.string.closet_editor_saving
                        } else {
                            R.string.closet_editor_save
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

            SectionHeader(text = stringResource(id = R.string.closet_editor_section_name))
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(id = R.string.closet_editor_field_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ClothingIllustrationSwatch(
                        category = state.selectedCategory.category,
                        colorHex = state.selectedColor.colorHex,
                        swatchSize = 96.dp,
                        iconSize = 64.dp
                    )
                }
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
            Text(
                text = stringResource(id = R.string.closet_editor_attribute_sleeve),
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
            Text(
                text = stringResource(id = R.string.closet_editor_attribute_thickness),
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
            onCleaningTypeChanged = {},
            onSleeveLengthSelected = {},
            onThicknessSelected = {},
            onSave = {}
        )
    }
}

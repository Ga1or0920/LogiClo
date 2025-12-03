package com.example.myapplication.ui.dashboard

import android.content.Context
import android.widget.Toast
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.util.time.InstantCompat
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.InMemoryAppContainer
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.ui.common.UiMessage
import com.example.myapplication.ui.common.UiMessageArg
import com.example.myapplication.ui.common.labelResId
import com.example.myapplication.ui.components.ClothingIllustrationSwatch
import com.example.myapplication.ui.dashboard.model.AlertSeverity
import com.example.myapplication.ui.dashboard.model.DashboardUiState
import com.example.myapplication.ui.dashboard.model.InventoryAlert
import com.example.myapplication.ui.dashboard.model.OutfitSuggestion
import com.example.myapplication.ui.providers.LocalAppContainer
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.time.Instant
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val appContainer = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            closetRepository = appContainer.closetRepository,
            userPreferencesRepository = appContainer.userPreferencesRepository,
            weatherRepository = appContainer.weatherRepository,
            stringResolver = context::getString
        )
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { messages ->
            messages.forEach { message ->
                Toast.makeText(context, message.resolve(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    DashboardContent(
        state = state,
        onModeSelected = viewModel::onModeSelected,
        onEnvironmentSelected = viewModel::onEnvironmentSelected,
        onSuggestionSelected = viewModel::onSuggestionSelected,
        onWearClicked = viewModel::onWearSelected,
        onRerollSuggestion = viewModel::rerollSuggestion,
        onReviewInventory = viewModel::onReviewInventoryRequested,
        onDismissReviewInventory = viewModel::onReviewInventoryDismissed,
        onRefreshWeather = viewModel::refreshWeather,
        modifier = modifier
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onModeSelected: (TpoMode) -> Unit,
    onEnvironmentSelected: (EnvironmentMode) -> Unit,
    onSuggestionSelected: (OutfitSuggestion) -> Unit,
    onWearClicked: () -> Unit,
    onRerollSuggestion: () -> Unit,
    onReviewInventory: () -> Unit,
    onDismissReviewInventory: () -> Unit,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.dashboard_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            ModeSwitcher(
                currentMode = state.mode,
                onModeSelected = onModeSelected
            )
        }

        item {
            EnvironmentSwitcher(
                currentEnvironment = state.environment,
                onEnvironmentSelected = onEnvironmentSelected
            )
        }

        state.weather?.let { weather ->
            item {
                WeatherCard(
                    weather = weather,
                    isRefreshing = state.isRefreshingWeather,
                    lastUpdatedAt = state.lastWeatherUpdatedAt,
                    errorMessage = state.weatherErrorMessage,
                    onRefresh = onRefreshWeather
                )
            }
        }

        state.alert?.let { alert ->
            item {
                AlertCard(alert = alert)
            }
        }

        when {
            state.isLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.suggestions.isEmpty() -> {
                item {
                    PurchaseRecommendationList(
                        recommendations = state.purchaseRecommendations
                    )
                }
            }

            else -> {
                item {
                    OutfitSuggestionSection(
                        suggestions = state.suggestions,
                        selectedSuggestion = state.selectedSuggestion,
                        onSuggestionSelected = onSuggestionSelected
                    )
                }

                if (state.selectionInsights.isNotEmpty()) {
                    item {
                        SelectionInsightsCard(insights = state.selectionInsights)
                    }
                }
            }
        }

        if (state.inventoryReviewMessages.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = onReviewInventory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.dashboard_review_button))
                }
            }
        }

        item {
            ActionButtons(
                onRerollSuggestion = onRerollSuggestion,
                onWearClicked = onWearClicked,
                isRerollEnabled = state.totalSuggestionCount > 1,
                isWearEnabled = state.selectedSuggestion != null
            )
        }
    }

    if (state.isInventoryReviewVisible) {
        InventoryReviewDialog(
            messages = state.inventoryReviewMessages,
            onDismiss = onDismissReviewInventory
        )
    }
}

@Composable
private fun ModeSwitcher(
    currentMode: TpoMode,
    onModeSelected: (TpoMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = currentMode == TpoMode.CASUAL,
            onClick = { onModeSelected(TpoMode.CASUAL) },
            label = { Text(stringResource(id = R.string.dashboard_mode_casual)) }
        )
        FilterChip(
            selected = currentMode == TpoMode.OFFICE,
            onClick = { onModeSelected(TpoMode.OFFICE) },
            label = { Text(stringResource(id = R.string.dashboard_mode_office)) }
        )
    }
}

@Composable
private fun EnvironmentSwitcher(
    currentEnvironment: EnvironmentMode,
    onEnvironmentSelected: (EnvironmentMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.dashboard_environment_label),
            style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = currentEnvironment == EnvironmentMode.OUTDOOR,
                onClick = { onEnvironmentSelected(EnvironmentMode.OUTDOOR) },
                label = { Text(stringResource(id = R.string.dashboard_environment_outdoor)) }
            )
            FilterChip(
                selected = currentEnvironment == EnvironmentMode.INDOOR,
                onClick = { onEnvironmentSelected(EnvironmentMode.INDOOR) },
                label = { Text(stringResource(id = R.string.dashboard_environment_indoor)) }
            )
        }
    }
}

@Composable
private fun SelectionInsightsCard(
    insights: List<UiMessage>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_insights_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            insights.forEach { message ->
                Text(
                    text = message.resolve(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InventoryReviewDialog(
    messages: List<UiMessage>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dashboard_review_dialog_close))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.dashboard_review_dialog_title))
        },
        text = {
            Column(
                modifier = modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                messages.forEach { message ->
                    Text(
                        text = "• ${message.resolve()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun AlertCard(alert: InventoryAlert) {
    val containerColor = when (alert.severity) {
        AlertSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        AlertSeverity.NONE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (alert.severity) {
        AlertSeverity.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        AlertSeverity.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val text = alert.message.resolve()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun WeatherCard(
    weather: WeatherSnapshot,
    isRefreshing: Boolean,
    lastUpdatedAt: Instant?,
    errorMessage: UiMessage?,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(id = R.string.dashboard_weather_title), style = MaterialTheme.typography.titleMedium)
                    val updatedLabel = lastUpdatedAt?.let { instant ->
                        val text = formatWeatherTimestamp(instant)
                        stringResource(id = R.string.dashboard_weather_last_updated, text)
                    } ?: stringResource(id = R.string.dashboard_weather_last_unknown)
                    Text(
                        text = updatedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    TextButton(onClick = onRefresh) {
                        Text(text = stringResource(id = R.string.dashboard_weather_refresh))
                    }
                }
            }
            Text(text = stringResource(id = R.string.dashboard_weather_min, weather.minTemperatureCelsius))
            Text(text = stringResource(id = R.string.dashboard_weather_max, weather.maxTemperatureCelsius))
            Text(text = stringResource(id = R.string.dashboard_weather_humidity, weather.humidityPercent))
            if (errorMessage != null) {
                Text(
                    text = errorMessage.resolve(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PurchaseRecommendationList(
    recommendations: List<UiMessage>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.dashboard_no_suggestions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(id = R.string.dashboard_recommendations_title),
            style = MaterialTheme.typography.titleMedium
        )
        if (recommendations.isEmpty()) {
            Text(
                text = stringResource(id = R.string.dashboard_recommendations_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recommendations.forEach { message ->
                        Text(
                            text = "- ${message.resolve()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitSuggestionSection(
    suggestions: List<OutfitSuggestion>,
    selectedSuggestion: OutfitSuggestion?,
    onSuggestionSelected: (OutfitSuggestion) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.dashboard_suggestions_section_title),
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(suggestions, key = { "${it.top.id}-${it.bottom.id}" }) { suggestion ->
                val isSelected = suggestion == selectedSuggestion
                OutfitSuggestionCard(
                    suggestion = suggestion,
                    selected = isSelected,
                    onClick = { onSuggestionSelected(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun OutfitSuggestionCard(
    suggestion: OutfitSuggestion,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SuggestionItemPreview(
                label = stringResource(id = R.string.dashboard_suggestion_top),
                item = suggestion.top
            )
            HorizontalDivider()
            SuggestionItemPreview(
                label = stringResource(id = R.string.dashboard_suggestion_bottom),
                item = suggestion.bottom
            )
            SuggestionMeta(text = stringResource(id = R.string.dashboard_suggestion_formality, suggestion.totalScore))
        }
    }
}

@Composable
private fun SuggestionItemPreview(label: String, item: ClothingItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClothingIllustrationSwatch(
            category = item.category,
            colorHex = item.colorHex,
            swatchSize = 72.dp,
            iconSize = 48.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = item.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(id = item.category.labelResId()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.dashboard_suggestion_color,
                    item.colorHex.removePrefix("#").uppercase()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuggestionMeta(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ActionButtons(
    onRerollSuggestion: () -> Unit,
    onWearClicked: () -> Unit,
    isRerollEnabled: Boolean,
    isWearEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onRerollSuggestion,
            enabled = isRerollEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.dashboard_button_reroll))
        }

        Button(
            onClick = onWearClicked,
            enabled = isWearEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.dashboard_button_wear))
        }
    }
}

private fun UiMessage.resolve(context: Context): String {
    val resolvedArgs = args.map { arg ->
        when (arg) {
            is UiMessageArg.Raw -> arg.value
            is UiMessageArg.Resource -> context.getString(arg.resId)
        }
    }.toTypedArray()
    return context.getString(resId, *resolvedArgs)
}

@Composable
private fun UiMessage.resolve(): String {
    val context = LocalContext.current
    return resolve(context)
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    MyApplicationTheme {
        val container = InMemoryAppContainer()
        androidx.compose.runtime.CompositionLocalProvider(
            LocalAppContainer provides container
        ) {
            DashboardContent(
                state = DashboardUiState(
                    isLoading = false,
                    mode = TpoMode.CASUAL,
                    environment = EnvironmentMode.OUTDOOR,
                    suggestions = listOf(
                        OutfitSuggestion(
                            top = SampleData.closetItems[0],
                            bottom = SampleData.closetItems[1],
                            totalScore = 7
                        ),
                        OutfitSuggestion(
                            top = SampleData.closetItems[2],
                            bottom = SampleData.closetItems[3],
                            totalScore = 5
                        )
                    ),
                    totalSuggestionCount = 2,
                    selectionInsights = listOf(
                        UiMessage(
                            resId = R.string.dashboard_insight_mode,
                            args = listOf(
                                UiMessageArg.Raw("Casual"),
                                UiMessageArg.Raw("0~6"),
                                UiMessageArg.Raw(7)
                            )
                        ),
                        UiMessage(
                            resId = R.string.dashboard_insight_weather,
                            args = listOf(
                                UiMessageArg.Raw("18.0"),
                                UiMessageArg.Raw("24.0"),
                                UiMessageArg.Raw("18.0"),
                                UiMessageArg.Raw("27.0"),
                                UiMessageArg.Raw(65),
                                UiMessageArg.Raw("屋外")
                            )
                        )
                    ),
                    inventoryReviewMessages = listOf(
                        UiMessage(
                            resId = R.string.dashboard_alert_low_inventory,
                            args = listOf(UiMessageArg.Resource(R.string.dashboard_mode_casual))
                        ),
                        UiMessage(
                            resId = R.string.dashboard_recommendation_generic_top,
                            args = listOf(UiMessageArg.Raw("Tシャツ"))
                        )
                    ),
                    isInventoryReviewVisible = true,
                    alert = InventoryAlert(
                        severity = AlertSeverity.WARNING,
                        message = UiMessage(
                            resId = R.string.dashboard_alert_low_inventory,
                            args = listOf(UiMessageArg.Resource(R.string.dashboard_mode_casual))
                        )
                    ),
                    weather = SampleData.weather,
                    selectedSuggestion = OutfitSuggestion(
                        top = SampleData.closetItems[0],
                        bottom = SampleData.closetItems[1],
                        totalScore = 7
                    ),
                    isRefreshingWeather = false,
                    lastWeatherUpdatedAt = InstantCompat.nowOrNull(),
                    weatherErrorMessage = UiMessage(R.string.dashboard_weather_error)
                ),
                onModeSelected = {},
                onEnvironmentSelected = {},
                onSuggestionSelected = {},
                onWearClicked = {},
                onRerollSuggestion = {},
                onReviewInventory = {},
                onDismissReviewInventory = {},
                onRefreshWeather = {}
            )
        }
    }
}
private fun formatWeatherTimestamp(instant: Instant): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        java.time.format.DateTimeFormatter.ofPattern("M/d HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } else {
        val epochMillis = InstantCompat.toEpochMilliOrNull(instant)
            ?: return SimpleDateFormat("M/d HH:mm", Locale.getDefault()).format(Date())
        SimpleDateFormat("M/d HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }
}

package com.example.myapplication.ui.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.InMemoryAppContainer
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.domain.model.ClothingItem
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
import java.time.format.DateTimeFormatter

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
        onSuggestionSelected = viewModel::onSuggestionSelected,
        onWearClicked = viewModel::onWearSelected,
        onRerollSuggestion = viewModel::rerollSuggestion,
        onRefreshWeather = viewModel::refreshWeather,
        modifier = modifier
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onModeSelected: (TpoMode) -> Unit,
    onSuggestionSelected: (OutfitSuggestion) -> Unit,
    onWearClicked: () -> Unit,
    onRerollSuggestion: () -> Unit,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.dashboard_title),
            style = MaterialTheme.typography.titleLarge
        )

        ModeSwitcher(
            currentMode = state.mode,
            onModeSelected = onModeSelected
        )

        state.weather?.let { weather ->
            WeatherCard(
                weather = weather,
                isRefreshing = state.isRefreshingWeather,
                lastUpdatedAt = state.lastWeatherUpdatedAt,
                errorMessage = state.weatherErrorMessage,
                onRefresh = onRefreshWeather
            )
        }

        state.alert?.let { alert ->
            AlertCard(alert = alert)
        }

        Column(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                }
                state.suggestions.isEmpty() -> {
                    PurchaseRecommendationList(
                        recommendations = state.purchaseRecommendations
                    )
                }
                else -> {
                    SuggestionList(
                        suggestions = state.suggestions,
                        selectedSuggestion = state.selectedSuggestion,
                        onSuggestionSelected = onSuggestionSelected
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onRerollSuggestion,
                enabled = state.totalSuggestionCount > 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.dashboard_button_reroll))
            }

            Button(
                onClick = onWearClicked,
                enabled = state.selectedSuggestion != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.dashboard_button_wear))
            }
        }
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
                        val text = WEATHER_TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))
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
private fun SuggestionList(
    suggestions: List<OutfitSuggestion>,
    selectedSuggestion: OutfitSuggestion?,
    onSuggestionSelected: (OutfitSuggestion) -> Unit
) {
    if (suggestions.isEmpty()) {
        Text(
            text = stringResource(id = R.string.dashboard_no_suggestions),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(suggestions, key = { "${it.top.id}-${it.bottom.id}" }) { suggestion ->
            val selected = suggestion == selectedSuggestion
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionSelected(suggestion) },
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SuggestionItemRow(
                        label = stringResource(id = R.string.dashboard_suggestion_top),
                        item = suggestion.top
                    )
                    SuggestionItemRow(
                        label = stringResource(id = R.string.dashboard_suggestion_bottom),
                        item = suggestion.bottom
                    )
                    Text(
                        text = stringResource(id = R.string.dashboard_suggestion_formality, suggestion.totalScore),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionItemRow(label: String, item: ClothingItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ClothingIllustrationSwatch(
            category = item.category,
            colorHex = item.colorHex,
            swatchSize = 48.dp,
            iconSize = 32.dp
        )
        Column {
            Text(text = "$label: ${item.name}", style = MaterialTheme.typography.titleMedium)
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
                    suggestions = listOf(
                        OutfitSuggestion(
                            top = SampleData.closetItems[0],
                            bottom = SampleData.closetItems[1],
                            totalScore = 7
                        )
                    ),
                    totalSuggestionCount = 2,
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
                    lastWeatherUpdatedAt = Instant.now(),
                    weatherErrorMessage = UiMessage(R.string.dashboard_weather_error)
                ),
                onModeSelected = {},
                onSuggestionSelected = {},
                onWearClicked = {},
                onRerollSuggestion = {},
                onRefreshWeather = {}
            )
        }
    }
}

private val WEATHER_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d HH:mm")

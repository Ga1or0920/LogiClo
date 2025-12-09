package com.example.myapplication.ui.dashboard

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.util.time.InstantCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.InMemoryAppContainer
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.CasualForecastDay
import com.example.myapplication.domain.model.CasualForecastSegment
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.ui.common.UiMessageArg
import com.example.myapplication.ui.common.labelResId
import com.example.myapplication.ui.components.ClothingIllustrationSwatch
import com.example.myapplication.ui.common.UiMessage
import com.example.myapplication.ui.closet.closetColorOptions
import com.example.myapplication.ui.dashboard.model.AlertSeverity
import com.example.myapplication.ui.dashboard.model.DashboardUiState
import com.example.myapplication.ui.dashboard.model.InventoryAlert
import com.example.myapplication.ui.dashboard.model.OutfitSuggestion
import com.example.myapplication.ui.dashboard.model.ClockDebugUiState
import com.example.myapplication.ui.dashboard.model.CasualForecastSegmentOption
import com.example.myapplication.ui.dashboard.model.CasualForecastSummary
import com.example.myapplication.ui.dashboard.model.CasualForecastUiState
import com.example.myapplication.ui.dashboard.model.WeatherDebugUiState
import com.example.myapplication.ui.dashboard.model.WearFeedbackDebugUiState
import com.example.myapplication.ui.providers.LocalAppContainer
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.time.Instant
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val appContainer = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            closetRepository = appContainer.closetRepository,
            userPreferencesRepository = appContainer.userPreferencesRepository,
            weatherRepository = appContainer.weatherRepository,
            wearFeedbackRepository = appContainer.wearFeedbackRepository,
            weatherDebugController = appContainer.weatherDebugController,
            clockDebugController = appContainer.clockDebugController,
            stringResolver = context::getString
        )
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        ensureWearNotificationChannel(context)
        viewModel.wearNotificationEvents.collect { messages ->
            showWearNotification(context, messages)
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
        onCasualDaySelected = viewModel::onCasualForecastDaySelected,
        onCasualSegmentSelected = viewModel::onCasualForecastSegmentSelected,
        onClockDebugNextDayChanged = viewModel::onClockDebugNextDayChanged,
        onDebugMinTempChanged = viewModel::onDebugMinTemperatureChanged,
        onDebugMaxTempChanged = viewModel::onDebugMaxTemperatureChanged,
        onDebugHumidityChanged = viewModel::onDebugHumidityChanged,
        onApplyWeatherDebug = viewModel::applyWeatherDebugOverride,
        onClearWeatherDebug = viewModel::clearWeatherDebugOverride,
        onClockDebugManualInputChanged = viewModel::onClockDebugManualOverrideInputChanged,
        onApplyClockDebugManualOverride = viewModel::applyClockDebugManualOverride,
        onClearClockDebugManualOverride = viewModel::clearClockDebugManualOverride,
        onDismissComebackDialog = viewModel::onComebackDialogDismissed,
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
    onCasualDaySelected: (CasualForecastDay) -> Unit,
    onCasualSegmentSelected: (CasualForecastSegment) -> Unit,
    onClockDebugNextDayChanged: (Boolean) -> Unit,
    onClockDebugManualInputChanged: (String) -> Unit,
    onApplyClockDebugManualOverride: () -> Unit,
    onClearClockDebugManualOverride: () -> Unit,
    onDismissComebackDialog: () -> Unit,
    onDebugMinTempChanged: (String) -> Unit,
    onDebugMaxTempChanged: (String) -> Unit,
    onDebugHumidityChanged: (String) -> Unit,
    onApplyWeatherDebug: () -> Unit,
    onClearWeatherDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = DashboardTab.entries
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(DashboardTab.OVERVIEW.ordinal) }
    val selectedTab = tabs[selectedTabIndex]

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                DashboardTab.OVERVIEW -> OverviewTabContent(
                    state = state,
                    onModeSelected = onModeSelected,
                    onEnvironmentSelected = onEnvironmentSelected,
                    onSuggestionSelected = onSuggestionSelected,
                    onWearClicked = onWearClicked,
                    onRerollSuggestion = onRerollSuggestion,
                    onReviewInventory = onReviewInventory,
                    onRefreshWeather = onRefreshWeather,
                    onCasualDaySelected = onCasualDaySelected,
                    onCasualSegmentSelected = onCasualSegmentSelected,
                    modifier = Modifier.fillMaxSize()
                )

                DashboardTab.DEBUG -> DebugTabContent(
                    weatherDebug = state.weatherDebug,
                    clockDebug = state.clockDebug,
                    wearFeedbackDebug = state.wearFeedbackDebug,
                    onClockDebugNextDayChanged = onClockDebugNextDayChanged,
                    onClockDebugManualInputChanged = onClockDebugManualInputChanged,
                    onApplyClockDebugManualOverride = onApplyClockDebugManualOverride,
                    onClearClockDebugManualOverride = onClearClockDebugManualOverride,
                    onDebugMinTempChanged = onDebugMinTempChanged,
                    onDebugMaxTempChanged = onDebugMaxTempChanged,
                    onDebugHumidityChanged = onDebugHumidityChanged,
                    onApplyWeatherDebug = onApplyWeatherDebug,
                    onClearWeatherDebug = onClearWeatherDebug,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        DashboardTabSelector(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = { selectedTabIndex = it.ordinal }
        )
    }

    if (state.isInventoryReviewVisible) {
        InventoryReviewDialog(
            alert = state.alert,
            messages = state.inventoryReviewMessages,
            onDismiss = onDismissReviewInventory
        )
    }

    state.comebackDialogMessage?.let { message ->
        ComebackDialog(
            message = message,
            onDismiss = onDismissComebackDialog
        )
    }
}

@Composable
private fun OverviewTabContent(
    state: DashboardUiState,
    onModeSelected: (TpoMode) -> Unit,
    onEnvironmentSelected: (EnvironmentMode) -> Unit,
    onSuggestionSelected: (OutfitSuggestion) -> Unit,
    onWearClicked: () -> Unit,
    onRerollSuggestion: () -> Unit,
    onReviewInventory: () -> Unit,
    onRefreshWeather: () -> Unit,
    onCasualDaySelected: (CasualForecastDay) -> Unit,
    onCasualSegmentSelected: (CasualForecastSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
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

        state.casualForecast?.let { casualForecastState ->
            item {
                CasualForecastPlanner(
                    state = casualForecastState,
                    onDaySelected = onCasualDaySelected,
                    onSegmentSelected = onCasualSegmentSelected
                )
            }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CasualForecastPlanner(
    state: CasualForecastUiState,
    onDaySelected: (CasualForecastDay) -> Unit,
    onSegmentSelected: (CasualForecastSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_casual_forecast_title),
                style = MaterialTheme.typography.titleMedium
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                state.dayOptions.forEachIndexed { index, day ->
                    val selected = day == state.selectedDay
                    SegmentedButton(
                        selected = selected,
                        onClick = { onDaySelected(day) },
                        shape = SegmentedButtonDefaults.itemShape(index, state.dayOptions.size),
                        label = {
                            Text(text = stringResource(id = day.labelResId()))
                        }
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                state.segmentOptions.forEachIndexed { index, option ->
                    val selected = option.segment == state.selectedSegment
                    SegmentedButton(
                        selected = selected,
                        onClick = { onSegmentSelected(option.segment) },
                        enabled = option.isEnabled,
                        shape = SegmentedButtonDefaults.itemShape(index, state.segmentOptions.size),
                        label = {
                            Text(text = stringResource(id = option.segment.labelResId()))
                        }
                    )
                }
            }

            Text(
                text = stringResource(
                    id = R.string.dashboard_casual_forecast_summary,
                    state.summary.averageApparentTemperatureCelsius,
                    state.summary.minTemperatureCelsius,
                    state.summary.maxTemperatureCelsius
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DebugTabContent(
    weatherDebug: WeatherDebugUiState?,
    clockDebug: ClockDebugUiState?,
    wearFeedbackDebug: WearFeedbackDebugUiState?,
    onClockDebugNextDayChanged: (Boolean) -> Unit,
    onClockDebugManualInputChanged: (String) -> Unit,
    onApplyClockDebugManualOverride: () -> Unit,
    onClearClockDebugManualOverride: () -> Unit,
    onDebugMinTempChanged: (String) -> Unit,
    onDebugMaxTempChanged: (String) -> Unit,
    onDebugHumidityChanged: (String) -> Unit,
    onApplyWeatherDebug: () -> Unit,
    onClearWeatherDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (weatherDebug == null && clockDebug == null && wearFeedbackDebug == null) {
            item {
                Text(
                    text = stringResource(id = R.string.debug_tab_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            weatherDebug?.let { debugState ->
                item {
                    WeatherDebugCard(
                        state = debugState,
                        onMinTempChanged = onDebugMinTempChanged,
                        onMaxTempChanged = onDebugMaxTempChanged,
                        onHumidityChanged = onDebugHumidityChanged,
                        onApply = onApplyWeatherDebug,
                        onClear = onClearWeatherDebug
                    )
                }
            }

            clockDebug?.let { clockState ->
                item {
                    ClockDebugCard(
                        state = clockState,
                        onToggleNextDay = onClockDebugNextDayChanged,
                        onManualInputChanged = onClockDebugManualInputChanged,
                        onApplyManualOverride = onApplyClockDebugManualOverride,
                        onClearManualOverride = onClearClockDebugManualOverride
                    )
                }
            }

            wearFeedbackDebug?.let { feedbackState ->
                item {
                    WearFeedbackDebugCard(state = feedbackState)
                }
            }
        }
    }
}

private enum class DashboardTab(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    OVERVIEW(R.string.dashboard_tab_overview, Icons.Filled.Home),
    DEBUG(R.string.dashboard_tab_debug, Icons.Filled.Build)
}

@Composable
private fun DashboardTabSelector(
    tabs: List<DashboardTab>,
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(id = tab.labelRes)
                    )
                },
                label = { Text(text = stringResource(id = tab.labelRes)) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@StringRes
private fun CasualForecastDay.labelResId(): Int = when (this) {
    CasualForecastDay.TODAY -> R.string.dashboard_casual_forecast_day_today
    CasualForecastDay.TOMORROW -> R.string.dashboard_casual_forecast_day_tomorrow
}

@StringRes
private fun CasualForecastSegment.labelResId(): Int = when (this) {
    CasualForecastSegment.MORNING -> R.string.dashboard_casual_forecast_segment_morning
    CasualForecastSegment.AFTERNOON -> R.string.dashboard_casual_forecast_segment_afternoon
    CasualForecastSegment.EVENING -> R.string.dashboard_casual_forecast_segment_evening
}

@Composable
private fun ModeSwitcher(
    currentMode: TpoMode,
    onModeSelected: (TpoMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.dashboard_mode_selector_label),
            style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    alert: InventoryAlert?,
    messages: List<UiMessage>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredMessages = alert?.message?.let { message ->
        messages.filterNot { it == message }
    } ?: messages

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                alert?.let {
                    Text(
                        text = stringResource(id = R.string.dashboard_review_dialog_alert_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InventoryReviewAlertCard(alert = it)
                }

                if (filteredMessages.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.dashboard_review_dialog_recommendation_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InventoryReviewRecommendationList(messages = filteredMessages)
                } else if (alert == null) {
                    Text(
                        text = stringResource(id = R.string.dashboard_review_dialog_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun ComebackDialog(
    message: UiMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dashboard_comeback_dialog_close))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.dashboard_comeback_dialog_title))
        },
        text = {
            Text(
                text = message.resolve(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = modifier
            )
        }
    )
}

@Composable
private fun InventoryReviewAlertCard(
    alert: InventoryAlert,
    modifier: Modifier = Modifier
) {
    val (containerColor, onContainerColor, labelResId) = when (alert.severity) {
        AlertSeverity.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            R.string.dashboard_review_dialog_severity_error
        )
        AlertSeverity.WARNING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            R.string.dashboard_review_dialog_severity_warning
        )
        AlertSeverity.NONE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.dashboard_review_dialog_severity_info
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = onContainerColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = labelResId),
                style = MaterialTheme.typography.labelMedium,
                color = onContainerColor
            )
            Text(
                text = alert.message.resolve(),
                style = MaterialTheme.typography.bodyMedium,
                color = onContainerColor
            )
        }
    }
}

@Composable
private fun InventoryReviewRecommendationList(
    messages: List<UiMessage>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
private fun WeatherDebugCard(
    state: WeatherDebugUiState,
    onMinTempChanged: (String) -> Unit,
    onMaxTempChanged: (String) -> Unit,
    onHumidityChanged: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.debug_weather_title),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = state.minTemperatureInput,
                onValueChange = onMinTempChanged,
                label = { Text(stringResource(id = R.string.debug_weather_min_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.maxTemperatureInput,
                onValueChange = onMaxTempChanged,
                label = { Text(stringResource(id = R.string.debug_weather_max_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.humidityInput,
                onValueChange = onHumidityChanged,
                label = { Text(stringResource(id = R.string.debug_weather_humidity_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onApply) {
                    Text(text = stringResource(id = R.string.debug_weather_apply))
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = state.isOverrideActive
                ) {
                    Text(text = stringResource(id = R.string.debug_weather_clear))
                }
            }
            if (state.isOverrideActive) {
                Text(
                    text = stringResource(id = R.string.debug_weather_active),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.debug_weather_persistent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.lastAppliedAt?.let { instant ->
                val label = formatWeatherTimestamp(instant)
                Text(
                    text = stringResource(id = R.string.debug_weather_last_applied, label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ClockDebugCard(
    state: ClockDebugUiState,
    onToggleNextDay: (Boolean) -> Unit,
    onManualInputChanged: (String) -> Unit,
    onApplyManualOverride: () -> Unit,
    onClearManualOverride: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.debug_clock_title),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.debug_clock_next_day_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(id = R.string.debug_clock_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.isNextDayEnabled,
                    onCheckedChange = onToggleNextDay
                )
            }
            if (state.isNextDayEnabled) {
                Text(
                    text = stringResource(id = R.string.debug_clock_active),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.debug_clock_manual_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = state.manualOverrideInput,
                    onValueChange = onManualInputChanged,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    placeholder = {
                        Text(text = stringResource(id = R.string.debug_clock_manual_placeholder))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(id = R.string.debug_clock_manual_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onApplyManualOverride,
                        enabled = state.manualOverrideInput.isNotBlank()
                    ) {
                        Text(text = stringResource(id = R.string.debug_clock_manual_apply))
                    }
                    OutlinedButton(
                        onClick = onClearManualOverride,
                        enabled = state.isManualOverrideActive
                    ) {
                        Text(text = stringResource(id = R.string.debug_clock_manual_clear))
                    }
                }
            }
            if (state.isManualOverrideActive) {
                val manualLabel = state.manualOverrideLabel ?: state.manualOverrideInput
                if (manualLabel.isNotBlank()) {
                    Text(
                        text = stringResource(id = R.string.debug_clock_manual_active, manualLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            state.lastAppliedAt?.let { instant ->
                val label = formatWeatherTimestamp(instant)
                Text(
                    text = stringResource(id = R.string.debug_clock_last_applied, label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WearFeedbackDebugCard(state: WearFeedbackDebugUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.debug_feedback_title),
                style = MaterialTheme.typography.titleMedium
            )
            state.lastUpdatedAt?.let { instant ->
                Text(
                    text = stringResource(id = R.string.debug_feedback_last_updated, formatWeatherTimestamp(instant)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.messages.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.debug_feedback_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.messages.forEach { message ->
                        Text(
                            text = "• ${message.resolve()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.dashboard_suggestions_section_title),
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(suggestions, key = { "${it.top.id}-${it.bottom.id}-${it.outer?.id ?: "none"}" }) { suggestion ->
                val isSelected = suggestion == selectedSuggestion
                OutfitSuggestionCard(
                    suggestion = suggestion,
                    selected = isSelected,
                    onClick = { onSuggestionSelected(suggestion) },
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 320.dp)
                )
            }
        }
    }
}

@Composable
private fun OutfitSuggestionCard(
    suggestion: OutfitSuggestion,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
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
            suggestion.outer?.let { outerItem ->
                HorizontalDivider()
                SuggestionItemPreview(
                    label = stringResource(id = R.string.dashboard_suggestion_outer),
                    item = outerItem
                )
            }
            SuggestionMeta(text = stringResource(id = R.string.dashboard_suggestion_formality, suggestion.totalScore))
        }
    }
}

@Composable
private fun SuggestionItemPreview(label: String, item: ClothingItem) {
    val colorOption = remember(item.colorHex) {
        closetColorOptions().firstOrNull { option ->
            option.colorHex.equals(item.colorHex, ignoreCase = true)
        }
    }
    val colorLabel = when {
        colorOption != null -> stringResource(id = colorOption.labelResId)
        item.colorGroup != ColorGroup.UNKNOWN -> stringResource(id = item.colorGroup.labelResId())
        else -> item.colorHex.removePrefix("#").uppercase(Locale.getDefault())
    }
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = item.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(id = item.category.labelResId()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(id = R.string.dashboard_suggestion_color, colorLabel),
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

private const val WEAR_NOTIFICATION_CHANNEL_ID = "wear_notifications"
private const val WEAR_NOTIFICATION_ID = 1001

private fun ensureWearNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(WEAR_NOTIFICATION_CHANNEL_ID)
        if (existing == null) {
            val channelName = context.getString(R.string.notification_channel_wear_title)
            val channel = NotificationChannel(
                WEAR_NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_wear_description)
            }
            manager.createNotificationChannel(channel)
        }
    }
}

private fun showWearNotification(context: Context, messages: List<UiMessage>) {
    if (messages.isEmpty()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            return
        }
    }

    val resolvedMessages = messages.map { it.resolve(context) }
    val contentTitle = context.getString(R.string.notification_wear_title)
    val contentText = resolvedMessages.first()

    val notificationManager = NotificationManagerCompat.from(context)
    if (!notificationManager.areNotificationsEnabled()) {
        return
    }

    val builder = NotificationCompat.Builder(context, WEAR_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_wear)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setStyle(
            NotificationCompat.InboxStyle().also { style ->
                resolvedMessages.forEach(style::addLine)
            }
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setAutoCancel(true)

    notificationManager.notify(WEAR_NOTIFICATION_ID, builder.build())
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    MyApplicationTheme {
        val container = InMemoryAppContainer()
        androidx.compose.runtime.CompositionLocalProvider(
            LocalAppContainer provides container
        ) {
            val weatherDebugState = WeatherDebugUiState(
                minTemperatureInput = "18.0",
                maxTemperatureInput = "27.0",
                humidityInput = "65",
                isOverrideActive = false
            )
            val clockDebugState = ClockDebugUiState(isNextDayEnabled = false)
            val feedbackDebugState = WearFeedbackDebugUiState(
                messages = listOf(
                    UiMessage(R.string.wear_message_remaining, listOf(UiMessageArg.Raw("ネイビーポロ"), UiMessageArg.Raw(2)))
                ),
                lastUpdatedAt = InstantCompat.nowOrNull()
            )
            DashboardContent(
                state = DashboardUiState(
                    isLoading = false,
                    mode = TpoMode.CASUAL,
                    environment = EnvironmentMode.OUTDOOR,
                    suggestions = listOf(
                        OutfitSuggestion(
                            top = SampleData.closetItems[0],
                            bottom = SampleData.closetItems[1],
                            outer = SampleData.closetItems[6],
                            totalScore = 7
                        ),
                        OutfitSuggestion(
                            top = SampleData.closetItems[2],
                            bottom = SampleData.closetItems[3],
                            outer = SampleData.closetItems[6],
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
                        outer = SampleData.closetItems[6],
                        totalScore = 7
                    ),
                    isRefreshingWeather = false,
                    lastWeatherUpdatedAt = InstantCompat.nowOrNull(),
                    weatherErrorMessage = UiMessage(R.string.dashboard_weather_error),
                    weatherDebug = weatherDebugState,
                    clockDebug = clockDebugState,
                    wearFeedbackDebug = feedbackDebugState,
                    casualForecast = CasualForecastUiState(
                        dayOptions = listOf(CasualForecastDay.TODAY, CasualForecastDay.TOMORROW),
                        segmentOptions = listOf(
                            CasualForecastSegmentOption(CasualForecastSegment.MORNING, true),
                            CasualForecastSegmentOption(CasualForecastSegment.AFTERNOON, true),
                            CasualForecastSegmentOption(CasualForecastSegment.EVENING, false)
                        ),
                        selectedDay = CasualForecastDay.TODAY,
                        selectedSegment = CasualForecastSegment.AFTERNOON,
                        summary = CasualForecastSummary(
                            minTemperatureCelsius = 21.0,
                            maxTemperatureCelsius = 24.0,
                            averageApparentTemperatureCelsius = 22.5
                        )
                    )
                ),
                onModeSelected = {},
                onEnvironmentSelected = {},
                onSuggestionSelected = {},
                onWearClicked = {},
                onRerollSuggestion = {},
                onReviewInventory = {},
                onDismissReviewInventory = {},
                onRefreshWeather = {},
                onCasualDaySelected = {},
                onCasualSegmentSelected = {},
                onClockDebugNextDayChanged = {},
                onClockDebugManualInputChanged = {},
                onApplyClockDebugManualOverride = {},
                onClearClockDebugManualOverride = {},
                onDismissComebackDialog = {},
                onDebugMinTempChanged = {},
                onDebugMaxTempChanged = {},
                onDebugHumidityChanged = {},
                onApplyWeatherDebug = {},
                onClearWeatherDebug = {}
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

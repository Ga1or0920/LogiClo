package com.example.myapplication.ui.dashboard

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.InMemoryAppContainer
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.domain.model.CasualForecastDay
import com.example.myapplication.domain.model.CasualForecastSegment
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.ui.dashboard.model.OutfitSuggestion
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.ui.closet.closetColorOptions
import com.example.myapplication.ui.common.UiMessage
import com.example.myapplication.ui.common.UiMessageArg
import com.example.myapplication.ui.common.formatWeatherTimestamp
import com.example.myapplication.ui.common.labelResId
import com.example.myapplication.ui.common.resolve
import com.example.myapplication.ui.components.ClothingIllustrationSwatch
import com.example.myapplication.ui.dashboard.DashboardViewModel
import com.example.myapplication.ui.dashboard.model.AlertSeverity
import com.example.myapplication.ui.dashboard.model.CasualForecastSegmentOption
import com.example.myapplication.ui.dashboard.model.CasualForecastSummary
import com.example.myapplication.ui.dashboard.model.CasualForecastUiState
import com.example.myapplication.ui.dashboard.model.DashboardUiState
import com.example.myapplication.ui.dashboard.model.InventoryAlert
import com.example.myapplication.ui.dashboard.model.LocationSearchResultUiState
import com.example.myapplication.ui.dashboard.model.LocationSearchUiState
import com.example.myapplication.ui.dashboard.model.MapPickerUiState
import com.example.myapplication.ui.dashboard.model.WearFeedbackDebugUiState
import com.example.myapplication.ui.dashboard.model.WeatherDebugUiState
import com.example.myapplication.ui.dashboard.model.ClockDebugUiState
import com.example.myapplication.ui.dashboard.model.WeatherLocationUiState
import com.example.myapplication.ui.providers.LocalAppContainer
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.time.InstantCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.time.Instant
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
            wearFeedbackRepository = appContainer.wearFeedbackRepository,
            weatherDebugController = appContainer.weatherDebugController,
            clockDebugController = appContainer.clockDebugController,
            locationSearchRepository = appContainer.locationSearchRepository,
            stringResolver = context::getString
        )
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isMapSupported = remember(context) {
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

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
        onWeatherLocationDialogDismissed = viewModel::onWeatherLocationDialogDismissed,
        onWeatherLocationLabelChanged = viewModel::onWeatherLocationLabelChanged,
        onWeatherLocationLatitudeChanged = viewModel::onWeatherLocationLatitudeChanged,
        onWeatherLocationLongitudeChanged = viewModel::onWeatherLocationLongitudeChanged,
        onUseDeviceLocation = viewModel::onUseDeviceLocationSelected,
        onApplyWeatherLocationOverride = viewModel::onApplyWeatherLocationOverride,
        onLocationSearchDismissed = viewModel::onLocationSearchDismissed,
        onLocationSearchQueryChanged = viewModel::onLocationSearchQueryChanged,
        onLocationSearchResultSelected = viewModel::onLocationSearchResultSelected,
        onMapPickerDismissed = viewModel::onMapPickerDismissed,
        onMapPickerLabelChanged = viewModel::onMapPickerLabelChanged,
        onMapPickerCoordinateChanged = viewModel::onMapPickerLocationChanged,
        onMapPickerConfirm = viewModel::onMapPickerConfirmed,
        onDismissComebackDialog = viewModel::onComebackDialogDismissed,
        isMapSupported = isMapSupported,
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
    onWeatherLocationDialogDismissed: () -> Unit,
    onWeatherLocationLabelChanged: (String) -> Unit,
    onWeatherLocationLatitudeChanged: (String) -> Unit,
    onWeatherLocationLongitudeChanged: (String) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onApplyWeatherLocationOverride: () -> Unit,
    onLocationSearchDismissed: () -> Unit,
    onLocationSearchQueryChanged: (String) -> Unit,
    onLocationSearchResultSelected: (String) -> Unit,
    onMapPickerDismissed: () -> Unit,
    onMapPickerLabelChanged: (String) -> Unit,
    onMapPickerCoordinateChanged: (Double, Double) -> Unit,
    onMapPickerConfirm: () -> Unit,
    onDismissComebackDialog: () -> Unit,
    isMapSupported: Boolean,
    modifier: Modifier = Modifier
) {
    var isMainMenuVisible by rememberSaveable { mutableStateOf(false) }
    var mainMenuDestination by rememberSaveable { mutableStateOf("") }
    var isWearConfirmVisible by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        OverviewTabContent(
            state = state,
            onSuggestionSelected = onSuggestionSelected,
            onWearClicked = { isWearConfirmVisible = true },
            onRerollSuggestion = onRerollSuggestion,
            onReviewInventory = onReviewInventory,
            onRefreshWeather = onRefreshWeather,
            onCasualDaySelected = onCasualDaySelected,
            onCasualSegmentSelected = onCasualSegmentSelected,
            modifier = Modifier.fillMaxSize()
        )

        MainMenuButton(
            onClick = { isMainMenuVisible = true },
            isActive = isMainMenuVisible,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        )

        if (isMainMenuVisible) {
            MainMenuDialog(
                currentMode = state.mode,
                currentEnvironment = state.environment,
                destination = mainMenuDestination,
                onModeSelected = onModeSelected,
                onEnvironmentSelected = onEnvironmentSelected,
                onDestinationChanged = { mainMenuDestination = it },
                onDismiss = { isMainMenuVisible = false }
            )
        }
    }

    if (isWearConfirmVisible) {
        WearConfirmDialog(
            onConfirm = {
                isWearConfirmVisible = false
                onWearClicked()
            },
            onDismiss = { isWearConfirmVisible = false }
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

    if (state.weatherLocation.isDialogVisible) {
        WeatherLocationDialog(
            state = state.weatherLocation,
            onDismiss = onWeatherLocationDialogDismissed,
            onLabelChanged = onWeatherLocationLabelChanged,
            onLatitudeChanged = onWeatherLocationLatitudeChanged,
            onLongitudeChanged = onWeatherLocationLongitudeChanged,
            onUseDeviceLocation = onUseDeviceLocation,
            onApply = onApplyWeatherLocationOverride
        )
    }

    if (state.locationSearch.isVisible) {
        LocationSearchDialog(
            state = state.locationSearch,
            onDismiss = onLocationSearchDismissed,
            onQueryChanged = onLocationSearchQueryChanged,
            onResultSelected = onLocationSearchResultSelected
        )
    }

    if (isMapSupported && state.mapPicker.isVisible) {
        MapPickerDialog(
            state = state.mapPicker,
            onDismiss = onMapPickerDismissed,
            onLabelChanged = onMapPickerLabelChanged,
            onConfirm = onMapPickerConfirm,
            onPositionChanged = onMapPickerCoordinateChanged
        )
    }
}

@Composable
private fun OverviewTabContent(
    state: DashboardUiState,
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

        item {
            ActionButtons(
                onRerollSuggestion = onRerollSuggestion,
                onWearClicked = onWearClicked,
                isRerollEnabled = state.totalSuggestionCount > 1,
                isWearEnabled = state.selectedSuggestion != null
            )
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
    }
}

    @Composable
    private fun MainMenuDialog(
        currentMode: TpoMode,
        currentEnvironment: EnvironmentMode,
        destination: String,
        onModeSelected: (TpoMode) -> Unit,
        onEnvironmentSelected: (EnvironmentMode) -> Unit,
        onDestinationChanged: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
            val dismissInteraction = remember { MutableInteractionSource() }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrimColor)
                        .clickable(
                            interactionSource = dismissInteraction,
                            indication = null,
                            onClick = onDismiss
                        )
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    MainMenuContent(
                        currentMode = currentMode,
                        currentEnvironment = currentEnvironment,
                        destination = destination,
                        onModeSelected = onModeSelected,
                        onEnvironmentSelected = onEnvironmentSelected,
                        onDestinationChanged = onDestinationChanged
                    )
                }

                MainMenuButton(
                    onClick = onDismiss,
                    isActive = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                )
            }
        }
    }

    @Composable
    private fun MainMenuContent(
        currentMode: TpoMode,
        currentEnvironment: EnvironmentMode,
        destination: String,
        onModeSelected: (TpoMode) -> Unit,
        onEnvironmentSelected: (EnvironmentMode) -> Unit,
        onDestinationChanged: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.dashboard_mode_selector_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuSelectionButton(
                        label = stringResource(id = R.string.dashboard_mode_casual),
                        selected = currentMode == TpoMode.CASUAL,
                        onClick = { onModeSelected(TpoMode.CASUAL) }
                    )
                    MenuSelectionButton(
                        label = stringResource(id = R.string.dashboard_mode_office),
                        selected = currentMode == TpoMode.OFFICE,
                        onClick = { onModeSelected(TpoMode.OFFICE) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.dashboard_environment_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuSelectionButton(
                        label = stringResource(id = R.string.dashboard_environment_outdoor),
                        selected = currentEnvironment == EnvironmentMode.OUTDOOR,
                        onClick = { onEnvironmentSelected(EnvironmentMode.OUTDOOR) }
                    )
                    MenuSelectionButton(
                        label = stringResource(id = R.string.main_menu_environment_indoor),
                        selected = currentEnvironment == EnvironmentMode.INDOOR,
                        onClick = { onEnvironmentSelected(EnvironmentMode.INDOOR) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.main_menu_destination_label),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = onDestinationChanged,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(text = stringResource(id = R.string.main_menu_destination_placeholder))
                    }
                )
            }
        }
    }

    @Composable
    private fun RowScope.MenuSelectionButton(
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        if (selected) {
            Button(
                onClick = onClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = label)
            }
        } else {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = label)
            }
        }
    }

    @Composable
    private fun MainMenuButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        isActive: Boolean = false
    ) {
        val containerColor = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        }
        val contentColor = if (isActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        val buttonDescription = if (isActive) {
            stringResource(id = R.string.main_menu_button_close_cd)
        } else {
            stringResource(id = R.string.main_menu_button_open_cd)
        }

        Surface(
            modifier = modifier
                .size(48.dp)
                .semantics { contentDescription = buttonDescription },
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = if (isActive) 6.dp else 2.dp,
            shadowElevation = 6.dp,
            onClick = onClick
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "三",
                    style = MaterialTheme.typography.titleLarge
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
private fun WeatherLocationDialog(
    state: WeatherLocationUiState,
    onDismiss: () -> Unit,
    onLabelChanged: (String) -> Unit,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onApply: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.dashboard_weather_location_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.labelInput,
                    onValueChange = onLabelChanged,
                    label = { Text(text = stringResource(id = R.string.dashboard_weather_location_dialog_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.latitudeInput,
                    onValueChange = onLatitudeChanged,
                    label = { Text(text = stringResource(id = R.string.dashboard_weather_location_dialog_latitude)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.longitudeInput,
                    onValueChange = onLongitudeChanged,
                    label = { Text(text = stringResource(id = R.string.dashboard_weather_location_dialog_longitude)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onUseDeviceLocation) {
                    Text(text = stringResource(id = R.string.dashboard_weather_location_dialog_use_device))
                }
                Button(onClick = onApply) {
                    Text(text = stringResource(id = R.string.dashboard_weather_location_dialog_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun LocationSearchDialog(
    state: LocationSearchUiState,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onResultSelected: (String) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.dashboard_weather_location_search_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    label = { Text(text = stringResource(id = R.string.dashboard_weather_location_search_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                val instructionsNeeded = state.query.trim().length < LOCATION_SEARCH_MIN_QUERY_LENGTH
                if (instructionsNeeded && !state.isSearching && state.errorMessage == null) {
                    Text(
                        text = stringResource(id = R.string.dashboard_weather_location_search_instructions),
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
                if (state.results.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(state.results, key = { it.id }) { result ->
                            LocationSearchResultRow(
                                result = result,
                                onClick = { onResultSelected(result.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dashboard_weather_location_search_close))
            }
        }
    )
}

@Composable
@androidx.compose.ui.UiComposable
private fun MapPickerDialog(
    state: MapPickerUiState,
    onDismiss: () -> Unit,
    onLabelChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onPositionChanged: (Double, Double) -> Unit
) {
    val mapView = rememberMapView()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var lastTarget by remember { mutableStateOf<LatLng?>(null) }
    var lastZoom by remember { mutableStateOf<Float?>(null) }
    val targetZoom = if (state.zoom > 0f) state.zoom else MAP_PICKER_DEFAULT_ZOOM

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dashboard_weather_location_map_dialog_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            mapView.apply {
                                getMapAsync { map ->
                                    googleMap = map.apply {
                                        uiSettings.isZoomControlsEnabled = true
                                        uiSettings.isMapToolbarEnabled = false
                                        setOnMapClickListener { latLng ->
                                            onPositionChanged(latLng.latitude, latLng.longitude)
                                        }
                                    }
                                }
                            }
                        },
                        update = {
                            val map = googleMap ?: return@AndroidView
                            val target = LatLng(state.latitude, state.longitude)
                            val previous = lastTarget
                            val previousZoom = lastZoom
                            val cameraUpdate = when {
                                previous == null || previousZoom == null ->
                                    CameraUpdateFactory.newLatLngZoom(target, targetZoom)
                                previousZoom != targetZoom ->
                                    CameraUpdateFactory.newLatLngZoom(target, targetZoom)
                                previous != target ->
                                    CameraUpdateFactory.newLatLng(target)
                                else -> null
                            }
                            cameraUpdate?.let { update ->
                                if (previous == null) {
                                    map.moveCamera(update)
                                } else {
                                    map.animateCamera(update)
                                }
                            }
                            lastTarget = target
                            lastZoom = targetZoom
                            val currentMarker = marker
                            if (state.hasLocationSelection) {
                                if (currentMarker == null) {
                                    marker = map.addMarker(MarkerOptions().position(target))
                                } else {
                                    currentMarker.position = target
                                }
                            } else {
                                currentMarker?.remove()
                                marker = null
                            }
                        }
                    )
                }
                Text(
                    text = stringResource(id = R.string.dashboard_weather_location_map_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        id = R.string.dashboard_weather_location_coordinates,
                        state.latitude,
                        state.longitude
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = state.labelInput,
                    onValueChange = onLabelChanged,
                    label = { Text(text = stringResource(id = R.string.dashboard_weather_location_map_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.common_cancel))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = state.isConfirmEnabled
                    ) {
                        Text(text = stringResource(id = R.string.dashboard_weather_location_map_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSearchResultRow(
    result: LocationSearchResultUiState,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = result.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        result.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
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
private fun WearConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.dashboard_wear_confirm_title))
        },
        text = {
            Text(text = stringResource(id = R.string.dashboard_wear_confirm_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.dashboard_wear_confirm_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dashboard_wear_confirm_cancel))
            }
        }
    )
}

@Composable
private fun ActionButtons(
    onRerollSuggestion: () -> Unit,
    onWearClicked: () -> Unit,
    isRerollEnabled: Boolean,
    isWearEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onWearClicked,
            enabled = isWearEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.dashboard_button_wear))
        }

        OutlinedButton(
            onClick = onRerollSuggestion,
            enabled = isRerollEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.dashboard_button_reroll))
        }
    }
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

private const val MAP_PICKER_DEFAULT_ZOOM = 9.5f
private const val LOCATION_SEARCH_MIN_QUERY_LENGTH = 2

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
                    weatherLocation = WeatherLocationUiState(
                        displayLabel = "場所指定: 東京駅",
                        description = "緯度 35.6810 / 経度 139.7670",
                        isOverrideActive = true
                    ),
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
                onWeatherLocationDialogDismissed = {},
                onWeatherLocationLabelChanged = {},
                onWeatherLocationLatitudeChanged = {},
                onWeatherLocationLongitudeChanged = {},
                onUseDeviceLocation = {},
                onApplyWeatherLocationOverride = {},
                onLocationSearchDismissed = {},
                onLocationSearchQueryChanged = {},
                onLocationSearchResultSelected = {},
                onMapPickerDismissed = {},
                onMapPickerLabelChanged = {},
                onMapPickerCoordinateChanged = { _, _ -> },
                onMapPickerConfirm = {},
                onDismissComebackDialog = {},
                isMapSupported = true
            )
        }
    }
}

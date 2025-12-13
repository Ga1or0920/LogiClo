package com.example.myapplication.ui.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.example.myapplication.domain.model.ThemeOption
import com.example.myapplication.ui.common.formatWeatherTimestamp
import com.example.myapplication.ui.common.resolve
import com.example.myapplication.ui.dashboard.DashboardViewModel
import com.example.myapplication.ui.dashboard.model.ClockDebugUiState
import com.example.myapplication.ui.dashboard.model.WearFeedbackDebugUiState
import com.example.myapplication.ui.dashboard.model.WeatherDebugUiState
import com.example.myapplication.ui.navigation.AppDestination
import com.example.myapplication.ui.providers.LocalAppContainer
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(navController: NavHostController) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val coroutineScope = rememberCoroutineScope()

    val parentEntry = remember(navController) {
        navController.getBackStackEntry(AppDestination.Dashboard.route)
    }
    val dashboardViewModel: DashboardViewModel = viewModel(
        viewModelStoreOwner = parentEntry,
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

    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(appContainer.userPreferencesRepository))

    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val userPreferences by appContainer.userPreferencesRepository.observe().collectAsStateWithLifecycle(null)

    userPreferences?.let {
        SettingsScreen(
            settingsUiState = settingsState,
            weatherDebug = dashboardState.weatherDebug,
            clockDebug = dashboardState.clockDebug,
            wearFeedbackDebug = dashboardState.wearFeedbackDebug,
            themeOption = it.theme,
            onUsernameChanged = settingsViewModel::onUsernameChange,
            onSendEmailLink = settingsViewModel::sendEmailLink,
            onGoogleSignIn = { intent -> settingsViewModel.handleGoogleSignInResult(intent) },
            onLogout = settingsViewModel::logout,
            onThemeChanged = { newTheme ->
                coroutineScope.launch {
                    appContainer.userPreferencesRepository.update { current ->
                        current.copy(theme = newTheme)
                    }
                }
            },
            onClockDebugNextDayChanged = dashboardViewModel::onClockDebugNextDayChanged,
            onClockDebugManualInputChanged = dashboardViewModel::onClockDebugManualOverrideInputChanged,
            onApplyClockDebugManualOverride = dashboardViewModel::applyClockDebugManualOverride,
            onClearClockDebugManualOverride = dashboardViewModel::clearClockDebugManualOverride,
            onDebugMinTempChanged = dashboardViewModel::onDebugMinTemperatureChanged,
            onDebugMaxTempChanged = dashboardViewModel::onDebugMaxTemperatureChanged,
            onDebugHumidityChanged = dashboardViewModel::onDebugHumidityChanged,
            onApplyWeatherDebug = dashboardViewModel::applyWeatherDebugOverride,
            onClearWeatherDebug = dashboardViewModel::clearWeatherDebugOverride
        )
    }
}

@Composable
fun SettingsScreen(
    settingsUiState: SettingsUiState,
    weatherDebug: WeatherDebugUiState?,
    clockDebug: ClockDebugUiState?,
    wearFeedbackDebug: WearFeedbackDebugUiState?,
    themeOption: ThemeOption,
    onUsernameChanged: (String) -> Unit,
    onSendEmailLink: () -> Unit,
    onGoogleSignIn: (Intent?) -> Unit,
    onLogout: () -> Unit,
    onThemeChanged: (ThemeOption) -> Unit,
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
    val context = LocalContext.current
    val hasDebugContent = weatherDebug != null || clockDebug != null || wearFeedbackDebug != null

    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var isDebugEnabled by rememberSaveable(hasDebugContent) { mutableStateOf(hasDebugContent) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onGoogleSignIn(result.data)
            }
        }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.settings_section_account),
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            if (settingsUiState.isAuthenticated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "ログイン済み: ${settingsUiState.username}")
                    Button(onClick = onLogout) {
                        Text(text = "ログアウト")
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = settingsUiState.username,
                        onValueChange = onUsernameChanged,
                        label = { Text(stringResource(id = R.string.settings_account_username_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = {
                            Text(text = stringResource(id = R.string.settings_account_username_placeholder))
                        },
                        isError = settingsUiState.authError != null && !settingsUiState.emailSent
                    )

                    if (settingsUiState.emailSent) {
                        Text("ログイン用のメールを送信しました。メール内のリンクをクリックしてログインしてください。")
                    } else {
                        if (settingsUiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onSendEmailLink,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "メールリンクでログイン")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(context.getString(R.string.default_web_client_id))
                                            .requestEmail()
                                            .build()
                                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = stringResource(id = R.string.settings_login_google))
                                }
                            }
                        }

                        settingsUiState.authError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            Text(
                text = stringResource(id = R.string.settings_section_theme),
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            ThemeSelectorRow(
                selected = themeOption,
                onSelected = onThemeChanged
            )
        }
        item {
            Text(
                text = stringResource(id = R.string.settings_section_notifications),
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            NotificationToggleRow(
                enabled = notificationsEnabled,
                onToggle = { notificationsEnabled = it }
            )
        }
        item {
            Text(
                text = stringResource(id = R.string.settings_section_debug),
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            DebugToggleRow(
                isEnabled = isDebugEnabled,
                hasDebugContent = hasDebugContent,
                onCheckedChange = { enabled ->
                    if (enabled && !hasDebugContent) {
                        Toast.makeText(context, context.getString(R.string.settings_debug_unavailable), Toast.LENGTH_SHORT).show()
                    } else {
                        isDebugEnabled = enabled
                    }
                }
            )
        }

        if (!hasDebugContent) {
            item {
                Text(
                    text = stringResource(id = R.string.settings_debug_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isDebugEnabled) {
            weatherDebug?.let {
                item { WeatherDebugCard(it, onDebugMinTempChanged, onDebugMaxTempChanged, onDebugHumidityChanged, onApplyWeatherDebug, onClearWeatherDebug) }
            }
            clockDebug?.let {
                item { ClockDebugCard(it, onClockDebugNextDayChanged, onClockDebugManualInputChanged, onApplyClockDebugManualOverride, onClearClockDebugManualOverride) }
            }
            wearFeedbackDebug?.let {
                item { WearFeedbackDebugCard(it) }
            }
        }
    }
}

@Composable
private fun DebugToggleRow(
    isEnabled: Boolean,
    hasDebugContent: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isEnabled) stringResource(id = R.string.settings_debug_on) else stringResource(id = R.string.settings_debug_off),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onCheckedChange,
            enabled = hasDebugContent || isEnabled
        )
    }
}

@Composable
private fun NotificationToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (enabled) stringResource(id = R.string.settings_notifications_on) else stringResource(id = R.string.settings_notifications_off),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectorRow(
    selected: ThemeOption,
    onSelected: (ThemeOption) -> Unit
) {
    val options = ThemeOption.entries
    BoxWithConstraints {
        val segmentWidth = if (options.isNotEmpty()) maxWidth / options.size else maxWidth
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    modifier = Modifier.width(segmentWidth),
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(text = stringResource(id = option.labelRes)) }
                )
            }
        }
    }
}

@get:StringRes
private val ThemeOption.labelRes: Int
    get() = when (this) {
        ThemeOption.LIGHT -> R.string.settings_theme_light
        ThemeOption.DARK -> R.string.settings_theme_dark
        ThemeOption.SYSTEM -> R.string.settings_theme_system
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
                OutlinedButton(onClick = onClear, enabled = state.isOverrideActive) {
                    Text(text = stringResource(id = R.string.debug_weather_clear))
                }
            }
            if (state.isOverrideActive) {
                Text(
                    text = stringResource(id = R.string.debug_weather_active),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            state.errorMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(id = R.string.debug_clock_next_day_label), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.isNextDayEnabled, onCheckedChange = onToggleNextDay)
            }
            OutlinedTextField(
                value = state.manualOverrideInput,
                onValueChange = onManualInputChanged,
                label = { Text(stringResource(id = R.string.debug_clock_manual_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                placeholder = { Text(text = stringResource(id = R.string.debug_clock_manual_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onApplyManualOverride, enabled = state.manualOverrideInput.isNotBlank()) {
                    Text(text = stringResource(id = R.string.debug_clock_manual_apply))
                }
                OutlinedButton(onClick = onClearManualOverride, enabled = state.isManualOverrideActive) {
                    Text(text = stringResource(id = R.string.debug_clock_manual_clear))
                }
            }
            state.errorMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
            if (state.messages.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.debug_feedback_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.messages.forEach { message ->
                    Text(
                        text = message.resolve(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

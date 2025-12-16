package com.example.myapplication.ui.settings

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Slider
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.example.myapplication.ui.common.formatWeatherTimestamp
import com.example.myapplication.ui.common.resolve
import com.example.myapplication.ui.dashboard.DashboardViewModel
import com.example.myapplication.ui.dashboard.model.ClockDebugUiState
import com.example.myapplication.ui.dashboard.model.WearFeedbackDebugUiState
import com.example.myapplication.ui.dashboard.model.WeatherDebugUiState
import com.example.myapplication.ui.navigation.AppDestination
import com.example.myapplication.ui.providers.LocalAppContainer

@Composable
fun SettingsRoute(navController: NavHostController) {
	val appContainer = LocalAppContainer.current
	val context = LocalContext.current
	val parentEntry = remember(navController) {
		navController.getBackStackEntry(AppDestination.Dashboard.route)
	}
	val viewModel: DashboardViewModel = viewModel(
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

	val state by viewModel.uiState.collectAsStateWithLifecycle()

	   SettingsScreen(
		   weatherDebug = state.weatherDebug,
		   clockDebug = state.clockDebug,
		   wearFeedbackDebug = state.wearFeedbackDebug,
		   onClockDebugNextDayChanged = viewModel::onClockDebugNextDayChanged,
		   onClockDebugManualInputChanged = viewModel::onClockDebugManualOverrideInputChanged,
		   onApplyClockDebugManualOverride = viewModel::applyClockDebugManualOverride,
		   onClearClockDebugManualOverride = viewModel::clearClockDebugManualOverride,
		   onDebugMinTempChanged = viewModel::onDebugMinTemperatureChanged,
		   onDebugMaxTempChanged = viewModel::onDebugMaxTemperatureChanged,
		   onDebugHumidityChanged = viewModel::onDebugHumidityChanged,
		   onApplyWeatherDebug = viewModel::applyWeatherDebugOverride,
		   onClearWeatherDebug = viewModel::clearWeatherDebugOverride,
		   navController = navController
	   )
}

@Composable
fun SettingsScreen(
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
	navController: NavHostController,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	val hasDebugContent = weatherDebug != null || clockDebug != null || wearFeedbackDebug != null
	var username by rememberSaveable { mutableStateOf("") }
	var password by rememberSaveable { mutableStateOf("") }
	var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
	val themeState = com.example.myapplication.ui.theme.LocalThemePreference.current
	var selectedTheme by rememberSaveable { mutableStateOf(PreferenceToSettingsThemeOption(themeState.value)) }
	var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
	var isDebugEnabled by rememberSaveable(hasDebugContent) { mutableStateOf(hasDebugContent) }

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
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				Text(
					text = stringResource(id = R.string.settings_account_username_label),
					style = MaterialTheme.typography.bodyLarge
				)
				OutlinedTextField(
					value = username,
					onValueChange = { username = it },
					singleLine = true,
					modifier = Modifier.fillMaxWidth(),
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
					placeholder = {
						Text(text = stringResource(id = R.string.settings_account_username_placeholder))
					}
				)
				Text(
					text = stringResource(id = R.string.settings_account_password_label),
					style = MaterialTheme.typography.bodyLarge
				)
				OutlinedTextField(
					value = password,
					onValueChange = { password = it },
					singleLine = true,
					modifier = Modifier.fillMaxWidth(),
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
					visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
					placeholder = {
						Text(text = stringResource(id = R.string.settings_account_password_placeholder))
					},
					trailingIcon = {
						IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
							Icon(
								imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
								contentDescription = stringResource(
									if (isPasswordVisible) R.string.settings_account_password_hide else R.string.settings_account_password_show
								)
							)
						}
					}
				)
				Text(
					text = stringResource(id = R.string.settings_account_forgot_password),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.primary,
					textDecoration = TextDecoration.Underline,
					modifier = Modifier.clickable {
						Toast.makeText(context, context.getString(R.string.settings_account_forgot_password_feedback), Toast.LENGTH_SHORT).show()
					}
				)
				Text(
					text = stringResource(id = R.string.settings_account_register),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.primary,
					textDecoration = TextDecoration.Underline,
					modifier = Modifier.clickable {
						Toast.makeText(context, context.getString(R.string.settings_account_register_feedback), Toast.LENGTH_SHORT).show()
					}
				)
				Button(
					onClick = {
						Toast.makeText(context, context.getString(R.string.settings_login_google), Toast.LENGTH_SHORT).show()
					},
					modifier = Modifier.fillMaxWidth()
				) {
					Text(text = stringResource(id = R.string.settings_login_google))
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
				selected = selectedTheme,
				onSelected = {
					selectedTheme = it
					themeState.value = SettingsThemeOptionToPreference(it)
				}
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
				text = stringResource(id = R.string.settings_section_reset),
				style = MaterialTheme.typography.titleLarge
			)
		}
		item {
			OutlinedButton(
				onClick = {
					Toast.makeText(context, context.getString(R.string.settings_reset_feedback), Toast.LENGTH_SHORT).show()
				},
				modifier = Modifier.fillMaxWidth()
			) {
				Text(text = stringResource(id = R.string.settings_reset_all))
			}
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
			   // デバッグ用：着用フィードバック動作確認ボタン
			   item {
				   Button(
					   onClick = { navController.navigate("feedback/pending") },
					   modifier = Modifier
						   .fillMaxWidth()
						   .padding(vertical = 8.dp)
				   ) {
					   Text("着用フィードバック動作確認")
				   }
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
			text = if (isEnabled) {
				stringResource(id = R.string.settings_debug_on)
			} else {
				stringResource(id = R.string.settings_debug_off)
			},
			style = MaterialTheme.typography.bodyLarge,
			color = if (isEnabled) {
				MaterialTheme.colorScheme.primary
			} else {
				MaterialTheme.colorScheme.onSurfaceVariant
			},
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
			text = if (enabled) {
				stringResource(id = R.string.settings_notifications_on)
			} else {
				stringResource(id = R.string.settings_notifications_off)
			},
			style = MaterialTheme.typography.bodyLarge,
			color = if (enabled) {
				MaterialTheme.colorScheme.primary
			} else {
				MaterialTheme.colorScheme.onSurfaceVariant
			},
			modifier = Modifier.padding(end = 12.dp)
		)
		Switch(
			checked = enabled,
			onCheckedChange = onToggle
		)
	}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ThemeSelectorRow(
	selected: SettingsThemeOption,
	onSelected: (SettingsThemeOption) -> Unit
) {
	val options = SettingsThemeOption.entries
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

private fun SettingsThemeOptionToPreference(option: SettingsThemeOption): com.example.myapplication.ui.theme.ThemePreference {
	return when(option) {
		SettingsThemeOption.LIGHT -> com.example.myapplication.ui.theme.ThemePreference.LIGHT
		SettingsThemeOption.DARK -> com.example.myapplication.ui.theme.ThemePreference.DARK
		SettingsThemeOption.SYSTEM -> com.example.myapplication.ui.theme.ThemePreference.SYSTEM
	}
}

private fun PreferenceToSettingsThemeOption(pref: com.example.myapplication.ui.theme.ThemePreference): SettingsThemeOption {
	return when(pref) {
		com.example.myapplication.ui.theme.ThemePreference.LIGHT -> SettingsThemeOption.LIGHT
		com.example.myapplication.ui.theme.ThemePreference.DARK -> SettingsThemeOption.DARK
		com.example.myapplication.ui.theme.ThemePreference.SYSTEM -> SettingsThemeOption.SYSTEM
	}
}

private enum class SettingsThemeOption(@StringRes val labelRes: Int) {
	LIGHT(R.string.settings_theme_light),
	DARK(R.string.settings_theme_dark),
	SYSTEM(R.string.settings_theme_system)
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
			// スライダーで温度と湿度を指定できるようにする
			val minTemp = state.minTemperatureInput.toDoubleOrNull() ?: 10.0
			val maxTemp = state.maxTemperatureInput.toDoubleOrNull() ?: 25.0
			val humidity = state.humidityInput.toIntOrNull() ?: 50

			var sliderMin by remember { mutableStateOf(minTemp.toFloat()) }
			var sliderMax by remember { mutableStateOf(maxTemp.toFloat()) }
			var sliderHum by remember { mutableStateOf(humidity.toFloat()) }

			Text(text = stringResource(id = R.string.debug_weather_min_label) + ": ${String.format(Locale.getDefault(), "%.1f", sliderMin)}℃")
			Slider(
				value = sliderMin,
				onValueChange = {
					sliderMin = it
					onMinTempChanged(String.format(Locale.getDefault(), "%.1f", it))
				},
				valueRange = -20f..40f,
				steps = 60,
				modifier = Modifier.fillMaxWidth()
			)
			Text(text = stringResource(id = R.string.debug_weather_max_label) + ": ${String.format(Locale.getDefault(), "%.1f", sliderMax)}℃")
			Slider(
				value = sliderMax,
				onValueChange = {
					sliderMax = it
					onMaxTempChanged(String.format(Locale.getDefault(), "%.1f", it))
				},
				valueRange = -20f..40f,
				steps = 60,
				modifier = Modifier.fillMaxWidth()
			)
			Text(text = stringResource(id = R.string.debug_weather_humidity_label) + ": ${sliderHum.toInt()}%")
			Slider(
				value = sliderHum,
				onValueChange = {
					sliderHum = it
					onHumidityChanged(it.toInt().toString())
				},
				valueRange = 0f..100f,
				steps = 100,
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
				horizontalArrangement = Arrangement.SpaceBetween
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
				val context = LocalContext.current
				val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
				var selectedLabel by remember { mutableStateOf(state.manualOverrideInput) }
				Button(onClick = {
					val now = Calendar.getInstance()
					DatePickerDialog(context, { _, year, month, dayOfMonth ->
						// set to noon by default
						val cal = Calendar.getInstance()
						cal.set(year, month, dayOfMonth, 12, 0, 0)
						val formatted = sdf.format(cal.time)
						selectedLabel = formatted
						onManualInputChanged(formatted)
					}, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
				}) {
					Text(text = if (state.manualOverrideInput.isNotBlank()) state.manualOverrideInput else stringResource(id = R.string.debug_clock_manual_placeholder))
				}
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
							text = message.resolve(),
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurface
						)
					}
				}
			}
		}
	}
}


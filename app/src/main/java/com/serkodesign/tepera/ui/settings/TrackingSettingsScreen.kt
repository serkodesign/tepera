package com.serkodesign.tepera.ui.settings

import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.TeperaDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.data.GapSensitivity
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.HourRangeSlider
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.util.TargetSuggestion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * "Відстеження" — за прямим запитом користувача виокремлено з головного екрана Налаштувань в
 * окремий під-екран (той самий патерн навігації, що Категорії/Виключені застосунки/Ворота —
 * рядок з ">" веде сюди, не інлайн-блок на головній сторінці). Зміст не змінився: орієнтир
 * Online-часу, вікно сну (T-12), чутливість детекції пауз (T-11) — лише переїхали з
 * `SettingsScreen` без зміни власної логіки/копірайтингу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingSettingsScreen(
    settingsStore: SettingsStore,
    sleepWindowRepository: SleepWindowRepository,
    balanceRepository: BalanceRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var targetHours by remember { mutableStateOf<Int?>(null) }
    var showTargetInfo by remember { mutableStateOf(false) }
    var window1StartHour by remember { mutableStateOf(0) }
    var window1EndHour by remember { mutableStateOf(6) }
    var showSleepWindowInfo by remember { mutableStateOf(false) }
    var gapSensitivity by remember { mutableStateOf(GapSensitivity.NORMAL) }
    var showGapSensitivityInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        targetHours = settingsStore.targetMinutes.first()?.let { (it / 60f).roundToInt().coerceIn(TargetSuggestion.MIN_HOURS, TargetSuggestion.MAX_HOURS) }
        val windows = sleepWindowRepository.getWindows()
        windows.find { it.slot == 1 }?.let {
            window1StartHour = (it.startMinuteOfDay / 60).coerceIn(0, 23)
            window1EndHour = (it.endMinuteOfDay / 60).coerceIn(0, 23)
        }
        gapSensitivity = settingsStore.gapSensitivity.first()
    }

    if (showTargetInfo) {
        TeperaDialog(
            onDismissRequest = { showTargetInfo = false },
            text = stringResource(R.string.settings_target_info),
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = { showTargetInfo = false }
        )
    }
    if (showSleepWindowInfo) {
        TeperaDialog(
            onDismissRequest = { showSleepWindowInfo = false },
            text = stringResource(R.string.settings_sleep_window_info),
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = { showSleepWindowInfo = false }
        )
    }
    if (showGapSensitivityInfo) {
        TeperaDialog(
            onDismissRequest = { showGapSensitivityInfo = false },
            text = stringResource(R.string.settings_gap_sensitivity_info),
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = { showGapSensitivityInfo = false }
        )
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.settings_tracking_screen_title), onBack = onBack)

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_target_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showTargetInfo = true }, modifier = Modifier.size(20.dp)) {
                            Icon(TeperaSymbols.Info, contentDescription = stringResource(R.string.settings_target_info))
                        }
                        Spacer(Modifier.weight(1f))
                        // CC-1: орієнтир можна вимкнути зовсім. Вмикаючи, стартуємо від середнього самої
                        // людини (якщо є історія), а не від "стандартного" значення.
                        Switch(
                            checked = targetHours != null,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    if (enabled) {
                                        val average = balanceRepository.averageDailyOnline()
                                        val hours = average?.let { TargetSuggestion.hoursFor(it.minutesPerDay) }
                                            ?: TargetSuggestion.NEUTRAL_START_HOURS
                                        targetHours = hours
                                        settingsStore.setTargetMinutes(hours * 60)
                                    } else {
                                        targetHours = null
                                        settingsStore.setTargetMinutes(null)
                                    }
                                }
                            },
                            colors = teperaSwitchColors()
                        )
                    }
                    val hours = targetHours
                    if (hours != null) {
                        HourRangeSlider(
                            hours = hours,
                            onHoursChange = { newHours ->
                                targetHours = newHours
                                scope.launch { settingsStore.setTargetMinutes(newHours * 60) }
                            },
                            valueLabel = { value -> stringResource(R.string.settings_target_hours_format, value) },
                            minHours = TargetSuggestion.MIN_HOURS,
                            maxHours = TargetSuggestion.MAX_HOURS
                        )
                    } else {
                        Text(
                            stringResource(R.string.settings_target_off_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_sleep_window_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showSleepWindowInfo = true }, modifier = Modifier.size(20.dp)) {
                            Icon(TeperaSymbols.Info, contentDescription = stringResource(R.string.settings_sleep_window_info))
                        }
                    }
                    Text(
                        stringResource(R.string.settings_sleep_window_start_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HourRangeSlider(
                        hours = window1StartHour,
                        onHoursChange = { hour ->
                            window1StartHour = hour
                            scope.launch { sleepWindowRepository.setWindow(1, hour * 60, window1EndHour * 60, enabled = true) }
                        },
                        valueLabel = { hour -> stringResource(R.string.settings_sleep_window_hour_format, hour) },
                        minHours = 0,
                        maxHours = 23
                    )
                    Text(
                        stringResource(R.string.settings_sleep_window_end_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HourRangeSlider(
                        hours = window1EndHour,
                        onHoursChange = { hour ->
                            window1EndHour = hour
                            scope.launch { sleepWindowRepository.setWindow(1, window1StartHour * 60, hour * 60, enabled = true) }
                        },
                        valueLabel = { hour -> stringResource(R.string.settings_sleep_window_hour_format, hour) },
                        minHours = 0,
                        maxHours = 23
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_gap_sensitivity_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showGapSensitivityInfo = true }, modifier = Modifier.size(20.dp)) {
                            Icon(TeperaSymbols.Info, contentDescription = stringResource(R.string.settings_gap_sensitivity_info))
                        }
                    }
                    val sensitivityOptions = listOf(
                        GapSensitivity.RARE to stringResource(R.string.settings_gap_sensitivity_rare),
                        GapSensitivity.NORMAL to stringResource(R.string.settings_gap_sensitivity_normal),
                        GapSensitivity.FREQUENT to stringResource(R.string.settings_gap_sensitivity_frequent)
                    )
                    PillSegmentedControl(
                        options = sensitivityOptions,
                        selected = gapSensitivity,
                        onSelect = { sensitivity ->
                            gapSensitivity = sensitivity
                            scope.launch { settingsStore.setGapSensitivity(sensitivity) }
                        }
                    )
                }
            }
        }
    }
}

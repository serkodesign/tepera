package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.GapDetectionConfig
import com.serkodesign.tepera.data.GapSensitivity
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.ui.gates.TimeChip
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.HourRangeSlider
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.util.TargetSuggestion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * "Відстеження" — під-екран Налаштувань: орієнтир Online-часу, вікно сну (T-12), чутливість детекції пауз (T-11).
 *
 * **Розкладка за best practices налаштувань (M3):** кожне налаштування — окрема картка-група з іконкою, назвою й
 * поясненням прямо в картці (а не лише за ⓘ), елемент керування — поруч із назвою (перемикач) або одразу під нею;
 * поточне значення видно текстом ("02:00 – 07:00"), а не лише положенням повзунка. Прогресивне розкриття:
 * повзунок орієнтира з'являється, лише коли орієнтир увімкнений.
 * - Орієнтир: перемикач у заголовку картки + повзунок 1-8 год; пояснення (що це і що нічого не оцінює) — під назвою.
 * - Вікно сну: два поля часу ("Початок"/"Кінець", системний вибір часу до хвилини) замість двох великих повзунків
 *   годин — інтервал читається одним рядком, і його не треба вгадувати за довжиною заповнення.
 * - Пауза: три пресети + рядок із реальними порогами обраного пресета (мінімум хвилин і стеля на день).
 */
@Composable
fun TrackingSettingsScreen(
    settingsStore: SettingsStore,
    sleepWindowRepository: SleepWindowRepository,
    balanceRepository: BalanceRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var targetHours by remember { mutableStateOf<Int?>(null) }
    var windowStartMinute by remember { mutableStateOf(0) }
    var windowEndMinute by remember { mutableStateOf(6 * 60) }
    var showSleepWindowInfo by remember { mutableStateOf(false) }
    var gapSensitivity by remember { mutableStateOf(GapSensitivity.NORMAL) }
    var showGapSensitivityInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        targetHours = settingsStore.targetMinutes.first()?.let { (it / 60f).roundToInt().coerceIn(TargetSuggestion.MIN_HOURS, TargetSuggestion.MAX_HOURS) }
        sleepWindowRepository.getWindows().find { it.slot == 1 }?.let {
            windowStartMinute = it.startMinuteOfDay.coerceIn(0, 24 * 60 - 1)
            windowEndMinute = it.endMinuteOfDay.coerceIn(0, 24 * 60 - 1)
        }
        gapSensitivity = settingsStore.gapSensitivity.first()
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
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Орієнтир Online-часу
                SettingCard(
                    icon = TeperaSymbols.TrackChanges,
                    title = stringResource(R.string.settings_target_label),
                    supporting = stringResource(
                        if (targetHours != null) R.string.settings_target_info else R.string.settings_target_off_hint
                    ),
                    trailing = {
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
                ) {
                    targetHours?.let { hours ->
                        HourRangeSlider(
                            hours = hours,
                            onHoursChange = { newHours ->
                                targetHours = newHours
                                scope.launch { settingsStore.setTargetMinutes(newHours * 60) }
                            },
                            valueLabel = { value -> stringResource(R.string.settings_target_hours_format, value) },
                            minHours = TargetSuggestion.MIN_HOURS,
                            maxHours = TargetSuggestion.MAX_HOURS,
                            accessibilityLabel = stringResource(R.string.settings_target_label)
                        )
                    }
                }

                // --- Вікно сну
                SettingCard(
                    icon = TeperaSymbols.Bedtime,
                    title = stringResource(R.string.settings_sleep_window_label),
                    supporting = stringResource(R.string.settings_sleep_window_hint),
                    onInfo = { showSleepWindowInfo = true },
                    infoDescription = stringResource(R.string.settings_sleep_window_info)
                ) {
                    // Поточний інтервал одним рядком — головне значення картки.
                    Text(
                        text = "%s – %s".format(formatMinute(windowStartMinute), formatMinute(windowEndMinute)),
                        color = TeperaPalette.buttonBrandDark,
                        fontFamily = TeperaPalette.headlineFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        lineHeight = 26.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeChip(
                            label = stringResource(R.string.settings_sleep_window_start_label),
                            minute = windowStartMinute,
                            onSelected = { minute ->
                                windowStartMinute = minute
                                scope.launch { sleepWindowRepository.setWindow(1, minute, windowEndMinute, enabled = true) }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = Color.White,
                            borderColor = TimeFieldBorder
                        )
                        TimeChip(
                            label = stringResource(R.string.settings_sleep_window_end_label),
                            minute = windowEndMinute,
                            onSelected = { minute ->
                                windowEndMinute = minute
                                scope.launch { sleepWindowRepository.setWindow(1, windowStartMinute, minute, enabled = true) }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = Color.White,
                            borderColor = TimeFieldBorder
                        )
                    }
                }

                // --- Чутливість детекції пауз
                val config = GapDetectionConfig.forSensitivity(gapSensitivity)
                SettingCard(
                    icon = TeperaSymbols.PauseCircle,
                    title = stringResource(R.string.settings_gap_sensitivity_label),
                    supporting = stringResource(
                        R.string.settings_gap_sensitivity_hint_format, config.minGapMinutes, config.maxGapsPerDay
                    ),
                    onInfo = { showGapSensitivityInfo = true },
                    infoDescription = stringResource(R.string.settings_gap_sensitivity_info)
                ) {
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

/**
 * Картка одного налаштування: іконка в кружку, назва (+ необов'язкова ⓘ з докладним поясненням), стисле пояснення
 * під назвою, [trailing] (напр. перемикач) праворуч у заголовку й [content] під ним (повзунок, поля часу тощо).
 */
@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    supporting: String,
    modifier: Modifier = Modifier,
    onInfo: (() -> Unit)? = null,
    infoDescription: String = "",
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            TeperaIconCircle(icon)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        title,
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        color = TeperaPalette.buttonBrandDark
                    )
                    if (onInfo != null) {
                        IconButton(onClick = onInfo, modifier = Modifier.size(20.dp)) {
                            Icon(TeperaSymbols.Info, contentDescription = infoDescription)
                        }
                    }
                }
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeperaPalette.buttonBrandDark.copy(alpha = 0.8f)
                )
            }
            trailing()
        }
        content()
    }
}

/** Легка сіра обводка полів часу (#DDE2E4) — за запитом користувача поля білі з тонкою рамкою. */
private val TimeFieldBorder = Color(0xFFDDE2E4)

private fun formatMinute(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

package com.serkodesign.tepera.ui.gates

import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.TeperaDialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.ui.theme.NavChevron
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.ui.theme.TeperaDatePickerDialog
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.util.PauseWindow
import com.serkodesign.tepera.util.startOfTodayMillis
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.diary.EntryChip
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlinx.coroutines.launch

// Тривалість затримки: чіп у списку воріт перемикає їх по колу; старе значення поза набором (20 с з
// попередньої версії) першим тапом переходить на перше (3 с).
private val DELAY_OPTIONS = listOf(3, 5, 10)

/**
 * T-4 (tepera-dev-spec.md, FR-G частина 1): "Застосунки з затримкою". Дії, недоступні до наступного
 * запуску застосунку (напр. якщо `isRequestPinShortcutSupported() == false`), показуються спокійним
 * поясненням замість краху — буквальна вимога приймання.
 */
@Composable
fun GatesScreen(
    gateRepository: GateRepository,
    installedAppsProvider: InstalledAppsProvider,
    onOpenSchedule: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: GatesViewModel = viewModel(
        factory = GatesViewModel.Factory(gateRepository, installedAppsProvider)
    )
    val state by viewModel.uiState.collectAsState()
    val growingDelay by viewModel.growingDelay.collectAsState()
    val scope = rememberCoroutineScope()

    // Видалення ярлика воріт відбувається поза застосунком (long-press на робочому столі) — без
    // цього ефекту список лишався б "активним" до наступного повного перестворення ViewModel.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    var pendingApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var instructionApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var pinFailed by remember { mutableStateOf(false) }

    var showPauseDialog by remember { mutableStateOf(false) }
    var showPauseDatePicker by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.gates_screen_title), onBack = onBack)

            when {
                state.loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.gates_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        // CC-5: ворота активні = зараз вікно розкладу І немає паузи.
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassRow(
                                label = stringResource(R.string.gates_pause_label),
                                onClick = { showPauseDialog = true },
                                leading = { TeperaIconCircle(TeperaSymbols.PauseCircle) },
                                trailing = {
                                    Text(
                                        text = pauseSummary(state.pause),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    NavChevron()
                                }
                            )
                            GlassRow(
                                label = stringResource(R.string.gates_schedule_label),
                                onClick = onOpenSchedule,
                                leading = { TeperaIconCircle(TeperaSymbols.Schedule) },
                                trailing = {
                                    Text(
                                        text = stringResource(
                                            if (state.schedule == null) R.string.gates_schedule_always
                                            else R.string.gates_schedule_custom
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    NavChevron()
                                }
                            )
                            // CC-6: опційна «зростаюча» затримка; лічильник повторних відкриттів ніде не показується.
                            GlassRow(
                                label = stringResource(R.string.gates_growing_delay_label),
                                leading = { TeperaIconCircle(TeperaSymbols.Timer) },
                                trailing = {
                                    Switch(
                                        checked = growingDelay,
                                        onCheckedChange = { viewModel.setGrowingDelay(it) },
                                        colors = teperaSwitchColors()
                                    )
                                }
                            )
                            Text(
                                text = stringResource(R.string.gates_growing_delay_hint),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    if (!state.pinShortcutSupported) {
                        item {
                            Text(
                                text = stringResource(R.string.gates_unsupported),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                            )
                        }
                        return@LazyColumn
                    }

                    item { GlassSectionHeader(stringResource(R.string.gates_section_active)) }
                    if (state.gates.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.gates_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(state.gates, key = { it.gate.packageName }) { gateState ->
                        GateRow(
                            gateState = gateState,
                            onMarkHandled = { viewModel.markOriginalIconHandled(gateState.gate.packageName) },
                            onRemove = { viewModel.removeGate(gateState.gate.packageName) },
                            onDelayChange = { viewModel.setDelaySeconds(gateState.gate.packageName, it) }
                        )
                    }

                    item { GlassSectionHeader(stringResource(R.string.gates_section_add)) }
                    items(state.availableApps, key = { it.packageName }) { app ->
                        GlassRow(
                            label = app.label,
                            onClick = { pendingApp = app },
                            leading = { AppIcon(app) },
                            trailing = {}
                        )
                    }
                }
            }
        }
    }

    if (showPauseDialog) {
        PauseDialog(
            isPaused = state.pause != null,
            onChoose = { choice ->
                showPauseDialog = false
                when (choice) {
                    PauseChoice.TODAY -> viewModel.pauseToday()
                    PauseChoice.WEEKEND -> viewModel.pauseWeekend()
                    PauseChoice.UNTIL_DATE -> showPauseDatePicker = true
                }
            },
            onEndPause = {
                showPauseDialog = false
                viewModel.endPause()
            },
            onDismiss = { showPauseDialog = false }
        )
    }

    if (showPauseDatePicker) {
        val today = startOfTodayMillis()
        TeperaDatePickerDialog(
            dayMillis = today,
            minDayMillis = today,
            maxDayMillis = null,
            onSelected = { dayMillis ->
                showPauseDatePicker = false
                viewModel.pauseUntil(Instant.ofEpochMilli(dayMillis).atZone(ZoneId.systemDefault()).toLocalDate())
            },
            onDismiss = { showPauseDatePicker = false }
        )
    }

    pendingApp?.let { app ->
        DelayPickerDialog(
            app = app,
            onDismiss = { pendingApp = null },
            onConfirm = { delaySeconds ->
                pendingApp = null
                scope.launch {
                    val created = viewModel.createGate(app, delaySeconds)
                    if (created) instructionApp = app else pinFailed = true
                }
            }
        )
    }

    instructionApp?.let { app ->
        InstructionDialog(
            app = app,
            onDone = {
                viewModel.markOriginalIconHandled(app.packageName)
                instructionApp = null
            },
            onLater = { instructionApp = null }
        )
    }

    if (pinFailed) {
        TeperaDialog(
            onDismissRequest = { pinFailed = false },
            text = stringResource(R.string.gates_pin_failed),
            confirmText = stringResource(R.string.gates_instruction_done),
            onConfirm = { pinFailed = false }
        )
    }
}

@Composable
private fun GateRow(gateState: GateUiState, onMarkHandled: () -> Unit, onRemove: () -> Unit, onDelayChange: (Int) -> Unit) {
    Column {
        GlassRow(
            label = gateState.app.label,
            leading = { AppIcon(gateState.app) },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentDelay = gateState.gate.delaySeconds
                    // Тап по чіпу перемикає тривалість по колу (DELAY_OPTIONS); indexOf == -1 для значення поза
                    // набором дає перший елемент.
                    TeperaButton(
                        text = stringResource(R.string.gates_delay_format, currentDelay),
                        onClick = {
                            onDelayChange(DELAY_OPTIONS[(DELAY_OPTIONS.indexOf(currentDelay) + 1) % DELAY_OPTIONS.size])
                        },
                        modifier = Modifier.width(72.dp),
                        size = TeperaButtonSize.Small,
                        type = TeperaButtonType.Secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    TeperaIconButton(icon = TeperaSymbols.Close, contentDescription = stringResource(R.string.gates_remove_action), onClick = onRemove)
                }
            }
        )
        if (!gateState.gate.originalIconHandled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(TeperaPalette.cardTranslucent)
                    .clickable(onClick = onMarkHandled)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    TeperaSymbols.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TeperaPalette.brandAccent
                )
                Text(
                    text = stringResource(R.string.gates_icon_not_handled),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DelayPickerDialog(app: InstalledAppInfo, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var selected by remember { mutableStateOf(DELAY_OPTIONS[1]) }
    TeperaDialog(
        onDismissRequest = onDismiss,
        title = app.label,
        text = stringResource(R.string.gates_delay_picker_title),
        confirmText = stringResource(R.string.gates_delay_picker_confirm),
        onConfirm = { onConfirm(selected) },
        dismissText = stringResource(R.string.gates_delay_picker_cancel)
    ) {
        PillSegmentedControl(
            options = DELAY_OPTIONS.map { it to stringResource(R.string.gates_delay_format, it) },
            selected = selected,
            onSelect = { selected = it }
        )
    }
}

@Composable
private fun InstructionDialog(app: InstalledAppInfo, onDone: () -> Unit, onLater: () -> Unit) {
    TeperaDialog(
        onDismissRequest = onLater,
        title = stringResource(R.string.gates_instruction_title),
        text = stringResource(R.string.gates_instruction_body, app.label),
        confirmText = stringResource(R.string.gates_instruction_done),
        onConfirm = onDone,
        dismissText = stringResource(R.string.gates_instruction_later)
    )
}

@Composable
private fun AppIcon(app: InstalledAppInfo) {
    val icon = app.resolvedIcon
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(TeperaPalette.brandAccentSoft),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
        } else {
            Icon(TeperaSymbols.Apps, contentDescription = null, tint = TeperaPalette.brandAccent)
        }
    }
}

@Composable
private fun pauseSummary(pause: PauseWindow?): String {
    if (pause == null) return stringResource(R.string.gates_pause_none)
    val now = System.currentTimeMillis()
    val format = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    // Кінець паузи — початок наступного дня, тож людині показуємо останній день паузи.
    val lastDay = format.format(Date(pause.untilMillis - 1))
    return if (pause.fromMillis > now) {
        stringResource(R.string.gates_pause_from_until_format, format.format(Date(pause.fromMillis)), lastDay)
    } else {
        stringResource(R.string.gates_pause_until_format, lastDay)
    }
}

private enum class PauseChoice { TODAY, WEEKEND, UNTIL_DATE }

/** CC-5: вибір паузи — "сьогодні", "на вихідні" або "до дати"; чинну паузу можна скасувати одним тапом. */
@Composable
private fun PauseDialog(
    isPaused: Boolean,
    onChoose: (PauseChoice) -> Unit,
    onEndPause: () -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(PauseChoice.TODAY) }
    TeperaDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.gates_pause_dialog_title),
        confirmText = stringResource(R.string.gates_pause_dialog_confirm),
        onConfirm = { onChoose(selected) },
        dismissText = stringResource(R.string.dialog_cancel)
    ) {
        listOf(
            PauseChoice.TODAY to R.string.gates_pause_choice_today,
            PauseChoice.WEEKEND to R.string.gates_pause_choice_weekend,
            PauseChoice.UNTIL_DATE to R.string.gates_pause_choice_date
        ).forEach { (choice, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(selected = selected == choice, role = Role.RadioButton) { selected = choice },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == choice,
                    onClick = null,
                    modifier = Modifier.padding(12.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = TeperaPalette.buttonBrand,
                        unselectedColor = TeperaPalette.buttonBrandDark
                    )
                )
                Text(text = stringResource(label), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (isPaused) {
            TeperaButton(
                text = stringResource(R.string.gates_pause_end_now),
                onClick = onEndPause,
                type = TeperaButtonType.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

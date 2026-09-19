package com.serkodesign.tepera.ui.gates

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import kotlinx.coroutines.launch

private val DELAY_OPTIONS = listOf(5, 10, 20)

/**
 * T-4 (tepera-dev-spec.md, FR-G частина 1): "Застосунки з затримкою". Дії, недоступні до наступного
 * запуску застосунку (напр. якщо `isRequestPinShortcutSupported() == false`), показуються спокійним
 * поясненням замість краху — буквальна вимога приймання.
 */
@Composable
fun GatesScreen(
    gateRepository: GateRepository,
    installedAppsProvider: InstalledAppsProvider,
    settingsStore: SettingsStore,
    onBack: () -> Unit
) {
    val viewModel: GatesViewModel = viewModel(
        factory = GatesViewModel.Factory(gateRepository, installedAppsProvider, settingsStore)
    )
    val state by viewModel.uiState.collectAsState()
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

    val isPaused = state.gatesPausedUntilMillis > System.currentTimeMillis()

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
                        // Розділ 2.3 документа: один тап, без підтвердження, без пояснювального тексту.
                        GlassRow(
                            label = stringResource(R.string.gates_pause_today_label),
                            leading = {},
                            trailing = {
                                Switch(
                                    checked = isPaused,
                                    onCheckedChange = { viewModel.toggleGatesPausedForToday() },
                                    colors = teperaSwitchColors()
                                )
                            }
                        )
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
                            onRemove = { viewModel.removeGate(gateState.gate.packageName) }
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
        AlertDialog(
            onDismissRequest = { pinFailed = false },
            confirmButton = {
                TeperaButton(text = stringResource(R.string.gates_instruction_done), onClick = { pinFailed = false }, type = TeperaButtonType.Tertiary)
            },
            text = { Text(stringResource(R.string.gates_pin_failed)) }
        )
    }
}

@Composable
private fun GateRow(gateState: GateUiState, onMarkHandled: () -> Unit, onRemove: () -> Unit) {
    Column {
        GlassRow(
            label = gateState.app.label,
            leading = { AppIcon(gateState.app) },
            trailing = {
                Row {
                    Text(
                        text = stringResource(R.string.gates_delay_format, gateState.gate.delaySeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    TeperaIconButton(icon = Icons.Filled.Close, contentDescription = stringResource(R.string.gates_remove_action), onClick = onRemove)
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
                    Icons.Filled.WarningAmber,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.gates_delay_picker_title),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                PillSegmentedControl(
                    options = DELAY_OPTIONS.map { it to stringResource(R.string.gates_delay_format, it) },
                    selected = selected,
                    onSelect = { selected = it }
                )
            }
        },
        confirmButton = {
            TeperaButton(text = stringResource(R.string.gates_delay_picker_confirm), onClick = { onConfirm(selected) }, type = TeperaButtonType.Tertiary)
        },
        dismissButton = {
            TeperaButton(text = stringResource(R.string.gates_delay_picker_cancel), onClick = onDismiss, type = TeperaButtonType.Tertiary)
        }
    )
}

@Composable
private fun InstructionDialog(app: InstalledAppInfo, onDone: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.gates_instruction_title)) },
        text = { Text(stringResource(R.string.gates_instruction_body, app.label)) },
        confirmButton = {
            TeperaButton(text = stringResource(R.string.gates_instruction_done), onClick = onDone, type = TeperaButtonType.Tertiary)
        },
        dismissButton = {
            TeperaButton(text = stringResource(R.string.gates_instruction_later), onClick = onLater, type = TeperaButtonType.Tertiary)
        }
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
            Icon(Icons.Filled.Apps, contentDescription = null, tint = TeperaPalette.brandAccent)
        }
    }
}

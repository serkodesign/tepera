package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.M3GroupLabel
import com.serkodesign.tepera.ui.theme.M3ListDivider
import com.serkodesign.tepera.ui.theme.M3ListGroup
import com.serkodesign.tepera.ui.theme.M3ListItem
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.util.BerserkStore

/**
 * Екран «Застосунки-стрічки», нижня частина налаштувань (лише збірка зі службою доступності): тип підрахунку часу
 * (окремо для кожної стрічки чи разом) і берсерк-режим. Тип підрахунку стосується і берсерка, і «Відкрити на 5 хв».
 */
@Composable
fun BerserkSettings() {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(BerserkStore.countMode(context)) }
    var enabled by remember { mutableStateOf(BerserkStore.isEnabled(context)) }
    var hour by remember { mutableStateOf(BerserkStore.hourMinutes(context)) }
    var block by remember { mutableStateOf(BerserkStore.blockMinutes(context)) }
    var skipped by remember { mutableStateOf(BerserkStore.isSkippedToday(context)) }
    var confirmSkip by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        M3GroupLabel(stringResource(R.string.count_mode_group))
        M3ListGroup {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillSegmentedControl(
                    options = listOf(
                        BerserkStore.CountMode.PER_APP to stringResource(R.string.count_mode_per_app),
                        BerserkStore.CountMode.ALL to stringResource(R.string.count_mode_all)
                    ),
                    selected = mode,
                    onSelect = { mode = it; BerserkStore.setCountMode(context, it) }
                )
                Text(
                    stringResource(R.string.count_mode_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        M3GroupLabel(stringResource(R.string.berserk_settings_group))
        M3ListGroup {
            M3ListItem(
                title = stringResource(R.string.berserk_toggle_title),
                supporting = stringResource(R.string.berserk_toggle_supporting),
                icon = TeperaSymbols.Timer,
                trailing = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it; BerserkStore.setEnabled(context, it) },
                        colors = teperaSwitchColors()
                    )
                }
            )
            if (enabled) {
                M3ListDivider()
                IntervalPicker(
                    label = stringResource(R.string.berserk_hour_label),
                    options = BerserkStore.HOUR_OPTIONS, selected = hour,
                    onSelect = { hour = it; BerserkStore.setHourMinutes(context, it) }
                )
                M3ListDivider()
                IntervalPicker(
                    label = stringResource(R.string.berserk_block_label),
                    options = BerserkStore.BLOCK_OPTIONS, selected = block,
                    onSelect = { block = it; BerserkStore.setBlockMinutes(context, it) }
                )
                M3ListDivider()
                M3ListItem(
                    title = stringResource(R.string.berserk_skip_title),
                    supporting = stringResource(if (skipped) R.string.berserk_skip_done else R.string.berserk_skip_supporting),
                    icon = TeperaSymbols.Close,
                    onClick = if (skipped) null else ({ confirmSkip = true })
                )
            }
        }
    }

    if (confirmSkip) {
        TeperaDialog(
            onDismissRequest = { confirmSkip = false },
            title = stringResource(R.string.berserk_skip_confirm_title),
            text = stringResource(R.string.berserk_skip_confirm_body),
            confirmText = stringResource(R.string.berserk_skip_confirm_action),
            dismissText = stringResource(R.string.dialog_cancel),
            onConfirm = {
                confirmSkip = false
                BerserkStore.skipToday(context)
                skipped = true
            }
        )
    }
}

@Composable
private fun IntervalPicker(label: String, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        PillSegmentedControl(options = options.map { it to it.toString() }, selected = selected, onSelect = onSelect)
    }
}

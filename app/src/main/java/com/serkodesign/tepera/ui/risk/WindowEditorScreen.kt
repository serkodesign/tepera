package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.NudgeScheduler
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.RiskWindowFinder
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaTimePickerDialog
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * v3.0 MVP (beta): ручне вікно — людина сама задає початок, кінець і чи діє воно щодня чи лише в будні. Нове
 * ([windowId] = `null`) або редагування наявного ручного. Кінець раніше за початок означає перехід через північ;
 * збігатись вони не можуть, а коротше за слот (15 хв) вікно не рахується.
 */
@Composable
fun WindowEditorScreen(windowId: String?, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(windowId == null) }
    var start by remember { mutableIntStateOf(22 * 60) }
    var end by remember { mutableIntStateOf(23 * 60 + 30) }
    var dayType by remember { mutableStateOf(DayType.ALL) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }

    LaunchedEffect(windowId) {
        if (windowId != null) {
            app.riskWindowRepository.getWindow(windowId)?.takeIf { it.isManual }?.let {
                start = it.startMinute
                end = it.endMinute
                dayType = it.dayType
            }
            loaded = true
        }
    }

    val length = RiskWindowFinder.lengthMinutes(start, end)
    val valid = start != end && length >= RiskWindowFinder.SLOT_MINUTES

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(
                title = stringResource(if (windowId == null) R.string.v3_editor_title_new else R.string.v3_editor_title_edit),
                onBack = onBack
            )
            if (loaded) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.v3_editor_intro), color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeCard(stringResource(R.string.v3_editor_start), start, Modifier.weight(1f)) { pickStart = true }
                        TimeCard(stringResource(R.string.v3_editor_end), end, Modifier.weight(1f)) { pickEnd = true }
                    }
                    V3Card(padding = PaddingValues(16.dp), spacing = 12.dp) {
                        Text(stringResource(R.string.v3_editor_days), color = TeperaPalette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        PillSegmentedControl(
                            options = listOf(
                                DayType.ALL to stringResource(R.string.v3_editor_days_all),
                                DayType.WEEKDAYS to stringResource(R.string.v3_editor_days_weekdays)
                            ),
                            selected = dayType,
                            onSelect = { dayType = it }
                        )
                    }
                    Text(
                        text = if (valid) {
                            stringResource(R.string.v3_editor_summary_format, formatWindowTime(start, end), durationMinutesText(length))
                        } else {
                            stringResource(R.string.v3_editor_invalid)
                        },
                        color = if (valid) TeperaPalette.textSecondary else TeperaPalette.textError, fontSize = 12.sp, lineHeight = 16.sp
                    )
                }
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TeperaButton(
                        text = stringResource(R.string.v3_editor_save),
                        enabled = valid,
                        onClick = {
                            scope.launch {
                                if (windowId == null) app.riskWindowRepository.createManual(start, end, dayType)
                                else app.riskWindowRepository.updateManual(windowId, start, end, dayType)
                                NudgeScheduler.reschedule(app)
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (windowId != null) {
                        TeperaButton(
                            text = stringResource(R.string.v3_editor_delete),
                            onClick = { scope.launch { app.riskWindowRepository.deleteManual(windowId); NudgeScheduler.reschedule(app); onBack() } },
                            type = TeperaButtonType.Secondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TeperaButton(
                        text = stringResource(R.string.v3_plan_later),
                        onClick = onBack,
                        type = TeperaButtonType.Tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (pickStart) {
        TeperaTimePickerDialog(
            title = stringResource(R.string.v3_editor_start), minuteOfDay = start,
            onSelected = { start = it; pickStart = false }, onDismiss = { pickStart = false }
        )
    }
    if (pickEnd) {
        TeperaTimePickerDialog(
            title = stringResource(R.string.v3_editor_end), minuteOfDay = end,
            onSelected = { end = it; pickEnd = false }, onDismiss = { pickEnd = false }
        )
    }
}

@Composable
private fun TimeCard(label: String, minute: Int, modifier: Modifier, onClick: () -> Unit) {
    V3Card(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), radius = 20.dp, spacing = 4.dp
    ) {
        Text(label, color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        Text(
            String.format(Locale.ROOT, "%02d:%02d", minute / 60 % 24, minute % 60),
            color = TeperaPalette.textPrimary, fontSize = 27.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun durationMinutesText(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.v3_dur_hours_minutes, hours, minutes)
        hours > 0 -> stringResource(R.string.v3_dur_hours, hours)
        else -> stringResource(R.string.v3_dur_minutes, minutes)
    }
}

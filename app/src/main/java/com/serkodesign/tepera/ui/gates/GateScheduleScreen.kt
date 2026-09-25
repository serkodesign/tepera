package com.serkodesign.tepera.ui.gates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaTimePickerDialog
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.util.GateSchedule
import com.serkodesign.tepera.util.TimeInterval
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * CC-5: розклад воріт — спільний для всіх воріт. За замовчуванням "Завжди" (поведінка тих, хто
 * розклад не налаштовує, не змінюється). Інакше — по днях тижня до [GateSchedule.MAX_INTERVALS_PER_DAY]
 * проміжків на день; проміжок може переходити через північ; однакові початок і кінець — весь день;
 * день без проміжків — ворота цього дня не діють. Зміни — чернетка, що зберігається однією кнопкою
 * (одна подія `schedule_changed` на збереження, а не на кожен вибір часу).
 */
@Composable
fun GateScheduleScreen(gateRepository: GateRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var savedSchedule by remember { mutableStateOf<GateSchedule?>(null) }
    var always by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf(GateSchedule.wholeWeek().days) }

    LaunchedEffect(Unit) {
        val saved = gateRepository.schedule.first()
        savedSchedule = saved
        always = saved == null
        draft = saved?.days ?: GateSchedule.wholeWeek().days
        loaded = true
    }

    val current: GateSchedule? = if (always) null else GateSchedule(draft)
    val dirty = loaded && current?.encode() != savedSchedule?.encode()

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.gate_schedule_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassRow(
                    label = stringResource(R.string.gate_schedule_always),
                    leading = {},
                    trailing = {
                        Switch(
                            checked = always,
                            onCheckedChange = { always = it },
                            colors = teperaSwitchColors()
                        )
                    }
                )
                Text(
                    text = stringResource(
                        if (always) R.string.gate_schedule_always_hint else R.string.gate_schedule_custom_hint
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                if (!always) {
                    DayOfWeek.entries.forEach { day ->
                        DayCard(
                            day = day,
                            intervals = draft[day].orEmpty(),
                            onChange = { updated -> draft = draft + (day to updated) },
                            onCopyToAll = { draft = DayOfWeek.entries.associateWith { draft[day].orEmpty() } }
                        )
                    }
                }
            }
            TeperaButton(
                text = stringResource(R.string.dialog_save),
                onClick = {
                    scope.launch {
                        gateRepository.setSchedule(current)
                        onBack()
                    }
                },
                enabled = dirty,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun DayCard(
    day: DayOfWeek,
    intervals: List<TimeInterval>,
    onChange: (List<TimeInterval>) -> Unit,
    onCopyToAll: () -> Unit
) {
    val dayName = remember(day) {
        day.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.uppercase() }
    }
    TeperaCard(title = dayName) {
        if (intervals.isEmpty()) {
            Text(
                text = stringResource(R.string.gate_schedule_day_off),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        intervals.forEachIndexed { index, interval ->
            IntervalRow(
                interval = interval,
                onChange = { changed -> onChange(intervals.toMutableList().also { it[index] = changed }) },
                onRemove = { onChange(intervals.toMutableList().also { it.removeAt(index) }) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (intervals.size < GateSchedule.MAX_INTERVALS_PER_DAY) {
                TeperaButton(
                    text = stringResource(R.string.gate_schedule_add_interval),
                    onClick = {
                        val next = if (intervals.isEmpty()) TimeInterval(9 * 60, 18 * 60) else TimeInterval(20 * 60, 22 * 60)
                        onChange(intervals + next)
                    },
                    type = TeperaButtonType.Secondary
                )
            }
            if (intervals.isNotEmpty()) {
                TeperaButton(
                    text = stringResource(R.string.gate_schedule_copy_all),
                    onClick = onCopyToAll,
                    type = TeperaButtonType.Tertiary
                )
            }
        }
    }
}

@Composable
private fun IntervalRow(interval: TimeInterval, onChange: (TimeInterval) -> Unit, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeChip(
                label = stringResource(R.string.gate_schedule_from),
                minute = interval.startMinute,
                onSelected = { onChange(interval.copy(startMinute = it)) },
                modifier = Modifier.weight(1f)
            )
            TimeChip(
                label = stringResource(R.string.gate_schedule_to),
                minute = interval.endMinute,
                onSelected = { onChange(interval.copy(endMinute = it)) },
                modifier = Modifier.weight(1f)
            )
            TeperaIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.gate_schedule_remove_interval),
                onClick = onRemove
            )
        }
        val note = when {
            interval.isWholeDay -> R.string.gate_schedule_note_whole_day
            interval.crossesMidnight -> R.string.gate_schedule_note_next_day
            else -> null
        }
        if (note != null) {
            Text(text = stringResource(note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TimeChip(label: String, minute: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    val text = remember(minute) { "%02d:%02d".format(minute / 60, minute % 60) }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TeperaPalette.surfaceBrandLight)
            .clickable(role = Role.Button) { showPicker = true }
            .semantics { contentDescription = "$label $text" }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TeperaPalette.buttonBrandDark)
            Text(
                text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = TeperaPalette.timeChipText
            )
        }
    }
    if (showPicker) {
        TeperaTimePickerDialog(
            title = label,
            minuteOfDay = minute,
            onSelected = { onSelected(it); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

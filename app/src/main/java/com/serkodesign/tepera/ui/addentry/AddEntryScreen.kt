package com.serkodesign.tepera.ui.addentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.util.utcMidnightToLocalStartOfDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    initialCategoryId: String? = null
) {
    val viewModel: AddEntryViewModel = viewModel(
        factory = AddEntryViewModel.Factory(categoryRepository, activityRepository, initialCategoryId)
    )
    val categories by viewModel.categories.collectAsState()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_entry_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.add_entry_category_label),
                    style = MaterialTheme.typography.labelLarge
                )
                if (categories.isEmpty()) {
                    Text(stringResource(R.string.add_entry_category_empty))
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = state.selectedCategoryId == category.id,
                                onClick = { viewModel.selectCategory(category.id) },
                                label = { Text(categoryDisplayName(category)) },
                                leadingIcon = { Icon(categoryIcon(category.iconName), contentDescription = null) }
                            )
                        }
                    }
                }
                if (state.categoryRequiredError) {
                    Text(
                        stringResource(R.string.add_entry_select_category_first),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            DateField(dateMillis = state.dateMillis, onDateSelected = viewModel::setDate)

            SingleChoiceSegmentedButtonRow {
                DurationMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.mode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, DurationMode.entries.size)
                    ) {
                        Text(modeLabel(mode))
                    }
                }
            }

            when (state.mode) {
                DurationMode.PRESETS -> PresetDurationSection(
                    minutes = state.presetMinutes,
                    onAdd = viewModel::addPresetMinutes,
                    onReset = viewModel::resetPresetMinutes
                )
                DurationMode.MANUAL -> ManualDurationSection(
                    text = state.manualMinutesText,
                    onChange = viewModel::setManualMinutes
                )
                DurationMode.INTERVAL -> IntervalDurationSection(
                    startMinuteOfDay = state.startMinuteOfDay,
                    endMinuteOfDay = state.endMinuteOfDay,
                    onStartChange = viewModel::setStartMinuteOfDay,
                    onEndChange = viewModel::setEndMinuteOfDay,
                    isError = state.intervalInvalidError
                )
            }

            if (state.mode != DurationMode.INTERVAL) {
                TimeField(
                    label = stringResource(R.string.add_entry_start_time_label),
                    minuteOfDay = state.startMinuteOfDay,
                    onMinuteSelected = viewModel::setStartMinuteOfDay
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.add_entry_note_label)) },
                supportingText = {
                    Text(stringResource(R.string.add_entry_note_counter, state.note.length, 250))
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_entry_save))
            }
        }
    }

    if (state.overlapEntries != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissOverlapDialog,
            title = { Text(stringResource(R.string.add_entry_overlap_title)) },
            text = { Text(stringResource(R.string.add_entry_overlap_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.save(forceOverwrite = true) }) {
                    Text(stringResource(R.string.add_entry_overlap_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverlapDialog) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun modeLabel(mode: DurationMode): String = when (mode) {
    DurationMode.PRESETS -> stringResource(R.string.add_entry_mode_presets)
    DurationMode.MANUAL -> stringResource(R.string.add_entry_mode_manual)
    DurationMode.INTERVAL -> stringResource(R.string.add_entry_mode_interval)
}

@Composable
private fun PresetDurationSection(minutes: Int, onAdd: (Int) -> Unit, onReset: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.add_entry_duration_label, minutes),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            // FR-1.2: +15/+30/+60/+120 хв, стекуються при повторному тапі.
            listOf(15, 30, 60, 120).forEach { preset ->
                OutlinedButton(onClick = { onAdd(preset) }) {
                    Text(stringResource(R.string.add_entry_preset_format, preset))
                }
            }
        }
        TextButton(onClick = onReset) { Text(stringResource(R.string.add_entry_reset_duration)) }
    }
}

@Composable
private fun ManualDurationSection(text: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.add_entry_duration_minutes_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun IntervalDurationSection(
    startMinuteOfDay: Int,
    endMinuteOfDay: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    isError: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeField(stringResource(R.string.add_entry_start_time_label), startMinuteOfDay, onStartChange)
        TimeField(stringResource(R.string.add_entry_end_time_label), endMinuteOfDay, onEndChange)
        if (isError) {
            Text(stringResource(R.string.add_entry_interval_invalid), color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(dateMillis: Long, onDateSelected: (Long) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatted = remember(dateMillis) {
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(dateMillis))
    }

    OutlinedButton(onClick = { showPicker = true }) { Text(formatted) }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(utcMidnightToLocalStartOfDay(it)) }
                    showPicker = false
                }) { Text(stringResource(R.string.dialog_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, minuteOfDay: Int, onMinuteSelected: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatted = remember(minuteOfDay) {
        "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
    }

    OutlinedButton(onClick = { showPicker = true }) {
        Text(stringResource(R.string.time_field_format, label, formatted))
    }

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = minuteOfDay / 60,
            initialMinute = minuteOfDay % 60,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { showPicker = false },
            onConfirm = {
                onMinuteSelected(pickerState.hour * 60 + pickerState.minute)
                showPicker = false
            }
        ) {
            TimePicker(state = pickerState)
        }
    }
}

// Material3 не постачає готовий TimePickerDialog (на відміну від DatePickerDialog) — це
// мінімальна обгортка навколо TimePicker у Dialog, стандартний паттерн для М3.
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
                    TextButton(onClick = onConfirm) { Text(stringResource(R.string.dialog_save)) }
                }
            }
        }
    }
}

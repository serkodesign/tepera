package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.util.localStartOfDayToUtcMidnight
import com.serkodesign.tepera.util.utcMidnightToLocalStartOfDay

/**
 * Спільні M3-діалоги вибору часу й дати у стилі застосунку (форма додавання активності, розклад воріт,
 * пауза воріт). Час — 24-годинний циферблат із перемикачем на клавіатурний ввід.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeperaTimePickerDialog(
    title: String,
    minuteOfDay: Int,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = minuteOfDay / 60,
        initialMinute = minuteOfDay % 60,
        is24Hour = true
    )
    var keyboardMode by remember { mutableStateOf(false) }
    val pickerColors = TimePickerDefaults.colors(
        clockDialColor = Color.White,
        clockDialSelectedContentColor = Color.White,
        clockDialUnselectedContentColor = TeperaPalette.buttonBrandDark,
        selectorColor = TeperaPalette.buttonBrand,
        containerColor = Color.Transparent,
        periodSelectorBorderColor = TeperaPalette.buttonBrandDark,
        periodSelectorSelectedContainerColor = TeperaPalette.buttonBrand,
        periodSelectorUnselectedContainerColor = Color.Transparent,
        periodSelectorSelectedContentColor = Color.White,
        periodSelectorUnselectedContentColor = TeperaPalette.buttonBrandDark,
        timeSelectorSelectedContainerColor = TeperaPalette.buttonBrand,
        timeSelectorUnselectedContainerColor = Color.White,
        timeSelectorSelectedContentColor = Color.White,
        timeSelectorUnselectedContentColor = TeperaPalette.buttonBrandDark
    )
    TeperaDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = stringResource(R.string.dialog_save),
        onConfirm = { onSelected(pickerState.hour * 60 + pickerState.minute) },
        dismissText = stringResource(R.string.dialog_cancel)
    ) {
        if (keyboardMode) {
            TimeInput(
                state = pickerState,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = pickerColors
            )
        } else {
            TimePicker(
                state = pickerState,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = pickerColors
            )
        }
        TeperaIconButton(
            icon = if (keyboardMode) TeperaSymbols.Schedule else TeperaSymbols.Keyboard,
            contentDescription = stringResource(
                if (keyboardMode) R.string.add_entry_time_mode_dial else R.string.add_entry_time_mode_keyboard
            ),
            onClick = { keyboardMode = !keyboardMode },
            modifier = Modifier.width(44.dp).align(Alignment.Start)
        )
    }
}

/** M3-календар у стилі застосунку; [minDayMillis]/[maxDayMillis] (локальна північ) обмежують вибір. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeperaDatePickerDialog(
    dayMillis: Long,
    minDayMillis: Long?,
    maxDayMillis: Long?,
    onSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val minUtc = minDayMillis?.let(::localStartOfDayToUtcMidnight)
    val maxUtc = maxDayMillis?.let(::localStartOfDayToUtcMidnight)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = localStartOfDayToUtcMidnight(dayMillis),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                (minUtc == null || utcTimeMillis >= minUtc) && (maxUtc == null || utcTimeMillis <= maxUtc)
        }
    )
    val pickerColors = DatePickerDefaults.colors(
        containerColor = TeperaPalette.surfaceBrandLight,
        titleContentColor = TeperaPalette.buttonBrandDark,
        headlineContentColor = TeperaPalette.buttonBrandDark,
        weekdayContentColor = TeperaPalette.buttonBrand,
        subheadContentColor = TeperaPalette.buttonBrandDark,
        navigationContentColor = TeperaPalette.buttonBrandDark,
        yearContentColor = TeperaPalette.buttonBrandDark,
        currentYearContentColor = TeperaPalette.buttonBrand,
        selectedYearContentColor = Color.White,
        selectedYearContainerColor = TeperaPalette.buttonBrand,
        dayContentColor = TeperaPalette.buttonBrandDark,
        selectedDayContentColor = Color.White,
        selectedDayContainerColor = TeperaPalette.buttonBrand,
        disabledDayContentColor = TeperaPalette.buttonBrandDark.copy(alpha = 0.3f),
        todayContentColor = TeperaPalette.buttonBrand,
        todayDateBorderColor = TeperaPalette.buttonBrand,
        dividerColor = TeperaPalette.buttonBrand.copy(alpha = 0.2f)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        colors = pickerColors,
        confirmButton = {
            TeperaButton(text = stringResource(R.string.dialog_save), onClick = {
                pickerState.selectedDateMillis?.let { onSelected(utcMidnightToLocalStartOfDay(it)) } ?: onDismiss()
            })
        },
        dismissButton = {
            TeperaButton(text = stringResource(R.string.dialog_cancel), onClick = onDismiss, type = TeperaButtonType.Secondary)
        }
    ) {
        DatePicker(state = pickerState, colors = pickerColors)
    }
}

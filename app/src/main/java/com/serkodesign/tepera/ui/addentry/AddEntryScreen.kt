package com.serkodesign.tepera.ui.addentry

import androidx.compose.foundation.layout.BoxWithConstraints

import com.serkodesign.tepera.ui.theme.TeperaDialog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSpecs
import com.serkodesign.tepera.util.localStartOfDayToUtcMidnight
import com.serkodesign.tepera.util.utcMidnightToLocalStartOfDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Перемальовано за Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR, node 61:3516, "New activity") —
 * 3 варіанти макета (Presets/Manual/Interval) звірені через get_design_context, спільний каркас:
 * заголовок → дата → сітка категорій (іконка в тонованому кружку + назва, 3 колонки) → перемикач
 * Presets/Manual/Interval → поля часу конкретного режиму → нотатка → Cancel/Save.
 *
 * **Свідомі відхилення від макета (узгоджено з користувачем перед реалізацією):**
 * - Сітка категорій — ДИНАМІЧНА з реальних активних категорій (`categoryRepository.
 *   observeActiveCategories()`), не жорстко зашитий набір 8 іконок з макета (у Tepera немає
 *   окремої категорії "Walk" — вона злита в "Рух/спорт", "Sleep" архівована, є до 2 кастомних).
 *   Іконки — ті самі контурні `ic_widget_*` (`categoryLineArtIconRes()`), що вже імпортовані для
 *   віджета з ТОГО САМОГО набору Figma-асетів; кастомні категорії не мають такого стилю й
 *   падають на звичайні `categoryIcon()` Material-глyфи. Колір кружка — реальний колір категорії
 *   (`categoryColor()`), не фіксована палітра макета.
 * - Вибір дати (для запису заднім числом) — макет його не показує; лишили функціонал, лише
 *   перемалювали під новий "скляний" рядок (іконка календаря + дата + шеврон).
 * - Manual-поле — те саме поле цілих ХВИЛИН, що й раніше (не формат Год:Хв, як натякає
 *   плейсхолдер "00:00" у макеті) — лише новий візуальний стиль.
 * - Редагування запису (Щоденник/Stats) отримує той самий новий стиль, і сітка категорій
 *   лишається видимою й змінюваною (як і раніше) — на відміну від "додати час" з картки
 *   категорії на Home/віджеті, де сітку СХОВАНО за прямим запитом користувача.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    initialCategoryId: String? = null,
    editingEntryId: String? = null
) {
    val viewModel: AddEntryViewModel = viewModel(
        factory = AddEntryViewModel.Factory(categoryRepository, activityRepository, initialCategoryId, editingEntryId)
    )
    val categories by viewModel.categories.collectAsState()
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // "Додати час" з картки категорії (Home/віджет) приходить із вже відомою категорією — сітку
    // вибору ховаємо за прямим запитом користувача. Редагування показує сітку завжди (можна
    // змінити категорію заднім числом), незалежно від того, чи прийшло воно з попередньо обраною
    // категорією.
    val showCategoryGrid = viewModel.isEditing || initialCategoryId == null

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(
                title = stringResource(
                    if (viewModel.isEditing) R.string.edit_entry_screen_title
                    else R.string.add_entry_screen_title
                ),
                onBack = onBack,
                trailing = {
                    if (viewModel.isEditing) {
                        TeperaIconButton(icon = Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.edit_entry_delete_action), onClick = { showDeleteConfirm = true })
                    }
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                DateRow(dateMillis = state.dateMillis, onDateSelected = viewModel::setDate)

                if (showCategoryGrid) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            stringResource(R.string.add_entry_category_label),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        )
                        if (categories.isEmpty()) {
                            Text(stringResource(R.string.add_entry_category_empty))
                        } else {
                            CategoryGrid(
                                categories = categories,
                                selectedId = state.selectedCategoryId,
                                onSelect = viewModel::selectCategory
                            )
                        }
                        if (state.categoryRequiredError) {
                            Text(
                                stringResource(R.string.add_entry_select_category_first),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.add_entry_duration_type_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    )
                    PillSegmentedControl(
                        options = DurationMode.entries.map { it to modeLabel(it) },
                        selected = state.mode,
                        onSelect = viewModel::selectMode
                    )

                    when (state.mode) {
                        DurationMode.PRESETS -> {
                            GlassTimeChip(
                                label = stringResource(R.string.add_entry_start_time_label),
                                minuteOfDay = state.startMinuteOfDay,
                                onMinuteSelected = viewModel::setStartMinuteOfDay
                            )
                            PresetGrid(minutes = state.presetMinutes, onAdd = viewModel::addPresetMinutes)
                            DurationSummaryRow(totalMinutes = state.presetMinutes, onReset = viewModel::resetPresetMinutes)
                        }
                        DurationMode.MANUAL -> {
                            GlassTimeChip(
                                label = stringResource(R.string.add_entry_start_time_label),
                                minuteOfDay = state.startMinuteOfDay,
                                onMinuteSelected = viewModel::setStartMinuteOfDay
                            )
                            GlassTextField(
                                value = state.manualMinutesText,
                                onValueChange = viewModel::setManualMinutes,
                                placeholder = stringResource(R.string.add_entry_duration_minutes_label),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                        DurationMode.INTERVAL -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassTimeChip(
                                    label = stringResource(R.string.add_entry_start_time_label),
                                    minuteOfDay = state.startMinuteOfDay,
                                    onMinuteSelected = viewModel::setStartMinuteOfDay,
                                    modifier = Modifier.weight(1f)
                                )
                                GlassTimeChip(
                                    label = stringResource(R.string.add_entry_end_time_label),
                                    minuteOfDay = state.endMinuteOfDay,
                                    onMinuteSelected = viewModel::setEndMinuteOfDay,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (state.intervalInvalidError) {
                                Text(
                                    stringResource(R.string.add_entry_interval_invalid),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                GlassTextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    placeholder = stringResource(R.string.add_entry_note_label),
                    minLines = 3
                )

                // Запас під sticky-панель Cancel/Save нижче (поза скролом) — без цього останнє
                // поле форми впиралось би прямо в неї без жодного проміжку.
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Cancel/Save — sticky, ЗА МЕЖАМИ скролу (за прямим запитом користувача): Column вище
            // займає weight(1f) і зупиняється рівно там, де починається цей рядок, тож кнопки
            // завжди на тому самому місці внизу екрана, незалежно від прокрутки. 24.dp — сам
            // запит, ПОВЕРХ системного інсету навігаційної панелі (жестова смуга чи 3 кнопки),
            // інакше на пристроях з товстим системним навбаром кнопки опинились би під ним
            // (той самий принцип, що вже застосований у TeperaBottomNavBar).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TeperaPalette.cardActive)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.dialog_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TeperaPalette.brandAccent)
                        .clickable { viewModel.save() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.add_entry_save),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        TeperaDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = stringResource(R.string.edit_entry_delete_confirm_title),
            text = stringResource(R.string.edit_entry_delete_confirm_body),
            confirmText = stringResource(R.string.edit_entry_delete_action),
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteEntry()
            },
            dismissText = stringResource(R.string.dialog_cancel)
        )
    }

    if (state.overlapEntries != null) {
        TeperaDialog(
            onDismissRequest = viewModel::dismissOverlapDialog,
            title = stringResource(R.string.add_entry_overlap_title),
            text = stringResource(R.string.add_entry_overlap_body),
            confirmText = stringResource(R.string.add_entry_overlap_confirm),
            onConfirm = { viewModel.save(forceOverwrite = true) },
            dismissText = stringResource(R.string.dialog_cancel)
        )
    }
}

@Composable
private fun modeLabel(mode: DurationMode): String = when (mode) {
    DurationMode.PRESETS -> stringResource(R.string.add_entry_mode_presets)
    DurationMode.MANUAL -> stringResource(R.string.add_entry_mode_manual)
    DurationMode.INTERVAL -> stringResource(R.string.add_entry_mode_interval)
}

/** 3 колонки, рядки добудовуються вручну (chunked) — категорій завжди небагато (до 6 дефолтних
 * + 2 кастомні), lazy-грід тут надлишковий, той самий принцип, що сітка категорій на Home. */
@Composable
private fun CategoryGrid(categories: List<CategoryEntity>, selectedId: String?, onSelect: (String) -> Unit) {
    // На вузьких екранах (~360dp, напр. Huawei P9) у плитці ~105dp не вміщується "Живе спілкування" навіть у два рядки —
    // там 2 колонки, на звичайних (~410dp) лишаються 3.
    BoxWithConstraints {
        val columns = if (maxWidth < 390.dp) 2 else 3
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            categories.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowItems.forEach { category ->
                        CategoryTile(
                            category = category,
                            selected = category.id == selectedId,
                            onClick = { onSelect(category.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: CategoryEntity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = categoryColor(category.colorHex)
    val lineArtRes = categoryLineArtIconRes(category.iconName)
    val tileColor by animateColorAsState(
        if (selected) accentColor.copy(alpha = 0.35f) else TeperaPalette.cardTranslucentLight,
        TeperaSpecs.effects(), label = "categoryTile"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tileColor)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (lineArtRes != null) {
                Icon(
                    painter = painterResource(lineArtRes),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = categoryIcon(category.iconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            categoryDisplayName(category),
            // Довгі назви ("Хобі/творчість", "Живе спілкування") переносяться на другий рядок, а не обрізаються "…".
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Один рядок, 4 кнопки Fill-шириною (за прямим запитом користувача, відхилення від 2×2-сітки
 * макета) — кожна ділить ширину порівну, висота фіксована 48.dp. */
@Composable
private fun PresetGrid(minutes: Int, onAdd: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(15, 30, 60, 120).forEach { preset ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TeperaPalette.cardTranslucentLight)
                    .clickable { onAdd(preset) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    presetLabel(preset),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun presetLabel(minutes: Int): String =
    if (minutes % 60 == 0) stringResource(R.string.add_entry_preset_hours_format, minutes / 60)
    else stringResource(R.string.add_entry_preset_format, minutes)

@Composable
private fun DurationSummaryRow(totalMinutes: Int, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.add_entry_duration_word), style = MaterialTheme.typography.bodyLarge)
            Text(durationText(totalMinutes), style = MaterialTheme.typography.bodyLarge, color = TeperaPalette.timeChipText)
        }
        Text(
            stringResource(R.string.add_entry_reset_duration),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = TeperaPalette.brandAccent,
            modifier = Modifier.clickable(onClick = onReset)
        )
    }
}

@Composable
private fun durationText(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, mins)
        mins == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, mins)
    }
}

/** Плейсхолдер-only "скляне" поле — спільне для Manual-хвилин і Нотатки (той самий фон/
 * заокруглення/паддінг, що й решта карток на цьому екрані). */
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF888888)
                    )
                }
                innerTextField()
            }
        )
    }
}

/** Мітка + окрема бордюрована "чіп"-капсула зі значенням часу (Figma node 61:3516) — тап
 * відкриває той самий TimePickerDialog, що й раніше, лише сам тригер тепер не суцільна кнопка. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassTimeChip(
    label: String,
    minuteOfDay: Int,
    onMinuteSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatted = remember(minuteOfDay) {
        "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(TeperaPalette.timeChipBackground)
                .border(1.dp, TeperaPalette.timeChipBorder, RoundedCornerShape(4.dp))
                .clickable { showPicker = true }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(formatted, color = TeperaPalette.timeChipText, style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = minuteOfDay / 60,
            initialMinute = minuteOfDay % 60,
            is24Hour = true
        )
        TeperaDialog(
            onDismissRequest = { showPicker = false },
            confirmText = stringResource(R.string.dialog_save),
            onConfirm = {
                onMinuteSelected(pickerState.hour * 60 + pickerState.minute)
                showPicker = false
            },
            dismissText = stringResource(R.string.dialog_cancel)
        ) {
            TimePicker(
                state = pickerState,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = TimePickerDefaults.colors(
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
            )
        }
    }
}

/** "Скляний" рядок вибору дня (макет його не показує — функціонал лишається, за прямим запитом
 * користувача, лише новий стиль). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(dateMillis: Long, onDateSelected: (Long) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatted = remember(dateMillis) {
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(dateMillis))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .clickable { showPicker = true }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
        Text(formatted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localStartOfDayToUtcMidnight(dateMillis))
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
            todayContentColor = TeperaPalette.buttonBrand,
            todayDateBorderColor = TeperaPalette.buttonBrand,
            dividerColor = TeperaPalette.buttonBrand.copy(alpha = 0.2f)
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            shape = RoundedCornerShape(28.dp),
            colors = pickerColors,
            confirmButton = {
                TeperaButton(text = stringResource(R.string.dialog_save), onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(utcMidnightToLocalStartOfDay(it)) }
                    showPicker = false
                })
            },
            dismissButton = {
                TeperaButton(text = stringResource(R.string.dialog_cancel), onClick = { showPicker = false }, type = TeperaButtonType.Secondary)
            }
        ) {
            DatePicker(state = pickerState, colors = pickerColors)
        }
    }
}

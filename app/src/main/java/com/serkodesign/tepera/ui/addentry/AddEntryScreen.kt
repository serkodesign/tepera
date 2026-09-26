package com.serkodesign.tepera.ui.addentry

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.semantics.error
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.heading

import com.serkodesign.tepera.ui.theme.TeperaSymbols
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimeInput
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaDatePickerDialog
import com.serkodesign.tepera.ui.theme.TeperaTimePickerDialog
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.category.categoryGlyphColor
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
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
 * - Дата початку й кінця — окремі тапабельні рядки в полях інтервалу (макет дати не показує; функціонал лишили).
 * - **Час вводиться лише інтервалом (початок → кінець)** — за прямим запитом користувача (21.09.2026)
 *   пресети й ручні хвилини прибрані. Два великі тапабельні поля, M3-діалог часу (циферблат або
 *   клавіатура), тривалість і помилка інтервалу оновлюються наживо, початок тягне кінець за собою.
 * - **Активність може тривати кілька діб (до 7):** кінець має власну дату; при збереженні запис ділиться на
 *   частини по логічних добах (`ActivityRepository.saveInterval`), а редагування збирає їх назад.
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
                        TeperaIconButton(icon = TeperaSymbols.Delete, contentDescription = stringResource(R.string.edit_entry_delete_action), onClick = { showDeleteConfirm = true })
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
                        stringResource(R.string.add_entry_time_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IntervalField(
                            label = stringResource(R.string.add_entry_start_time_label),
                            dayMillis = state.startDayMillis,
                            minuteOfDay = state.startMinuteOfDay,
                            onDateSelected = viewModel::setStartDate,
                            onMinuteSelected = viewModel::setStartMinuteOfDay,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        IntervalField(
                            label = stringResource(R.string.add_entry_end_time_label),
                            dayMillis = state.endDayMillis,
                            minuteOfDay = state.endMinuteOfDay,
                            onDateSelected = viewModel::setEndDate,
                            onMinuteSelected = viewModel::setEndMinuteOfDay,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            minDayMillis = state.startDayMillis,
                            maxDayMillis = state.maxEndDayMillis,
                            invalid = !state.intervalValid
                        )
                    }
                    IntervalSummary(state = state, onEndNextDay = viewModel::endNextDay)
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
                TeperaButton(
                    text = stringResource(R.string.dialog_cancel),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    type = TeperaButtonType.Secondary
                )
                TeperaButton(
                    text = stringResource(R.string.add_entry_save),
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f),
                    type = TeperaButtonType.Primary,
                    enabled = state.intervalValid
                )
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
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
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
                    tint = categoryGlyphColor(accentColor, badgeAlpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = categoryIcon(category.iconName),
                    contentDescription = null,
                    tint = categoryGlyphColor(accentColor, badgeAlpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            categoryDisplayName(category),
            // Довгі назви ("Хобі/творчість", "Живе спілкування") переносяться на другий рядок, а не обрізаються "…".
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Підсумок під полями інтервалу: тривалість, що перераховується наживо, або — замість неї — спокійне
 * пояснення, чому інтервал некоректний (`liveRegion` — скрінрідер озвучує зміну сам). Коли кінець
 * раніше за початок у межах ОДНІЄЇ дати, пропонує найчастіше виправлення одним тапом:
 * "закінчилась наступного дня" (23:00 → 01:00).
 */
@Composable
private fun IntervalSummary(state: AddEntryUiState, onEndNextDay: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (state.intervalError) {
                IntervalError.NONE -> {
                    Text(stringResource(R.string.add_entry_duration_word), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        durationText(state.durationMinutes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TeperaPalette.buttonBrand
                    )
                }
                IntervalError.END_BEFORE_START -> Text(
                    stringResource(R.string.add_entry_interval_invalid),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                IntervalError.TOO_LONG -> Text(
                    stringResource(R.string.add_entry_interval_too_long, MAX_INTERVAL_DAYS),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (state.intervalError == IntervalError.END_BEFORE_START && state.endDayMillis == state.startDayMillis) {
            TeperaButton(
                text = stringResource(R.string.add_entry_end_next_day_action),
                onClick = onEndNextDay,
                type = TeperaButtonType.Secondary
            )
        }
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

/**
 * Поле моменту інтервалу: підпис, дата (тап — календар) і велике значення часу (тап — M3-діалог
 * часу). Дві окремі тапабельні зони в одній "скляній" картці, кожна ≥48dp. У діалозі часу за
 * замовчуванням циферблат, а кнопка-перемикач дає клавіатурний ввід (`TimeInput`): швидше, коли
 * людина вже знає точний час.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalField(
    label: String,
    dayMillis: Long,
    minuteOfDay: Int,
    onDateSelected: (Long) -> Unit,
    onMinuteSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minDayMillis: Long? = null,
    maxDayMillis: Long? = null,
    invalid: Boolean = false
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val timeText = remember(minuteOfDay) { "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60) }
    val dateText = remember(dayMillis) { SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(dayMillis)) }
    val fullDateText = remember(dayMillis) { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(dayMillis)) }
    val shape = RoundedCornerShape(16.dp)
    val invalidMessage = stringResource(R.string.add_entry_interval_invalid)

    Column(
        modifier = modifier
            .clip(shape)
            .background(TeperaPalette.cardTranslucentLight)
            .then(if (invalid) Modifier.border(1.dp, MaterialTheme.colorScheme.error, shape) else Modifier)
            // Помилка прив'язана до самого поля для скрінрідера (WCAG 3.3.1), а не лише окремий текст під формою.
            .semantics { if (invalid) error(invalidMessage) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TeperaPalette.buttonBrandDark)
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clickable(role = Role.Button) { showDatePicker = true }
                .semantics(mergeDescendants = true) { contentDescription = "$label, $fullDateText" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(TeperaSymbols.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp), tint = TeperaPalette.buttonBrandDark)
            Text(dateText, style = MaterialTheme.typography.bodyLarge, color = TeperaPalette.buttonBrandDark)
        }
        Text(
            timeText,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
            color = TeperaPalette.buttonBrand,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { showTimePicker = true }
                .semantics { contentDescription = "$label, $timeText" }
                .wrapContentHeight(Alignment.CenterVertically)
        )
    }

    if (showDatePicker) {
        TeperaDatePickerDialog(
            dayMillis = dayMillis,
            minDayMillis = minDayMillis,
            maxDayMillis = maxDayMillis,
            onSelected = { onDateSelected(it); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        TeperaTimePickerDialog(
            title = label,
            minuteOfDay = minuteOfDay,
            onSelected = { onMinuteSelected(it); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }
}

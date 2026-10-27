package com.serkodesign.tepera.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryGlyphColor
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.diary.HistoryEntryItem
import com.serkodesign.tepera.ui.diary.HistoryEntryRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Календар активностей: місяць або тиждень, у днях з офлайн-записами — стікери, наліплені як на
 * настінному календарі (кожен запис — окремий стікер, форма й нахил залежать від id запису).
 * Тап по дню зі стікерами відкриває нижню шторку з його записами.
 */
@Composable
fun CalendarScreen(
    activityRepository: ActivityRepository,
    categoryRepository: CategoryRepository,
    onEditEntry: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.Factory(activityRepository, categoryRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val anchor by viewModel.anchor.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    var openedDay by remember { mutableStateOf<LocalDate?>(null) }
    val today = remember { LocalDate.now() }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.calendar_screen_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillSegmentedControl(
                    options = listOf(
                        CalendarMode.MONTH to stringResource(R.string.calendar_mode_month),
                        CalendarMode.WEEK to stringResource(R.string.calendar_mode_week)
                    ),
                    selected = mode,
                    onSelect = viewModel::setMode
                )
                CalendarNavRow(
                    title = periodTitle(mode, anchor, firstDay, locale),
                    onPrev = { viewModel.step(-1) },
                    onNext = { viewModel.step(1) },
                    onToday = viewModel::goToToday
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(TeperaPalette.cardTranslucent)
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                ) {
                    when (mode) {
                        CalendarMode.MONTH -> MonthGrid(
                            month = YearMonth.from(anchor),
                            firstDay = firstDay,
                            today = today,
                            locale = locale,
                            stickers = state.stickers,
                            onOpenDay = { openedDay = it }
                        )
                        CalendarMode.WEEK -> WeekList(
                            days = weekDays(anchor, firstDay),
                            today = today,
                            locale = locale,
                            stickers = state.stickers,
                            onOpenDay = { openedDay = it }
                        )
                    }
                }
            }
        }
    }

    openedDay?.let { day ->
        val items = state.stickers[day].orEmpty()
        DaySheet(
            day = day,
            locale = locale,
            items = items.map { HistoryEntryItem(it.entry, it.category, it.entry.seriesId?.let(state.seriesRanges::get)) },
            onEdit = { id ->
                openedDay = null
                onEditEntry(id)
            },
            onDismiss = { openedDay = null }
        )
    }
}

@Composable
private fun CalendarNavRow(title: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeperaIconButton(
            icon = TeperaSymbols.ChevronRight,
            contentDescription = stringResource(R.string.calendar_prev),
            onClick = onPrev,
            modifier = Modifier.width(44.dp).rotate(180f)
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = TeperaPalette.headlineFont,
            color = TeperaPalette.buttonBrandDark
        )
        TeperaButton(
            text = stringResource(R.string.calendar_today),
            onClick = onToday,
            type = TeperaButtonType.Tertiary
        )
        TeperaIconButton(
            icon = TeperaSymbols.ChevronRight,
            contentDescription = stringResource(R.string.calendar_next),
            onClick = onNext
        )
    }
}

private fun periodTitle(mode: CalendarMode, anchor: LocalDate, firstDay: DayOfWeek, locale: Locale): String =
    when (mode) {
        CalendarMode.MONTH ->
            DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(anchor)
                .replaceFirstChar { it.titlecase(locale) }
        CalendarMode.WEEK -> {
            val days = weekDays(anchor, firstDay)
            val fmt = DateTimeFormatter.ofPattern("d.MM", locale)
            "${fmt.format(days.first())} – ${fmt.format(days.last())}"
        }
    }

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDay: DayOfWeek,
    today: LocalDate,
    locale: Locale,
    stickers: Map<LocalDate, List<CalendarSticker>>,
    onOpenDay: (LocalDate) -> Unit
) {
    val cells = remember(month, firstDay) { monthGrid(month, firstDay) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            weekDays(today, firstDay).forEach { d ->
                Text(
                    text = d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TeperaPalette.textSecondary
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    MonthDayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        stickers = stickers[date].orEmpty(),
                        locale = locale,
                        onClick = { onOpenDay(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    stickers: List<CalendarSticker>,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = stringResource(
        R.string.calendar_day_description,
        DateTimeFormatter.ofPattern("d MMMM", locale).format(date),
        stickers.size
    )
    Column(
        modifier = modifier
            .height(MONTH_CELL_HEIGHT.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.chipSurface)
            .clickable(enabled = stickers.isNotEmpty(), role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DayNumber(date, isToday, faded = !inMonth)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp)) {
            val layout = remember(stickers.size, maxWidth) { monthStickerSlots(stickers.size, maxWidth.value) }
            val s = layout.size.dp
            stickers.take(layout.shown).forEachIndexed { i, sticker ->
                val (x, y) = layout.slots[i]
                Sticker(sticker, s, Modifier.offset(x.dp, y.dp))
            }
            if (layout.more) {
                val (x, y) = layout.slots[layout.shown]
                Box(
                    Modifier.offset(x.dp, y.dp).size(s).clip(CircleShape)
                        .background(TeperaPalette.buttonBrandDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+${stickers.size - layout.shown}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TeperaPalette.colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun DayNumber(date: LocalDate, isToday: Boolean, faded: Boolean) {
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape)
            .background(if (isToday) TeperaPalette.buttonBrandDark else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 12.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isToday -> TeperaPalette.colors.onPrimary
                faded -> TeperaPalette.textSecondary.copy(alpha = 0.4f)
                else -> TeperaPalette.buttonBrandDark
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekList(
    days: List<LocalDate>,
    today: LocalDate,
    locale: Locale,
    stickers: Map<LocalDate, List<CalendarSticker>>,
    onOpenDay: (LocalDate) -> Unit
) {
    Column {
        days.forEach { date ->
            val items = stickers[date].orEmpty()
            val description = stringResource(
                R.string.calendar_day_description,
                DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(date),
                items.size
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .semantics(mergeDescendants = true) { contentDescription = description }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        fontSize = 12.sp,
                        color = TeperaPalette.textSecondary
                    )
                    DayNumber(date, date == today, faded = false)
                }
                // Біла картка дня (Figma: surface/card, радіус 16, відступ 12, між рядками стікерів 6).
                FlowRow(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TeperaPalette.chipSurface)
                        .clickable(enabled = items.isNotEmpty(), role = Role.Button) { onOpenDay(date) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { sticker ->
                        // Чіп тривалості (Figma 961:29840): пілюля «г:хх», що на 10dp заходить на низ стікера.
                        Column(
                            modifier = Modifier.padding(horizontal = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((-10).dp)
                        ) {
                            Sticker(sticker, 44.dp)
                            DurationChip(sticker.entry.durationMinutes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationChip(minutes: Int) {
    val shape = RoundedCornerShape(100.dp)
    Text(
        text = formatDurationClock(minutes),
        modifier = Modifier
            .clip(shape)
            .background(TeperaPalette.chipSurface)
            .border(1.dp, TeperaPalette.fieldBorder, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        fontSize = 11.sp,
        lineHeight = 12.sp,
        color = TeperaPalette.textSecondary
    )
}

/** Паперовий стікер: пастельна заливка кольору категорії, біла «вирубна» обвідка, тінь і нахил. */
@Composable
private fun Sticker(sticker: CalendarSticker, size: Dp, modifier: Modifier = Modifier) {
    val id = sticker.entry.id
    val color = categoryColor(sticker.category.colorHex)
    val kind = remember(id) { stickerShapeFor(id) }
    val shape = remember(kind) { shapeOf(kind) }
    val fit = remember(kind) { glyphFit(kind) }
    val tilt = remember(id) { stickerTiltFor(id) }
    val paper = lerp(Color.White, color, 0.4f)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = tilt }
            .shadow(2.dp, shape)
            .background(paper, shape)
            .border(BorderStroke(1.5.dp, Color.White), shape),
        contentAlignment = Alignment.Center
    ) {
        val glyph = categoryGlyphColor(color, badgeAlpha = 0.4f)
        val lineArt = categoryLineArtIconRes(sticker.category.iconName)
        if (lineArt != null) {
            Icon(painterResource(lineArt), contentDescription = null, tint = glyph, modifier = Modifier.offset(y = size * fit.dy).size(size * fit.scale))
        } else {
            Icon(categoryIcon(sticker.category.iconName), contentDescription = null, tint = glyph, modifier = Modifier.offset(y = size * fit.dy).size(size * fit.scale))
        }
    }
}

private fun shapeOf(kind: StickerShape): Shape = when (kind) {
    StickerShape.CIRCLE -> CircleShape
    StickerShape.SQUIRCLE -> RoundedCornerShape(32)
    StickerShape.COOKIE -> lobedShape(lobes = 9, amplitude = 0.07f)
    StickerShape.FLOWER -> lobedShape(lobes = 5, amplitude = 0.14f)
    // Аркуш із зрізаним нижнім правим кутом, як відривна нотатка.
    StickerShape.FOLDED -> GenericShape { size, _ ->
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height * 0.68f)
        lineTo(size.width * 0.68f, size.height)
        lineTo(0f, size.height)
        close()
    }
    StickerShape.HEART -> heartShape()
}

/** Серце з округлим низом: замість гострого кінчика — невелика дуга. Контур у сітці 100×100, масштабується під стікер. */
private fun heartShape() = GenericShape { size, _ ->
    val w = size.width / 100f
    val h = size.height / 100f
    moveTo(50f * w, 20f * h)
    cubicTo(53f * w, 13f * h, 59f * w, 8f * h, 68f * w, 8f * h)
    cubicTo(82f * w, 8f * h, 94f * w, 18f * h, 94f * w, 34f * h)
    cubicTo(94f * w, 58f * h, 66f * w, 80f * h, 58f * w, 88f * h)
    quadraticTo(50f * w, 96f * h, 42f * w, 88f * h)
    cubicTo(34f * w, 80f * h, 6f * w, 58f * h, 6f * w, 34f * h)
    cubicTo(6f * w, 18f * h, 18f * w, 8f * h, 32f * w, 8f * h)
    cubicTo(41f * w, 8f * h, 47f * w, 13f * h, 50f * w, 20f * h)
    close()
}

private fun lobedShape(lobes: Int, amplitude: Float) = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = min(size.width, size.height) / 2f / (1f + amplitude)
    val steps = 90
    for (i in 0..steps) {
        val t = 2.0 * PI * i / steps
        val rr = r * (1f + amplitude * cos(lobes * t).toFloat())
        val x = cx + rr * cos(t).toFloat()
        val y = cy + rr * sin(t).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    day: LocalDate,
    locale: Locale,
    items: List<HistoryEntryItem>,
    onEdit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = TeperaPalette.colors.backgroundTop ?: Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        com.serkodesign.tepera.util.ProvideAppLocale {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(day)
                        .replaceFirstChar { it.titlecase(locale) },
                    modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = TeperaPalette.headlineFont,
                    color = TeperaPalette.buttonBrandDark
                )
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { item ->
                        HistoryEntryRow(item = item, onEdit = { onEdit(item.entry.id) })
                    }
                }
            }
        }
    }
}

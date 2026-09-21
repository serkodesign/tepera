package com.serkodesign.tepera.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.serkodesign.tepera.util.localStartOfDay
import com.serkodesign.tepera.util.startOfTodayMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.home.HomeLabelValueChip
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaScreenTitle
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Щоденник" — за прямим запитом користувача перенесено сюди з екрана Статистики (там була
 * `HistoryCard`, окрема від `PeriodSelector`) на нову вкладку навбару (node 2146:320, Figma),
 * яка замінила неактивну заглушку "Незабаром". Зміст: "сьогодні"+"вчора", лічильники
 * розблокувань, час останнього використання (лише вчора), список записів з редагуванням/
 * видаленням через `AddEntryScreen` у режимі редагування.
 *
 * **Перемальовано за Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR, node 208:1261), значення
 * взяті з get_design_context/get_metadata, не зі скриншота:** заголовок 27sp Golos Medium
 * (#003926, відступ зліва 24/справа 16, висота 60), секції "Сьогодні"/"Вчора" — заголовок 18sp
 * Medium з відступом 8, проміжок 16 до списку й 32 між секціями, картки записів — білі 80%,
 * радіус 16, padding 12, gap 8, кружок 40dp, назва 16sp, два чіпи (#DCF6ED, текст #006944, 11sp
 * Medium) з тривалістю й інтервалом, кнопка редагування — іконка 24dp без фону. За відповідями
 * користувача: чіпи однакові для сьогодні й вчора (у макеті вчора було білим із сірим текстом),
 * кружок категорії — 20% її кольору (не 10-30% з макета), гліф — сам колір категорії; лічильники
 * розблокувань і "востаннє брав телефон" (яких нема в макеті) лишились, оформлені плашками
 * `HomeLabelValueChip`, як "Початок"/"День триває" на Home; нотатка запису — третім рядком.
 *
 * Кругла кнопка "+" (за прямим запитом користувача) — єдиний вхід на Щоденнику для ЗАГАЛЬНОГО
 * додавання активності (без попередньо обраної категорії, на відміну від кнопки "додати час"
 * на картці категорії на Home). Позиціонована окремим Box-оверлеєм поверх контенту: скрольований
 * контент отримує запас 100.dp знизу під напівпрозору навбар-"таблетку".
 */
@Composable
fun DiaryScreen(
    activityRepository: ActivityRepository,
    categoryRepository: CategoryRepository,
    balanceRepository: BalanceRepository,
    sleepWindowRepository: SleepWindowRepository,
    unlockRepository: UnlockRepository,
    pauseRepository: PauseRepository,
    onEditEntry: (String) -> Unit,
    onAddEntry: () -> Unit
) {
    val viewModel: DiaryViewModel = viewModel(
        factory = DiaryViewModel.Factory(
            activityRepository, categoryRepository, balanceRepository,
            sleepWindowRepository, unlockRepository, pauseRepository
        )
    )
    val state by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TeperaScreenTitle(stringResource(R.string.diary_screen_title))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                HistoryContent(
                    groups = state.history,
                    unlockCountsByDay = state.unlockCountsByDay,
                    lastPhoneUseYesterdayMillis = state.lastPhoneUseYesterdayMillis,
                    onEditEntry = onEditEntry
                )
            }
        }

        TeperaIconButton(
            icon = Icons.Filled.Add,
            contentDescription = stringResource(R.string.diary_add_entry_action),
            onClick = onAddEntry,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 112.dp)
                .width(64.dp),
            shape = CircleShape,
            containerColor = TeperaPalette.buttonBrand,
            contentColor = Color.White,
            height = 64.dp,
            iconSize = 32.dp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryContent(
    groups: List<HistoryDayGroup>,
    unlockCountsByDay: Map<Long, Int>,
    lastPhoneUseYesterdayMillis: Long?,
    onEditEntry: (String) -> Unit
) {
    val todayStart = remember { startOfTodayMillis() }
    val yesterdayStart = remember(todayStart) { localStartOfDay(todayStart - 1) }
    val groupsByDay = groups.associateBy { it.dayStartMillis }

    // Доба з'являється, якщо в ній є записи; сьогодні й вчора — ще й коли є лічильник розблокувань
    // чи "востаннє брав телефон" (як було до розширення історії). Старші доби без записів не
    // показуються порожніми чіпами — щоб 2 тижні не перетворились на стовпчик самих цифр.
    val dayStarts = (groupsByDay.keys +
        listOfNotNull(
            todayStart.takeIf { it in unlockCountsByDay },
            yesterdayStart.takeIf { it in unlockCountsByDay || lastPhoneUseYesterdayMillis != null }
        )).toSortedSet(compareByDescending { it })

    if (dayStarts.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.home_no_entries_today), color = TeperaPalette.buttonBrandDark)
        }
        return
    }

    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, d MMMM", locale) }

    dayStarts.forEach { dayStart ->
        val group = groupsByDay[dayStart]
        val isToday = dayStart == todayStart
        val isYesterday = dayStart == yesterdayStart
        val unlockCount = unlockCountsByDay[dayStart]
        val lastPhoneUseMillis = if (isYesterday) lastPhoneUseYesterdayMillis else null

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Figma 208:1549: Subheader 1 — Golos Text Medium 18sp, line-height 1.1, letter-spacing 0.018,
            // #003926, горизонтальний відступ 8.
            Text(
                text = when {
                    isToday -> stringResource(R.string.stats_history_today)
                    isYesterday -> stringResource(R.string.stats_history_yesterday)
                    else -> dateFormat.format(Date(dayStart)).replaceFirstChar { it.titlecase(locale) }
                },
                modifier = Modifier.padding(horizontal = 8.dp),
                color = TeperaPalette.buttonBrandDark,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 19.8.sp,
                letterSpacing = 0.018.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (unlockCount != null || lastPhoneUseMillis != null) {
                    FlowRow(
                        modifier = Modifier.padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        unlockCount?.let { count ->
                            HomeLabelValueChip(
                                label = stringResource(
                                    when {
                                        isToday -> R.string.diary_unlock_today_label
                                        isYesterday -> R.string.diary_unlock_yesterday_label
                                        else -> R.string.diary_unlock_label
                                    }
                                ),
                                value = count.toString()
                            )
                        }
                        lastPhoneUseMillis?.let { millis ->
                            HomeLabelValueChip(
                                label = stringResource(R.string.diary_last_phone_use_yesterday_label),
                                value = formatClockTime(millis)
                            )
                        }
                    }
                }
                group?.items?.forEach { item ->
                    HistoryEntryRow(item = item, onEdit = { onEditEntry(item.entry.id) })
                }
            }
        }
    }
}

private fun formatClockTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryEntryRow(item: HistoryEntryItem, onEdit: () -> Unit) {
    val accentColor = categoryColor(item.category.colorHex)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val endMillis = item.entry.startTime + item.entry.durationMinutes * 60_000L
    val (hours, remainderMinutes) = roundToQuarterHour(item.entry.durationMinutes)
    val durationText = when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
    val rangeText = "${timeFormat.format(Date(item.entry.startTime))}-${timeFormat.format(Date(endMillis))}"

    // Figma 208:1554: біла заливка 80%, радіус 16, padding 12, gap 8.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .clickable(onClick = onEdit)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            val lineArt = categoryLineArtIconRes(item.category.iconName)
            if (lineArt != null) {
                Icon(painterResource(lineArt), contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            } else {
                Icon(categoryIcon(item.category.iconName), contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Figma 208:1558: Body 1 — Golos Text Regular 16sp, line-height 1.3, letter-spacing 0.016, #003926.
            Text(
                text = categoryDisplayName(item.category),
                color = TeperaPalette.buttonBrandDark,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 20.8.sp,
                letterSpacing = 0.016.sp
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EntryChip(durationText)
                EntryChip(rangeText)
            }
            if (!item.entry.note.isNullOrBlank()) {
                Text(
                    text = item.entry.note,
                    fontSize = 12.sp,
                    lineHeight = 15.6.sp,
                    color = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }
        // Figma 208:1564: область 40x40, іконка edit 24dp (#1C1B1F), без фону.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onEdit),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TeperaIcons.Edit,
                contentDescription = stringResource(R.string.stats_history_edit_action),
                tint = Color(0xFF1C1B1F),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Чіп запису (Figma 208:1560): #DCF6ED, радіус 100, padding 8x4, текст 11sp Medium #006944, line-height 1.1. */
@Composable
private fun EntryChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(TeperaPalette.surfaceBrandLight)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TeperaPalette.buttonBrand,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 12.1.sp,
            letterSpacing = 0.011.sp,
            maxLines = 1
        )
    }
}

package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.serkodesign.tepera.ui.theme.LocalTeperaColors
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.NudgeScheduler
import com.serkodesign.tepera.data.SuggestionPicker
import com.serkodesign.tepera.data.WeeklySummaryWorker
import com.serkodesign.tepera.data.suggestionTextRes
import com.serkodesign.tepera.domain.risk.ActivitySuggestions
import com.serkodesign.tepera.domain.risk.NextWindow
import com.serkodesign.tepera.domain.risk.NudgePlanner
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.ui.theme.M3Chevron
import com.serkodesign.tepera.ui.theme.M3Extra
import com.serkodesign.tepera.ui.theme.M3GroupLabel
import com.serkodesign.tepera.ui.theme.M3ListDivider
import com.serkodesign.tepera.ui.theme.M3ListGroup
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.util.WindowSkipStore
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Головна v2 — Figma "Tepera", 07 · Головна v2 (1081:2166): наступна година, тиждень одним числом, сьогоднішні години
 * рядками. Планів «якщо — то» немає (рішення власника, 11.11.2026): головний текст картки — підказка, що зробити
 * замість стрічки, з того самого набору, що й лист поверх стрічки.
 */

private val HeroShape = RoundedCornerShape(28.dp)

/** `HomeHeader`: дата (Body Small) над «Сьогодні» (Headline Small) і кругла кнопка налаштувань 44dp на склі. */
@Composable
internal fun HomeV2Header(date: String, onOpenSettings: () -> Unit, settingsModifier: Modifier = Modifier) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.v2_home_title), style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() }
            )
        }
        TeperaIconButton(
            icon = TeperaIcons.Settings,
            contentDescription = stringResource(R.string.settings_nav_action),
            onClick = onOpenSettings,
            modifier = Modifier.width(44.dp).then(settingsModifier),
            shape = CircleShape,
            containerColor = M3Extra.colors.glass,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * `next hour`: найближча година. До години — назва й час, відлік дрібним чипом, підказка великим текстом і «Не сьогодні»;
 * під час години — «до HH:MM», крапка «зараз» і факт із UsageStats без кнопок; коли сьогодні все — перша година завтра.
 */
@Composable
internal fun NextHourCard(
    window: RiskWindowUi,
    occurrence: NextWindow.Occurrence,
    now: LocalDateTime,
    logicalToday: LocalDate,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val inside = occurrence.isInside(now)
    val tomorrow = !inside && occurrence.startAt.minusHours(RiskConfig().dayCutoffHour.toLong()).toLocalDate().isAfter(logicalToday)
    val part = stringResource(partOfDayRes(window.partOfDay))
    val title = when {
        inside -> stringResource(R.string.v2_next_title_inside, part, clock(occurrence.endAt))
        tomorrow -> stringResource(R.string.v2_next_title_tomorrow, part, formatWindowTime(window.startMinute, window.endMinute))
        else -> stringResource(R.string.v2_next_title_upcoming, part, clock(occurrence.startAt))
    }
    var idea by remember(window.id) { mutableStateOf<ActivitySuggestions.Id?>(null) }
    LaunchedEffect(window.id, inside) {
        idea = PlanOverlayStore.activeCommitment(context) ?: SuggestionPicker.pickFor(context, window.id)?.id
    }
    var skipped by remember(window.id, occurrence.startAt) { mutableStateOf(WindowSkipStore.isSkipped(context, window.id)) }
    val zone = ZoneId.systemDefault()

    // Темна тема (звичайна й «Світанок»): не чорна, а м'яка підсвітка кольором акценту — трохи відрізняється від скляних
    // карток, але лишається в гамі фону. Світла тема без змін.
    val teperaColors = LocalTeperaColors.current
    val cardTint = when {
        !teperaColors.isDark -> null
        teperaColors.isDawn -> teperaColors.brand
        else -> Color(0xFF71CCA4)
    }
    val cardFill = cardTint?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.surfaceContainerLowest
    val cardStroke = cardTint?.copy(alpha = 0.40f) ?: MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.fillMaxWidth().clip(HeroShape).background(cardFill)
            .border(1.dp, cardStroke, HeroShape)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
            )
            when {
                inside -> WhenChip(stringResource(R.string.v2_when_now), dot = true)
                !tomorrow -> WhenChip(
                    stringResource(R.string.v2_when_in, durationText(Duration.between(now, occurrence.startAt).toMinutes().coerceAtLeast(1)))
                )
            }
        }
        Text(
            text = idea?.let { stringResource(suggestionTextRes(it)) } ?: stringResource(R.string.v2_next_idea_fallback),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val footer: String?
            val icon = if (inside) TeperaIcons.Smartphone else TeperaSymbols.Schedule
            footer = when {
                inside -> stringResource(R.string.v2_next_fact, (window.todayMinutes ?: 0.0).roundToInt())
                skipped -> stringResource(R.string.v2_next_skipped)
                // Нагадування до години поки вимкнені в застосунку — тоді й не обіцяємо їх.
                NudgeScheduler.NUDGES_ENABLED && WeeklySummaryWorker.canNotify(context) ->
                    stringResource(R.string.v2_next_reminder, clock(occurrence.startAt.minusMinutes(NudgePlanner.LEAD_MINUTES)))
                else -> null
            }
            if (footer != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(
                    footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(Modifier.weight(1f))
            }
            // «Не сьогодні» — лише до години: вікно мовчить до кінця найближчого проходу (без листа поверх стрічки й нагадування).
            if (!inside) {
                TeperaButton(
                    text = stringResource(if (skipped) R.string.v2_not_today_undo else R.string.v2_not_today),
                    type = TeperaButtonType.Secondary,
                    onClick = {
                        if (skipped) WindowSkipStore.undo(context, window.id)
                        else WindowSkipStore.skipUntil(context, window.id, occurrence.endAt.atZone(zone).toInstant().toEpochMilli())
                        skipped = !skipped
                        onChanged()
                    }
                )
            }
        }
    }
}

/** Чип `when`: surfaceContainerHigh, Label Medium; «зараз» — з крапкою 8dp кольору signalVivid. */
@Composable
private fun WhenChip(text: String, dot: Boolean = false) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot) Box(Modifier.size(8.dp).clip(CircleShape).background(M3Extra.colors.signalVivid))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/**
 * `week`: середнє на день у стрічках у твої години. Дві смуги одного масштабу — тиждень спостереження (signalVivid) і
 * останні 7 діб (primary); поки після спостереження не минув тиждень — лише «Точка відліку» однією смугою.
 */
@Composable
internal fun WeekCard(windows: List<RiskWindowUi>, daysSinceObservation: Int?, onOpen: (() -> Unit)?) {
    val baseline = windows.mapNotNull { it.baselineMinutes }.takeIf { it.isNotEmpty() }?.sum()
    val current = windows.mapNotNull { it.currentMinutes }.takeIf { it.isNotEmpty() }?.sum()
    val reference = daysSinceObservation == null || daysSinceObservation < 7 || current == null
    val big = (if (reference) baseline else current) ?: return
    val scale = maxOf(baseline ?: 0.0, current ?: 0.0, 1.0)
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(M3Extra.colors.glass)
            .border(1.dp, M3Extra.colors.cardStroke, shape)
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (reference) R.string.v2_week_reference_title else R.string.v2_week_title),
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onOpen != null) M3Chevron()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                durationText(big.roundToInt().toLong()), style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(if (reference) R.string.v2_week_reference_unit else R.string.v2_week_unit),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Column(Modifier.fillMaxWidth().padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (baseline != null) WeekBar(stringResource(R.string.v2_week_baseline), baseline, scale, M3Extra.colors.signalVivid)
            if (!reference && current != null) {
                WeekBar(stringResource(R.string.v2_week_current), current, scale, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun WeekBar(label: String, minutes: Double, scale: Double, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.width(104.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp)).background(M3Extra.colors.dataTrack)) {
            val fraction = (minutes / scale).toFloat().coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceAtLeast(0.03f)).clip(RoundedCornerShape(999.dp)).background(color))
            }
        }
        Text(
            stringResource(R.string.v3_dur_minutes, minutes.roundToInt()),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End, modifier = Modifier.width(48.dp)
        )
    }
}

/** Рядок години сьогодні (`today/row`): час 48dp (Title Medium), назва й підпис, шеврон; веде в «Чому саме це вікно». */
@Composable
private fun TodayRow(window: RiskWindowUi, supporting: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            clockOf(window.startMinute), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(48.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(stringResource(partOfDayRes(window.partOfDay)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        M3Chevron()
    }
}

/**
 * Сьогоднішні години рядками: «Сьогодні вже було» — факт без оцінки (хвилини в стрічках · зазвичай N); «Далі сьогодні» —
 * години, що ще попереду (крім тієї, що вже в картці зверху). Коли все минуло — один список «Сьогодні».
 */
@Composable
internal fun TodayRows(
    windows: List<RiskWindowUi>,
    now: LocalDateTime,
    logicalToday: LocalDate,
    heroWindowId: String?,
    onOpenWhy: (String) -> Unit
) {
    val cutoff = RiskConfig().dayCutoffHour
    val todays = windows.filter { !it.isPaused && it.todayMinutes != null }.map { w ->
        // Прохід вікна в поточній логічній добі: години до межі доби належать наступній календарній даті.
        val startDate = if (w.startMinute < cutoff * 60) logicalToday.plusDays(1) else logicalToday
        val start = startDate.atStartOfDay().plusMinutes(w.startMinute.toLong())
        val length = ((w.endMinute - w.startMinute + 1440) % 1440).toLong().let { if (it == 0L) 1440L else it }
        Triple(w, start, start.plusMinutes(length))
    }.sortedBy { it.second }
    val passed = todays.filter { !it.third.isAfter(now) }
    val upcoming = todays.filter { it.second.isAfter(now) && it.first.id != heroWindowId }
    if (passed.isEmpty() && upcoming.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (passed.isNotEmpty()) {
            M3GroupLabel(
                // Усі сьогоднішні години вже минули — один список «Сьогодні».
                stringResource(if (todays.none { it.third.isAfter(now) }) R.string.v2_today_all else R.string.v2_today_was),
                top = 8.dp, bottom = 0.dp
            )
            M3ListGroup {
                passed.forEachIndexed { i, (w, _, _) ->
                    if (i > 0) M3ListDivider()
                    TodayRow(
                        w,
                        stringResource(R.string.v2_row_fact, (w.todayMinutes ?: 0.0).roundToInt(), w.avgMinutes),
                        onClick = { onOpenWhy(w.id) }
                    )
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            M3GroupLabel(stringResource(R.string.v2_today_next), top = 8.dp, bottom = 0.dp)
            M3ListGroup {
                upcoming.forEachIndexed { i, (w, _, _) ->
                    if (i > 0) M3ListDivider()
                    TodayRow(w, formatWindowTime(w.startMinute, w.endMinute), onClick = { onOpenWhy(w.id) })
                }
            }
        }
    }
}

/** Пропозиція зсунути вікно — лише коли вона є (рішення власника): та сама дія, що була в тижневому підсумку. */
@Composable
internal fun ShiftSuggestions(windows: List<RiskWindowUi>, onApply: (String) -> Unit, onKeep: (String) -> Unit) {
    val withShift = windows.filter { it.shiftStart != null && it.shiftEnd != null }
    if (withShift.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        withShift.forEach { w ->
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(M3Extra.colors.glass)
                    .border(1.dp, M3Extra.colors.cardStroke, shape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    formatWindowTime(w.startMinute, w.endMinute), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.v3_summary_shift, formatWindowTime(w.shiftStart!!, w.shiftEnd!!)),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TeperaButton(stringResource(R.string.v3_summary_shift_apply), onClick = { onApply(w.id) }, modifier = Modifier.weight(1f))
                    TeperaButton(
                        stringResource(R.string.v3_summary_shift_keep), onClick = { onKeep(w.id) },
                        type = TeperaButtonType.Secondary, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun clock(dt: LocalDateTime): String = String.format(Locale.ROOT, "%02d:%02d", dt.hour, dt.minute)

private fun clockOf(minuteOfDay: Int): String = String.format(Locale.ROOT, "%02d:%02d", minuteOfDay / 60 % 24, minuteOfDay % 60)

@Composable
private fun durationText(totalMinutes: Long): String {
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> stringResource(R.string.v3_dur_days_hours, days.toInt(), hours.toInt())
        hours > 0 && minutes > 0 -> stringResource(R.string.v3_dur_hours_minutes, hours.toInt(), minutes.toInt())
        hours > 0 -> stringResource(R.string.v3_dur_hours, hours.toInt())
        else -> stringResource(R.string.v3_dur_minutes, minutes.toInt())
    }
}

package com.serkodesign.tepera.widget

import android.content.Context
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.isPausedAt
import com.serkodesign.tepera.domain.risk.ActivitySuggestions
import com.serkodesign.tepera.domain.risk.HeatGrid
import com.serkodesign.tepera.domain.risk.LogicalDay
import com.serkodesign.tepera.domain.risk.NextWindow
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.WindowMinutes
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.util.WindowSkipStore
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Дані віджетів режиму «години скролінгу» й дій (наступна година, ідея, тиждень, паузи). Нічого не кешується тут: віджети
 * перераховують це при кожному оновленні (WorkManager ~30 хв, тапи, зміни таймерів), а самі запити легкі.
 */

/** Найближча година детоксу: та, що триває, або перша з тих, що настануть (логіка з картки `NextHourCard` на Головній). */
internal class NextHourInfo(
    val windowId: String,
    val part: PartOfDay,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val inside: Boolean,
    val tomorrow: Boolean,
    val skipped: Boolean,
    val startMinute: Int,
    val endMinute: Int
)

/** Тиждень у години детоксу: хвилини на день у стрічках (тиждень спостереження й останні 7 діб). */
internal class WeekInfo(val baseline: Double?, val current: Double?, val reference: Boolean) {
    /** Число для великого тексту. */
    val big: Double? get() = if (reference) baseline else current
}

internal object ActionWidgetData {

    /** `null` — годин ще нема (спостереження триває або всі вимкнені/на паузі). */
    suspend fun nextHour(app: TeperaApp, nowMillis: Long = System.currentTimeMillis()): NextHourInfo? {
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        // Вимкнені людиною й призупинені вікна не рахуємо — як на Головній (там для них не показується «зараз твоя година»).
        val windows = app.riskWindowRepository.getAll().filter { !it.isUserDisabled && !it.isPausedAt(nowMillis) }
        val occurrences = NextWindow.occurrences(
            windows.map { NextWindow.Spec(it.id, it.startMinute, it.endMinute, it.dayType) }, now
        )
        val occurrence = occurrences.firstOrNull { it.isInside(now) } ?: occurrences.firstOrNull() ?: return null
        val window = windows.firstOrNull { it.id == occurrence.id } ?: return null
        val cutoff = RiskConfig().dayCutoffHour.toLong()
        val logicalToday = now.minusHours(cutoff).toLocalDate()
        val inside = occurrence.isInside(now)
        return NextHourInfo(
            windowId = window.id,
            part = HeatGrid.partOfDay(window.startMinute),
            startAt = occurrence.startAt,
            endAt = occurrence.endAt,
            inside = inside,
            tomorrow = !inside && occurrence.startAt.minusHours(cutoff).toLocalDate().isAfter(logicalToday),
            skipped = WindowSkipStore.isSkipped(app, window.id, nowMillis),
            startMinute = window.startMinute,
            endMinute = window.endMinute
        )
    }

    /**
     * Той самий розрахунок, що картка «Цей тиждень» на Головній (`RiskHomeViewModel`): сума середніх хвилин на день за всіма
     * годинами — у тиждень спостереження й за останні 7 завершених діб. Поки після спостереження не минув тиждень,
     * лишається лише «Точка відліку».
     */
    suspend fun week(app: TeperaApp, nowMillis: Long = System.currentTimeMillis()): WeekInfo? {
        val cfg = RiskConfig()
        val zone = ZoneId.systemDefault()
        val feed = app.feedAppRepository.packages()
        if (feed.isEmpty()) return null
        val windows = app.riskWindowRepository.getAll().filter { !it.isUserDisabled }
        if (windows.isEmpty()) return null
        val lastLogicalDay = Instant.ofEpochMilli(nowMillis).atZone(zone).minusHours(cfg.dayCutoffHour.toLong()).toLocalDate().minusDays(1)
        val logicalDays = (cfg.observationDays - 1 downTo 0).map { lastLogicalDay.minusDays(it.toLong()) }
        val period = app.database.observationPeriodDao().getLatest()
        val baselineDays = period?.let { p ->
            val end = p.endDate?.let(LocalDate::ofEpochDay) ?: lastLogicalDay
            generateSequence(LocalDate.ofEpochDay(p.startDate)) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
        }.orEmpty()
        val from = minOf(logicalDays.first(), baselineDays.firstOrNull() ?: logicalDays.first())
        val records = app.usageSlotRepository.readRecords(from, lastLogicalDay.plusDays(1), feed)
        val baseline = if (baselineDays.isEmpty()) null else windows
            .mapNotNull { WindowMinutes.averagePerDay(records, baselineDays, it.startMinute, it.endMinute, it.dayType, cfg) }
            .takeIf { it.isNotEmpty() }?.sum()
        val current = windows
            .mapNotNull { WindowMinutes.averagePerDay(records, logicalDays, it.startMinute, it.endMinute, it.dayType, cfg) }
            .takeIf { it.isNotEmpty() }?.sum()
        val daysSince = period?.endDate?.let { end ->
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.ofEpochDay(end), lastLogicalDay).toInt().coerceAtLeast(0)
        }
        val reference = daysSince == null || daysSince < 7 || current == null
        val info = WeekInfo(baseline, current, reference)
        return if (info.big == null) null else info
    }

    /**
     * Скільки пауз без телефону за останню добу ще не названо. Лише те, що Tepera вже знайшла (скан паузи йде, коли
     * відкривають застосунок), тож віджет нічого не сканує сам.
     */
    suspend fun unnamedPauses(app: TeperaApp, nowMillis: Long = System.currentTimeMillis()): Int =
        app.pauseRepository.getUnresolvedGaps(nowMillis - 24 * 3_600_000L, nowMillis).size
}

/**
 * Ідея для віджета «Замість скролу»: одна на пору доби, тримається, доки людина не натисне «Інша» (або минуло 3 години).
 * Підбір той самий, що в підказок поверх стрічки ([ActivitySuggestions]), але пора доби — поточна, а не початку вікна.
 */
internal object WidgetIdeaStore {
    private const val PREFS = "widget_idea"
    private const val KEY_ID = "id"
    private const val KEY_AT = "at"
    private const val KEY_PART = "part"
    private const val TTL_MILLIS = 3 * 3_600_000L

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun currentPart(nowMillis: Long): PartOfDay {
        val minutes = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }
        return HeatGrid.partOfDay(minutes)
    }

    suspend fun current(context: Context, nowMillis: Long = System.currentTimeMillis()): ActivitySuggestions.Id? {
        val p = prefs(context)
        val saved = p.getString(KEY_ID, null)?.let { name -> ActivitySuggestions.Id.entries.firstOrNull { it.name == name } }
        val fresh = nowMillis - p.getLong(KEY_AT, 0L) < TTL_MILLIS && p.getString(KEY_PART, null) == currentPart(nowMillis).name
        // Ідея, яку людина вже робить зараз (взяла з підказки), важливіша за випадкову.
        PlanOverlayStore.activeCommitment(context, nowMillis)?.let { return it }
        return if (saved != null && fresh) saved else pick(context, emptySet(), nowMillis)
    }

    /** «Інша»: нова ідея, не та, що щойно показана. */
    suspend fun another(context: Context, nowMillis: Long = System.currentTimeMillis()): ActivitySuggestions.Id? {
        val shown = prefs(context).getString(KEY_ID, null)?.let { name -> ActivitySuggestions.Id.entries.firstOrNull { it.name == name } }
        return pick(context, setOfNotNull(shown), nowMillis)
    }

    private suspend fun pick(context: Context, exclude: Set<ActivitySuggestions.Id>, nowMillis: Long): ActivitySuggestions.Id? {
        val app = context.applicationContext as TeperaApp
        val available = app.categoryRepository.observeActiveCategories().first().map { it.id }.toSet()
        val part = currentPart(nowMillis)
        val weekend = LocalDate.ofEpochDay(LogicalDay.epochDay(nowMillis)).dayOfWeek.value > 5
        val id = ActivitySuggestions.pick(
            part = part,
            weekend = weekend,
            availableCategories = available,
            declined = PlanOverlayStore.declined(context),
            exclude = exclude,
            nowMillis = nowMillis
        )?.id
        prefs(context).edit().apply {
            if (id != null) putString(KEY_ID, id.name) else remove(KEY_ID)
            putLong(KEY_AT, nowMillis)
            putString(KEY_PART, part.name)
        }.apply()
        return id
    }
}

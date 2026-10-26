package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.NudgeEventDao
import com.serkodesign.tepera.data.local.dao.ObservationPeriodDao
import com.serkodesign.tepera.data.local.dao.PlanDao
import com.serkodesign.tepera.data.local.dao.RiskWindowDao
import com.serkodesign.tepera.data.local.entity.NudgeAction
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.WindowMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class TesterWindowStat(
    val startMinute: Int,
    val endMinute: Int,
    val dayType: DayType,
    val recurrenceDays: Int,
    val observedDays: Int,
    val avgMinutes: Int,
    val isUserDisabled: Boolean,
    val hasPlan: Boolean,
    val shiftSuggested: Boolean,
    /** Вікно задала людина вручну (а не знайшов алгоритм). */
    val isManual: Boolean = false,
    /** Середні хвилини у вікні за добу в тиждень спостереження / в останні 7 завершених діб; `null` — нема з чого рахувати. */
    val baselineMinutes: Double?,
    val currentMinutes: Double?
)

data class TesterStats(
    val firstDataDate: LocalDate?,
    val coveredDays: Int,
    val feedAppCount: Int,
    val baselineRange: Pair<LocalDate, LocalDate>?,
    val currentRange: Pair<LocalDate, LocalDate>,
    val windows: List<TesterWindowStat>,
    val nudgesScheduled: Int,
    val nudgesShown: Int,
    val nudgesOk: Int,
    val nudgesNotToday: Int,
    /** Середня затримка показу відносно запланованого, хвилини; `null`, доки нічого не показано. */
    val nudgeDelayMinutes: Double? = null,
    /** Етап 8: скільки разів показано план поверх стрічки і що людина обрала; `null` — функція не використовується (не beta). */
    val overlay: Triple<Int, Int, Int>? = null
)

/**
 * v3.0 MVP, Етап 7: підсумок для прихованого екрана «Статистика для тестувальників». Лише локальні дані,
 * без мережі; текст копіюється вручну ([format]).
 */
class TesterStatsRepository(
    private val slots: UsageSlotRepository,
    private val feedApps: FeedAppRepository,
    private val windowDao: RiskWindowDao,
    private val planDao: PlanDao,
    private val nudgeDao: NudgeEventDao,
    private val periodDao: ObservationPeriodDao
) {
    suspend fun build(nowMillis: Long = System.currentTimeMillis(), cfg: RiskConfig = RiskConfig()): TesterStats =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val lastLogicalDay = Instant.ofEpochMilli(nowMillis).atZone(zone)
                .minusHours(cfg.dayCutoffHour.toLong()).toLocalDate().minusDays(1)
            val currentDays = (cfg.observationDays - 1 downTo 0).map { lastLogicalDay.minusDays(it.toLong()) }
            val period = periodDao.getLatest()
            val baselineDays = period?.let { p ->
                val start = LocalDate.ofEpochDay(p.startDate)
                val end = p.endDate?.let(LocalDate::ofEpochDay) ?: lastLogicalDay
                generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
            }.orEmpty()

            val feed = feedApps.packages()
            val earliest = (baselineDays + currentDays).minOrNull() ?: lastLogicalDay
            val records = slots.readRecords(earliest, lastLogicalDay.plusDays(1), feed)
            val covered = slots.coveredDays()

            val windows = windowDao.getAll().map { w ->
                TesterWindowStat(
                    w.startMinute, w.endMinute, w.dayType, w.recurrenceDays, w.observedDays, w.avgMinutes,
                    w.isUserDisabled,
                    hasPlan = planDao.getForWindow(w.id) != null,
                    shiftSuggested = w.suggestedStartMinute != null,
                    isManual = w.isManual,
                    baselineMinutes = WindowMinutes.averagePerDay(records, baselineDays, w.startMinute, w.endMinute, w.dayType, cfg),
                    currentMinutes = WindowMinutes.averagePerDay(records, currentDays, w.startMinute, w.endMinute, w.dayType, cfg)
                )
            }
            val all = 0L to Long.MAX_VALUE
            TesterStats(
                firstDataDate = covered.minOrNull(),
                coveredDays = covered.size,
                feedAppCount = feed.size,
                baselineRange = baselineDays.takeIf { it.isNotEmpty() }?.let { it.first() to it.last() },
                currentRange = currentDays.first() to currentDays.last(),
                windows = windows,
                nudgesScheduled = nudgeDao.countScheduled(all.first, all.second),
                nudgesShown = nudgeDao.countShown(all.first, all.second),
                nudgesOk = nudgeDao.countByAction(NudgeAction.OK, all.first, all.second),
                nudgesNotToday = nudgeDao.countByAction(NudgeAction.NOT_TODAY, all.first, all.second),
                nudgeDelayMinutes = nudgeDao.averageDelayMillis()?.let { it / 60_000.0 }
            )
        }


    companion object {
        private fun hm(minute: Int) = String.format(Locale.ROOT, "%02d:%02d", minute / 60 % 24, minute % 60)

        private const val DASH = "—"

        /**
         * Текст для вставки у форму. Мова — та, якою відкрито застосунок: шаблони приходять із ресурсів ([ReportText]),
         * цифри й дати форматуються тут, без залежності від локалі.
         */
        fun format(s: TesterStats, t: ReportText): String = buildString {
            fun minutes(value: Double?) = value?.let { t.minutes.format(String.format(Locale.ROOT, "%.0f", it)) } ?: DASH

            appendLine(t.title)
            appendLine(t.firstData.format(s.firstDataDate?.toString() ?: DASH, s.coveredDays, s.feedAppCount))
            appendLine(
                t.periods.format(
                    s.baselineRange?.let { t.range.format(it.first.toString(), it.second.toString()) } ?: t.periodNotSet,
                    t.range.format(s.currentRange.first.toString(), s.currentRange.second.toString())
                )
            )
            if (s.windows.isEmpty()) {
                appendLine(t.noWindows)
            } else {
                s.windows.forEach { w ->
                    val tags = listOfNotNull(
                        if (w.isManual) t.tagManual else null,
                        if (w.dayType == DayType.WEEKDAYS) t.tagWeekdays else null,
                        if (w.isUserDisabled) t.tagDisabled else null,
                        if (w.hasPlan) t.tagPlan else t.tagNoPlan,
                        if (w.shiftSuggested) t.tagShift else null
                    ).joinToString(", ")
                    appendLine(
                        t.window.format(
                            hm(w.startMinute), hm(w.endMinute), tags, w.recurrenceDays, w.observedDays, w.avgMinutes,
                            minutes(w.baselineMinutes), minutes(w.currentMinutes)
                        )
                    )
                }
            }
            appendLine(t.nudges.format(s.nudgesScheduled, s.nudgesShown, s.nudgesOk, s.nudgesNotToday))
            s.nudgeDelayMinutes?.let { appendLine(t.nudgeDelay.format(String.format(Locale.ROOT, "%.1f", it))) }
            s.overlay?.let { (shown, left, opened) -> appendLine(t.overlay.format(shown, left, opened)) }
            appendLine(t.note)
        }
    }
}

/**
 * Шаблони тексту звіту (з рядкових ресурсів, `v3_report_*`). Шаблони з `%N$s`/`%N$d` підставляє [format]; окремий
 * клас потрібен, щоб формат звіту тестувався на JVM без Android-ресурсів.
 */
data class ReportText(
    val title: String,
    val firstData: String,
    val periods: String,
    val periodNotSet: String,
    val range: String,
    val noWindows: String,
    val window: String,
    val tagManual: String,
    val tagWeekdays: String,
    val tagDisabled: String,
    val tagPlan: String,
    val tagNoPlan: String,
    val tagShift: String,
    val minutes: String,
    val nudges: String,
    val nudgeDelay: String,
    val overlay: String,
    val note: String
)

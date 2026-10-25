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
    val nudgesNotToday: Int
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
                nudgesNotToday = nudgeDao.countByAction(NudgeAction.NOT_TODAY, all.first, all.second)
            )
        }

    companion object {
        private fun hm(minute: Int) = String.format(Locale.ROOT, "%02d:%02d", minute / 60 % 24, minute % 60)

        private fun minutes(value: Double?) = value?.let { String.format(Locale.ROOT, "%.0f хв", it) } ?: "—"

        /** Текст для вставки у форму. Мова фіксована (українська): це службовий звіт тестувальника. */
        fun format(s: TesterStats): String = buildString {
            appendLine("Tepera v3 · статистика тестувальника")
            appendLine("Перші дані: ${s.firstDataDate ?: "—"}; днів покрито: ${s.coveredDays}; стрічок обрано: ${s.feedAppCount}")
            appendLine(
                "Тиждень спостереження: ${s.baselineRange?.let { "${it.first} — ${it.second}" } ?: "ще не заданий"}; " +
                    "поточний тиждень: ${s.currentRange.first} — ${s.currentRange.second}"
            )
            if (s.windows.isEmpty()) {
                appendLine("Вікон нема")
            } else {
                s.windows.forEach { w ->
                    val tags = listOfNotNull(
                        if (w.dayType == DayType.WEEKDAYS) "будні" else null,
                        if (w.isUserDisabled) "вимкнене" else null,
                        if (w.hasPlan) "є план" else "без плану",
                        if (w.shiftSuggested) "запропоновано зсув" else null
                    ).joinToString(", ")
                    appendLine(
                        "Вікно ${hm(w.startMinute)}–${hm(w.endMinute)} ($tags): повторюваність ${w.recurrenceDays}/${w.observedDays}, " +
                            "≈${w.avgMinutes} хв; хвилини у вікні за добу: спостереження ${minutes(w.baselineMinutes)} → " +
                            "поточний тиждень ${minutes(w.currentMinutes)}"
                    )
                }
            }
            appendLine(
                "Нагадування: заплановано ${s.nudgesScheduled}, показано ${s.nudgesShown}, " +
                    "«Добре» ${s.nudgesOk}, «Не сьогодні» ${s.nudgesNotToday}"
            )
            appendLine("Примітка: вікна підбирались за тижнем спостереження, тож його хвилини у вікнах завищені відносно звичайного тижня — порівняння орієнтовне.")
        }
    }
}

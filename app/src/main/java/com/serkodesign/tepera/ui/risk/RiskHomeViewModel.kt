package com.serkodesign.tepera.ui.risk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.FoundWindowsNotifier
import com.serkodesign.tepera.data.local.entity.isPausedAt
import com.serkodesign.tepera.data.WeeklySummaryWorker
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.data.NudgeScheduler
import com.serkodesign.tepera.data.local.entity.NudgeAction
import com.serkodesign.tepera.data.repository.ScanOutcome
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.HeatGrid
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.PackageSlotUsage
import com.serkodesign.tepera.domain.risk.RiskWindowFinder
import com.serkodesign.tepera.domain.risk.WindowExplain
import com.serkodesign.tepera.domain.risk.WindowMinutes
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Стан кружка «повторювалось у цей день» на картці вікна. */
enum class DotState { HIT, MISS, NOT_APPLICABLE }

data class RiskWindowUi(
    val id: String,
    val startMinute: Int,
    val endMinute: Int,
    val dayType: DayType,
    val recurrenceDays: Int,
    val observedDays: Int,
    val avgMinutes: Int,
    val partOfDay: PartOfDay,
    val appLabels: List<String>,
    val dots: List<DotState>,
    /** Збережений план («якщо» і «то» дослівно) або `null`. */
    val plan: Pair<String, String>? = null,
    /** Людина вимкнула вікно («Вважати це вікно годиною ризику» у M6): показується приглушеним у кінці списку. */
    val isDisabled: Boolean = false,
    /** Вікно на паузі (повторюваність впала, напр. відпустка): не нагадує, повернеться само. */
    val isPaused: Boolean = false,
    val isManual: Boolean = false,
    /** Середні хвилини у вікні за добу: тиждень спостереження / останні 7 завершених діб; `null` — нема з чого рахувати. */
    val baselineMinutes: Double? = null,
    val currentMinutes: Double? = null,
    /** «Добре» на нагадуваннях цього вікна за останні 7 діб (самозвіт). */
    val okThisWeek: Int = 0,
    val notTodayThisWeek: Int = 0,
    /** Запропонований зсув вікна (етап 2, п. 8) або `null`. */
    val shiftStart: Int? = null,
    val shiftEnd: Int? = null
)

/** Підказка на екрані спостереження, коли вже видно щось схоже (≥ 3 дні даних). */
data class RiskTeaser(val startMinute: Int, val endMinute: Int, val daysLeft: Int)

data class RiskHomeState(
    val loading: Boolean = true,
    val hasAccess: Boolean = true,
    val feedCount: Int = 0,
    /** Завершені логічні доби з даними (0–7). */
    val observedDays: Int = 0,
    val enoughData: Boolean = false,
    /** Рядки теплової карти: календарні дати від найстарішої до сьогодні. */
    val dates: List<LocalDate> = emptyList(),
    val grid: Array<IntArray> = emptyArray(),
    val windows: List<RiskWindowUi> = emptyList(),
    val disabledWindows: List<RiskWindowUi> = emptyList(),
    val teaser: RiskTeaser? = null,
    /** М'який вихід за 7 діб (beta): сесій «на 5 хв» розпочато / завершено вчасно; `null`, поки сесій не було. */
    val softExit: Pair<Int, Int>? = null,
    /** Показати м'яку підказку, що без сповіщень нагадувань не буде (не частіше разу на тиждень). */
    val showNotifyHint: Boolean = false
)

/**
 * v3.0 MVP, Етап 3 (L3/L4/L6): стан головного екрана «Години ризику». Читає лише локальні дані; під час відкриття
 * запускає той самий скан, що й тижневий воркер (не частіше за [SCAN_INTERVAL_MILLIS]), щоб вікна з'явились одразу, як
 * набереться тиждень, а не через тиждень очікування воркера.
 */
class RiskHomeViewModel(private val app: TeperaApp) : ViewModel() {
    private val _state = MutableStateFlow(RiskHomeState())
    val state: StateFlow<RiskHomeState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val cfg = RiskConfig()
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            val hasAccess = app.balanceRepository.hasUsageAccess()
            val feed = app.feedAppRepository.packages()
            if (hasAccess && feed.isNotEmpty() && now - lastScanMillis > SCAN_INTERVAL_MILLIS) {
                val outcome = app.riskScanner.scan(cfg, now)
                if (outcome !is ScanOutcome.Failed) lastScanMillis = now
                if (outcome is ScanOutcome.Done && outcome.windows.any { !it.isManual }) FoundWindowsNotifier.markSeen(app)
            }
            // Розклад нагадувань залежить від вікон, планів і дозволу: оновлюємо щоразу, як людина відкрила Home.
            NudgeScheduler.reschedule(app, now)

            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val lastLogicalDay = Instant.ofEpochMilli(now).atZone(zone).minusHours(cfg.dayCutoffHour.toLong()).toLocalDate().minusDays(1)
            val logicalDays = (cfg.observationDays - 1 downTo 0).map { lastLogicalDay.minusDays(it.toLong()) }
            val covered = app.usageSlotRepository.coveredDays()
            val observed = logicalDays.count { it in covered && it.plusDays(1) in covered }
            val records = app.usageSlotRepository.readRecords(minOf(dates.first(), logicalDays.first()), today.plusDays(1), feed)
            val labels = feedLabels()

            val entities = app.riskWindowRepository.getAll()
            // Тижневий підсумок (M5): хвилини у вікні «спостереження → останні 7 діб» і «Добре» за ці 7 діб.
            val period = app.database.observationPeriodDao().getLatest()
            val baselineDays = period?.let { p ->
                val end = p.endDate?.let(LocalDate::ofEpochDay) ?: lastLogicalDay
                generateSequence(LocalDate.ofEpochDay(p.startDate)) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
            }.orEmpty()
            val baselineRecords = if (baselineDays.isEmpty()) emptyList() else
                app.usageSlotRepository.readRecords(baselineDays.first(), lastLogicalDay.plusDays(1), feed)
            val weekFromMillis = logicalDays.first().atStartOfDay(zone).toInstant().toEpochMilli()
            // Для ручних вікон повторюваність, середнє й застосунки рахуються зі слотів (збереженої статистики в них нема).
            val manualRows = if (entities.any { it.isManual }) {
                app.database.usageSlotDao().getRange(minOf(dates.first(), logicalDays.first()).toEpochDay(), today.plusDays(1).toEpochDay())
                    .filter { it.packageName in feed }
                    .map { PackageSlotUsage(LocalDate.ofEpochDay(it.date), it.slot, it.packageName, it.seconds) }
            } else emptyList()
            val allWindows = entities.map { w ->
                val hits = RiskWindowFinder.hitDays(records, covered, lastLogicalDay, w.startMinute, w.endMinute, w.dayType, cfg).toSet()
                val hitList = hits.toList()
                val manualApps = if (w.isManual) {
                    WindowExplain.minutesPerApp(manualRows, hitList, w.startMinute, w.endMinute, cfg).take(2).map { labels[it.first] ?: it.first }
                } else emptyList()
                RiskWindowUi(
                    id = w.id, startMinute = w.startMinute, endMinute = w.endMinute, dayType = w.dayType,
                    recurrenceDays = if (w.isManual) hits.size else w.recurrenceDays,
                    observedDays = if (w.isManual) logicalDays.count { it in covered && it.plusDays(1) in covered && (w.dayType == DayType.ALL || it.dayOfWeek.value <= 5) } else w.observedDays,
                    avgMinutes = if (w.isManual) (WindowMinutes.averagePerDay(records, hitList, w.startMinute, w.endMinute, w.dayType, cfg)?.roundToInt() ?: 0) else w.avgMinutes,
                    isManual = w.isManual,
                    partOfDay = HeatGrid.partOfDay(w.startMinute),
                    appLabels = if (w.isManual) manualApps else w.topApps.split(",").filter { it.isNotBlank() }.take(2).map { labels[it] ?: it },
                    plan = app.riskWindowRepository.getPlan(w.id)?.let { it.ifText to it.thenText },
                    isDisabled = w.isUserDisabled,
                    isPaused = w.isPausedAt(now),
                    baselineMinutes = WindowMinutes.averagePerDay(baselineRecords, baselineDays, w.startMinute, w.endMinute, w.dayType, cfg),
                    currentMinutes = WindowMinutes.averagePerDay(records, logicalDays, w.startMinute, w.endMinute, w.dayType, cfg),
                    okThisWeek = app.database.nudgeEventDao().countByActionForWindow(w.id, NudgeAction.OK, weekFromMillis, now + 1),
                    notTodayThisWeek = app.database.nudgeEventDao().countByActionForWindow(w.id, NudgeAction.NOT_TODAY, weekFromMillis, now + 1),
                    shiftStart = w.suggestedStartMinute, shiftEnd = w.suggestedEndMinute,
                    dots = logicalDays.map { day ->
                        when {
                            w.dayType == DayType.WEEKDAYS && day.dayOfWeek.value > 5 -> DotState.NOT_APPLICABLE
                            day in hits -> DotState.HIT
                            else -> DotState.MISS
                        }
                    }
                )
            }

            val windows = allWindows.filter { !it.isDisabled }
            val (seStarted, seOnTime, _) = PlanOverlayStore.softExitCounts(app)
            val notifyHintDue = now - app.getSharedPreferences(HINT_PREFS, Context.MODE_PRIVATE).getLong(HINT_KEY, 0L) > HINT_INTERVAL_MILLIS
            _state.update {
                it.copy(
                    softExit = if (seStarted > 0) seStarted to seOnTime else null,
                    showNotifyHint = windows.isNotEmpty() && !WeeklySummaryWorker.canNotify(app) && notifyHintDue,
                    loading = false, hasAccess = hasAccess, feedCount = feed.size, observedDays = observed,
                    enoughData = observed >= cfg.minObservedDays, dates = dates,
                    grid = HeatGrid.minutesByHour(records, dates), windows = windows,
                    disabledWindows = allWindows.filter { w -> w.isDisabled },
                    teaser = if (windows.none { !it.isManual } && observed in TEASER_MIN_DAYS until cfg.minObservedDays) {
                        teaser(records, covered, lastLogicalDay, observed, cfg)
                    } else null
                )
            }
        }
    }

    /** «Пізніше» на підказці про сповіщення: наступна — не раніше ніж за тиждень. */
    fun dismissNotifyHint() {
        app.getSharedPreferences(HINT_PREFS, Context.MODE_PRIVATE).edit().putLong(HINT_KEY, System.currentTimeMillis()).apply()
        _state.update { it.copy(showNotifyHint = false) }
    }

    fun applyShift(windowId: String) {
        viewModelScope.launch {
            app.riskWindowRepository.applyShift(windowId)
            NudgeScheduler.reschedule(app)
            refresh()
        }
    }

    fun dismissShift(windowId: String) {
        viewModelScope.launch {
            app.riskWindowRepository.dismissShift(windowId)
            refresh()
        }
    }

    private suspend fun feedLabels(): Map<String, String> =
        app.feedAppRepository.observeAll().first().associate { it.packageName to it.label }

    /** Пробний пошук на коротшому вікні з м'якшими порогами: лише для підказки «вже помітно…», не для збереження. */
    private fun teaser(
        records: List<com.serkodesign.tepera.domain.risk.SlotUsage>,
        covered: Set<LocalDate>,
        lastLogicalDay: LocalDate,
        observed: Int,
        cfg: RiskConfig
    ): RiskTeaser? {
        val relaxed = cfg.copy(
            observationDays = observed, minObservedDays = observed,
            minRecurrenceDays = maxOf(2, kotlin.math.ceil(observed * 0.6).toInt()),
            seedMinDays = 2
        )
        val c = RiskWindowFinder.find(records, covered, lastLogicalDay, relaxed).candidates.firstOrNull() ?: return null
        return RiskTeaser(c.startMinute, c.endMinute, cfg.minObservedDays - observed)
    }

    class Factory(private val app: TeperaApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RiskHomeViewModel(app) as T
    }

    private companion object {
        const val SCAN_INTERVAL_MILLIS = 15 * 60 * 1000L
        const val TEASER_MIN_DAYS = 3
        const val HINT_PREFS = "v3_notify_hint"
        const val HINT_KEY = "dismissed_at"
        const val HINT_INTERVAL_MILLIS = 7 * 24 * 3_600_000L

        /** Спільне на процес: щоб повторне відкриття Home не перераховувало все щоразу. */
        @Volatile var lastScanMillis = 0L
    }
}

package com.serkodesign.tepera.ui.risk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.ScanOutcome
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.HeatGrid
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.RiskWindowFinder
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
    val isDisabled: Boolean = false
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
    val teaser: RiskTeaser? = null
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
            }

            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val lastLogicalDay = Instant.ofEpochMilli(now).atZone(zone).minusHours(cfg.dayCutoffHour.toLong()).toLocalDate().minusDays(1)
            val logicalDays = (cfg.observationDays - 1 downTo 0).map { lastLogicalDay.minusDays(it.toLong()) }
            val covered = app.usageSlotRepository.coveredDays()
            val observed = logicalDays.count { it in covered && it.plusDays(1) in covered }
            val records = app.usageSlotRepository.readRecords(minOf(dates.first(), logicalDays.first()), today.plusDays(1), feed)
            val labels = feedLabels()

            val allWindows = app.riskWindowRepository.getAll().map { w ->
                val hits = RiskWindowFinder.hitDays(records, covered, lastLogicalDay, w.startMinute, w.endMinute, w.dayType, cfg).toSet()
                RiskWindowUi(
                    id = w.id, startMinute = w.startMinute, endMinute = w.endMinute, dayType = w.dayType,
                    recurrenceDays = w.recurrenceDays, observedDays = w.observedDays, avgMinutes = w.avgMinutes,
                    partOfDay = HeatGrid.partOfDay(w.startMinute),
                    appLabels = w.topApps.split(',').filter { it.isNotBlank() }.take(2).map { labels[it] ?: it },
                    plan = app.riskWindowRepository.getPlan(w.id)?.let { it.ifText to it.thenText },
                    isDisabled = w.isUserDisabled,
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
            _state.update {
                it.copy(
                    loading = false, hasAccess = hasAccess, feedCount = feed.size, observedDays = observed,
                    enoughData = observed >= cfg.minObservedDays, dates = dates,
                    grid = HeatGrid.minutesByHour(records, dates), windows = windows,
                    disabledWindows = allWindows.filter { w -> w.isDisabled },
                    teaser = if (windows.isEmpty() && observed in TEASER_MIN_DAYS until cfg.minObservedDays) {
                        teaser(records, covered, lastLogicalDay, observed, cfg)
                    } else null
                )
            }
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

        /** Спільне на процес: щоб повторне відкриття Home не перераховувало все щоразу. */
        @Volatile var lastScanMillis = 0L
    }
}

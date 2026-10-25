package com.serkodesign.tepera.ui.risk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.NudgeScheduler
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.HeatGrid
import com.serkodesign.tepera.domain.risk.PackageSlotUsage
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.RiskWindowFinder
import com.serkodesign.tepera.domain.risk.WindowExplain
import com.serkodesign.tepera.domain.risk.WindowMinutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Дані для M6 «Чому саме це вікно?». Усі числа рахуються заново зі слотів, а не беруться зі збереженої статистики. */
data class WhyUi(
    val windowId: String,
    val startMinute: Int,
    val endMinute: Int,
    val dayType: DayType,
    val partOfDay: PartOfDay,
    val hitCount: Int,
    val observedDays: Int,
    val avgMinutes: Int,
    /** Назва застосунку і середні хвилини на добу, до двох найбільших. */
    val apps: List<Pair<String, Int>>,
    val missedWeekdays: List<DayOfWeek>,
    val dates: List<LocalDate>,
    val grid: Array<IntArray>,
    val enabled: Boolean,
    val isManual: Boolean
)

class WindowWhyViewModel(private val app: TeperaApp, private val windowId: String) : ViewModel() {
    private val _ui = MutableStateFlow<WhyUi?>(null)
    val ui: StateFlow<WhyUi?> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val window = app.riskWindowRepository.getWindow(windowId) ?: return@launch
            val cfg = RiskConfig()
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val lastLogicalDay = Instant.ofEpochMilli(now).atZone(zone).minusHours(cfg.dayCutoffHour.toLong()).toLocalDate().minusDays(1)
            val logicalDays = (cfg.observationDays - 1 downTo 0).map { lastLogicalDay.minusDays(it.toLong()) }
            val covered = app.usageSlotRepository.coveredDays()
            val coveredLogical = logicalDays.filter { it in covered && it.plusDays(1) in covered }
            val feed = app.feedAppRepository.packages()
            val from = minOf(dates.first(), logicalDays.first())

            val records = app.usageSlotRepository.readRecords(from, today.plusDays(1), feed)
            val hits = RiskWindowFinder.hitDays(records, covered, lastLogicalDay, window.startMinute, window.endMinute, window.dayType, cfg)
            val rows = app.database.usageSlotDao().getRange(from.toEpochDay(), today.plusDays(1).toEpochDay())
                .filter { it.packageName in feed }
                .map { PackageSlotUsage(LocalDate.ofEpochDay(it.date), it.slot, it.packageName, it.seconds) }
            val labels = app.feedAppRepository.observeAll().first().associate { it.packageName to it.label }

            _ui.value = WhyUi(
                windowId = window.id, startMinute = window.startMinute, endMinute = window.endMinute,
                dayType = window.dayType, partOfDay = HeatGrid.partOfDay(window.startMinute),
                hitCount = hits.size,
                observedDays = coveredLogical.count { window.dayType == DayType.ALL || it.dayOfWeek.value <= 5 },
                avgMinutes = WindowMinutes.averagePerDay(records, hits, window.startMinute, window.endMinute, window.dayType, cfg)
                    ?.roundToInt() ?: window.avgMinutes,
                apps = WindowExplain.minutesPerApp(rows, hits, window.startMinute, window.endMinute, cfg)
                    .take(2).map { (pkg, minutes) -> (labels[pkg] ?: pkg) to minutes },
                missedWeekdays = WindowExplain.missedWeekdays(coveredLogical, hits, window.dayType),
                dates = dates, grid = HeatGrid.minutesByHour(records, dates),
                enabled = !window.isUserDisabled,
                isManual = window.isManual
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.riskWindowRepository.setUserDisabled(windowId, !enabled)
            NudgeScheduler.reschedule(app)
            _ui.update { it?.copy(enabled = enabled) }
        }
    }

    class Factory(private val app: TeperaApp, private val windowId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WindowWhyViewModel(app, windowId) as T
    }
}

package com.serkodesign.tepera.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import com.serkodesign.tepera.data.repository.SlotRecompute
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.RiskResult
import com.serkodesign.tepera.domain.risk.RiskWindowFinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

data class RiskSpikeUiState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val feed: Set<String> = emptySet(),
    val config: RiskConfig = RiskConfig(),
    val result: RiskResult? = null,
    /** Рядки — логічні доби результату, колонки — години від межі доби. */
    val heat: Array<IntArray> = emptyArray(),
    val storedDays: Int = 0,
    val coveredDays: Int = 0,
    val historyLine: String = "",
    val status: String = "",
    val busy: Boolean = false
)

/**
 * v3.0 MVP, Етап 0: службовий екран перевірки алгоритму на власних даних (лише beta). Гарантій UX не дає:
 * тексти без локалізації, параметри живуть лише в пам'яті.
 */
class RiskSpikeViewModel(private val app: TeperaApp) : ViewModel() {
    private val _state = MutableStateFlow(RiskSpikeUiState())
    val state: StateFlow<RiskSpikeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = app.installedAppsProvider.listUsedApps()
            _state.update { it.copy(apps = apps) }
            reload()
        }
    }

    fun toggleFeed(pkg: String) {
        viewModelScope.launch {
            val feed = _state.value.feed.let { if (pkg in it) it - pkg else it + pkg }
            app.settingsStore.setFeedPackages(feed)
            reload()
        }
    }

    fun setConfig(config: RiskConfig) {
        _state.update { it.copy(config = config) }
        viewModelScope.launch { reload() }
    }

    /** Перерахунок слотів з системних подій (те саме, що робить воркер) і оновлення екрана. */
    fun recompute() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "Рахую…") }
            val status = when (val r = app.usageSlotRepository.recompute()) {
                SlotRecompute.Skipped -> "Пропущено: оберіть застосунки-стрічки"
                SlotRecompute.Failed -> "Не вдалось прочитати події (або подій нема). Спробуйте ще раз"
                is SlotRecompute.Done -> "Пораховано днів: ${r.daysComputed}"
            }
            _state.update { it.copy(busy = false, status = status) }
            reload()
        }
    }

    /** Скільки днів подій реально повертає `queryEvents()` на цьому пристрої (запис у verification log). */
    fun checkHistoryDepth() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val earliest = app.usageSlotRepository.earliestEventMillis(now - 60L * DAY_MILLIS, now)
            val line = if (earliest == null) {
                "Подій немає (або нема доступу)"
            } else {
                val days = (now - earliest) / DAY_MILLIS
                "Найраніша подія: ${java.time.Instant.ofEpochMilli(earliest).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()} (≈ $days дн. тому)"
            }
            _state.update { it.copy(historyLine = line) }
        }
    }

    private suspend fun reload() {
        val repo = app.usageSlotRepository
        val feed = app.settingsStore.feedPackages.first()
        val cfg = _state.value.config
        val covered = repo.coveredDays()
        val lastLogicalDay = LocalDateTime.now().minusHours(cfg.dayCutoffHour.toLong()).toLocalDate().minusDays(1)
        val from = lastLogicalDay.minusDays(cfg.observationDays.toLong())
        val records = repo.readRecords(from, lastLogicalDay.plusDays(1), feed)
        val result = RiskWindowFinder.find(records, covered, lastLogicalDay, cfg)
        val heat = RiskWindowFinder.hourlyMinutes(records, result.days, cfg)
        _state.update {
            it.copy(
                feed = feed,
                result = result,
                heat = heat,
                coveredDays = covered.size,
                storedDays = app.database.usageSlotDao().countDaysWithRows()
            )
        }
    }

    /** Текст для вставки у форму / журнал. */
    fun report(): String {
        val s = _state.value
        val r = s.result
        return buildString {
            appendLine("Tepera v3 · Етап 0 · ${LocalDate.now()}")
            appendLine("Стрічки: ${s.feed.joinToString { p -> s.apps.firstOrNull { it.packageName == p }?.label ?: p }}")
            appendLine("Днів покрито: ${s.coveredDays}; з даними в стрічках: ${s.storedDays}; логічних діб у спостереженні: ${r?.observedDays ?: 0}")
            if (s.historyLine.isNotEmpty()) appendLine(s.historyLine)
            val c = s.config
            appendLine("Пороги: слот ≥${c.activeSlotSeconds}с, склейка ≤${c.mergeGapMinutes}хв, повтор ≥${c.minRecurrenceDays}/${c.observationDays}, середнє ≥${c.minAvgMinutes}хв")
            if (r == null || !r.enoughData) {
                appendLine("Вікон нема: замало днів даних")
            } else if (r.candidates.isEmpty()) {
                appendLine("Вікон нема")
            } else {
                r.candidates.forEach {
                    appendLine("Вікно ${formatMinute(it.startMinute)}–${formatMinute(it.endMinute)}: ${it.recurrenceDays}/${it.observedDays} днів, ≈${it.avgMinutes} хв")
                }
            }
        }
    }

    class Factory(private val app: TeperaApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RiskSpikeViewModel(app) as T
    }

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}

internal fun formatMinute(minuteOfDay: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", minuteOfDay / 60 % 24, minuteOfDay % 60)

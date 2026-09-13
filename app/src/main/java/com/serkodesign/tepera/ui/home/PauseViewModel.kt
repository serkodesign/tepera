package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.GapDetectionConfig
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.util.SleepWindowCalculator
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/** FR-D.3: картка показується лише в цих двох вікнах — паузи "сьогодні" ввечері або "вчора" вранці. */
enum class PauseCardMode { TODAY_EVENING, YESTERDAY_MORNING }

data class PauseUiGap(val id: String, val startTime: Long, val durationMinutes: Int)

data class PauseCardUiState(
    val visible: Boolean = false,
    val mode: PauseCardMode = PauseCardMode.TODAY_EVENING,
    val gaps: List<PauseUiGap> = emptyList()
)

/**
 * FR-D.1–D.5 (SRS v2.6): детекція й позначення пауз. Сканування (queryEvents) запускається
 * on-demand при відкритті Home, а не фоновою задачею (узгоджено зі стейкхолдером) — і так
 * викликається лише у дозволеному вікні опитування (FR-D.3), тож фонового сканування поза
 * ним не потрібно: "жодних push" (FR-D.3) уже виконано самою відсутністю тригера поза вікном.
 * **FR-D.6 (SRS v2.8):** ця ViewModel більше НЕ обчислює "твій день з телефоном" (межі
 * першої/останньої сесії) — прибрано як окрема метрика. Час останньої сесії тепер живе в
 * `PauseRepository.lastPhoneUseForShiftedDay()`/`LastPhoneUseEstimateViewModel` (FR-D.7, T-10),
 * межі дня — в тепловому патерні (`PatternViewModel`).
 */
class PauseViewModel(
    private val pauseRepository: PauseRepository,
    private val balanceRepository: BalanceRepository,
    private val activityRepository: ActivityRepository,
    private val settingsStore: SettingsStore,
    private val sleepWindowRepository: SleepWindowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PauseCardUiState())
    val uiState: StateFlow<PauseCardUiState> = _uiState.asStateFlow()

    // "Закрити картку, якщо прочитав" (за прямим запитом користувача, не в SRS) — ключ анкера
    // конкретного вікна опитування (todayDayStart/yesterdayDayStart), НЕ просто дата: сьогоднішній
    // вечір і вчорашній ранок стосуються різних даних (FR-D.3), тож закриття одного не повинно
    // ховати інший, навіть якщо обидва трапляються в межах одного календарного дня.
    private var currentDismissKey: Long = -1L

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) {
                _uiState.value = PauseCardUiState()
                return@launch
            }

            val sleepWindows = sleepWindowRepository.getEnabledWindows()
            // T-11: пресет читається наживо щоразу — зміна в Налаштуваннях діє з наступного
            // refresh(), без перезапуску застосунку.
            val config = GapDetectionConfig.forSensitivity(settingsStore.gapSensitivity.first())
            val todayMidnightForCoverageCheck = startOfTodayMillis()
            // FR-D.4: відоме обмеження нічного графіка — якщо налаштоване(і) вікно(а) сну
            // (T-12) ПОВНІСТЮ покривають вранішню половину вікна опитування (00:00–10:00),
            // картка мовчить, паузи лишаються в БД.
            if (SleepWindowCalculator.isFullyCovered(sleepWindows, todayMidnightForCoverageCheck, todayMidnightForCoverageCheck + 10 * 60 * 60_000L)) {
                _uiState.value = PauseCardUiState()
                return@launch
            }

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val mode = when {
                hour in 22..23 -> PauseCardMode.TODAY_EVENING
                hour in 0..9 -> PauseCardMode.YESTERDAY_MORNING
                else -> null
            }
            if (mode == null) {
                _uiState.value = PauseCardUiState()
                return@launch
            }

            val todayMidnight = startOfTodayMillis()
            val todayDayStart = balanceRepository.calculateDayStartMillis(sleepWindows, todayMidnight)

            when (mode) {
                PauseCardMode.TODAY_EVENING -> {
                    currentDismissKey = todayDayStart
                    if (settingsStore.pauseCardDismissedKey.first() == todayDayStart) {
                        _uiState.value = PauseCardUiState()
                        return@launch
                    }
                    val now = System.currentTimeMillis()
                    val scan = pauseRepository.scan(todayDayStart, now, sleepWindows, config)
                    pauseRepository.persist(todayDayStart, now, scan.gaps)
                    val gaps = pauseRepository.getUnresolvedGaps(todayDayStart, now)
                    _uiState.value = PauseCardUiState(
                        visible = gaps.isNotEmpty(),
                        mode = mode,
                        gaps = gaps.map { PauseUiGap(it.id, it.startTime, it.durationMinutes) }
                    )
                }
                PauseCardMode.YESTERDAY_MORNING -> {
                    val yesterdayMidnight = todayMidnight - DAY_MILLIS
                    val yesterdayDayStart = balanceRepository.calculateDayStartMillis(
                        sleepWindows,
                        yesterdayMidnight,
                        todayDayStart
                    )
                    // Нічого не знайдено в межах учорашньої доби (напр. свіжовстановлений застосунок).
                    if (yesterdayDayStart >= todayDayStart) {
                        _uiState.value = PauseCardUiState()
                        return@launch
                    }
                    currentDismissKey = yesterdayDayStart
                    if (settingsStore.pauseCardDismissedKey.first() == yesterdayDayStart) {
                        _uiState.value = PauseCardUiState()
                        return@launch
                    }
                    val scan = pauseRepository.scan(yesterdayDayStart, todayDayStart, sleepWindows, config)
                    pauseRepository.persist(yesterdayDayStart, todayDayStart, scan.gaps)
                    val gaps = pauseRepository.getUnresolvedGaps(yesterdayDayStart, todayDayStart)
                    _uiState.value = PauseCardUiState(
                        visible = gaps.isNotEmpty(),
                        mode = mode,
                        gaps = gaps.map { PauseUiGap(it.id, it.startTime, it.durationMinutes) }
                    )
                }
            }
        }
    }

    /**
     * Закрити ВСЮ картку (на відміну від [dismissGap] — пропуск однієї паузи назавжди) до
     * наступного вікна опитування (FR-D.3) — `currentDismissKey` завжди відповідає останньому
     * [refresh]. Непозначені паузи лишаються в БД, доступні пізніше через Історію (FR-D.5).
     */
    fun dismissCard() {
        viewModelScope.launch {
            settingsStore.setPauseCardDismissedKey(currentDismissKey)
            _uiState.value = PauseCardUiState()
        }
    }

    /** FR-D.5: тап по паузі → вибір категорії → звичайний запис (forceOverwrite, як і тап-таймер — швидка дія без overlap-діалогу). */
    fun labelGap(gap: PauseUiGap, categoryId: String) {
        viewModelScope.launch {
            val entry = ActivityEntryEntity(
                categoryId = categoryId,
                startTime = gap.startTime,
                durationMinutes = gap.durationMinutes,
                source = EntrySource.GAP_LABELED
            )
            activityRepository.addEntry(entry, forceOverwrite = true)
            pauseRepository.markLabeled(gap.id, entry.id)
            _uiState.value = _uiState.value.let { state ->
                state.copy(gaps = state.gaps.filterNot { it.id == gap.id })
            }
        }
    }

    fun dismissGap(gap: PauseUiGap) {
        viewModelScope.launch {
            pauseRepository.dismiss(gap.id)
            _uiState.value = _uiState.value.let { state ->
                state.copy(gaps = state.gaps.filterNot { it.id == gap.id })
            }
        }
    }

    class Factory(
        private val pauseRepository: PauseRepository,
        private val balanceRepository: BalanceRepository,
        private val activityRepository: ActivityRepository,
        private val settingsStore: SettingsStore,
        private val sleepWindowRepository: SleepWindowRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PauseViewModel(pauseRepository, balanceRepository, activityRepository, settingsStore, sleepWindowRepository) as T
    }
}

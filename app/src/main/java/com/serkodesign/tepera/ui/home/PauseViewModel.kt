package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.PauseRepository
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

/** FR-D.6/D.7: "твій день з телефоном тривав X — з HH:MM до HH:MM" — лише для вчорашньої картки. */
data class DayWithPhoneBounds(val startMillis: Long, val endMillis: Long, val durationMinutes: Int)

data class PauseCardUiState(
    val visible: Boolean = false,
    val mode: PauseCardMode = PauseCardMode.TODAY_EVENING,
    val dayBounds: DayWithPhoneBounds? = null,
    val gaps: List<PauseUiGap> = emptyList()
)

/**
 * FR-D.1–D.7 (SRS v2.6): детекція й позначення пауз. Сканування (queryEvents) запускається
 * on-demand при відкритті Home, а не фоновою задачею (узгоджено зі стейкхолдером) — і так
 * викликається лише у дозволеному вікні опитування (FR-D.3), тож фонового сканування поза
 * ним не потрібно: "жодних push" (FR-D.3) уже виконано самою відсутністю тригера поза вікном.
 */
class PauseViewModel(
    private val pauseRepository: PauseRepository,
    private val balanceRepository: BalanceRepository,
    private val activityRepository: ActivityRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PauseCardUiState())
    val uiState: StateFlow<PauseCardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) {
                _uiState.value = PauseCardUiState()
                return@launch
            }

            val sleepWindowEndHour = settingsStore.sleepWindowEndHour.first()
            // FR-D.4: відоме обмеження нічного графіка — якщо налаштоване вікно сну (00:00 до
            // sleepWindowEndHour) уже саме по собі сягає 10:00, воно повністю накриває вранішню
            // половину вікна опитування (00:00–10:00) — картка мовчить, паузи лишаються в БД.
            if (sleepWindowEndHour >= 10) {
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
            val todayDayStart = balanceRepository.calculateDayStartMillis(sleepWindowEndHour, todayMidnight)

            when (mode) {
                PauseCardMode.TODAY_EVENING -> {
                    val now = System.currentTimeMillis()
                    val scan = pauseRepository.scan(todayDayStart, now)
                    pauseRepository.persist(scan.gaps)
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
                        sleepWindowEndHour,
                        yesterdayMidnight,
                        todayDayStart
                    )
                    // Нічого не знайдено в межах учорашньої доби (напр. свіжовстановлений застосунок).
                    if (yesterdayDayStart >= todayDayStart) {
                        _uiState.value = PauseCardUiState()
                        return@launch
                    }
                    val scan = pauseRepository.scan(yesterdayDayStart, todayDayStart)
                    pauseRepository.persist(scan.gaps)
                    val gaps = pauseRepository.getUnresolvedGaps(yesterdayDayStart, todayDayStart)
                    val bounds = if (scan.firstSessionStart != null && scan.lastSessionEnd != null) {
                        DayWithPhoneBounds(
                            startMillis = scan.firstSessionStart,
                            endMillis = scan.lastSessionEnd,
                            durationMinutes = ((scan.lastSessionEnd - scan.firstSessionStart) / 60_000L).toInt()
                        )
                    } else null
                    _uiState.value = PauseCardUiState(
                        visible = gaps.isNotEmpty() || bounds != null,
                        mode = mode,
                        dayBounds = bounds,
                        gaps = gaps.map { PauseUiGap(it.id, it.startTime, it.durationMinutes) }
                    )
                }
            }
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
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PauseViewModel(pauseRepository, balanceRepository, activityRepository, settingsStore) as T
    }
}

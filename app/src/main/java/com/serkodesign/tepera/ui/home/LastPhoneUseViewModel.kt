package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val CUTOFF_HOUR = 2 // FR-D.7a

data class LastPhoneUseUiState(
    val visible: Boolean = false,
    val lastUseMillis: Long? = null
)

/**
 * FR-D.7 (SRS v2.8): "востаннє брав телефон о HH:MM" — заміна прибраної метрики "твій день з
 * телефоном" (FR-D.6, колишній `DayWithPhoneBounds` у PauseViewModel). Показує лише ЧАС, без
 * висновків і коментарів (FR-D.7b) — обґрунтування: вчасно відкласти телефон перед сном — одна
 * з ключових практик цифрового детоксу, і сам факт говорить голосніше за будь-яку пораду.
 *
 * **Метрика вчорашнього дня** (FR-D.7): остання сесія відома лише постфактум, тож завжди
 * показуємо завершене вікно, не поточне. Межа належності — **02:00, НЕ вікно сну** (FR-D.7a):
 * сесія о 01:42 належить учора, о 02:30 — уже сьогодні. Це окремий, фіксований поріг, свідомо
 * не прив'язаний до `SettingsStore.sleepWindowEndHour` (той визначає межу ПОЧАТКУ дня й
 * детекцію пауз, а не межу ВІДНЕСЕННЯ пізньої сесії до попереднього/наступного дня).
 *
 * Перевикористовує `PauseRepository.scan()` (той самий алгоритм, що для детекції пауз, БЕЗ
 * Exclusion List — "торкався телефону взагалі", не категоризація залежності, FR-D.2) лише
 * заради `lastSessionEnd`, без виклику `persist()` — тут не потрібно зберігати паузи вдруге.
 */
class LastPhoneUseViewModel(
    private val pauseRepository: PauseRepository,
    private val balanceRepository: BalanceRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LastPhoneUseUiState())
    val uiState: StateFlow<LastPhoneUseUiState> = _uiState.asStateFlow()

    private var currentDismissKey: Long = -1L

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) {
                _uiState.value = LastPhoneUseUiState()
                return@launch
            }

            val windowEnd = shiftedDayBoundary(System.currentTimeMillis())
            val windowStart = windowEnd - DAY_MILLIS
            currentDismissKey = windowEnd
            if (settingsStore.lastPhoneUseCardDismissedKey.first() == windowEnd) {
                _uiState.value = LastPhoneUseUiState()
                return@launch
            }

            val lastUse = pauseRepository.scan(windowStart, windowEnd).lastSessionEnd
            _uiState.value = LastPhoneUseUiState(visible = lastUse != null, lastUseMillis = lastUse)
        }
    }

    /** Закрити картку до наступного зсуву 02:00-межі (FR-D.7a) — `currentDismissKey` відповідає останньому [refresh]. */
    fun dismiss() {
        viewModelScope.launch {
            settingsStore.setLastPhoneUseCardDismissedKey(currentDismissKey)
            _uiState.value = LastPhoneUseUiState()
        }
    }

    /** Найближча межа 02:00 у минулому (включно) відносно [nowMillis] — FR-D.7a. */
    private fun shiftedDayBoundary(nowMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowMillis
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        calendar.set(Calendar.HOUR_OF_DAY, CUTOFF_HOUR)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (hour < CUTOFF_HOUR) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return calendar.timeInMillis
    }

    class Factory(
        private val pauseRepository: PauseRepository,
        private val balanceRepository: BalanceRepository,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LastPhoneUseViewModel(pauseRepository, balanceRepository, settingsStore) as T
    }
}

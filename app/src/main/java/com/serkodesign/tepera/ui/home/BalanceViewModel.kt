package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Одна категорія, за якою сьогодні є хоч один запис — "Ти відмітив" (FR-3.3), розбитий по категоріях. */
data class CategorySegment(val category: CategoryEntity, val minutes: Int)

/** hasUsageAccess == null означає, що перевірка ще не завершилась (перший рендер). */
data class BalanceUiState(
    val hasUsageAccess: Boolean? = null,
    val onlineMinutes: Int = 0,
    val categorySegments: List<CategorySegment> = emptyList(),
    val restOfDayMinutes: Int = 0,
    val dayLengthMinutes: Int = 0,
    val daySpanMinutes: Int = 1,
    val targetMinutes: Int = 180,
    val denominatorMinutes: Int = 180,
    /** Точка старту дня (перше суттєве розблокування) — для плашки "Перше розблокування" на Home. */
    val dayStartMillis: Long = 0L
)

/**
 * FR-3.1–3.12 (SRS v2.5): "Структура доби" — три нейтральні складові: Online (автоматично,
 * від точки старту дня), категорії "Ти відмітив" (ручні записи, розбиті по кожній категорії
 * з ненульовим часом сьогодні — FR-5.1) і "Решта дня" (залишок, FR-3.4). Свідомо НЕ протиставлення
 * Online/Offline на одній шкалі (FR-3.8) — три доданки одного цілого (довжини дня).
 */
class BalanceViewModel(
    private val balanceRepository: BalanceRepository,
    activityRepository: ActivityRepository,
    categoryRepository: CategoryRepository,
    private val settingsStore: SettingsStore,
    private val sleepWindowRepository: SleepWindowRepository
) : ViewModel() {

    private val hasUsageAccess = MutableStateFlow<Boolean?>(null)
    private val onlineMinutes = MutableStateFlow(0)
    private val dayStartMillis = MutableStateFlow(System.currentTimeMillis())
    // T-12: вікна сну (0-2 ввімкнені) міняються рідко (лише з Налаштувань) — окремий MutableStateFlow
    // замість settingsStore.observeWindows(), щоб лишатись у парі з dayStartMillis, обчисленим
    // для ТИХ САМИХ вікон у refresh() (а не для щойно зміненого набору, поки ще не перерахований).
    private val sleepWindows = MutableStateFlow<List<SleepWindowEntity>>(emptyList())

    // combine() підтримує щонайбільше 5 потоків з типізованою лямбдою — dayStart і windows
    // завжди рахуються разом у refresh(), тож об'єднані в один потік заздалегідь.
    private val dayStartWithWindows = combine(dayStartMillis, sleepWindows) { dayStart, windows -> dayStart to windows }

    private val categoriesAndTarget = combine(
        categoryRepository.observeAllCategories(),
        settingsStore.targetMinutes
    ) { categories, target -> categories to target }

    val uiState: StateFlow<BalanceUiState> = combine(
        hasUsageAccess,
        onlineMinutes,
        dayStartWithWindows,
        activityRepository.observeEntriesInRange(startOfTodayMillis(), Long.MAX_VALUE),
        categoriesAndTarget
    ) { access, online, (dayStart, windows), entries, (categories, target) ->
        val minutesByCategory = entries.groupBy { it.categoryId }
            .mapValues { (_, categoryEntries) -> categoryEntries.sumOf { it.durationMinutes } }
        // FR-5.1: лише категорії, у яких сьогодні є хоч один запис — динамічна легенда
        // (референсний макет SRS 4.4 показує лише реально залоговані сьогодні категорії, не
        // фіксовану п'ятірку).
        val segments = categories
            .filter { (minutesByCategory[it.id] ?: 0) > 0 }
            .sortedBy { it.sortOrder }
            .map { CategorySegment(it, minutesByCategory.getValue(it.id)) }

        val dayLength = balanceRepository.calculateDayLengthMinutes(dayStart, windows)
        // Шкала охоплює весь день — від пробудження до 00:00 (за запитом користувача), але
        // "Офлайн-життя" (раніше "Решта дня") заповнює лише ДО позначки "Now", не далі: те, що
        // ще не сталося (від "зараз" до півночі), лишається порожньою ділянкою шкали — її додає
        // DayStructureBar/GlanceDayStructureBar окремим незабарвленим сегментом, порівнюючи
        // dayLengthMinutes із daySpanMinutes нижче.
        val daySpan = balanceRepository.calculateDaySpanMinutes(dayStart, windows)
        val loggedMinutes = segments.sumOf { it.minutes }
        // FR-3.4: "Офлайн-життя" — залишок часу, що вже МИНУВ (не Online, не запис), НІКОЛИ
        // від'ємний, навіть якщо Online+записи вже перевищили довжину дня (напр. запис заднім числом).
        val restOfDay = (dayLength - online - loggedMinutes).coerceAtLeast(0)

        BalanceUiState(
            hasUsageAccess = access,
            onlineMinutes = online,
            categorySegments = segments,
            restOfDayMinutes = restOfDay,
            dayLengthMinutes = dayLength,
            daySpanMinutes = daySpan,
            targetMinutes = target,
            denominatorMinutes = balanceRepository.calculateDenominatorMinutes(dayStart, windows),
            dayStartMillis = dayStart
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BalanceUiState())

    init {
        refresh()
    }

    /** Викликається при вході на Home і при поверненні з системних Налаштувань (LifecycleResumeEffect). */
    fun refresh() {
        viewModelScope.launch {
            val access = balanceRepository.hasUsageAccess()
            hasUsageAccess.value = access
            // FR-3.5: точка старту дня потрібна незалежно від доступу до статистики — вона
            // визначає й довжину дня (FR-3.7), яку показуємо навіть без наданого дозволу.
            val windows = sleepWindowRepository.getEnabledWindows()
            val dayStart = balanceRepository.calculateDayStartMillis(windows)
            // Порядок важливий: sleepWindows виставляється ДО dayStartMillis (обидва —
            // MutableStateFlow, combine() емітить на кожну зміну окремо), щоб проміжний стан
            // dayStartWithWindows не встиг зіставити НОВИЙ dayStart зі СТАРИМ набором вікон.
            sleepWindows.value = windows
            dayStartMillis.value = dayStart
            onlineMinutes.value = if (access) balanceRepository.getOnlineMinutesToday(dayStart) else 0
        }
    }

    class Factory(
        private val balanceRepository: BalanceRepository,
        private val activityRepository: ActivityRepository,
        private val categoryRepository: CategoryRepository,
        private val settingsStore: SettingsStore,
        private val sleepWindowRepository: SleepWindowRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BalanceViewModel(balanceRepository, activityRepository, categoryRepository, settingsStore, sleepWindowRepository) as T
    }
}

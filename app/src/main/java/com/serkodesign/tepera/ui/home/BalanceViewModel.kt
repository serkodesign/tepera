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
import com.serkodesign.tepera.util.TimeSpan
import com.serkodesign.tepera.util.logicalDayStartFlow
import com.serkodesign.tepera.util.offlineUnloggedMinutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Одна категорія, за якою сьогодні є хоч один запис — "Ти відмітив" (FR-3.3), розбитий по категоріях. */
data class CategorySegment(val category: CategoryEntity, val minutes: Int)

/**
 * Online від точки старту дня до "зараз": сума хвилин (як і раніше) і самі інтервали — для "Офлайн-життя" за
 * об'єднанням з ручними записами (перекриття Online із записом не віднімається двічі).
 */
data class OnlineSnapshot(val minutes: Int, val intervals: List<TimeSpan>)

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
@OptIn(ExperimentalCoroutinesApi::class)
class BalanceViewModel(
    private val balanceRepository: BalanceRepository,
    activityRepository: ActivityRepository,
    categoryRepository: CategoryRepository,
    private val settingsStore: SettingsStore,
    private val sleepWindowRepository: SleepWindowRepository
) : ViewModel() {

    private val hasUsageAccess = MutableStateFlow<Boolean?>(null)
    private val onlineSnapshot = MutableStateFlow(OnlineSnapshot(0, emptyList()))
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
        onlineSnapshot,
        dayStartWithWindows,
        logicalDayStartFlow().flatMapLatest { activityRepository.observeEntriesInRange(it, Long.MAX_VALUE) },
        categoriesAndTarget
    ) { access, onlineSnap, (dayStart, windows), entries, (categories, target) ->
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
        // "Офлайн-життя" за об'єднанням (те саме, що "Офлайн" у Статистиці): неспана доба мінус час, зайнятий
        // Online АБО записом. Перекриття (аудіокнига на прогулянці, запис поверх Online, записи різних категорій
        // між собою) рахується один раз — раніше "довжина дня − Online − сума записів" віднімало його двічі.
        val restOfDay = offlineUnloggedMinutes(dayStart, System.currentTimeMillis(), onlineSnap.intervals, entries, windows)

        BalanceUiState(
            hasUsageAccess = access,
            onlineMinutes = onlineSnap.minutes,
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
        // Настала нова доба, поки Home відкритий чи процес живий: точка старту дня й Online-хвилини
        // лишились би вчорашніми (записи оновлюються самі через logicalDayStartFlow вище).
        viewModelScope.launch {
            logicalDayStartFlow().drop(1).collect { refresh() }
        }
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
            val now = System.currentTimeMillis()
            onlineSnapshot.value = if (access) {
                OnlineSnapshot(
                    balanceRepository.getOnlineMinutes(dayStart, now),
                    balanceRepository.getOnlineIntervals(dayStart, now)
                )
            } else {
                OnlineSnapshot(0, emptyList())
            }
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

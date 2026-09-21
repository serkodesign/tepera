package com.serkodesign.tepera.ui.stats

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.util.SleepWindowCalculator


import com.serkodesign.tepera.data.repository.GapCandidate
import com.serkodesign.tepera.widget.DAILY_GRID_SLOT_COUNT
import com.serkodesign.tepera.widget.DailyGridSlot
import com.serkodesign.tepera.widget.calculateDailyGridSlots

private const val DAY_MILLIS = 24 * 60 * 60_000L
private const val SLOT_MILLIS = 30 * 60_000L

/** Мінімум днів із даними, щоб "типовий день" узагалі показувався (менше — це не медіана, а випадковість). */
const val MIN_DAYS_FOR_TYPICAL = 3

/**
 * Клітинка хронології вчорашньої доби (Статистика → День). Окремий від віджетного [DailyGridSlot]
 * тип, бо тут є ще "Пауза" — виявлена пауза без телефону (FR-D.1), якої віджет не показує.
 */
sealed class TimelineSlot {
    /** Поза неспаним днем: до точки старту дня або у вікні сну (T-12) — нейтрально, не "порожнеча" й не офлайн. */
    data object BeforeStart : TimelineSlot()
    data class Category(val categoryId: String) : TimelineSlot()
    data object Online : TimelineSlot()
    /**
     * Офлайн, який не залоговано: день уже почався, Online нема, запису нема (те саме "Офлайн-життя",
     * що на Home — залишок, а не прогалина). Пауза без телефону — його підвид, виділений окремо.
     */
    data object Offline : TimelineSlot()
    /** Виявлена й не зайнята записом/Online пауза без телефону. */
    data object Pause : TimelineSlot()
    /** Невідомо: нема доступу до статистики (Online не знаємо) або день не починався. */
    data object Blank : TimelineSlot()
}

/**
 * 48 клітинок по 30 хв від календарної півночі [dayStartMillis] за минулу добу. Пріоритет у межах
 * клітинки: запис категорії > Online > пауза без телефону > офлайн без запису > невідомо (запис
 * категорії, зокрема позначена пауза, важливіший за автоматику).
 * [firstUseMillis] — точка старту дня (`null`, коли в добі не було жодного суттєвого розблокування):
 * клітинки до неї — [TimelineSlot.BeforeStart]. [onlineKnown] — чи маємо Online (доступ до статистики):
 * лише тоді порожня клітинка після старту дня — справжній [TimelineSlot.Offline], інакше [TimelineSlot.Blank].
 */
fun buildDayTimeline(
    dayStartMillis: Long,
    firstUseMillis: Long?,
    entries: List<ActivityEntryEntity>,
    onlineMinutesPerSlot: IntArray,
    gaps: List<GapCandidate>,
    onlineKnown: Boolean = true,
    sleepWindows: List<SleepWindowEntity> = emptyList()
): List<TimelineSlot> {
    val base = calculateDailyGridSlots(
        calendarMidnightMillis = dayStartMillis,
        dayStartMillis = firstUseMillis ?: dayStartMillis,
        nowMillis = dayStartMillis + DAY_MILLIS, // доба завершена — "майбутнього" нема
        entries = entries,
        onlineMinutesPerSlot = onlineMinutesPerSlot
    )
    return base.mapIndexed { index, slot ->
        when (slot) {
            is DailyGridSlot.PreUnlock -> TimelineSlot.BeforeStart
            is DailyGridSlot.Category -> TimelineSlot.Category(slot.categoryId)
            is DailyGridSlot.Online -> TimelineSlot.Online
            is DailyGridSlot.Blank -> {
                val mid = dayStartMillis + index * SLOT_MILLIS + SLOT_MILLIS / 2
                val inGap = gaps.any { mid >= it.startTime && mid < it.startTime + it.durationMinutes * 60_000L }
                when {
                    // Порожня клітинка у вікні сну — це сон, а не "офлайн без запису".
                    SleepWindowCalculator.isInsideWindow(sleepWindows, mid) -> TimelineSlot.BeforeStart
                    inGap -> TimelineSlot.Pause
                    onlineKnown && firstUseMillis != null -> TimelineSlot.Offline
                    else -> TimelineSlot.Blank
                }
            }
        }
    }
}

/** Медіана (для парної кількості — середнє двох центральних); `null` для порожнього списку. */
fun medianOf(values: List<Int>): Int? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
}

/** Підсумок вчорашніх пауз: кількість і найдовша (із категорією, якщо її позначили). */
data class PauseSummary(val count: Int, val longest: GapCandidate, val longestCategory: CategoryEntity?)

/** Online вчора проти власної типової доби — лише з власною історією, ніколи з "нормою" (FR-P.5). */
data class OnlineComparison(val yesterdayMinutes: Int, val typicalMinutes: Int, val daysCount: Int)

data class DayDetailsUiState(
    /** Календарна північ вчорашньої доби. */
    val dayStartMillis: Long = 0L,
    val timeline: List<TimelineSlot> = List(DAILY_GRID_SLOT_COUNT) { TimelineSlot.Blank },
    val categoriesById: Map<String, CategoryEntity> = emptyMap(),
    val pauses: PauseSummary? = null,
    val comparison: OnlineComparison? = null,
    /** Точка старту дня вчора; `null` — жодного суттєвого розблокування. */
    val firstUseMillis: Long? = null,
    val lastUseMillis: Long? = null,
    /** `null` — нема доступу або API < 28 (не "0", той був би хибним фактом). */
    val unlockCount: Int? = null
) {
    /** Є що показати в хронології: хоч одна клітинка з даними. */
    val hasTimelineData: Boolean
        get() = timeline.any { it !is TimelineSlot.Blank && it !is TimelineSlot.BeforeStart }
}

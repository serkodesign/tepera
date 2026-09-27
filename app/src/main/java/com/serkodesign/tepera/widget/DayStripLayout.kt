package com.serkodesign.tepera.widget

/** Один сегмент смуги дня (пауза) — частки `[0,1]` ширини від початку дня; [categoryId] `null` = неназвана. */
data class DayStripSegment(val startFraction: Float, val endFraction: Float, val categoryId: String?)

/** Одна позначка використання (тонка лінія на смузі) — частки `[0,1]` ширини. */
data class DayStripUsageTick(val startFraction: Float, val endFraction: Float)

data class DayStripLayout(val gapSegments: List<DayStripSegment>, val usageTicks: List<DayStripUsageTick>)

/**
 * W-3 (`CLAUDE-CODE-TASK-widgets.md`): перетворює [DaySnapshot] на нормалізовані частки `[0,1]`
 * для смуги дня — незалежно від пікселів/розміру віджета, той самий принцип, що
 * `DailyGridCalculator` (чисте обчислення, тестується без пристрою й без Glance). "Перерви —
 * заповнені ділянки" → [DayStripSegment], "використання — тонка лінія" → [DayStripUsageTick].
 * Паузи/інтервали поза `[dayStartMillis, dayEndMillis)` обрізаються по межах; ті, що після
 * обрізання мають нульову чи від'ємну тривалість, відкидаються (захист від хибних вхідних даних,
 * не очікувана поведінка викликача).
 */
fun buildDayStripLayout(snapshot: DaySnapshot): DayStripLayout {
    val totalMillis = (snapshot.dayEndMillis - snapshot.dayStartMillis).toFloat()
    if (totalMillis <= 0f) return DayStripLayout(emptyList(), emptyList())

    fun fraction(millis: Long): Float = ((millis - snapshot.dayStartMillis) / totalMillis).coerceIn(0f, 1f)

    val segments = snapshot.gaps.mapNotNull { gap ->
        val start = fraction(gap.startMillis)
        val end = fraction(gap.endMillis)
        if (end <= start) null else DayStripSegment(start, end, gap.categoryId)
    }
    val ticks = snapshot.onlineIntervals.mapNotNull { interval ->
        val start = fraction(interval.start)
        val end = fraction(interval.end)
        if (end <= start) null else DayStripUsageTick(start, end)
    }
    return DayStripLayout(segments, ticks)
}

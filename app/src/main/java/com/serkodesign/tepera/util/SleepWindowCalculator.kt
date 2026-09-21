package com.serkodesign.tepera.util

import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import java.util.Calendar

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/**
 * T-12 (tepera-dev-spec.md): чиста арифметика "вікна сну" — годин доби, виключених з розрахунку
 * структури доби, детекції пауз і (пізніше) лічильника розблокувань. Кожне ввімкнене вікно
 * повторюється щодня за [SleepWindowEntity.startMinuteOfDay]/[SleepWindowEntity.endMinuteOfDay];
 * [SleepWindowEntity.endMinuteOfDay] <= [SleepWindowEntity.startMinuteOfDay] означає перетин
 * півночі. Не залежить від Context/Room — легко тестується без пристрою.
 */
object SleepWindowCalculator {

    /** Сумарна кількість хвилин з [fromMillis, toMillis), що потрапляють у ХОЧ ОДНЕ ввімкнене вікно. */
    fun minutesInWindows(windows: List<SleepWindowEntity>, fromMillis: Long, toMillis: Long): Int {
        if (fromMillis >= toMillis) return 0
        val intervals = windows.filter { it.enabled }
            .flatMap { windowIntervalsIn(it, fromMillis, toMillis) }
        return (mergedMillis(intervals) / 60_000L).toInt()
    }

    /** Чи перетинається [startMillis, endMillis) хоч частково з якимось ввімкненим вікном. */
    fun overlapsWindow(windows: List<SleepWindowEntity>, startMillis: Long, endMillis: Long): Boolean =
        minutesInWindows(windows, startMillis, endMillis) > 0

    /** Чи [fromMillis, toMillis) ПОВНІСТЮ вкрите ввімкненими вікнами (FR-D.4-подібна перевірка). */
    fun isFullyCovered(windows: List<SleepWindowEntity>, fromMillis: Long, toMillis: Long): Boolean {
        if (fromMillis >= toMillis) return true
        return minutesInWindows(windows, fromMillis, toMillis) >= ((toMillis - fromMillis) / 60_000L).toInt()
    }

    /**
     * Чи момент [millis] лежить у ввімкненому вікні. Це точкова перевірка: через [minutesInWindows]
     * вона б завжди давала false (інтервал у 1 мс округлюється вниз до 0 хв) — тому дивимось на
     * самі інтервали.
     */
    fun isInsideWindow(windows: List<SleepWindowEntity>, millis: Long): Boolean =
        windows.any { it.enabled && windowIntervalsIn(it, millis, millis + 1).isNotEmpty() }

    /** [window], повторене на кожен календарний день, що перетинає [fromMillis, toMillis), обрізане по межах. */
    private fun windowIntervalsIn(window: SleepWindowEntity, fromMillis: Long, toMillis: Long): List<Pair<Long, Long>> {
        if (window.startMinuteOfDay == window.endMinuteOfDay) return emptyList()
        val result = mutableListOf<Pair<Long, Long>>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = fromMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // На день раніше — щоб зловити вікно, яке почалось учора й перетнуло північ у [fromMillis, toMillis).
        cal.add(Calendar.DAY_OF_YEAR, -1)
        while (cal.timeInMillis < toMillis) {
            val dayStart = cal.timeInMillis
            val winStart = dayStart + window.startMinuteOfDay * 60_000L
            val spansMidnight = window.endMinuteOfDay <= window.startMinuteOfDay
            val winEnd = dayStart + window.endMinuteOfDay * 60_000L + if (spansMidnight) DAY_MILLIS else 0L
            val overlapStart = maxOf(winStart, fromMillis)
            val overlapEnd = minOf(winEnd, toMillis)
            if (overlapStart < overlapEnd) result += overlapStart to overlapEnd
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    /** Сума мілісекунд у списку інтервалів після злиття перекриттів (вікно 1 і вікно 2 можуть перекриватись). */
    private fun mergedMillis(intervals: List<Pair<Long, Long>>): Long {
        if (intervals.isEmpty()) return 0L
        val sorted = intervals.sortedBy { it.first }
        var total = 0L
        var curStart = sorted[0].first
        var curEnd = sorted[0].second
        for (i in 1 until sorted.size) {
            val (s, e) = sorted[i]
            if (s <= curEnd) {
                curEnd = maxOf(curEnd, e)
            } else {
                total += curEnd - curStart
                curStart = s
                curEnd = e
            }
        }
        total += curEnd - curStart
        return total
    }
}

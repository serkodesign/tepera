package com.serkodesign.tepera.util

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity

/**
 * "Офлайн, який не залоговано" за об'єднанням: неспана частина [windowStart, windowEnd) (без вікон сну)
 * мінус час, зайнятий Online АБО записом. Перекриття (запис поверх Online, записи різних категорій між
 * собою) рахуються один раз — на відміну від залишку "доба − Online − сума записів", який віднімав їх двічі.
 * Зайнятий час у вікні сну не віднімається (вікно й так не входить в "неспану" частину).
 */
fun offlineUnloggedMinutes(
    windowStart: Long,
    windowEnd: Long,
    onlineIntervals: List<TimeSpan>,
    entries: List<ActivityEntryEntity>,
    sleepWindows: List<SleepWindowEntity>
): Int {
    if (windowEnd <= windowStart) return 0
    val awakeMinutes = ((windowEnd - windowStart) / 60_000L).toInt() -
        SleepWindowCalculator.minutesInWindows(sleepWindows, windowStart, windowEnd)

    val busy = mergeTimeSpans(
        (onlineIntervals + entries.map { TimeSpan(it.startTime, it.startTime + it.durationMinutes * 60_000L) })
            .map { TimeSpan(maxOf(it.start, windowStart), minOf(it.end, windowEnd)) }
    )
    val busyAwake = busy.sumOf { span ->
        (span.durationMillis / 60_000L).toInt() -
            SleepWindowCalculator.minutesInWindows(sleepWindows, span.start, span.end)
    }
    return (awakeMinutes - busyAwake).coerceAtLeast(0)
}

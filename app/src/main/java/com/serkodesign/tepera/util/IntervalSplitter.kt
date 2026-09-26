package com.serkodesign.tepera.util

/** Частина інтервалу в межах однієї логічної доби; [minutes] — цілі хвилини. */
data class IntervalPart(val startMillis: Long, val endMillis: Long) {
    val minutes: Int get() = ((endMillis - startMillis) / 60_000L).toInt()
}

/**
 * Активність, що триває через кілька діб ("19 вер 19:00 → 20 вер 12:00"), зберігається кількома
 * записами — по одному на кожну ЛОГІЧНУ добу ([DAY_ROLLOVER_HOUR], 01:00, а не календарну північ).
 * Причина: Home, віджет, Щоденник, Статистика й дайджест відносять запис до доби за його
 * `startTime`, тож цілий 17-годинний запис розтягнув би «Ти відмітив» однієї доби, а іншу
 * лишив би порожньою. Ділення по 01:00 (а не по 00:00) дає Home правильні числа: частина, що
 * починається о 01:00, потрапляє в нову логічну добу, а не в стару.
 *
 * Запис, що не перетинає межі, повертається однією частиною. Порожній результат — для
 * некоректного інтервалу ([endMillis] ≤ [startMillis]).
 */
fun splitAtDayRollover(startMillis: Long, endMillis: Long): List<IntervalPart> {
    if (endMillis <= startMillis) return emptyList()
    val parts = mutableListOf<IntervalPart>()
    var cursor = startMillis
    while (cursor < endMillis) {
        val next = minOf(nextDayRolloverMillis(cursor), endMillis)
        parts += IntervalPart(cursor, next)
        cursor = next
    }
    return parts
}

/**
 * Для кожної багатодобової активності (за `seriesId`) — від початку першої частини до кінця останньої
 * серед переданих [entries]. Для підпису "одна активність" у Щоденнику й історії категорії.
 */
fun seriesRanges(entries: List<com.serkodesign.tepera.data.local.entity.ActivityEntryEntity>): Map<String, Pair<Long, Long>> =
    entries.filter { it.seriesId != null }
        .groupBy { it.seriesId!! }
        .mapValues { (_, parts) ->
            parts.minOf { it.startTime } to parts.maxOf { it.startTime + it.durationMinutes * 60_000L }
        }

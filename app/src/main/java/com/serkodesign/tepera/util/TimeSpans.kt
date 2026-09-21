package com.serkodesign.tepera.util

/** Проміжок часу [start, end) у мілісекундах. */
data class TimeSpan(val start: Long, val end: Long) {
    val durationMillis: Long get() = (end - start).coerceAtLeast(0)
}

/**
 * Об'єднує проміжки, що перекриваються або торкаються, у мінімальний набір неперетинних проміжків
 * (відсортований за початком). Порожні й від'ємні проміжки відкидаються.
 */
fun mergeTimeSpans(spans: List<TimeSpan>): List<TimeSpan> {
    val sorted = spans.filter { it.end > it.start }.sortedBy { it.start }
    if (sorted.isEmpty()) return emptyList()
    val result = mutableListOf(sorted.first())
    for (span in sorted.drop(1)) {
        val last = result.last()
        if (span.start <= last.end) {
            if (span.end > last.end) result[result.lastIndex] = TimeSpan(last.start, span.end)
        } else {
            result.add(span)
        }
    }
    return result
}

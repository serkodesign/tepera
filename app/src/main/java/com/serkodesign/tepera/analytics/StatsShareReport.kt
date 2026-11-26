package com.serkodesign.tepera.analytics

import com.serkodesign.tepera.data.local.entity.DailyEventCounterEntity
import java.time.LocalDate

/**
 * Текст «Поділитися анонімною статистикою»: рівно те, що показує екран попереднього перегляду й що йде в листі.
 * Лише період, версії та підсумкові лічильники за весь період (без розбивки по днях). Жодних ідентифікаторів,
 * моделі пристрою, назв застосунків чи введеного тексту. Англійською завжди — лист читає розробник.
 */
object StatsShareReport {
    const val PERIOD_DAYS = 30L

    fun build(
        rows: List<DailyEventCounterEntity>,
        today: LocalDate,
        appVersion: String,
        androidRelease: String,
        androidSdk: Int
    ): String {
        val from = today.minusDays(PERIOD_DAYS - 1)
        val totals = rows
            .filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
            .groupBy { it.eventName to it.propsKey }
            .map { (key, group) -> key to group.sumOf { it.count } }
            .sortedWith(compareBy({ it.first.first }, { it.first.second }))
        return buildString {
            appendLine("Tepera anonymous usage statistics")
            appendLine("Period: $from – $today ($PERIOD_DAYS days)")
            appendLine("App version: $appVersion")
            appendLine("Android: $androidRelease (API $androidSdk)")
            appendLine()
            if (totals.isEmpty()) {
                append("No events yet.")
            } else {
                totals.forEachIndexed { i, (key, count) ->
                    val (name, props) = key
                    append(if (props.isEmpty()) "$name: $count" else "$name [$props]: $count")
                    if (i != totals.lastIndex) appendLine()
                }
            }
        }
    }

    /** Є ли що надсилати. */
    fun hasData(rows: List<DailyEventCounterEntity>, today: LocalDate): Boolean {
        val from = today.minusDays(PERIOD_DAYS - 1)
        return rows.any { !it.date.isBefore(from) && !it.date.isAfter(today) }
    }
}

package com.serkodesign.tepera.analytics

import com.serkodesign.tepera.data.local.dao.DailyEventCounterDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Канонічний ключ властивостей: лише allowlist, ключі за алфавітом, `k=v;k=v`; порожній рядок, якщо властивостей нема. */
fun analyticsPropsKey(props: Map<String, Any>): String =
    AnalyticsAllowlist.sanitize(props).entries
        .sortedBy { it.key }
        .joinToString(";") { "${it.key}=${it.value}" }

/** Скільки днів зберігаються лічильники. */
const val ANALYTICS_RETENTION_DAYS = 90L

/** Найстаріша дата, що ще лишається (раніші видаляються). */
fun analyticsRetentionCutoff(today: LocalDate): LocalDate = today.minusDays(ANALYTICS_RETENTION_DAYS)

/**
 * Рівень 1: пише денні лічильники в Room. Нічого не блокує (запис у [scope] з IO-диспетчером), нікуди не надсилає.
 * Помилка запису мовчки ігнорується — аналітика не має ламати застосунок.
 */
class LocalAnalytics(
    private val dao: DailyEventCounterDao,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate = { LocalDate.now() }
) : Analytics {
    override fun track(event: AnalyticsEvent) {
        val date = today()
        val key = analyticsPropsKey(event.props)
        scope.launch { runCatching { dao.increment(date, event.name, key) } }
    }
}

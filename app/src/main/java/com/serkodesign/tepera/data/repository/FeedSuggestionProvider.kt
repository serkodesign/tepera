package com.serkodesign.tepera.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import com.serkodesign.tepera.domain.risk.FeedSuggestions
import com.serkodesign.tepera.domain.risk.SlotSplitter
import com.serkodesign.tepera.util.foregroundSessionsByPackage
import com.serkodesign.tepera.util.queryEventsOrNull
import com.serkodesign.tepera.util.systemExclusionPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * v3.0 MVP, Етап 1/3: підказує, які застосунки показати першими в списку стрічок — ті, у яких найбільше вечірнього
 * часу (18:00–04:00) за доступну системну історію. Нічого не вмикає й не зберігає; месенджери не пропонуються.
 */
class FeedSuggestionProvider(private val context: Context) {

    suspend fun suggest(historyDays: Int = 9, limit: Int = 12): Set<String> = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val start = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().minusDays(historyDays.toLong())
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val excluded = systemExclusionPackages(context)
        val seconds = HashMap<String, Long>()
        try {
            // По добі, як у PatternRepository: загублена подія «у фон» не розтягується на кілька днів.
            var dayStart = start
            while (dayStart < now) {
                val dayEnd = minOf(Instant.ofEpochMilli(dayStart).atZone(zone).toLocalDate().plusDays(1)
                    .atStartOfDay(zone).toInstant().toEpochMilli(), now)
                val events = usm.queryEventsOrNull(dayStart, dayEnd) ?: return@withContext emptySet()
                val packages = HashSet<String>()
                val event = android.app.usage.UsageEvents.Event()
                // Перший прохід збирає пакети, другий (через новий запит) рахує сесії лише для них.
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    event.packageName?.let { if (it !in excluded) packages += it }
                }
                if (packages.isNotEmpty()) {
                    val again = usm.queryEventsOrNull(dayStart, dayEnd) ?: return@withContext emptySet()
                    for ((pkg, span) in foregroundSessionsByPackage(again, packages, dayEnd)) {
                        SlotSplitter.split(span.start, span.end, zone) { _, slot, millis ->
                            if (FeedSuggestions.isEveningSlot(slot)) seconds.merge(pkg, millis / 1000, Long::plus)
                        }
                    }
                }
                dayStart = dayEnd
            }
        } catch (e: SecurityException) {
            return@withContext emptySet()
        }
        FeedSuggestions.top(seconds, limit).toSet()
    }
}

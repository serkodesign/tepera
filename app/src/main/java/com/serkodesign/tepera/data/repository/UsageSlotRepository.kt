package com.serkodesign.tepera.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.dao.UsageSlotDao
import com.serkodesign.tepera.data.local.entity.UsageSlotEntity
import com.serkodesign.tepera.domain.risk.SlotUsage
import com.serkodesign.tepera.domain.risk.SlotSplitter
import com.serkodesign.tepera.util.foregroundSessionsByPackage
import com.serkodesign.tepera.util.queryEventsOrNull
import com.serkodesign.tepera.util.withReliableUsageEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Результат [UsageSlotRepository.recompute]. */
sealed interface SlotRecompute {
    /** Список стрічок порожній або нема доступу/подій — нічого не рахувалось. */
    data object Skipped : SlotRecompute

    /** Системні події не вдалось прочитати — нічого не записано, повториться пізніше. */
    data object Failed : SlotRecompute

    data class Done(val daysComputed: Int, val earliestEventMillis: Long) : SlotRecompute
}

/**
 * v3.0 MVP, Етап 0/1: накопичує власні 15-хвилинні агрегати часу в застосунках-стрічках, бо `UsageStatsManager`
 * тримає лише кілька днів подій. Не рахує дні, що починаються до найдавнішої збереженої системою події (вони
 * неповні й перезаписали б правильні старі дані), і не зберігає запасних нулів при збої читання подій.
 */
class UsageSlotRepository(
    private val context: Context,
    private val dao: UsageSlotDao,
    private val settingsStore: SettingsStore
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Перераховує слоти за останні [lookbackDays] днів (разом із сьогоднішнім частковим), де системна історія повна. */
    suspend fun recompute(
        nowMillis: Long = System.currentTimeMillis(),
        lookbackDays: Int = 10
    ): SlotRecompute = withContext(Dispatchers.IO) {
        val feed = settingsStore.feedPackages.first()
        if (feed.isEmpty()) return@withContext SlotRecompute.Skipped
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val windowStart = today.minusDays(lookbackDays.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

        val outcome = try {
            withReliableUsageEvents {
                val earliest = earliestEventMillis(usm, windowStart, nowMillis) ?: return@withReliableUsageEvents null
                val earliestDate = Instant.ofEpochMilli(earliest).atZone(zone).toLocalDate()
                val computed = mutableListOf<Long>()
                var date = maxOf(today.minusDays(lookbackDays.toLong()), earliestDate.plusDays(1))
                while (!date.isAfter(today)) {
                    val rows = computeDay(usm, date, feed, nowMillis) ?: return@withReliableUsageEvents null
                    dao.replaceDay(date.toEpochDay(), rows)
                    computed += date.toEpochDay()
                    date = date.plusDays(1)
                }
                computed to earliest
            }
        } catch (e: SecurityException) {
            return@withContext SlotRecompute.Skipped
        } ?: return@withContext SlotRecompute.Failed

        val (computed, earliest) = outcome
        settingsStore.addUsageSlotCoveredDays(computed, keepFromEpochDay = today.toEpochDay() - KEEP_DAYS)
        dao.deleteOlderThan(today.toEpochDay() - KEEP_DAYS)
        SlotRecompute.Done(computed.size, earliest)
    }

    /** Найраніша подія, яку система ще тримає в межах [fromMillis, toMillis); `null` — подій нема. */
    suspend fun earliestEventMillis(fromMillis: Long, toMillis: Long): Long? = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        try {
            earliestEventMillis(usm, fromMillis, toMillis)
        } catch (e: SecurityException) {
            null
        }
    }

    private fun earliestEventMillis(usm: UsageStatsManager, fromMillis: Long, toMillis: Long): Long? {
        val events = usm.queryEventsOrNull(fromMillis, toMillis) ?: return null
        if (!events.hasNextEvent()) return null
        val event = android.app.usage.UsageEvents.Event()
        events.getNextEvent(event)
        return event.timeStamp
    }

    /** Слоти однієї календарної дати лише для [feed]; `null`, якщо події не вдалось прочитати. */
    private fun computeDay(usm: UsageStatsManager, date: LocalDate, feed: Set<String>, nowMillis: Long): List<UsageSlotEntity>? {
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = minOf(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), nowMillis)
        if (dayEnd <= dayStart) return emptyList()
        val events = usm.queryEventsOrNull(dayStart, dayEnd) ?: return null
        val millisBySlot = HashMap<Pair<Int, String>, Long>()
        for ((pkg, span) in foregroundSessionsByPackage(events, feed, dayEnd)) {
            SlotSplitter.split(span.start, span.end, zone) { _, slot, millis ->
                millisBySlot.merge(slot to pkg, millis, Long::plus)
            }
        }
        return millisBySlot.mapNotNull { (key, millis) ->
            val seconds = (millis / 1000).toInt()
            if (seconds <= 0) null else UsageSlotEntity(date.toEpochDay(), key.first, key.second, seconds)
        }
    }


    /** Слоти за календарні дати [from]..[to]; якщо [feed] задано — лише ці застосунки. */
    suspend fun readRecords(from: LocalDate, to: LocalDate, feed: Set<String>? = null): List<SlotUsage> = withContext(Dispatchers.IO) {
        dao.getRange(from.toEpochDay(), to.toEpochDay())
            .filter { feed == null || it.packageName in feed }
            .groupBy { it.date to it.slot }
            .map { (key, rows) -> SlotUsage(LocalDate.ofEpochDay(key.first), key.second, rows.sumOf { it.seconds }) }
    }

    suspend fun coveredDays(): Set<LocalDate> =
        settingsStore.usageSlotCoveredDays.first().map(LocalDate::ofEpochDay).toSet()

    private companion object {
        const val KEEP_DAYS = 60L
    }
}

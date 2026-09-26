package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.util.EntrySpan
import com.serkodesign.tepera.util.HistoryCoverage
import com.serkodesign.tepera.util.WelcomeBack
import com.serkodesign.tepera.util.WelcomeBackSummary
import kotlinx.coroutines.flow.first
import java.time.ZoneId

/**
 * CC-4: «повернення без провини». [onAppOpened] викликається при кожному справжньому відкритті Tepera (не
 * через ворота): якщо з попереднього відкриття минуло [WelcomeBack.MIN_GAP_DAYS] і більше діб, запам'ятовує
 * початок цієї перерви — підсумок лишається доступним, доки людина його не закриє. Слів про «пропущене» немає:
 * підсумок лише перелічує, що відмічено й скільки було Online.
 *
 * Нічого не фіксується у фоні (D-25): Online береться з системної історії подій у момент показу, тож для
 * дуже довгих перерв (давніших за ~7-9 діб історії) середнє Online не показується — лише те, що відмічено.
 */
class WelcomeBackRepository(
    private val settingsStore: SettingsStore,
    private val balanceRepository: BalanceRepository,
    private val activityRepository: ActivityRepository
) {
    suspend fun onAppOpened(nowMillis: Long = System.currentTimeMillis()) {
        val lastOpen = settingsStore.lastOpenMillis.first()
        if (WelcomeBack.gapReached(lastOpen, nowMillis)) settingsStore.setWelcomeBackPendingFrom(lastOpen)
        settingsStore.setLastOpenMillis(nowMillis)
    }

    /** Підсумок для показу зараз, або `null` (перерви нема, закрито, чи показувати нічого). */
    suspend fun currentSummary(nowMillis: Long = System.currentTimeMillis()): WelcomeBackSummary? {
        val pendingFrom = settingsStore.welcomeBackPendingFrom.first()
        if (pendingFrom <= 0L) return null
        val zone = ZoneId.systemDefault()
        val days = WelcomeBack.periodDays(pendingFrom, nowMillis, zone)

        // Найдавніша подія шукається з запасом за межі історії, щоб знайти справжній початок системної історії.
        val earliest = balanceRepository.earliestUsageEventMillis(nowMillis - HISTORY_PROBE_MILLIS)
        val online = HistoryCoverage.coveredDays(days, earliest, zone).associate { day ->
            val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            day.toEpochDay() to balanceRepository.getOnlineMinutes(from, to)
        }
        val entries = if (days.isEmpty()) emptyList() else {
            val from = days.first().atStartOfDay(zone).toInstant().toEpochMilli()
            val to = days.last().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            activityRepository.observeEntriesInRange(from, to).first().map { EntrySpan(it.startTime, it.durationMinutes) }
        }
        val summary = WelcomeBackSummary.build(days, online, entries, zone)
        if (summary == null) settingsStore.setWelcomeBackPendingFrom(0L) // нічого показувати — не тримаємо
        return summary
    }


    suspend fun dismiss() {
        settingsStore.setWelcomeBackPendingFrom(0L)
    }

    private companion object {
        const val HISTORY_PROBE_MILLIS = 10L * 24 * 60 * 60 * 1000
    }
}

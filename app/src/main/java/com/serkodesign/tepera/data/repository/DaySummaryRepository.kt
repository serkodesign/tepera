package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.DaySummaryDao
import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import com.serkodesign.tepera.data.local.entity.DaySummaryEntity
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.util.DaySummaryBackfill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * P-2 (`CLAUDE-CODE-PLAN-pro.md`), D-39 (варіант B+): щоденний Online-підсумок для майбутньої
 * Pro-статистики "Місяць"/"Рік" (P-11/P-12). Викликається з `WidgetUpdateWorker.doWork()`
 * (~30 хв) і `WidgetRolloverWorker.doWork()` (01:00) — НЕ лише з точки входу застосунку на
 * передньому плані ("чистий" варіант B із плану) — щоб розрив для довгих перерв без відкриття
 * застосунку був мінімальним (власник прийняв цей компроміс явно, D-39).
 *
 * Жодних назв пакетів застосунків не зберігається. У безкоштовному UI ці дані НІДЕ не читаються
 * цим завданням — лише накопичуються.
 */
class DaySummaryRepository(
    private val dao: DaySummaryDao,
    private val balanceRepository: BalanceRepository,
    private val unlockRepository: UnlockRepository,
    private val excludedAppDao: ExcludedAppDao,
    private val sleepWindowRepository: SleepWindowRepository
) {
    /**
     * Дозаписує завершені доби, яких ще немає в таблиці й які системна історія покриває
     * повністю. Ідемпотентно — безпечно викликати що ~30 хв. Без доступу до статистики нічого
     * не пише й не падає (FR-3.12, GAP-2).
     */
    suspend fun backfillCompletedDays(nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        if (!balanceRepository.hasUsageAccess()) return@withContext
        val zone = ZoneId.systemDefault()
        val recorded = dao.getAllDates().map(LocalDate::ofEpochDay).toSet()
        val earliest = balanceRepository.earliestUsageEventMillis(nowMillis - HISTORY_PROBE_MILLIS)
        val candidates = DaySummaryBackfill.candidateDays(nowMillis, recorded, earliest, zone)
        if (candidates.isEmpty()) return@withContext

        val sleepWindows = sleepWindowRepository.getEnabledWindows()
        // Хеш налаштувань на МОМЕНТ підрахунку — рядок потім не перераховується, навіть якщо
        // Exclusion List чи вікна сну зміняться пізніше (той самий принцип "рядок незмінний").
        val exclusionHash = excludedAppDao.getExcludedPackageNames().toSet().hashCode()
        val sleepWindowHash = sleepWindows.hashCode()

        candidates.forEach { day -> dao.insert(buildSummary(day, zone, sleepWindows, exclusionHash, sleepWindowHash)) }
    }

    private suspend fun buildSummary(
        day: LocalDate,
        zone: ZoneId,
        sleepWindows: List<SleepWindowEntity>,
        exclusionHash: Int,
        sleepWindowHash: Int
    ): DaySummaryEntity {
        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val dayStartMillis = balanceRepository.calculateDayStartMillis(sleepWindows, from, to)
        val dayStartMinute = if (dayStartMillis >= to) null else ((dayStartMillis - from) / 60_000L).toInt()

        // "Останнє використання" цієї календарної доби — кінець останнього Online-інтервалу в
        // межах [from, to). Свідомо НЕ через PauseRepository.lastPhoneUseForShiftedDay() — та
        // функція рахує 02:00-зсунуту добу (FR-D.7a, навмисно не пов'язана з календарною добою),
        // а day_summaries увесь інший час тримає рівно календарні межі; змішування двох конвенцій
        // дня в одному рядку дало б нечесне число.
        val lastUseMillis = balanceRepository.getOnlineIntervals(from, to).maxByOrNull { it.end }?.end
        val lastUseMinute = lastUseMillis?.let { ((it - from) / 60_000L).toInt() }

        val unlocks = if (unlockRepository.isSupported()) {
            unlockRepository.countUnlocks(from, to, sleepWindows)
        } else {
            null
        }

        return DaySummaryEntity(
            date = day.toEpochDay(),
            onlineMinutes = balanceRepository.getOnlineMinutes(from, to),
            unlocks = unlocks,
            dayStartMinute = dayStartMinute,
            lastUseMinute = lastUseMinute,
            onlineByHour = balanceRepository.getOnlineMinutesPerSlot(from, 60, 24),
            exclusionHash = exclusionHash,
            sleepWindowHash = sleepWindowHash,
            createdAt = System.currentTimeMillis()
        )
    }

    private companion object {
        const val HISTORY_PROBE_MILLIS = 10L * 24 * 60 * 60 * 1000
    }
}

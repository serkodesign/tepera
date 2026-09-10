package com.serkodesign.tepera.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.serkodesign.tepera.data.local.dao.DetectedGapDao
import com.serkodesign.tepera.data.local.entity.DetectedGapEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MIN_GAP_MINUTES = 30 // FR-D.1

/** Один кандидат на паузу — ще не запис у БД (див. [PauseRepository.scan]). */
data class GapCandidate(val startTime: Long, val durationMinutes: Int)

/**
 * FR-D.6: "твій день з телефоном" — від першої "суттєвої" сесії до останньої перед наступним
 * вікном сну. null-поля — коли в межах запитаного проміжку взагалі не було жодної сесії
 * (typowo немає доступу до статистики або порожній проміжок).
 */
data class PhoneUsageScan(
    val gaps: List<GapCandidate>,
    val firstSessionStart: Long?,
    val lastSessionEnd: Long?
)

/**
 * FR-D.1–D.7 (SRS v2.6): детекція пауз між сесіями використання телефону (БУДЬ-ЯКИЙ застосунок
 * на передньому плані — на відміну від BalanceRepository.getOnlineMinutes(), тут навмисно НЕ
 * застосовується Exclusion List і виключення лаунчера/клавіатури: пауза — це про те, чи людина
 * взагалі торкалась телефону, не про категоризацію "залежності" (FR-D.2).
 */
class PauseRepository(
    private val context: Context,
    private val gapDao: DetectedGapDao
) {

    /**
     * Один прохід по сирих подіях [from, to): зливає перекривні/суміжні сесії різних застосунків
     * в один таймлайн "телефон у використанні" (лічильник одночасно активних на передньому плані
     * застосунків, а не перетин інтервалів по одному), знаходить проміжки між сесіями ≥30 хв
     * (FR-D.1) і межі першої/останньої сесії (FR-D.6).
     *
     * Відоме обмеження: сесія, що вже тривала на момент [from] (її MOVE_TO_FOREGROUND — поза
     * вибіркою, у полі зору лише завершальний MOVE_TO_BACKGROUND), не рахується як сесія від
     * самого [from] — крайовий випадок на межі вікна запиту, не критичний для детекції пауз.
     */
    suspend fun scan(from: Long, to: Long): PhoneUsageScan = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        try {
            val events = usm.queryEvents(from, to)
            val event = UsageEvents.Event()
            val foregroundApps = HashSet<String>()
            var sessionStart: Long? = null
            val sessions = mutableListOf<LongRange>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val packageName = event.packageName ?: continue
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (foregroundApps.isEmpty()) sessionStart = event.timeStamp
                        foregroundApps.add(packageName)
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        foregroundApps.remove(packageName)
                        val start = sessionStart
                        if (foregroundApps.isEmpty() && start != null) {
                            sessions.add(start..event.timeStamp)
                            sessionStart = null
                        }
                    }
                }
            }
            // Остання сесія ще триває на момент "to" (немає завершального MOVE_TO_BACKGROUND).
            val openStart = sessionStart
            if (openStart != null) sessions.add(openStart..to)

            val gaps = mutableListOf<GapCandidate>()
            for (i in 0 until sessions.size - 1) {
                val gapStart = sessions[i].last
                val gapEnd = sessions[i + 1].first
                val durationMinutes = ((gapEnd - gapStart) / 60_000L).toInt()
                if (durationMinutes >= MIN_GAP_MINUTES) {
                    gaps.add(GapCandidate(gapStart, durationMinutes))
                }
            }
            PhoneUsageScan(gaps, sessions.firstOrNull()?.first, sessions.lastOrNull()?.last)
        } catch (e: SecurityException) {
            PhoneUsageScan(emptyList(), null, null)
        }
    }

    /** Зберігає нововиявлені паузи; уже наявні (той самий startTime) лишаються без змін (DAO). */
    suspend fun persist(gaps: List<GapCandidate>) {
        if (gaps.isEmpty()) return
        gapDao.insertAll(gaps.map { DetectedGapEntity(startTime = it.startTime, durationMinutes = it.durationMinutes) })
    }

    /** FR-D.3, FR-D.5: паузи в проміжку, ще не позначені й не пропущені користувачем. */
    suspend fun getUnresolvedGaps(from: Long, to: Long): List<DetectedGapEntity> =
        gapDao.getUnresolvedInRange(from, to)

    suspend fun markLabeled(gapId: String, entryId: String) = gapDao.markLabeled(gapId, entryId)

    suspend fun dismiss(gapId: String) = gapDao.markDismissed(gapId)
}

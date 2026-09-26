package com.serkodesign.tepera.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.serkodesign.tepera.data.GapDetectionConfig
import com.serkodesign.tepera.data.GapSensitivity
import com.serkodesign.tepera.data.local.dao.DetectedGapDao
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.DetectedGapEntity
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.util.SleepWindowCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

private val DEFAULT_CONFIG = GapDetectionConfig.forSensitivity(GapSensitivity.NORMAL)
private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val SHIFTED_DAY_CUTOFF_HOUR = 2 // FR-D.7a

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
 * T-10 (tepera-dev-spec.md): остання сесія використання за конкретну 02:00-зсунуту "добу"
 * (FR-D.7a) — [boundaryMillis] сама межа (для переведення [lastUseMillis] у координати "хвилин
 * до межі", `LastPhoneUseEstimateViewModel`), [lastUseMillis] `null`, якщо в цю добу взагалі не
 * було сесій.
 */
data class ShiftedDayLastUse(val boundaryMillis: Long, val lastUseMillis: Long?)

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
     *
     * [sleepWindows] (T-12, tepera-dev-spec.md): кандидат на паузу, що перетинається з ввімкненим
     * вікном сну, взагалі не потрапляє в [PhoneUsageScan.gaps] — "пауза, що припадає на вікно
     * сну, не детектується". За замовчуванням порожній список (без фільтрації) — так викликає
     * [lastPhoneUseForShiftedDay] (FR-D.7 рахується наскрізь, незалежно від вікна сну), лише
     * `PauseViewModel` передає реальні вікна.
     *
     * [config] (T-11, tepera-dev-spec.md): усі пороги детекції в одному місці, замість розкиданих
     * констант. Після фільтрації по [GapDetectionConfig.minGapMinutes] і вікнах сну, сусідні
     * кандидати, розділені коротшою за [GapDetectionConfig.minIntervalBetweenGapsMinutes] сесією
     * використання, зливаються в одну паузу ([mergeCloseGaps]) — інакше серія коротких поглядів
     * у телефон посеред довгої відсутності дає кілька карток замість однієї. Далі — стеля
     * [GapDetectionConfig.maxGapsPerDay]: при перевищенні лишаються найдовші паузи.
     */
    suspend fun scan(
        from: Long,
        to: Long,
        sleepWindows: List<SleepWindowEntity> = emptyList(),
        config: GapDetectionConfig = DEFAULT_CONFIG
    ): PhoneUsageScan = withContext(Dispatchers.IO) {
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

            val rawGaps = mutableListOf<GapCandidate>()
            for (i in 0 until sessions.size - 1) {
                val gapStart = sessions[i].last
                val gapEnd = sessions[i + 1].first
                val durationMinutes = ((gapEnd - gapStart) / 60_000L).toInt()
                if (durationMinutes >= config.minGapMinutes && !SleepWindowCalculator.overlapsWindow(sleepWindows, gapStart, gapEnd)) {
                    rawGaps.add(GapCandidate(gapStart, durationMinutes))
                }
            }

            val merged = mergeCloseGaps(rawGaps, config.minIntervalBetweenGapsMinutes)
            // Стеля: при перевищенні лишаються найдовші, потім назад у хронологічний порядок.
            val capped = merged.sortedByDescending { it.durationMinutes }
                .take(config.maxGapsPerDay)
                .sortedBy { it.startTime }

            PhoneUsageScan(capped, sessions.firstOrNull()?.first, sessions.lastOrNull()?.last)
        } catch (e: SecurityException) {
            PhoneUsageScan(emptyList(), null, null)
        }
    }

    /**
     * Дві паузи-кандидати, між якими лежить коротша за [minIntervalMinutes] сесія використання,
     * зливаються в одну неперервну паузу (від початку першої до кінця другої) — та коротка сесія
     * посередині трактується як шум (погляд на годинник), не як повернення до використання.
     * [gaps] мають не перетинатись і вже бути відсортовані як кандидати зі сканування сесій
     * (сортування тут — захист, а не припущення про виклик).
     */
    private fun mergeCloseGaps(gaps: List<GapCandidate>, minIntervalMinutes: Int): List<GapCandidate> {
        if (gaps.size <= 1) return gaps
        val sorted = gaps.sortedBy { it.startTime }
        val result = mutableListOf<GapCandidate>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            val currentEnd = current.startTime + current.durationMinutes * 60_000L
            val intervalMinutes = ((next.startTime - currentEnd) / 60_000L).toInt()
            current = if (intervalMinutes < minIntervalMinutes) {
                val mergedEnd = next.startTime + next.durationMinutes * 60_000L
                GapCandidate(current.startTime, ((mergedEnd - current.startTime) / 60_000L).toInt())
            } else {
                result.add(current)
                next
            }
        }
        result.add(current)
        return result
    }

    /**
     * Зберігає нововиявлені паузи в [from, to), ЗАМІНЮючи попередній непозначений/непропущений
     * набір кандидатів цього діапазону (T-11: зміна пресету чутливості інакше лише додавала б нові
     * кандидати поверх застарілих з іншими startTime за старим пресетом, а не перераховувала б
     * добу) — уже позначені чи пропущені паузи не зачіпає, `DetectedGapDao.replaceUnresolvedInRange()`
     * відсіює їх власним WHERE.
     */
    suspend fun persist(from: Long, to: Long, gaps: List<GapCandidate>) {
        gapDao.replaceUnresolvedInRange(from, to, gaps.map { DetectedGapEntity(startTime = it.startTime, durationMinutes = it.durationMinutes) })
    }

    /** FR-D.3, FR-D.5: паузи в проміжку, ще не позначені й не пропущені користувачем. */
    suspend fun getUnresolvedGaps(from: Long, to: Long): List<DetectedGapEntity> =
        gapDao.getUnresolvedInRange(from, to)

    /**
     * Захисний фільтр (реальний баг, знайдений користувачем): [getUnresolvedGaps] довіряє лише
     * власному полю [DetectedGapEntity.labeledEntryId], яке виставляється ЛИШЕ через [markLabeled]
     * (тап по картці пауз). Якщо користувач замість цього вручну додав активність на той самий
     * час (звичайний "+Додати"/редагування, а не тап по паузі) — або лишився старий, ще не
     * позначений рядок з часів ДО T-2-бекфілу чи попередньої версії пресету чутливості (T-11) —
     * пауза лишалась "непозначеною" в БД НАЗАВЖДИ й пропонувалась знову щоразу, коли вікно
     * опитування (FR-D.3) знову накривало той самий проміжок, хоча час насправді вже заповнений.
     * Тут пауза, чий інтервал ПЕРЕТИНАЄТЬСЯ з будь-яким уже наявним записом активності (незалежно
     * від джерела), ретроактивно позначається позначеною (той самий [markLabeled], що й ручний
     * тап) — не лише ховається з поточного списку, а й більше не спливе в наступних запитах.
     */
    suspend fun reconcileWithEntries(
        gaps: List<DetectedGapEntity>,
        entries: List<ActivityEntryEntity>
    ): List<DetectedGapEntity> {
        if (gaps.isEmpty() || entries.isEmpty()) return gaps
        val stillUnresolved = mutableListOf<DetectedGapEntity>()
        for (gap in gaps) {
            val gapEnd = gap.startTime + gap.durationMinutes * 60_000L
            val covering = entries.firstOrNull { entry ->
                val entryEnd = entry.startTime + entry.durationMinutes * 60_000L
                entry.startTime < gapEnd && entryEnd > gap.startTime
            }
            if (covering != null) {
                gapDao.markLabeled(gap.id, covering.id)
            } else {
                stillUnresolved.add(gap)
            }
        }
        return stillUnresolved
    }

    suspend fun markLabeled(gapId: String, entryId: String) = gapDao.markLabeled(gapId, entryId)

    suspend fun dismiss(gapId: String) = gapDao.markDismissed(gapId)

    /**
     * FR-D.7a: найближча межа 02:00 у минулому (включно) відносно [nowMillis] — межа
     * ВІДНЕСЕННЯ пізньої сесії до попереднього/наступного дня (сесія о 01:42 — учора, о 02:30 —
     * вже сьогодні). Свідомо НЕ прив'язана до `SleepWindowRepository`/T-12 (те визначає межу
     * ПОЧАТКУ дня й детекцію пауз — інше питання).
     */
    fun shiftedDayBoundary(nowMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowMillis
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        calendar.set(Calendar.HOUR_OF_DAY, SHIFTED_DAY_CUTOFF_HOUR)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (hour < SHIFTED_DAY_CUTOFF_HOUR) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return calendar.timeInMillis
    }

    /**
     * T-10 (tepera-dev-spec.md): остання сесія використання за 02:00-зсунуту добу, [daysBack]
     * діб тому відносно найновішої завершеної відносно [nowMillis] (daysBack=0 — те, що FR-D.7
     * називає "вчора"). Перевикористовує [scan] (той самий алгоритм детекції "телефон у
     * використанні" БЕЗ Exclusion List — "торкався телефону взагалі", не категоризація
     * залежності, FR-D.2) лише заради `lastSessionEnd`, без виклику [persist] — тут не потрібно
     * зберігати паузи вдруге.
     */
    suspend fun lastPhoneUseForShiftedDay(nowMillis: Long, daysBack: Int): ShiftedDayLastUse {
        val boundary = shiftedDayBoundary(nowMillis) - daysBack * DAY_MILLIS
        val lastUse = scan(boundary - DAY_MILLIS, boundary).lastSessionEnd
        return ShiftedDayLastUse(boundary, lastUse)
    }

    /**
     * T-10: медіана часу останнього використання за останні [windowDays] 02:00-зсунутих діб —
     * і другий, тихіший рядок картки-оцінки ([lastPhoneUseForShiftedDay]-споживач
     * `LastPhoneUseEstimateViewModel`), і "тижневий огляд" на Stats (`StatsViewModel`), той самий
     * розрахунок в обох місцях. Рахується в координатах "хвилин до межі" (не "хвилина доби") —
     * ці дані типово перетинають північ, а пряме порівняння 0-1439 дало б хибний порядок навколо
     * півночі; результат переводиться назад у мітку часу відносно межі [nowMillis]. `null`, якщо
     * жодна з [windowDays] діб не має даних.
     */
    suspend fun medianLastPhoneUseMillis(nowMillis: Long, windowDays: Int = 7): Long? {
        val referenceBoundary = shiftedDayBoundary(nowMillis)
        val diffsMinutes = (0 until windowDays).mapNotNull { daysBack ->
            val day = lastPhoneUseForShiftedDay(nowMillis, daysBack)
            val lastUse = day.lastUseMillis ?: return@mapNotNull null
            (day.boundaryMillis - lastUse) / 60_000L
        }
        if (diffsMinutes.isEmpty()) return null
        val sorted = diffsMinutes.sorted()
        val mid = sorted.size / 2
        val medianDiff = if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
        return referenceBoundary - medianDiff * 60_000L
    }
}

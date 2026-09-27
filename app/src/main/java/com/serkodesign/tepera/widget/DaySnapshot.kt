package com.serkodesign.tepera.widget

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.data.repository.GapCandidate
import com.serkodesign.tepera.util.TimeSpan
import com.serkodesign.tepera.util.offlineUnloggedMinutes

/**
 * Пауза в межах доби (W-1) — [categoryId] `null`, доки не названа. Той самий кандидат, що
 * [GapCandidate], лише з готовими межами часу (зручніше для UI віджетів, яким кінець потрібен
 * так само часто, як початок).
 */
data class NamedGap(val startMillis: Long, val endMillis: Long, val categoryId: String?) {
    val durationMinutes: Int get() = ((endMillis - startMillis) / 60_000L).toInt()
}

/**
 * W-1 (`CLAUDE-CODE-TASK-widgets.md`): спільна "форма доби" для віджетів "Сьогодні" й "Пульт" —
 * межі дня (з вікон сну), паузи ≥ порогу чутливості, сумарний час без телефону, найдовша пауза й
 * підсумок названого за категоріями.
 */
data class DaySnapshot(
    val dayStartMillis: Long,
    val dayEndMillis: Long,
    val gaps: List<NamedGap>,
    val phoneFreeMinutes: Int,
    val longestGap: NamedGap?,
    val namedMinutesByCategory: Map<String, Int>,
    // W-3: смуга дня показує використання тонкою лінією (не заливкою, як паузи) — тому потрібні
    // самі інтервали, не лише сумарна кількість хвилин.
    val onlineIntervals: List<TimeSpan>
)

/**
 * Чиста функція без Context/Room — фактичний фетч (сканування пауз, записи, Online-інтервали)
 * лежить у [DaySnapshotRepository]; тут лише збірка з уже отриманих значень, той самий принцип,
 * що `buildDayTimeline()` у `ui/stats/DayStats.kt` (пряме тестування без пристрою/моків).
 *
 * Категорія паузи визначається так само, як `StatsViewModel.computeDayDetails()`: перший запис
 * із `source == GAP_LABELED`, чий `startTime` потрапляє в `[gap.startTime, gapEnd)` — НЕ через
 * `DetectedGapEntity.labeledEntryId`, бо кандидати з `PauseRepository.scan()` ще не збережені в БД
 * (це проміжний результат сканування, не персистентний рядок) і того поля не мають.
 *
 * [phoneFreeMinutes] рахується тим самим `offlineUnloggedMinutes()`, що вже показує Home/Stats —
 * не нова формула, щоб число на віджеті ніколи не розходилось із числом у застосунку.
 */
fun buildDaySnapshot(
    dayStartMillis: Long,
    dayEndMillis: Long,
    gapCandidates: List<GapCandidate>,
    entries: List<ActivityEntryEntity>,
    onlineIntervals: List<TimeSpan>,
    sleepWindows: List<SleepWindowEntity>
): DaySnapshot {
    val gaps = gapCandidates.map { gap ->
        val gapEnd = gap.startTime + gap.durationMinutes * 60_000L
        val labeled = entries.firstOrNull {
            it.source == EntrySource.GAP_LABELED && it.startTime >= gap.startTime && it.startTime < gapEnd
        }
        NamedGap(gap.startTime, gapEnd, labeled?.categoryId)
    }
    val longest = gaps.maxByOrNull { it.durationMinutes }
    val namedMinutesByCategory = gaps
        .filter { it.categoryId != null }
        .groupBy { it.categoryId!! }
        .mapValues { (_, group) -> group.sumOf { it.durationMinutes } }
    val phoneFreeMinutes = offlineUnloggedMinutes(dayStartMillis, dayEndMillis, onlineIntervals, entries, sleepWindows)
    return DaySnapshot(dayStartMillis, dayEndMillis, gaps, phoneFreeMinutes, longest, namedMinutesByCategory, onlineIntervals)
}

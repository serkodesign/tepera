package com.serkodesign.tepera.domain.risk

import java.time.LocalDate

/**
 * v3.0, Етап 0/2 (`docs/CLAUDE-CODE-PLAN-v3-MVP.md`): пошук «годин ризику» — вікон, у яких людина регулярно
 * сидить у стрічках. Чиста Kotlin-логіка без Android-залежностей (щоб потім перенести на iOS через ті самі
 * фікстури). Усі пороги — у [RiskConfig], щоб підкручувати їх з debug-екрана після Етапу 0.
 *
 * Етап 0: перша версія без розділення будні/вихідні, зсуву вікна й тестів DST (це Етап 2).
 */
data class RiskConfig(
    /** Межа «вечора»: слоти до цієї години належать попередній логічній добі (вечір 22:40–00:10 — одна доба). */
    val dayCutoffHour: Int = 4,
    /** Слот активний, якщо в ньому не менше стількох секунд у застосунках-стрічках (за планом — 5 хв). */
    val activeSlotSeconds: Int = 300,
    /** Активні слоти склеюються, якщо проміжок між ними не більший за стільки хвилин. */
    val mergeGapMinutes: Int = 15,
    /** Скільки днів із 7 мінімум має «світитись» слот, щоб стати зерном кандидата (ширше за поріг прийняття). */
    val seedMinDays: Int = 3,
    /** Вікно приймається, якщо повторюється щонайменше в стількох днях із [observationDays]. */
    val minRecurrenceDays: Int = 4,
    /** …і якщо середні хвилини в стрічках усередині вікна (за днями повторення) не менші за це. */
    val minAvgMinutes: Int = 15,
    /** День «повторює» кандидата, якщо активні слоти покривають щонайменше цю частку його тривалості. */
    val overlapShare: Double = 0.5,
    val roundToMinutes: Int = 10,
    val maxWindows: Int = 3,
    val observationDays: Int = 7,
    /** Скільки з [observationDays] днів мають бути покриті даними, щоб будувати вікна взагалі. */
    val minObservedDays: Int = 7
)

/** Секунди в застосунках-стрічках за один 15-хвилинний слот локальної календарної дати (слот 0 = 00:00–00:15). */
data class SlotUsage(val date: LocalDate, val slot: Int, val seconds: Int)

/**
 * [startMinute]/[endMinute] — хвилини від локальної півночі; якщо [endMinute] ≤ [startMinute], вікно перетинає
 * північ. [avgMinutes] — середнє по днях повторення.
 */
data class RiskCandidate(
    val startMinute: Int,
    val endMinute: Int,
    val recurrenceDays: Int,
    val observedDays: Int,
    val avgMinutes: Int,
    val hitDays: List<LocalDate>
) {
    val crossesMidnight: Boolean get() = endMinute <= startMinute
}

class RiskResult(
    /** Логічні доби спостереження від найдавнішої до найновішої. */
    val days: List<LocalDate>,
    val observedDays: Int,
    val enoughData: Boolean,
    val candidates: List<RiskCandidate>
)

object RiskWindowFinder {
    const val SLOT_MINUTES = 15
    const val SLOTS_PER_DAY = 96

    /** Логічна доба, до якої належить слот календарної дати [date]. */
    fun logicalDate(date: LocalDate, slot: Int, cfg: RiskConfig): LocalDate =
        if (slot * SLOT_MINUTES < cfg.dayCutoffHour * 60) date.minusDays(1) else date

    /** Позиція слота всередині логічної доби: 0 = слот одразу після межі доби. */
    fun position(slot: Int, cfg: RiskConfig): Int =
        (slot - cfg.dayCutoffHour * 60 / SLOT_MINUTES + SLOTS_PER_DAY) % SLOTS_PER_DAY

    private class Accepted(val startPos: Int, val endPos: Int, val hits: List<LocalDate>, val avg: Double)

    /**
     * @param records ненульові слоти (відсутній слот = 0 с)
     * @param coveredDays календарні дати, для яких слоти реально пораховані (порожній день без записів ≠ немає даних)
     * @param lastLogicalDay остання ЗАВЕРШЕНА логічна доба
     */
    fun find(
        records: List<SlotUsage>,
        coveredDays: Set<LocalDate>,
        lastLogicalDay: LocalDate,
        cfg: RiskConfig = RiskConfig()
    ): RiskResult {
        val days = (cfg.observationDays - 1 downTo 0).map { lastLogicalDay.minusDays(it.toLong()) }
        // Логічна доба D = календарний D з межі доби + ранок D+1 до межі: покрита, лише якщо покриті обидві дати.
        val covered = days.filter { it in coveredDays && it.plusDays(1) in coveredDays }
        if (covered.size < cfg.minObservedDays) return RiskResult(days, covered.size, false, emptyList())

        val grid = covered.associateWith { IntArray(SLOTS_PER_DAY) }
        for (r in records) grid[logicalDate(r.date, r.slot, cfg)]?.let { it[position(r.slot, cfg)] += r.seconds }

        val maxGapSlots = cfg.mergeGapMinutes / SLOT_MINUTES
        val masks = covered.associateWith { mergedMask(grid.getValue(it), cfg, maxGapSlots) }
        val perSlotDays = IntArray(SLOTS_PER_DAY) { p -> masks.values.count { it[p] } }

        val accepted = seedRuns(perSlotDays, cfg.seedMinDays, maxGapSlots).mapNotNull { (a, b) ->
            val length = b - a + 1
            val hits = covered.filter { day ->
                (a..b).count { masks.getValue(day)[it] } >= length * cfg.overlapShare
            }
            if (hits.size < cfg.minRecurrenceDays) return@mapNotNull null
            val avg = hits.map { day -> (a..b).sumOf { grid.getValue(day)[it] } / 60.0 }.average()
            if (avg < cfg.minAvgMinutes) return@mapNotNull null
            val startPos = a * SLOT_MINUTES / cfg.roundToMinutes * cfg.roundToMinutes
            val endPos = minOf(1440, ceilDiv((b + 1) * SLOT_MINUTES, cfg.roundToMinutes) * cfg.roundToMinutes)
            Accepted(startPos, endPos, hits, avg)
        }.sortedByDescending { it.avg }

        val chosen = mutableListOf<Accepted>()
        for (c in accepted) {
            if (chosen.size >= cfg.maxWindows) break
            if (chosen.none { c.startPos < it.endPos && it.startPos < c.endPos }) chosen += c
        }
        val offset = cfg.dayCutoffHour * 60
        return RiskResult(
            days, covered.size, true,
            chosen.map {
                RiskCandidate((offset + it.startPos) % 1440, (offset + it.endPos) % 1440, it.hits.size, covered.size, it.avg.toInt(), it.hits)
            }
        )
    }

    /** Хвилини в стрічках по годинах логічної доби (колонка 0 = година межі доби), для теплової карти. */
    fun hourlyMinutes(records: List<SlotUsage>, days: List<LocalDate>, cfg: RiskConfig = RiskConfig()): Array<IntArray> {
        val rows = days.associateWith { IntArray(24) }
        val perHour = SLOTS_PER_DAY / 24
        for (r in records) rows[logicalDate(r.date, r.slot, cfg)]?.let { it[position(r.slot, cfg) / perHour] += r.seconds }
        return days.map { d -> IntArray(24) { rows.getValue(d)[it] / 60 } }.toTypedArray()
    }

    private fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b

    /** Позначає слоти, що входять до склеєних активних інтервалів доби. */
    private fun mergedMask(secs: IntArray, cfg: RiskConfig, maxGapSlots: Int): BooleanArray {
        val mask = BooleanArray(SLOTS_PER_DAY)
        var last = -1
        for (p in 0 until SLOTS_PER_DAY) {
            if (secs[p] < cfg.activeSlotSeconds) continue
            if (last >= 0 && p - last - 1 <= maxGapSlots) for (q in last..p) mask[q] = true else mask[p] = true
            last = p
        }
        return mask
    }

    /** Послідовності слотів, що світились щонайменше [minDays] днів; проміжки до [maxGapSlots] склеюються. */
    private fun seedRuns(perSlotDays: IntArray, minDays: Int, maxGapSlots: Int): List<Pair<Int, Int>> {
        val runs = mutableListOf<Pair<Int, Int>>()
        var p = 0
        while (p < perSlotDays.size) {
            if (perSlotDays[p] < minDays) { p++; continue }
            var end = p
            var q = p + 1
            while (q < perSlotDays.size && q - end - 1 <= maxGapSlots) {
                if (perSlotDays[q] >= minDays) end = q
                q++
            }
            runs += p to end
            p = end + 1
        }
        return runs
    }
}

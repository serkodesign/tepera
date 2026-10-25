package com.serkodesign.tepera.domain.risk

import java.time.LocalDate

/**
 * v3.0 MVP, Етап 5/7: скільки хвилин у застосунках-стрічках припадає на вікно за день. Чиста логіка без Android.
 * Це лише вимірюваний факт («хвилини у вікні»), без оцінки того, чи людина трималась плану.
 */
object WindowMinutes {

    /** Чи слот [slot] календарної дати лежить усередині вікна [startMinute]..[endMinute] (за логічною добою). */
    fun slotInWindow(slot: Int, startMinute: Int, endMinute: Int, cfg: RiskConfig = RiskConfig()): Boolean {
        val first = RiskWindowFinder.startPosition(startMinute, cfg) / RiskWindowFinder.SLOT_MINUTES
        val count = (RiskWindowFinder.lengthMinutes(startMinute, endMinute) + RiskWindowFinder.SLOT_MINUTES - 1) /
            RiskWindowFinder.SLOT_MINUTES
        return RiskWindowFinder.position(slot, cfg) in first until first + count
    }

    /**
     * Середня кількість хвилин у вікні за логічну добу серед [logicalDays]. Для вікна «лише будні» вихідні не рахуються.
     * `null`, якщо жодної доби для розрахунку нема. [records] можуть містити слоти й поза [logicalDays] — вони ігноруються.
     */
    fun averagePerDay(
        records: List<SlotUsage>,
        logicalDays: List<LocalDate>,
        startMinute: Int,
        endMinute: Int,
        dayType: DayType,
        cfg: RiskConfig = RiskConfig()
    ): Double? {
        val counted = logicalDays.filter { dayType == DayType.ALL || it.dayOfWeek.value <= 5 }.toSet()
        if (counted.isEmpty()) return null
        var seconds = 0L
        for (r in records) {
            if (RiskWindowFinder.logicalDate(r.date, r.slot, cfg) in counted && slotInWindow(r.slot, startMinute, endMinute, cfg)) {
                seconds += r.seconds
            }
        }
        return seconds / 60.0 / counted.size
    }
}

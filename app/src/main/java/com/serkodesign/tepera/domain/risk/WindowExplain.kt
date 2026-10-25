package com.serkodesign.tepera.domain.risk

import java.time.DayOfWeek
import java.time.LocalDate

/** Секунди одного застосунку в одному слоті календарної дати (для пояснення «чому це вікно»). */
data class PackageSlotUsage(val date: LocalDate, val slot: Int, val packageName: String, val seconds: Int)

/**
 * v3.0 MVP, Етап 3, M6 «Чому саме це вікно?»: чиста логіка пояснення — які застосунки домінують у вікні й у які дні
 * вікна не було. Лише вимірювані факти, без оцінок.
 */
object WindowExplain {

    /**
     * Середні хвилини на добу по застосунках у вікні [startMinute]–[endMinute] за [hitDays] (дні, коли вікно
     * повторилось), за спаданням; застосунки з нулем хвилин після округлення пропускаються.
     */
    fun minutesPerApp(
        rows: List<PackageSlotUsage>,
        hitDays: List<LocalDate>,
        startMinute: Int,
        endMinute: Int,
        cfg: RiskConfig = RiskConfig()
    ): List<Pair<String, Int>> {
        if (hitDays.isEmpty()) return emptyList()
        val days = hitDays.toSet()
        val seconds = HashMap<String, Long>()
        for (r in rows) {
            if (RiskWindowFinder.logicalDate(r.date, r.slot, cfg) in days && WindowMinutes.slotInWindow(r.slot, startMinute, endMinute, cfg)) {
                seconds.merge(r.packageName, r.seconds.toLong(), Long::plus)
            }
        }
        return seconds.map { (pkg, s) -> pkg to Math.round(s / 60.0 / hitDays.size).toInt() }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
    }

    /**
     * Дні тижня, у які вікна не було, серед покритих логічних діб спостереження: ті, що не в [hitDays]; для «лише
     * будні» вихідні не рахуються. Без повторів, у порядку Пн..Нд.
     */
    fun missedWeekdays(
        coveredLogicalDays: List<LocalDate>,
        hitDays: List<LocalDate>,
        dayType: DayType
    ): List<DayOfWeek> {
        val hits = hitDays.toSet()
        return coveredLogicalDays
            .filter { it !in hits && (dayType == DayType.ALL || it.dayOfWeek.value <= 5) }
            .map { it.dayOfWeek }
            .distinct()
            .sortedBy { it.value }
    }
}

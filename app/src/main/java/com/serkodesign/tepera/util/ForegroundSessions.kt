package com.serkodesign.tepera.util

import android.app.usage.UsageEvents

/**
 * Сесії переднього плану з подій [UsageEvents] — спільне для Online-часу (BalanceRepository) і теплового
 * патерну (PatternRepository), щоб обидва рахували однаково.
 *
 * Застосунок вважається видимим, поки є хоча б одна відкрита його активність: ACTIVITY_RESUMED (1) збільшує
 * лічильник видимих активностей, ACTIVITY_PAUSED (2) — зменшує. Сесія починається, коли лічильник з 0 стає 1,
 * і закінчується, коли повертається в 0. Так застосунок з кількома активностями не обривається, коли одна з них
 * призупинена, а друга лишається на екрані (раніше сесія закривалась на першу ж паузу — занижувала час, напр.
 * Instagram 7 хв замість ~68 хв за добу на S23).
 *
 * Вимкнення екрана (16) і блокування (17) закривають усі відкриті сесії: від застосунку, що не отримав
 * PAUSED (напр. G84, 409 хв "висячої" сесії), інакше час тягнувся б до "зараз".
 *
 * ACTIVITY_STOPPED (23) навмисно не закриває сесію: зупинка активності йде після її паузи, тож закриття на ній
 * давало б подвійний облік у бік занижень.
 */
fun foregroundSessions(events: UsageEvents, excluded: Set<String>, to: Long): List<TimeSpan> {
    val spans = mutableListOf<TimeSpan>()
    val visibleCount = HashMap<String, Int>()
    val sessionStart = HashMap<String, Long>()
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        when (event.eventType) {
            // 16 = SCREEN_NON_INTERACTIVE, 17 = KEYGUARD_SHOWN: відкриті сесії закінчуються тут.
            16, 17 -> {
                sessionStart.forEach { (_, start) -> spans.add(TimeSpan(start, event.timeStamp)) }
                sessionStart.clear()
                visibleCount.clear()
                continue
            }
        }
        val packageName = event.packageName ?: continue
        if (packageName in excluded) continue
        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> { // = ACTIVITY_RESUMED
                val count = visibleCount[packageName] ?: 0
                visibleCount[packageName] = count + 1
                if (count == 0) sessionStart[packageName] = event.timeStamp
            }
            UsageEvents.Event.MOVE_TO_BACKGROUND -> { // = ACTIVITY_PAUSED
                val count = visibleCount[packageName] ?: 0
                if (count <= 0) continue // PAUSED без відповідного RESUMED (вікно вибірки почалось всередині сесії)
                visibleCount[packageName] = count - 1
                if (count == 1) {
                    sessionStart.remove(packageName)?.let { spans.add(TimeSpan(it, event.timeStamp)) }
                }
            }
        }
    }
    // Застосунок, що й досі на передньому плані на момент "to", рахуємо до "to".
    sessionStart.forEach { (_, start) -> spans.add(TimeSpan(start, to)) }
    return spans
}

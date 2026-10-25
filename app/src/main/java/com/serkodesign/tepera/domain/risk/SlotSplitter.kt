package com.serkodesign.tepera.domain.risk

import java.time.Instant
import java.time.ZoneId

/**
 * Ділить проміжок [startMillis, endMillis) по межах 15-хвилинних слотів за настінним годинником зони [zone].
 * Кроки рахуються в реальних хвилинах, тож доба зі зміною часу (25 або 23 год) нічого не губить: повторена година
 * осені накопичує час у тих самих слотах двічі (обмеження слота — в [RiskWindowFinder]), пропущена навесні слотів не має.
 *
 * [add] отримує календарну дату (epoch-day) слота, індекс слота 0..95 і мілісекунди.
 */
object SlotSplitter {
    private const val SLOT_MINUTES = 15

    fun split(startMillis: Long, endMillis: Long, zone: ZoneId, add: (epochDay: Long, slot: Int, millis: Long) -> Unit) {
        var cursor = startMillis
        while (cursor < endMillis) {
            val zdt = Instant.ofEpochMilli(cursor).atZone(zone)
            val slot = (zdt.hour * 60 + zdt.minute) / SLOT_MINUTES
            val slotStart = zdt.withMinute(zdt.minute / SLOT_MINUTES * SLOT_MINUTES).withSecond(0).withNano(0)
            val boundary = slotStart.plusMinutes(SLOT_MINUTES.toLong()).toInstant().toEpochMilli()
            val segmentEnd = minOf(endMillis, boundary)
            add(zdt.toLocalDate().toEpochDay(), slot, segmentEnd - cursor)
            cursor = segmentEnd
        }
    }
}

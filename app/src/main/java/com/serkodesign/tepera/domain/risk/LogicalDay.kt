package com.serkodesign.tepera.domain.risk

import java.time.Instant
import java.time.ZoneId

/** Логічна доба v3: межа о [CUTOFF_HOUR]:00, тож вечір 23:30–00:30 — одна доба. Чиста логіка, без Android. */
object LogicalDay {
    const val CUTOFF_HOUR = 4L

    fun epochDay(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(nowMillis).atZone(zone).minusHours(CUTOFF_HOUR).toLocalDate().toEpochDay()
}

/** М'який вихід: «вчасно» = вихід не пізніше ніж за [GRACE_MILLIS] після строку сесії. */
object SoftExitTiming {
    const val GRACE_MILLIS = 2 * 60_000L

    fun isOnTime(deadlineMillis: Long, nowMillis: Long) = nowMillis <= deadlineMillis + GRACE_MILLIS
}

package com.serkodesign.tepera.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * DatePicker працює в UTC-півночі, застосунок — у локальній півночі. Раніше початкова дата передавалась без конвертації, і на
 * схід від UTC (Київ) picker показував попередній день. Перевіряємо круговий шлях у поясах по обидва боки від UTC.
 */
class DatePickerConversionTest {
    private lateinit var original: TimeZone

    @Before fun saveZone() { original = TimeZone.getDefault() }
    @After fun restoreZone() { TimeZone.setDefault(original) }

    private fun localMidnight(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply { clear(); set(year, month, day, 0, 0, 0) }.timeInMillis

    @Test fun roundTripKyiv() = roundTrip("Europe/Kyiv")
    @Test fun roundTripNewYork() = roundTrip("America/New_York")
    @Test fun roundTripAuckland() = roundTrip("Pacific/Auckland")

    private fun roundTrip(zone: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        val local = localMidnight(2026, Calendar.SEPTEMBER, 21)
        val utc = localStartOfDayToUtcMidnight(local)
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
        // picker бачить саме 21 вересня
        assertEquals(21, utcCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SEPTEMBER, utcCal.get(Calendar.MONTH))
        // і зворотна конвертація повертає ту саму локальну північ
        assertEquals(local, utcMidnightToLocalStartOfDay(utc))
    }
}

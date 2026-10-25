package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.RiskCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskScanCodecTest {
    private fun candidate(start: Int, end: Int, dayType: DayType = DayType.ALL) =
        RiskCandidate(start, end, 6, 7, 36, emptyList(), dayType)

    private fun snap(key: Long) = ScanSnapshot(key, listOf(candidate(1320, 1410)))

    @Test
    fun `encode and decode keep windows without hit days`() {
        val original = listOf(
            ScanSnapshot(20750, listOf(candidate(1320, 1410), candidate(780, 810, DayType.WEEKDAYS))),
            ScanSnapshot(20757, emptyList())
        )
        assertEquals(original, RiskScanCodec.decode(RiskScanCodec.encode(original)))
    }

    @Test
    fun `blank or broken text gives empty history`() {
        assertEquals(emptyList<ScanSnapshot>(), RiskScanCodec.decode(""))
        assertEquals(emptyList<ScanSnapshot>(), RiskScanCodec.decode("{oops"))
        assertEquals(emptyList<ScanSnapshot>(), RiskScanCodec.decode("""[{"key":1,"windows":[{"start":1}]}]"""))
    }

    @Test
    fun `second run in the same week replaces the last snapshot`() {
        val history = RiskScanCodec.append(listOf(snap(100)), snap(103))
        assertEquals(listOf(103L), history.map { it.key })
    }

    @Test
    fun `runs a week apart are kept as separate weeks and only four are stored`() {
        var history = emptyList<ScanSnapshot>()
        for (week in 0..5) history = RiskScanCodec.append(history, snap(100L + week * 7))
        assertEquals(listOf(114L, 121L, 128L, 135L), history.map { it.key })
    }
}

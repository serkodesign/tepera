package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.domain.risk.DayType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TesterStatsFormatTest {
    private val range = LocalDate.of(2026, 10, 18) to LocalDate.of(2026, 10, 24)

    /** Ті самі шаблони, що `v3_report_*` в `values/strings.xml`; мова тесту нічого не вирішує, перевіряється підстановка. */
    private val uk = ReportText(
        title = "Tepera v3 · статистика тестувальника",
        firstData = "Перші дані: %1\$s; днів покрито: %2\$d; стрічок обрано: %3\$d",
        periods = "Тиждень спостереження: %1\$s; поточний тиждень: %2\$s",
        periodNotSet = "ще не заданий",
        range = "%1\$s — %2\$s",
        noWindows = "Вікон нема",
        window = "Вікно %1\$s–%2\$s (%3\$s): повторюваність %4\$d/%5\$d, ≈%6\$d хв; " +
            "хвилини у вікні за добу: спостереження %7\$s → поточний тиждень %8\$s",
        tagWeekdays = "будні", tagDisabled = "вимкнене", tagPlan = "є план", tagNoPlan = "без плану",
        tagShift = "запропоновано зсув",
        minutes = "%1\$s хв",
        nudges = "Нагадування: заплановано %1\$d, показано %2\$d, «Добре» %3\$d, «Не сьогодні» %4\$d",
        nudgeDelay = "Середня затримка: %1\$s хв",
        note = "Примітка: порівняння орієнтовне."
    )

    private val en = uk.copy(
        title = "Tepera v3 · tester statistics",
        noWindows = "No windows",
        window = "Window %1\$s–%2\$s (%3\$s): recurrence %4\$d/%5\$d, ≈%6\$d min; observation %7\$s → current week %8\$s",
        minutes = "%1\$s min",
        tagPlan = "has a plan"
    )

    private fun stats(windows: List<TesterWindowStat>) = TesterStats(
        firstDataDate = LocalDate.of(2026, 10, 16), coveredDays = 10, feedAppCount = 3,
        baselineRange = range, currentRange = LocalDate.of(2026, 10, 25) to LocalDate.of(2026, 10, 31),
        windows = windows, nudgesScheduled = 4, nudgesShown = 3, nudgesOk = 2, nudgesNotToday = 1
    )

    private fun window(baseline: Double?, current: Double?, dayType: DayType = DayType.ALL) = TesterWindowStat(
        startMinute = 610, endMinute = 680, dayType = dayType, recurrenceDays = 5, observedDays = 7, avgMinutes = 30,
        isUserDisabled = false, hasPlan = true, shiftSuggested = false, baselineMinutes = baseline, currentMinutes = current
    )

    @Test
    fun `report contains window times recurrence minutes and nudge counts`() {
        val text = TesterStatsRepository.format(stats(listOf(window(31.4, 18.0))), uk)
        assertTrue(text.contains("10:10–11:20"))
        assertTrue(text.contains("повторюваність 5/7"))
        assertTrue(text.contains("спостереження 31 хв → поточний тиждень 18 хв"))
        assertTrue(text.contains("заплановано 4, показано 3, «Добре» 2, «Не сьогодні» 1"))
        assertTrue(text.contains("є план"))
        assertTrue(text.contains("Перші дані: 2026-10-16; днів покрито: 10; стрічок обрано: 3"))
    }

    @Test
    fun `missing numbers are shown as dash and weekday windows are tagged`() {
        val text = TesterStatsRepository.format(stats(listOf(window(null, null, DayType.WEEKDAYS))), uk)
        assertTrue(text.contains("спостереження — → поточний тиждень —"))
        assertTrue(text.contains("будні"))
    }

    @Test
    fun `no windows says so and the note is always present`() {
        val text = TesterStatsRepository.format(stats(emptyList()), uk)
        assertTrue(text.contains("Вікон нема"))
        assertTrue(text.contains("орієнтовне"))
        assertFalse(text.contains("Вікно "))
    }

    @Test
    fun `unset observation week uses the translated placeholder`() {
        val text = TesterStatsRepository.format(stats(emptyList()).copy(baselineRange = null), uk)
        assertTrue(text.contains("Тиждень спостереження: ще не заданий; поточний тиждень: 2026-10-25 — 2026-10-31"))
    }

    @Test
    fun `report follows the language of the templates`() {
        val text = TesterStatsRepository.format(stats(listOf(window(31.4, 18.0))), en)
        assertTrue(text.startsWith("Tepera v3 · tester statistics"))
        assertTrue(text.contains("Window 10:10–11:20 (has a plan): recurrence 5/7, ≈30 min; observation 31 min → current week 18 min"))
        assertFalse(text.contains("Вікно"))
    }
}

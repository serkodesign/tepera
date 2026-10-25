package com.serkodesign.tepera.domain.risk

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Golden-фікстури (JSON-файли в `src/test/resources/risk`) — формат навмисно не залежить від Kotlin, щоб ті самі файли
 * можна було прогнати на iOS-реалізації. `rules` розгортаються у слоти: `days` = "all" | "weekdays" |
 * "except:<дата>" | "<дата>"; `calendarOffset` зсуває календарну дату запису (ранок після вечора).
 */
class RiskWindowGoldenTest {

    private fun load(name: String): JSONObject =
        JSONObject(javaClass.getResource("/risk/$name.json")!!.readText())

    private fun hm(minute: Int) = "%02d:%02d".format(minute / 60 % 24, minute % 60)

    private fun check(name: String) {
        val json = load(name)
        val last = LocalDate.parse(json.getString("lastLogicalDay"))
        val week = (6 downTo 0).map { last.minusDays(it.toLong()) }
        val coveredFrom = json.optString("coveredFrom").takeIf { it.isNotEmpty() }?.let(LocalDate::parse) ?: week.first()
        val covered = (week + last.plusDays(1)).filter { !it.isBefore(coveredFrom) }.toSet()

        val records = mutableListOf<SlotUsage>()
        val rules = json.getJSONArray("rules")
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            val days = when (val spec = rule.getString("days")) {
                "all" -> week
                "weekdays" -> week.filter { it.dayOfWeek.value <= 5 }
                else -> if (spec.startsWith("except:")) week.filter { it != LocalDate.parse(spec.removePrefix("except:")) }
                else listOf(LocalDate.parse(spec))
            }
            val offset = rule.optLong("calendarOffset", 0)
            for (d in days) for (s in 0 until rule.getInt("slots")) {
                records += SlotUsage(d.plusDays(offset), rule.getInt("startSlot") + s, rule.getInt("seconds"))
            }
        }

        val result = RiskWindowFinder.find(records, covered, last)
        val expected: JSONArray = json.getJSONArray("expected")
        assertEquals("$name: кількість вікон", expected.length(), result.candidates.size)
        for (i in 0 until expected.length()) {
            val e = expected.getJSONObject(i)
            val c = result.candidates[i]
            assertEquals("$name: початок", e.getString("start"), hm(c.startMinute))
            assertEquals("$name: кінець", e.getString("end"), hm(c.endMinute))
            assertEquals("$name: повторюваність", e.getInt("recurrence"), c.recurrenceDays)
            assertEquals("$name: спостережено", e.getInt("observed"), c.observedDays)
            assertEquals("$name: тип днів", DayType.valueOf(e.getString("dayType")), c.dayType)
            assertEquals("$name: середнє", e.getInt("avgMinutes"), c.avgMinutes)
        }
    }

    @Test fun clearEvening() = check("clear_evening_6of7")
    @Test fun lunchOnWeekdaysOnly() = check("lunch_weekdays_only")
    @Test fun chaoticSchedule() = check("chaotic_schedule")
    @Test fun acrossMidnight() = check("across_midnight")
    @Test fun dstFallBackWeek() = check("dst_fall_back_week")
    @Test fun lessThanWeekOfData() = check("less_than_week")
}

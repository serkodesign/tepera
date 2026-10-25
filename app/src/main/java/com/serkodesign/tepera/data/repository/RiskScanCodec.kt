package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.RiskCandidate
import org.json.JSONArray
import org.json.JSONObject

/** Результат одного тижневого перерахунку: [key] — epoch-day останньої логічної доби спостереження. */
data class ScanSnapshot(val key: Long, val candidates: List<RiskCandidate>)

/**
 * v3.0 MVP: зберігає останні тижневі результати пошуку вікон (для виявлення зсуву: «початок зміщений два тижні
 * поспіль»). Формат — JSON-рядок; `hitDays` кандидатів не зберігається (для порівняння тижнів не потрібен).
 */
object RiskScanCodec {
    private const val KEEP = 4

    fun encode(snapshots: List<ScanSnapshot>): String = JSONArray().apply {
        snapshots.forEach { s ->
            put(
                JSONObject().put("key", s.key).put(
                    "windows",
                    JSONArray().apply {
                        s.candidates.forEach { c ->
                            put(
                                JSONObject()
                                    .put("start", c.startMinute).put("end", c.endMinute)
                                    .put("recurrence", c.recurrenceDays).put("observed", c.observedDays)
                                    .put("avg", c.avgMinutes).put("dayType", c.dayType.name)
                            )
                        }
                    }
                )
            )
        }
    }.toString()

    /** Порожній або пошкоджений рядок дає порожній список — знімки відновляться наступними перерахунками. */
    fun decode(raw: String): List<ScanSnapshot> = try {
        if (raw.isBlank()) emptyList() else JSONArray(raw).let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ws = o.getJSONArray("windows")
                ScanSnapshot(
                    o.getLong("key"),
                    (0 until ws.length()).map { j ->
                        val w = ws.getJSONObject(j)
                        RiskCandidate(
                            w.getInt("start"), w.getInt("end"), w.getInt("recurrence"), w.getInt("observed"),
                            w.getInt("avg"), emptyList(), DayType.valueOf(w.getString("dayType"))
                        )
                    }
                )
            }
        }
    } catch (e: org.json.JSONException) {
        emptyList()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }

    /**
     * Додає [snapshot]. Повторний запуск у межах [sameWeekDays] днів від попереднього знімка (ручний перерахунок)
     * замінює його, а не рахується окремим тижнем. Зберігає останні чотири.
     */
    fun append(history: List<ScanSnapshot>, snapshot: ScanSnapshot, sameWeekDays: Int = 5): List<ScanSnapshot> {
        val last = history.lastOrNull()
        val base = if (last != null && snapshot.key - last.key < sameWeekDays) history.dropLast(1) else history
        return (base + snapshot).takeLast(KEEP)
    }
}

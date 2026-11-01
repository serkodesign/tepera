package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.entity.FeedAppEntity
import com.serkodesign.tepera.data.local.entity.NudgeAction
import com.serkodesign.tepera.data.local.entity.NudgeEventEntity
import com.serkodesign.tepera.data.local.entity.ObservationPeriodEntity
import com.serkodesign.tepera.data.local.entity.PlanEntity
import com.serkodesign.tepera.data.local.entity.RiskWindowEntity
import com.serkodesign.tepera.data.local.entity.UsageSlotEntity
import com.serkodesign.tepera.domain.risk.DayType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Туди-назад для v3-частини резервної копії: все, що експортується, імпортується без втрат. */
class V3BackupJsonTest {

    private fun roundTrip(o: JSONObject) = JSONObject(o.toString())

    @Test
    fun feedApp_roundTrip() {
        val a = FeedAppEntity("com.instagram.android", "Instagram", 1_700_000_000_000)
        assertEquals(a, V3BackupJson.feedAppFromJson(roundTrip(V3BackupJson.feedAppToJson(a))))
    }

    @Test
    fun usageSlot_roundTrip() {
        val s = UsageSlotEntity(date = 20_400, slot = 77, packageName = "com.google.android.youtube", seconds = 612)
        assertEquals(s, V3BackupJson.usageSlotFromJson(roundTrip(V3BackupJson.usageSlotToJson(s))))
    }

    @Test
    fun riskWindow_roundTrip_withAllOptionalFields() {
        val w = RiskWindowEntity(
            id = "w1", startMinute = 20 * 60, endMinute = 22 * 60 + 30, dayType = DayType.WEEKDAYS, recurrenceDays = 4,
            observedDays = 7, avgMinutes = 50, topApps = "com.a,com.b", createdAt = 1, updatedAt = 2, isUserDisabled = true,
            suggestedStartMinute = 21 * 60, suggestedEndMinute = 23 * 60, isManual = true, pausedUntil = 99L, pausedByUser = true
        )
        assertEquals(w, V3BackupJson.riskWindowFromJson(roundTrip(V3BackupJson.riskWindowToJson(w))))
    }

    @Test
    fun riskWindow_roundTrip_withNulls() {
        val w = RiskWindowEntity(
            id = "w2", startMinute = 0, endMinute = 60, dayType = DayType.ALL, recurrenceDays = 3, observedDays = 7,
            avgMinutes = 10, topApps = "", createdAt = 1, updatedAt = 1, isUserDisabled = false
        )
        val back = V3BackupJson.riskWindowFromJson(roundTrip(V3BackupJson.riskWindowToJson(w)))
        assertEquals(w, back)
        assertNull(back.pausedUntil)
        assertNull(back.suggestedStartMinute)
    }

    @Test
    fun unknownDayType_fallsBackToAll() {
        val o = V3BackupJson.riskWindowToJson(
            RiskWindowEntity("w3", 0, 60, DayType.WEEKDAYS, 3, 7, 10, "", 1, 1, false)
        ).put("dayType", "SOMETHING_FROM_THE_FUTURE")
        assertEquals(DayType.ALL, V3BackupJson.riskWindowFromJson(roundTrip(o)).dayType)
    }

    @Test
    fun plan_roundTrip() {
        val p = PlanEntity("p1", "w1", "Коли відкриваю стрічку", "відкладу телефон і вип'ю води", 1, 2)
        assertEquals(p, V3BackupJson.planFromJson(roundTrip(V3BackupJson.planToJson(p))))
    }

    @Test
    fun nudgeEvent_roundTrip_includingNotShown() {
        val shown = NudgeEventEntity("n1", "w1", 1000, 1500, NudgeAction.OK)
        val notShown = NudgeEventEntity("n2", "w1", 2000, null, NudgeAction.NONE)
        assertEquals(shown, V3BackupJson.nudgeEventFromJson(roundTrip(V3BackupJson.nudgeEventToJson(shown))))
        assertEquals(notShown, V3BackupJson.nudgeEventFromJson(roundTrip(V3BackupJson.nudgeEventToJson(notShown))))
    }

    @Test
    fun unknownNudgeAction_fallsBackToNone() {
        val o = V3BackupJson.nudgeEventToJson(NudgeEventEntity("n3", "w1", 1, null, NudgeAction.OK)).put("action", "FUTURE")
        assertEquals(NudgeAction.NONE, V3BackupJson.nudgeEventFromJson(roundTrip(o)).action)
    }

    @Test
    fun observationPeriod_roundTrip_openAndClosed() {
        val open = ObservationPeriodEntity("o1", 20_000, null)
        val closed = ObservationPeriodEntity("o2", 20_000, 20_006)
        assertEquals(open, V3BackupJson.observationPeriodFromJson(roundTrip(V3BackupJson.observationPeriodToJson(open))))
        assertEquals(closed, V3BackupJson.observationPeriodFromJson(roundTrip(V3BackupJson.observationPeriodToJson(closed))))
    }

    @Test
    fun mapObjects_nullArray_isEmpty() {
        val result = with(V3BackupJson) { (null as org.json.JSONArray?).mapObjects { it.toString() } }
        assertEquals(emptyList<String>(), result)
    }
}

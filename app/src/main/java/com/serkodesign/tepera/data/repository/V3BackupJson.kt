package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.entity.FeedAppEntity
import com.serkodesign.tepera.data.local.entity.NudgeAction
import com.serkodesign.tepera.data.local.entity.NudgeEventEntity
import com.serkodesign.tepera.data.local.entity.ObservationPeriodEntity
import com.serkodesign.tepera.data.local.entity.PlanEntity
import com.serkodesign.tepera.data.local.entity.RiskWindowEntity
import com.serkodesign.tepera.data.local.entity.UsageSlotEntity
import com.serkodesign.tepera.domain.risk.DayType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Серіалізація v3-таблиць для JSON-резервної копії (схема 3). Окремо від [BackupRepository], щоб перетворення
 * туди-назад можна було перевірити юніт-тестом без бази й пристрою.
 */
internal object V3BackupJson {

    /** Читає JSON-масив у список; `null` (ключа немає) — порожній список. */
    fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else List(length()) { transform(getJSONObject(it)) }

    fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
    fun JSONObject.optNullableInt(key: String): Int? = if (isNull(key)) null else getInt(key)

    fun feedAppToJson(a: FeedAppEntity) = JSONObject().apply {
        put("packageName", a.packageName)
        put("label", a.label)
        put("confirmedAt", a.confirmedAt)
    }

    fun feedAppFromJson(o: JSONObject) = FeedAppEntity(
        packageName = o.getString("packageName"),
        label = o.getString("label"),
        confirmedAt = o.getLong("confirmedAt")
    )

    fun usageSlotToJson(s: UsageSlotEntity) = JSONObject().apply {
        put("date", s.date)
        put("slot", s.slot)
        put("packageName", s.packageName)
        put("seconds", s.seconds)
    }

    fun usageSlotFromJson(o: JSONObject) = UsageSlotEntity(
        date = o.getLong("date"),
        slot = o.getInt("slot"),
        packageName = o.getString("packageName"),
        seconds = o.getInt("seconds")
    )

    fun riskWindowToJson(w: RiskWindowEntity) = JSONObject().apply {
        put("id", w.id)
        put("startMinute", w.startMinute)
        put("endMinute", w.endMinute)
        put("dayType", w.dayType.name)
        put("recurrenceDays", w.recurrenceDays)
        put("observedDays", w.observedDays)
        put("avgMinutes", w.avgMinutes)
        put("topApps", w.topApps)
        put("createdAt", w.createdAt)
        put("updatedAt", w.updatedAt)
        put("isUserDisabled", w.isUserDisabled)
        put("suggestedStartMinute", w.suggestedStartMinute ?: JSONObject.NULL)
        put("suggestedEndMinute", w.suggestedEndMinute ?: JSONObject.NULL)
        put("isManual", w.isManual)
        put("pausedUntil", w.pausedUntil ?: JSONObject.NULL)
        put("pausedByUser", w.pausedByUser)
    }

    fun riskWindowFromJson(o: JSONObject) = RiskWindowEntity(
        id = o.getString("id"),
        startMinute = o.getInt("startMinute"),
        endMinute = o.getInt("endMinute"),
        // Форвард-сумісність: невідомий тип днів з майбутнього формату не ламає імпорт.
        dayType = runCatching { DayType.valueOf(o.getString("dayType")) }.getOrDefault(DayType.ALL),
        recurrenceDays = o.getInt("recurrenceDays"),
        observedDays = o.getInt("observedDays"),
        avgMinutes = o.getInt("avgMinutes"),
        topApps = o.getString("topApps"),
        createdAt = o.getLong("createdAt"),
        updatedAt = o.getLong("updatedAt"),
        isUserDisabled = o.getBoolean("isUserDisabled"),
        suggestedStartMinute = o.optNullableInt("suggestedStartMinute"),
        suggestedEndMinute = o.optNullableInt("suggestedEndMinute"),
        isManual = o.optBoolean("isManual", false),
        pausedUntil = o.optNullableLong("pausedUntil"),
        pausedByUser = o.optBoolean("pausedByUser", false)
    )

    fun planToJson(p: PlanEntity) = JSONObject().apply {
        put("id", p.id)
        put("windowId", p.windowId)
        put("ifText", p.ifText)
        put("thenText", p.thenText)
        put("createdAt", p.createdAt)
        put("updatedAt", p.updatedAt)
    }

    fun planFromJson(o: JSONObject) = PlanEntity(
        id = o.getString("id"),
        windowId = o.getString("windowId"),
        ifText = o.getString("ifText"),
        thenText = o.getString("thenText"),
        createdAt = o.getLong("createdAt"),
        updatedAt = o.getLong("updatedAt")
    )

    fun nudgeEventToJson(e: NudgeEventEntity) = JSONObject().apply {
        put("id", e.id)
        put("windowId", e.windowId)
        put("scheduledAt", e.scheduledAt)
        put("shownAt", e.shownAt ?: JSONObject.NULL)
        put("action", e.action.name)
    }

    fun nudgeEventFromJson(o: JSONObject) = NudgeEventEntity(
        id = o.getString("id"),
        windowId = o.getString("windowId"),
        scheduledAt = o.getLong("scheduledAt"),
        shownAt = o.optNullableLong("shownAt"),
        action = runCatching { NudgeAction.valueOf(o.getString("action")) }.getOrDefault(NudgeAction.NONE)
    )

    fun observationPeriodToJson(p: ObservationPeriodEntity) = JSONObject().apply {
        put("id", p.id)
        put("startDate", p.startDate)
        put("endDate", p.endDate ?: JSONObject.NULL)
    }

    fun observationPeriodFromJson(o: JSONObject) = ObservationPeriodEntity(
        id = o.getString("id"),
        startDate = o.getLong("startDate"),
        endDate = o.optNullableLong("endDate")
    )
}

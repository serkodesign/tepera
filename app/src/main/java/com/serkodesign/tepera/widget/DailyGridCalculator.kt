package com.serkodesign.tepera.widget

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity

private const val SLOT_MINUTES = 30
const val DAILY_GRID_SLOT_COUNT = 48
private const val SLOT_MILLIS = SLOT_MINUTES * 60_000L

/**
 * Джерело кольору клітинки сітки доби (Figma node 11:647, "перемалюй віджет" — замінює колишню
 * шкалу "Твій день"). Чиста арифметика без Android/Glance-залежностей, той самий принцип, що
 * util/SleepWindowCalculator.kt — тестується без пристрою.
 */
sealed class DailyGridSlot {
    /** До точки старту дня (calculateDayStartMillis) — за прямим рішенням користувача, окремий
     *  колір (#A172FF), не "звичайний" Blank. */
    data object PreUnlock : DailyGridSlot()
    data class Category(val categoryId: String) : DailyGridSlot()
    data object Online : DailyGridSlot()
    /** Нічого не залоговано. [isFuture] розрізняє "уже минуло, але порожньо" (непрозорий білий,
     *  Figma рядки 2-3) від "ще не настало" (напівпрозорий білий, Figma рядок 4). */
    data class Blank(val isFuture: Boolean) : DailyGridSlot()
}

/**
 * Рахує 48 клітинок сітки доби (12×4, по 30 хв) від КАЛЕНДАРНОЇ півночі [calendarMidnightMillis] —
 * свідомо не від точки старту дня Tepera (calculateDayStartMillis), за прямим рішенням користувача
 * під час перемальовування віджета: сітка завжди починається з 00:00, а [dayStartMillis] лише
 * позначає межу PreUnlock-клітинок (до пробудження). Слот, чия середина ще ДО [dayStartMillis],
 * зафарбовується як PreUnlock незалежно від наявних даних. Слот, що ще не почався
 * ([slotStart] >= [nowMillis]), завжди Blank(isFuture = true).
 *
 * Пріоритет джерела в межах одного слота: ручна категорія з найбільшим перекриттям (хвилини) >
 * Online > Blank — той самий принцип, що структура доби на Home (ручний запис — свідомий вибір
 * користувача, важливіший за автоматичний Online-підрахунок).
 */
fun calculateDailyGridSlots(
    calendarMidnightMillis: Long,
    dayStartMillis: Long,
    nowMillis: Long,
    entries: List<ActivityEntryEntity>,
    onlineMinutesPerSlot: IntArray
): List<DailyGridSlot> = (0 until DAILY_GRID_SLOT_COUNT).map { index ->
    val slotStart = calendarMidnightMillis + index * SLOT_MILLIS
    val slotEnd = slotStart + SLOT_MILLIS
    val slotMid = slotStart + SLOT_MILLIS / 2

    when {
        slotMid < dayStartMillis -> DailyGridSlot.PreUnlock
        slotStart >= nowMillis -> DailyGridSlot.Blank(isFuture = true)
        else -> {
            val categoryId = dominantCategory(entries, slotStart, slotEnd)
            when {
                categoryId != null -> DailyGridSlot.Category(categoryId)
                onlineMinutesPerSlot.getOrElse(index) { 0 } > 0 -> DailyGridSlot.Online
                else -> DailyGridSlot.Blank(isFuture = false)
            }
        }
    }
}

/** Категорія з найбільшим перекриттям (у хвилинах) з [slotStart, slotEnd); null, якщо жодна не перекривається. */
private fun dominantCategory(entries: List<ActivityEntryEntity>, slotStart: Long, slotEnd: Long): String? {
    val minutesByCategory = HashMap<String, Int>()
    for (entry in entries) {
        val entryEnd = entry.startTime + entry.durationMinutes * 60_000L
        val overlapStart = maxOf(entry.startTime, slotStart)
        val overlapEnd = minOf(entryEnd, slotEnd)
        if (overlapEnd <= overlapStart) continue
        val overlapMinutes = ((overlapEnd - overlapStart) / 60_000L).toInt()
        minutesByCategory[entry.categoryId] = (minutesByCategory[entry.categoryId] ?: 0) + overlapMinutes
    }
    return minutesByCategory.maxByOrNull { it.value }?.key
}

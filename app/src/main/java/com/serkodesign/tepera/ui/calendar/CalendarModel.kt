package com.serkodesign.tepera.ui.calendar

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class CalendarMode { MONTH, WEEK }

/** Один стікер = один запис активності за день. */
data class CalendarSticker(val entry: ActivityEntryEntity, val category: CategoryEntity)

/** Форми стікерів — вибираються за id запису, тож той самий запис завжди виглядає однаково. */
enum class StickerShape { CIRCLE, SQUIRCLE, COOKIE, FLOWER, FOLDED, HEART }

/** Усі дні місяця плюс сусідні, щоб сітка складалась із повних тижнів (5 або 6 рядків). */
fun monthGrid(month: YearMonth, firstDay: DayOfWeek): List<LocalDate> {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - firstDay.value + 7) % 7
    val start = first.minusDays(lead.toLong())
    val total = lead + month.lengthOfMonth()
    val weeks = (total + 6) / 7
    return List(weeks * 7) { start.plusDays(it.toLong()) }
}

fun weekDays(date: LocalDate, firstDay: DayOfWeek): List<LocalDate> {
    val lead = (date.dayOfWeek.value - firstDay.value + 7) % 7
    val start = date.minusDays(lead.toLong())
    return List(7) { start.plusDays(it.toLong()) }
}

/** Групує записи за календарним днем початку (той самий принцип, що Щоденник). */
fun groupStickersByDay(
    entries: List<ActivityEntryEntity>,
    categories: List<CategoryEntity>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, List<CalendarSticker>> {
    val byId = categories.associateBy { it.id }
    return entries
        .mapNotNull { e -> byId[e.categoryId]?.let { CalendarSticker(e, it) } }
        .sortedBy { it.entry.startTime }
        .groupBy { Instant.ofEpochMilli(it.entry.startTime).atZone(zone).toLocalDate() }
}

fun stickerShapeFor(id: String): StickerShape =
    StickerShape.values()[Math.floorMod(id.hashCode(), StickerShape.values().size)]

/** Нахил від -6 до +6 градусів — «наліплено нашвидкуруч». */
fun stickerTiltFor(id: String): Float = Math.floorMod(id.hashCode() / 7, 13) - 6f

/** Тривалість у форматі «г:хх»: 15 хв → «0:15», 75 хв → «1:15», 120 хв → «2:00». */
fun formatDurationClock(minutes: Int): String {
    val m = minutes.coerceAtLeast(0)
    return "${m / 60}:${(m % 60).toString().padStart(2, '0')}"
}

/** Висота області стікерів у клітинці місяця (dp): клітинка 88 мінус номер дня й відступи. */
const val MONTH_STICKER_AREA_HEIGHT = 58f

/** Висота клітинки місяця (dp). */
const val MONTH_CELL_HEIGHT = 88f

/** Розкладка стікерів у клітинці місяця: розмір, позиції (dp), скільки показати і чи потрібен кружок «+N». */
data class StickerSlots(val size: Float, val slots: List<Pair<Float, Float>>, val shown: Int, val more: Boolean)

/**
 * До 4 стікерів (30dp) — по кутах із помітним перекриттям. 5–6 — купка 24dp у дві колонки, що накладаються
 * (як стікери, наліплені гуртом). Більше 6 — 5 стікерів і кружок «+N» на шостому місці.
 */
fun monthStickerSlots(count: Int, width: Float): StickerSlots {
    if (count <= 4) {
        val s = minOf(width * 0.7f, 30f)
        val right = width - s
        val slots = listOf(0f to 0f, right to 28f, right to 2f, 0f to 28f)
        return StickerSlots(s, slots, count, false)
    }
    val t = minOf(width * 0.55f, 24f)
    val step = 15f
    val right = width - t
    val slots = listOf(0f to 0f, right to 2f, 0f to step, right to step + 2f, 0f to 2 * step, right to 2 * step + 2f)
    return if (count <= 6) StickerSlots(t, slots, count, false) else StickerSlots(t, slots, 5, true)
}

/** Як вписати значок категорії у форму стікера: частка розміру й вертикальний зсув (частка розміру). */
data class GlyphFit(val scale: Float, val dy: Float)

/**
 * Серце займає не весь квадрат (нижній виріз), тож значок на ньому менший і трохи вище центру.
 * Решта форм заповнюють квадрат повністю.
 */
fun glyphFit(shape: StickerShape): GlyphFit = when (shape) {
    StickerShape.HEART -> GlyphFit(0.46f, -0.04f)
    else -> GlyphFit(0.55f, 0f)
}

/** ТИМЧАСОВО: тестові записи для перегляду «купки» стікерів (5–6 за день). Вимкнути, коли не потрібно. */
const val PREVIEW_FAKE_STICKERS = false

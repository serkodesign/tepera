package com.serkodesign.tepera.ui.home

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * Модель hero-картки Home FINAL (docs/HANDOFF.md, розділи 4-5; Figma "App concept", `HomeHero` 678:12959).
 * Чиста Kotlin-логіка без Android-залежностей — тестується без пристрою. Рішення "≤ 4 / > 4" живе тут
 * (HANDOFF, розділ 9), а не в композаблі.
 */

/** Одна активність у hero: назва, колір і хвилини за сьогодні. */
data class HeroActivity(val name: String, val color: Color, val minutes: Int)

/** Відрізок смуги структури доби в частках ширини (0f..1f); [hatched] — ще не прожитий час доби. */
data class StripSegment(val start: Float, val end: Float, val color: Color, val hatched: Boolean = false)

/**
 * Смуга структури доби (той самий принцип, що колишня шкала "Твій день", FR-3.3–3.10): пропорційні сегменти за
 * СУМАРНИМ часом — Online, кожна категорія з ненульовим часом своїм кольором, "Решта дня" — і заштрихована частина
 * ще не прожитого часу до півночі. [targetFraction] — тиха засічка орієнтиру без підпису (FR-3.10), null без орієнтиру.
 */
data class DayStructure(val segments: List<StripSegment>, val targetFraction: Float?) {
    companion object {
        val Empty = DayStructure(emptyList(), null)
    }
}

sealed interface HeroUiState {
    /** hasUsageAccess ще не відомий (перший рендер): скелетон. */
    data object Loading : HeroUiState

    /** Доступ до статистики не надано: пунктирний орб і кнопка "Надати доступ". */
    data object NoAccess : HeroUiState

    /** Початок дня: ще не було перерви ≥ 30 хв (орб порожній, "—"). */
    data class Empty(val dayStartMillis: Long, val elapsedMinutes: Int, val structure: DayStructure) : HeroUiState

    data class Data(
        val dayStartMillis: Long,
        val elapsedMinutes: Int,
        val onlineMinutes: Int,
        /** Відсортовані за тривалістю спадно, не більше [HERO_MAX_ACTIVITIES]. */
        val activities: List<HeroActivity>,
        val structure: DayStructure
    ) : HeroUiState {
        /** "Без телефону" = elapsed − online, ніколи від'ємне. */
        val offlineMinutes: Int get() = (elapsedMinutes - onlineMinutes).coerceAtLeast(0)

        /** Рівень води в орбі = offline / elapsed (HANDOFF, розділ 4). */
        val waterLevel: Float get() = if (elapsedMinutes <= 0) 0f else (offlineMinutes.toFloat() / elapsedMinutes).coerceIn(0f, 1f)

        val percent: Int get() = (waterLevel * 100).roundToInt()

        /** > 4 активностей — згорнутий вигляд з топ-3 і пілюлею "Ще N". */
        val isMany: Boolean get() = activities.size > HERO_MAX_INLINE_ACTIVITIES

        val visibleActivities: List<HeroActivity> get() = if (isMany) activities.take(HERO_COLLAPSED_TOP) else activities

        val hiddenActivities: List<HeroActivity> get() = if (isMany) activities.drop(HERO_COLLAPSED_TOP) else emptyList()

        /** Сума хвилин усіх названих активностей ("названо 3 год 25 хв"). */
        val namedMinutes: Int get() = activities.sumOf { it.minutes }
    }
}

const val HERO_MAX_INLINE_ACTIVITIES = 4
const val HERO_COLLAPSED_TOP = 3

/** До 8 активностей усе вміщується; понад 8 кольори повторюються — показуємо перші 8 (рішення не прийняте, HANDOFF п. 10.2). */
const val HERO_MAX_ACTIVITIES = 8

/** Менше за цей час від початку дня — стан Empty (поріг перерви FR-D.1, "Перерви від 30 хв з'являться тут"). */
const val HERO_EMPTY_BELOW_MINUTES = 30

fun buildHeroState(
    hasUsageAccess: Boolean?,
    dayStartMillis: Long,
    elapsedMinutes: Int,
    onlineMinutes: Int,
    activities: List<HeroActivity>,
    structure: DayStructure
): HeroUiState = when {
    hasUsageAccess == null -> HeroUiState.Loading
    !hasUsageAccess -> HeroUiState.NoAccess
    elapsedMinutes < HERO_EMPTY_BELOW_MINUTES -> HeroUiState.Empty(dayStartMillis, elapsedMinutes, structure)
    else -> HeroUiState.Data(
        dayStartMillis = dayStartMillis,
        elapsedMinutes = elapsedMinutes,
        onlineMinutes = onlineMinutes,
        activities = activities.filter { it.minutes > 0 }.sortedByDescending { it.minutes }.take(HERO_MAX_ACTIVITIES),
        structure = structure
    )
}

/**
 * Структура доби за колишнім принципом шкали "Твій день". Порядок: Online → категорії (у порядку [categories], тобто
 * за `sortOrder`) → "Решта дня" ([restOfDayMinutes]) → заштрихований ще не прожитий час ([daySpanMinutes] −
 * [dayLengthMinutes]). Сегменти можуть перекриватись (Online + запис + офлайн за об'єднанням у сумі більші за довжину
 * дня) — їхні ширини нормалізуються до довжини дня, щоб шкала не виходила за позначку "зараз". Ширина кожного
 * сегмента пропорційна його хвилинам (мінімум 1 хв, щоб короткий не зник). Засічка орієнтиру — частка від повного
 * діапазону шкали ([daySpanMinutes] або орієнтир, якщо він більший), а не від минулої частини дня: інакше вона
 * "стрибала" б праворуч разом з ростом дня.
 */
fun buildDayStructure(
    onlineMinutes: Int,
    categories: List<HeroActivity>,
    restOfDayMinutes: Int,
    dayLengthMinutes: Int,
    daySpanMinutes: Int,
    targetMinutes: Int?,
    onlineColor: Color,
    restColor: Color,
    futureColor: Color
): DayStructure {
    data class Part(val minutes: Float, val color: Color, val hatched: Boolean)

    val parts = buildList {
        if (onlineMinutes > 0) add(Part(onlineMinutes.toFloat(), onlineColor, false))
        categories.filter { it.minutes > 0 }.forEach { add(Part(it.minutes.toFloat(), it.color, false)) }
        if (restOfDayMinutes > 0) add(Part(restOfDayMinutes.toFloat(), restColor, false))
    }
    val total = parts.sumOf { it.minutes.toDouble() }.toFloat()
    val scale = if (total > dayLengthMinutes && total > 0f) dayLengthMinutes / total else 1f
    val weighted = parts.map { it.copy(minutes = (it.minutes * scale).coerceAtLeast(1f)) }.toMutableList()
    val future = (daySpanMinutes - dayLengthMinutes).coerceAtLeast(0)
    if (future > 0) weighted.add(Part(future.toFloat(), futureColor, true))

    val weightSum = weighted.sumOf { it.minutes.toDouble() }.toFloat()
    val segments = if (weightSum <= 0f) {
        emptyList()
    } else {
        var cursor = 0f
        weighted.map { part ->
            val start = cursor / weightSum
            cursor += part.minutes
            StripSegment(start, cursor / weightSum, part.color, part.hatched)
        }
    }
    val reference = maxOf(daySpanMinutes, targetMinutes ?: 0, 1)
    val targetFraction = targetMinutes?.let { (it.toFloat() / reference).coerceIn(0f, 1f) }
    return DayStructure(segments, targetFraction)
}

package com.serkodesign.tepera.util

import kotlin.math.roundToInt

/**
 * Округлення хвилин до найближчих 15 (за запитом користувача — Offline/Online картки на Home
 * і той самий рядок на віджеті показують "3 год 45 хв", а не хвилини з точністю до 1 хв).
 * Повертає пару (години, хвилини-залишок).
 */
fun roundToQuarterHour(minutes: Int): Pair<Int, Int> {
    val rounded = (minutes / 15f).roundToInt() * 15
    return (rounded / 60) to (rounded % 60)
}

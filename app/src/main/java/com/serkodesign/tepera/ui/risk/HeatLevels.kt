package com.serkodesign.tepera.ui.risk

/**
 * Рівні теплової карти дизайн-системи v3.1 (розділ 2.2 плану): `dataHeat0`…`dataHeat5`. Чиста логіка без Android —
 * тестується без пристрою.
 *
 * Стара карта фарбувала кошик бурштином із прозорістю; таблиця плану переводить стару прозорість у рівень:
 * немає даних → 0; ≤ 0.19 → 1; ≤ 0.30 → 2; ≤ 0.60 → 3; ≤ 0.90 → 4; 1.0 → 5.
 */
internal object HeatLevels {
    /** Стара прозорість бурштину для кошиків 1–4 (`HeatGrid.bucket`): 0.15, 0.45, 0.7, 1.0. */
    private val LEGACY_ALPHAS = floatArrayOf(0.15f, 0.45f, 0.7f, 1f)

    /** Рівень 0–5 за старою прозорістю бурштину (`null` — немає даних). */
    fun levelForAlpha(alpha: Float?): Int = when {
        alpha == null || alpha <= 0f -> 0
        alpha <= 0.19f -> 1
        alpha <= 0.30f -> 2
        alpha <= 0.60f -> 3
        alpha <= 0.90f -> 4
        else -> 5
    }

    /** Рівень 0–5 для кошика `HeatGrid.bucket` (0–4): 0→0, 1→1, 2→3, 3→4, 4→5. Рівень 2 цими кошиками не вживається. */
    fun levelForBucket(bucket: Int): Int =
        if (bucket <= 0) 0 else levelForAlpha(LEGACY_ALPHAS[(bucket - 1).coerceAtMost(LEGACY_ALPHAS.lastIndex)])

    /** Рівні, що реально вживаються кошиками 1–4 — для шкали «менше → більше» в легенді. */
    val USED_LEVELS: List<Int> = (1..4).map(::levelForBucket)
}

package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Дизайн-система v3.1: форми M3 (розділ 2.4 плану). Не підключені до теми до ітерації I07.
 * Назва `TeperaShapes` зайнята приватним значенням у Theme.kt, тож тут — `TeperaShapesM3`.
 */
val TeperaShapesM3 = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Форми поза шкалою M3. Правило: картки 16dp (`large`); шторки 28dp лише зверху ([sheetTop]); плашка виходу
 * 28dp (`extraLarge`); кнопки, чипи, бейджі — [full]; snackbar 4dp (`extraSmall`); клітинки хітмапи 2dp ([heatCell]).
 */
object TeperaExtraShapes {
    val largeIncreased = RoundedCornerShape(20.dp)
    val extraLargeIncreased = RoundedCornerShape(32.dp)
    val full = CircleShape
    val sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val heatCell = RoundedCornerShape(2.dp)
}

/**
 * Дизайн-система v3.1: шкала відступів (розділ 2.5 плану) — 2, 4, 8, 12, 16, 20, 24, 32, 40 dp.
 * Значення поза шкалою заборонені. Розміри іконок і висоти компонентів сюди не входять.
 */
object TeperaSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp

    // Семантичні псевдоніми з таблиці 2.5
    val screenHorizontal = l
    val betweenCards = m
    val introToContent = l
    val cardPadding = l
    val cardGroups = m
    val blockRows = xs
    val betweenButtons = s
    val headerHorizontal = l
    val dotsGap = s
}

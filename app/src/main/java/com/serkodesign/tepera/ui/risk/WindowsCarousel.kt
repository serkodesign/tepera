package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SIDE_PADDING = 16.dp
private val CARD_GAP = 12.dp

/**
 * Години скролінгу рядом, що гортається вбік, замість вертикального стека: з двох-трьох вікон екран не росте втричі,
 * а наступна картка трохи визирає з-за краю — видно, що є ще. Одне вікно займає всю ширину. Ряд виходить за бічні
 * відступи батька (16dp) і повертає їх як `contentPadding`, тож перша картка вирівняна з рештою екрана.
 *
 * Усі картки однієї висоти: спершу кожну міряємо з її справжньою шириною, береться найвища, і ряд малюється з цією
 * висотою (картка з рядком «на паузі» вища за звичайну, тож без цього ряд мав би зубчасті краї).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WindowsCarousel(
    windows: List<RiskWindowUi>,
    onOpenWhy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (windows.isEmpty()) return
    val state = rememberLazyListState()
    val density = LocalDensity.current
    SubcomposeLayout(modifier.fullBleed(SIDE_PADDING)) { constraints ->
        val usable = constraints.maxWidth - (SIDE_PADDING * 2).roundToPx()
        val cardWidth = if (windows.size == 1) usable else (usable * 0.84f).toInt()
        val cardWidthDp = with(density) { cardWidth.toDp() }

        val tallest = windows.mapIndexed { i, w ->
            subcompose("measure$i") {
                RiskWindowCard(w, onOpenWhy = {}, modifier = Modifier.width(cardWidthDp))
            }.maxOf { it.measure(Constraints(minWidth = cardWidth, maxWidth = cardWidth)).height }
        }.max()
        val rowHeightDp = with(density) { tallest.toDp() }

        val row = subcompose("row") {
            LazyRow(
                state = state,
                modifier = Modifier.height(rowHeightDp),
                contentPadding = PaddingValues(horizontal = SIDE_PADDING),
                horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
                flingBehavior = rememberSnapFlingBehavior(state)
            ) {
                items(windows, key = { it.id }) { w ->
                    RiskWindowCard(
                        window = w, onOpenWhy = { onOpenWhy(w.id) },
                        modifier = Modifier.width(cardWidthDp).fillMaxHeight()
                    )
                }
            }
        }.first().measure(Constraints.fixed(constraints.maxWidth, tallest))
        layout(constraints.maxWidth, tallest) { row.place(0, 0) }
    }
}

/** Розтягує елемент на [bleed] за межі бічних відступів батька з обох боків. */
private fun Modifier.fullBleed(bleed: Dp): Modifier = layout { measurable, constraints ->
    val extra = bleed.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.maxWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

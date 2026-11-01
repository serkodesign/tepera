package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private val SIDE_PADDING = 16.dp
private val CARD_GAP = 12.dp

/**
 * Години скролінгу рядом, що гортається вбік: з двох-трьох вікон екран не росте втричі, а наступна картка трохи
 * визирає з-за краю (84% ширини). Одне вікно займає всю ширину. Ряд на всю ширину екрана (викликач не додає
 * бічних відступів), а відступи 16dp — це `padding` усередині прокрутки.
 *
 * Звичайний `Row` + `horizontalScroll`, без лінивих макетів: Pager/LazyRow і власний SubcomposeLayout у цьому
 * місці «з'їдали» жест — дотик споживався, але зсув не застосовувався. Усі картки однієї висоти
 * (`IntrinsicSize.Max` + `fillMaxHeight`).
 */
@Composable
internal fun WindowsCarousel(
    windows: List<RiskWindowUi>,
    onOpenWhy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (windows.isEmpty()) return
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val usable = screenWidth - SIDE_PADDING * 2
    val cardWidth = if (windows.size == 1) usable else usable * 0.84f
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = SIDE_PADDING).height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(CARD_GAP)
    ) {
        windows.forEach { w ->
            RiskWindowCard(
                window = w, onOpenWhy = { onOpenWhy(w.id) },
                modifier = Modifier.width(cardWidth).fillMaxHeight()
            )
        }
    }
}

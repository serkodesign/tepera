package com.serkodesign.tepera.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.serkodesign.tepera.ui.theme.TeperaSpecs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Горизонтальний слайдер верхніх карток Home (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node
 * 192:726): "Мій день" → "Патерн вчора" → "Цей тиждень", з крапками-індикатором під ним. Сторінка
 * без даних просто не додається до [pages]. **Свідомо скасовує попереднє рішення проти карусельного
 * пейджера (FR-D.10 "не карусель")** — за прямим запитом користувача після нового макета; решта
 * контекстних карток (пауза, оцінки тощо) лишаються вертикальним стеком під слайдером.
 * Наступна картка визирає справа на 12dp (`contentPadding` 16dp мінус `pageSpacing` 4dp).
 * Усі сторінки тримаються в композиції (`beyondViewportPageCount`), щоб висота слайдера не
 * стрибала при свайпі.
 */
@Composable
fun HomeCardsPager(pages: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    if (pages.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 4.dp,
            beyondViewportPageCount = pages.size,
            verticalAlignment = Alignment.Top
        ) { page ->
            pages[page]()
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(pages.size) { index ->
                    val active = pagerState.currentPage == index
                    val dotColor by animateColorAsState(
                        if (active) Color.White else Color.White.copy(alpha = 0.5f),
                        TeperaSpecs.effects(), label = "pagerDot"
                    )
                    Row(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    ) {}
                }
            }
        }
    }
}

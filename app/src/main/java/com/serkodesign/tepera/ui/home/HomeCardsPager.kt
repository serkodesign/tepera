package com.serkodesign.tepera.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.ui.theme.TeperaSpecs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Горизонтальний слайдер верхніх карток Home (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node
 * 192:726): "Мій день" → "Патерн вчора" → "Цей тиждень", з крапками-індикатором під ним. Сторінка
 * без даних просто не додається до [pages]. **Свідомо скасовує попереднє рішення проти карусельного
 * пейджера (FR-D.10 "не карусель")** — за прямим запитом користувача після нового макета; решта
 * контекстних карток (пауза, оцінки тощо) лишаються вертикальним стеком під слайдером.
 *
 * **Власний layout замість `HorizontalPager` (за запитом користувача — усі картки однакової висоти):**
 * `HorizontalPager` вимірює лише видимі сторінки й не вміє вирівняти їх за найвищою. Тут усі сторінки
 * лежать в одному `Layout`: висота кожної = найбільша власна (`maxIntrinsicHeight`) серед усіх, тож
 * картки завжди однакові й висота слайдера не стрибає при свайпі. Наступна картка визирає справа на
 * 12dp (відступ 16dp мінус проміжок 4dp), свайп — `draggable` зі "прилипанням" до найближчої сторінки.
 * Обмеження: на відміну від `HorizontalPager` немає вбудованих семантичних дій прокрутки для
 * скрінрідерів — сторінки доступні лише жестом.
 */
@Composable
fun HomeCardsPager(pages: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    if (pages.isEmpty()) return
    val density = LocalDensity.current
    val paddingPx = with(density) { 16.dp.toPx() }
    val spacingPx = with(density) { 4.dp.toPx() }
    val lastIndex = pages.size - 1

    // Зсув усього ряду сторінок у пікселях (0 — перша сторінка, від'ємний — прокручено). Читається лише у
    // фазі розміщення layout, тож під час свайпу перекомпоновки нема.
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var stridePx by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var dragStartPage by remember { mutableIntStateOf(0) }

    val currentPage by remember(lastIndex) {
        derivedStateOf { if (stridePx <= 0f) 0 else (-offsetPx / stridePx).roundToInt().coerceIn(0, lastIndex) }
    }

    fun settleTo(page: Int) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(offsetPx, -page * stridePx, animationSpec = TeperaSpecs.spatial()) { value, _ -> offsetPx = value }
        }
    }

    val dragState = rememberDraggableState { delta ->
        offsetPx = (offsetPx + delta).coerceIn(-lastIndex * stridePx, 0f)
    }

    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Layout(
            content = { pages.forEach { page -> Box(propagateMinConstraints = true) { page() } } },
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { stridePx = (it.width - 2 * paddingPx + spacingPx).coerceAtLeast(1f) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        settleJob?.cancel()
                        dragStartPage = currentPage
                    },
                    onDragStopped = { velocity ->
                        val projected = offsetPx + velocity * 0.2f
                        // Один свайп — не більше однієї сторінки від тієї, з якої почали (швидкий "кидок" не
                        // пролітає одразу через дві картки).
                        val target = (-projected / stridePx).roundToInt()
                            .coerceIn(dragStartPage - 1, dragStartPage + 1)
                            .coerceIn(0, lastIndex)
                        settleTo(target)
                    }
                )
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val pageWidth = (width - 2 * paddingPx).roundToInt().coerceAtLeast(0)
            val stride = pageWidth + spacingPx
            // Однакова висота: найбільша з природних висот усіх сторінок (без повторного вимірювання —
            // measure() двічі на одному measurable заборонено, тож рахуємо через intrinsics).
            val height = measurables.maxOf { it.maxIntrinsicHeight(pageWidth) }
            val placeables = measurables.map { it.measure(Constraints.fixed(pageWidth, height)) }
            layout(width, height) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative((paddingPx + index * stride + offsetPx).roundToInt(), 0)
                }
            }
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(pages.size) { index ->
                    val active = currentPage == index
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


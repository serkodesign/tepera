package com.serkodesign.tepera.ui.tutorial

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.M3Extra
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.bottomNavClearance
import kotlin.math.roundToInt

/*
 * Туторіал підказками (рішення власника, 14.11.2026: мікс «ланцюжок на Головній» + «контекстні підказки по екранах»).
 * Ланцюжок: затемнення з вирізом навколо елемента й карткою-поясненням («Далі» / «Пропустити»). Контекстні підказки —
 * разова картка зверху екрана з «Зрозуміло». Показується один раз; повтор — Налаштування → «Показати підказки знову».
 */

/** Межі елементів-цілей у координатах кореня; ключ — id кроку. */
class TutorialTargets {
    val rects = mutableStateMapOf<String, Rect>()
}

fun Modifier.tutorialTarget(targets: TutorialTargets, id: String): Modifier =
    // Не boundsInRoot(): воно обрізається по видимій зоні, а ціль може бути нижче за екран (Головна прокручується).
    onGloballyPositioned { targets.rects[id] = Rect(it.positionInRoot(), it.size.toSize()) }

/** Крок ланцюжка; `targetId == null` — пояснення без виділення (напр. вкладка навбару, що поза цим екраном). */
data class TutorialStep(
    val targetId: String?,
    @StringRes val title: Int,
    @StringRes val body: Int
)

/**
 * Оверлей ланцюжка. Кроки, чиєї цілі зараз нема на екрані (напр. картки ідеї у тиждень спостереження), тихо
 * пропускаються. [scrollTo] підводить ціль у видиму зону (Головна прокручується); [onFinish] — і «Готово», і «Пропустити».
 */
@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    targets: TutorialTargets,
    scrollTo: suspend (target: Rect, viewport: Rect) -> Unit,
    onFinish: () -> Unit
) {
    val active = remember { steps.filter { it.targetId == null || (targets.rects[it.targetId]?.height ?: 0f) > 1f } }
    if (active.isEmpty()) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }
    var index by remember { mutableIntStateOf(0) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    var cardHeight by remember { mutableIntStateOf(0) }
    val step = active[index]
    val target = step.targetId?.let { targets.rects[it] }
    val density = LocalDensity.current

    BackHandler(onBack = onFinish)
    LaunchedEffect(index, viewport) {
        if (target != null && viewport != Rect.Zero) scrollTo(target, viewport)
    }

    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { viewport = it.boundsInRoot() }
            // Тап по затемненню нічого не робить (і не проходить на екран під ним): рух — лише кнопками.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        val pad = with(density) { 8.dp.toPx() }
        val radius = with(density) { 20.dp.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.62f))
            if (target != null) {
                val r = target.translate(-viewport.left, -viewport.top)
                drawRoundRect(
                    color = Color.Transparent, blendMode = BlendMode.Clear,
                    topLeft = Offset(r.left - pad, r.top - pad),
                    size = Size(r.width + pad * 2, r.height + pad * 2),
                    cornerRadius = CornerRadius(radius)
                )
            }
        }

        val gap = with(density) { 12.dp.roundToPx() }
        val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).onSizeChanged { cardHeight = it.height }
        if (target == null) {
            Box(Modifier.fillMaxSize().padding(bottom = bottomNavClearance() + 8.dp), contentAlignment = Alignment.BottomCenter) {
                StepCard(step, index, active.size, cardModifier, onNext = { if (index == active.lastIndex) onFinish() else index++ }, onSkip = onFinish)
            }
        } else {
            val r = target.translate(-viewport.left, -viewport.top)
            // Навбар лежить поверх оверлея — картка має поміститись над ним. Вибір: під ціллю, якщо вміщується, інакше над
            // нею, інакше притискається до нижньої межі (перекриваючи низ цілі).
            val bottomLimit = viewport.height.roundToInt() - with(density) { bottomNavClearance().roundToPx() }
            val belowY = (r.bottom + pad).roundToInt() + gap
            val aboveY = (r.top - pad).roundToInt() - gap - cardHeight
            val y = when {
                belowY + cardHeight <= bottomLimit -> belowY
                aboveY >= gap -> aboveY
                else -> bottomLimit - cardHeight
            }
            Box(Modifier.offset { IntOffset(0, y.coerceAtLeast(gap)) }) {
                StepCard(step, index, active.size, cardModifier, onNext = { if (index == active.lastIndex) onFinish() else index++ }, onSkip = onFinish)
            }
        }
    }
}

@Composable
private fun StepCard(
    step: TutorialStep, index: Int, total: Int, modifier: Modifier, onNext: () -> Unit, onSkip: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, M3Extra.colors.cardStroke, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.tutorial_step_counter, index + 1, total),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(step.title), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() }
        )
        Text(stringResource(step.body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (index < total - 1) TeperaButton(stringResource(R.string.tutorial_skip), onClick = onSkip, type = TeperaButtonType.Tertiary)
            TeperaButton(stringResource(if (index == total - 1) R.string.tutorial_done else R.string.tutorial_next), onClick = onNext)
        }
    }
}

/** Разова контекстна підказка зверху екрана (Щоденник, Налаштування): картка з «Зрозуміло». */
@Composable
fun TutorialHintCard(@StringRes title: Int, @StringRes body: Int, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(M3Extra.colors.glass)
            .border(1.dp, M3Extra.colors.cardStroke, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(title), style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() }
        )
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TeperaButton(stringResource(R.string.tutorial_got_it), onClick = onDismiss, type = TeperaButtonType.Secondary)
        }
    }
}

package com.serkodesign.tepera.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

/**
 * iOS-подібний "відскок" на краю списку замість системного розтягування (stretch).
 * Лише вертикальна вісь — усі прокручувані екрани Tepera вертикальні.
 */
object BounceOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = BounceOverscrollEffect()
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0
}

private class BounceOverscrollEffect : OverscrollEffect {
    private var offset by mutableFloatStateOf(0f)
    private var maxOffset = 400f

    override val isInProgress: Boolean get() = offset != 0f

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        if (source != NestedScrollSource.UserInput) return performScroll(delta)

        var remaining = delta.y
        var consumedByOverscroll = 0f
        // Спершу "повертаємо" вже набраний відскок, якщо палець рухається назад.
        if (offset != 0f && remaining * offset < 0f) {
            val newOffset = offset + remaining
            if (newOffset * offset <= 0f) {
                consumedByOverscroll = -offset
                offset = 0f
            } else {
                consumedByOverscroll = remaining
                offset = newOffset
            }
            remaining -= consumedByOverscroll
        }
        if (remaining == 0f) return Offset(delta.x, consumedByOverscroll)

        val consumedByScroll = performScroll(Offset(delta.x, remaining))
        val leftover = remaining - consumedByScroll.y
        if (leftover != 0f) {
            // Опір зростає з відстанню.
            val resistance = (0.5f * (1f - abs(offset) / maxOffset)).coerceAtLeast(0f)
            offset += leftover * resistance
        }
        return Offset(consumedByScroll.x, consumedByScroll.y + consumedByOverscroll + leftover)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        // Свайп "від краю" під час відскоку мусить прокрутити список (раніше швидкість губилась і
        // прокрутка здавалась зупиненою); лише свайп ГЛИБШЕ за край лишається відскоку.
        val pushesDeeper = offset != 0f && offset * velocity.y > 0f
        val left = if (pushesDeeper) Velocity(0f, velocity.y) else velocity - performFling(velocity)
        animate(
            initialValue = offset,
            targetValue = 0f,
            initialVelocity = left.y * 0.15f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
        ) { value, _ -> offset = value }
        offset = 0f
    }

    override val node: Modifier.Node = BounceNode()

    private inner class BounceNode : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            maxOffset = placeable.height.toFloat().coerceAtLeast(1f) * 0.5f
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) { translationY = offset }
            }
        }
    }
}

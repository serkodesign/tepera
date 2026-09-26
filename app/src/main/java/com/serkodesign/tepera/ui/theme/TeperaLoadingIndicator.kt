package com.serkodesign.tepera.ui.theme

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import kotlin.math.cos
import kotlin.math.sin

private const val LOADING_DIRECTION_MILLIS = 1200
private const val LOADING_OUTER_LAG_MILLIS = 200
private const val LOADING_SCALE_SMALL = 145f / 215f
private const val LOADING_OUTER_ROTATION_DEGREES = 15f
private const val LOADING_OUTER_SIZE_RATIO = 1.25f // напівпрозора форма на 25% більша за основну

/**
 * Індикатор завантаження замість дефолтного M3 `CircularProgressIndicator` (той брав фіолетовий primary теми): та сама
 * «квітка», що дихає на екрані воріт, але менша (60dp — на 50% більша за стандартний 40dp) і швидша — 1.2 с в кожен бік
 * замість 4 с. Основна форма суцільна біла, під нею напівпрозора повернута на 15° з відставанням 0.2 с.
 * Значення читаються лише в `graphicsLayer`, без перекомпонування.
 */
@Composable
fun TeperaLoadingIndicator(modifier: Modifier = Modifier, size: Dp = 60.dp) {
    val diameterPx = with(LocalDensity.current) { size.toPx() }
    val path = remember(diameterPx) { scallopedBlobPath(diameter = diameterPx, lobes = 12, wobbleFraction = 0.07f) }
    val transition = rememberInfiniteTransition(label = "loadingFlower")
    val main = transition.animateFloat(
        initialValue = LOADING_SCALE_SMALL,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LOADING_DIRECTION_MILLIS, easing = EaseInOut), RepeatMode.Reverse),
        label = "loadingMain"
    )
    val outer = transition.animateFloat(
        initialValue = LOADING_SCALE_SMALL,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(LOADING_DIRECTION_MILLIS, easing = EaseInOut),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(LOADING_OUTER_LAG_MILLIS)
        ),
        label = "loadingOuter"
    )
    val description = stringResource(R.string.loading_description)
    Box(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
    ) {
        // Зовнішня напівпрозора форма — під основною.
        Canvas(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = outer.value * LOADING_OUTER_SIZE_RATIO
                scaleY = outer.value * LOADING_OUTER_SIZE_RATIO
                rotationZ = LOADING_OUTER_ROTATION_DEGREES
                alpha = 0.5f
            }
        ) { drawPath(path, Color.White) }
        Canvas(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = main.value
                scaleY = main.value
            }
        ) { drawPath(path, Color.White) }
    }
}

fun scallopedBlobPath(diameter: Float, lobes: Int, wobbleFraction: Float): Path {
    val path = Path()
    val center = diameter / 2f
    val baseRadius = diameter / 2f
    val segments = 200
    for (i in 0..segments) {
        val t = i / segments.toFloat()
        val angle = t * 2f * Math.PI.toFloat()
        val r = baseRadius * (1f - wobbleFraction + wobbleFraction * cos(lobes * angle))
        val x = center + r * cos(angle)
        val y = center + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

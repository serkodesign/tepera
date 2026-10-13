package com.serkodesign.tepera.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Базова шкала смуги в годинах (Figma: Estimate bar → Scale=6h). */
const val TeperaEstimateBaseScaleHours = 6

/**
 * Шкала для заданого факту: 6 год, а якщо факт більший — наступне кратне 6 (12/18/24 год), не більше
 * доби. Кратність 6 тримає рівно 7 підписів осі (0…N) з цілим кроком (Figma: Scale=6h / 12h).
 */
fun teperaEstimateScaleHours(actualMinutes: Int): Int {
    val actualHours = ceil(actualMinutes.coerceAtLeast(0) / 60.0).toInt()
    return if (actualHours <= TeperaEstimateBaseScaleHours) TeperaEstimateBaseScaleHours
    else (ceil(actualHours / 6.0).toInt() * 6).coerceAtMost(24)
}

/**
 * Смуга-пігулка "оцінка → реальність" — компонент дизайн-системи (Figma "Assets" → секція
 * "Estimate card" → компонент `Estimate bar`, варіанти `Type=Guess | Actual inside | Actual outside`,
 * `Scale=6h | 12h`). M3 large: радіус 20, висота 64, тло [TeperaPalette.guessFill], ледь помітні
 * лінії щогодини.
 *
 * - [range] задано → "Guess": штриховий діапазон оцінки (хв від початку шкали), число зліва; якщо
 *   на довгій шкалі діапазон накриває місце підпису, підпис переходить усередину діапазону
 *   (якщо влазить) або праворуч від нього.
 * - [fillMinutes] задано → "Actual": заливка від нуля, число всередині білим; якщо заливка коротша за
 *   число — число праворуч від заливки темним кольором ("Actual outside").
 * - Підпис усередині: лівий відступ 20dp, мітка 12sp, число 22sp Medium, між ними 4dp.
 * Ні [range], ні [fillMinutes] — просто порожня смуга з підписом.
 */
@Composable
fun TeperaEstimateBar(
    label: String,
    value: String,
    scaleMinutes: Int,
    modifier: Modifier = Modifier,
    fillMinutes: Int? = null,
    range: IntRange? = null
) {
    val shape = RoundedCornerShape(20.dp)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val valueStyle = TextStyle(
        fontFamily = TeperaPalette.headlineFont,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp
    )
    val targetFraction = fillMinutes?.let { (it.toFloat() / scaleMinutes).coerceIn(0f, 1f) } ?: 0f
    val animated = remember { Animatable(0f) }
    LaunchedEffect(targetFraction) {
        animated.animateTo(targetFraction, tween(durationMillis = 700, easing = TeperaMotion.EmphasizedDecelerate))
    }

    val trackColor = TeperaPalette.guessFill
    val gridColor = TeperaPalette.buttonBrandDark.copy(alpha = 0.06f)
    val brand = TeperaPalette.buttonBrand
    val brandDark = TeperaPalette.buttonBrandDark
    val onBrand = TeperaPalette.onPrimary
    val secondary = TeperaPalette.textSecondary
    val scaleHours = scaleMinutes / 60

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(trackColor)
            .drawBehind {
                val w = size.width
                val h = size.height
                // Лінії щогодини — ледь помітні, щоб шкала читалась без підписів усередині смуги.
                for (hour in 1 until scaleHours) {
                    val x = w * hour / scaleHours
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
                }
                if (range != null) {
                    val left = if (range.first <= 0) 6.dp.toPx() else w * range.first / scaleMinutes + 2.dp.toPx()
                    val right = if (range.last >= scaleMinutes) w - 6.dp.toPx() else w * range.last / scaleMinutes - 2.dp.toPx()
                    val inset = 6.dp.toPx()
                    val topLeft = Offset(left, inset)
                    val rectSize = Size((right - left).coerceAtLeast(0f), h - inset * 2)
                    val corner = CornerRadius(14.dp.toPx())
                    drawRoundRect(secondary.copy(alpha = 0.06f), topLeft, rectSize, corner)
                    drawRoundRect(
                        color = secondary.copy(alpha = 0.55f),
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = corner,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                        )
                    )
                }
                if (fillMinutes != null) {
                    drawRoundRect(
                        color = brand,
                        size = Size(w * animated.value, h),
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val startPadPx = with(density) { 20.dp.toPx() }
        val endPadPx = with(density) { 12.dp.toPx() }
        val valueWidthPx = remember(value) { textMeasurer.measure(value, valueStyle).size.width }
        val fillPx = widthPx * targetFraction
        val insideFill = fillMinutes != null && fillPx >= startPadPx + valueWidthPx + endPadPx
        val textX = when {
            fillMinutes != null -> if (insideFill) startPadPx else fillPx + endPadPx
            range != null -> {
                // Оцінка: текст зліва, але на довгій шкалі (понад 6 год) діапазон ближче до початку й
                // може його накрити. Тоді — усередину діапазону (якщо влазить), інакше праворуч від нього.
                val gapPx = with(density) { 8.dp.toPx() }
                val innerPadPx = with(density) { 16.dp.toPx() }
                val rangeLeft = if (range.first <= 0) with(density) { 6.dp.toPx() } else widthPx * range.first / scaleMinutes
                val rangeRight = if (range.last >= scaleMinutes) widthPx else widthPx * range.last / scaleMinutes
                val collides = startPadPx + valueWidthPx + gapPx > rangeLeft && startPadPx < rangeRight
                when {
                    !collides -> startPadPx
                    rangeRight - rangeLeft >= valueWidthPx + innerPadPx * 2 -> maxOf(startPadPx, rangeLeft + innerPadPx)
                    widthPx - rangeRight >= valueWidthPx + startPadPx + endPadPx -> rangeRight + endPadPx
                    else -> startPadPx
                }
            }
            else -> startPadPx
        }
        // Число стає білим, коли заливка його накриває (плавно, у такт анімації заливки).
        val progress = if (insideFill && targetFraction > 0f) (animated.value / targetFraction).coerceIn(0f, 1f) else 0f
        val labelColor = when {
            fillMinutes == null -> secondary
            insideFill -> lerp(brand, onBrand.copy(alpha = 0.85f), progress)
            else -> brand
        }
        val valueColor = when {
            fillMinutes == null -> secondary
            insideFill -> lerp(brandDark, onBrand, progress)
            else -> brandDark
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(textX.roundToInt(), 0) },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = labelColor,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
                maxLines = 1
            )
            Text(text = value, style = valueStyle, color = valueColor, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * Підписи годин під смугами [TeperaEstimateBar] (Figma: `Estimate axis`, Scale=6h/12h): 0 … [scaleHours]
 * рівномірно; [scaleHours] кратне 6, тож завжди 7 підписів з цілим кроком (1 год на 6h, 2 год на 12h…).
 */
@Composable
fun TeperaHourAxis(scaleHours: Int, modifier: Modifier = Modifier) {
    val step = (scaleHours / 6).coerceAtLeast(1)
    val labels = (0..scaleHours step step).toList()
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { hour ->
            Text(
                text = hour.toString(),
                color = TeperaPalette.buttonBrandDark.copy(alpha = 0.55f),
                fontFamily = TeperaPalette.headlineFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1
            )
        }
    }
}

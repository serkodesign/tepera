package com.serkodesign.tepera.ui.stats

import androidx.compose.foundation.Canvas
import com.serkodesign.tepera.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Графік "Online trend" — Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR), node 210:1880, значення
 * з get_design_context/download_assets (SVG-контури), не зі скриншота.
 *
 * Геометрія з макета (dp): область графіка заввишки 217, зліва вісь Y, знизу вісь X (#ADADAD, 1),
 * 7 вертикальних пунктирних ліній (штрих 2/4, 50%) — перша на 23 від осі, крок 43, права межа на
 * 9 від краю; горизонтальні пунктирні на 12h/9h/6h/3h (y = 9, 60, 110, 161); лінія — #006944,
 * штрих 4, заокруглена; під нею градієнт від #006944 до #00CF87@0 (від y=9 до низу осі). Підписи
 * осі Y — 12sp Regular #003926, вирівняні по правому краю, розподілені на всю висоту (5 підписів),
 * підписи днів — 12sp Regular #003926 під вертикалями.
 *
 * За рішеннями користувача: вісь Y = 0..12h, а якщо тиждень перевищує 12 год — розширюється
 * кроком 3h; точка кожного дня лежить на своїй вертикалі (лінія починається на першій вертикалі,
 * а не на осі, як у схематичному макеті).
 */
@Composable
fun OnlineTrendChart(
    minutesPerDay: List<Int>,
    dayLabels: List<String>,
    hoursFormat: String,
    modifier: Modifier = Modifier
) {
    val maxHours = (minutesPerDay.maxOrNull() ?: 0) / 60f
    val topHours = max(12, (ceil(maxHours / 3f) * 3f).toInt())
    val labelStyleColor = TeperaPalette.buttonBrandDark

    // Текстова альтернатива графіку (WCAG 1.1.1): "Пн 3 год; Вт 2 год …" — скрінрідер не бачить Canvas.
    val summary = dayLabels.zip(minutesPerDay).map { (label, minutes) -> "$label ${durationText(minutes)}" }.joinToString("; ")
    val chartDescription = stringResource(R.string.trend_chart_description, summary)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = chartDescription },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            // Figma 210:1905: підписи осі Y — justify-between на всю висоту графіка, по правому краю.
            Column(
                modifier = Modifier.height(PLOT_HEIGHT),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (h in topHours downTo 0 step 3) {
                    Text(
                        text = String.format(hoursFormat, h),
                        color = labelStyleColor,
                        fontFamily = TeperaPalette.headlineFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 15.6.sp,
                        letterSpacing = 0.012.sp,
                        maxLines = 1
                    )
                }
            }
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val plotWidth = maxWidth
                val step = pointStep(plotWidth, minutesPerDay.size)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(PLOT_HEIGHT)) {
                        drawTrend(minutesPerDay, topHours, step.toPx(), density)
                    }
                    // Підписи днів — по центру під своїми вертикалями.
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                        val labelWidth = if (step > 0.dp) step else 43.dp
                        dayLabels.forEachIndexed { index, label ->
                            val center = FIRST_POINT_X + step * index
                            Text(
                                text = label,
                                modifier = Modifier
                                    .width(labelWidth)
                                    .offset(x = center - labelWidth / 2),
                                color = labelStyleColor,
                                fontFamily = TeperaPalette.headlineFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 15.6.sp,
                                letterSpacing = 0.012.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private val PLOT_HEIGHT = 217.dp
private val FIRST_POINT_X = 23.5.dp
private val RIGHT_MARGIN = 9.dp
private const val TOP_PAD = 9f
private const val ZERO_Y = 211f

private fun pointStep(plotWidth: Dp, count: Int): Dp =
    if (count <= 1) 0.dp else (plotWidth - FIRST_POINT_X - RIGHT_MARGIN) / (count - 1)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrend(
    minutes: List<Int>,
    topHours: Int,
    stepPx: Float,
    d: Float
) {
    val axisColor = Color(0xFFADADAD)
    val dash = PathEffect.dashPathEffect(floatArrayOf(2f * d, 4f * d))
    val w = size.width
    val h = size.height

    // Осі (Figma Vector 10): ліва й нижня, #ADADAD, 1dp.
    drawLine(axisColor, Offset(0.5f * d, 0f), Offset(0.5f * d, h), strokeWidth = 1f * d)
    drawLine(axisColor, Offset(0.5f * d, h - 0.5f * d), Offset(w, h - 0.5f * d), strokeWidth = 1f * d)

    val firstX = FIRST_POINT_X.toPx()
    val count = minutes.size

    // Вертикальні пунктирні лінії — по одній на кожен день.
    for (i in 0 until count) {
        val x = firstX + stepPx * i
        drawLine(axisColor.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f * d, pathEffect = dash)
    }

    // Горизонтальні пунктирні лінії на topH, topH-3, ..., 3 год.
    fun yOf(hours: Float): Float = (TOP_PAD + (topHours - hours) * ((ZERO_Y - TOP_PAD) / topHours)) * d
    for (hh in topHours downTo 3 step 3) {
        val y = yOf(hh.toFloat())
        drawLine(axisColor.copy(alpha = 0.5f), Offset(0.5f * d, y), Offset(w, y), strokeWidth = 1f * d, pathEffect = dash)
    }

    if (count == 0) return
    val pts = List(count) { i -> Offset(firstX + stepPx * i, yOf(minutes[i] / 60f)) }
    // Лінія й заливка починаються від осі Y (Figma: лінія стартує біля осі): від осі до першої точки
    // йде горизонтальний відрізок на висоті першого значення.
    val axisX = 0.5f * d
    val line = smoothPath(pts)
    val areaLine = smoothPath(pts, startX = axisX)

    // Заливка під лінією: градієнт #006944 -> #00CF87@0 від верху сітки (y=9) до низу осі.
    val area = Path().apply {
        addPath(areaLine)
        lineTo(pts.last().x, h)
        lineTo(axisX, h)
        close()
    }
    drawPath(
        area,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF006944), Color(0x0000CF87)),
            startY = TOP_PAD * d,
            endY = h
        )
    )
    drawPath(
        line,
        color = Color(0xFF006944),
        style = Stroke(width = 4f * d, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Відрізок від осі до першого дня — пунктиром (крапки-штрихи з заокругленими кінцями).
    val extension = Path().apply {
        moveTo(axisX, pts.first().y)
        lineTo(pts.first().x, pts.first().y)
    }
    drawPath(
        extension,
        color = Color(0xFF006944),
        style = Stroke(
            width = 4f * d,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * d, 8f * d))
        )
    )
}

/** Згладжена крива через точки (Catmull-Rom → Безьє), контрольні точки не виходять за межі сусідів по Y. */
private fun smoothPath(p: List<Offset>, startX: Float? = null): Path {
    val path = Path()
    if (p.isEmpty()) return path
    if (startX != null) {
        path.moveTo(startX, p[0].y)
        path.lineTo(p[0].x, p[0].y)
    } else {
        path.moveTo(p[0].x, p[0].y)
    }
    if (p.size == 1) return path
    for (i in 0 until p.size - 1) {
        val p0 = p[max(i - 1, 0)]
        val p1 = p[i]
        val p2 = p[i + 1]
        val p3 = p[min(i + 2, p.size - 1)]
        val lo = min(p1.y, p2.y)
        val hi = max(p1.y, p2.y)
        // Перший сегмент виходить із першої точки горизонтально — плавно продовжує відрізок від осі.
        val c1 = if (i == 0) {
            Offset(p1.x + (p2.x - p1.x) / 2.5f, p1.y)
        } else {
            Offset(p1.x + (p2.x - p0.x) / 6f, (p1.y + (p2.y - p0.y) / 6f).coerceIn(lo, hi))
        }
        val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, (p2.y - (p3.y - p1.y) / 6f).coerceIn(lo, hi))
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}

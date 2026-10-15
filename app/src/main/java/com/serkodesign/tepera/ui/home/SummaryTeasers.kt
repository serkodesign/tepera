package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols

/**
 * Слот тізерів у hero (docs/HANDOFF.md, розділ 5; Figma `SummaryTeaserPair` 684:22175, `SummaryTile` 684:19223,
 * `SummaryTeaser` 677:15496): фіксована висота 52dp, до двох плиток. Без стрілок ↑/↓, відсотків порівняння, цілей
 * і кольорового оцінювання ("без тиску"). Тап по плитці відкриває шторку на відповідній вкладці.
 */
@Composable
fun SummaryTeaserSlot(
    slot: TeaserSlot,
    onOpen: (SummaryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    when (slot) {
        is TeaserSlot.Pair -> Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            val left = slot.left
            when (left) {
                is TeaserTile.Yesterday -> SummaryTile(
                    title = stringResource(R.string.summary_yesterday),
                    value = heroDuration(left.offlineMinutes),
                    bars = offlineBars(left.offlineByHour),
                    onClick = { onOpen(SummaryTab.YESTERDAY) },
                    modifier = Modifier.weight(1f)
                )
                // "Вчора — —": без графіка, не натискається.
                else -> SummaryTile(
                    title = stringResource(R.string.summary_yesterday),
                    value = "—",
                    bars = emptyList(),
                    onClick = null,
                    modifier = Modifier.weight(1f)
                )
            }
            val right = slot.right
            SummaryTile(
                title = stringResource(if (right.tab == SummaryTab.MONTH) R.string.summary_month else R.string.summary_week),
                value = formatHoursPerDay(right.averageMinutesPerDay, LocalConfiguration.current.locales[0]),
                valueUnit = stringResource(R.string.summary_per_day_unit),
                bars = periodBars(right.bars),
                onClick = { onOpen(right.tab) },
                modifier = Modifier.weight(1f)
            )
        }
        is TeaserSlot.WideYesterday -> WideTeaser(
            title = stringResource(R.string.summary_yesterday),
            value = stringResource(R.string.summary_start_format, "%02d:%02d".format(slot.startMinute / 60, slot.startMinute % 60)),
            bars = onlineBars(slot.onlineByHour),
            onClick = { onOpen(SummaryTab.YESTERDAY) },
            modifier = modifier
        )
        TeaserSlot.WideEmpty -> WideTeaser(
            title = stringResource(R.string.summary_yesterday),
            value = "—",
            bars = emptyList(),
            onClick = null,
            modifier = modifier
        )
    }
}

/** Один стовпчик міні-графіка: висота 0..1 від висоти полотна, колір (вже з прозорістю). */
data class MiniBar(val fraction: Float, val color: Color)

private val MIN_BAR_FRACTION = 3f / 18f

@Composable
private fun offlineBars(offlineByHour: List<Int>): List<MiniBar> {
    val base = TeperaPalette.restOfDayCard
    return offlineByHour.map {
        val f = (it / 60f).coerceIn(0f, 1f)
        MiniBar(maxOf(MIN_BAR_FRACTION, f), base.copy(alpha = 0.45f + 0.45f * f))
    }
}

@Composable
private fun onlineBars(onlineByHour: List<Int>): List<MiniBar> {
    val base = TeperaPalette.onlineCard
    return onlineByHour.map {
        val f = (it / 60f).coerceIn(0f, 1f)
        MiniBar(maxOf(MIN_BAR_FRACTION, f), base.copy(alpha = 0.3f + 0.65f * f))
    }
}

/** Тиждень / Місяць: висота відносно найвищого дня вікна, сьогодні — виділений (повна непрозорість). */
@Composable
private fun periodBars(bars: List<DayBar>): List<MiniBar> {
    val base = TeperaPalette.restOfDayCard
    val max = maxOf(60, bars.maxOfOrNull { it.offlineMinutes ?: 0 } ?: 0).toFloat()
    return bars.map {
        val value = it.offlineMinutes
        when {
            value == null -> MiniBar(MIN_BAR_FRACTION, base.copy(alpha = 0.25f))
            it.isToday -> MiniBar(maxOf(MIN_BAR_FRACTION, value / max), base)
            else -> MiniBar(maxOf(MIN_BAR_FRACTION, value / max), base.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun SummaryTile(
    title: String,
    value: String,
    bars: List<MiniBar>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** Необов'язкова одиниця поруч із числом ("год/день") — дрібніша, щоб довге середнє ("11,6 год/день") вміщалось. */
    valueUnit: String? = null
) {
    TeaserSurface(onClick = onClick, modifier = modifier.heightIn(min = 52.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                TeaserText(title, 11, FontWeight.Normal, alpha = 0.8f)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    TeaserText(value, 14, FontWeight.Medium)
                    if (valueUnit != null) TeaserText(valueUnit, 11, FontWeight.Normal, alpha = 0.8f)
                }
            }
            MiniChart(bars, Modifier.fillMaxWidth().height(18.dp))
        }
    }
}

@Composable
private fun WideTeaser(
    title: String,
    value: String,
    bars: List<MiniBar>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    TeaserSurface(onClick = onClick, modifier = modifier.heightIn(min = 52.dp)) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TeaserText(title, 11, FontWeight.Normal, alpha = 0.8f)
                TeaserText(value, 14, FontWeight.Medium)
            }
            if (bars.isNotEmpty()) MiniChart(bars, Modifier.width(119.dp).height(22.dp))
            if (onClick != null) {
                Icon(TeperaSymbols.ChevronRight, contentDescription = null, tint = TeperaPalette.textOnDark, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/** Плитка-тло: `hero/track`, радіус 14, pressed — `text/on-dark` @14%. Без [onClick] — не натискається. */
@Composable
private fun TeaserSurface(onClick: (() -> Unit)?, modifier: Modifier, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val overlay = TeperaPalette.textOnDark.copy(alpha = 0.14f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TeperaPalette.heroTrack)
            .drawWithContent {
                drawContent()
                if (pressed) drawRect(overlay)
            }
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
                else Modifier
            )
    ) { content() }
}

@Composable
private fun TeaserText(text: String, size: Int, weight: FontWeight, alpha: Float = 1f) {
    Text(
        text = text,
        color = TeperaPalette.textOnDark.copy(alpha = alpha),
        fontFamily = TeperaPalette.headlineFont,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * 1.1f).sp,
        maxLines = 1
    )
}

/** Міні-графік: стовпчики з нижнім вирівнюванням, радіус 2dp, проміжок ≈ 43% ширини стовпчика (як у Figma). */
@Composable
private fun MiniChart(bars: List<MiniBar>, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) return
    Canvas(modifier) {
        val n = bars.size
        val gapRatio = 0.43f
        val barWidth = size.width / (n + (n - 1) * gapRatio)
        val step = barWidth * (1f + gapRatio)
        val radius = CornerRadius(minOf(2.dp.toPx(), barWidth / 2f))
        bars.forEachIndexed { index, bar ->
            val height = size.height * bar.fraction.coerceIn(0f, 1f)
            drawRoundRect(
                color = bar.color,
                topLeft = Offset(index * step, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = radius
            )
        }
    }
}

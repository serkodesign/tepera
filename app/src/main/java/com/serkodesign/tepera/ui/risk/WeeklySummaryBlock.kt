package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaChip
import com.serkodesign.tepera.ui.theme.LocalTeperaColors
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlin.math.roundToInt

/**
 * v3.0 MVP, Етап 5 (M5): тижневий підсумок прямо на Home, без окремого пуша. Лише те, що вимірюється: хвилини в
 * стрічках у вікні (спостереження → останні 7 діб) і «Добре» на нагадуваннях. Без оцінок, серій і «тримався плану».
 * Вигляд за M3: одна картка; «було» й «тепер» — дві смуги одного масштабу в тональному контейнері вікна (довжина
 * порівнюється з власною історією, без кольорів «добре/погано»); сесії «на 5 хв» — один рядок великих чисел.
 * Якщо вікно зсунулося — питаємо, а не міняємо мовчки.
 */
@Composable
internal fun WeeklySummaryBlock(
    windows: List<RiskWindowUi>,
    softExit: Pair<Int, Int>?,
    onApplyShift: (String) -> Unit,
    onDismissShift: (String) -> Unit
) {
    if (windows.isEmpty()) return
    TeperaCard(title = stringResource(R.string.v3_summary_title)) {
        // Закон близькості: дві групи (числа сесій; вікна з підписом) по 16dp одна від одної, всередині груп — тісніше.
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Сесії «на 5 хв» (макет «М'який вихід»): скільки розпочато й скільки завершено вчасно (до +2 хв).
            softExit?.let { (started, onTime) ->
                Text(
                    stringResource(R.string.v3_summary_soft_exit, started, onTime),
                    color = TeperaPalette.textPrimary, fontSize = 14.sp, lineHeight = 20.sp
                )
            }
            // Підпис стоїть упритул до вікон, яких стосується (8dp), а не плаває посередині між групами.
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Більше трьох вікон — решта під кнопкою, щоб підсумок не ріс у довгий стек. Вікно із запропонованим зсувом видно завжди.
                var expanded by rememberSaveable { mutableStateOf(false) }
                val (always, rest) = windows.withIndex().partition { (i, w) -> i < COLLAPSED_COUNT || w.shiftStart != null }
                always.forEach { WindowSummary(it.value, onApplyShift, onDismissShift) }
                if (expanded) rest.forEach { WindowSummary(it.value, onApplyShift, onDismissShift) }
                if (rest.isNotEmpty()) {
                    TeperaButton(
                        text = if (expanded) stringResource(R.string.v3_summary_show_less) else stringResource(R.string.v3_summary_show_more, rest.size),
                        onClick = { expanded = !expanded }, type = TeperaButtonType.Tertiary, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Одне вікно: час (і «Добре») над смугою. Заливка смуги — «тепер», риска на смузі — «було» (перший тиждень), один
 * масштаб; під смугою «було N хв». Без кольорів «добре/погано»: довжина порівнюється з власною історією.
 */
@Composable
private fun WindowSummary(w: RiskWindowUi, onApplyShift: (String) -> Unit, onDismissShift: (String) -> Unit) {
    // Закон близькості: час, смуга й підпис «було» — одна група (4dp); між вікнами відступ більший (його задає батько).
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatWindowTime(w.startMinute, w.endMinute),
                color = TeperaPalette.textPrimary, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (w.okThisWeek > 0) {
                TeperaChip(label = stringResource(R.string.v3_summary_stat_ok), value = w.okThisWeek.toString(), compact = true)
            }
        }
        val before = w.baselineMinutes
        val now = w.currentMinutes
        if (now != null) {
            ComparisonBar(now = now, before = before)
            if (before != null) {
                Text(
                    stringResource(R.string.v3_summary_was, before.roundToInt()),
                    color = TeperaPalette.textSecondary, fontSize = 13.sp, lineHeight = 18.sp
                )
            }
        }
        if (w.shiftStart != null && w.shiftEnd != null) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.v3_summary_shift, formatWindowTime(w.shiftStart, w.shiftEnd)),
                    color = TeperaPalette.textPrimary, fontSize = 14.sp, lineHeight = 20.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TeperaButton(
                        stringResource(R.string.v3_summary_shift_apply), onClick = { onApplyShift(w.id) },
                        modifier = Modifier.weight(1f)
                    )
                    TeperaButton(
                        stringResource(R.string.v3_summary_shift_keep), onClick = { onDismissShift(w.id) },
                        type = TeperaButtonType.Secondary, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Смуга «тепер» з рискою «було» в одному масштабі (більше з двох значень = повна довжина) і значенням праворуч. */
@Composable
private fun ComparisonBar(now: Double, before: Double?) {
    // Запас 15% у масштабі, щоб риска «було» не злипалась із краєм смуги.
    val scale = maxOf(now, before ?: 0.0, 1.0).toFloat() * 1.15f
    val fill = TeperaPalette.brandAccent
    // Риска і її обводка мають контрастувати із заливкою (у темній темі вона світла): темна риска в світлій обводці й навпаки.
    val dark = LocalTeperaColors.current.isDark
    val tick = if (dark) Color(0xFF2A1F45) else TeperaPalette.textPrimary
    val tickEdge = if (dark) TeperaPalette.textPrimary else Color.White
    val track = TeperaPalette.textPrimary.copy(alpha = 0.08f)
    val fraction = (now.toFloat() / scale).coerceIn(0f, 1f)
    val beforeFraction = before?.let { (it.toFloat() / scale).coerceIn(0f, 1f) }
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(track)) {
            // Навіть мала ненульова величина лишає помітну смужку, нуль — порожньо.
            if (fraction > 0f) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceAtLeast(0.04f)).clip(RoundedCornerShape(8.dp)).background(fill))
            }
            // Риска «було»: тонка вертикальна лінія на всю висоту смуги.
            if (beforeFraction != null && beforeFraction > 0f) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(beforeFraction).padding(end = 0.dp), contentAlignment = Alignment.CenterEnd) {
                    Box(Modifier.fillMaxHeight().width(5.dp).background(tickEdge), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxHeight().width(3.dp).background(tick))
                    }
                }
            }
        }
        Text(
            stringResource(R.string.v3_dur_minutes, now.roundToInt()),
            color = TeperaPalette.textPrimary, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

private const val COLLAPSED_COUNT = 3

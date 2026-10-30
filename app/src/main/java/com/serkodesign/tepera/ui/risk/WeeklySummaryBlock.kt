package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
    TeperaCard(
        title = stringResource(R.string.v3_summary_title),
        subtitle = stringResource(R.string.v3_summary_intro)
    ) {
        // Сесії «на 5 хв» (макет «М'який вихід»): скільки розпочато й скільки завершено вчасно (до +2 хв).
        softExit?.let { (started, onTime) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigStat(started.toString(), stringResource(R.string.v3_summary_soft_started))
                BigStat(onTime.toString(), stringResource(R.string.v3_summary_soft_on_time))
            }
        }
        Text(
            stringResource(R.string.v3_summary_per_day),
            color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium
        )
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

/** Контейнер одного вікна: час і «Добре» в шапці, під ними дві смуги «Було» / «Тепер» з підписаними значеннями. */
@Composable
private fun WindowSummary(w: RiskWindowUi, onApplyShift: (String) -> Unit, onDismissShift: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(TeperaPalette.heatmapNoDataFill.copy(alpha = 0.35f)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatWindowTime(w.startMinute, w.endMinute),
                    color = TeperaPalette.textPrimary, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium
                )
            }
            if (w.okThisWeek > 0) {
                TeperaChip(label = stringResource(R.string.v3_summary_stat_ok), value = w.okThisWeek.toString(), compact = true)
            }
        }
        // Обидві смуги в одному масштабі (більше з двох значень = повна довжина), щоб різницю було видно, а не вгадувати.
        val scale = maxOf(w.baselineMinutes ?: 0.0, w.currentMinutes ?: 0.0, 1.0)
        MinutesBar(stringResource(R.string.v3_summary_stat_before), w.baselineMinutes, scale, TeperaPalette.textSecondary.copy(alpha = 0.45f))
        MinutesBar(stringResource(R.string.v3_summary_stat_now), w.currentMinutes, scale, TeperaPalette.brandAccent)
        if (w.shiftStart != null && w.shiftEnd != null) {
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

/** Рядок «підпис — смуга — значення»; без даних смуга порожня, значення «—». */
@Composable
private fun MinutesBar(caption: String, minutes: Double?, scale: Double, color: androidx.compose.ui.graphics.Color) {
    val value = minutes?.let { stringResource(R.string.v3_dur_minutes, it.roundToInt()) } ?: "—"
    val fraction = (minutes ?: 0.0).div(scale).toFloat().coerceIn(0f, 1f)
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(caption, color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.width(56.dp))
        Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(TeperaPalette.cardSurface)) {
            // Навіть мала ненульова величина лишає помітну смужку (мінімум — висота смуги), нуль — порожньо.
            if (fraction > 0f) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceAtLeast(0.04f)).clip(RoundedCornerShape(6.dp)).background(color))
            }
        }
        Text(
            value, color = TeperaPalette.textPrimary, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.width(52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/** Велике число над підписом у тональному контейнері. */
@Composable
private fun RowScope.BigStat(value: String, caption: String) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(TeperaPalette.heatmapNoDataFill.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 10.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, color = TeperaPalette.textPrimary, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
        Text(caption, color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

private const val COLLAPSED_COUNT = 3

package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlin.math.roundToInt

/**
 * v3.0 MVP, Етап 5 (M5): тижневий підсумок прямо на Home, без окремого пуша. Лише те, що вимірюється: хвилини в
 * стрічках у вікні (спостереження → останні 7 діб) і «Добре» на нагадуваннях. Без оцінок, серій і «тримався плану».
 * Редизайн: цифри — плитками (число велике, підпис під ним) замість суцільного тексту, щоб підсумок читався одним
 * поглядом. Якщо вікно зсунулося — питаємо, а не міняємо мовчки.
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
        // Третя цифра з макета «М'який вихід»: скільки сесій «на 5 хв» завершено вчасно (до +2 хв). Без оцінок.
        softExit?.let { (started, onTime) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(started.toString(), stringResource(R.string.v3_summary_soft_started))
                StatTile(onTime.toString(), stringResource(R.string.v3_summary_soft_on_time))
            }
        }
        windows.forEachIndexed { index, w ->
            if (index > 0 || softExit != null) HorizontalDivider(color = TeperaPalette.borderDefault)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    formatWindowTime(w.startMinute, w.endMinute),
                    color = TeperaPalette.textPrimary, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.v3_summary_per_day),
                    color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(minutesText(w.baselineMinutes), stringResource(R.string.v3_summary_stat_before))
                    StatTile(minutesText(w.currentMinutes), stringResource(R.string.v3_summary_stat_now))
                }
                if (w.okThisWeek > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(w.okThisWeek.toString(), stringResource(R.string.v3_summary_stat_ok))
                    }
                }
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
    }
}

/** Плитка «підпис ← число»: підпис 14sp приглушеним ліворуч (може йти у 2 рядки), число 18sp Medium праворуч — низька, читається як рядок. */
@Composable
private fun RowScope.StatTile(value: String, caption: String) {
    Row(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(TeperaPalette.labelChipFill)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(caption, color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
        Text(value, color = TeperaPalette.textPrimary, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun minutesText(value: Double?): String =
    value?.let { stringResource(R.string.v3_dur_minutes, it.roundToInt()) } ?: "—"

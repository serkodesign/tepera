package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlin.math.roundToInt

/**
 * v3.0 MVP, Етап 5 (M5): тижневий підсумок прямо на Home, без окремого пуша. Лише те, що вимірюється: хвилини в
 * стрічках у вікні (спостереження → останні 7 діб) і «Добре» на нагадуваннях. Без оцінок, серій і «тримався плану».
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
    V3Card {
        Text(stringResource(R.string.v3_summary_title), color = TeperaPalette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(stringResource(R.string.v3_summary_intro), color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        // Третя цифра з макета «М'який вихід»: скільки сесій «на 5 хв» завершено вчасно (до +2 хв). Без оцінок.
        softExit?.let { (started, onTime) ->
            Text(
                stringResource(R.string.v3_summary_soft_exit, started, onTime),
                color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp
            )
        }
        windows.forEach { w ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatWindowTime(w.startMinute, w.endMinute), color = TeperaPalette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.v3_summary_minutes, minutesText(w.baselineMinutes), minutesText(w.currentMinutes)),
                    color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp
                )
                if (w.plan != null) {
                    Text(
                        stringResource(R.string.v3_summary_ok, w.okThisWeek, w.notTodayThisWeek),
                        color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp
                    )
                }
                if (w.shiftStart != null && w.shiftEnd != null) {
                    Text(
                        stringResource(R.string.v3_summary_shift, formatWindowTime(w.shiftStart, w.shiftEnd)),
                        color = TeperaPalette.textPrimary, fontSize = 14.sp, lineHeight = 18.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TeperaButton(stringResource(R.string.v3_summary_shift_apply), onClick = { onApplyShift(w.id) })
                        TeperaButton(
                            stringResource(R.string.v3_summary_shift_keep), onClick = { onDismissShift(w.id) },
                            type = TeperaButtonType.Secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun minutesText(value: Double?): String =
    value?.let { stringResource(R.string.v3_dur_minutes, it.roundToInt()) } ?: "—"

package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.home.BalanceUiState
import com.serkodesign.tepera.ui.home.heroDuration
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * Режим v3 (beta): картка «Online сьогодні» зі звичайного Home — лише число й тиха шкала з позначкою орієнтиру.
 * Позначка без підпису й ніколи не змінює колір при перевищенні (FR-3.10, ніякого світлофора). Без доступу до статистики
 * не показується: заклик дати доступ уже є на екрані.
 */
@Composable
fun OnlineTodayCard(state: BalanceUiState) {
    if (state.hasUsageAccess != true) return
    val target = state.targetMinutes
    TeperaCard(title = stringResource(R.string.v3_online_today_title)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                heroDuration(state.onlineMinutes), color = TeperaPalette.textPrimary,
                fontSize = 32.sp, lineHeight = 36.sp
            )
            if (target != null) {
                Text(
                    stringResource(R.string.v3_online_target_format, heroDuration(target)),
                    color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
                )
            }
        }
        if (target != null) {
            // Шкала до 1.25× орієнтира (або більше, якщо Online його перевищив); позначка — на місці орієнтира.
            val scale = maxOf(state.onlineMinutes, (target * 1.25f).toInt()).coerceAtLeast(1)
            Box(
                Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(TeperaPalette.heatmapNoDataFill)
            ) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(state.onlineMinutes.toFloat() / scale)
                        .background(TeperaPalette.onlineCard)
                )
                Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                    Box(Modifier.fillMaxWidth(target.toFloat() / scale).fillMaxHeight())
                    Box(Modifier.width(2.dp).fillMaxHeight().background(TeperaPalette.textPrimary))
                }
            }
        }
    }
}


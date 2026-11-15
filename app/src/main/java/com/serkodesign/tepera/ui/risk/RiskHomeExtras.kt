package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.ui.home.BalanceUiState
import com.serkodesign.tepera.ui.home.heroDuration
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.minuteOfDay
import com.serkodesign.tepera.util.startOfTodayMillis

private const val TYPICAL_WINDOW_DAYS = 7
private const val TYPICAL_MIN_DAYS = 3
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Скільки Online зазвичай набігає до [minuteOfDay]: середнє на день за [days] попередніх діб. [hourly] — сума Online-хвилин
 * по кожній з 24 годин за ці доби. Поточна година враховується пропорційно.
 */
internal fun typicalOnlineSoFar(hourly: List<Int>, days: Int, minuteOfDay: Int): Int {
    if (days <= 0 || hourly.size < 24) return 0
    val hour = (minuteOfDay / 60).coerceIn(0, 23)
    val whole = (0 until hour).sumOf { hourly[it] }
    val partial = hourly[hour] * (minuteOfDay % 60) / 60f
    return ((whole + partial) / days).toInt()
}

/**
 * Режим v3 (beta): картка «Online сьогодні» — число, тиха шкала з позначкою орієнтиру й друга, ледь помітна позначка «зазвичай до
 * цього часу» (порівняння лише з власною історією, FR-P.5). Позначки без підписів і ніколи не змінюють колір при перевищенні
 * (FR-3.10, ніякого світлофора). Без доступу до статистики не показується: заклик дати доступ уже є на екрані.
 */
@Composable
fun OnlineTodayCard(state: BalanceUiState) {
    if (state.hasUsageAccess != true) return
    val context = LocalContext.current
    val target = state.targetMinutes
    // «Зазвичай»: лише коли є хоча б кілька днів реальної історії; null — нічого не показуємо.
    var typical by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.onlineMinutes) {
        val repo = (context.applicationContext as TeperaApp).patternRepository
        val now = System.currentTimeMillis()
        val days = repo.availableHistoryDays(now, TYPICAL_WINDOW_DAYS)
        typical = if (days < TYPICAL_MIN_DAYS) null else {
            val todayStart = startOfTodayMillis()
            val hourly = repo.hourlyOnlineMinutes(todayStart - days * DAY_MILLIS, todayStart)
            typicalOnlineSoFar(hourly, days, minuteOfDay(now))
        }
    }
    val typicalNow = typical
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
        if (target != null || typicalNow != null) {
            // Шкала до 1.25× більшого з орієнтира й «зазвичай» (або більше, якщо Online їх перевищив).
            val reference = maxOf(target ?: 0, typicalNow ?: 0)
            val scale = maxOf(state.onlineMinutes, (reference * 1.25f).toInt()).coerceAtLeast(1)
            Box(
                Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(TeperaPalette.heatmapNoDataFill)
            ) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(state.onlineMinutes.toFloat() / scale)
                        .background(TeperaPalette.onlineCard)
                )
                if (typicalNow != null) {
                    // Тихіша позначка «зазвичай»: той самий розмір, але напівпрозора.
                    Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                        Box(Modifier.fillMaxWidth((typicalNow.toFloat() / scale).coerceIn(0f, 1f)).fillMaxHeight())
                        Box(Modifier.width(2.dp).fillMaxHeight().background(TeperaPalette.textSecondary))
                    }
                }
                if (target != null) {
                    Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                        Box(Modifier.fillMaxWidth((target.toFloat() / scale).coerceIn(0f, 1f)).fillMaxHeight())
                        Box(Modifier.width(2.dp).fillMaxHeight().background(TeperaPalette.textPrimary))
                    }
                }
            }
        }
        if (typicalNow != null) {
            Text(
                stringResource(R.string.v3_online_typical_format, heroDuration(typicalNow)),
                color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
            )
        }
    }
}

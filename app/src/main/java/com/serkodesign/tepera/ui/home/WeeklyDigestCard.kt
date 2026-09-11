package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour

/**
 * "Цей тиждень" — досліджено з Figma-макета (node 2062:2862, "This week"), той самий "скляний"
 * стиль карток стеку (WeeklyReflectionCard/PatternMiniCard). Компактно (FR-D.11-подібний принцип):
 * рядок заголовка + до трьох коротких рядків метрик, без ілюстрацій. "×" у заголовку — закриття,
 * якщо прочитав (не в SRS, за запитом користувача), до наступної доби.
 */
@Composable
fun WeeklyDigestCard(state: WeeklyDigestUiState, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ContextCardHeader(title = stringResource(R.string.weekly_digest_card_title), onDismiss = onDismiss)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (state.readingCount > 0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.category_reading), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(
                            R.string.weekly_digest_reading_format,
                            state.readingCount,
                            formatDuration(state.readingMinutes)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (state.movementCount > 0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.category_movement), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.weekly_digest_count_format, state.movementCount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            state.dayUsuallyStartsMinuteOfDay?.let { minuteOfDay ->
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.weekly_digest_day_start_label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatTimeOfDay(minuteOfDay),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun formatDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}

private fun formatTimeOfDay(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

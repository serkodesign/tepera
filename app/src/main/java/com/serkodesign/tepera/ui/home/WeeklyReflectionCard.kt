package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * FR-P.1, розділ 4.4 SRS: питання + три кнопки-діапазони + видимий підпис "Можна пропустити".
 * Показується інлайн на Home, над карткою "Мій день" (референс-макет для цієї конкретної
 * картки нема — той самий "скляний" стиль картки-обгортки, що й решта Home, для узгодженості).
 */
@Composable
fun WeeklyReflectionCard(state: WeeklyReflectionUiState, onSelectGuess: (WeeklyOnlineGuess) -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.guess == null) {
            Text(
                text = stringResource(R.string.weekly_reflection_question),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeeklyOnlineGuess.entries.forEach { guess ->
                    OutlinedButton(onClick = { onSelectGuess(guess) }, modifier = Modifier.weight(1f)) {
                        Text(guessLabel(guess))
                    }
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.weekly_reflection_skip))
            }
        } else {
            // FR-P.6: дві цифри поруч, без "вище/нижче" — рефлексію робить сама людина.
            Text(
                stringResource(R.string.weekly_reflection_your_guess_format, guessLabel(state.guess)),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                stringResource(R.string.weekly_reflection_actual_format, formatDuration(state.actualMinutes)),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.weekly_reflection_done))
            }
        }
    }
}

@Composable
private fun guessLabel(guess: WeeklyOnlineGuess): String = when (guess) {
    WeeklyOnlineGuess.UNDER_10 -> stringResource(R.string.weekly_reflection_range_under_10)
    WeeklyOnlineGuess.FROM_10_TO_20 -> stringResource(R.string.weekly_reflection_range_10_20)
    WeeklyOnlineGuess.OVER_20 -> stringResource(R.string.weekly_reflection_range_over_20)
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

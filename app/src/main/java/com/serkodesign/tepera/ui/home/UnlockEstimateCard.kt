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

/**
 * T-14 (tepera-dev-spec.md): "Скільки разів, по-твоєму, ти вчора розблоковував телефон?" — той
 * самий формат картки, що [WeeklyReflectionCard] (питання + три кнопки-діапазони, потім дві
 * цифри поруч). Жодного "×" в заголовку — "Можна пропустити"/"Гаразд" уже покривають закриття,
 * той самий принцип, що й у WeeklyReflectionCard.
 */
@Composable
fun UnlockEstimateCard(state: UnlockEstimateUiState, onSelectGuess: (UnlockCountGuess) -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.guess == null) {
            Text(
                text = stringResource(R.string.unlock_estimate_question),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UnlockCountGuess.entries.forEach { guess ->
                    OutlinedButton(onClick = { onSelectGuess(guess) }, modifier = Modifier.weight(1f)) {
                        Text(unlockGuessLabel(guess))
                    }
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.weekly_reflection_skip))
            }
        } else {
            // FR-P.6/розділ 2.2: дві цифри поруч, без "багато"/"мало" — рефлексію робить сама людина.
            Text(
                stringResource(R.string.weekly_reflection_your_guess_format, unlockGuessLabel(state.guess)),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                stringResource(R.string.weekly_reflection_actual_format, state.actualCount.toString()),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.weekly_reflection_done))
            }
        }
    }
}

@Composable
private fun unlockGuessLabel(guess: UnlockCountGuess): String = when (guess) {
    UnlockCountGuess.UNDER_30 -> stringResource(R.string.unlock_estimate_range_under_30)
    UnlockCountGuess.THIRTY_TO_70 -> stringResource(R.string.unlock_estimate_range_30_70)
    UnlockCountGuess.OVER_70 -> stringResource(R.string.unlock_estimate_range_over_70)
}

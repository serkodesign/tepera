package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * T-10 (tepera-dev-spec.md): "О котрій ти вчора востаннє брав телефон?" — той самий формат, що
 * [WeeklyReflectionCard]/[UnlockEstimateCard] (питання → діапазони → дві цифри поруч), з ОДНІЄЮ
 * відмінністю за прямою вимогою документа: тихий, без підпису третій рядок — медіана за 7 днів
 * (менший, приглушений стиль, без порівняння з двома цифрами вище).
 */
@Composable
fun LastPhoneUseEstimateCard(
    state: LastPhoneUseEstimateUiState,
    onSelectGuess: (LastPhoneUseGuess) -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
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
                text = stringResource(R.string.last_phone_use_estimate_question),
                style = MaterialTheme.typography.titleMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LastPhoneUseGuess.entries.forEach { guess ->
                    OutlinedButton(onClick = { onSelectGuess(guess) }, modifier = Modifier.fillMaxWidth()) {
                        Text(lastPhoneUseGuessLabel(guess))
                    }
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.weekly_reflection_skip))
            }
        } else {
            // FR-D.7b/розділ 2.2: дві цифри поруч, без "пізно"/"рано"/"вдалося"/"варто".
            Text(
                stringResource(R.string.weekly_reflection_your_guess_format, lastPhoneUseGuessLabel(state.guess)),
                style = MaterialTheme.typography.bodyLarge
            )
            state.actualMillis?.let { actual ->
                Text(
                    stringResource(R.string.weekly_reflection_actual_format, timeFormat.format(Date(actual))),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            // Документ: "другим рядком, тихіше — медіана за 7 днів, без підпису й без порівняння".
            state.medianMillis?.let { median ->
                Text(
                    timeFormat.format(Date(median)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.weekly_reflection_done))
            }
        }
    }
}

@Composable
private fun lastPhoneUseGuessLabel(guess: LastPhoneUseGuess): String = when (guess) {
    LastPhoneUseGuess.BEFORE_22 -> stringResource(R.string.last_phone_use_estimate_range_before_22)
    LastPhoneUseGuess.FROM_22_TO_23 -> stringResource(R.string.last_phone_use_estimate_range_22_23)
    LastPhoneUseGuess.FROM_23_TO_00 -> stringResource(R.string.last_phone_use_estimate_range_23_00)
    LastPhoneUseGuess.FROM_00_TO_01 -> stringResource(R.string.last_phone_use_estimate_range_00_01)
    LastPhoneUseGuess.AFTER_01 -> stringResource(R.string.last_phone_use_estimate_range_after_01)
}

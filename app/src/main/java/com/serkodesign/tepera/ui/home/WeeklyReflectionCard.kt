package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour

/**
 * FR-P.1, розділ 4.4 SRS: питання + три кнопки-діапазони + видимий підпис "Можна пропустити".
 * Показується інлайн на Home у стеку контекстних карток, той самий "скляний" стиль, що решта.
 *
 * **Перероблено 13.10.2026 за прямим запитом власника — ближче до анатомії картки Material 3:**
 * - Заголовок — M3 titleMedium (16/24 Medium, шрифт заголовків, `ink`), позначений як heading — замість жирного
 *   Roboto; під ним допоміжний текст (bodyMedium, `ink` 70%), що пояснює, що буде після вибору.
 * - Варіанти — один ряд Secondary-кнопок з короткими підписами ("<3 год" / "3–5" / "5+ год"), бо в трьох рівних
 *   кнопках довші підписи обрізались "…" (кнопка дизайн-системи не переносить текст).
 * - Питання — про середній Online на день за тиждень (не суму за тиждень), як питання онбордингу про вчора.
 * - Дії картки — праворуч унизу, як у M3 (Tertiary "Можна пропустити"; у режимі відповіді — Primary "Гаразд"),
 *   а не кнопка на всю ширину чи притиснута ліворуч.
 * - У режимі відповіді заголовок лишається — два числа поруч завжди мають контекст (FR-P.6), без жодного
 *   "вище/нижче".
 * Внутрішні проміжки — крок M3: 4dp між заголовком і текстом, 16dp між блоками.
 */
@Composable
fun WeeklyReflectionCard(state: WeeklyReflectionUiState, onSelectGuess: (WeeklyOnlineGuess) -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .contextInfoSurface()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.weekly_reflection_question),
                modifier = Modifier.semantics { heading() },
                color = TeperaPalette.buttonBrandDark,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            )
            if (state.guess == null) {
                Text(
                    text = stringResource(R.string.weekly_reflection_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f)
                )
            }
        }

        if (state.guess == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeeklyOnlineGuess.entries.forEach { guess ->
                    TeperaButton(
                        text = guessLabel(guess),
                        onClick = { onSelectGuess(guess) },
                        modifier = Modifier.weight(1f),
                        type = TeperaButtonType.Secondary
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TeperaButton(
                    text = stringResource(R.string.weekly_reflection_skip),
                    onClick = onDismiss,
                    type = TeperaButtonType.Tertiary
                )
            }
        } else {
            // FR-P.6: дві цифри поруч, без "вище/нижче" — рефлексію робить сама людина.
            GuessRevealRow(guessValue = guessLabel(state.guess), actualValue = formatDuration(state.actualMinutes))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TeperaButton(
                    text = stringResource(R.string.weekly_reflection_done),
                    onClick = onDismiss,
                    type = TeperaButtonType.Primary
                )
            }
        }
    }
}

@Composable
private fun guessLabel(guess: WeeklyOnlineGuess): String = when (guess) {
    WeeklyOnlineGuess.UNDER_3 -> stringResource(R.string.weekly_reflection_range_under_3)
    WeeklyOnlineGuess.FROM_3_TO_5 -> stringResource(R.string.weekly_reflection_range_3_5)
    WeeklyOnlineGuess.OVER_5 -> stringResource(R.string.weekly_reflection_range_over_5)
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

package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.onboarding.DailyOnlineGuess
import com.serkodesign.tepera.ui.onboarding.dailyGuessLabel
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour

/**
 * T-3 (tepera-dev-spec.md), розділ 2.2: "два числа поруч" — той самий стиль, що
 * [WeeklyReflectionCard], "твоя оцінка" показує ОРИГІНАЛЬНИЙ текст діапазону (не перетворену
 * цифру — людина обирала діапазон, не число), "насправді" — реальні хвилини. Без "×" на per-gap
 * рівні (тут нема кількох елементів) — [ContextCardHeader] закриває картку назавжди для цього
 * розкриття (`OnlineEstimateRevealViewModel.dismiss()`).
 */
@Composable
fun OnlineEstimateRevealCard(state: OnlineEstimateRevealUiState, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ContextCardHeader(title = stringResource(R.string.online_estimate_reveal_title), onDismiss = onDismiss)
        val guess = DailyOnlineGuess.fromRepresentativeMinutes(state.estimatedMinutes)
        GuessRevealRow(
            guessValue = guess?.let { dailyGuessLabel(it) } ?: formatDuration(state.estimatedMinutes),
            actualValue = formatDuration(state.actualMinutes)
        )
    }
}

@Composable
private fun formatDuration(minutes: Long): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes.toInt())
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}

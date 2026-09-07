package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour

/**
 * FR-3.1–3.6: "Life balance" — стиль з Figma-фрейму Everyday_Designs (Home screen, node
 * 1930:233): два пропорційні блоки Offline/Online (не тонкий бар), з вертикальною позначкою
 * таргету (стрілки згори й знизу лінії) на місці Online-таргету всередині Online-блоку.
 */
@Composable
fun LifeBalanceSection(
    state: BalanceUiState,
    onOpenUsageAccessSettings: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state.hasUsageAccess) {
        null -> Unit // перевірка ще триває, секція мовчить, щоб не блимати fallback-текстом
        false -> {
            Card(modifier = modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.usage_access_prompt_title), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.usage_access_prompt_body), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onLearnMore) { Text(stringResource(R.string.usage_access_learn_more)) }
                        Button(onClick = onOpenUsageAccessSettings) { Text(stringResource(R.string.usage_access_open_settings)) }
                    }
                }
            }
        }
        true -> {
            val offlineWeight = state.offlineMinutes.coerceAtLeast(1).toFloat()
            val onlineWeight = state.onlineMinutes.coerceAtLeast(1).toFloat()

            // Позначка таргету — той самий лінійний діапазон 0-8 год, що й HourRangeSlider у
            // Налаштуваннях (FR-3.4), але ОБЕРНЕНИЙ відносно нього за прямим запитом користувача:
            // там 0h зліва, 8h справа (заповнення росте вправо); тут навпаки — 0h справа, 8h
            // зліва (targetSliderMaxHours той самий maxHours=8, що в SettingsScreen.HourRangeSlider).
            val targetSliderMaxHours = 8f
            val targetHours = (state.targetMinutes / 60f).coerceIn(0f, targetSliderMaxHours)
            val markerFraction = 1f - (targetHours / targetSliderMaxHours)

            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                val markerX = maxWidth * markerFraction
                Column {
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.balance_target_label),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.offset(x = (markerX - 24.dp).coerceAtLeast(0.dp))
                        )
                    }
                    Box(Modifier.fillMaxWidth()) {
                        Text("▼", style = MaterialTheme.typography.bodySmall, modifier = Modifier.offset(x = markerX - 8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(121.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BalanceBlock(
                            label = stringResource(R.string.balance_offline_label),
                            valueText = formatBalanceDuration(state.offlineMinutes),
                            color = TeperaPalette.offlineCard,
                            modifier = Modifier.weight(offlineWeight)
                        )
                        BalanceBlock(
                            label = stringResource(R.string.balance_online_label),
                            valueText = formatBalanceDuration(state.onlineMinutes),
                            color = TeperaPalette.onlineCard,
                            modifier = Modifier.weight(onlineWeight)
                        )
                    }
                    Box(Modifier.fillMaxWidth()) {
                        Text("▲", style = MaterialTheme.typography.bodySmall, modifier = Modifier.offset(x = markerX - 8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Offline/Online картки показують години+хвилини з округленням до 15 хв (за запитом
 * користувача, замість "245 хв") — напр. "3 год 45 хв", а не "3 год 47 хв".
 */
@Composable
private fun formatBalanceDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}

@Composable
private fun BalanceBlock(label: String, valueText: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(color, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

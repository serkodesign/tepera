package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.pattern.HourlyHeatGridTall
import com.serkodesign.tepera.ui.pattern.PatternUiState
import com.serkodesign.tepera.ui.theme.TeperaChip

/**
 * FR-D.8/D.9, друга сторінка горизонтального пейджера Home (Figma "App concept"
 * k6s4prQ9oK9x2uUvzHRghR, node 192:726, "Day usage"): заголовок + ⓘ + "×", сітка 12x2
 * (`HourlyHeatGrid`, та сама, що на Stats), і внизу фіолетова плашка "Перше розблокування HH:MM"
 * ВЧОРАШНЬОЇ доби. Показує календарне вчора (`PatternViewModel` з periodDays = 1), заголовок —
 * "Патерн екрану вчора" (за запитом користувача; макет каже "Day usage"). Легенда лишається з
 * "Без даних" (за запитом користувача; макет каже "other day"). Кнопки закриття "×" нема (за запитом користувача).
 */
@Composable
fun PatternMiniCard(state: PatternUiState, modifier: Modifier = Modifier) {
    var showInfo by remember { mutableStateOf(false) }

    HomeCardSurface(modifier = modifier) {
        HomeCardTitleRow(
            title = stringResource(R.string.pattern_card_title),
            onInfo = { showInfo = true }
        )
        if (!state.hasEnoughData) {
            Text(stringResource(R.string.pattern_empty_state), style = MaterialTheme.typography.bodySmall, color = HomeCardTextSecondary)
        }
        // Висока сітка 6×4 бере всю вільну висоту картки (усі картки пейджера однакові за висотою).
        HourlyHeatGridTall(hourlyMinutes = if (state.hasEnoughData) state.hourlyMinutes else null, modifier = Modifier.weight(1f))
        state.firstUnlockMinuteOfDay?.let { minuteOfDay ->
            TeperaChip(
                label = stringResource(R.string.home_card_first_unlock_label),
                value = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
            )
        }
    }

    if (showInfo) {
        HomeInfoDialog(
            title = stringResource(R.string.pattern_card_title),
            body = stringResource(R.string.day_usage_info_body),
            onDismiss = { showInfo = false }
        )
    }
}

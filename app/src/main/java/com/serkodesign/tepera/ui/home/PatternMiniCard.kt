package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.serkodesign.tepera.ui.pattern.HourlyHeatGrid
import com.serkodesign.tepera.ui.pattern.PatternUiState
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * FR-D.8/D.9, компактна версія для стеку контекстних карток на Home (FR-D.11: без коментаря
 * поверх патерну — лише заголовок + сітка). Повна версія — `PatternCard` на Stats
 * (StatsScreen.kt), той самий `HourlyHeatGrid` — за прямим рішенням користувача ОБИДВІ версії
 * ідентичні (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 154:287), без окремого
 * "стиснутого" варіанта для Home.
 * "×" у заголовку — закриття, якщо прочитав (не в SRS, за запитом користувача), до наступної доби.
 * Заголовок — "Патерн екрану вчора": за прямим запитом користувача Home показує саме
 * календарне вчора (`PatternViewModel` з periodDays = 1), не середнє за тиждень. Стилізований
 * як заголовок секції "Активності" (`titleMedium` + Bold).
 */
@Composable
fun PatternMiniCard(state: PatternUiState, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ContextCardHeader(
            title = stringResource(R.string.pattern_card_title),
            onDismiss = onDismiss,
            titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        if (!state.hasEnoughData) {
            Text(stringResource(R.string.pattern_empty_state), style = MaterialTheme.typography.bodySmall)
        }
        HourlyHeatGrid(hourlyMinutes = if (state.hasEnoughData) state.hourlyMinutes else null)
    }
}

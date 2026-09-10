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
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.pattern.HourlyHeatStrip
import com.serkodesign.tepera.ui.pattern.PatternUiState
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * FR-D.8/D.9, компактна версія для стеку контекстних карток на Home (FR-D.11: 1-2 рядки, без
 * коментаря поверх патерну — лише заголовок + смужка). Повна версія з годинними позначками —
 * `PatternCard` на Stats (StatsScreen.kt), той самий `HourlyHeatStrip`, просто вищий.
 */
@Composable
fun PatternMiniCard(state: PatternUiState) {
    if (!state.visible) return

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.pattern_card_title), style = MaterialTheme.typography.bodyMedium)
        if (state.hasEnoughData) {
            HourlyHeatStrip(hourlyMinutes = state.hourlyMinutes, height = 20.dp)
        } else {
            Text(stringResource(R.string.pattern_empty_state), style = MaterialTheme.typography.bodySmall)
        }
    }
}

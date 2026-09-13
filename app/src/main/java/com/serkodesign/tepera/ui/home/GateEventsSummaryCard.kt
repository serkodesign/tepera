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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * T-6 (tepera-dev-spec.md), FR-P.3: "свідчення компетентності замість натхнення" — один тихий
 * рядок факту, той самий "скляний" стиль, що решта карток стеку. Буквальна вимога приймання:
 * жодного стріку, жодного співвідношення, жодного натяку на те, що PROCEEDED — невдача (тут
 * узагалі не з'являється жодне число, крім кількості CANCELLED).
 */
@Composable
fun GateEventsSummaryCard(state: GateEventsSummaryUiState, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ContextCardHeader(title = stringResource(R.string.gate_events_summary_card_title), onDismiss = onDismiss)
        Text(
            pluralStringResource(R.plurals.gate_events_summary_text, state.cancelledCount, state.cancelledCount),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

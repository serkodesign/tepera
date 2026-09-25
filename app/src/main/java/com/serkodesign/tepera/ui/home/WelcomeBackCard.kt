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
 * CC-4: підсумок днів після перерви. Тон — спокійна констатація: що відмічено й скільки було Online.
 * Жодних слів про «пропущене» чи «відсутність», жодного порівняння з нормою чи вимоги щось надолужити.
 * Той самий «скляний» стиль і закриття «×», що в решти карток стеку.
 */
@Composable
fun WelcomeBackCard(state: WelcomeBackUiState, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ContextCardHeader(title = stringResource(R.string.welcome_back_title), onDismiss = onDismiss)
        Text(
            text = pluralStringResource(R.plurals.welcome_back_period, state.days, state.days),
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.loggedMinutes > 0) {
            Text(
                text = stringResource(R.string.welcome_back_logged_format, formatBalanceDuration(state.loggedMinutes)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        state.averageOnlineMinutes?.let {
            Text(
                text = stringResource(R.string.welcome_back_online_format, formatBalanceDuration(it)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

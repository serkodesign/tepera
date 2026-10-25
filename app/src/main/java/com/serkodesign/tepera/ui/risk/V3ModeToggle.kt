package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.TeperaHint
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import kotlinx.coroutines.launch

/**
 * v3.0 MVP (лише beta): перемикач режиму «Години ризику» замість звичайного Home. Нічого не видаляє: стара Home, її
 * дані й екрани повертаються вимкненням перемикача.
 */
@Composable
fun V3ModeToggle(settingsStore: SettingsStore, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val enabled by settingsStore.v3ModeEnabled.collectAsState(initial = false)
    Column(modifier = modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassRow(
            label = stringResource(R.string.v3_mode_label),
            leading = { TeperaIconCircle(TeperaSymbols.AutoAwesome) },
            trailing = {
                Switch(
                    checked = enabled,
                    onCheckedChange = { on -> scope.launch { settingsStore.setV3ModeEnabled(on) } },
                    colors = teperaSwitchColors()
                )
            }
        )
        TeperaHint(text = stringResource(R.string.v3_mode_hint), modifier = Modifier.padding(horizontal = 12.dp))
    }
}

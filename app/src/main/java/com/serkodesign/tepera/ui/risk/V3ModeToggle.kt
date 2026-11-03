package com.serkodesign.tepera.ui.risk

import com.serkodesign.tepera.BuildConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.util.GateAccessibility
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.gates.AccessibilityDisclosureDialog
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
    val enabled by settingsStore.v3ModeEnabled.collectAsState(initial = BuildConfig.V3_MVP)
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
        val context = LocalContext.current
        var overlayOn by remember { mutableStateOf(PlanOverlayStore.isEnabled(context)) }
        var showA11yDisclosure by remember { mutableStateOf(false) }
        var a11yOn by remember { mutableStateOf(GateAccessibility.isEnabled(context)) }
        LifecycleResumeEffect(Unit) {
            a11yOn = GateAccessibility.isEnabled(context)
            onPauseOrDispose { }
        }
        GlassRow(
            label = stringResource(R.string.v3_overlay_toggle_label),
            leading = { TeperaIconCircle(TeperaSymbols.Schedule) },
            trailing = {
                Switch(
                    checked = overlayOn,
                    onCheckedChange = { on ->
                        overlayOn = on
                        PlanOverlayStore.setEnabled(context, on)
                        // Play: помітне розкриття ПЕРЕД відправкою в системні налаштування Accessibility.
                        if (on && GateAccessibility.isAvailable && !GateAccessibility.isEnabled(context)) {
                            showA11yDisclosure = true
                        }
                    },
                    colors = teperaSwitchColors()
                )
            }
        )
        if (showA11yDisclosure) {
            AccessibilityDisclosureDialog(
                onDismiss = { showA11yDisclosure = false },
                onContinue = {
                    showA11yDisclosure = false
                    GateAccessibility.openSettings(context)
                }
            )
        }
        TeperaHint(text = stringResource(R.string.v3_overlay_toggle_hint), modifier = Modifier.padding(horizontal = 12.dp))
        if (overlayOn && GateAccessibility.isAvailable && !a11yOn) {
            TeperaButton(
                text = stringResource(R.string.v3_overlay_enable_access),
                onClick = { showA11yDisclosure = true },
                type = TeperaButtonType.Secondary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        var puzzlesOn by remember { mutableStateOf(PlanOverlayStore.arePuzzlesEnabled(context)) }
        GlassRow(
            label = stringResource(R.string.v3_puzzle_toggle_label),
            leading = { TeperaIconCircle(TeperaSymbols.AutoAwesome) },
            trailing = {
                Switch(
                    checked = puzzlesOn,
                    onCheckedChange = { on ->
                        puzzlesOn = on
                        PlanOverlayStore.setPuzzlesEnabled(context, on)
                    },
                    colors = teperaSwitchColors()
                )
            }
        )
        TeperaHint(text = stringResource(R.string.v3_puzzle_toggle_hint), modifier = Modifier.padding(horizontal = 12.dp))
    }
}

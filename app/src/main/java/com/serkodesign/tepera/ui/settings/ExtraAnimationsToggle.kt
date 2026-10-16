package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.TeperaHint
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.util.MotionStore

/** Перемикач "Додаткові анімації" (Налаштування → Загальні): рідина в орбі, числа, що докручуються, плавні кнопки. */
@Composable
fun ExtraAnimationsToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    MotionStore.load(context)
    Column(modifier = modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassRow(
            label = stringResource(R.string.extra_animations_label),
            leading = { TeperaIconCircle(TeperaSymbols.AutoAwesome) },
            trailing = {
                Switch(
                    checked = MotionStore.extraAnimations(),
                    onCheckedChange = { MotionStore.setExtraAnimations(context, it) },
                    colors = teperaSwitchColors()
                )
            }
        )
        TeperaHint(
            text = stringResource(R.string.extra_animations_explanation),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

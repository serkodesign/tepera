package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.util.CrashReporting

/**
 * Перемикач "Звіти про збої" з чесним поясненням, що саме надсилається (D-15). Один і той самий
 * блок стоїть на першому екрані онбордингу й у Налаштуваннях → Загальні, тож вибір видно й
 * змінюється в обох місцях.
 */
@Composable
fun CrashReportsToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(CrashReporting.isEnabled(context)) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassRow(
            label = stringResource(R.string.crash_reports_label),
            leading = { TeperaIconCircle(TeperaSymbols.BugReport) },
            trailing = {
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        CrashReporting.setEnabled(context, it)
                    },
                    colors = teperaSwitchColors()
                )
            }
        )
        Text(
            text = stringResource(R.string.crash_reports_explanation),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

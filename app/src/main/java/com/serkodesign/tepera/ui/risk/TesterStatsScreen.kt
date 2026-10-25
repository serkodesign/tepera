package com.serkodesign.tepera.ui.risk

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.TesterStats
import com.serkodesign.tepera.data.repository.ReportText
import com.serkodesign.tepera.data.repository.TesterStatsRepository
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaCard

/**
 * v3.0 MVP, Етап 7: прихована «Статистика для тестувальників» (довге натискання на версію в «Про застосунок»).
 * Лише локальні дані; єдиний вихід — кнопка «Скопіювати як текст» (тестувальник сам вставляє в форму).
 */
@Composable
fun TesterStatsScreen(onBack: () -> Unit, onOpenTools: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    val clipboard = LocalClipboardManager.current
    var stats by remember { mutableStateOf<TesterStats?>(null) }

    LaunchedEffect(Unit) { stats = app.testerStatsRepository.build() }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.v3_tester_title), onBack = onBack)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val s = stats
                if (s == null) {
                    Text(stringResource(R.string.v3_tester_loading), style = MaterialTheme.typography.bodyMedium)
                } else {
                    val reportText = ReportText(
                        title = stringResource(R.string.v3_report_title),
                        firstData = stringResource(R.string.v3_report_first_data),
                        periods = stringResource(R.string.v3_report_periods),
                        periodNotSet = stringResource(R.string.v3_report_period_not_set),
                        range = stringResource(R.string.v3_report_range),
                        noWindows = stringResource(R.string.v3_report_no_windows),
                        window = stringResource(R.string.v3_report_window),
                        tagWeekdays = stringResource(R.string.v3_report_tag_weekdays),
                        tagDisabled = stringResource(R.string.v3_report_tag_disabled),
                        tagPlan = stringResource(R.string.v3_report_tag_plan),
                        tagNoPlan = stringResource(R.string.v3_report_tag_no_plan),
                        tagShift = stringResource(R.string.v3_report_tag_shift),
                        minutes = stringResource(R.string.v3_report_minutes),
                        nudges = stringResource(R.string.v3_report_nudges),
                        note = stringResource(R.string.v3_report_note)
                    )
                    val text = TesterStatsRepository.format(s, reportText)
                    TeperaCard {
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                    val copied = stringResource(R.string.v3_tester_copied)
                    TeperaButton(
                        text = stringResource(R.string.v3_tester_copy),
                        onClick = {
                            clipboard.setText(AnnotatedString(text))
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                TeperaButton(
                    text = stringResource(R.string.v3_tester_tools),
                    onClick = onOpenTools,
                    type = TeperaButtonType.Secondary
                )
            }
        }
    }
}

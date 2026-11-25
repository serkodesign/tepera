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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.serkodesign.tepera.BuildConfig
import com.serkodesign.tepera.util.PlanOverlayStore
import kotlinx.coroutines.launch
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
import androidx.core.app.NotificationManagerCompat
import com.serkodesign.tepera.data.repository.OverlayCounts
import com.serkodesign.tepera.data.repository.ReportText
import com.serkodesign.tepera.util.GateAccessibility
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
fun TesterStatsScreen(onBack: () -> Unit, onOpenTools: () -> Unit, onRestartOnboarding: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    val clipboard = LocalClipboardManager.current
    var stats by remember { mutableStateOf<TesterStats?>(null) }

    LaunchedEffect(Unit) {
        stats = app.testerStatsRepository.build().copy(
            overlay = if (BuildConfig.GATE_ACCESSIBILITY) {
                OverlayCounts(
                    PlanOverlayStore.shownCount(context), PlanOverlayStore.acceptedCount(context),
                    PlanOverlayStore.continuedCount(context), PlanOverlayStore.leftCount(context)
                )
            } else null,
            softExit = if (BuildConfig.GATE_ACCESSIBILITY) PlanOverlayStore.softExitCounts(context) else null,
            appVersion = BuildConfig.VERSION_NAME,
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            accessibilityEnabled = if (BuildConfig.GATE_ACCESSIBILITY) GateAccessibility.isEnabled(context) else null
        )
    }
    val scope = rememberCoroutineScope()

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
                        tagManual = stringResource(R.string.v3_report_tag_manual),
                        tagWeekdays =stringResource(R.string.v3_report_tag_weekdays),
                        tagDisabled = stringResource(R.string.v3_report_tag_disabled),
                        tagShift = stringResource(R.string.v3_report_tag_shift),
                        minutes = stringResource(R.string.v3_report_minutes),
                        overlay = stringResource(R.string.v3_report_overlay),
                        note = stringResource(R.string.v3_report_note),
                        softExit = stringResource(R.string.v3_report_soft_exit),
                        status = stringResource(R.string.v3_report_status),
                        yes = stringResource(R.string.v3_report_yes),
                        no = stringResource(R.string.v3_report_no)
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
                if (BuildConfig.GATE_ACCESSIBILITY) { PreviewWindowsCard(); BerserkTestCard() }
                TeperaButton(
                    text = stringResource(R.string.v3_tester_restart),
                    onClick = onRestartOnboarding,
                    type = TeperaButtonType.Secondary
                )
                TeperaButton(
                    text = stringResource(R.string.v3_tester_tools),
                    onClick = onOpenTools,
                    type = TeperaButtonType.Secondary
                )
            }
        }
    }
}

/**
 * Тестовий перегляд вікон поверх стрічок: лист плану (його малює окрема activity beta-збірки) і плашка/затемнення
 * м'якого виходу (їх малює служба доступності через [com.serkodesign.tepera.util.SoftExitPreviewBridge]). Без очікування
 * й без запису в статистику.
 */
@Composable
private fun PreviewWindowsCard() {
    val context = LocalContext.current
    TeperaCard(title = stringResource(R.string.preview_title)) {
        Text(stringResource(R.string.preview_hint), style = MaterialTheme.typography.bodyMedium)
        fun sheet(kind: String) {
            runCatching {
                context.startActivity(
                    android.content.Intent().setClassName(context, "com.serkodesign.tepera.gates.PlanOverlayActivity")
                        .putExtra("plan_preview", kind)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
        }
        fun overlay(kind: String) {
            val shown = com.serkodesign.tepera.util.SoftExitPreviewBridge.show?.invoke(kind) ?: false
            if (!shown) Toast.makeText(context, context.getString(R.string.preview_need_a11y), Toast.LENGTH_LONG).show()
        }
        listOf(
            R.string.preview_idea to { sheet("idea") },
            R.string.preview_duration to { sheet("duration") },
            R.string.preview_watching to { sheet("watching") },
            R.string.preview_puzzle to { sheet("puzzle") },
            R.string.preview_pill to { overlay("pill") },
            R.string.preview_pill_strong to { overlay("pill_strong") },
            R.string.preview_confirm to { overlay("confirm") },
            R.string.preview_toast to { overlay("toast") }
        ).forEach { (label, action) ->
            TeperaButton(text = stringResource(label), onClick = action, type = TeperaButtonType.Secondary)
        }
    }
}

/**
 * Тестові «дані» для берсерк-режиму: він працює від живих подій застосунків, тож замість даних тут швидкі значення
 * (година 1 хв, блок 2 хв) і кнопка, що одразу показує екран блоку. Екран — у beta-джерелах, тому запуск за іменем класу.
 */
@Composable
private fun BerserkTestCard() {
    val context = LocalContext.current
    var summary by remember { mutableStateOf(berserkSummary(context)) }
    TeperaCard(title = stringResource(R.string.berserk_test_title)) {
        Text(summary, style = MaterialTheme.typography.bodyMedium)
        TeperaButton(
            text = stringResource(R.string.berserk_test_fast),
            onClick = {
                com.serkodesign.tepera.util.BerserkStore.applyTestValues(context)
                summary = berserkSummary(context)
                Toast.makeText(context, context.getString(R.string.berserk_test_fast_done), Toast.LENGTH_LONG).show()
            }
        )
        TeperaButton(
            text = stringResource(R.string.berserk_test_show),
            onClick = {
                val until = System.currentTimeMillis() + 2 * 60_000L
                runCatching {
                    context.startActivity(
                        android.content.Intent().setClassName(context, "com.serkodesign.tepera.gates.BerserkActivity")
                            .putExtra("berserk_until", until).putExtra("berserk_joke", 0)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
            },
            type = TeperaButtonType.Secondary
        )
        TeperaButton(
            text = stringResource(R.string.preview_berserk_reopen),
            onClick = {
                val until = System.currentTimeMillis() + 2 * 60_000L
                runCatching {
                    context.startActivity(
                        android.content.Intent().setClassName(context, "com.serkodesign.tepera.gates.BerserkActivity")
                            .putExtra("berserk_until", until).putExtra("berserk_joke", -1)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
            },
            type = TeperaButtonType.Secondary
        )
        TeperaButton(
            text = stringResource(R.string.berserk_test_reset),
            onClick = {
                com.serkodesign.tepera.util.BerserkStore.resetToDefaults(context)
                summary = berserkSummary(context)
            },
            type = TeperaButtonType.Secondary
        )
    }
}

private fun berserkSummary(context: android.content.Context): String {
    val s = com.serkodesign.tepera.util.BerserkStore
    return "${if (s.isEnabled(context)) "ON" else "OFF"} · ${s.hourMinutes(context)} / ${s.blockMinutes(context)} min · " +
        "${s.countMode(context)} · skip today: ${if (s.isSkippedToday(context)) "yes" else "no"}"
}

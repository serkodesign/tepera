package com.serkodesign.tepera.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaCard

/**
 * v3.0 MVP, Етап 0 (`docs/CLAUDE-CODE-PLAN-v3-MVP.md`): службовий екран для перевірки на власних даних — теплова
 * карта 7×годин, кандидати у вікна й параметри алгоритму. Лише beta ([com.serkodesign.tepera.BuildConfig.V3_MVP]),
 * відкривається довгим натисканням на версію в «Про застосунок». Тексти навмисно без локалізації.
 */
@Composable
fun RiskSpikeScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val vm: RiskSpikeViewModel = viewModel(factory = RiskSpikeViewModel.Factory(app))
    val s by vm.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = "Години ризику (Етап 0)", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TeperaCard(title = "Дані", subtitle = "Покрито днів: ${s.coveredDays} · днів зі слотами: ${s.storedDays}") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TeperaButton(text = "Перерахувати", onClick = vm::recompute, enabled = !s.busy, modifier = Modifier.weight(1f))
                            TeperaButton(text = "Глибина історії", onClick = vm::checkHistoryDepth, type = TeperaButtonType.Secondary, modifier = Modifier.weight(1f))
                        }
                        if (s.status.isNotEmpty()) Text(s.status, style = MaterialTheme.typography.bodySmall)
                        if (s.historyLine.isNotEmpty()) Text(s.historyLine, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    TeperaCard(title = "Теплова карта", subtitle = "Хвилини в стрічках; колонки — години від ${s.config.dayCutoffHour}:00") {
                        Heatmap(s.result?.days.orEmpty().map { it.toString().substring(5) }, s.heat)
                    }
                }
                item {
                    TeperaCard(
                        title = "Кандидати у вікна",
                        subtitle = s.result?.let { "Логічних діб у спостереженні: ${it.observedDays} з ${s.config.observationDays}" }
                    ) {
                        val r = s.result
                        when {
                            r == null -> Text("…")
                            !r.enoughData -> Text("Замало днів даних — вікна не будуються", style = MaterialTheme.typography.bodyMedium)
                            r.candidates.isEmpty() -> Text("Стабільних вікон не знайдено", style = MaterialTheme.typography.bodyMedium)
                            else -> r.candidates.forEach {
                                Text(
                                    "${formatMinute(it.startMinute)}–${formatMinute(it.endMinute)} · ${it.recurrenceDays}/${it.observedDays} днів · ≈${it.avgMinutes} хв",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        TeperaButton(
                            text = "Скопіювати як текст",
                            onClick = { clipboard.setText(AnnotatedString(vm.report())) },
                            type = TeperaButtonType.Secondary
                        )
                    }
                }
                item { ConfigCard(s.config, vm::setConfig) }
                item {
                    TeperaCard(title = "Застосунки-стрічки", subtitle = "Обрано: ${s.feed.size}. Месенджери краще не обирати") {}
                }
                items(s.apps, key = { it.packageName }) { a ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = a.packageName in s.feed, onCheckedChange = { vm.toggleFeed(a.packageName) })
                        Text(a.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun Heatmap(rowLabels: List<String>, heat: Array<IntArray>) {
    if (heat.isEmpty()) {
        Text("Даних поки немає", style = MaterialTheme.typography.bodySmall)
        return
    }
    val base = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        heat.forEachIndexed { i, row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rowLabels.getOrElse(i) { "" }, fontSize = 10.sp, modifier = Modifier.width(36.dp))
                row.forEach { minutes ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(18.dp)
                            .padding(horizontal = 1.dp)
                            .background(base.copy(alpha = (minutes / 30f).coerceIn(0f, 1f) * 0.9f + 0.05f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigCard(cfg: RiskConfig, onChange: (RiskConfig) -> Unit) {
    TeperaCard(title = "Параметри алгоритму") {
        ConfigSlider("Активний слот, хв: ${cfg.activeSlotSeconds / 60}", cfg.activeSlotSeconds / 60f, 1f..15f) {
            onChange(cfg.copy(activeSlotSeconds = it.toInt() * 60))
        }
        ConfigSlider("Склейка проміжку, хв: ${cfg.mergeGapMinutes}", cfg.mergeGapMinutes / 15f, 0f..4f) {
            onChange(cfg.copy(mergeGapMinutes = it.toInt() * 15))
        }
        ConfigSlider("Повторюваність, днів: ${cfg.minRecurrenceDays}", cfg.minRecurrenceDays.toFloat(), 2f..7f) {
            onChange(cfg.copy(minRecurrenceDays = it.toInt(), seedMinDays = minOf(cfg.seedMinDays, it.toInt())))
        }
        ConfigSlider("Середнє у вікні, хв: ${cfg.minAvgMinutes}", cfg.minAvgMinutes.toFloat(), 5f..40f) {
            onChange(cfg.copy(minAvgMinutes = it.toInt()))
        }
        ConfigSlider("Мін. днів із даними: ${cfg.minObservedDays}", cfg.minObservedDays.toFloat(), 1f..7f) {
            onChange(cfg.copy(minObservedDays = it.toInt()))
        }
    }
}

@Composable
private fun ConfigSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = { onValue(it.roundToInt().toFloat()) },
            valueRange = range,
            steps = (range.endInclusive - range.start).roundToInt() - 1
        )
    }
}

package com.serkodesign.tepera.ui.stats

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val categoryLabelKey = ExtraStore.Key<List<String>>()
private val dayLabelKey = ExtraStore.Key<List<String>>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    balanceRepository: BalanceRepository
) {
    val viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(categoryRepository, activityRepository, balanceRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Доступ до статистики використання надається поза застосунком — оновлюємо тренд при
    // поверненні з системних Налаштувань, так само як BalanceViewModel.refresh() на Home.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshWeeklyTrend()
        onPauseOrDispose { }
    }

    // Прозорий containerColor: градієнтний фон малює зовнішній Box у TeperaNavHost (а не тут) —
    // інакше він потрапляє під contentPadding зовнішнього Scaffold і не сягає країв екрана.
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_screen_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSelector(selected = state.period, onSelect = viewModel::selectPeriod)

            CategoryBreakdownCard(items = state.categoryBreakdown)

            WeeklyTrendCard(
                points = state.weeklyTrend,
                hasUsageAccess = state.hasUsageAccess,
                onOpenUsageAccessSettings = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    val options = listOf(
        StatsPeriod.DAY to R.string.stats_period_day,
        StatsPeriod.WEEK to R.string.stats_period_week,
        StatsPeriod.MONTH to R.string.stats_period_month
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (period, labelRes) ->
            SegmentedButton(
                selected = selected == period,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(stringResource(labelRes))
            }
        }
    }
}

/** FR-5.2: стовпчикова діаграма розподілу офлайн-часу по категоріях за обраний період. */
@Composable
private fun CategoryBreakdownCard(items: List<CategoryBreakdownItem>) {
    val labels = items.map { categoryDisplayName(it.category) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.stats_category_breakdown_title), style = MaterialTheme.typography.titleMedium)

            if (items.isEmpty() || items.all { it.minutes == 0 }) {
                Text(
                    stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val modelProducer = remember { CartesianChartModelProducer() }
                LaunchedEffect(items, labels) {
                    modelProducer.runTransaction {
                        columnModel { series(items.map { it.minutes }) }
                        extras { it[categoryLabelKey] = labels }
                    }
                }
                ProvideVicoTheme(rememberM3VicoTheme()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = CartesianValueFormatter { context, x, _ ->
                                    context.model.extraStore[categoryLabelKey].getOrElse(x.toInt()) { "" }
                                }
                            )
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }
        }
    }
}

/** FR-5.3: тижневий тренд Online-ratio (частка Online-хвилин від знаменника Grace Period Buffer). */
@Composable
private fun WeeklyTrendCard(
    points: List<DailyBalancePoint>,
    hasUsageAccess: Boolean,
    onOpenUsageAccessSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.stats_weekly_trend_title), style = MaterialTheme.typography.titleMedium)

            if (!hasUsageAccess) {
                Text(stringResource(R.string.usage_access_prompt_title), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.usage_access_prompt_body), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onOpenUsageAccessSettings) {
                    Text(stringResource(R.string.usage_access_open_settings))
                }
            } else if (points.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.stats_no_data))
                }
            } else {
                val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
                val dayLabels = points.map { dayFormat.format(Date(it.dayStartMillis)) }
                val modelProducer = remember { CartesianChartModelProducer() }
                LaunchedEffect(points) {
                    modelProducer.runTransaction {
                        lineModel { series(points.map { it.onlineRatio }) }
                        extras { it[dayLabelKey] = dayLabels }
                    }
                }
                ProvideVicoTheme(rememberM3VicoTheme()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(
                                valueFormatter = CartesianValueFormatter { _, y, _ ->
                                    "${(y * 100).roundToInt()}%"
                                }
                            ),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = CartesianValueFormatter { context, x, _ ->
                                    context.model.extraStore[dayLabelKey].getOrElse(x.toInt()) { "" }
                                }
                            )
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }
        }
    }
}

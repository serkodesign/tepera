package com.serkodesign.tepera.ui.stats

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.PatternRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.pattern.HourlyHeatGrid
import com.serkodesign.tepera.ui.pattern.PatternUiState
import com.serkodesign.tepera.ui.pattern.PatternViewModel
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
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
    balanceRepository: BalanceRepository,
    patternRepository: PatternRepository,
    settingsStore: SettingsStore,
    sleepWindowRepository: SleepWindowRepository,
    unlockRepository: UnlockRepository,
    pauseRepository: PauseRepository
) {
    val viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(categoryRepository, activityRepository, balanceRepository, sleepWindowRepository, unlockRepository, pauseRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // FR-D.8/D.9: власний інстанс (окремий від Home, кожен зі своїм refresh-циклом). За прямим
    // запитом користувача тепер РЕАГУЄ на PeriodSelector — initialPeriodDays узгоджений із
    // дефолтним period == WEEK у StatsUiState(), LaunchedEffect(state.period) нижче тримає їх
    // синхронізованими далі.
    val patternViewModel: PatternViewModel = viewModel(
        factory = PatternViewModel.Factory(
            patternRepository, balanceRepository, settingsStore,
            initialPeriodDays = daysForStatsPeriod(state.period),
            respectDismissal = false
        )
    )
    val patternState by patternViewModel.uiState.collectAsState()

    // Доступ до статистики використання надається поза застосунком — оновлюємо тренд при
    // поверненні з системних Налаштувань, так само як BalanceViewModel.refresh() на Home.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshWeeklyTrend()
        patternViewModel.refresh()
        onPauseOrDispose { }
    }

    // За прямим запитом користувача: зміна День/Тиждень/Місяць одразу перераховує патерн доби
    // (середнє Online-хвилин на годину за ВІДПОВІДНЕ вікно — 1/7/30 днів), не лише bar chart і
    // тренд нижче.
    LaunchedEffect(state.period) {
        patternViewModel.refresh(daysForStatsPeriod(state.period))
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
        // Прокручуваний Column, а не fillMaxSize() без скролу: NavHost більше не резервує нижній
        // відступ під навбар-"таблетку" (TeperaNavHost.kt, той самий фікс, що й для Home), тож без
        // прокрутки й запасу знизу графік тижневого тренду обрізався б під напівпрозорою
        // "таблеткою" на екранах, де вміст не влазить.
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSelector(selected = state.period, onSelect = viewModel::selectPeriod)

            // T-14 (tepera-dev-spec.md): "доступне... в тижневому огляді — звичайним рядком,
            // без виділення" — саме тут (Stats, period == WEEK), НЕ на Home (розділ 2.2 забороняє
            // пасивний показ на головному екрані). null = нема доступу/API < 28 — рядок відсутній,
            // не "0".
            if (state.period == StatsPeriod.WEEK) {
                state.unlockStats.weekCount?.let { count ->
                    Text(
                        stringResource(R.string.stats_unlock_count_week_format, count),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // T-10: та сама медіана, що другий (тихий) рядок LastPhoneUseEstimateCard — тут
                // звичайним підписаним рядком, бо це самостійний факт серед інших рядків Stats,
                // не другорядна деталь під двома щойно показаними числами.
                state.lastPhoneUseStats.weekMedianMillis?.let { millis ->
                    Text(
                        stringResource(R.string.stats_last_phone_use_week_format, formatClockTime(millis)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            CategoryBreakdownCard(items = state.categoryBreakdown)

            WeeklyTrendCard(
                points = state.weeklyTrend,
                period = state.period,
                hasUsageAccess = state.hasUsageAccess,
                onOpenUsageAccessSettings = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )

            PatternCard(state = patternState, period = state.period)
        }
    }
}

private fun formatClockTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

/**
 * FR-D.8/D.9: повна версія теплового патерну доби — той самий `HourlyHeatGrid`, що компактна
 * картка на Home (`PatternMiniCard`); за прямим рішенням користувача обидві версії ідентичні
 * (редизайн за Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 154:287) — попередні годинні
 * підписи (0/6/12/18/23) під смугою прибрано, макет їх не показує.
 * **За прямим запитом користувача тепер РЕАГУЄ на `PeriodSelector` вище** (`StatsScreen`
 * передає `daysForStatsPeriod(period)` у `PatternViewModel.refresh()`) — кожен бакет це середнє
 * Online-хвилин на годину за відповідне вікно (1/7/30 днів), не сума, щоб довші періоди не
 * виглядали тривіально "гарячішими" лише через довжину вікна. Підпис періоду під заголовком —
 * щоб було видно, за яке саме вікно показано патерн, коли він відрізняється від дефолтного
 * тижня.
 * **Заголовок за прямим запитом користувача залежить від періоду:** `StatsPeriod.DAY` —
 * буквально вчорашня доба, лишається "Патерн екрану вчора"; `WEEK` — середнє за кілька
 * днів, не "вчора" — "Добовий патерн використання" (той самий рядок, що тепер завжди на Home).
 */
@Composable
private fun PatternCard(state: PatternUiState, period: StatsPeriod) {
    if (!state.visible) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val titleRes = if (period == StatsPeriod.DAY) {
                R.string.pattern_card_title
            } else {
                R.string.pattern_card_title_average
            }
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(periodLabelRes(period)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!state.hasEnoughData) {
                Text(stringResource(R.string.pattern_empty_state), style = MaterialTheme.typography.bodyMedium)
            }
            HourlyHeatGrid(hourlyMinutes = if (state.hasEnoughData) state.hourlyMinutes else null)
        }
    }
}

private fun periodLabelRes(period: StatsPeriod): Int = when (period) {
    StatsPeriod.DAY -> R.string.stats_period_day
    StatsPeriod.WEEK -> R.string.stats_period_week
}

@Composable
private fun PeriodSelector(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    val options = listOf(
        StatsPeriod.DAY to stringResource(R.string.stats_period_day),
        StatsPeriod.WEEK to stringResource(R.string.stats_period_week)
    )
    PillSegmentedControl(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth()
    )
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

/**
 * FR-5.3: тижневий тренд Online-часу за днями — абсолютні години (FR-P.6, SRS v2.5: голий %
 * без контексту читається як оцінка, не факт), не частка від знаменника Grace Period Buffer.
 */
@Composable
private fun WeeklyTrendCard(
    points: List<DailyBalancePoint>,
    period: StatsPeriod,
    hasUsageAccess: Boolean,
    onOpenUsageAccessSettings: () -> Unit
) {
    val hoursFormat = stringResource(R.string.hours_short_format)
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
                val dayFormat = remember(period) {
                    SimpleDateFormat("EEE", Locale.getDefault())
                }
                val dayLabels = points.map { dayFormat.format(Date(it.dayStartMillis)) }
                val modelProducer = remember { CartesianChartModelProducer() }
                LaunchedEffect(points) {
                    modelProducer.runTransaction {
                        lineModel { series(points.map { it.onlineMinutes }) }
                        extras { it[dayLabelKey] = dayLabels }
                    }
                }
                ProvideVicoTheme(rememberM3VicoTheme()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(
                                valueFormatter = CartesianValueFormatter { _, y, _ ->
                                    String.format(hoursFormat, (y / 60.0).roundToInt())
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

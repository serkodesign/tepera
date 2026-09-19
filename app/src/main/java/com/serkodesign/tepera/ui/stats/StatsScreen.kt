package com.serkodesign.tepera.ui.stats

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaScreenTitle
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.style.TextOverflow
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.util.roundToQuarterHour
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
import androidx.compose.material3.CardDefaults
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
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
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
            TeperaScreenTitle(stringResource(R.string.stats_screen_title))
        }
    ) { padding ->
        // Прокручуваний Column, а не fillMaxSize() без скролу: NavHost більше не резервує нижній
        // відступ під навбар-"таблетку" (TeperaNavHost.kt, той самий фікс, що й для Home), тож без
        // прокрутки й запасу знизу графік тижневого тренду обрізався б під напівпрозорою
        // "таблеткою" на екранах, де вміст не влазить.
        // Перемикач День/Тиждень — sticky (за запитом користувача): стоїть ПОЗА прокручуваним
        // Column, тож лишається на місці, коли решта екрана гортається.
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
        PeriodSelector(
            selected = state.period,
            onSelect = viewModel::selectPeriod,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

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

            CategoryBreakdownCard(
                items = state.categoryBreakdown,
                onlineMinutes = if (state.hasUsageAccess && state.weeklyTrend.isNotEmpty()) state.weeklyTrend.sumOf { it.onlineMinutes } else null
            )

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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f))
    ) {
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
private fun PeriodSelector(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        StatsPeriod.DAY to stringResource(R.string.stats_period_day),
        StatsPeriod.WEEK to stringResource(R.string.stats_period_week)
    )
    PillSegmentedControl(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier.fillMaxWidth()
    )
}

private data class BarRow(val label: String, val minutes: Int, val color: Color)

/**
 * FR-5.2: розподіл офлайн-часу по категоріях за обраний період — горизонтальні смуги (за
 * запитом користувача, замість стовпчиків Vico). Колір смуги — колір категорії з наявної гами
 * (`categoryColor(colorHex)`), довжина — частка від найбільшої категорії; зверху смуги назва
 * зліва й час справа. Контейнер — як картка тренду (білий 80%, радіус 16, padding 12, gap 8).
 */
@Composable
private fun CategoryBreakdownCard(items: List<CategoryBreakdownItem>, onlineMinutes: Int?) {
    // "Online" — окремий рядок серед категорій (за запитом користувача): Online-хвилини за обраний
    // період (null — нема доступу до статистики), колір — той самий, що на Home (`onlineCard`).
    val onlineLabel = stringResource(R.string.balance_online_label)
    val rows = buildList {
        onlineMinutes?.takeIf { it > 0 }?.let { add(BarRow(onlineLabel, it, TeperaPalette.onlineCard)) }
        items.forEach { add(BarRow(categoryDisplayName(it.category), it.minutes, categoryColor(it.category.colorHex))) }
    }
    val visible = rows.filter { it.minutes > 0 }.sortedByDescending { it.minutes }
    val maxMinutes = visible.maxOfOrNull { it.minutes } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_category_breakdown_title),
            color = TeperaPalette.buttonBrandDark,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 20.8.sp,
            letterSpacing = 0.016.sp
        )

        if (visible.isEmpty()) {
            Text(stringResource(R.string.stats_no_data), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visible.forEach { item ->
                    val (hours, remainderMinutes) = roundToQuarterHour(item.minutes)
                    val durationText = when {
                        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
                        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
                        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.label,
                                modifier = Modifier.weight(1f),
                                color = TeperaPalette.buttonBrandDark,
                                fontFamily = TeperaPalette.headlineFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 15.6.sp,
                                letterSpacing = 0.012.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = durationText,
                                color = TeperaPalette.buttonBrandDark,
                                fontFamily = TeperaPalette.headlineFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 15.6.sp,
                                letterSpacing = 0.012.sp,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(TeperaPalette.buttonBrandDark.copy(alpha = 0.06f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(item.minutes.toFloat() / maxMinutes)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(item.color)
                            )
                        }
                    }
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
    // Figma "App concept" node 210:1880: білий 80%, радіус 16, padding 12, gap 8; заголовок —
    // Golos Text Regular 16sp, line-height 1.3, letter-spacing 0.016, #003926.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_weekly_trend_title),
            color = TeperaPalette.buttonBrandDark,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 20.8.sp,
            letterSpacing = 0.016.sp
        )

        if (!hasUsageAccess) {
            Text(stringResource(R.string.usage_access_prompt_title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.usage_access_prompt_body), style = MaterialTheme.typography.bodyMedium)
            TeperaButton(
                text = stringResource(R.string.usage_access_open_settings),
                onClick = onOpenUsageAccessSettings,
                type = TeperaButtonType.Primary
            )
        } else if (points.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.stats_no_data))
            }
        } else {
            val dayFormat = remember(period) {
                SimpleDateFormat("EEE", Locale.getDefault())
            }
            OnlineTrendChart(
                minutesPerDay = points.map { it.onlineMinutes },
                dayLabels = points.map { dayFormat.format(Date(it.dayStartMillis)) },
                hoursFormat = hoursFormat
            )
        }
    }
}

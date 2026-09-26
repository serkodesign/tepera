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
import androidx.compose.foundation.layout.IntrinsicSize
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
import com.serkodesign.tepera.ui.theme.StatTile
import com.serkodesign.tepera.ui.theme.TeperaCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
        factory = StatsViewModel.Factory(categoryRepository, activityRepository, balanceRepository, sleepWindowRepository, unlockRepository, pauseRepository, settingsStore,
            (LocalContext.current.applicationContext as com.serkodesign.tepera.TeperaApp).activeTimerStore.subMinuteStore
        )
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // За прямим запитом користувача екран щоразу відкривається на "Вчора", а не на останньому
    // обраному періоді (ViewModel переживає перемикання вкладок навбару, тож без цього лишався б "Тиждень").
    LaunchedEffect(Unit) {
        if (state.period != StatsPeriod.DAY) viewModel.selectPeriod(StatsPeriod.DAY)
    }

    // FR-D.8/D.9: власний інстанс (окремий від Home, кожен зі своїм refresh-циклом). За прямим
    // запитом користувача тепер РЕАГУЄ на PeriodSelector — initialPeriodDays узгоджений із
    // дефолтним period == DAY у StatsUiState(), LaunchedEffect(state.period) нижче тримає їх
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
            // не "0". За прямим запитом користувача — дві картки в ряд (`StatTile`), не чипи.
            if (state.period == StatsPeriod.WEEK) {
                if (state.unlockStats.weekCount != null || state.lastPhoneUseStats.weekMedianMillis != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.unlockStats.weekCount?.let { count ->
                            StatTile(
                                label = stringResource(R.string.stats_unlock_week_label),
                                value = count.toString(),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        // T-10: та сама медіана, що другий (тихий) рядок LastPhoneUseEstimateCard —
                        // тут окремою карткою серед інших фактів Stats, не другорядна деталь.
                        state.lastPhoneUseStats.weekMedianMillis?.let { millis ->
                            StatTile(
                                label = stringResource(R.string.stats_last_phone_use_week_label),
                                value = formatClockTime(millis),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
                // За прямим запитом користувача: тепловий патерн одразу під картками вище.
                PatternCard(state = patternState, period = state.period)
            }

            // "День" = вчора: замість тренду з однією точкою — деталі доби (межі, хронологія, паузи,
            // порівняння зі своєю типовою добою). Тепловий патерн для Дня рендериться всередині
            // DayDetailsSection, одразу під картками меж дня (той самий принцип, що Тиждень вище).
            if (state.period == StatsPeriod.DAY) {
                DayDetailsSection(
                    details = state.dayDetails,
                    hasUsageAccess = state.hasUsageAccess,
                    onOpenUsageAccessSettings = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    patternState = patternState
                )
            }

            CategoryBreakdownCard(
                items = state.categoryBreakdown,
                onlineMinutes = if (state.hasUsageAccess && state.weeklyTrend.isNotEmpty()) state.weeklyTrend.sumOf { it.onlineMinutes } else null,
                offlineMinutes = if (state.hasUsageAccess && state.weeklyTrend.isNotEmpty()) state.weeklyTrend.sumOf { it.offlineMinutes } else null
            )

            if (state.period == StatsPeriod.WEEK) {
                WeeklyTrendCard(
                    points = state.weeklyTrend,
                    period = state.period,
                    hasUsageAccess = state.hasUsageAccess,
                    onOpenUsageAccessSettings = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
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
internal fun PatternCard(state: PatternUiState, period: StatsPeriod) {
    if (!state.visible) return

    TeperaCard(
        title = stringResource(if (period == StatsPeriod.DAY) R.string.pattern_card_title else R.string.pattern_card_title_average),
        // Для "Вчора" підпис дублював би заголовок ("Патерн екрану вчора") — лише для тижня.
        subtitle = if (period == StatsPeriod.DAY) null else stringResource(periodLabelRes(period))
    ) {
        if (!state.hasEnoughData) {
            Text(stringResource(R.string.pattern_empty_state), style = MaterialTheme.typography.bodyMedium)
        }
        HourlyHeatGrid(hourlyMinutes = if (state.hasEnoughData) state.hourlyMinutes else null)
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

/** [belowThreshold] — сума категорії менша за [MIN_SHOWN_CATEGORY_SECONDS]: замість хвилин підпис "<5 хв". */
private data class BarRow(val label: String, val minutes: Int, val color: Color, val belowThreshold: Boolean = false)

/**
 * FR-5.2: розподіл офлайн-часу по категоріях за обраний період — горизонтальні смуги (за
 * запитом користувача, замість стовпчиків Vico). Колір смуги — колір категорії з наявної гами
 * (`categoryColor(colorHex)`), довжина — частка від найбільшої категорії; зверху смуги назва
 * зліва й час справа. Контейнер — як картка тренду (білий 80%, радіус 16, padding 12, gap 8).
 */
@Composable
private fun CategoryBreakdownCard(
    items: List<CategoryBreakdownItem>,
    onlineMinutes: Int?,
    offlineMinutes: Int?
) {
    // "Online" — окремий рядок серед категорій (за запитом користувача): Online-хвилини за обраний
    // період (null — нема доступу до статистики), колір — той самий, що на Home (`onlineCard`).
    val onlineLabel = stringResource(R.string.balance_online_label)
    val rows = buildList {
        onlineMinutes?.takeIf { it > 0 }?.let { add(BarRow(onlineLabel, it, TeperaPalette.onlineCard)) }
        items.forEach {
            // Сума категорії (цілі хвилини записів + накопичені секунди коротких таймерів) менша за 5 хв —
            // не показуємо точне число, а "<5 хв"; нуль не показуємо взагалі.
            val total = it.totalSeconds
            if (total > 0) {
                add(
                    BarRow(
                        categoryDisplayName(it.category), total / 60, categoryColor(it.category.colorHex),
                        belowThreshold = total < MIN_SHOWN_CATEGORY_SECONDS
                    )
                )
            }
        }
        // "Офлайн" — залишок періоду (не Online і не відмічене), як "Офлайн-життя" на Home; ніколи
        // не від'ємний. Без доступу до статистики (offlineMinutes == null) рядка нема.
        // (за об'єднанням Online й записів, без ночі — див. offlineUnloggedMinutes).
        if (offlineMinutes != null && offlineMinutes > 0) {
            add(BarRow(stringResource(R.string.stats_offline_label), offlineMinutes, TeperaPalette.restOfDayCard))
        }
    }
    val visible = rows.filter { it.minutes > 0 || it.belowThreshold }.sortedByDescending { it.minutes }
    val maxMinutes = visible.maxOfOrNull { it.minutes } ?: 0

    TeperaCard(title = stringResource(R.string.stats_category_breakdown_title)) {
        if (visible.isEmpty()) {
            Text(stringResource(R.string.stats_no_data), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visible.forEach { item ->
                    // Менше 15 хв — точні хвилини (заокруглення до чверті давало б "0 хв" для 5-7 хв).
                    val (hours, remainderMinutes) =
                        if (item.minutes < 15) 0 to item.minutes else roundToQuarterHour(item.minutes)
                    val durationText = when {
                        item.belowThreshold -> stringResource(R.string.stats_below_five_min)
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
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.25.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = durationText,
                                color = TeperaPalette.buttonBrandDark,
                                fontFamily = TeperaPalette.headlineFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.25.sp,
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
                                    .fillMaxWidth((item.minutes.toFloat() / maxMinutes).coerceAtLeast(0.03f)) // "<5 хв" — тонка смужка, не порожнеча
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
    TeperaCard(title = stringResource(R.string.stats_weekly_trend_title)) {
        if (!hasUsageAccess) {
            Text(stringResource(R.string.usage_access_prompt_title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.usage_access_prompt_body), style = MaterialTheme.typography.bodyMedium)
            TeperaButton(
                text = stringResource(R.string.usage_access_open_settings),
                onClick = onOpenUsageAccessSettings,
                type = TeperaButtonType.Secondary
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

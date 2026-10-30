package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.home.BalanceViewModel
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.bottomNavClearance
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.domain.risk.NextWindow
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.domain.risk.RiskHero
import com.serkodesign.tepera.domain.risk.RiskHeroPicker
import com.serkodesign.tepera.ui.home.PatternMiniCard
import com.serkodesign.tepera.ui.home.PauseCard
import com.serkodesign.tepera.ui.home.PauseViewModel
import com.serkodesign.tepera.ui.pattern.PatternViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.Date

/**
 * v3.0 MVP, Етап 3: головний екран режиму v3 — L3 «Тиждень спостереження» (Figma 911:415), поки немає тижня даних, і
 * L4 «Знайдено вікна» (911:739), коли вікна є. Без вікон після тижня — спокійний порожній стан. Плану «якщо — то»
 * нема: підказки, що зробити замість стрічки, приходять самі (оверлей поверх стрічки й нагадування перед годиною).
 */
@Composable
fun RiskHomeScreen(
    onOpenSettings: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenFeedPicker: () -> Unit,
    onOpenAccess: () -> Unit,
    onOpenWhy: (String) -> Unit,
    onAddWindow: () -> Unit,
    onAddEntry: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val vm: RiskHomeViewModel = viewModel(factory = RiskHomeViewModel.Factory(app))
    val state by vm.state.collectAsState()
    // Нове бачення головного екрана: герой за пріоритетом (година → паузи → наступна година → патерн), під ним чіпи
    // активностей і «Online сьогодні»; решта (вікна, підсумок тижня) — деталі нижче.
    val balanceViewModel: BalanceViewModel = viewModel(
        factory = BalanceViewModel.Factory(
            app.balanceRepository, app.activityRepository, app.categoryRepository, app.settingsStore, app.sleepWindowRepository
        )
    )
    val balanceState by balanceViewModel.uiState.collectAsState()
    val pauseViewModel: PauseViewModel = viewModel(
        factory = PauseViewModel.Factory(app.pauseRepository, app.balanceRepository, app.activityRepository, app.settingsStore, app.sleepWindowRepository)
    )
    val pauseState by pauseViewModel.uiState.collectAsState()
    val patternViewModel: PatternViewModel = viewModel(
        // Вчорашня доба (1 день), як у шторці підсумків звичайного Home.
        factory = PatternViewModel.Factory(
            app.patternRepository, app.balanceRepository, app.settingsStore, initialPeriodDays = 1,
            sleepWindowRepository = app.sleepWindowRepository
        )
    )
    val patternState by patternViewModel.uiState.collectAsState()
    val categories by app.categoryRepository.observeActiveCategories().collectAsState(initial = emptyList())

    LifecycleResumeEffect(Unit) {
        vm.refresh()
        balanceViewModel.refresh()
        pauseViewModel.refresh()
        patternViewModel.refresh()
        onPauseOrDispose {}
    }

    val observing = state.hasAccess && state.feedCount > 0 && state.windows.none { !it.isManual } && !state.enoughData
    val planned = state.hasAccess && state.feedCount > 0 && state.windows.isNotEmpty()
    val locale = LocalConfiguration.current.locales[0]
    val nowMillis by rememberNowMillis()
    val now = remember(nowMillis) { LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault()) }
    val occurrences = remember(state.windows, now.withSecond(0).withNano(0)) {
        // Вікна на паузі не рахуємо: «Зараз твоя година» для вікна без нагадувань і підказок вводило б в оману.
        NextWindow.occurrences(state.windows.filter { !it.isPaused }.map { NextWindow.Spec(it.id, it.startMinute, it.endMinute, it.dayType) }, now)
    }
    val dateText = remember(locale) { SimpleDateFormat("d MMMM", locale).format(Date()) }
    val label = when {
        observing -> stringResource(R.string.v3_home_label_observing)
        planned -> stringResource(R.string.v3_home_today_weekday, now.dayOfWeek.getDisplayName(TextStyle.FULL, locale).lowercase(locale))
        else -> stringResource(R.string.v3_home_label_today)
    }
    // Заголовок — день/дата; відлік до наступної години живе в `NextCard`, а не дублюється тут.
    val title = when {
        observing -> stringResource(R.string.v3_home_day_format, state.observedDays.coerceIn(1, 7))
        else -> dateText
    }
    val ready = !state.loading && state.hasAccess && state.feedCount > 0
    val insideOcc = occurrences.firstOrNull { it.isInside(now) }
    val nextOcc = occurrences.firstOrNull { !it.isInside(now) }
    val hero = RiskHeroPicker.pick(
        insideWindow = insideOcc != null, hasPauses = pauseState.visible, hasNextWindow = nextOcc != null,
        hasPattern = patternState.hasEnoughData
    )

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RiskHeader(label, title, onOpenCalendar, onOpenSettings)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    state.loading -> HomeSkeleton()
                    !state.hasAccess -> NoticeCard(
                        R.string.v3_home_need_access_title, R.string.v3_home_need_access_body, R.string.v3_access_grant, onOpenAccess
                    )
                    state.feedCount == 0 -> NoticeCard(
                        R.string.v3_home_need_feeds_title, R.string.v3_home_need_feeds_body, R.string.v3_feed_picker_action, onOpenFeedPicker
                    )
                }
            }
            // Герой: одна головна картка за пріоритетом. Паузи мають власний відступ, решта — загальні 16dp.
            if (ready) {
                val byId = state.windows.associateBy { it.id }
                when (hero) {
                    RiskHero.IN_WINDOW -> byId[insideOcc?.id]?.let { w ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            // Тиждень спостереження мовчить: без ідей, лише відлік.
                            if (observing) NextCard(w, insideOcc!!, now, onClick = { onOpenWhy(w.id) })
                            else IdeaHero(w.id, insideOcc!!, now, categories)
                        }
                    }
                    RiskHero.PAUSES -> PauseCard(
                        state = pauseState, categories = categories, onLabel = pauseViewModel::labelGap,
                        onDismissGap = pauseViewModel::dismissGap, onDismissCard = pauseViewModel::dismissCard
                    )
                    RiskHero.NEXT_WINDOW -> byId[nextOcc?.id]?.let { w ->
                        Box(Modifier.padding(horizontal = 16.dp)) { NextCard(w, nextOcc!!, now, onClick = { onOpenWhy(w.id) }) }
                    }
                    RiskHero.PATTERN -> Box(Modifier.padding(horizontal = 16.dp)) { PatternMiniCard(patternState) }
                    RiskHero.NONE -> Unit
                }
            }
            if (!state.loading) ActivityChips(onAddEntry)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OnlineTodayCard(balanceState)
                if (ready) {
                    // Деталі: вікна й тиждень. NextCard вже є героєм, якщо вікно ближче за все; інакше він тут не потрібен.
                    when {
                        observing -> {
                            ObservingContent(state)
                            // Ручні вікна видно одразу, не чекаючи тиждень даних.
                            WindowsHeader(onAddWindow)
                            WindowsCarousel(state.windows + state.disabledWindows, onOpenWhy)
                        }
                        planned -> PlannedContent(state, occurrences, now, onOpenWhy, onAddWindow, showNext = hero != RiskHero.IN_WINDOW && hero != RiskHero.NEXT_WINDOW)
                        else -> FoundContent(state, onOpenWhy, onAddWindow)
                    }
                    if (!observing) {
                        WeeklySummaryBlock(state.windows, state.softExit, vm::applyShift, vm::dismissShift)
                    }
                }
            }
            // Запас під навбар-«таблетку», щоб останній рядок прокручувався над нею.
            Spacer(Modifier.height(bottomNavClearance()))
        }
    }
}

/** Заголовок розділу зі списком вікон і кнопкою «+» поруч — додавання власного вікна було знайти важко внизу екрана. */
@Composable
private fun WindowsHeader(onAddWindow: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.v3_windows_section), color = TeperaPalette.textPrimary,
            fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { heading() }
        )
        TeperaIconButton(
            icon = TeperaSymbols.Add,
            contentDescription = stringResource(R.string.v3_add_window),
            onClick = onAddWindow,
            shape = CircleShape,
            containerColor = TeperaPalette.headerButtonFill,
            contentColor = TeperaPalette.textPrimary
        )
    }
}

/** Статичний скелетон, поки вантажиться стан: екран не блимає порожнім (без анімації — стабільніше на слабких пристроях). */
@Composable
private fun HomeSkeleton() {
    repeat(2) {
        Box(
            Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(16.dp))
                .background(TeperaPalette.heatmapNoDataFill.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun RiskHeader(label: String, title: String, onOpenCalendar: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            Text(
                title, color = TeperaPalette.textPrimary, fontSize = 27.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics { heading() }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TeperaIconButton(
                icon = TeperaIcons.Calendar,
                contentDescription = stringResource(R.string.calendar_screen_title),
                onClick = onOpenCalendar,
                shape = CircleShape,
                containerColor = TeperaPalette.headerButtonFill,
                contentColor = TeperaPalette.textPrimary
            )
            TeperaIconButton(
                icon = TeperaIcons.Settings,
                contentDescription = stringResource(R.string.settings_nav_action),
                onClick = onOpenSettings,
                shape = CircleShape,
                containerColor = TeperaPalette.headerButtonFill,
                contentColor = TeperaPalette.textPrimary
            )
        }
    }
}

@Composable
private fun NoticeCard(titleRes: Int, bodyRes: Int, buttonRes: Int, onClick: () -> Unit) {
    TeperaCard(title = stringResource(titleRes)) {
        Text(stringResource(bodyRes), color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        TeperaButton(text = stringResource(buttonRes), onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}

/** L3: прогрес 7 днів, пояснення «нічого робити не треба», теплова карта й, якщо вже щось видно, підказка. */
@Composable
private fun ObservingContent(state: RiskHomeState) {
    V3Card(spacing = 12.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(7) { i ->
                Box(
                    Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (i < state.observedDays) TeperaPalette.brandAccent else TeperaPalette.heatmapNoDataFill)
                )
            }
        }
        Text(stringResource(R.string.v3_observing_hint), color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
    HeatWeek(state.dates, state.grid, emptyList(), stringResource(R.string.v3_heat_description))
    state.teaser?.let { t ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(TeperaPalette.guessFill)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(TeperaSymbols.AutoAwesome, contentDescription = null, tint = TeperaPalette.textSecondary, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.v3_teaser_format, formatWindowTime(t.startMinute, t.endMinute), t.daysLeft),
                color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
            )
        }
    }
}

/** L4: пояснення, теплова карта з пунктирними вікнами й картки вікон; без вікон — спокійне «і це теж добре». */
@Composable
private fun FoundContent(state: RiskHomeState, onOpenWhy: (String) -> Unit, onAddWindow: () -> Unit) {
    val windows = state.windows
    Text(
        text = if (windows.isEmpty() && state.disabledWindows.isNotEmpty()) {
            stringResource(R.string.v3_windows_all_off)
        } else if (windows.isEmpty()) {
            stringResource(R.string.v3_windows_empty)
        } else {
            stringResource(R.string.v3_windows_intro_format, windows.size)
        },
        color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    val desc = if (windows.isEmpty()) {
        stringResource(R.string.v3_heat_description)
    } else {
        stringResource(
            R.string.v3_heat_window_description,
            stringResource(R.string.v3_heat_description),
            windows.joinToString(", ") { formatWindowTime(it.startMinute, it.endMinute) }
        )
    }
    HeatWeek(state.dates, state.grid, windows.map { it.startMinute to it.endMinute }, desc)
    WindowsHeader(onAddWindow)
    WindowsCarousel(windows + state.disabledWindows, onOpenWhy)
}

/** Поточний час, що оновлюється раз на 30 с: відлік до наступної години на L6 не застигає. */
@Composable
private fun rememberNowMillis(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(30_000)
        value = System.currentTimeMillis()
    }
}

private fun clock(dt: LocalDateTime): String = String.format(Locale.ROOT, "%02d:%02d", dt.hour, dt.minute)

/** L6 (Figma 911:1192): «До твоєї години» і картки вікон з планами, найближче — першим. Без теплової карти (вона — в «Чому це вікно?»). */
@Composable
private fun PlannedContent(
    state: RiskHomeState,
    occurrences: List<NextWindow.Occurrence>,
    now: LocalDateTime,
    onOpenWhy: (String) -> Unit,
    onAddWindow: () -> Unit,
    showNext: Boolean
) {
    val byId = state.windows.associateBy { it.id }
    val ordered = occurrences.mapNotNull { o -> byId[o.id]?.let { it to o } }
    val rest = state.windows.filter { w -> ordered.none { it.first.id == w.id } }
    if (showNext) ordered.firstOrNull()?.let { (w, o) -> NextCard(w, o, now, onClick = { onOpenWhy(w.id) }) }
    WindowsHeader(onAddWindow)
    WindowsCarousel(ordered.map { it.first } + rest + state.disabledWindows, onOpenWhy)
}

/** `next`: темна картка з відліком до години або «до кінця» для години, що триває; значки й підписи — токени hero. */
@Composable
private fun NextCard(window: RiskWindowUi, occurrence: NextWindow.Occurrence, now: LocalDateTime, onClick: () -> Unit) {
    val inside = occurrence.isInside(now)
    val onDark = TeperaPalette.textOnDark
    val big = if (inside) {
        stringResource(R.string.v3_next_until_format, clock(occurrence.endAt))
    } else {
        durationText(Duration.between(now, occurrence.startAt).toMinutes().coerceAtLeast(1))
    }
    val sub = when {
        !inside -> stringResource(R.string.v3_next_sub_starts, clock(occurrence.startAt))
        else -> stringResource(R.string.v3_next_sub_inside_hint)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(TeperaPalette.heroSurface)
            .clickable(role = Role.Button, onClick = onClick).padding(20.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(if (inside) R.string.v3_next_label_inside else R.string.v3_next_label_upcoming),
                color = onDark.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp
            )
            Text(big, color = onDark, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = onDark.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
        }
        Icon(
            imageVector = if (window.partOfDay == PartOfDay.EVENING || window.partOfDay == PartOfDay.NIGHT) TeperaSymbols.Bedtime else TeperaSymbols.Schedule,
            contentDescription = null, tint = TeperaPalette.restOfDayCard, modifier = Modifier.size(32.dp)
        )
        Icon(TeperaSymbols.ChevronRight, contentDescription = null, tint = onDark.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun durationText(totalMinutes: Long): String {
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> stringResource(R.string.v3_dur_days_hours, days.toInt(), hours.toInt())
        hours > 0 && minutes > 0 -> stringResource(R.string.v3_dur_hours_minutes, hours.toInt(), minutes.toInt())
        hours > 0 -> stringResource(R.string.v3_dur_hours, hours.toInt())
        else -> stringResource(R.string.v3_dur_minutes, minutes.toInt())
    }
}

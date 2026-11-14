package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import com.serkodesign.tepera.ui.tutorial.TutorialOverlay
import com.serkodesign.tepera.ui.tutorial.TutorialStep
import com.serkodesign.tepera.ui.tutorial.TutorialTargets
import com.serkodesign.tepera.ui.tutorial.tutorialTarget
import com.serkodesign.tepera.util.TutorialStore
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
import com.serkodesign.tepera.ui.theme.heroMoodBackground
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
    val categories by app.categoryRepository.observeActiveCategories().collectAsState(initial = emptyList())

    LifecycleResumeEffect(Unit) {
        vm.refresh()
        balanceViewModel.refresh()
        pauseViewModel.refresh()
        onPauseOrDispose {}
    }

    val observing = state.hasAccess && state.feedCount > 0 && state.windows.none { !it.isManual } && !state.enoughData
    val locale = LocalConfiguration.current.locales[0]
    val nowMillis by rememberNowMillis()
    val now = remember(nowMillis) { LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault()) }
    val occurrences = remember(state.windows, now.withSecond(0).withNano(0)) {
        // Вікна на паузі не рахуємо: «Зараз твоя година» для вікна без нагадувань і підказок вводило б в оману.
        NextWindow.occurrences(state.windows.filter { !it.isPaused }.map { NextWindow.Spec(it.id, it.startMinute, it.endMinute, it.dayType) }, now)
    }
    // Шапка Головної v2: «Четвер, 8 жовтня» над «Сьогодні» (у тиждень спостереження — «День N з 7»).
    val dateText = remember(locale, now.toLocalDate()) {
        SimpleDateFormat("EEEE, d MMMM", locale).format(Date()).replaceFirstChar { it.titlecase(locale) }
    }
    val headerDate = if (observing) stringResource(R.string.v3_home_day_format, state.observedDays.coerceIn(1, 7)) else dateText
    val logicalToday = remember(now) { now.minusHours(com.serkodesign.tepera.domain.risk.RiskConfig().dayCutoffHour.toLong()).toLocalDate() }
    val ready = !state.loading && state.hasAccess && state.feedCount > 0
    val insideOcc = occurrences.firstOrNull { it.isInside(now) }
    val heroOcc = insideOcc ?: occurrences.firstOrNull()
    val byId = state.windows.associateBy { it.id }
    val heroWindow = heroOcc?.let { byId[it.id] }

    // Туторіал підказками (14.11.2026): ланцюжок один раз, коли Головна вже «жива» (є доступ і стрічки); затримка, щоб
    // не перекрити онбординг і діалоги, що відкриваються поверх Головної.
    val tutorialContext = LocalContext.current
    val targets = remember { TutorialTargets() }
    val scroll = rememberScrollState()
    var tutorialOn by remember { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (ready && !TutorialStore.isSeen(tutorialContext, TutorialStore.HOME_CHAIN)) {
            delay(1500)
            tutorialOn = true
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(scroll)
                .padding(top = 12.dp, bottom = 32.dp),
            // Порядок (рішення власника, 14.11.2026): ідея → години скролінгу (головна функція, тож одразу під ідеєю) →
            // Online сьогодні → сьогоднішні години → тиждень у години детоксу. Активності — у Щоденнику.
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeV2Header(headerDate, onOpenSettings, settingsModifier = Modifier.tutorialTarget(targets, "settings"))
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        state.loading -> HomeSkeleton()
                        !state.hasAccess -> NoticeCard(
                            R.string.v3_home_need_access_title, R.string.v3_home_need_access_body, R.string.v3_access_grant, onOpenAccess
                        )
                        state.feedCount == 0 -> NoticeCard(
                            R.string.v3_home_need_feeds_title, R.string.v3_home_need_feeds_body, R.string.v3_feed_picker_action, onOpenFeedPicker
                        )
                        observing -> ObservingContent(state, onAddWindow, Modifier.tutorialTarget(targets, "observing"))
                        else -> {
                            // 1. Картка ідеї: наступна година (або та, що триває; коли сьогодні все — перша завтра).
                            if (heroWindow != null && heroOcc != null) {
                                Box(Modifier.tutorialTarget(targets, "idea")) {
                                    NextHourCard(heroWindow, heroOcc, now, logicalToday, onChanged = vm::refresh)
                                }
                            }
                        }
                    }
                    // Паузи — лише коли є що назвати, одразу під карткою ідеї.
                    if (ready && pauseState.visible) {
                        PauseCard(
                            state = pauseState, categories = categories, onLabel = pauseViewModel::labelGap,
                            onDismissGap = pauseViewModel::dismissGap, onDismissCard = pauseViewModel::dismissCard
                        )
                    }
                }
            }
            if (ready) {
                // 2. Години скролінгу каруселлю з «+»: ручні вікна видно й у тиждень спостереження.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        WindowsHeader(onAddWindow, addModifier = Modifier.tutorialTarget(targets, "add"))
                    }
                    if (state.windows.isEmpty() && !observing) {
                        Text(
                            stringResource(if (state.disabledWindows.isNotEmpty()) R.string.v3_windows_all_off else R.string.v3_windows_empty),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                    val ordered = occurrences.mapNotNull { byId[it.id] }.distinctBy { it.id }
                    Box(Modifier.tutorialTarget(targets, "hours")) {
                        WindowsCarousel(ordered + state.windows.filter { w -> ordered.none { it.id == w.id } } + state.disabledWindows, onOpenWhy)
                    }
                }
            }
            if (ready && observing) Box(Modifier.padding(horizontal = 16.dp)) { ObservingHeat(state) }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 3. Online сьогодні.
                if (!state.loading) OnlineTodayCard(balanceState)
                // 4. Сьогоднішні години рядками: «Сьогодні вже було» / «Далі сьогодні».
                if (ready && !observing) {
                    TodayRows(state.windows, now, logicalToday, heroWindowId = heroOcc?.id, onOpenWhy = onOpenWhy)
                }
            }
            if (ready && !observing) {
                // 5. Тиждень у години детоксу; під ним пропозиція зсунути вікно — лише коли вона є.
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeekCard(
                        state.windows, state.daysSinceObservation,
                        onOpen = state.windows.firstOrNull()?.let { w -> { onOpenWhy(w.id) } }
                    )
                    ShiftSuggestions(state.windows, vm::applyShift, vm::dismissShift)
                }
            }
            // Запас під навбар-«таблетку», щоб останній рядок прокручувався над нею.
            Spacer(Modifier.height(bottomNavClearance()))
        }
    }
    if (tutorialOn) {
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        TutorialOverlay(
            steps = HOME_TUTORIAL,
            targets = targets,
            scrollTo = { target, viewport ->
                // Ціль має бути у видимій зоні й не під карткою підказки внизу.
                val margin = 120f
                if (target.top < viewport.top + margin || target.bottom > viewport.bottom - 360f) {
                    val wanted = scroll.value + (target.top - viewport.top - margin)
                    scroll.animateScrollTo(wanted.toInt().coerceIn(0, scroll.maxValue))
                }
            },
            onFinish = {
                tutorialOn = false
                TutorialStore.markSeen(tutorialContext, TutorialStore.HOME_CHAIN)
            }
        )
    }
    }
}

private val HOME_TUTORIAL = listOf(
    TutorialStep("observing", R.string.tutorial_observing_title, R.string.tutorial_observing_body),
    TutorialStep("idea", R.string.tutorial_idea_title, R.string.tutorial_idea_body),
    TutorialStep("hours", R.string.tutorial_hours_title, R.string.tutorial_hours_body),
    TutorialStep("add", R.string.tutorial_add_title, R.string.tutorial_add_body),
    TutorialStep(null, R.string.tutorial_diary_title, R.string.tutorial_diary_body),
    TutorialStep("settings", R.string.tutorial_settings_title, R.string.tutorial_settings_body)
)

/** Заголовок розділу зі списком вікон і кнопкою «+» поруч — додавання власного вікна було знайти важко внизу екрана. */
@Composable
private fun WindowsHeader(onAddWindow: () -> Unit, addModifier: Modifier = Modifier) {
    // Новий стиль (Головна v2): підпис групи — Title Small кольору primary, «+» на склі, як кнопка налаштувань у шапці.
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.v3_windows_section), style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )
        TeperaIconButton(
            icon = TeperaSymbols.Add,
            contentDescription = stringResource(R.string.v3_add_window),
            onClick = onAddWindow,
            modifier = androidx.compose.ui.Modifier.width(44.dp).then(addModifier),
            shape = CircleShape,
            containerColor = com.serkodesign.tepera.ui.theme.M3Extra.colors.glass,
            contentColor = MaterialTheme.colorScheme.onSurface
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
private fun NoticeCard(titleRes: Int, bodyRes: Int, buttonRes: Int, onClick: () -> Unit) {
    TeperaCard(title = stringResource(titleRes)) {
        Text(stringResource(bodyRes), color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        TeperaButton(text = stringResource(buttonRes), onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}

/** L3: прогрес 7 днів, пояснення «нічого робити не треба», теплова карта й, якщо вже щось видно, підказка. */
@Composable
private fun ObservingContent(state: RiskHomeState, onAddWindow: () -> Unit, cardModifier: Modifier = Modifier) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    V3Card(modifier = cardModifier, spacing = 12.dp) {
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
    V3Card(spacing = 12.dp) {
        // Поки збираються дані, годину можна обрати самому — стрічки працюватимуть у неї вже зараз.
        Text(stringResource(R.string.v3_observing_manual_hint), color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        TeperaButton(
            text = stringResource(R.string.v3_observing_manual_action), onClick = onAddWindow,
            type = TeperaButtonType.Secondary, modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Теплова карта тижня спостереження й підказка про першу знайдену годину: нижче за години скролінгу. */
@Composable
private fun ObservingHeat(state: RiskHomeState) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

/** Поточний час, що оновлюється раз на 30 с: відлік до наступної години на L6 не застигає. */
@Composable
private fun rememberNowMillis(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(30_000)
        value = System.currentTimeMillis()
    }
}


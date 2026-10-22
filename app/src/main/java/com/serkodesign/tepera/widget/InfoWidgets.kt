package com.serkodesign.tepera.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.home.HeroActivity
import com.serkodesign.tepera.ui.home.buildDayStructure
import com.serkodesign.tepera.util.offlineUnloggedMinutes
import com.serkodesign.tepera.util.roundToQuarterHour
import com.serkodesign.tepera.util.startOfLogicalDayMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Інформаційні віджети за Figma "App concept" → "Widgets v2 — Online і нові ідеї": Online 2x1 (кільце доби) і 4x1
 * (число + структура доби), активна зараз 2x1, початок дня 2x1, орб "без телефону" 1x1, остання активність 4x1,
 * найбільше часу за тиждень 4x1. Спільний вигляд — напівпрозора капсула 72dp (світла/темна за темою системи на момент
 * оновлення), текст і малюнки в кольорах теми. Усе — в основному source set, тож обидві збірки (standard і beta) мають
 * ці віджети однаково.
 *
 * Свідомо БЕЗ метрик-оцінок (лічильник розблокувань, дні підряд): за tepera-dev-spec.md розділ 2.2 вони не
 * показуються пасивно. Online й "без телефону" — ті самі числа, що вже постійно видно на Home.
 */

private val CAPSULE_HEIGHT = 72.dp
private val ONLINE_COLOR = Color(0xFFF5C401) // той самий #F5C401, що TeperaPalette.onlineCard

/** Кольори теми віджета: капсула/плашка — drawable за темою, решта — значення макета (light/dark). */
internal class InfoColors(
    val capsule: Int,
    val chip: Int,
    val text: Color,
    val sub: Color,
    val track: Color,
    val btn: Color,
    val btnIcon: Color,
    val mint: Color,
    val mintIcon: Color,
    val rest: Color
)

/**
 * Тема береться з конфігурації на момент (пере)композиції: Glance на API < 31 запікає кольори в RemoteViews при
 * оновленні, тож фон і текст завжди узгоджені між собою (стара тема можлива лише до наступного оновлення).
 */
internal fun infoColors(context: Context): InfoColors {
    val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    return if (night) {
        InfoColors(
            capsule = R.drawable.widget_info_capsule_dark, chip = R.drawable.widget_info_chip_dark,
            text = Color(0xFFFEFFEF), sub = Color(0xFFA9C7BD), track = Color(0x24FFFFFF),
            btn = Color(0xFFFEFFEF), btnIcon = Color(0xFF062924),
            mint = Color(0xFF1D4A41), mintIcon = Color(0xFF9FE3C6), rest = Color(0xFF3A5A52)
        )
    } else {
        InfoColors(
            capsule = R.drawable.widget_info_capsule_light, chip = R.drawable.widget_info_chip_light,
            text = Color(0xFF003926), sub = Color(0xFF4D6B60), track = Color(0x1F003926),
            btn = Color(0xFF006944), btnIcon = Color.White,
            mint = Color(0xFFDCF6ED), mintIcon = Color(0xFF006944), rest = Color(0xFFC5E2CB)
        )
    }
}

/** Усе, що читають інформаційні віджети; [facts] — лише в тих, що показують Online ([InfoWidget.needsFacts]). */
internal class InfoState(
    val facts: OnlineFacts?,
    val entries: List<ActivityEntryEntity>,
    val weekEntries: List<ActivityEntryEntity>,
    val categories: List<CategoryEntity>,
    val timers: Map<String, Long>,
    val selectedIds: List<String>
)

abstract class InfoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    /** Чи потрібен Online від точки старту дня (важке сканування UsageEvents, кешується). */
    protected open val needsFacts: Boolean = false

    /** Чи потрібні записи за останні 7 днів. */
    protected open val needsWeek: Boolean = false

    @Composable
    internal abstract fun Content(context: Context, state: InfoState)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TeperaApp
        // Початкові значення завантажуються ДО першого кадру (причина — у TeperaWidget.provideGlance): без цього
        // кожен collectAsState стартував би з порожнього значення, і віджет мигав би порожнім станом.
        val midnight = startOfLogicalDayMillis()
        val initialFacts = if (needsFacts) InfoWidgetData.facts(app) else null
        val initialEntries = app.activityRepository.observeEntriesInRange(midnight, Long.MAX_VALUE).first()
        val initialWeek = if (needsWeek) app.activityRepository.observeEntriesInRange(addDaysMillis(midnight, -6), Long.MAX_VALUE).first() else emptyList()
        val initialCategories = app.categoryRepository.observeAllCategories().first()
        val initialTimers = app.activeTimerStore.activeTimers.first()
        val initialSelected = app.settingsStore.widgetCategoryIds.first()
        provideContent {
            val tick by WidgetLiveData.refreshTick.collectAsState()
            val dayMidnight = remember(tick) { startOfLogicalDayMillis() }
            val entriesFlow = remember(dayMidnight) { app.activityRepository.observeEntriesInRange(dayMidnight, Long.MAX_VALUE) }
            val entries by entriesFlow.collectAsState(initial = initialEntries)
            val weekFlow = remember(dayMidnight) {
                if (needsWeek) app.activityRepository.observeEntriesInRange(addDaysMillis(dayMidnight, -6), Long.MAX_VALUE) else flowOf(emptyList())
            }
            val week by weekFlow.collectAsState(initial = initialWeek)
            val categories by app.categoryRepository.observeAllCategories().collectAsState(initial = initialCategories)
            val timers by app.activeTimerStore.activeTimers.collectAsState(initial = initialTimers)
            val selected by app.settingsStore.widgetCategoryIds.collectAsState(initial = initialSelected)
            val facts by produceState(initialValue = initialFacts, tick) {
                if (needsFacts) value = InfoWidgetData.facts(app)
            }
            GlanceTheme {
                Content(context, InfoState(facts, entries, week, categories, timers, selected))
            }
        }
    }

    /** Прев'ю для меню віджетів (Android 15+): той самий [Content] на демо-даних. */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            GlanceTheme { Content(context, previewState()) }
        }
    }
}

private fun previewState(): InfoState {
    val now = System.currentTimeMillis()
    val categories = DefaultCategories.all
    val reading = categories.first { it.id == DefaultCategories.READING_ID }
    val nature = categories.first { it.nameKey == "nature" }
    val entries = listOf(
        ActivityEntryEntity(categoryId = nature.id, startTime = now - 4 * 3_600_000L, durationMinutes = 50),
        ActivityEntryEntity(categoryId = reading.id, startTime = now - 2 * 3_600_000L, durationMinutes = 25)
    )
    val facts = OnlineFacts(
        hasUsageAccess = true,
        dayStartMillis = now - 8 * 3_600_000L,
        windows = emptyList(),
        onlineMinutes = 130,
        intervals = emptyList(),
        dayLengthMinutes = 480,
        daySpanMinutes = 1020,
        targetMinutes = 180
    )
    return InfoState(facts, entries, entries, categories, mapOf(reading.id to now), emptyList())
}

// ---------- спільні елементи ----------

private fun Context.px(dp: Dp): Int = (dp.value * resources.displayMetrics.density).toInt()

private fun glanceColor(color: Color) = ColorProvider(day = color, night = color)

private fun formatDuration(context: Context, minutes: Int, roundQuarter: Boolean = false): String {
    // Округлення до чверті години не повинно перетворювати короткий, але ненульовий час на "0 хв".
    val (hours, rest) = if (roundQuarter) {
        roundToQuarterHour(minutes).let { if (it.first == 0 && it.second == 0) 0 to minutes else it }
    } else {
        (minutes / 60) to (minutes % 60)
    }
    return when {
        hours <= 0 -> context.getString(R.string.minutes_short_format, rest)
        rest == 0 -> context.getString(R.string.hours_short_format, hours)
        else -> context.getString(R.string.hours_minutes_short_format, hours, rest)
    }
}

private fun clockText(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

@Composable
private fun InfoText(
    text: String,
    sizeSp: Int,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    modifier: GlanceModifier = GlanceModifier
) {
    Text(
        text = text,
        maxLines = 1,
        modifier = modifier,
        style = TextStyle(fontSize = sizeSp.sp, fontWeight = weight, color = glanceColor(color))
    )
}

/** Капсула на всю ширину віджета, по центру по висоті; тап по ній відкриває застосунок. */
@Composable
private fun Capsule(context: Context, colors: InfoColors, horizontalPadding: Dp, content: @Composable RowScope.() -> Unit) {
    val height = minOf(CAPSULE_HEIGHT, LocalSize.current.height)
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(height)
                .background(ImageProvider(colors.capsule))
                .padding(horizontal = horizontalPadding, vertical = 6.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun Dot(color: Color, size: Dp = 8.dp) {
    Image(
        provider = ImageProvider(R.drawable.widget_circle_solid),
        contentDescription = null,
        colorFilter = ColorFilter.tint(glanceColor(color)),
        modifier = GlanceModifier.size(size)
    )
}

/** Кругла кнопка play/stop для категорії — той самий тап-таймер, що на Home і в кнопках віджета. */
@Composable
private fun TimerButton(context: Context, colors: InfoColors, categoryId: String, running: Boolean, size: Dp) {
    Box(
        modifier = GlanceModifier.size(size).clickable(
            actionRunCallback<ToggleCategoryTimerAction>(actionParametersOf(CATEGORY_ID_KEY to categoryId))
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.widget_circle_solid),
            contentDescription = null,
            colorFilter = ColorFilter.tint(glanceColor(colors.btn)),
            modifier = GlanceModifier.size(size)
        )
        Image(
            provider = ImageProvider(if (running) R.drawable.ic_widget_info_stop else R.drawable.ic_widget_info_play),
            contentDescription = context.getString(if (running) R.string.category_stop_action else R.string.category_start_action),
            colorFilter = ColorFilter.tint(glanceColor(colors.btnIcon)),
            modifier = GlanceModifier.size(18.dp)
        )
    }
}

@Composable
private fun NoAccess(context: Context, colors: InfoColors) {
    InfoText(context.getString(R.string.widget_info_no_access), 14, colors.text, FontWeight.Bold)
}

/** Кнопки активності: обрані в налаштуваннях віджета (або автоматичний порядок), без архівованих. */
private fun widgetCategories(state: InfoState): List<CategoryEntity> =
    categoriesForWidget(state.categories.filter { !it.isHidden }, state.selectedIds)

// ---------- Online 2x1 · кільце доби ----------

class InfoOnlineRing2x1 : InfoWidget() {
    override val needsFacts = true

    @Composable
    override fun Content(context: Context, state: InfoState) {
        val colors = infoColors(context)
        val facts = state.facts
        Capsule(context, colors, 12.dp) {
            if (facts == null || !facts.hasUsageAccess) {
                NoAccess(context, colors)
            } else {
                val fraction = if (facts.elapsedMinutes > 0) (facts.onlineMinutes.toFloat() / facts.elapsedMinutes).coerceIn(0f, 1f) else 0f
                val ring = renderRing(context.px(48.dp), fraction, colors.track.toArgb(), ONLINE_COLOR.toArgb())
                Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(48.dp))
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column {
                    InfoText(context.getString(R.string.balance_online_label), 12, colors.sub, FontWeight.Medium)
                    InfoText(
                        context.getString(R.string.widget_info_hours_value_format, facts.onlineMinutes / 60, facts.onlineMinutes % 60),
                        17, colors.text, FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------- Online 4x1 · число + структура доби ----------

class InfoOnlineBar4x1 : InfoWidget() {
    override val needsFacts = true

    @Composable
    override fun Content(context: Context, state: InfoState) {
        val colors = infoColors(context)
        val facts = state.facts
        Capsule(context, colors, 20.dp) {
            if (facts == null || !facts.hasUsageAccess) {
                NoAccess(context, colors)
            } else {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(ONLINE_COLOR)
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        InfoText(context.getString(R.string.balance_online_label), 13, colors.sub, FontWeight.Medium)
                    }
                    InfoText(formatDuration(context, facts.onlineMinutes), 22, colors.text, FontWeight.Bold)
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                val barWidth = minOf(150.dp, LocalSize.current.width * 0.42f)
                val minutesByCategory = state.entries.groupBy { it.categoryId }.mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
                val categories = state.categories.filter { (minutesByCategory[it.id] ?: 0) > 0 }.sortedBy { it.sortOrder }
                    .map { HeroActivity(categoryDisplayName(it, context), categoryColor(it.colorHex), minutesByCategory.getValue(it.id)) }
                val rest = offlineUnloggedMinutes(facts.dayStartMillis, System.currentTimeMillis(), facts.intervals, state.entries, facts.windows)
                val structure = buildDayStructure(
                    onlineMinutes = facts.onlineMinutes,
                    categories = categories,
                    restOfDayMinutes = rest,
                    dayLengthMinutes = facts.dayLengthMinutes,
                    daySpanMinutes = facts.daySpanMinutes,
                    targetMinutes = facts.targetMinutes,
                    onlineColor = ONLINE_COLOR,
                    restColor = colors.rest,
                    futureColor = colors.track
                )
                val bar = renderStructureBar(
                    widthPx = context.px(barWidth),
                    heightPx = context.px(14.dp),
                    structure = structure,
                    trackArgb = colors.track.toArgb(),
                    tickArgb = colors.text.copy(alpha = 0.7f).toArgb(),
                    gapPx = context.resources.displayMetrics.density
                )
                Image(ImageProvider(bar), contentDescription = null, modifier = GlanceModifier.size(barWidth, 14.dp))
            }
        }
    }
}

// ---------- Активна зараз 2x1 ----------

class InfoActive2x1 : InfoWidget() {
    @Composable
    override fun Content(context: Context, state: InfoState) {
        val colors = infoColors(context)
        val byId = state.categories.associateBy { it.id }
        val runningId = state.timers.keys.firstOrNull { byId.containsKey(it) }
        // Нічого не триває — пропонуємо першу з кнопок віджета, щоб запустити одним тапом.
        val category = runningId?.let { byId[it] } ?: widgetCategories(state).firstOrNull()
        Capsule(context, colors, 14.dp) {
            if (category == null) {
                InfoText("—", 22, colors.text, FontWeight.Bold)
            } else {
                val running = runningId != null
                Column(modifier = GlanceModifier.defaultWeight()) {
                    InfoText(categoryDisplayName(category, context), 15, colors.text, FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (running) {
                            Dot(categoryColor(category.colorHex))
                            Spacer(modifier = GlanceModifier.width(6.dp))
                        }
                        InfoText(
                            context.getString(if (running) R.string.widget_info_running else R.string.widget_info_idle),
                            12, colors.sub, FontWeight.Medium
                        )
                    }
                }
                TimerButton(context, colors, category.id, running, 40.dp)
            }
        }
    }
}

// ---------- Початок дня 2x1 ----------

class InfoStart2x1 : InfoWidget() {
    override val needsFacts = true

    @Composable
    override fun Content(context: Context, state: InfoState) {
        val colors = infoColors(context)
        val facts = state.facts
        Capsule(context, colors, 12.dp) {
            if (facts == null || !facts.hasUsageAccess) {
                NoAccess(context, colors)
            } else {
                Box(modifier = GlanceModifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_circle_solid),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(colors.mint)),
                        modifier = GlanceModifier.size(44.dp)
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_info_sun),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(colors.mintIcon)),
                        modifier = GlanceModifier.size(24.dp)
                    )
                }
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column {
                    InfoText(context.getString(R.string.widget_info_start_title), 12, colors.sub, FontWeight.Medium)
                    // День ще не почався (жодного суттєвого розблокування) — тире замість вигаданого часу.
                    InfoText(if (facts.dayStarted) clockText(facts.dayStartMillis) else "—", 22, colors.text, FontWeight.Bold)
                }
            }
        }
    }
}

// ---------- Орб "без телефону" 1x1 ----------

class InfoOrb1x1 : InfoWidget() {
    override val needsFacts = true

    @Composable
    override fun Content(context: Context, state: InfoState) {
        val facts = state.facts
        val size = LocalSize.current
        val diameter = minOf(CAPSULE_HEIGHT, size.width, size.height)
        val open = GlanceModifier.fillMaxSize().clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        Box(modifier = open, contentAlignment = Alignment.Center) {
            if (facts == null || !facts.hasUsageAccess) {
                val colors = infoColors(context)
                InfoText(context.getString(R.string.widget_info_no_access), 12, colors.text, FontWeight.Bold)
            } else {
                // Як hero на Home: "без телефону" = минуло − Online, рівень води — його частка від минулого.
                val elapsed = facts.elapsedMinutes
                val offline = (elapsed - facts.onlineMinutes).coerceAtLeast(0)
                val level = if (elapsed > 0) (offline.toFloat() / elapsed).coerceIn(0f, 1f) else 0f
                val showValue = elapsed >= 30
                val orb = renderOrb(context.px(diameter), if (showValue) level else 0f, 0xFF0F4A38.toInt(), 0xF26FD3A8.toInt())
                Box(modifier = GlanceModifier.size(diameter), contentAlignment = Alignment.Center) {
                    Image(ImageProvider(orb), contentDescription = null, modifier = GlanceModifier.size(diameter))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        InfoText(if (showValue) "%d:%02d".format(offline / 60, offline % 60) else "—", 19, Color.White, FontWeight.Bold)
                        InfoText(context.getString(R.string.widget_info_orb_short), 12, Color.White.copy(alpha = 0.92f), FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ---------- Остання активність 4x1 ----------

class InfoLast4x1 : InfoWidget() {
    @Composable
    override fun Content(context: Context, state: InfoState) {
        val colors = infoColors(context)
        val byId = state.categories.associateBy { it.id }
        val last = state.entries
            .filter { byId.containsKey(it.categoryId) }
            .maxByOrNull { it.startTime + it.durationMinutes * 60_000L }
        Capsule(context, colors, 14.dp) {
            val category = last?.let { byId[it.categoryId] }
            if (last == null || category == null) {
                InfoText(context.getString(R.string.home_no_entries_today), 16, colors.text, FontWeight.Bold)
            } else {
                Box(modifier = GlanceModifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_circle_solid),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(categoryColor(category.colorHex).copy(alpha = 0.25f))),
                        modifier = GlanceModifier.size(44.dp)
                    )
                    Image(
                        provider = ImageProvider(widgetIconRes(category.iconName, selected = false)),
                        contentDescription = null,
                        modifier = GlanceModifier.size(22.dp)
                    )
                }
                Spacer(modifier = GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    InfoText(categoryDisplayName(category, context), 16, colors.text, FontWeight.Bold)
                    InfoText(
                        context.getString(
                            R.string.widget_info_last_format,
                            formatDuration(context, last.durationMinutes),
                            clockText(last.startTime + last.durationMinutes * 60_000L)
                        ),
                        12, colors.sub, FontWeight.Medium
                    )
                }
                TimerButton(context, colors, category.id, state.timers.containsKey(category.id), 44.dp)
            }
        }
    }
}

// ---------- Найбільше часу за тиждень 4x1 ----------

class InfoWeek4x1 : InfoWidget() {
    override val needsWeek = true

    @Composable
    override fun Content(context: Context, state: InfoState) {
        val colors = infoColors(context)
        val byId = state.categories.associateBy { it.id }
        // Два лідери за сумарним часом; абсолютні числа без порівняння з попереднім тижнем чи "нормою" (FR-P.5).
        val top = state.weekEntries
            .filter { byId.containsKey(it.categoryId) }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
            .filterValues { it > 0 }
            .entries.sortedByDescending { it.value }
            .take(2)
        Capsule(context, colors, 20.dp) {
            if (top.isEmpty()) {
                InfoText(context.getString(R.string.home_no_entries_today), 16, colors.text, FontWeight.Bold)
            } else {
                Column {
                    InfoText(context.getString(R.string.widget_info_week_title), 12, colors.sub, FontWeight.Medium)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        top.forEachIndexed { index, (categoryId, minutes) ->
                            if (index > 0) Spacer(modifier = GlanceModifier.width(8.dp))
                            val category = byId.getValue(categoryId)
                            Row(
                                modifier = GlanceModifier
                                    .background(ImageProvider(colors.chip))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Dot(categoryColor(category.colorHex))
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                InfoText(
                                    context.getString(
                                        R.string.widget_info_chip_format,
                                        categoryDisplayName(category, context),
                                        formatDuration(context, minutes, roundQuarter = true)
                                    ),
                                    13, colors.text, FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- ресівери й оновлення ----------

class InfoOnlineRing2x1Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = InfoOnlineRing2x1() }
class InfoOnlineBar4x1Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = InfoOnlineBar4x1() }
class InfoActive2x1Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = InfoActive2x1() }
class InfoStart2x1Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = InfoStart2x1() }
class InfoOrb1x1Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = InfoOrb1x1() }
class InfoLast4x1Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = InfoLast4x1() }
class InfoWeek4x1Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = InfoWeek4x1() }

/** Оновлює всі інформаційні віджети — викликають WidgetUpdateWorker/WidgetRolloverWorker і ActiveTimerStore. */
suspend fun updateAllInfoWidgets(context: Context) {
    InfoOnlineRing2x1().updateAll(context)
    InfoOnlineBar4x1().updateAll(context)
    InfoActive2x1().updateAll(context)
    InfoStart2x1().updateAll(context)
    InfoOrb1x1().updateAll(context)
    InfoLast4x1().updateAll(context)
    InfoWeek4x1().updateAll(context)
}

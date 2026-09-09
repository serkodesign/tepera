package com.serkodesign.tepera.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.toggleCategoryTimer
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.first

/**
 * RemoteViews (тобто й Glance) не вміє відобразити androidx.compose.material ImageVector
 * напряму — потрібен реальний drawable-ресурс. Тому для віджета — окремий, спрощений набір
 * vector drawable (drawable/ic_widget_*), а не той самий catalog, що categoryIcon() в застосунку
 * (ui/category/CategoryVisuals.kt). Лише 5 дефолтних категорій: CategoryButtonsRow бере
 * take(5), кастомна (6-та) категорія на віджеті ніколи не показується.
 */
private fun widgetIconRes(iconName: String): Int = when (iconName) {
    "nature" -> R.drawable.ic_widget_nature
    "reading" -> R.drawable.ic_widget_reading
    "hobby" -> R.drawable.ic_widget_hobby
    "movement" -> R.drawable.ic_widget_movement
    "social" -> R.drawable.ic_widget_social
    "sleep" -> R.drawable.ic_widget_sleep // legacy, вже заархівована категорія (v2.4)
    else -> R.drawable.ic_widget_generic
}

/**
 * FR-4.1–4.6: компактна 4x1 (5 кнопок категорій) і розширена 4x2 (+ шкала балансу) через
 * SizeMode.Responsive. Кнопки категорій — той самий тап-таймер, що на Home (перший тап починає,
 * другий по тій самій категорії зупиняє й зберігає, toggleCategoryTimer()) — БЕЗ live-лічильника
 * (FR-4.2 лишається чинним для самого віджета): активний стан позначається лише статичним
 * кільцем навколо кнопки, оновлюється одразу після тапу (ToggleCategoryTimerAction викликає
 * update()) або періодично через WidgetUpdateWorker ~30 хв.
 */
class TeperaWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 60.dp),
            DpSize(250.dp, 120.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TeperaApp

        val activeCategories = app.categoryRepository.observeActiveCategories().first()
        val sorted = sortCategoriesForWidget(activeCategories)

        // FR-4.6: перша занедбана категорія (>3 дні без запису) серед показаних кнопок.
        val neglectedCategoryId = sorted.take(5).firstOrNull { category ->
            isNeglected(app.activityRepository.lastLoggedTime(category.id))
        }?.id

        val activeTimers = app.activeTimerStore.activeTimers.first()

        // SRS v2.5, FR-3.5: точка старту дня замінює локальну північ — та сама логіка, що на
        // Home (BalanceViewModel.refresh()).
        val hasUsageAccess = app.balanceRepository.hasUsageAccess()
        val sleepWindowEndHour = app.settingsStore.sleepWindowEndHour.first()
        val dayStartMillis = app.balanceRepository.calculateDayStartMillis(sleepWindowEndHour)
        val onlineMinutes = if (hasUsageAccess) app.balanceRepository.getOnlineMinutesToday(dayStartMillis) else 0
        val dayLengthMinutes = app.balanceRepository.calculateDayLengthMinutes(dayStartMillis)
        val targetMinutes = app.settingsStore.targetMinutes.first()

        // FR-4.1: та сама тришарова структура доби, що на Home (Online + категорії з часом
        // сьогодні + Решта дня) — не окремий Online/Offline підрахунок.
        val entries = app.activityRepository
            .observeEntriesInRange(startOfTodayMillis(), Long.MAX_VALUE)
            .first()
        val minutesByCategory = entries.groupBy { it.categoryId }
            .mapValues { (_, categoryEntries) -> categoryEntries.sumOf { it.durationMinutes } }
        val allCategories = app.categoryRepository.observeAllCategories().first()
        val loggedSegments = allCategories
            .filter { (minutesByCategory[it.id] ?: 0) > 0 }
            .sortedBy { it.sortOrder }
            .map { categoryColor(it.colorHex) to minutesByCategory.getValue(it.id) }
        val loggedMinutes = loggedSegments.sumOf { it.second }
        val restOfDayMinutes = (dayLengthMinutes - onlineMinutes - loggedMinutes).coerceAtLeast(0)
        val daySegments = buildList {
            if (onlineMinutes > 0) add(TeperaPalette.onlineCard to onlineMinutes)
            addAll(loggedSegments)
            if (restOfDayMinutes > 0) add(TeperaPalette.restOfDayCard to restOfDayMinutes)
        }

        provideContent {
            GlanceTheme {
                val isExtended = LocalSize.current.height >= 100.dp

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        // Фіксований TeperaPalette-колір (не GlanceTheme.colors.background,
                        // яке слідує системній темі) — узгоджується з рішенням "дизайн ЗАВЖДИ
                        // light" для Home/Статистики (Theme.kt): та сама напівпрозора "скляна"
                        // картка, що й нижній навбар-"таблетка" в застосунку.
                        .background(ColorProvider(day = TeperaPalette.navPill, night = TeperaPalette.navPill))
                        .cornerRadius(24.dp)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryButtonsRow(
                        categories = sorted.take(5),
                        neglectedCategoryId = neglectedCategoryId,
                        activeTimers = activeTimers,
                        context = context
                    )
                    if (isExtended) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        DayStructureRow(
                            hasUsageAccess = hasUsageAccess,
                            segments = daySegments,
                            dayLengthMinutes = dayLengthMinutes,
                            targetMinutes = targetMinutes,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

// За запитом (новий стиль застосунку) — збільшено з 48dp: FR-4.1 вимагає ЛИШЕ мінімум
// >=48x48dp, а не стелю в 48dp; попередня стеля не давала кнопкам вирости, навіть коли
// ширина/висота віджета дозволяли, через що іконки виглядали дрібними в 4x1.
private val MAX_BUTTON_SIZE = 56.dp
private val BUTTON_GAP = 4.dp
private val RING_INSET = 4.dp // зазор між зовнішнім кільцем і внутрішньою карткою
private val ICON_PADDING = 6.dp // відступ від картки до самої іконки

@Composable
private fun CategoryButtonsRow(
    categories: List<CategoryEntity>,
    neglectedCategoryId: String?,
    activeTimers: Map<String, Long>,
    context: Context
) {
    // Реальна ширина, яку дає launcher, не завжди збігається з нашими DpSize-кандидатами
    // (targetCellWidth залежить від конкретного launcher-а) — тому розмір кнопки рахуємо
    // від фактичної LocalSize.current.width, а не жорстко фіксуємо 48dp: інакше 5 кнопок
    // по 48dp можуть не влізти й обрізатись праворуч замість акуратного зменшення.
    val count = categories.size.coerceAtLeast(1)
    val availableWidth = LocalSize.current.width - BUTTON_GAP * (count - 1)
    val buttonSize = (availableWidth / count).coerceIn(1.dp, MAX_BUTTON_SIZE)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        categories.forEachIndexed { index, category ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(BUTTON_GAP))
            CategoryButton(
                category = category,
                isNeglected = category.id == neglectedCategoryId,
                isTracking = activeTimers.containsKey(category.id),
                context = context,
                size = buttonSize
            )
        }
    }
}

private val CATEGORY_ID_KEY = ActionParameters.Key<String>("category_id")

@Composable
private fun CategoryButton(
    category: CategoryEntity,
    isNeglected: Boolean,
    isTracking: Boolean,
    context: Context,
    size: Dp
) {
    // Пріоритет кільця: активний таймер > занедбана категорія > нічого. Колір кільця для
    // активного таймера навмисно контрастний (error), а не колір самої категорії — інакше він
    // зливається з однаково пофарбованою карткою і кільце не видно.
    //
    // ВАЖЛИВО: .background() застосовується ЗАВЖДИ, лише колір змінюється (прозорий за
    // замовчуванням) — а не умовно то є, то немає самого модифікатора. RemoteViews-діфінг у
    // Glance не завжди коректно ЗНІМАЄ раніше застосований background, коли новий рендер узагалі
    // не викликає .background(): на реальному пристрої кільце "застрягало" після зупинки
    // таймера, поки колір лишався той самий модифікатор з іншим значенням.
    val ringColor = when {
        isTracking -> GlanceTheme.colors.error
        isNeglected -> GlanceTheme.colors.primary
        else -> ColorProvider(day = Color.Transparent, night = Color.Transparent)
    }
    // Картка — той самий принцип, що категорійні картки на Home (BalanceCard.kt/HomeScreen.kt):
    // біла, коли активна, напівпрозора інакше, іконка тонована власним кольором категорії
    // (не колір-кружок з білою літерою, як було раніше).
    val cardColor = if (isTracking) TeperaPalette.cardActive else TeperaPalette.cardTranslucent

    Box(
        modifier = GlanceModifier
            .size(size)
            .background(ringColor)
            .cornerRadius(size / 2)
            .clickable(
                actionRunCallback<ToggleCategoryTimerAction>(
                    actionParametersOf(CATEGORY_ID_KEY to category.id)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val innerSize = (size - RING_INSET).coerceAtLeast(1.dp)
        Box(
            modifier = GlanceModifier
                .size(innerSize)
                .background(ColorProvider(day = cardColor, night = cardColor))
                .cornerRadius(innerSize / 2),
            contentAlignment = Alignment.Center
        ) {
            if (isTracking) {
                // "■" — той самий принцип, що іконка "стоп" на Home, без live-лічильника
                // (FR-4.2 лишається чинним саме для віджета).
                Text(
                    text = "■",
                    style = TextStyle(color = GlanceTheme.colors.error, fontWeight = FontWeight.Bold)
                )
            } else {
                val color = categoryColor(category.colorHex)
                val iconSize = (innerSize - ICON_PADDING).coerceAtLeast(1.dp)
                Image(
                    provider = ImageProvider(widgetIconRes(category.iconName)),
                    contentDescription = categoryDisplayName(category, context),
                    colorFilter = ColorFilter.tint(ColorProvider(day = color, night = color)),
                    modifier = GlanceModifier.size(iconSize)
                )
            }
        }
    }
}

/** Тап по кнопці категорії на віджеті — toggleCategoryTimer(), та сама логіка, що на Home. */
class ToggleCategoryTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val categoryId = parameters[CATEGORY_ID_KEY] ?: return
        val app = context.applicationContext as TeperaApp
        toggleCategoryTimer(app.activeTimerStore, app.activityRepository, categoryId)
        // provideGlance() не перекомпоновується сам по собі після ActionCallback — без явного
        // update() кільце й "■" з'явились би лише при наступному WidgetUpdateWorker (~30 хв).
        TeperaWidget().update(context, glanceId)
    }
}

/**
 * FR-4.1, FR-3.10 (SRS v2.5): тиха тришарова шкала структури доби — БЕЗ тексту з сумами (шкала
 * має бути "візуально тихішою за кнопки логування", розділ 4.3), лише коли є доступ до
 * статистики використання; без нього — той самий заклик до дії, що на Home.
 */
@Composable
private fun DayStructureRow(
    hasUsageAccess: Boolean,
    segments: List<Pair<Color, Int>>,
    dayLengthMinutes: Int,
    targetMinutes: Int,
    context: Context
) {
    if (!hasUsageAccess) {
        Text(
            text = context.getString(R.string.usage_access_prompt_title),
            style = TextStyle(color = GlanceTheme.colors.onBackground)
        )
        return
    }
    if (dayLengthMinutes <= 0 || segments.isEmpty()) return // день щойно почався — ще нема чого показувати

    GlanceDayStructureBar(segments = segments, dayLengthMinutes = dayLengthMinutes, targetMinutes = targetMinutes)
}

// Glance/RemoteViews не має Canvas і GlanceModifier.defaultWeight() не приймає довільну вагу
// (лише рівний розподіл) — на відміну від BalanceCard у застосунку (Compose Canvas), тому
// шкалу тут імітуємо решіткою з фіксованої кількості РІВНИХ за вагою сегментів, кожен пофарбований
// залежно від того, у яку смугу дня (Online/категорія/Решта дня) він потрапляє за часовою часткою.
// 20 сегментів дають ~5% роздільної здатності, достатньо для розміру віджета. Заввишки 8dp —
// суттєво тихіша за 48-56dp кнопки категорій над нею (FR-4.1).
private const val BALANCE_BAR_SEGMENTS = 20

/** FR-3.10: та сама формула засічки орієнтиру, що на Home (BalanceCard.DayStructureBar). */
@Composable
private fun GlanceDayStructureBar(segments: List<Pair<Color, Int>>, dayLengthMinutes: Int, targetMinutes: Int) {
    val referenceMinutes = maxOf(dayLengthMinutes, targetMinutes, 1)
    val markerIndex = ((targetMinutes.toFloat() / referenceMinutes) * (BALANCE_BAR_SEGMENTS - 1))
        .toInt()
        .coerceIn(0, BALANCE_BAR_SEGMENTS - 1)
    // Тиха нейтральна риска — НЕ error/тривожний колір (FR-4.3: жодного trafic-light кодування,
    // засічка ніколи не змінює колір при перевищенні).
    val markerColor = ColorProvider(day = Color.Black.copy(alpha = 0.3f), night = Color.Black.copy(alpha = 0.3f))

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(8.dp)
            .cornerRadius(4.dp)
    ) {
        for (index in 0 until BALANCE_BAR_SEGMENTS) {
            if (index > 0) Spacer(modifier = GlanceModifier.width(1.dp))
            val segmentColor = if (index == markerIndex) {
                markerColor
            } else {
                val fraction = (index + 0.5f) / BALANCE_BAR_SEGMENTS
                val color = colorForFraction(segments, dayLengthMinutes, fraction)
                ColorProvider(day = color, night = color)
            }
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .background(segmentColor)
            ) {}
        }
    }
}

/** Який сегмент дня (Online/категорія/Решта дня) відповідає даній частці ширини шкали. */
private fun colorForFraction(segments: List<Pair<Color, Int>>, totalMinutes: Int, fraction: Float): Color {
    val targetMinute = fraction * totalMinutes
    var cumulative = 0
    for ((color, minutes) in segments) {
        cumulative += minutes
        if (targetMinute < cumulative) return color
    }
    return segments.lastOrNull()?.first ?: TeperaPalette.restOfDayCard
}

class TeperaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget()
}

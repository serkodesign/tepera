package com.serkodesign.tepera.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.toggleCategoryTimer
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.startOfLogicalDayMillis
import kotlinx.coroutines.flow.first


/**
 * Кнопки віджета — Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR), компонент "Activity icons 2"
 * (node 234:442) і віджети 4x1/2x1/1x1/4x2 (nodes 234:788, 235:935, 234:808, 234:848): БЕЗ фону
 * віджета, кола 64dp прямо на шпалерах. Невибрана — біле коло й сірий (#505050) контурний гліф;
 * вибрана (запущений таймер) — коло відтінку категорії й ТЕМНИЙ заповнений гліф того ж відтінку.
 * Гліфи — vector drawable `ic_widget2_*` 1:1 з SVG-асетів макета (RemoteViews не вміє
 * ImageVector). Це не той самий набір, що `ic_widget_*` (їх і далі використовує застосунок).
 * "Прогулянка" (footprint) імпортована для повноти, але не підключена — окремої категорії нема.
 */
internal fun widgetIconRes(iconName: String, selected: Boolean): Int = when (iconName) {
    "nature" -> if (selected) R.drawable.ic_widget2_nature_selected else R.drawable.ic_widget2_nature
    "reading" -> if (selected) R.drawable.ic_widget2_reading_selected else R.drawable.ic_widget2_reading
    "hobby" -> if (selected) R.drawable.ic_widget2_hobby_selected else R.drawable.ic_widget2_hobby
    "movement" -> if (selected) R.drawable.ic_widget2_movement_selected else R.drawable.ic_widget2_movement
    "social" -> if (selected) R.drawable.ic_widget2_social_selected else R.drawable.ic_widget2_social
    "errands" -> if (selected) R.drawable.ic_widget2_errands_selected else R.drawable.ic_widget2_errands
    "sleep" -> if (selected) R.drawable.ic_widget2_sleep_selected else R.drawable.ic_widget2_sleep // legacy, архівна (v2.4)
    else -> R.drawable.ic_widget_generic
}

/** Розмір гліфа з макета: book/content_cut/directions_bike — 24, решта — 25.5. */
private fun widgetIconGlyphSize(iconName: String): Dp = when (iconName) {
    "reading", "hobby", "movement" -> 24.dp
    else -> 25.5.dp
}

/** Колір кола й темного гліфа вибраної кнопки. */
internal class ButtonTint(val circle: Color, val glyph: Color)

// Точні значення макета (node 234:442): коло / темний гліф вибраного стану.
internal val FIGMA_TINTS = mapOf(
    "reading" to ButtonTint(Color(0xFFECC3F2), Color(0xFF783D83)),
    "movement" to ButtonTint(Color(0xFFFAECCC), Color(0xFF7C5B14)), // "Sport"
    "hobby" to ButtonTint(Color(0xFFFADEEE), Color(0xFF790645)),
    "social" to ButtonTint(Color(0xFFC6F1EF), Color(0xFF004340)), // "Time with people"
    "nature" to ButtonTint(Color(0xFFD4EFE5), Color(0xFF00744C)),
    "errands" to ButtonTint(Color(0xFFE3EFC4), Color(0xFF42550F)),
    "sleep" to ButtonTint(Color(0xFFBCC9FA), Color(0xFF213260))
)

/**
 * Вибрана кнопка: для дефолтних категорій — відтінки макета, для власних (їх у макеті нема, за
 * рішенням користувача) — власний колір категорії з прозорістю 25% і його затемнений варіант як гліф.
 */
private fun selectedTint(category: CategoryEntity): ButtonTint {
    category.nameKey?.let { key -> FIGMA_TINTS[key]?.let { return it } }
    val base = categoryColor(category.colorHex)
    return ButtonTint(
        circle = base.copy(alpha = 0.25f),
        glyph = Color(base.red * 0.45f, base.green * 0.45f, base.blue * 0.45f)
    )
}

/**
 * FR-4.1–4.6: компактна 4x1 (5 кнопок категорій) і розширена 4x2 (кнопки + картка сітки доби) через
 * SizeMode.Responsive. Кнопки категорій — той самий тап-таймер, що на Home (перший тап починає,
 * другий по тій самій категорії зупиняє й зберігає, toggleCategoryTimer()) — БЕЗ live-лічильника
 * (FR-4.2 лишається чинним): активний стан — вибрана кнопка (коло відтінку категорії), оновлюється
 * одразу після тапу або періодично через WidgetUpdateWorker ~30 хв. Зовнішній вигляд — за Figma
 * "App concept" (див. коментар біля [widgetIconRes]); без фону віджета.
 */
open class TeperaWidget : GlanceAppWidget() {

    /** Скільки кнопок категорій показує цей розмір віджета (1x1 — 1, 2x1 — 2, 4x1/4x2 — 5). */
    protected open val maxButtons: Int = MAX_WIDGET_BUTTONS

    // 60 — лише ряд кнопок (4x1); решта — розширений стан: 76 (капсула) + 12 + картка сітки. Макет 4x2 —
    // повний макет = капсула 76 + 12 + картка 127 = 215dp, але реальна висота 4x2 на S23 ~212dp, а Responsive обирає найбільший розмір, що ВМІЩУЄТЬСЯ, — тому брейкпоінт 205 (клітинки ~21dp); 140/170 — лаунчери з нижчими рядками.
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 60.dp),
            DpSize(250.dp, 140.dp),
            DpSize(250.dp, 170.dp),
            DpSize(250.dp, 205.dp),
            // Ширші брейкпоінти: кнопки 4x1/4x2 підбираються під реальну ширину (див. CategoryButtonsRow)
            DpSize(300.dp, 60.dp),
            DpSize(300.dp, 140.dp),
            DpSize(300.dp, 170.dp),
            DpSize(300.dp, 205.dp),
            DpSize(340.dp, 60.dp),
            DpSize(340.dp, 140.dp),
            DpSize(340.dp, 170.dp),
            DpSize(340.dp, 205.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TeperaApp
        // Початкові значення завантажуються ДО першого кадру: без цього кожен collectAsState стартував
        // з порожнього значення й віджет мигав порожнім станом та перемальовувався 6-11 разів поспіль
        // (виміряно на S23) — тепер оновлення шлеться лише при справжній зміні даних.
        val midnight = startOfLogicalDayMillis()
        val initial = LiveWidgetInitial(
            activeCategories = app.categoryRepository.observeActiveCategories().first(),
            allCategories = app.categoryRepository.observeAllCategories().first(),
            selectedIds = app.settingsStore.widgetCategoryIds.first(),
            activeTimers = app.activeTimerStore.activeTimers.first(),
            entries = app.activityRepository.observeEntriesInRange(midnight, Long.MAX_VALUE).first(),
            grid = WidgetLiveData.gridInputs(app, midnight)
        )
        provideContent { LiveWidgetContent(context, app, initial, maxButtons) }
    }

    /**
     * Прев'ю для меню віджетів (Android 15+, Glance 1.2.0): той самий [WidgetContent] на демо-даних
     * (шість дефолтних категорій, "типова" сітка доби). Раніше `previewLayout`/`initialLayout`
     * вказували на `widget_loading` — просто спінер, тому меню віджетів показувало спінер замість
     * віджета. Реєструється через `GlanceAppWidgetManager.setWidgetPreviews()` (див. TeperaApp).
     * На старіших версіях Android діє статичний `previewImage` (`widget_preview.xml`).
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            WidgetContent(
                context = context,
                categories = sortCategoriesForWidget(DefaultCategories.all),
                // Як у макеті: перша кнопка вибрана.
                activeTimers = sortCategoriesForWidget(DefaultCategories.all).firstOrNull()?.let { mapOf(it.id to 0L) } ?: emptyMap(),
                hasUsageAccess = true,
                gridSlots = previewGridSlots(),
                categoriesById = DefaultCategories.all.associateBy { it.id },
                maxButtons = maxButtons
            )
        }
    }
}

// Figma node 236:956: ряд кнопок — капсула 72dp заввишки (Surface/surface-card-transparent #FFFFFF@30%,
// радіус 100, відступ 8) з колами 56dp; проміжок — justify-between на всю ширину; у 2x1 — 8dp.
private val CATEGORY_BUTTON_SIZE = 60.dp // макет 236:956 — 56; збільшено до 60 за запитом користувача
private val PILL_PADDING = 12.dp // макет 236:956 — 8; збільшено до 12 за запитом користувача
private val PILL_HEIGHT = CATEGORY_BUTTON_SIZE + PILL_PADDING * 2
private val BUTTON_GAP = 8.dp
private val MIN_SPREAD_GAP = 4.dp
internal val WIDGET_GLYPH_UNSELECTED = Color(0xFF505050) // Text/text-secondary
internal val WIDGET_CIRCLE_UNSELECTED = Color.White // Surface/surface-card

// Figma node 234:855 (картка сітки в 4x2): відступ 12, проміжок клітинок 3, радіус клітинки 4,
// клітинка 23.83x23.5 при ширині 343; картка — Surface/surface-card-transparent.
private const val DAILY_GRID_COLUMNS = 12
private const val DAILY_GRID_ROWS = 4
private val CARD_PADDING = 12.dp
private val DAILY_GRID_GAP = 3.dp
private val DAILY_GRID_CELL_RADIUS = 4.dp
private val DAILY_GRID_MAX_CELL = 23.5.dp
private val DAILY_GRID_MIN_CELL = 8.dp // нижче — на тісних лаунчерах (висота 140dp) клітинки ще читаються
private val ROW_TO_CARD_GAP = 12.dp

// Figma node 234:848: кольори клітинок. Категорії й Online — реальні кольори (макет має лише демо).
private val WIDGET_GRID_PRE_UNLOCK_COLOR = Color(0xFFA172FF) // до точки старту дня
private val WIDGET_GRID_ONLINE_COLOR = Color(0xFFF5C401) // той самий #F5C401, що TeperaPalette.onlineCard
private val WIDGET_GRID_BLANK_PAST = Color.White // Grey/0 — минуло, нічого не залоговано
private val WIDGET_GRID_BLANK_FUTURE = Color(0x80A7A7A7) // rgba(167,167,167,0.5) — ще не настало

/**
 * Ряд кнопок у напівпрозорій капсулі (Figma node 236:956). Повний ряд (5) на всю ширину віджета —
 * justify-between (Glance Row не має Arrangement.SpaceBetween, той самий ефект дає
 * Spacer(defaultWeight()) між кнопками); менше кнопок або [fillWidth] = false (2x1/1x1) — капсула
 * по вмісту, кнопки щільно з проміжком 8dp, щоб 2-3 кнопки не розліталися по краях. Радіус капсули
 * більший за половину висоти — GradientDrawable сам обмежує його до повного заокруглення (для
 * 1x1 виходить коло 72dp).
 */
@Composable
private fun CategoryButtonsRow(
    categories: List<CategoryEntity>,
    activeTimers: Map<String, Long>,
    context: Context,
    fillWidth: Boolean = true,
    buttonSize: Dp = CATEGORY_BUTTON_SIZE,
    padding: Dp = PILL_PADDING
) {
    val spread = fillWidth && categories.size >= MAX_WIDGET_BUTTONS
    // Повний ряд: кнопка зменшується пропорційно, щоб 5 кіл + відступи + мінімальні проміжки вміщались у
    // ширину віджета. Інакше RemoteViews стискає кола по ширині в овали (P9, вужчий лаунчер-грід).
    val size = if (spread) {
        minOf(buttonSize, (LocalSize.current.width - padding * 2 - MIN_SPREAD_GAP * (categories.size - 1)) / categories.size)
            .coerceAtLeast(32.dp)
    } else {
        buttonSize
    }
    Row(
        modifier = (if (spread) GlanceModifier.fillMaxWidth() else GlanceModifier)
            .background(ImageProvider(R.drawable.widget_pill_translucent))
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEachIndexed { index, category ->
            if (index > 0) {
                if (spread) Spacer(modifier = GlanceModifier.defaultWeight()) else Spacer(modifier = GlanceModifier.width(BUTTON_GAP))
            }
            CategoryButton(category, activeTimers.containsKey(category.id), context, size)
        }
    }
}

private val CATEGORY_ID_KEY = ActionParameters.Key<String>("category_id")

/**
 * Кнопка-коло: шар кола (тонується) під шаром гліфа (тонується). Не `cornerRadius`/`background` —
 * кругле тло в RemoteViews надійно дає лише drawable (cornerRadius працює з API 31), а тонування
 * drawable-кола ColorFilter-ом дає будь-який відтінок (у т.ч. напівпрозорий для власних категорій).
 */
@Composable
private fun CategoryButton(
    category: CategoryEntity,
    isTracking: Boolean,
    context: Context,
    size: Dp = CATEGORY_BUTTON_SIZE
) {
    val tint = if (isTracking) selectedTint(category) else null
    val circleColor = tint?.circle ?: WIDGET_CIRCLE_UNSELECTED
    val glyphColor = tint?.glyph ?: WIDGET_GLYPH_UNSELECTED

    Box(
        modifier = GlanceModifier.size(size).clickable(
            actionRunCallback<ToggleCategoryTimerAction>(actionParametersOf(CATEGORY_ID_KEY to category.id))
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.widget_circle_solid),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(day = circleColor, night = circleColor)),
            modifier = GlanceModifier.fillMaxSize()
        )
        Image(
            provider = ImageProvider(widgetIconRes(category.iconName, selected = isTracking)),
            contentDescription = categoryDisplayName(category, context),
            colorFilter = ColorFilter.tint(ColorProvider(day = glyphColor, night = glyphColor)),
            modifier = GlanceModifier.size(widgetIconGlyphSize(category.iconName))
        )
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
    }
}

/**
 * Картка сітки доби (Figma node 234:855) — напівпрозорий білий прямокутник із радіусом 16 і
 * відступом 12, усередині сітка 12x4 (клітинки 4dp-радіуса, проміжок 3). БЕЗ доступу до статистики —
 * текстовий заклик до дії; Online-клітинки тоді не з'являться, решта лишається коректною.
 */
@Composable
private fun DailyGridCard(
    hasUsageAccess: Boolean,
    slots: List<DailyGridSlot>,
    categoriesById: Map<String, CategoryEntity>,
    context: Context
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_card_translucent))
            .padding(CARD_PADDING)
    ) {
        if (!hasUsageAccess) {
            Text(
                text = context.getString(R.string.usage_access_prompt_title),
                style = TextStyle(color = GlanceTheme.colors.onBackground)
            )
            return@Column
        }

        // Сітка малюється ОДНИМ растровим зображенням (Canvas), а не 12x4 вузлами Box — RemoteViews-хост
        // відкидає зайві діти контейнера (на S23 замість 12 клітинок у ряду було видно 10, а з Spacer-ами
        // між ними ~5). Ширина картинки розтягується на всю ширину картки (FillBounds).
        // Висота клітинки: LocalSize.height у Responsive — найближчий МЕНШИЙ розмір зі списку, а не
        // реальна висота, тож вона наближена; від макетних 23.5dp стеля, знизу — запобіжник.
        val available = LocalSize.current.height - PILL_HEIGHT - ROW_TO_CARD_GAP - CARD_PADDING * 2
        val cellHeight = ((available - DAILY_GRID_GAP * (DAILY_GRID_ROWS - 1)) / DAILY_GRID_ROWS)
            .coerceIn(DAILY_GRID_MIN_CELL, DAILY_GRID_MAX_CELL)
        val density = context.resources.displayMetrics.density
        val gridHeight = cellHeight * DAILY_GRID_ROWS + DAILY_GRID_GAP * (DAILY_GRID_ROWS - 1)
        val bitmap = renderDailyGridBitmap(
            slots = slots,
            categoriesById = categoriesById,
            // Номінальна ширина ~343dp (макет) мінус відступи; реальна відрізнятиметься — FillBounds підганяє.
            widthPx = ((343.dp - CARD_PADDING * 2).value * density).toInt(),
            cellHeightPx = cellHeight.value * density,
            gapPx = DAILY_GRID_GAP.value * density,
            cornerRadiusPx = DAILY_GRID_CELL_RADIUS.value * density
        )
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxWidth().height(gridHeight)
        )
    }
}

/** Малює сітку доби 12x4 (по рядках зліва направо) у Bitmap — див. коментар у [DailyGridSection]. */
private fun renderDailyGridBitmap(
    slots: List<DailyGridSlot>,
    categoriesById: Map<String, CategoryEntity>,
    widthPx: Int,
    cellHeightPx: Float,
    gapPx: Float,
    cornerRadiusPx: Float
): Bitmap {
    val heightPx = (cellHeightPx * DAILY_GRID_ROWS + gapPx * (DAILY_GRID_ROWS - 1)).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cellWidthPx = (widthPx - gapPx * (DAILY_GRID_COLUMNS - 1)) / DAILY_GRID_COLUMNS
    for (row in 0 until DAILY_GRID_ROWS) {
        for (col in 0 until DAILY_GRID_COLUMNS) {
            paint.color = colorForGridSlot(slots[row * DAILY_GRID_COLUMNS + col], categoriesById).toArgb()
            val left = col * (cellWidthPx + gapPx)
            val top = row * (cellHeightPx + gapPx)
            canvas.drawRoundRect(RectF(left, top, left + cellWidthPx, top + cellHeightPx), cornerRadiusPx, cornerRadiusPx, paint)
        }
    }
    return bitmap
}

private fun colorForGridSlot(slot: DailyGridSlot, categoriesById: Map<String, CategoryEntity>): Color =
    when (slot) {
        is DailyGridSlot.PreUnlock -> WIDGET_GRID_PRE_UNLOCK_COLOR
        is DailyGridSlot.Category ->
            categoriesById[slot.categoryId]?.let { categoryColor(it.colorHex) } ?: WIDGET_GRID_BLANK_PAST
        is DailyGridSlot.Online -> WIDGET_GRID_ONLINE_COLOR
        is DailyGridSlot.Blank -> if (slot.isFuture) WIDGET_GRID_BLANK_FUTURE else WIDGET_GRID_BLANK_PAST
    }


/**
 * Другий запис у меню віджетів (типовий розмір 4x2, life_balance_widget_4x2_info.xml) — той самий
 * вміст і ті самі розміри Responsive, що [TeperaWidget]; окремий клас потрібен, бо Glance зіставляє
 * провайдера з класом віджета.
 */
class TeperaWidget4x2 : TeperaWidget()

/** 1x1: одна кнопка — перша з обраних у налаштуваннях віджета. */
class TeperaWidget1x1 : TeperaWidget() {
    override val maxButtons = 1
    override val sizeMode: SizeMode = SizeMode.Exact
}

class TeperaWidget1x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget1x1()
}

/** 2x1: дві кнопки — перші дві з обраних у налаштуваннях віджета. */
class TeperaWidget2x1 : TeperaWidget() {
    override val maxButtons = 2
    override val sizeMode: SizeMode = SizeMode.Exact
}

class TeperaWidget2x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget2x1()
}

class TeperaWidget4x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget4x2()
}

class TeperaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget()
}


/** Початкові значення для [LiveWidgetContent] — завантажуються в provideGlance() до першого кадру. */private class LiveWidgetInitial(    val activeCategories: List<CategoryEntity>,    val allCategories: List<CategoryEntity>,    val selectedIds: List<String>,    val activeTimers: Map<String, Long>,    val entries: List<ActivityEntryEntity>,    val grid: GridInputs)
/**
 * Вміст віджета на ЖИВИХ даних: таймери/категорії/записи — потоки всередині композиції (див. коментар
 * у [WidgetLiveData] — раніше все читалось один раз на сесію й показувало застарілий стан).
 */
@Composable
private fun LiveWidgetContent(context: Context, app: TeperaApp, initial: LiveWidgetInitial, maxButtons: Int) {
    val selectedIds by app.settingsStore.widgetCategoryIds.collectAsState(initial = initial.selectedIds)
    val activeCategories by app.categoryRepository.observeActiveCategories().collectAsState(initial = initial.activeCategories)
    val allCategories by app.categoryRepository.observeAllCategories().collectAsState(initial = initial.allCategories)
    val activeTimers by app.activeTimerStore.activeTimers.collectAsState(initial = initial.activeTimers)
    val tick by WidgetLiveData.refreshTick.collectAsState()

    // Figma node 11:647: сітка доби — 48 клітинок по 30 хв від КАЛЕНДАРНОЇ півночі, кожна фарбується
    // реальним кольором категорії/Online. Ручні записи — жива підписка, Online/точка старту — кеш.
    val midnight = remember(tick) { startOfLogicalDayMillis() }
    val entriesFlow = remember(midnight) { app.activityRepository.observeEntriesInRange(midnight, Long.MAX_VALUE) }
    val entries by entriesFlow.collectAsState(initial = initial.entries)
    val gridInputs by produceState<GridInputs?>(initialValue = initial.grid, tick) {
        value = WidgetLiveData.gridInputs(app, midnight)
    }

    val sorted = remember(activeCategories, selectedIds) { categoriesForWidget(activeCategories, selectedIds) }
    val categoriesById = remember(allCategories) { allCategories.associateBy { it.id } }
    val slots = gridInputs?.let {
        calculateDailyGridSlots(
            calendarMidnightMillis = midnight,
            dayStartMillis = it.dayStartMillis,
            nowMillis = System.currentTimeMillis(),
            entries = entries,
            onlineMinutesPerSlot = it.onlineMinutesPerSlot
        )
    } ?: List(DAILY_GRID_SLOT_COUNT) { DailyGridSlot.Blank(isFuture = true) }

    WidgetContent(
        context = context,
        categories = sorted,
        activeTimers = activeTimers,
        hasUsageAccess = gridInputs?.hasUsageAccess ?: true,
        gridSlots = slots,
        categoriesById = categoriesById,
        maxButtons = maxButtons
    )
}

/** Демо-сітка доби для прев'ю: ніч (до пробудження), трохи Online й активностей, далі ще не настало. */
private fun previewGridSlots(): List<DailyGridSlot> = List(DAILY_GRID_SLOT_COUNT) { index ->
    when (index) {
        in 0..13 -> DailyGridSlot.PreUnlock
        in 14..15, 19, 24, 28 -> DailyGridSlot.Blank(isFuture = false)
        in 16..18, 25, 26 -> DailyGridSlot.Online
        in 20..22 -> DailyGridSlot.Category(DefaultCategories.READING_ID)
        23 -> DailyGridSlot.Category(DefaultCategories.MOVEMENT_ID)
        27 -> DailyGridSlot.Category(DefaultCategories.HOBBY_ID)
        else -> DailyGridSlot.Blank(isFuture = true)
    }
}

/**
 * 1x1 / 2x1 (Figma nodes 234:808, 235:935, у стилі капсули 236:956): одна або дві кнопки 56dp у
 * напівпрозорій капсулі (1x1 — коло 72dp), проміжок 8dp, по центру віджета. Макета цих розмірів у
 * новому стилі нема — капсула перенесена з 4x1 за рішенням користувача ("всі віджети під цей").
 */
@Composable
private fun SmallWidgetContent(
    context: Context,
    categories: List<CategoryEntity>,
    activeTimers: Map<String, Long>
) {
    if (categories.isEmpty()) return
    // SizeMode.Exact: LocalSize — справжній розмір віджета. 1x1 на Samsung — лише 76x94dp, тож капсула
    // 84dp (кнопка 60 + відступ 12) обрізалась. Кнопка лишається 60dp, доки вміщується, а відступ капсули
    // стискається (до 0), щоб капсула не виходила за межі віджета.
    val size = LocalSize.current
    val gaps = BUTTON_GAP * (categories.size - 1)
    val button = minOf(
        CATEGORY_BUTTON_SIZE,
        (size.width - gaps) / categories.size,
        size.height
    ).coerceAtLeast(32.dp)
    val padding = minOf(
        PILL_PADDING,
        (size.width - button * categories.size - gaps) / 2,
        (size.height - button) / 2
    ).coerceAtLeast(0.dp)
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CategoryButtonsRow(categories, activeTimers, context, fillWidth = false, buttonSize = button, padding = padding)
    }
}

private const val MAX_WIDGET_BUTTONS = 5

/** Вміст віджета — спільний для [TeperaWidget.provideGlance] (реальні дані) і [TeperaWidget.providePreview] (демо). */
@Composable
private fun WidgetContent(
    context: Context,
    categories: List<CategoryEntity>,
    activeTimers: Map<String, Long>,
    hasUsageAccess: Boolean,
    gridSlots: List<DailyGridSlot>,
    categoriesById: Map<String, CategoryEntity>,
    maxButtons: Int = MAX_WIDGET_BUTTONS
) {
    GlanceTheme {
        if (maxButtons < MAX_WIDGET_BUTTONS) {
            SmallWidgetContent(context, categories.take(maxButtons), activeTimers)
            return@GlanceTheme
        }
        // 4x1 — лише ряд кнопок (Figma 234:788); від 100dp — розширений 4x2 із карткою сітки (234:848).
        val isExtended = LocalSize.current.height >= 100.dp
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryButtonsRow(categories.take(maxButtons), activeTimers, context)
            if (isExtended) {
                Spacer(modifier = GlanceModifier.height(ROW_TO_CARD_GAP))
                DailyGridCard(hasUsageAccess, gridSlots, categoriesById, context)
            }
        }
    }
}

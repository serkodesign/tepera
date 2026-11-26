package com.serkodesign.tepera.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.toArgb
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.toggleCategoryTimer
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import kotlinx.coroutines.flow.first


/**
 * Кнопки віджета — Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR), компонент "Activity icons 2"
 * (node 234:442) і віджети 4x1/2x1/1x1 (nodes 234:788, 235:935, 234:808): БЕЗ фону
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
    // Кастомні категорії (Material Symbols, ic_widget2_<key>.xml): ті самі ключі, що в CategoryVisuals.customCategoryIconChoices.
    "star" -> if (selected) R.drawable.ic_widget2_star_selected else R.drawable.ic_widget2_star
    "favorite" -> if (selected) R.drawable.ic_widget2_favorite_selected else R.drawable.ic_widget2_favorite
    "coffee" -> if (selected) R.drawable.ic_widget2_coffee_selected else R.drawable.ic_widget2_coffee
    "music" -> if (selected) R.drawable.ic_widget2_music_selected else R.drawable.ic_widget2_music
    "brush" -> if (selected) R.drawable.ic_widget2_brush_selected else R.drawable.ic_widget2_brush
    "pets" -> if (selected) R.drawable.ic_widget2_pets_selected else R.drawable.ic_widget2_pets
    "work" -> if (selected) R.drawable.ic_widget2_work_selected else R.drawable.ic_widget2_work
    "restaurant" -> if (selected) R.drawable.ic_widget2_restaurant_selected else R.drawable.ic_widget2_restaurant
    "flight" -> if (selected) R.drawable.ic_widget2_flight_selected else R.drawable.ic_widget2_flight
    "fitness" -> if (selected) R.drawable.ic_widget2_fitness_selected else R.drawable.ic_widget2_fitness
    "celebration" -> if (selected) R.drawable.ic_widget2_celebration_selected else R.drawable.ic_widget2_celebration
    "home" -> if (selected) R.drawable.ic_widget2_home_selected else R.drawable.ic_widget2_home
    else -> if (selected) R.drawable.ic_widget_generic else R.drawable.ic_widget2_generic
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
    "nature" to ButtonTint(Color(0xFFC5E2CB), Color(0xFF00744C)),
    "errands" to ButtonTint(Color(0xFFE3EFC4), Color(0xFF42550F)),
    "sleep" to ButtonTint(Color(0xFFBCC9FA), Color(0xFF213260))
)

/**
 * Вибрана кнопка: для дефолтних категорій — відтінки макета, для власних (їх у макеті нема, за
 * рішенням користувача) — власний колір категорії з прозорістю 25% і його затемнений варіант як гліф.
 */
private fun selectedTint(category: CategoryEntity): ButtonTint {
    category.nameKey?.let { key -> FIGMA_TINTS[key]?.let { return it } }
    return customActiveTint(categoryColor(category.colorHex))
}

/** Мінімальний контраст гліфа до кола активної кнопки — мінімум WCAG для графічних елементів (3:1). */
private const val ACTIVE_MIN_CONTRAST = 3.0

/** Найбільше світлішання кола (частка білого): фон завжди лишається помітно кольоровим. */
private const val ACTIVE_MAX_MIX = 0.7f

/**
 * Активна кастомна кнопка: гліф — точний колір категорії (обраний при створенні), коло — той самий колір,
 * світлішає (змішування з білим), доки контраст гліфа до кола не досягне [ACTIVE_MIN_CONTRAST].
 * Якщо навіть білий фон не дає потрібного контрасту (деякі теплі кольори), лишається білий.
 */
private fun customActiveTint(base: Color): ButtonTint {
    // blendARGB на малих значеннях може дати альфу < 255 (напр. #FE…), а ColorUtils.calculateContrast
    // такого фону не приймає ("background can not be translucent") — тому кожне значення робимо непрозорим.
    val baseArgb = ColorUtils.setAlphaComponent(base.toArgb(), 0xFF)
    fun lighten(mix: Float): Int = ColorUtils.setAlphaComponent(
        ColorUtils.blendARGB(baseArgb, android.graphics.Color.WHITE, mix), 0xFF
    )
    var mix = 0f
    while (mix < ACTIVE_MAX_MIX && ColorUtils.calculateContrast(baseArgb, lighten(mix)) < ACTIVE_MIN_CONTRAST) {
        mix += 0.02f
    }
    return ButtonTint(
        circle = Color(lighten(mix.coerceAtMost(1f))),
        glyph = base
    )
}

/**
 * FR-4.1–4.6: ряд кнопок категорій у капсулі — 4x1 (до 5 кнопок), 2x1 (2) і 1x1 (1), див. підкласи нижче. Кнопки — той
 * самий тап-таймер, що на Home (перший тап починає, другий по тій самій категорії зупиняє й зберігає,
 * toggleCategoryTimer()) — БЕЗ live-лічильника (FR-4.2 лишається чинним): активний стан — вибрана кнопка (коло відтінку
 * категорії), оновлюється одразу після тапу або періодично через WidgetUpdateWorker ~30 хв. Вигляд — за Figma
 * "App concept" (див. коментар біля [widgetIconRes]). Віджет 4x2 із сіткою доби прибрано (23.11.2026, за запитом власника).
 */
open class TeperaWidget : GlanceAppWidget() {

    /** Скільки кнопок категорій показує цей розмір віджета (1x1 — 1, 2x1 — 2, 4x1 — 5). */
    protected open val maxButtons: Int = MAX_WIDGET_BUTTONS

    // Exact: LocalSize — справжній розмір віджета, кнопки підбираються під реальну ширину (див. CategoryButtonsRow).
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TeperaApp
        // Початкові значення завантажуються ДО першого кадру: без цього кожен collectAsState стартував
        // з порожнього значення й віджет мигав порожнім станом та перемальовувався 6-11 разів поспіль
        // (виміряно на S23) — тепер оновлення шлеться лише при справжній зміні даних.
        val initial = LiveWidgetInitial(
            activeCategories = app.categoryRepository.observeActiveCategories().first(),
            selectedIds = app.settingsStore.widgetCategoryIds.first(),
            activeTimers = app.activeTimerStore.activeTimers.first()
        )
        provideContent { LiveWidgetContent(context, app, initial, maxButtons) }
    }

    /**
     * Прев'ю для меню віджетів (Android 15+, Glance 1.2.0): той самий [WidgetContent] на демо-даних (шість
     * дефолтних категорій). Реєструється через `GlanceAppWidgetManager.setWidgetPreviews()` (див. TeperaApp).
     * На старіших версіях Android діє `previewLayout` / статичний `previewImage`.
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            WidgetContent(
                context = context,
                categories = sortCategoriesForWidget(DefaultCategories.all),
                // Як у макеті: перша кнопка вибрана.
                activeTimers = sortCategoriesForWidget(DefaultCategories.all).firstOrNull()?.let { mapOf(it.id to 0L) } ?: emptyMap(),
                maxButtons = maxButtons
            )
        }
    }
}

// Figma node 236:956: ряд кнопок — капсула 72dp заввишки (Surface/surface-card-transparent #FFFFFF@30%,
// радіус 100, відступ 8) з колами 56dp; проміжок — justify-between на всю ширину; у 2x1 — 8dp.
private val CATEGORY_BUTTON_SIZE = 60.dp // макет 236:956 — 56; збільшено до 60 за запитом користувача
private val PILL_PADDING = 12.dp // макет 236:956 — 8; збільшено до 12 за запитом користувача
private val BUTTON_GAP = 8.dp
private val MIN_SPREAD_GAP = 4.dp
internal val WIDGET_GLYPH_UNSELECTED = Color(0xFF505050) // Text/text-secondary
internal val WIDGET_CIRCLE_UNSELECTED = Color.White // Surface/surface-card
// ui-redesign: ті самі значення, що @color/widget_circle_unselected і @color/widget_glyph_unselected у values-night — сам
// віджет бере їх із ресурсів; ці константи лише для Compose-прев'ю віджета в онбордингу.
internal val WIDGET_GLYPH_UNSELECTED_NIGHT = Color(0xFFFEFFEF)
internal val WIDGET_CIRCLE_UNSELECTED_NIGHT = Color(0x24FFFFFF)


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
    padding: Dp = PILL_PADDING,
    // false — без капсули: кнопки лежать прямо на фоні віджета.
    capsule: Boolean = true
) {
    val spread = fillWidth && categories.size >= MAX_WIDGET_BUTTONS
    val inset = if (capsule) padding else 0.dp
    // Повний ряд: кнопка зменшується пропорційно, щоб 5 кіл + відступи + мінімальні проміжки вміщались у
    // ширину віджета. Інакше RemoteViews стискає кола по ширині в овали (P9, вужчий лаунчер-грід).
    val size = if (spread) {
        minOf(buttonSize, (LocalSize.current.width - inset * 2 - MIN_SPREAD_GAP * (categories.size - 1)) / categories.size)
            .coerceAtLeast(32.dp)
    } else {
        buttonSize
    }
    Row(
        modifier = (if (spread) GlanceModifier.fillMaxWidth() else GlanceModifier)
            .then(if (capsule) GlanceModifier.background(ImageProvider(R.drawable.widget_pill_translucent)) else GlanceModifier)
            .padding(inset),
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

internal val CATEGORY_ID_KEY = ActionParameters.Key<String>("category_id")

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
        // Невибрана кнопка — кольори з ресурсів (values-night), без ColorFilter: лаунчер сам перемикає їх разом із
        // системною темою. Вибрана — тонування відтінком категорії, однаковим в обох темах.
        Image(
            provider = ImageProvider(if (tint == null) R.drawable.widget_circle_unselected else R.drawable.widget_circle_solid),
            contentDescription = null,
            colorFilter = tint?.let { ColorFilter.tint(ColorProvider(day = circleColor, night = circleColor)) },
            modifier = GlanceModifier.fillMaxSize()
        )
        Image(
            provider = ImageProvider(widgetIconRes(category.iconName, selected = isTracking)),
            // Стан (таймер іде) — у описі, бо Glance не має stateDescription: інакше скрінрідер не відрізнить кнопки.
            contentDescription = categoryDisplayName(category, context).let {
                if (isTracking) context.getString(R.string.widget_button_running_format, it) else it
            },
            colorFilter = tint?.let { ColorFilter.tint(ColorProvider(day = glyphColor, night = glyphColor)) },
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

/** 1x1: одна кнопка — перша з обраних у налаштуваннях віджета. */
class TeperaWidget1x1 : TeperaWidget() {
    override val maxButtons = 1
    override val sizeMode: SizeMode = SizeMode.Exact
}

class TeperaWidget1x1Receiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.BUTTONS_1X1) {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget1x1()
}

/** 2x1: дві кнопки — перші дві з обраних у налаштуваннях віджета. */
class TeperaWidget2x1 : TeperaWidget() {
    override val maxButtons = 2
    override val sizeMode: SizeMode = SizeMode.Exact
}

class TeperaWidget2x1Receiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.BUTTONS_2X1) {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget2x1()
}

class TeperaWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.BUTTONS_4X1) {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget()
}


/** Початкові значення для [LiveWidgetContent] — завантажуються в provideGlance() до першого кадру. */
private class LiveWidgetInitial(
    val activeCategories: List<CategoryEntity>,
    val selectedIds: List<String>,
    val activeTimers: Map<String, Long>
)

/**
 * Вміст віджета на ЖИВИХ даних: таймери й категорії — потоки всередині композиції (див. коментар
 * у [WidgetLiveData] — раніше все читалось один раз на сесію й показувало застарілий стан).
 */
@Composable
private fun LiveWidgetContent(context: Context, app: TeperaApp, initial: LiveWidgetInitial, maxButtons: Int) {
    val selectedIds by app.settingsStore.widgetCategoryIds.collectAsState(initial = initial.selectedIds)
    val activeCategories by app.categoryRepository.observeActiveCategories().collectAsState(initial = initial.activeCategories)
    val activeTimers by app.activeTimerStore.activeTimers.collectAsState(initial = initial.activeTimers)

    val sorted = remember(activeCategories, selectedIds) { categoriesForWidget(activeCategories, selectedIds) }
    WidgetContent(context = context, categories = sorted, activeTimers = activeTimers, maxButtons = maxButtons)
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

/** Вміст віджета 4x1/2x1/1x1 — спільний для [TeperaWidget.provideGlance] (реальні дані) і [TeperaWidget.providePreview] (демо). */
@Composable
private fun WidgetContent(
    context: Context,
    categories: List<CategoryEntity>,
    activeTimers: Map<String, Long>,
    maxButtons: Int = MAX_WIDGET_BUTTONS
) {
    GlanceTheme {
        if (maxButtons < MAX_WIDGET_BUTTONS) {
            SmallWidgetContent(context, categories.take(maxButtons), activeTimers)
            return@GlanceTheme
        }
        // 4x1 — ряд кнопок у капсулі (Figma 234:788). Фон завжди задається (лише drawable) — умовний background()
        // на RemoteViews іноді не знімає раніше встановлений фон.
        Column(
            modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_root_none)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryButtonsRow(categories.take(maxButtons), activeTimers, context)
        }
    }
}

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
 * (ui/category/CategoryVisuals.kt). **CategoryButtonsRow бере take(5)** — з 6 дефолтних (T-8,
 * tepera-dev-spec.md, додано "Справи") і кастомними категоріями на віджеті завжди видно лише
 * перші 5 активних за sortOrder; яка саме це п'ятірка, залежить від того, що людина вимкнула
 * онбордингом T-8 чи пізніше в Налаштуваннях — не завжди буквально "5 початкових".
 *
 * **Figma node 9:609 ("Імпортуй всі ці іконки в проект") — увесь компонент "Activity icons"
 * (усі 8: Reading/Sport/Hobby/Walk/Sleep/Nature/Time with people/Errands) імпортовано як vector
 * drawable 1:1 з Figma pathData**, обидва стани кожного, крім Справ (checklist) — там Figma дає
 * ОДНУ форму на обидва стани (лише колір заливки відрізняється в самому асеті). "Прогулянка"
 * (footprint, ic_widget_walk*.xml) імпортована для повноти набору, але не підключена в `when`
 * нижче — у Tepera немає окремої категорії "Прогулянка" (Рух/спорт представлений велосипедом,
 * "Sport"). Сон підключений (був лише на старому дженерик-гліфі) — категорія архівна (v2.4), не
 * показується активним слотом на віджеті, але гліф коректний для будь-якого майбутнього виклику.
 */
private fun widgetIconRes(iconName: String, selected: Boolean): Int = when (iconName) {
    "nature" -> if (selected) R.drawable.ic_widget_nature_selected else R.drawable.ic_widget_nature
    "reading" -> if (selected) R.drawable.ic_widget_reading_selected else R.drawable.ic_widget_reading
    "hobby" -> if (selected) R.drawable.ic_widget_hobby_selected else R.drawable.ic_widget_hobby
    "movement" -> if (selected) R.drawable.ic_widget_movement_selected else R.drawable.ic_widget_movement
    "social" -> if (selected) R.drawable.ic_widget_social_selected else R.drawable.ic_widget_social
    "errands" -> R.drawable.ic_widget_errands // T-8 (tepera-dev-spec.md) — та сама форма обидва стани
    "sleep" -> if (selected) R.drawable.ic_widget_sleep_selected else R.drawable.ic_widget_sleep // legacy, вже заархівована категорія (v2.4)
    else -> R.drawable.ic_widget_generic
}

/**
 * Figma node 9:609: РІВНО той розмір гліфа, що в компоненті ("size-[24px]"/"size-[25.5px]"),
 * НЕ підганяється під розмір кнопки — раніше іконка займала майже всю кнопку
 * (`size - ICON_PADDING - RING_INSET` ≈ 39dp у 48dp колі), Figma ж центрує набагато менший
 * гліф із великим полем навколо (24-25.5px у 48px колі). Book/content_cut/directions_bike — 24px;
 * camping/groups/checklist (і legacy sleep) — 25.5px.
 */
private fun widgetIconGlyphSize(iconName: String): Dp = when (iconName) {
    "reading", "hobby", "movement" -> 24.dp
    else -> 25.5.dp
}

/**
 * FR-4.1–4.6: компактна 4x1 (5 кнопок категорій) і розширена 4x3 (кнопки + сітка доби) через
 * SizeMode.Responsive. Кнопки категорій — той самий тап-таймер, що на Home (перший тап починає,
 * другий по тій самій категорії зупиняє й зберігає, toggleCategoryTimer()) — БЕЗ live-лічильника
 * (FR-4.2 лишається чинним для самого віджета): активний стан позначається лише статичним
 * кільцем навколо кнопки, оновлюється одразу після тапу (ToggleCategoryTimerAction викликає
 * update()) або періодично через WidgetUpdateWorker ~30 хв.
 *
 * **Figma node 9:421/11:647 ("перемалюй віджет") — повний редизайн поверх T-7.** Замінює
 * попередню тришарову шкалу "Твій день" (GlanceDayStructureBar, T-7) на сітку доби 12x4
 * (DailyGridSection, DailyGridCalculator.kt) — за прямим рішенням користувача сітка анкерується
 * на календарну північ, не на точку старту дня Tepera; кнопки категорій лишаються ВЕРХНІМ рядом,
 * сітка — нижче. 4x1 не змінився (лише кнопки, як і раніше). Іконки Читання/Хобі/Рух-спорт і
 * градієнтний фон взято безпосередньо з Figma-асетів (деталі — коментарі біля widgetIconRes()
 * і widget_gradient_bg.xml); решта категорій лишається на попередньому контурному наборі.
 */
class TeperaWidget : GlanceAppWidget() {

    // Figma node 11:647 ("перемалюй віджет"): розширений стан піднято зі 120dp (4x2, T-7, тонка
    // шкала) до 180dp (4x3, сітка доби 12x4 потребує більше висоти). Компактний 4x1 (60dp, лише
    // кнопки) не змінився.
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 60.dp),
            DpSize(250.dp, 180.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TeperaApp

        val activeCategories = app.categoryRepository.observeActiveCategories().first()
        val sorted = sortCategoriesForWidget(activeCategories)

        val activeTimers = app.activeTimerStore.activeTimers.first()

        // SRS v2.5, FR-3.5: точка старту дня замінює локальну північ — та сама логіка, що на
        // Home (BalanceViewModel.refresh()). Тут вона позначає лише межу PreUnlock-клітинок
        // сітки доби (DailyGridCalculator.kt) — сама сітка анкерується на календарну північ
        // (нижче), за прямим рішенням користувача під час запиту на перемальовку.
        val hasUsageAccess = app.balanceRepository.hasUsageAccess()
        val sleepWindows = app.sleepWindowRepository.getEnabledWindows()
        val dayStartMillis = app.balanceRepository.calculateDayStartMillis(sleepWindows)

        val allCategories = app.categoryRepository.observeAllCategories().first()
        val categoriesById = allCategories.associateBy { it.id }

        // Figma node 11:647: сітка доби замінює колишню тришарову шкалу "Твій день" (T-7,
        // GlanceDayStructureBar) — 48 клітинок по 30 хв, кожна пофарбована реальним кольором
        // категорії/Online, а не часткою сумарних хвилин. Деталі алгоритму — DailyGridCalculator.kt.
        val calendarMidnightMillis = startOfTodayMillis()
        val entries = app.activityRepository
            .observeEntriesInRange(calendarMidnightMillis, Long.MAX_VALUE)
            .first()
        val onlineMinutesPerSlot = if (hasUsageAccess) {
            app.balanceRepository.getOnlineMinutesPerSlot(
                fromMillis = calendarMidnightMillis,
                slotMinutes = 30,
                slotCount = DAILY_GRID_SLOT_COUNT
            )
        } else {
            IntArray(DAILY_GRID_SLOT_COUNT)
        }
        val gridSlots = calculateDailyGridSlots(
            calendarMidnightMillis = calendarMidnightMillis,
            dayStartMillis = dayStartMillis,
            nowMillis = System.currentTimeMillis(),
            entries = entries,
            onlineMinutesPerSlot = onlineMinutesPerSlot
        )

        provideContent {
            GlanceTheme {
                val isExtended = LocalSize.current.height >= 100.dp

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        // Figma node 11:647 ("Widget") — темний фон із трьома розмитими кольоровими
                        // плямами (Group 2, node 11:648), відтворений трьома шарами radial-градієнта
                        // з ФАКТИЧНИХ координат/кольорів/blur-параметрів вектора (get_design_context +
                        // download_assets на сам SVG, не скріншот — деталі й точні значення
                        // Ellipse 4/5/6 — коментар у widget_gradient_bg.xml). shape-drawable, не
                        // ColorProvider: RemoteViews/Glance не має Canvas/Brush-градієнтів. Кути
                        // радіуса вже в кожному шарі shape, окремий .cornerRadius() тут не потрібен.
                        .background(ImageProvider(R.drawable.widget_gradient_bg))
                        // За прямим запитом користувача зменшено з буквального Figma-паддінга
                        // (p-[24px]) до 16dp — більше місця для збільшених 56dp-кнопок і сітки.
                        .padding(WIDGET_CONTENT_PADDING),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Figma node 11:647: кнопки категорій — ВЕРХНІЙ ряд, сітка доби — нижче
                    // (порядок протилежний попередній T-7 версії, де бар був зверху, кнопки —
                    // знизу). 4x1 (isExtended == false) не змінюється — лише кнопки.
                    CategoryButtonsRow(
                        categories = sorted.take(5),
                        activeTimers = activeTimers,
                        context = context
                    )
                    if (isExtended) {
                        // Буквальний Figma gap-[24px] між рядом кнопок і сіткою (код фрейму:
                        // flex-col gap-[24px]) — попередні 10dp були довільним наближенням.
                        Spacer(modifier = GlanceModifier.height(ROW_TO_GRID_GAP))
                        DailyGridSection(
                            hasUsageAccess = hasUsageAccess,
                            slots = gridSlots,
                            categoriesById = categoriesById,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

// За прямим запитом користувача — паддінг по краю всього контенту віджета (Column вище і
// availableWidth у DailyGridSection нижче), зменшений з буквального Figma-24dp до 16dp.
private val WIDGET_CONTENT_PADDING = 16.dp

// Figma node 9:421/11:647/9:609 ("перемалюй віджет") — кільцеві кнопки категорій на темному
// градієнтному фоні. Точний, буквально заданий користувачем spec для двох станів:
// - НЕ обрано: тло — #FFFFFF @ 0.5 прозорості ("ефект скла", widget_circle_fill_translucent.xml,
//   крізь яке просвічує кольоровий градієнт фону), символ — #FFFFFF @ 1.0 (повністю непрозорий).
// - Обрано: тло — #FFFFFF @ 1.0 (TeperaPalette.cardActive), символ — #003926 (фіксований темно-
//   зелений, ОДНАКОВИЙ для всіх категорій, не categoryColor() — перша версія тонувала вибрану
//   іконку кольором категорії, користувач прямо скасував це на користь фіксованого #003926,
//   точно як у вихідних Figma-асетах book_5/content_cut/directions_bike, де filled-варіант мав
//   буквально fill="#003926").
// Стан "занедбана категорія" (амбер-обвідна лінія, widget_circle_outline_neglected.xml, FR-4.6)
// прибрано за прямим запитом користувача — усі неактивні кнопки тепер виглядають однаково
// (напівпрозоре скляне заповнення), незалежно від того, коли категорію востаннє логували.
//
// **Реальний баг, знайдений на Samsung S23 (стосується й нинішньої, і попередньої версії
// контурної кнопки):** перша спроба — вкладені Box (зовнішній суцільного кольору кільця +
// внутрішній з `background(Color.Transparent)`) — на пристрої рендерилась як СУЦІЛЬНЕ
// зафарбоване коло, не контур. Причина: прозорий внутрішній Box не "пробиває діру" до фону
// віджета — у RemoteViews/Glance composite-порядку пізніший прозорий шар нічого не стирає з
// того, що вже намальоване під ним. Фікс — реальний shape-drawable-фон одним Box (не вкладена
// пара), тепер widget_circle_fill_translucent.xml (solid, напівпрозорий) для звичайної неактивної.
private val WIDGET_ICON_UNSELECTED = Color.White // #FFFFFF @ 1.0 — сам символ завжди непрозорий, прозорість дає лише тло кнопки
private val WIDGET_ICON_SELECTED = Color(0xFF003926) // фіксований темно-зелений, однаковий для всіх категорій

// Figma node 11:647: сітка доби (DailyGridSection нижче). Кольори підтверджені прямим рішенням
// користувача під час запиту на перемальовку — не з коду фрейму (той дає лише 2 умовні
// демо-кольори, #beffb8/#ffecac).
private val WIDGET_GRID_PRE_UNLOCK_COLOR = Color(0xFFA172FF) // до точки старту дня
private val WIDGET_GRID_ONLINE_COLOR = Color(0xFFFF9162) // непрозора версія TeperaPalette.onlineCard (той самий відтінок, 100% альфа — на маленькій клітинці 50%-прозорий колір губився б)
private val WIDGET_GRID_BLANK_PAST = Color.White // минуло, нічого не залоговано (Figma рядки 2-3)
private val WIDGET_GRID_BLANK_FUTURE = Color(0x80FFFFFF) // rgba(255,255,255,0.5) — ще не настало (Figma рядок 4)

// За прямим запитом користувача — кнопки категорій збільшено з буквального Figma 48dp до 56dp
// (гліф-символ усередині лишається попереднього розміру, widgetIconGlyphSize()).
private val CATEGORY_BUTTON_SIZE = 56.dp

// Буквальний Figma gap-[24px] між рядом кнопок і сіткою (винесено в константу — потрібен і в
// provideGlance() для Spacer, і в DailyGridSection() для розрахунку висоти клітинки, коментар там).
private val ROW_TO_GRID_GAP = 24.dp

// За прямим запитом користувача — сітка доби тепер розтягується на всю ширину віджета, а розмір
// клітинки не обмежений зверху (раніше стеля DAILY_GRID_MAX_CELL=16dp, буквальний Figma
// size-[16px] — прибрано, лишився тільки DAILY_GRID_MIN_CELL як запобіжник від виродження).
// Gap зменшено з 10dp до 8dp.
private const val DAILY_GRID_COLUMNS = 12
private const val DAILY_GRID_ROWS = 4
private val DAILY_GRID_GAP = 8.dp
private val DAILY_GRID_MIN_CELL = 10.dp

@Composable
private fun CategoryButtonsRow(
    categories: List<CategoryEntity>,
    activeTimers: Map<String, Long>,
    context: Context
) {
    // Кнопки — фіксовані 56dp (CATEGORY_BUTTON_SIZE вище), не адаптивні під ширину.
    val buttonSize = CATEGORY_BUTTON_SIZE

    // Буквальний Figma "justify-between" (код фрейму: flex items-center justify-between) — кнопки
    // впираються в обидва краї ряду, проміжки МІЖ ними рівні й заповнюють увесь залишок ширини,
    // а не фіксовані 4dp зліва купчасто. Glance Row не має Arrangement.SpaceBetween — той самий
    // ефект дає Spacer(defaultWeight()) МІЖ кнопками (не на самих кнопках): порожні розпірки
    // однаково розтягуються на весь залишок, кнопки лишаються фіксованого розміру.
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        categories.forEachIndexed { index, category ->
            if (index > 0) Spacer(modifier = GlanceModifier.defaultWeight())
            CategoryButton(
                category = category,
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
    isTracking: Boolean,
    context: Context,
    size: Dp
) {
    // Один Box, не вкладена пара (детальний розбір бага — коментар над WIDGET_ICON_UNSELECTED
    // вище). Активна — суцільне біле коло, іконка тонована фіксованим темно-зеленим. Неактивна —
    // напівпрозоре біле заповнення (widget_circle_fill_translucent.xml, Figma node 9:421/11:647).
    val boxModifier = GlanceModifier.size(size).let {
        if (isTracking) {
            it.background(ColorProvider(day = TeperaPalette.cardActive, night = TeperaPalette.cardActive))
                .cornerRadius(size / 2)
        } else {
            it.background(ImageProvider(R.drawable.widget_circle_fill_translucent))
        }
    }
    val iconColor = if (isTracking) WIDGET_ICON_SELECTED else WIDGET_ICON_UNSELECTED
    // Буквальний розмір гліфа з Figma (widgetIconGlyphSize(), коментар там), НЕ підганяється
    // під розмір кнопки.
    val iconSize = widgetIconGlyphSize(category.iconName)

    Box(
        modifier = boxModifier.clickable(
            actionRunCallback<ToggleCategoryTimerAction>(
                actionParametersOf(CATEGORY_ID_KEY to category.id)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(widgetIconRes(category.iconName, selected = isTracking)),
            contentDescription = categoryDisplayName(category, context),
            colorFilter = ColorFilter.tint(ColorProvider(day = iconColor, night = iconColor)),
            modifier = GlanceModifier.size(iconSize)
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
        TeperaWidget().update(context, glanceId)
    }
}

/**
 * Figma node 11:647 ("перемалюй віджет") — сітка доби, замінює колишню GlanceDayStructureBar
 * (T-7). БЕЗ доступу до статистики — той самий текстовий заклик до дії, що раніше показувала
 * шкала (Online-клітинки тоді просто не з'являться, DailyGridCalculator отримає нульовий
 * onlineMinutesPerSlot — решта категорійних/PreUnlock-клітинок лишається коректною й без доступу).
 */
@Composable
private fun DailyGridSection(
    hasUsageAccess: Boolean,
    slots: List<DailyGridSlot>,
    categoriesById: Map<String, CategoryEntity>,
    context: Context
) {
    if (!hasUsageAccess) {
        Text(
            text = context.getString(R.string.usage_access_prompt_title),
            style = TextStyle(color = GlanceTheme.colors.onBackground)
        )
        return
    }

    // За прямим запитом користувача — сітка МУСИТЬ розтягуватись на всю фактичну ширину віджета.
    // LocalSize.current.width НЕ підходить для цього: SizeMode.Responsive тут декларує рівно одне
    // значення ширини (250dp) для обох розмірів віджета, тож LocalSize завжди повертає це
    // номінальне число, навіть коли реальний виділений launcher-ом простір значно ширший (саме
    // тому попередня версія на основі LocalSize.current.width залишала порожній простір праворуч
    // від сітки — підтверджено пікселями на Samsung S23: ряд кнопок, який рахує свою ширину через
    // Spacer(defaultWeight()) і РЕАЛЬНЕ layout-обмеження, а не LocalSize, розтягувався коректно,
    // а сітка — ні). Фікс: ширина кожної клітинки — теж GlanceModifier.defaultWeight() (реальний
    // layout-розподіл замість Compose-time арифметики), гарантовано заповнює фактичну ширину на
    // будь-якому пристрої/лаунчері незалежно від того, що каже LocalSize.
    //
    // Висота клітинки й далі рахується через LocalSize.current.height (не ширину!) — вертикальний
    // розмір launcher-грида зазвичай відповідає номінальному значенню значно ближче за
    // горизонтальний (рядки грида менш гумові за колонки), тож ця арифметика лишається достатньо
    // точною для того, щоб клітинки виглядали приблизно квадратними.
    val availableHeight = LocalSize.current.height - WIDGET_CONTENT_PADDING * 2 - CATEGORY_BUTTON_SIZE - ROW_TO_GRID_GAP
    val cellHeight = ((availableHeight - DAILY_GRID_GAP * (DAILY_GRID_ROWS - 1)) / DAILY_GRID_ROWS)
        .coerceAtLeast(DAILY_GRID_MIN_CELL)
    // Буквальний Figma rounded-[6px] на клітинці 16px (6/16=0.375) — той самий коефіцієнт,
    // застосований до висоти (менший вимір клітинки-прямокутника).
    val cornerRadius = (cellHeight.value * 0.375f).dp

    // РЕАЛЬНИЙ баг, знайдений на Samsung S23 (той самий клас проблеми, що документований баг
    // Microsoft Launcher для старої шкали "Твій день" — коментар був у видаленому
    // colorForFraction/GlanceDayStructureBar): версія з окремим Spacer-дитиною між кожною
    // клітинкою (12 клітинок + 11 Spacer = 23 дитини в одному Row) рендерилась як лише ~5
    // видимих клітинок замість 12 — RemoteViews-хост, судячи з усього, згортає/обрізає Row з
    // надто великою кількістю дітей, і поріг тут НИЖЧИЙ, ніж на Microsoft Launcher (де збій був
    // при 39). Фікс — той самий принцип: проміжок як .padding(start=) на самій клітинці, а не
    // окремий Spacer-елемент, це вдвічі скорочує кількість дітей Row (12 замість 23).
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        for (row in 0 until DAILY_GRID_ROWS) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = if (row > 0) DAILY_GRID_GAP else 0.dp)
            ) {
                for (col in 0 until DAILY_GRID_COLUMNS) {
                    val slot = slots[row * DAILY_GRID_COLUMNS + col]
                    val color = colorForGridSlot(slot, categoriesById)
                    // Паддінг і фон — на РІЗНИХ Box (зовнішній/внутрішній), не на одному й тому
                    // самому вузлі: реальний баг, знайдений на Samsung S23 — `.padding(start=)`
                    // разом з `.defaultWeight()` на ОДНОМУ Box без дочірнього контенту не давав
                    // жодного видимого проміжку (фон малювався на всю виділену вагою ширину,
                    // паддінг просто ігнорувався, бо йому нема чийого контенту "стискати"). Фікс —
                    // паддінг+вага на зовнішньому порожньому Box, фон+заокруглення на внутрішньому
                    // fillMaxSize()-дочірньому — так паддінг зовнішнього справді звужує область,
                    // яку заповнює внутрішній колір. Кількість прямих дітей Row не змінюється
                    // (12, коментар про 23-дітей-баг нижче лишається чинним).
                    Box(
                        modifier = GlanceModifier
                            .padding(start = if (col > 0) DAILY_GRID_GAP else 0.dp)
                            .defaultWeight()
                            .height(cellHeight)
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(ColorProvider(day = color, night = color))
                                .cornerRadius(cornerRadius)
                        ) {}
                    }
                }
            }
        }
    }
}

private fun colorForGridSlot(slot: DailyGridSlot, categoriesById: Map<String, CategoryEntity>): Color =
    when (slot) {
        is DailyGridSlot.PreUnlock -> WIDGET_GRID_PRE_UNLOCK_COLOR
        is DailyGridSlot.Category ->
            categoriesById[slot.categoryId]?.let { categoryColor(it.colorHex) } ?: WIDGET_GRID_BLANK_PAST
        is DailyGridSlot.Online -> WIDGET_GRID_ONLINE_COLOR
        is DailyGridSlot.Blank -> if (slot.isFuture) WIDGET_GRID_BLANK_FUTURE else WIDGET_GRID_BLANK_PAST
    }

class TeperaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget()
}

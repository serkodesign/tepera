package com.serkodesign.tepera.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo

import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import kotlin.math.roundToInt

/**
 * Спільні "скляні" елементи для екранів другого рівня (Налаштування, Категорії, Виключені
 * застосунки, Резервне копіювання) з Figma-фрейму Everyday_Designs (сторінка "Tepera",
 * node 1951:909) — на відміну від Home/Статистики (TeperaPalette.teperaGradientBackground()),
 * ці екрани мають власний заголовок (кругла напівпрозора кнопка "назад" + текст) замість
 * TopAppBar, і "скляні" картки-рядки замість Material3 ListItem/Card.
 */

/**
 * Круглий напівпрозорий back-button + заголовок — замінює TopAppBar на цих екранах.
 * [trailing] — необов'язковий слот праворуч (напр. кнопка видалення на "Редагувати активність",
 * T-8+ styling pass) — порожній за замовчуванням, існуючі виклики без змін.
 */
@Composable
fun GlassScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        TeperaIconButton(
            icon = TeperaSymbols.ArrowBack,
            contentDescription = stringResource(R.string.nav_back),
            onClick = onBack,
            shape = CircleShape,
            containerColor = TeperaPalette.cardTranslucent,
            iconSize = 20.dp
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 24.sp),
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        trailing()
    }
}

/** Заголовок секції списку ("Активні", "Архівовано", "Excluded", "All" тощо). */
@Composable
fun GlassSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = 8.dp,
    bottomPadding: Dp = 8.dp
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp),
        modifier = modifier.padding(start = 8.dp, end = 8.dp, top = topPadding, bottom = bottomPadding).semantics { heading() }
    )
}

/**
 * Один "скляний" рядок картки: іконка (будь-який слот — тонований вектор у кружку чи реальна
 * бітмапа застосунку), назва, і trailing-слот (шеврон для навігації або Switch для перемикача).
 */
@Composable
fun GlassRow(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.settingsCardFill)
            // Обводка 1dp білим 100% (за запитом) — лише світла тема, як у картках активностей.
            .then(TeperaPalette.activityCardIdleBorder?.let { Modifier.border(1.dp, it, RoundedCornerShape(16.dp)) } ?: Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        leading()
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        trailing()
    }
}

/** Іконка в кольоровому кружку — стандартний "тайл" для навігаційних рядків Налаштувань. */
@Composable
fun TeperaIconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    background: Color = TeperaPalette.brandAccentSoft,
    tint: Color = TeperaPalette.brandAccent,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

/** Шеврон "перейти" для навігаційних GlassRow (Frame 9 у фреймі). */
@Composable
fun NavChevron(modifier: Modifier = Modifier) {
    Icon(
        TeperaSymbols.ChevronRight,
        contentDescription = null,
        modifier = modifier
    )
}

/** Кольори перемикача: увімкнений — фірмовий темно-зелений (як на toggle track у фреймі), вимкнений — за Material 3 (суцільний трек, рамка й ручка кольору outline). */
@Composable
fun teperaSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = TeperaPalette.brandAccent,
    checkedThumbColor = TeperaPalette.onPrimary,
    checkedBorderColor = Color.Transparent,
    uncheckedTrackColor = TeperaPalette.switchTrackOff,
    uncheckedThumbColor = TeperaPalette.switchOutlineOff,
    uncheckedBorderColor = TeperaPalette.switchOutlineOff
)

/**
 * "Таблетка"-перемикач на кілька варіантів (мова, і будь-де ще) — власна реалізація, а не
 * Material3 SingleChoiceSegmentedButtonRow: у фреймі суцільна напівпрозора "капсула" з ОДНИМ
 * суцільним білим чіпом на вибраному варіанті, без розділових ліній між сегментами.
 */
@Composable
fun <T> PillSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    // Анімація M3: білий "чіп" ковзає від старого варіанта до нового (emphasized, 300 мс), а не
    // з'являється миттєво на новому місці.
    val gap = 4.dp
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(TeperaPalette.cardTranslucent)
            .border(1.dp, TeperaPalette.fieldBorder, RoundedCornerShape(100.dp))
            .padding(4.dp)
    ) {
        val itemWidth = (maxWidth - gap * (options.size - 1)) / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = (itemWidth + gap) * selectedIndex,
            animationSpec = TeperaSpecs.spatial(),
            label = "segmentIndicator"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(100.dp))
                .background(TeperaPalette.colors.primaryFill)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEach { (value, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(100.dp))
                        .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) }),
                    contentAlignment = Alignment.Center
                ) {
                    // ui-redesign: вибраний сегмент — головна дія (референси: вибраний чип), текст контрастний до неї.
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (value == selected) TeperaPalette.onPrimary else TeperaPalette.buttonBrandDark
                    )
                }
            }
        }
    }
}

/**
 * Слайдер таргету Online-часу по цілих годинах (Settings, node 1951:1106) — за запитом
 * користувача, замість текстового поля з довільними хвилинами. Капсула з фрейму: ширина
 * заповненого чіпа = hours/maxHours від повної ширини (математично підтверджено самим
 * фреймом — приклад "3h" займає 128.64/343 ≈ 37.5% ширини, що дорівнює 3/8). Власна реалізація
 * через pointerInput, а не Material3 Slider: потрібен суцільний чіп-заповнення на всю висоту
 * капсули з текстом усередині, а не тонка лінія-трек + окремий кружок-повзунок.
 */
@Composable
fun HourRangeSlider(
    hours: Int,
    onHoursChange: (Int) -> Unit,
    valueLabel: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
    minHours: Int = 1,
    maxHours: Int = 8,
    accessibilityLabel: String? = null
) {
    val valueText = valueLabel(hours)
    fun hoursFromFraction(fraction: Float) =
        (fraction * maxHours).roundToInt().coerceIn(minHours, maxHours)

    // Анімація M3: тап плавно "дотягує" заповнення до нового значення (emphasized, 300 мс);
    // під час перетягування чіп іде за пальцем миттєво, без відставання.
    var dragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(TeperaPalette.cardTranslucent)
            .padding(4.dp)
            // Доступність (WCAG 4.1.2/2.1.1): повзунок з роллю, значенням і кроком для скрінрідера (свайп вгору/вниз
            // змінює значення) та стрілками клавіатури; без цього кастомний жест був недоступний не-дотиковим способом.
            .semantics {
                accessibilityLabel?.let { contentDescription = it }
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = hours.toFloat(),
                    range = minHours.toFloat()..maxHours.toFloat(),
                    steps = (maxHours - minHours - 1).coerceAtLeast(0)
                )
                setProgress { target ->
                    onHoursChange(target.roundToInt().coerceIn(minHours, maxHours))
                    true
                }
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionRight, Key.DirectionUp -> { onHoursChange((hours + 1).coerceAtMost(maxHours)); true }
                        Key.DirectionLeft, Key.DirectionDown -> { onHoursChange((hours - 1).coerceAtLeast(minHours)); true }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .pointerInput(minHours, maxHours) {
                detectTapGestures { offset ->
                    onHoursChange(hoursFromFraction(offset.x / size.width.toFloat()))
                }
            }
            .pointerInput(minHours, maxHours) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { change, _ ->
                    change.consume()
                    onHoursChange(hoursFromFraction(change.position.x / size.width.toFloat()))
                }
            }
    ) {
        // T-12 (tepera-dev-spec.md): вікно сну ввело слайдери з minHours=0 (0:00 — цілком
        // звичайне значення початку вікна), де попередня нижня межа 0.05f давала чіп занадто
        // вузьким для власного підпису ("0:00" переносився на два рядки в капсулі 44dp,
        // підтверджено на Samsung S23) — попередній єдиний виклик з minHours=1 (таргет
        // Online-часу) ніколи не діставався до hours=0, тож цей край не проявлявся раніше.
        // 0.16f — емпіричний мінімум, що вміщує "23:00"/"0:00" в один рядок.
        val targetFraction = (hours.toFloat() / maxHours).coerceIn(0.16f, 1f)
        val fraction by animateFloatAsState(
            targetValue = targetFraction,
            animationSpec = if (dragging) snap() else TeperaSpecs.spatial(),
            label = "sliderFill"
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(100.dp))
                .background(TeperaPalette.cardActive),
            contentAlignment = Alignment.Center
        ) {
            Text(valueText, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        // За прямим запитом користувача: тихі стрілки по краях, щоб було видніше, що капсулу
        // можна тягнути вліво/вправо (самого "повзунка" тут нема — лише заповнення, тож
        // перетягуваність інакше не читається візуально). Суто декоративні (clearAndSetSemantics) —
        // сам Box вище вже має повну семантику повзунка.
        Icon(
            TeperaSymbols.ChevronRight,
            contentDescription = null,
            tint = TeperaPalette.buttonBrandDark.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .size(18.dp)
                .graphicsLayer(scaleX = -1f)
                .clearAndSetSemantics {}
        )
        Icon(
            TeperaSymbols.ChevronRight,
            contentDescription = null,
            tint = TeperaPalette.buttonBrandDark.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .size(18.dp)
                .clearAndSetSemantics {}
        )
    }
}

enum class TeperaButtonSize(val height: Dp, val textSize: TextUnit, val fontWeight: FontWeight) {
    Big(108.dp, 14.sp, FontWeight.Medium),
    Medium(48.dp, 14.sp, FontWeight.Medium),
    Small(32.dp, 12.sp, FontWeight.Normal)
}

/** [Filled] — тональна акцентна дія (M3 filled tonal button): фон #DCF6ED, текст і іконка #006944, повне заокруглення. */
enum class TeperaButtonType { Primary, Secondary, Tertiary, Filled }

/**
 * Кнопка дизайн-системи — Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 190:639 (3 розміри x
 * 3 типи x enabled/disabled). Використовувати ЗАМІСТЬ ручного стилювання Material3
 * `Button`/`OutlinedButton`/`TextButton` (сталий запит користувача).
 *
 * - [TeperaButtonSize.Big] (108dp): Primary — суцільний #006944, білий текст, радіус 54dp
 *   (disabled — фон #003926); Secondary — рамка 1dp #003926, радіус 24dp. Tertiary у Big макет не
 *   містить — рендериться як Medium-стиль на висоті Big.
 * - [TeperaButtonSize.Medium] (48dp) / [TeperaButtonSize.Small] (32dp): Primary — білий фон, текст
 *   #006944; Secondary — рамка #003926; Tertiary — лише текст #003926, без фону й рамки. Радіус 24dp.
 * - Disabled — непрозорість 0.5 на всю кнопку (фон, рамку й текст разом, як у макеті), без кліків.
 * - Ширину задає виклик через [modifier] (макет: 163dp за замовчуванням, у рядках — weight(1f)).
 */
@Composable
fun TeperaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: TeperaButtonSize = TeperaButtonSize.Medium,
    type: TeperaButtonType = TeperaButtonType.Primary,
    enabled: Boolean = true,
    contentColorOverride: Color? = null, // для темних екранів (напр. онбординг дозволів), де тертиарний #003926 не читається
    textSizeOverride: TextUnit? = null,
    lineHeightOverride: TextUnit? = null,
    leadingIcon: ImageVector? = null
) {
    val big = size == TeperaButtonSize.Big
    val shape = RoundedCornerShape(if (big && type == TeperaButtonType.Primary) 54.dp else if (type == TeperaButtonType.Filled) 100.dp else 24.dp)

    val background: Color = when (type) {
        TeperaButtonType.Primary -> TeperaPalette.primaryButtonFill
        // Темна тема — #DCF6ED 30% (за запитом), світла — як раніше.
        TeperaButtonType.Filled -> if (LocalTeperaColors.current.isDark) Color(0x4DDCF6ED) else TeperaPalette.surfaceBrandLight.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val contentColor: Color = when {
        type == TeperaButtonType.Primary -> TeperaPalette.primaryButtonContent
        type == TeperaButtonType.Filled -> TeperaPalette.buttonBrand
        else -> TeperaPalette.buttonBrandDark
    }

    // Без тіней (за прямим запитом користувача — у застосунку їх немає ніде); вимкнення плавне (M3 effects).
    val stateAlpha by animateFloatAsState(if (enabled) 1f else 0.5f, TeperaSpecs.effects(), label = "buttonAlpha")

    Box(
        modifier = modifier
            .heightIn(min = size.height)
            .alpha(stateAlpha)
            .clip(shape)
            .background(background)
            .then(
                if (type == TeperaButtonType.Secondary) {
                    Modifier.border(1.dp, TeperaPalette.buttonBrandDark, shape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            // З іконкою ліворуч лівий відступ вдвічі менший (M3: іконка ближче до краю, ніж текст без іконки).
            .padding(start = if (leadingIcon != null) 8.dp else 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = contentColorOverride ?: contentColor, modifier = Modifier.size(18.dp))
            }
            Text(
                text = text,
                color = contentColorOverride ?: contentColor,
                fontSize = textSizeOverride ?: size.textSize,
                lineHeight = lineHeightOverride ?: TextUnit.Unspecified,
                letterSpacing = if (textSizeOverride != null) 0.sp else TextUnit.Unspecified,
                fontWeight = size.fontWeight,
                // Одна строка: текст у кнопці не переноситься (за запитом); задовгий обрізається "…", тож тексти
                // коротші (strings.xml). Раніше було 2 рядки — звідси перенос "Створити ворота".
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Степпер «− N год +» для орієнтиру Online-часу (меню "Відстеження") — замість [HourRangeSlider]
 * за вибором користувача: крок 1 година, кнопки — ті самі [TeperaIconButton] 44dp, значення
 * посередині. На межі відповідна кнопка стає неактивною (видима, але напівпрозора), щоб було
 * зрозуміло, що далі нема куди.
 */
@Composable
fun HourStepper(
    hours: Int,
    onHoursChange: (Int) -> Unit,
    valueLabel: @Composable (Int) -> String,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    minHours: Int = 1,
    maxHours: Int = 8
) {
    val valueText = valueLabel(hours)
    val brand = TeperaPalette.buttonBrandDark
    // Figma node 405:1703 "Орієнтир часу" (оновлено): без зовнішньої капсули — темні круглі кнопки 44dp з боків,
    // посередині світлий чіп (surface-brand-light #DCF6ED) зі значенням 18sp Medium, проміжки 4dp.
    // На межі неактивна кнопка лишається напівпрозорою.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = valueText },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeperaIconButton(
            icon = TeperaSymbols.Remove,
            contentDescription = decreaseDescription,
            onClick = { onHoursChange((hours - 1).coerceAtLeast(minHours)) },
            modifier = Modifier.width(44.dp),
            shape = CircleShape,
            containerColor = brand,
            contentColor = TeperaPalette.onPrimary,
            enabled = hours > minHours,
            height = 44.dp,
            iconSize = 24.dp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(28.dp))
                // Світла тема — Surface/surface-brand-light з макета (#DCF6ED); surfaceBrandLight тут білий (#FEFEFE).
                .background(if (LocalTeperaColors.current.isDark) TeperaPalette.surfaceBrandLight else Color(0xFFDCF6ED)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    lineHeight = 20.sp
                ),
                color = brand
            )
        }
        TeperaIconButton(
            icon = TeperaSymbols.Add,
            contentDescription = increaseDescription,
            onClick = { onHoursChange((hours + 1).coerceAtMost(maxHours)) },
            modifier = Modifier.width(44.dp),
            shape = CircleShape,
            containerColor = brand,
            contentColor = TeperaPalette.onPrimary,
            enabled = hours < maxHours,
            height = 44.dp,
            iconSize = 24.dp
        )
    }
}

/**
 * Поле часу в стилі Material 3 (OutlinedTextField із лейблом і іконкою годинника) для екранів
 * налаштувань. Поле тільки для читання: тап відкриває [TeperaTimePickerDialog], а не клавіатуру.
 * Рамка темна (#003926, 1dp) і текст 22sp — щоб поле читалось як кнопка-вибір, а не як фон картки.
 */
@Composable
fun TeperaTimeField(
    label: String,
    minute: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val text = "%02d:%02d".format(minute / 60, minute % 60)
    // Стиль поля "Назва" (TeperaTextField): блок 56dp, радіус 8, заливка inputSurface, підпис 11sp і значення 16sp.
    val fieldShape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(fieldShape)
            .background(TeperaPalette.inputSurface, fieldShape)
            .border(1.dp, TeperaPalette.inputSurface, fieldShape)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                color = TeperaPalette.textSecondary
            )
            Text(
                text = text,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = TeperaPalette.textPrimary
            )
        }
        // Прозорий шар поверх поля перехоплює тап — поле тільки для читання, вибір іде через діалог часу.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button) { showPicker = true }
                .semantics { contentDescription = "$label $text" }
        )
    }
    if (showPicker) {
        TeperaTimePickerDialog(
            title = label,
            minuteOfDay = minute,
            onSelected = { onSelected(it); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * Іконкова кнопка дизайн-системи (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR: play/pause і
 * "more_time" на картках Home, книга/шестерня в шапці) — висота 44dp, іконка 24dp. Ширину задає
 * виклик через [modifier] (за замовчуванням квадрат 44dp; `Modifier.weight(1f)` для широкої),
 * форму — через [shape] (шапка Home має асиметричні радіуси).
 */
@Composable
fun TeperaIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(44.dp),
    shape: Shape = RoundedCornerShape(22.dp),
    containerColor: Color = TeperaPalette.chipSurface,
    contentColor: Color = TeperaPalette.colors.chipContent,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    val stateAlpha by animateFloatAsState(if (enabled) 1f else 0.5f, TeperaSpecs.effects(), label = "iconButtonAlpha")
    Box(
        modifier = modifier
            .height(height)
            .alpha(stateAlpha)
            .clip(shape)
            .background(containerColor)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = contentColor, modifier = Modifier.size(iconSize))
    }
}

/**
 * Заголовок екрана верхнього рівня (Щоденник, Статистика) — Figma "App concept", node 208:1339:
 * Golos Text Medium 27sp, line-height 1.1, letter-spacing 0.027, #003926; контейнер висотою 60,
 * padding зліва 24 / справа 16, під статус-баром.
 */
@Composable
fun TeperaScreenTitle(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(60.dp)
            .padding(start = 24.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = TeperaPalette.buttonBrandDark,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 27.sp,
            lineHeight = 29.7.sp,
            letterSpacing = 0.027.sp
        )
    }
}

/**
 * Поле пошуку (M3 "search bar" у виконанні застосунку): пігулка 56dp зі скляним фоном, лупа ліворуч, поле вводу,
 * кнопка "очистити" з'являється лише коли щось введено. Клавіатура з дією "Пошук" (закриває клавіатуру).
 *
 * **Активний режим** (M3 "expanded search"): коли поле в фокусі або в ньому є запит, викликач ховає решту екрана й
 * показує лише видачу під полем — [onFocusChange] повідомляє про фокус, [active] переключає лупу на кнопку
 * "назад", яка очищає запит і закриває пошук (те саме робить системна "назад"). Фільтрацію робить викликач.
 */
@Composable
fun TeperaSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {}
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val closeSearch = {
        onQueryChange("")
        focusManager.clearFocus()
    }
    androidx.activity.compose.BackHandler(enabled = active) { closeSearch() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(28.dp))
            // Білий, як картки (cardActive): у світлій темі — білий, у темній — колір карток, а не сірий inputSurface.
            .background(TeperaPalette.cardActive)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (active) {
            TeperaIconButton(
                icon = TeperaSymbols.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
                onClick = closeSearch,
                modifier = Modifier.width(40.dp),
                shape = CircleShape,
                containerColor = Color.Transparent,
                contentColor = TeperaPalette.buttonBrandDark,
                height = 40.dp,
                iconSize = 24.dp
            )
        } else {
            Icon(TeperaSymbols.Search, contentDescription = null, tint = TeperaPalette.buttonBrandDark.copy(alpha = 0.75f))
        }
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChange(it.isFocused) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TeperaPalette.buttonBrandDark),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(TeperaPalette.buttonBrand),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focusManager.clearFocus() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TeperaPalette.buttonBrandDark.copy(alpha = 0.6f)
                        )
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            TeperaIconButton(
                icon = TeperaSymbols.Close,
                contentDescription = stringResource(R.string.search_clear),
                onClick = { onQueryChange("") },
                modifier = Modifier.width(40.dp),
                shape = CircleShape,
                containerColor = Color.Transparent,
                contentColor = TeperaPalette.buttonBrandDark,
                height = 40.dp,
                iconSize = 20.dp
            )
        }
    }
}

/**
 * Тихий вихід з усього ланцюжка онбордингу — за прямим запитом користувача. Кожен екран
 * онбордингу кладе цей контрол у правий верхній кут (поверх, не всередині вертикально
 * центрованої колонки контенту — інакше позиція "гуляла" б разом із центруванням різних екранів).
 * Приглушений колір і відсутність рамки/заливки — це не рівноцінна дія з "Продовжити"/"Пропустити"
 * конкретного кроку, а рідко потрібна втеча, тому візуально тихіша за решту кнопок.
 */
@Composable
fun OnboardingSkipAllButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.onboarding_skip_all),
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(12.dp),
        color = TeperaPalette.buttonBrandDark.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodyMedium
    )
}

/** Заголовок екрана онбордингу по центру — той самий стиль, що [TeperaScreenTitle] (Golos Medium 27sp, #003926). */
@Composable
fun TeperaOnboardingTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        color = TeperaPalette.buttonBrandDark,
        fontFamily = TeperaPalette.headlineFont,
        fontWeight = FontWeight.Medium,
        fontSize = 27.sp,
        lineHeight = 29.7.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

/**
 * Єдина картка застосунку (M3 "Filled" без тіні у фірмовому "скляному" виконанні): білий 80%,
 * радіус 16 (M3 large), внутрішній відступ 16 і проміжок 12 між блоками — крок 4/8dp за M3.
 * Заголовок — M3 titleMedium (16sp/24sp Medium, tracking 0.15) шрифтом застосунку, необов'язковий
 * підзаголовок — bodySmall приглушеним кольором. Вміст — колонка, тож блоки самі отримують проміжок.
 */
@Composable
fun TeperaCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (title != null || subtitle != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                title?.let {
                    Text(
                        text = it,
                        modifier = Modifier.semantics { heading() },
                        color = TeperaPalette.buttonBrandDark,
                        fontFamily = TeperaPalette.headlineFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        letterSpacing = 0.15.sp
                    )
                }
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f)
                    )
                }
            }
        }
        content()
    }
}

/**
 * Єдиний чіп "підпис значення" (M3 assist chip у фірмовому виконанні): тональний фон
 * [TeperaPalette.surfaceBrandLight] (#DCF6ED), повне заокруглення, висота від 32dp, горизонтальний
 * відступ 12; підпис — M3 labelLarge (14sp Medium) #006944, значення — те саме, але SemiBold #003926
 * (контраст підпису до фону ≈5.6:1, значення ≈11:1 — AA). Без значення — просто чіп-підпис.
 * Лише відображення (не натискається) — для дій є [TeperaButton].
 */
@Composable
fun TeperaChip(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    compact: Boolean = false, // 24dp заввишки, 12sp — для щільних списків (напр. значення в легенді картки дня)
    leading: (@Composable () -> Unit)? = null // необов'язковий елемент перед підписом (напр. кольорова крапка сегмента)
) {
    val labelStyle = androidx.compose.ui.text.TextStyle(
        fontFamily = TeperaPalette.headlineFont,
        fontWeight = FontWeight.Medium,
        fontSize = if (compact) 12.sp else 14.sp,
        lineHeight = if (compact) 16.sp else 20.sp,
        letterSpacing = 0.1.sp
    )
    Row(
        modifier = modifier
            .heightIn(min = if (compact) 24.dp else 32.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(TeperaPalette.chipSurface)
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Text(label, style = labelStyle, color = TeperaPalette.buttonBrand, maxLines = 1)
        value?.let {
            Text(it, style = labelStyle.copy(fontWeight = FontWeight.SemiBold), color = TeperaPalette.buttonBrandDark, maxLines = 1)
        }
    }
}

/**
 * Єдиний вигляд короткого пояснювального тексту (підказка під рядком/перемикачем, тіло
 * неруйнівного діалогу) — контурна ⓘ 16dp + bodySmall тим самим приглушеним кольором, що вже
 * використовує підзаголовок [TeperaCard] (`buttonBrandDark@70%`), замість дефолтного сірого
 * Material-кольору без іконки. Іконка вирівняна по першому рядку тексту, не по центру всього
 * блоку — інакше "пливе" вгору при двох і більше рядках.
 */
@Composable
fun TeperaHint(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            TeperaSymbols.Info,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
            tint = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f)
        )
    }
}

/**
 * Підсумок одним тональним блоком (замість двох окремих плиток, з яких одна на "Сьогодні" розтягувалась на
 * всю ширину майже порожньою): факти в рядок, розділені тонкою лінією; значення — першим і великим (28sp), підпис
 * під ним дрібніше — "число, а потім що це". Один факт займає блок зліва, без штучного розтягування.
 */
@Composable
fun TeperaStatsBar(stats: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier
            .fillMaxWidth()
            // Без тла, текст #003926 (за запитом користувача) — факти лежать прямо на градієнті сторінки.
            .padding(horizontal = 8.dp, vertical = 4.dp),
        content = {
            stats.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(TeperaPalette.buttonBrandDark.copy(alpha = 0.25f))
                    )
                }
                StatsBarColumn(label = label, value = value)
            }
        }
    ) { measurables, constraints ->
        // Діти чергуються: факт, розділювач, факт, ... Колонки ділять ширину порівну, але жодна не стає вужчою
        // за своє найдовше слово (minIntrinsicWidth) — інакше на вузьких екранах (Sony XZ1 Compact, 360dp)
        // підпис "Розблокувань учора" рвався посеред слова. Різницю віддають ширші за потребу колонки.
        val columns = measurables.filterIndexed { i, _ -> i % 2 == 0 }
        val dividers = measurables.filterIndexed { i, _ -> i % 2 == 1 }
        val dividerWidths = dividers.map { it.maxIntrinsicWidth(0) }
        val available = (constraints.maxWidth - dividerWidths.sum()).coerceAtLeast(0)
        val widths = statsBarColumnWidths(columns.map { it.minIntrinsicWidth(Int.MAX_VALUE) }, available)
        val placedColumns = columns.mapIndexed { i, m ->
            m.measure(androidx.compose.ui.unit.Constraints.fixedWidth(widths[i]))
        }
        // Top: значення різних фактів стоять на одному рівні, навіть коли підпис одного займає кілька рядків.
        val height = placedColumns.maxOfOrNull { it.height } ?: 0
        val placedDividers = dividers.mapIndexed { i, m ->
            m.measure(androidx.compose.ui.unit.Constraints.fixed(dividerWidths[i], height))
        }
        layout(constraints.maxWidth, height) {
            var x = 0
            placedColumns.forEachIndexed { i, column ->
                column.placeRelative(x, 0)
                x += column.width
                placedDividers.getOrNull(i)?.let { divider ->
                    divider.placeRelative(x, 0)
                    x += divider.width
                }
            }
        }
    }
}

/**
 * Ширини колонок [TeperaStatsBar]: порівну, але не менше за [minWidths] (найдовше слово колонки). Якщо мінімуми
 * разом не вміщаються в [available] — просто порівну (рвати слово тоді неминуче, хай хоч рівномірно).
 */
internal fun statsBarColumnWidths(minWidths: List<Int>, available: Int): List<Int> {
    val n = minWidths.size
    if (n == 0) return emptyList()
    if (minWidths.sum() > available) return List(n) { available / n }
    val widths = IntArray(n)
    val fixed = BooleanArray(n)
    var remaining = available
    var flexible = n
    // Колонки, яким рівної частки замало, отримують свій мінімум; решта ділить залишок — повторюємо, доки
    // рівна частка залишку не стане достатньою для всіх, що лишились.
    while (true) {
        val share = remaining / flexible
        var changed = false
        for (i in 0 until n) {
            if (!fixed[i] && minWidths[i] > share) {
                widths[i] = minWidths[i]
                fixed[i] = true
                remaining -= minWidths[i]
                flexible--
                changed = true
            }
        }
        if (!changed || flexible == 0) break
    }
    if (flexible > 0) {
        val share = remaining / flexible
        for (i in 0 until n) if (!fixed[i]) widths[i] = share
    }
    return widths.toList()
}

@Composable
private fun StatsBarColumn(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            color = TeperaPalette.buttonBrandDark,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TeperaPalette.buttonBrandDark
        )
    }
}

/**
 * Єдиний діалог застосунку (M3 basic dialog у фірмовому виконанні) — ЗАМІСТЬ `AlertDialog` з дефолтною
 * лілово-сірою поверхнею. Контейнер: тональний [TeperaPalette.surfaceBrandLight] (#DCF6ED, на ньому біла
 * Primary-кнопка дизайн-системи читається), радіус 28 (M3 extraLarge), відступ 24, проміжок 16.
 * Заголовок — Golos Medium 22/28 #003926, текст — M3 bodyMedium #003926 @85%. Дії — в один рядок на всю
 * ширину: [dismissText] — Secondary (рамка), [confirmText] — Primary; без [dismissText] (інформаційний
 * діалог) — одна широка Primary-кнопка. Кнопки — лише [TeperaButton], без ручного стилювання.
 * [content] — довільне тіло (поля, списки) під [text].
 */
@Composable
fun TeperaDialog(
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
    confirmEnabled: Boolean = true,
    content: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(TeperaPalette.dialogSurface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    modifier = Modifier.semantics { heading() },
                    color = TeperaPalette.buttonBrandDark,
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                text?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
                    )
                }
                content?.invoke(this)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (dismissText != null) {
                    TeperaButton(
                        text = dismissText,
                        onClick = onDismiss,
                        type = TeperaButtonType.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                TeperaButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Висота, яку плаваюча "таблетка" нижнього навбару перекриває знизу екрана: сама таблетка (62dp) + відступи (8+8) +
 * системна навігаційна панель + запас 16dp. Раніше скрізь стояло фіксоване 100dp, і на пристроях з високою
 * 3-кнопковою панеллю (Huawei P9) нижні елементи залишались під таблеткою — WCAG 2.4.11 (Focus Not Obscured).
 */
@Composable
fun bottomNavClearance(): Dp =
    62.dp + 16.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars
        .asPaddingValues().calculateBottomPadding() + 24.dp

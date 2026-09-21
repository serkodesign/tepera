package com.serkodesign.tepera.ui.theme

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(TeperaPalette.cardTranslucent)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 24.sp),
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

/** Заголовок секції списку ("Активні", "Архівовано", "Excluded", "All" тощо). */
@Composable
fun GlassSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp),
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp)
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
            .background(TeperaPalette.cardTranslucent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier = modifier
    )
}

/** Кольори перемикача: увімкнений — фірмовий темно-зелений (як на toggle track у фреймі), вимкнений — за Material 3 (суцільний трек, рамка й ручка кольору outline). */
@Composable
fun teperaSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = TeperaPalette.brandAccent,
    checkedThumbColor = Color.White,
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
                .background(TeperaPalette.cardActive)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEach { (value, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
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
    maxHours: Int = 8
) {
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
            Text(valueLabel(hours), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

enum class TeperaButtonSize(val height: Dp, val textSize: TextUnit, val fontWeight: FontWeight) {
    Big(108.dp, 14.sp, FontWeight.Medium),
    Medium(48.dp, 14.sp, FontWeight.Medium),
    Small(32.dp, 12.sp, FontWeight.Normal)
}

enum class TeperaButtonType { Primary, Secondary, Tertiary }

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
    lineHeightOverride: TextUnit? = null
) {
    val big = size == TeperaButtonSize.Big
    val shape = RoundedCornerShape(if (big && type == TeperaButtonType.Primary) 54.dp else 24.dp)

    val background: Color = when (type) {
        TeperaButtonType.Primary -> when {
            big && enabled -> TeperaPalette.buttonBrand
            big -> TeperaPalette.buttonBrandDark
            else -> Color.White
        }
        else -> Color.Transparent
    }
    val contentColor: Color = when {
        type == TeperaButtonType.Primary && big -> Color.White
        type == TeperaButtonType.Primary -> TeperaPalette.buttonBrand
        else -> TeperaPalette.buttonBrandDark
    }

    Box(
        modifier = modifier
            .height(size.height)
            .alpha(if (enabled) 1f else 0.5f)
            .then(
                when (type) {
                    // Figma: Primary — drop-shadow 0 0 12 @5%, Secondary — 0 0 24 @5%; у Tertiary тіні нема
                    // (нема що відкидати — без фону й рамки).
                    TeperaButtonType.Primary ->
                        Modifier.shadow(6.dp, shape, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f))
                    TeperaButtonType.Secondary ->
                        Modifier.shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f))
                    TeperaButtonType.Tertiary -> Modifier
                }
            )
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColorOverride ?: contentColor,
            fontSize = textSizeOverride ?: size.textSize,
            lineHeight = lineHeightOverride ?: TextUnit.Unspecified,
            letterSpacing = if (textSizeOverride != null) 0.sp else TextUnit.Unspecified,
            fontWeight = size.fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    containerColor: Color = Color.White,
    contentColor: Color = Color.Black,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.5f)
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
            .background(Color.White.copy(alpha = 0.8f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (title != null || subtitle != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                title?.let {
                    Text(
                        text = it,
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
    value: String? = null
) {
    val labelStyle = androidx.compose.ui.text.TextStyle(
        fontFamily = TeperaPalette.headlineFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(TeperaPalette.surfaceBrandLight)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = labelStyle, color = TeperaPalette.buttonBrand, maxLines = 1)
        value?.let {
            Text(it, style = labelStyle.copy(fontWeight = FontWeight.SemiBold), color = TeperaPalette.buttonBrandDark, maxLines = 1)
        }
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
                .background(TeperaPalette.surfaceBrandLight)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            title?.let {
                Text(
                    text = it,
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

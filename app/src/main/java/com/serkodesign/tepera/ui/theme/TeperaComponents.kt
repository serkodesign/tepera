package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

/** Круглий напівпрозорий back-button + заголовок — замінює TopAppBar на цих екранах. */
@Composable
fun GlassScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 24.sp)
        )
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

/** Кольори перемикача, узгоджені з фірмовим темно-зеленим (той самий, що на toggle track у фреймі). */
@Composable
fun teperaSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = TeperaPalette.brandAccent,
    checkedThumbColor = Color.White,
    checkedBorderColor = Color.Transparent,
    uncheckedTrackColor = TeperaPalette.switchTrackOff,
    uncheckedThumbColor = Color.White,
    uncheckedBorderColor = Color.Transparent
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(TeperaPalette.cardTranslucent)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .then(if (isSelected) Modifier.background(TeperaPalette.cardActive) else Modifier)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
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
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onHoursChange(hoursFromFraction(change.position.x / size.width.toFloat()))
                }
            }
    ) {
        val fraction = (hours.toFloat() / maxHours).coerceIn(0.05f, 1f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(100.dp))
                .background(TeperaPalette.cardActive),
            contentAlignment = Alignment.Center
        ) {
            Text(valueLabel(hours), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

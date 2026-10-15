package com.serkodesign.tepera.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.PI
import kotlin.math.sin

/**
 * Hero-картка Home FINAL (docs/HANDOFF.md, розділи 3-5; Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, `HomeHero`
 * 678:12959, сторінка UI design). Заголовок (дата + книга/шестерня), орб "без телефону" з рівнем води,
 * статистика (Online + активності за сьогодні, "Ще N" розгортається на місці), смуга порядку дня й слот
 * [teaser] для тізерів "Вчора / Тиждень" (етап 4). Кольори — лише семантичні ролі TeperaPalette (токени hero).
 */
@Composable
fun HomeHero(
    state: HeroUiState,
    onOpenSettings: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
    teaser: (@Composable () -> Unit)? = null
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    val glowColor = Color(0xFF3BB58A)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TeperaPalette.heroSurface)
            .border(1.dp, TeperaPalette.heroBorder, shape)
            // Світіння у верхньому правому куті (Figma "glow", 240dp, центр ≈ 50dp від правого краю й 29dp від верху).
            .drawBehind {
                val center = Offset(size.width - 50.dp.toPx(), 29.dp.toPx())
                drawCircle(
                    brush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.28f), Color.Transparent), center, 170.dp.toPx()),
                    radius = 170.dp.toPx(),
                    center = center
                )
            }
            .animateContentSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroHeader(onOpenSettings = onOpenSettings, onOpenKnowledgeBase = onOpenKnowledgeBase)
        when (state) {
            HeroUiState.Loading -> HeroLoading()
            HeroUiState.NoAccess -> HeroNoAccess(onGrantAccess)
            is HeroUiState.Empty -> {
                HeroEmpty(state)
                DayStructureBar(state.structure)
                teaser?.invoke()
            }
            is HeroUiState.Data -> {
                HeroData(state, expanded = expanded, onToggleExpanded = { expanded = !expanded })
                DayStructureBar(state.structure)
                teaser?.invoke()
            }
        }
    }
}

/**
 * Збирає [HeroUiState] з [BalanceUiState]: назви/кольори категорій, шари смуги дня (вісь 06:00–24:00 поточного
 * календарного дня). Композабл, бо [categoryDisplayName] читає ресурси.
 */
@Composable
fun rememberHeroState(state: BalanceUiState): HeroUiState {
    val activities = state.categorySegments.map {
        HeroActivity(categoryDisplayName(it.category), categoryColor(it.category.colorHex), it.minutes)
    }
    val restColor = TeperaPalette.restOfDayCard
    val onlineColor = TeperaPalette.onlineCard
    val futureColor = TeperaPalette.heroTrack
    return remember(state, activities, restColor, onlineColor, futureColor) {
        val structure = if (state.hasUsageAccess == true) {
            buildDayStructure(
                onlineMinutes = state.onlineMinutes,
                // Порядок категорій на смузі — за sortOrder (як у колишній шкалі "Твій день"), не за тривалістю.
                categories = activities,
                restOfDayMinutes = state.restOfDayMinutes,
                dayLengthMinutes = state.dayLengthMinutes,
                daySpanMinutes = state.daySpanMinutes,
                targetMinutes = state.targetMinutes,
                onlineColor = onlineColor,
                restColor = restColor,
                futureColor = futureColor
            )
        } else {
            DayStructure.Empty
        }
        buildHeroState(
            hasUsageAccess = state.hasUsageAccess,
            dayStartMillis = state.dayStartMillis,
            elapsedMinutes = state.elapsedMinutes,
            onlineMinutes = state.onlineMinutes,
            activities = activities,
            structure = structure
        )
    }
}

@Composable
private fun HeroText(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    alpha: Float = 1f,
    color: Color = TeperaPalette.textOnDark,
    lineHeight: Float = 1.1f,
    textAlign: TextAlign? = null,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.copy(alpha = color.alpha * alpha),
        fontFamily = TeperaPalette.headlineFont,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * lineHeight).sp,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun HeroHeader(onOpenSettings: () -> Unit, onOpenKnowledgeBase: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val locale = LocalConfiguration.current.locales[0]
        val todayText = remember(locale) { SimpleDateFormat("d MMMM", locale).format(Date()) }
        Column {
            HeroText(stringResource(R.string.home_header_subtitle), size = 12, alpha = 0.8f, lineHeight = 1.3f)
            HeroText(
                todayText, size = 27, weight = FontWeight.Medium,
                modifier = Modifier.semantics { heading() }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TeperaIconButton(
                icon = TeperaIcons.Book,
                contentDescription = stringResource(R.string.settings_knowledge_base_action),
                onClick = onOpenKnowledgeBase,
                shape = CircleShape,
                containerColor = TeperaPalette.heroTrack,
                contentColor = TeperaPalette.textOnDark
            )
            TeperaIconButton(
                icon = TeperaIcons.Settings,
                contentDescription = stringResource(R.string.settings_nav_action),
                onClick = onOpenSettings,
                shape = CircleShape,
                containerColor = TeperaPalette.heroTrack,
                contentColor = TeperaPalette.textOnDark
            )
        }
    }
}

// --- Стани -----------------------------------------------------------------------------------------------

@Composable
private fun HeroData(state: HeroUiState.Data, expanded: Boolean, onToggleExpanded: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroOrb(level = state.waterLevel, contentTopPadding = 32.dp) {
            val onWater = state.waterLevel >= 0.55f
            val textColor = if (onWater) TeperaPalette.heroOnLight else TeperaPalette.textOnDark
            HeroText(stringResource(R.string.hero_orb_label), size = 11, color = textColor)
            HeroText(heroDuration(state.offlineMinutes), size = 18, weight = FontWeight.Medium, color = textColor)
            HeroText(stringResource(R.string.hero_orb_percent_format, state.percent), size = 11, alpha = 0.85f, color = textColor)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ElapsedBlock(state.dayStartMillis, state.elapsedMinutes)
            Spacer(Modifier.height(6.dp))
            HeroStatRow(stringResource(R.string.balance_online_label), TeperaPalette.onlineCard, heroDuration(state.onlineMinutes))
            if (!expanded) {
                state.visibleActivities.forEach { HeroStatRow(it.name, it.color, heroDuration(it.minutes)) }
            }
            if (state.isMany) {
                HeroMorePill(
                    expanded = expanded,
                    label = if (expanded) stringResource(R.string.hero_collapse) else stringResource(R.string.hero_more_format, state.hiddenActivities.size),
                    dotColors = if (expanded) emptyList() else state.hiddenActivities.take(5).map { it.color },
                    onClick = onToggleExpanded
                )
            }
        }
    }
    if (state.isMany && expanded) AllActivities(state)
}

@Composable
private fun AllActivities(state: HeroUiState.Data) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HeroText(stringResource(R.string.hero_all_activities), size = 11, alpha = 0.8f)
            HeroText(
                stringResource(R.string.hero_named_format, heroDuration(state.namedMinutes)),
                size = 11, alpha = 0.8f
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val columns = listOf(state.activities.take(4), state.activities.drop(4))
            columns.forEach { column ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    column.forEach { HeroStatRow(it.name, it.color, heroDuration(it.minutes)) }
                }
            }
        }
    }
}

@Composable
private fun HeroEmpty(state: HeroUiState.Empty) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroOrb(level = 0f, contentTopPadding = 16.dp) {
            HeroText(stringResource(R.string.hero_orb_label), size = 11, alpha = 0.8f)
            HeroText("—", size = 18, weight = FontWeight.Medium)
            HeroText(stringResource(R.string.hero_orb_no_data), size = 11, alpha = 0.7f)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ElapsedBlock(state.dayStartMillis, state.elapsedMinutes)
            HeroText(stringResource(R.string.hero_empty_hint), size = 12, alpha = 0.85f, lineHeight = 1.3f, maxLines = 4)
        }
    }
}

@Composable
private fun HeroNoAccess(onGrantAccess: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroOrb(level = 0f, dashed = true) {
            HeroText(stringResource(R.string.hero_orb_no_access), size = 14, weight = FontWeight.Medium, alpha = 0.8f)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HeroText(stringResource(R.string.hero_no_access_title), size = 18, weight = FontWeight.Medium, maxLines = 2)
            HeroText(stringResource(R.string.hero_no_access_body), size = 12, alpha = 0.85f, lineHeight = 1.3f, maxLines = 6)
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .clickable(role = Role.Button, onClick = onGrantAccess)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                HeroText(stringResource(R.string.hero_grant_access), size = 14, weight = FontWeight.Medium, color = TeperaPalette.heroOnLight)
            }
        }
    }
}

@Composable
private fun HeroLoading() {
    val track = TeperaPalette.heroTrack
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(track))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(72.dp).height(11.dp).clip(RoundedCornerShape(6.dp)).background(track))
            Box(Modifier.fillMaxWidth().height(27.dp).clip(RoundedCornerShape(8.dp)).background(track))
            repeat(3) { Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(track)) }
        }
    }
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(track))
    Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(track))
}

// --- Частини ---------------------------------------------------------------------------------------------

@Composable
private fun ElapsedBlock(dayStartMillis: Long, elapsedMinutes: Int) {
    val locale = LocalConfiguration.current.locales[0]
    val startText = remember(dayStartMillis, locale) { SimpleDateFormat("HH:mm", locale).format(Date(dayStartMillis)) }
    HeroText(stringResource(R.string.hero_since_format, startText), size = 11, alpha = 0.8f)
    HeroText(
        stringResource(R.string.hours_minutes_short_format, elapsedMinutes / 60, elapsedMinutes % 60),
        size = 27, weight = FontWeight.Medium
    )
}

/** Тривалість без заокруглення до чверті години (на відміну від карток): "2 год 10 хв", "50 хв", "3 год". */
@Composable
internal fun heroDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, rest)
        rest == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, rest)
    }
}

@Composable
private fun HeroStatRow(label: String, dot: Color, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            HeroText(label, size = 12, alpha = 0.9f, lineHeight = 1.3f)
        }
        HeroText(value, size = 14, weight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun HeroMorePill(expanded: Boolean, label: String, dotColors: List<Color>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TeperaPalette.heroTrack)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HeroText(label, size = 12, alpha = 0.95f, lineHeight = 1.3f)
            dotColors.forEach { Box(Modifier.size(6.dp).clip(CircleShape).background(it)) }
        }
        Icon(
            imageVector = TeperaSymbols.ChevronRight,
            contentDescription = null,
            tint = TeperaPalette.textOnDark,
            modifier = Modifier.size(12.dp).rotate(if (expanded) -90f else 90f)
        )
    }
}

/**
 * Орб 150dp: коло-основа, "вода" (м'ятна, рівень = [level]) з двома хвилями, контур. Вміст ([content]) —
 * по центру. Без води при [level] = 0; [dashed] — пунктирний контур (немає доступу).
 */
@Composable
private fun HeroOrb(
    level: Float,
    dashed: Boolean = false,
    // Figma: блок тексту стоїть нижче геометричного центру (Data: 70–123 з 150, Empty: 58–113) — компенсація зверху.
    contentTopPadding: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val track = TeperaPalette.heroTrack
    val water = TeperaPalette.restOfDayCard
    val outline = TeperaPalette.textOnDark.copy(alpha = if (dashed) 0.5f else 0.4f)
    // Озвучується одним елементом: тексти всередині орба ("без телефону", значення, відсоток) читаються підряд.
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(120.dp)) {
            drawCircle(track)
            if (level > 0f) {
                val top = size.height * (1f - level)
                fun wave(amplitude: Float, period: Float, phase: Float, offsetY: Float, color: Color) {
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        var x = 0f
                        lineTo(0f, top + offsetY + amplitude * sin(phase))
                        while (x <= size.width) {
                            lineTo(x, top + offsetY + amplitude * sin(2f * PI.toFloat() * x / period + phase))
                            x += 2f
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path, color)
                }
                wave(4.dp.toPx(), 88.dp.toPx(), 0.8f, -3.dp.toPx(), water.copy(alpha = 0.5f))
                wave(3.dp.toPx(), 72.dp.toPx(), 2.4f, 0f, water)
            }
            val stroke = 1.5.dp.toPx()
            drawCircle(
                color = outline,
                radius = size.minDimension / 2f - stroke / 2f,
                style = Stroke(
                    width = stroke,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null
                )
            )
        }
        Column(
            modifier = Modifier.padding(top = contentTopPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            content()
        }
    }
}

/**
 * Смуга структури доби — вужча версія колишньої шкали "Твій день": пігулка-доріжка 14dp, всередині сегменти-пігулки
 * (Online, категорії, "Решта дня") з проміжками 1dp, заштрихована частина ще не прожитого часу й тиха засічка
 * орієнтиру БЕЗ підпису (FR-3.10: ніколи не змінює колір при перевищенні). Орієнтир — жовта лінія 2dp із
 * прозорою жовтою заливкою від початку шкали до неї, як раніше.
 */
@Composable
private fun DayStructureBar(structure: DayStructure) {
    val track = TeperaPalette.heroTrack
    val stripe = TeperaPalette.textOnDark.copy(alpha = 0.14f)
    val targetLine = TeperaPalette.barTargetLine
    val targetFill = TeperaPalette.barTargetFill
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val fullRadius = CornerRadius(size.height / 2f)
        drawRoundRect(track, size = size, cornerRadius = fullRadius)

        val inset = 2.dp.toPx()
        val innerHeight = size.height - 2 * inset
        val gap = 0.5.dp.toPx() // по 0.5dp з кожного боку = 1dp між сусідніми сегментами
        val innerLeft = inset
        val innerWidth = size.width - 2 * inset

        // Заливка орієнтиру — за сегментами, на всю висоту доріжки.
        structure.targetFraction?.let { fraction ->
            val x = innerLeft + innerWidth * fraction
            drawRoundRect(
                targetFill,
                topLeft = Offset(0f, 0f),
                size = Size(x.coerceAtLeast(0f), size.height),
                cornerRadius = fullRadius
            )
        }

        val segmentRadius = CornerRadius(innerHeight / 2f)
        structure.segments.forEach { segment ->
            val left = innerLeft + segment.start * innerWidth + gap
            val right = innerLeft + segment.end * innerWidth - gap
            val width = (right - left).coerceAtLeast(1.dp.toPx())
            val topLeft = Offset(left, inset)
            val segmentSize = Size(width, innerHeight)
            if (segment.hatched) {
                // Ще не прожитий час: доріжка зі штрихами під 45°, обрізана по сегменту.
                drawRoundRect(segment.color, topLeft, segmentSize, segmentRadius)
                val path = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(left, inset, left + width, inset + innerHeight, segmentRadius))
                }
                clipPath(path) {
                    val step = 5.dp.toPx()
                    var x = left - innerHeight
                    while (x < left + width) {
                        drawLine(stripe, Offset(x, inset + innerHeight), Offset(x + innerHeight, inset), strokeWidth = 1.5.dp.toPx())
                        x += step
                    }
                }
            } else {
                drawRoundRect(segment.color, topLeft, segmentSize, segmentRadius)
            }
        }

        // Засічка орієнтиру: тонка лінія без підпису поверх сегментів.
        structure.targetFraction?.let { fraction ->
            val x = innerLeft + innerWidth * fraction
            val lineWidth = 2.dp.toPx()
            drawLine(targetLine, Offset(x - lineWidth / 2f, 0f), Offset(x - lineWidth / 2f, size.height), strokeWidth = lineWidth)
        }
    }
}

package com.serkodesign.tepera.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.atan2
import kotlin.math.sqrt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import com.serkodesign.tepera.ui.theme.IntroPlayed
import com.serkodesign.tepera.ui.theme.extraAnimationsEnabled
import com.serkodesign.tepera.ui.theme.rememberCountedInt
import com.serkodesign.tepera.ui.theme.rememberWavePhase
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
    val glowDark = Color(0xFFFDFFD2)
    val glowGreen = Color(0xFF65FF93)
    val darkTheme = com.serkodesign.tepera.ui.theme.LocalTeperaColors.current.isDark
    // Заголовок (дата + кнопки) стоїть НАД карткою (Figma 721:23245): відступи 24/16 від країв екрана — 16/8 тут, бо
    // зовнішній `modifier` уже дає 8dp з боків.
    Column(modifier = modifier.fillMaxWidth()) {
    HeroHeader(
        onOpenSettings = onOpenSettings,
        onOpenKnowledgeBase = onOpenKnowledgeBase,
        modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 16.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TeperaPalette.heroSurface)
            .border(1.dp, TeperaPalette.heroBorder, shape)
            // Світіння у верхньому правому куті (Figma "glow", 240dp, центр ≈ 50dp від правого краю й 29dp від верху).
            .drawBehind {
                val center = Offset(size.width - 50.dp.toPx(), 29.dp.toPx())
                if (darkTheme) {
                    // Темна тема, Figma 721:23488: коло r=120, #FDFFD2 @30%, розмиття σ≈79 → ядро гауса ≈ 0.68 у центрі, ~0.45 на
                    // краю кола й затухання до нуля на ~280dp. Наближено радіальним градієнтом із цими опорними точками.
                    // Жовта пляма трохи менша за зелену нижню: крутіше затухання й радіус 220dp (за запитом власника).
                    val radius = 220.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to glowDark.copy(alpha = 0.17f),
                            0.35f to glowDark.copy(alpha = 0.11f),
                            0.6f to glowDark.copy(alpha = 0.05f),
                            0.82f to glowDark.copy(alpha = 0.012f),
                            1f to Color.Transparent,
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    // Друга пляма — зелена, знизу (Figma 721:23868): еліпс rx≈190, ry=120, #65FF93 @30%, те саме розмиття; центр
                    // на 123dp від лівого краю й на 113dp НИЖЧЕ нижнього краю картки — у картку заходить лише її верхній край.
                    val greenCenter = Offset(123.5.dp.toPx(), size.height + 113.dp.toPx())
                    scale(scaleX = 190.5f / 120f, scaleY = 1f, pivot = greenCenter) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to glowGreen.copy(alpha = 0.205f),
                                0.43f to glowGreen.copy(alpha = 0.135f),
                                0.71f to glowGreen.copy(alpha = 0.045f),
                                1f to Color.Transparent,
                                center = greenCenter,
                                radius = radius
                            ),
                            radius = radius,
                            center = greenCenter
                        )
                    }
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.28f), Color.Transparent), center, 170.dp.toPx()),
                        radius = 170.dp.toPx(),
                        center = center
                    )
                }
            }
            .animateContentSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            HeroUiState.Loading -> HeroLoading()
            HeroUiState.NoAccess -> HeroNoAccess(onGrantAccess)
            is HeroUiState.Empty -> {
                HeroEmpty(state)
                DayStructureRow(state.structure)
                teaser?.invoke()
            }
            is HeroUiState.Data -> {
                HeroData(state, expanded = expanded, onToggleExpanded = { expanded = !expanded })
                DayStructureRow(state.structure)
                teaser?.invoke()
            }
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
private fun HeroHeader(onOpenSettings: () -> Unit, onOpenKnowledgeBase: () -> Unit, modifier: Modifier = Modifier) {
    // Заголовок лежить на фоні екрана, не на картці: у темній темі — білий текст і кнопки hero/track (Figma 721:23828),
    // у світлій — темно-зелений текст (hero/on-light) і ледь помітна темна заливка кнопок.
    val dark = com.serkodesign.tepera.ui.theme.LocalTeperaColors.current.isDark
    val onPage = if (dark) TeperaPalette.textOnDark else TeperaPalette.heroOnLight
    val buttonBg = if (dark) TeperaPalette.heroTrack else TeperaPalette.heroOnLight.copy(alpha = 0.08f)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val locale = LocalConfiguration.current.locales[0]
        val todayText = remember(locale) { SimpleDateFormat("d MMMM", locale).format(Date()) }
        Column {
            HeroText(stringResource(R.string.home_header_subtitle), size = 12, alpha = 0.8f, lineHeight = 1.3f, color = onPage)
            HeroText(
                todayText, size = 27, weight = FontWeight.Medium, color = onPage,
                modifier = Modifier.semantics { heading() }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TeperaIconButton(
                icon = TeperaIcons.Book,
                contentDescription = stringResource(R.string.settings_knowledge_base_action),
                onClick = onOpenKnowledgeBase,
                shape = CircleShape,
                containerColor = buttonBg,
                contentColor = onPage
            )
            TeperaIconButton(
                icon = TeperaIcons.Settings,
                contentDescription = stringResource(R.string.settings_nav_action),
                onClick = onOpenSettings,
                shape = CircleShape,
                containerColor = buttonBg,
                contentColor = onPage
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
        verticalAlignment = Alignment.Top
    ) {
        HeroOrb(level = state.waterLevel, contentTopPadding = 32.dp) {
            val onWater = state.waterLevel >= 0.55f
            val textColor = if (onWater) TeperaPalette.heroOnLight else TeperaPalette.textOnDark
            HeroText(stringResource(R.string.hero_orb_label), size = 11, color = textColor)
            HeroText(heroDuration(rememberCountedInt(state.offlineMinutes)), size = 18, weight = FontWeight.Medium, color = textColor)
            HeroText(stringResource(R.string.hero_orb_percent_format, rememberCountedInt(state.percent)), size = 11, alpha = 0.85f, color = textColor)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ElapsedBlock(state.dayStartMillis, state.elapsedMinutes)
            Spacer(Modifier.height(6.dp))
            HeroStatRow(stringResource(R.string.balance_online_label), TeperaPalette.onlineCard, heroDuration(rememberCountedInt(state.onlineMinutes)))
            if (!expanded) {
                state.visibleActivities.forEach { HeroStatRow(it.name, it.color, heroDuration(rememberCountedInt(it.minutes))) }
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
                    column.forEach { HeroStatRow(it.name, it.color, heroDuration(rememberCountedInt(it.minutes))) }
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
    val shownElapsed = rememberCountedInt(elapsedMinutes)
    HeroText(stringResource(R.string.hero_since_format, startText), size = 11, alpha = 0.8f)
    HeroText(
        stringResource(R.string.hours_minutes_short_format, shownElapsed / 60, shownElapsed % 60),
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
    // Рідина "тече": фази двох хвиль плавно біжать у протилежні боки (цикл 2π — без шва). Значення читається лише в
    // draw-фазі Canvas, тож перекомпозиції немає; при вимкненій шкалі анімацій Compose сам зупиняє рух.
    // Тумблер "Додаткові анімації" (і системна шкала): вимкнено → рівна нерухома вода, без датчика, без сплесків.
    val motion = extraAnimationsEnabled()
    val phaseA by rememberWavePhase(motion, 12000)
    val phaseB by rememberWavePhase(motion, 18000)
    // Реакції рідини: рівень плавно піднімається (вперше за сесію — з нуля), кола від тапу (з місця дотику) і
    // м'який сплеск від струсу телефону (з центру).
    val levelAnim = remember { Animatable(if (motion && !IntroPlayed.orb) 0f else level) }
    LaunchedEffect(level, motion) {
        if (motion) {
            levelAnim.animateTo(level, tween(if (IntroPlayed.orb) 1500 else 2200, easing = FastOutSlowInEasing))
            IntroPlayed.orb = true
        } else {
            levelAnim.snapTo(level)
        }
    }
    val splash = remember { Animatable(1f) } // 0 → 1: прогрес поточного сплеску (1 = спокій)
    var splashStrength by remember { mutableFloatStateOf(0f) }
    var splashAt by remember { mutableFloatStateOf(0.5f) } // горизонтальна частка 0..1
    var shakeCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(shakeCount) {
        if (shakeCount > 0) {
            splashStrength = 0.6f
            splashAt = 0.5f
            splash.snapTo(0f)
            splash.animateTo(1f, tween(2600, easing = LinearEasing))
        }
    }
    // Нахил телефону: вода прагне лишатись горизонтальною у світі — поверхню повертаємо проти нахилу корпусу (обсяг
    // води зберігається, бо коло симетричне). Кут рухається за пружиною з легким недодемпфуванням: вода хитається
    // й заспокоюється. Акселерометр слухаємо лише поки Home на екрані; вимкнена шкала анімацій → датчик не підписуємо.
    val context = LocalContext.current
    var gravX by remember { mutableFloatStateOf(0f) } // відфільтрований вектор "вгору" у площині екрана, м/с²
    var gravY by remember { mutableFloatStateOf(SensorManager.GRAVITY_EARTH) }
    var tiltAngle by remember { mutableFloatStateOf(0f) } // рад, + = за годинниковою (ліва сторона телефону вниз)
    LifecycleResumeEffect(motion) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShake = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                gravX += (event.values[0] - gravX) * 0.104f
                gravY += (event.values[1] - gravY) * 0.104f
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val now = System.currentTimeMillis()
                if (abs(sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH) > 7f && now - lastShake > 1500L) {
                    lastShake = now
                    shakeCount++
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (motion && manager != null && sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onPauseOrDispose { manager?.unregisterListener(listener) }
    }
    LaunchedEffect(motion) {
        if (!motion) { tiltAngle = 0f; return@LaunchedEffect }
        var velocity = 0f
        var last = 0L
        val limit = 35f * PI.toFloat() / 180f
        while (true) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            // Телефон лежить плазом (g майже перпендикулярна екрану) — кут у площині не визначений, повертаємось до 0.
            val inPlane = sqrt(gravX * gravX + gravY * gravY)
            val target = if (inPlane < 3f) 0f else (atan2(gravX, gravY) * 0.5f).coerceIn(-limit, limit)
            // В'язка рідина: пружина з легким перетисканням демпфування (ζ≈1.04) — повільно й без хитання «тягнеться» до рівня.
            // Швидкість відгуку +30% (ω ×1.3: жорсткість ×1.69, демпфування ×1.3 — ζ незмінне).
            velocity += (11.83f * (target - tiltAngle) - 7.15f * velocity) * dt
            tiltAngle += velocity * dt
        }
    }
    // Озвучується одним елементом: тексти всередині орба ("без телефону", значення, відсоток) читаються підряд.
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            Modifier.size(120.dp).pointerInput(motion) {
                if (motion) detectTapGestures { pos ->
                    splashStrength = 1f
                    splashAt = (pos.x / size.width).coerceIn(0f, 1f)
                    scope.launch {
                        splash.snapTo(0f)
                        splash.animateTo(1f, tween(2600, easing = LinearEasing))
                    }
                }
            }
        ) {
            drawCircle(track)
            if (levelAnim.value > 0.001f) {
                val top = size.height * (1f - levelAnim.value)
                val p = splash.value
                val ripple = { x: Float ->
                    if (p >= 1f) 0f else {
                        val d = abs(x - splashAt * size.width)
                        splashStrength * (1f - p) * 5.dp.toPx() * sin(2f * PI.toFloat() * (d / 40.dp.toPx() - p * 2f)) * exp(-d / 70.dp.toPx())
                    }
                }
                fun wave(amplitude: Float, period: Float, phase: Float, offsetY: Float, color: Color) {
                    val path = Path().apply {
                        moveTo(-size.width, 2f * size.height)
                        var x = -size.width
                        lineTo(-size.width, top + offsetY + ripple(0f) + amplitude * sin(phase))
                        while (x <= 2f * size.width) {
                            lineTo(x, top + offsetY + ripple(x) + amplitude * sin(2f * PI.toFloat() * x / period + phase))
                            x += 2f
                        }
                        lineTo(2f * size.width, 2f * size.height)
                        close()
                    }
                    drawPath(path, color)
                }
                rotate(tiltAngle * 180f / PI.toFloat(), pivot = center) {
                wave(4.dp.toPx(), 88.dp.toPx(), 0.8f - phaseB, -3.dp.toPx(), water.copy(alpha = 0.5f))
                wave(3.dp.toPx(), 72.dp.toPx(), 2.4f + phaseA, 0f, water)
                }
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
 * Смуга структури доби + ⓘ праворуч: тап відкриває пояснення, що показують сегменти, штрихи й тиха риска орієнтира
 * (сама риска лишається без підпису — FR-3.10). Без орієнтира пояснення не згадує риску.
 */
@Composable
private fun DayStructureRow(structure: DayStructure) {
    var showInfo by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { DayStructureBar(structure) }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { showInfo = true }, modifier = Modifier.size(20.dp)) {
            Icon(
                TeperaSymbols.Info,
                contentDescription = stringResource(R.string.hero_structure_info_action),
                tint = TeperaPalette.textOnDark.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
    if (showInfo) {
        HomeInfoDialog(
            title = stringResource(R.string.hero_structure_info_title),
            body = stringResource(
                if (structure.targetFraction != null) R.string.hero_structure_info_body_target else R.string.hero_structure_info_body
            ),
            onDismiss = { showInfo = false }
        )
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

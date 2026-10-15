package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryGlyphColor
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols

/**
 * Список активностей Home FINAL (docs/HANDOFF.md, розділ 6; Figma "App concept", `ActivityRow` 677:12555,
 * `SectionHeader` 679:12929): горизонтальні рядки замість сітки карток 2×N.
 *
 * - Idle: скляний фон + контур, плитка (акцент категорії @30%), "Сьогодні · час", кружки "додати час" і "старт".
 * - Active (таймер іде): суцільний `card/active`, плитка `card/on-active` @30%, "● Триває · час", одна кнопка "пауза".
 * - Pressed: накладка `state/pressed` (Idle) / `state/pressed-on-primary` (Active) на весь рядок.
 * - Нуль часу — "—" кольором `text/secondary` (без "0 хв", без цілей — "без тиску").
 */
@Composable
fun ActivitiesSectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            color = TeperaPalette.buttonBrandDark,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 24.2.sp
        )
        val colors = TeperaPalette.colors
        // Візуально 32dp (Figma), але область дотику — не менше 48dp (HANDOFF, п. 10.4).
        Box(
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onAction)
                .minimumInteractiveComponentSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (colors.isDark) colors.listItem else Color(0x80FFFFFF))
                    .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(TeperaSymbols.Add, contentDescription = null, tint = TeperaPalette.buttonBrand, modifier = Modifier.size(18.dp))
                Text(
                    text = actionLabel,
                    color = TeperaPalette.buttonBrand,
                    fontFamily = TeperaPalette.headlineFont,
                    fontSize = 12.sp,
                    lineHeight = 15.6.sp
                )
            }
        }
    }
}

@Composable
fun ActivityRow(
    item: CategoryTodaySummary,
    onToggleTimer: () -> Unit,
    onAddTime: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = item.trackingStartTime != null
    val accent = categoryColor(item.category.colorHex)
    val name = categoryDisplayName(item.category)
    val shape = RoundedCornerShape(24.dp)

    val onActive = TeperaPalette.onPrimary
    val containerColor = if (isActive) TeperaPalette.activityCardActive else TeperaPalette.activityCardIdle
    val nameColor = if (isActive) onActive else TeperaPalette.textPrimary
    // Плитка простою — ті самі кольори, що в Щоденнику й Налаштуваннях категорій: колір категорії @20% і гліф
    // `categoryGlyphColor` (колір категорії, затемнений/освітлений до читабельного контрасту). Форма — нова (квадрат 16).
    val tileColor = if (isActive) onActive.copy(alpha = 0.3f) else accent.copy(alpha = 0.2f)
    val iconTint = if (isActive) onActive else categoryGlyphColor(accent)
    val pressedOverlay = if (isActive) onActive.copy(alpha = 0.16f) else TeperaPalette.buttonBrandDark.copy(alpha = 0.12f)
    val idleBorder = TeperaPalette.activityCardIdleBorder

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp) // росте разом зі шрифтом (WCAG 1.4.4), а не обрізає назву
            .clip(shape)
            .background(containerColor)
            .then(if (!isActive && idleBorder != null) Modifier.border(1.dp, idleBorder, shape) else Modifier)
            .drawWithContent {
                drawContent()
                if (pressed) drawRect(pressedOverlay)
            }
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onOpenHistory)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(tileColor),
            contentAlignment = Alignment.Center
        ) {
            val lineArt = categoryLineArtIconRes(item.category.iconName)
            if (lineArt != null) {
                Icon(painterResource(lineArt), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            } else {
                Icon(categoryIcon(item.category.iconName), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = name,
                color = nameColor,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(onActive))
                    SubText(stringResource(R.string.activity_row_running), onActive.copy(alpha = 0.85f))
                    if (item.minutesToday > 0) SubText(heroDuration(item.minutesToday), onActive)
                } else {
                    SubText(stringResource(R.string.home_header_subtitle), TeperaPalette.textSecondary)
                    if (item.minutesToday > 0) {
                        SubText(heroDuration(item.minutesToday), TeperaPalette.textPrimary)
                    } else {
                        SubText("—", TeperaPalette.textSecondary)
                    }
                }
            }
        }

        if (isActive) {
            TeperaIconButton(
                icon = TeperaSymbols.Pause,
                // З назвою категорії: інакше рядки озвучуються однаково — "Зупинити" (WCAG 2.4.6/4.1.2).
                contentDescription = stringResource(R.string.category_stop_action) + ": " + name,
                onClick = onToggleTimer,
                shape = CircleShape,
                containerColor = TeperaPalette.playActiveFill,
                contentColor = TeperaPalette.playActiveContent
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TeperaIconButton(
                    icon = TeperaSymbols.MoreTime,
                    contentDescription = stringResource(R.string.add_time_action_format, name),
                    onClick = onAddTime,
                    shape = CircleShape,
                    containerColor = TeperaPalette.addTimeTonal,
                    contentColor = TeperaPalette.buttonBrandDark
                )
                TeperaIconButton(
                    icon = TeperaSymbols.PlayArrow,
                    contentDescription = stringResource(R.string.category_start_action) + ": " + name,
                    onClick = onToggleTimer,
                    shape = CircleShape,
                    containerColor = TeperaPalette.activityCardActive,
                    contentColor = onActive
                )
            }
        }
    }
}

@Composable
private fun SubText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontFamily = TeperaPalette.headlineFont,
        fontSize = 12.sp,
        lineHeight = 15.6.sp,
        maxLines = 1
    )
}

/** Скелетон рядка (`Type=Loading`): `state/skeleton` ≈ 8% основного кольору. */
@Composable
fun ActivityRowSkeleton(modifier: Modifier = Modifier) {
    val skeleton = TeperaPalette.textPrimary.copy(alpha = 0.08f)
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(skeleton))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(skeleton))
            Box(Modifier.width(84.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(skeleton))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(skeleton))
            Box(Modifier.size(44.dp).clip(CircleShape).background(skeleton))
        }
    }
}

package com.serkodesign.tepera.ui.home

import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.TeperaDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * Спільні деталі карток горизонтального пейджера Home (Figma "App concept"
 * k6s4prQ9oK9x2uUvzHRghR, node 192:726: "My day" / "Day usage" / "This week") — контейнер картки,
 * рядок заголовка з ⓘ та "×", плашки й діалог пояснення.
 */
internal val HomeCardTextPrimary = Color(0xFF0F0F10) // Text/text-primary
internal val HomeCardTextSecondary = Color(0xFF505050) // Text/text-secondary

/** Картка пейджера: заливка, радіус 24, паддінги 16/12/12/12 і мінімальна висота 182dp (як у макеті). */
@Composable
internal fun HomeCardSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = TeperaPalette.homeCardFill,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 182.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** Заголовок 18sp + ⓘ (пояснення). Кнопки "×" нема — картки "Патерн" і "Цей тиждень" не закриваються (за запитом користувача). */
@Composable
internal fun HomeCardTitleRow(title: String, onInfo: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            color = HomeCardTextPrimary
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onInfo, modifier = Modifier.size(20.dp)) {
            Icon(
                TeperaSymbols.Info,
                contentDescription = stringResource(R.string.home_card_info_action),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Кольорова плашка значення (плитки "This week", "First unlock" у Day usage): радіус 4, паддінг 4. */
@Composable
internal fun HomeTintChip(
    text: String,
    textColor: Color,
    fill: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    fontSize: Int = 16
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(fill)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(4.dp)) else Modifier)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = fontSize.sp, fontWeight = FontWeight.Medium, color = textColor, maxLines = 1)
    }
}

/**
 * "Оцінка → реальність" — дві картки поруч замість голого тексту (за прямим запитом користувача:
 * "по всьому застосунку оформлення такого контенту зроби більш графічно"). Спільний вигляд для
 * [WeeklyReflectionCard]/[UnlockEstimateCard]/[LastPhoneUseEstimateCard]/[OnlineEstimateRevealCard] —
 * усі документовані як "той самий формат". "Твоя оцінка" — приглушений нейтральний тон (це
 * здогад), "Насправді" — фірмовий тон (це підтверджений факт): контраст кольору сам передає
 * різницю функцій рядків, без жодного слова-оцінки "вгадав"/"не вгадав" (FR-P.6/розділ 2.2 —
 * ніякого порівняння в тексті, лише два факти поруч). Однакова висота обох карток —
 * `IntrinsicSize.Min` на Row + `fillMaxHeight()` на дітях, той самий прийом, що `StatTile`
 * на Статистиці/Щоденнику.
 */
@Composable
internal fun GuessRevealRow(guessValue: String, actualValue: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GuessRevealTile(
            label = stringResource(R.string.weekly_reflection_your_guess_label),
            value = guessValue,
            textColor = HomeCardTextSecondary,
            fill = Color(0x14003926),
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        GuessRevealTile(
            label = stringResource(R.string.weekly_reflection_actual_label),
            value = actualValue,
            textColor = TeperaPalette.buttonBrandDark,
            fill = TeperaPalette.surfaceBrandLight,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

/** Одна картка [GuessRevealRow] — публічна (не `private`), бо [LastPhoneUseEstimateCard] інколи
 * має лише "оцінку" без "реальності" (дані ще недоступні) і рендерить саму цю картку окремо. */
@Composable
internal fun GuessRevealTile(label: String, value: String, textColor: Color, fill: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = textColor.copy(alpha = 0.7f), maxLines = 1)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 2)
    }
}

/** Діалог пояснення картки (ⓘ). Кнопка — `TeperaButton` (дизайн-система). */
@Composable
internal fun HomeInfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    TeperaDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = body,
        confirmText = stringResource(R.string.home_card_info_close),
        onConfirm = onDismiss
    )
}

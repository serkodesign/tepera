package com.serkodesign.tepera.ui.home

import com.serkodesign.tepera.ui.theme.TeperaSymbols
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
internal val HomeCardTextPrimary: Color @Composable @ReadOnlyComposable get() = TeperaPalette.textPrimary // Text/text-primary
internal val HomeCardTextSecondary: Color @Composable @ReadOnlyComposable get() = TeperaPalette.textSecondary // Text/text-secondary

/**
 * Картка пейджера (M3 filled card): заливка, радіус 28 (extra large — як картки активностей), відступ 16,
 * проміжок 12, мінімальна висота 182dp. [cornerRadius]/[borderColor]/[contentPadding] — необов'язкові
 * перевизначення для карток з іншим оформленням у конкретному Figma-фреймі (node 347:3037: "My day" —
 * радіус 24 + біла рамка 1dp + відступ 16 звідусіль; "Day usage"/"This week" — той самий радіус 24, але
 * асиметричний відступ top-16/sides-12/bottom-12).
 */
@Composable
internal fun HomeCardSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = TeperaPalette.homeCardFill,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 182.dp)
            .clip(shape)
            .background(containerColor)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(contentPadding),
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
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium
            ),
            color = TeperaPalette.buttonBrandDark
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onInfo, modifier = Modifier.size(20.dp)) {
            Icon(
                TeperaSymbols.Info,
                contentDescription = stringResource(R.string.home_card_info_action),
                tint = TeperaPalette.buttonBrand,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Fallback-сторінка горизонтального пейджера Home, коли жодна з карток з даними ("Мій день",
 * Патерн, "Цей тиждень") ще не має що показати (щойно встановлений застосунок на пристрої без
 * власної історії ОС) — той самий контейнер, що решта сторінок, лише один рядок тексту.
 */
@Composable
internal fun EmptyPagerCard(modifier: Modifier = Modifier) {
    HomeCardSurface(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_no_entries_today),
            style = MaterialTheme.typography.bodyMedium,
            color = HomeCardTextPrimary
        )
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
            fill = TeperaPalette.guessFill,
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

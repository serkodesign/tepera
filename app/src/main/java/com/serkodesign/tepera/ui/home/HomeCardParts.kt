package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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

/** Заголовок 18sp + ⓘ (пояснення) + "×" (закрити, доки не з'явиться нове вікно даних). */
@Composable
internal fun HomeCardTitleRow(title: String, onInfo: () -> Unit, onDismiss: () -> Unit) {
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
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.home_card_info_action),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.context_card_dismiss_action),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Біла плашка "підпис значення" (Figma: "First unlock 07:15"), 12sp, підпис #006944, значення #003926. */
@Composable
internal fun HomeLabelValueChip(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardActive)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TeperaPalette.buttonBrand, maxLines = 1)
        Text(value, fontSize = 12.sp, color = TeperaPalette.buttonBrandDark, maxLines = 1)
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

/** Діалог пояснення картки (ⓘ). Кнопка — `TeperaButton` (дизайн-система). */
@Composable
internal fun HomeInfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TeperaButton(
                text = stringResource(R.string.home_card_info_close),
                onClick = onDismiss,
                size = TeperaButtonSize.Medium,
                type = TeperaButtonType.Tertiary
            )
        }
    )
}

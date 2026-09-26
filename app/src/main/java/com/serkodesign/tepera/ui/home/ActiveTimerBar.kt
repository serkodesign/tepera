package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * GAP-5 (SRS v3.1): таймер, запущений з віджета, має бути видно при відкритті застосунку й зупиняється звідси.
 * Тихий рядок під шапкою Home: кольорова крапка категорії, "Зараз іде: {назва}" і кнопка "Зупинити" (та сама
 * toggleCategoryTimer(), що play/pause на картці). Без часу й лічильника — раніше прибрані з картки за запитом
 * користувача — і без тривожних кольорів (Monastic Style). Показується лише поки таймер іде.
 */
@Composable
internal fun ActiveTimerBar(
    active: CategoryTodaySummary,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(categoryColor(active.category.colorHex))
        )
        Text(
            text = stringResource(R.string.home_active_timer_format, categoryDisplayName(active.category)),
            style = MaterialTheme.typography.bodyMedium,
            color = TeperaPalette.buttonBrandDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Поява/зникнення "Йде: X" озвучується скрінрідером (WCAG 4.1.3): старт і стоп таймера інакше нічим не позначені.
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
        )
        TeperaButton(
            text = stringResource(R.string.home_active_timer_stop),
            onClick = onStop,
            size = TeperaButtonSize.Medium,
            type = TeperaButtonType.Secondary
        )
    }
}

package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Картка дизайн-системи v3.1 (розділ 3.3 плану): фон `card`, обвідка 1dp `cardStroke`, форма 16dp, поле 16dp,
 * групи всередині через 12dp. Кольори — [M3Extra], форма — `MaterialTheme.shapes.large`. Паралельна заміна
 * `TeperaCard`/`V3Card` (ті не змінювались; екрани переходять у етапі D, старі компоненти видаляються в I27).
 */
@Composable
fun M3Card(
    modifier: Modifier = Modifier,
    spacing: Dp = TeperaSpacing.cardGroups,
    // true — той самий колір і вигляд, що в TeperaCard (підсумок тижня, Online сьогодні): у темній темі й «Світанку»
    // M3-палітра давала нейтрально-сірий фон, що не збігався з гамою застосунку.
    appStyle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = M3Extra.colors
    val shape = if (appStyle) androidx.compose.foundation.shape.RoundedCornerShape(16.dp) else MaterialTheme.shapes.large
    val cardColor = if (appStyle) TeperaPalette.cardSurface else colors.card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardColor)
            .then(if (appStyle) Modifier else Modifier.border(1.dp, colors.cardStroke, shape))
            .padding(TeperaSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
private fun M3CardPreviewBody() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        M3Card(Modifier.padding(16.dp)) {
            Text("22:40 – 00:10", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("Увечері · Instagram · ≈ 38 хв", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(name = "M3Card · Light", widthDp = 375, showBackground = true)
@Composable
private fun M3CardLightPreview() {
    MaterialTheme(colorScheme = TeperaLightColors, typography = TeperaTypography, shapes = TeperaShapesM3) {
        CompositionLocalProvider(LocalTeperaExtraColors provides LightTeperaExtraColors) { M3CardPreviewBody() }
    }
}

@Preview(name = "M3Card · Dark", widthDp = 375, showBackground = true)
@Composable
private fun M3CardDarkPreview() {
    MaterialTheme(colorScheme = TeperaDarkColors, typography = TeperaTypography, shapes = TeperaShapesM3) {
        CompositionLocalProvider(LocalTeperaExtraColors provides DarkTeperaExtraColors) { M3CardPreviewBody() }
    }
}

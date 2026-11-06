package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.serkodesign.tepera.ui.theme.TeperaPalette
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.ui.theme.DarkTeperaExtraColors
import com.serkodesign.tepera.ui.theme.LightTeperaExtraColors
import com.serkodesign.tepera.ui.theme.LocalTeperaExtraColors
import com.serkodesign.tepera.ui.theme.M3Card
import com.serkodesign.tepera.ui.theme.M3Extra
import com.serkodesign.tepera.ui.theme.TeperaDarkColors
import com.serkodesign.tepera.ui.theme.TeperaExtraShapes
import com.serkodesign.tepera.ui.theme.TeperaLightColors
import com.serkodesign.tepera.ui.theme.TeperaShapesM3
import com.serkodesign.tepera.ui.theme.TeperaSpacing
import com.serkodesign.tepera.ui.theme.TeperaTypography

/**
 * Бейдж повторюваності («6 з 7 вечорів») за розділом 3.3 плану: фон `signalContainer`, текст `onSignalContainer`,
 * стиль labelMedium, поля 12×4dp, форма full. Паралельний до `TeperaChip(compact)`, який використовує наявна
 * `RiskWindowCard`; на нього картка переходить у I12.
 */
@Composable
fun M3RecurrenceBadge(text: String, modifier: Modifier = Modifier) {
    // Колір чіпа — той самий, що в TeperaChip (замість коричневого signalContainer M3-палітри).
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TeperaPalette.textPrimary,
        maxLines = 1,
        modifier = modifier
            .clip(TeperaExtraShapes.full)
            .background(TeperaPalette.labelChipFill)
            .border(1.dp, TeperaPalette.labelChipBorder, TeperaExtraShapes.full)
            .padding(horizontal = TeperaSpacing.m, vertical = TeperaSpacing.xs)
    )
}

/**
 * Крапки повторюваності за останні дні (розділ 3.3): ~7dp, проміжок 8dp; заповнена (HIT) — `signalVivid`, порожня
 * (MISS) — `dataHeat0`, «не застосовно» — `dataHeat0` із прозорістю 40% (як в наявній картці). Колір не єдиний носій
 * змісту: уся група має текстовий опис для TalkBack ([description]).
 */
@Composable
fun M3RecurrenceDots(dots: List<DotState>, description: String, modifier: Modifier = Modifier) {
    val colors = M3Extra.colors
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(TeperaSpacing.dotsGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { dot ->
            Box(
                Modifier.size(7.dp).clip(CircleShape).background(
                    when (dot) {
                        DotState.HIT -> colors.signalVivid
                        DotState.MISS -> colors.dataHeat0
                        DotState.NOT_APPLICABLE -> colors.dataHeat0.copy(alpha = 0.4f)
                    }
                )
            )
        }
    }
}

@Composable
private fun RecurrencePartsPreviewBody() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        M3Card(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(TeperaSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text("22:40 – 00:10", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                M3RecurrenceBadge("6 з 7 вечорів")
            }
            M3RecurrenceDots(
                dots = listOf(
                    DotState.HIT, DotState.HIT, DotState.MISS, DotState.HIT, DotState.HIT,
                    DotState.NOT_APPLICABLE, DotState.HIT
                ),
                description = "6 з 7 днів"
            )
        }
    }
}

@Preview(name = "Recurrence · Light", widthDp = 375, showBackground = true)
@Composable
private fun RecurrencePartsLightPreview() {
    MaterialTheme(colorScheme = TeperaLightColors, typography = TeperaTypography, shapes = TeperaShapesM3) {
        CompositionLocalProvider(LocalTeperaExtraColors provides LightTeperaExtraColors) { RecurrencePartsPreviewBody() }
    }
}

@Preview(name = "Recurrence · Dark", widthDp = 375, showBackground = true)
@Composable
private fun RecurrencePartsDarkPreview() {
    MaterialTheme(colorScheme = TeperaDarkColors, typography = TeperaTypography, shapes = TeperaShapesM3) {
        CompositionLocalProvider(LocalTeperaExtraColors provides DarkTeperaExtraColors) { RecurrencePartsPreviewBody() }
    }
}

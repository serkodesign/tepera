package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.ui.theme.DarkTeperaExtraColors
import com.serkodesign.tepera.ui.theme.LightTeperaExtraColors
import com.serkodesign.tepera.ui.theme.LocalTeperaExtraColors
import com.serkodesign.tepera.ui.theme.M3Extra
import com.serkodesign.tepera.ui.theme.TeperaDarkColors
import com.serkodesign.tepera.ui.theme.TeperaExtraShapes
import com.serkodesign.tepera.ui.theme.TeperaLightColors
import com.serkodesign.tepera.ui.theme.TeperaShapesM3
import com.serkodesign.tepera.ui.theme.TeperaSpacing
import com.serkodesign.tepera.ui.theme.TeperaTypography

/**
 * Оболонка шторки плану за дизайн-системою v3.1 (розділ 3.4 плану), M3 «нижній лист»: контейнер `surfaceContainerLow`,
 * форма 28dp лише зверху, ручка 32×4dp (`onSurfaceVariant` @40%, відступ зверху 14dp), вміст із полями 24dp з боків,
 * 8dp зверху після ручки, 24dp знизу, вертикальні проміжки 16dp. Затемнення екрана під шторкою — чорний 45% (як було).
 *
 * Це власний лист поверх `MainActivity`, а не `ModalBottomSheet`: його відкриває сервіс доступності як окремий екран, і
 * він сам керує «назад»/виходом (див. [PlanOverlayScreen]).
 */
@Composable
fun PlanSheetShell(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(TeperaExtraShapes.sheetTop)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(TeperaExtraShapes.full)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = TeperaSpacing.xxl, end = TeperaSpacing.xxl, top = TeperaSpacing.s, bottom = TeperaSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TeperaSpacing.l),
                content = content
            )
        }
    }
}

/**
 * Рядок застосунку в шторці (3.4): аватар 40dp кольору `dataSocial` (для всіх застосунків однаковий — окремих кольорів
 * застосунків у даних нема), назва `titleSmall` / `onSurface`, підпис `bodySmall` / `onSurfaceVariant`.
 */
@Composable
fun PlanSheetAppRow(appLabel: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TeperaSpacing.m),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(M3Extra.colors.dataSocial),
            contentAlignment = Alignment.Center
        ) {
            Text(appLabel.take(1).uppercase(), color = M3Extra.colors.onDark, style = MaterialTheme.typography.titleSmall)
        }
        Column {
            Text(appLabel, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}


@Composable
private fun PlanSheetPreviewBody() {
    PlanSheetShell {
        PlanSheetAppRow("Instagram", "Вікно 22:40–00:10")
        Text("Твій план на цей час", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(name = "PlanSheetShell · Dark", widthDp = 375, heightDp = 500, showBackground = true)
@Composable
private fun PlanSheetShellDarkPreview() {
    MaterialTheme(colorScheme = TeperaDarkColors, typography = TeperaTypography, shapes = TeperaShapesM3) {
        CompositionLocalProvider(LocalTeperaExtraColors provides DarkTeperaExtraColors) { PlanSheetPreviewBody() }
    }
}

@Preview(name = "PlanSheetShell · Light", widthDp = 375, heightDp = 500, showBackground = true)
@Composable
private fun PlanSheetShellLightPreview() {
    MaterialTheme(colorScheme = TeperaLightColors, typography = TeperaTypography, shapes = TeperaShapesM3) {
        CompositionLocalProvider(LocalTeperaExtraColors provides LightTeperaExtraColors) { PlanSheetPreviewBody() }
    }
}

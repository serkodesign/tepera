package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Тип кнопки дизайн-системи v3.1 (розділ 3.1 плану). */
enum class M3ButtonType { Filled, Tonal, Outlined, Text }

/** Розмір: S — 40dp (лейбл labelLarge), M — 56dp (лейбл titleMedium). */
enum class M3ButtonSize { S, M }

/**
 * Кнопка дизайн-системи v3.1 (Material 3), розділ 3.1 плану. Нова паралельна версія: наявний `TeperaButton`
 * (Primary/Secondary/Tertiary) не змінювався й лишається для неперенесених екранів — екрани переходять на цю кнопку
 * в етапі D, старий `TeperaButton` видаляється в I27.
 *
 * - Filled: primary / onPrimary. Tonal: surfaceContainerHighest / onSurface.
 *   Outlined: без фону, обвідка 1dp outlineVariant, текст primary. Text: без фону, текст primary.
 * - Висота S = 40dp (горизонтальне поле 16dp, у Text — 12dp), M = 56dp (поле 24dp). Форма — full.
 * - Для S ціль дотику розширена до 48dp ([minimumInteractiveComponentSize]).
 * - Ширину задає виклик через [modifier] (`fillMaxWidth()`, `weight(1f)`); за замовчуванням — за вмістом.
 * - Кольори беруться з поточного `MaterialTheme.colorScheme`, тому кнопка слідує за світлою/темною темою.
 */
@Composable
fun M3Button(
    type: M3ButtonType,
    size: M3ButtonSize,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val height: Dp = if (size == M3ButtonSize.S) 40.dp else 56.dp
    val horizontal: Dp = when {
        size == M3ButtonSize.M -> 24.dp
        type == M3ButtonType.Text -> 12.dp
        else -> 16.dp
    }
    val container: Color = when (type) {
        M3ButtonType.Filled -> scheme.primary
        M3ButtonType.Tonal -> scheme.surfaceContainerHighest
        M3ButtonType.Outlined, M3ButtonType.Text -> Color.Transparent
    }
    val content: Color = when (type) {
        M3ButtonType.Filled -> scheme.onPrimary
        M3ButtonType.Tonal -> scheme.onSurface
        M3ButtonType.Outlined, M3ButtonType.Text -> scheme.primary
    }
    val colors = ButtonDefaults.buttonColors(
        containerColor = container,
        contentColor = content,
        // Стандартні стани M3: контейнер onSurface 12%, вміст onSurface 38%. Без фону лишається без фону.
        disabledContainerColor = if (container == Color.Transparent) Color.Transparent else scheme.onSurface.copy(alpha = 0.12f),
        disabledContentColor = scheme.onSurface.copy(alpha = 0.38f)
    )
    val border = if (type == M3ButtonType.Outlined) {
        BorderStroke(1.dp, if (enabled) scheme.outlineVariant else scheme.onSurface.copy(alpha = 0.12f))
    } else null
    val sizing = if (size == M3ButtonSize.S) modifier.minimumInteractiveComponentSize() else modifier

    Button(
        onClick = onClick,
        modifier = sizing.height(height),
        enabled = enabled,
        shape = TeperaExtraShapes.full,
        colors = colors,
        border = border,
        elevation = null,
        contentPadding = PaddingValues(horizontal = horizontal, vertical = 0.dp)
    ) {
        Text(
            text = text,
            style = if (size == M3ButtonSize.S) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
    }
}

// ---- Превʼю: усі типи й розміри, включно з вимкненим станом, у світлій і темній темі ----

@Composable
private fun M3ButtonGallery() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (size in M3ButtonSize.entries) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (type in M3ButtonType.entries) {
                        M3Button(type, size, type.name, onClick = {})
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (type in M3ButtonType.entries) {
                        M3Button(type, size, type.name, onClick = {}, enabled = false)
                    }
                }
            }
            M3Button(M3ButtonType.Filled, M3ButtonSize.M, "Беру книгу", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(name = "M3Button · Light", widthDp = 420, showBackground = true)
@Composable
private fun M3ButtonLightPreview() {
    MaterialTheme(colorScheme = TeperaLightColors, typography = TeperaTypography, shapes = TeperaShapesM3) { M3ButtonGallery() }
}

@Preview(name = "M3Button · Dark", widthDp = 420, showBackground = true)
@Composable
private fun M3ButtonDarkPreview() {
    MaterialTheme(colorScheme = TeperaDarkColors, typography = TeperaTypography, shapes = TeperaShapesM3) { M3ButtonGallery() }
}

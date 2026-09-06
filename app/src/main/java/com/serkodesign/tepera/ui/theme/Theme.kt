package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ЗАГЛУШКА: дефолтна Material 3 палітра. Коли дизайн у Figma буде готовий, кольори/типографіка
// сюди переносяться через Figma MCP-конектор (SRS 4.1, CLAUDE.md) — логіка й екрани при цьому
// не переробляються, лише значення тут.

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun TeperaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

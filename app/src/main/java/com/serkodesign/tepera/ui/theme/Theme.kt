package com.serkodesign.tepera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Фаза 6: палітра з Figma-фрейму Everyday_Designs (TeperaPalette.kt) — м'який пастельний
// градієнт зі ЗАВЖДИ темним текстом. Дизайн не має темної версії (наданий лише один Light-фрейм
// Home), тож TeperaTheme свідомо ІГНОРУЄ isSystemInDarkTheme() і завжди застосовує LightColors:
// system dark theme раніше вмикав darkColorScheme() з білим текстом onSurface/onBackground, який
// був майже невидимий на світлому градієнті (підтверджено на Samsung S23 із системною темною
// темою — "Доброго ранку" й назви категорій ледь читались).
private val LightColors = lightColorScheme()

@Composable
fun TeperaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

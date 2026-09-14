package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Фаза 6: палітра з Figma-фрейму Everyday_Designs (TeperaPalette.kt) — м'який пастельний
// градієнт зі ЗАВЖДИ темним текстом. Дизайн не має темної версії (наданий лише один Light-фрейм
// Home), тож TeperaTheme свідомо ІГНОРУЄ isSystemInDarkTheme() і завжди застосовує LightColors:
// system dark theme раніше вмикав darkColorScheme() з білим текстом onSurface/onBackground, який
// був майже невидимий на світлому градієнті (підтверджено на Samsung S23 із системною темною
// темою — "Доброго ранку" й назви категорій ледь читались).
private val LightColors = lightColorScheme()

// За прямим запитом користувача: стандартний радіус скруглення M3 Card() (shapes.medium,
// дефолт 12dp) піднято до 16dp — єдине місце, звідки це поширюється на всі `Card(...)` без
// явного shape (4 картки Stats + fallback-картка "нема доступу" в BalanceCard). Картки з явним
// RoundedCornerShape (контекстний стек Home, плитки категорій, навбар) міняються там, де вони
// визначені — цей Shapes на них не впливає.
private val TeperaShapes = Shapes(medium = RoundedCornerShape(16.dp))

@Composable
fun TeperaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = TeperaShapes,
        content = content
    )
}

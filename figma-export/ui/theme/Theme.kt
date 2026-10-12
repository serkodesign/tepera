package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.util.ThemeStore

// За прямим запитом користувача: стандартний радіус скруглення M3 Card() (shapes.medium,
// дефолт 12dp) піднято до 16dp — єдине місце, звідки це поширюється на всі `Card(...)` без
// явного shape (4 картки Stats + fallback-картка "нема доступу" в BalanceCard). Картки з явним
// RoundedCornerShape (контекстний стек Home, плитки категорій, навбар) міняються там, де вони
// визначені — цей Shapes на них не впливає.
private val TeperaShapes = Shapes(medium = RoundedCornerShape(16.dp))

/**
 * Редизайн "ui-redesign" (02.10.2026, за рішенням власника): тема йде за системним світлим/темним режимом —
 * [RedesignLightColors] (референс image 4) або [RedesignDarkColors] (image 1-3), див. TeperaColors.kt. Раніше
 * TeperaTheme свідомо ігнорувала isSystemInDarkTheme() і завжди була світлою — дизайн тоді не мав темної версії.
 */
@Composable
fun TeperaTheme(
    content: @Composable () -> Unit
) {
    // Налаштування → Загальні → Тема: "Системна" слідує за пристроєм, "Світла"/"Темна" — примусово.
    val context = LocalContext.current
    ThemeStore.load(context)
    // ThemeStore.THEME_CHOICE_ENABLED = false вимикає вибір і завжди робить тему світлою.
    val dark = if (!ThemeStore.THEME_CHOICE_ENABLED) false else when (ThemeStore.current()) {
        ThemeStore.Mode.SYSTEM -> isSystemInDarkTheme()
        ThemeStore.Mode.LIGHT -> false
        ThemeStore.Mode.DARK -> true
    }
    val colors = if (dark) RedesignDarkColors else RedesignLightColors
    MaterialTheme(shapes = TeperaShapes) {
        RedesignScope(colors, content)
    }
}

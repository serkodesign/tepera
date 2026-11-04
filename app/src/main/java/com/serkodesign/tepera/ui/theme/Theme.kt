package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.util.MoodStore
import com.serkodesign.tepera.util.ThemeStore
import kotlinx.coroutines.delay
import java.time.LocalTime

// За прямим запитом користувача: стандартний радіус скруглення M3 Card() (shapes.medium,
// дефолт 12dp) піднято до 16dp — єдине місце, звідки це поширюється на всі `Card(...)` без
// явного shape (4 картки Stats + fallback-картка "нема доступу" в BalanceCard). Картки з явним
// RoundedCornerShape (контекстний стек Home, плитки категорій, навбар) міняються там, де вони
// визначені — цей Shapes на них не впливає.
private val TeperaShapes = Shapes(medium = RoundedCornerShape(16.dp))

private fun minuteBucketNow(): Int = LocalTime.now().let { (it.hour * 60 + it.minute) / 5 * 5 }

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
    val mode = if (ThemeStore.THEME_CHOICE_ENABLED) ThemeStore.current() else ThemeStore.Mode.LIGHT
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeStore.Mode.SYSTEM -> systemDark
        ThemeStore.Mode.DARK -> true
        else -> false
    }
    // Налаштування → Загальні → Настрій застосунку: "Світанок" замінює набір кольорів, небо залежить від години.
    MoodStore.load(context)
    val dawn = MoodStore.current() == MoodStore.Mood.DAWN
    // Налагоджувальний годинник (лише debug): підміняє справжній час, поки не скинутий.
    val debugMinute = DebugClock.override.value
    val realHour = remember { LocalTime.now().hour }
    val hour = debugMinute?.div(60) ?: realHour
    // «За часом»: хвилина доби з кроком 5 хв. Стан змінюється рідко, тож тема перераховується лише на межі кроку.
    val realBucket by produceState(minuteBucketNow(), mode) {
        if (mode == ThemeStore.Mode.TIME) while (true) { delay(60_000); value = minuteBucketNow() }
    }
    val timeBucket = debugMinute?.div(5)?.times(5) ?: realBucket
    val colors = when {
        mode == ThemeStore.Mode.TIME && dawn -> remember(timeBucket) { dawnColorsForMinute(timeBucket) }
        mode == ThemeStore.Mode.TIME -> remember(timeBucket) { classicTimeColors(timeBucket) }
        dawn -> remember(dark, hour) { dawnColors(dark, hour) }
        // Мостик I08: старі екрани бачать значення v3.1 (таблиця 4 плану), див. M3LegacyBridge.kt.
        dark -> remember { m3BridgeColors(dark = true) }
        else -> remember { m3BridgeColors(dark = false) }
    }
    // Дизайн-система v3.1 (M3): нова ColorScheme/Typography/Shapes + TeperaExtraColors для звичайних режимів
    // (Світла/Темна/Системна). «Світанок» і «За часом» лишаються на старих кольорах (план, I25l).
    val useM3 = !dawn && mode != ThemeStore.Mode.TIME
    if (useM3) {
        MaterialTheme(
            colorScheme = if (dark) TeperaDarkColors else TeperaLightColors,
            typography = TeperaTypography,
            shapes = TeperaShapesM3
        ) {
            CompositionLocalProvider(
                LocalOverscrollFactory provides BounceOverscrollFactory,
                LocalTeperaExtraColors provides (if (dark) DarkTeperaExtraColors else LightTeperaExtraColors)
            ) {
                RedesignScope(colors, useOuterM3Scheme = true, content = content)
            }
        }
    } else {
        MaterialTheme(shapes = TeperaShapes) {
            CompositionLocalProvider(
                LocalOverscrollFactory provides BounceOverscrollFactory,
                // Нові компоненти v3.1 (M3Card тощо) мають бути читабельними й тут: темний/світлий набір за isDark.
                LocalTeperaExtraColors provides (if (colors.isDark) DarkTeperaExtraColors else LightTeperaExtraColors)
            ) {
                RedesignScope(colors, content = content)
            }
        }
    }
}

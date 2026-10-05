package com.serkodesign.tepera.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Кольори інтерфейсу, що залежать від теми. [TeperaPalette] читає їх із [LocalTeperaColors], тож той самий
 * виклик `TeperaPalette.buttonBrandDark` дає різний колір у різних наборах.
 *
 * - [LegacyTeperaColors] — нинішній світлий вигляд (значення 1:1 з попередніх констант TeperaPalette).
 * - [RedesignLightColors] / [RedesignDarkColors] — редизайн "ui-redesign" за референсами Figma "App concept"
 *   (k6s4prQ9oK9x2uUvzHRghR, секція 334:39): світла тема — image 4 (334:41), темна — image 1-3 (334:31/34/37).
 *   Це растрові картинки без шарів, тож значення — піксельні заміри (найчастіший колір ділянки), за прямим
 *   рішенням власника; схвалена ним таблиця відповідності ролей — у сесії 02.10.2026.
 *
 * Поки редизайн показується лише на Home (за запитом власника "спершу один екран"), решта застосунку лишається
 * на [LegacyTeperaColors].
 */
@Immutable
data class TeperaColors(
    val isDark: Boolean,
    /** Фон екрана: вертикальний градієнт [backgroundTop] -> [backgroundBottom]; null у legacy (розмиті плями). */
    val backgroundTop: Color?,
    val backgroundBottom: Color?,
    /** Розмиті кольорові плями поверх градієнта (Figma "App concept", node 347:3037: Ellipse 5/7) — null, якщо для
     * цього набору кольорів плям нема (legacy має власний механізм, [RedesignDarkColors] свідомо без плям). */
    val backgroundBlobTopRight: Color? = null,
    val backgroundBlobBottomLeft: Color? = null,
    /** Основний текст / іконки (раніше buttonBrandDark #003926). */
    val ink: Color,
    /** Другорядний текст (раніше HomeCardTextSecondary #505050). */
    val textSecondary: Color,
    /** Фірмовий акцент для тексту й дрібних елементів (раніше buttonBrand #006944). */
    val brand: Color,
    /** Вимкнений перемикач: трек. */
    val switchTrackOff: Color,
    /** Тональна поверхня чипів (раніше surfaceBrandLight #DCF6ED). */
    val surfaceBrandLight: Color,
    /** Картки (TeperaCard, контекстні картки Home, "Твій день"). */
    val card: Color,
    /** Поверхня "вибраного" білого елемента всередині картки (раніше cardActive #FFFFFF). */
    val cardActive: Color,
    /** Рядки списків (записи Щоденника, історія категорії). */
    val listItem: Color,
    /** Головна дія: велика Primary-кнопка, картка категорії з увімкненим таймером. */
    val primaryFill: Color,
    val onPrimary: Color,
    /** Невибраний чип / кнопка play на картці категорії. */
    val chipFill: Color,
    val chipContent: Color,
    /** Кола-кнопки іконок (книга, шестерня, "назад"). */
    val iconButtonFill: Color,
    val iconButtonContent: Color,
    /** Друга кругла кнопка на картці категорії ("додати час"). */
    val moreTimeFill: Color,
    /** Навбар-"таблетка". */
    val navPill: Color,
    val navSelected: Color,
    val navSelectedContent: Color,
    val navUnselectedContent: Color,
    /** Заливка невибраних вкладок навбару (Figma: surface-card, суцільна). */
    val navTabUnselected: Color,
    /** Крапки перемикача сторінок карток Home. */
    val pagerDotActive: Color,
    val pagerDotInactive: Color,
    /** Темні іконки системних барів (true) чи світлі (false). */
    val lightSystemBars: Boolean
)

val LegacyTeperaColors = TeperaColors(
    isDark = false,
    backgroundTop = null,
    backgroundBottom = null,
    ink = Color(0xFF003926),
    textSecondary = Color(0xFF505050),
    brand = Color(0xFF006944),
    surfaceBrandLight = Color(0xFFDCF6ED),
    switchTrackOff = Color(0xFFE3EAE5),
    card = Color(0xB3FFFFFF),
    cardActive = Color(0xFFFFFFFF),
    listItem = Color(0xCCFFFFFF),
    primaryFill = Color(0xFF006944),
    onPrimary = Color.White,
    chipFill = Color.White,
    chipContent = Color.Black,
    iconButtonFill = Color(0x80FFFFFF),
    iconButtonContent = Color(0xFF003926),
    moreTimeFill = Color(0xFFC5E2CB),
    navPill = Color(0xFFFFFFFF),
    navSelected = Color(0xFFB2E5D3),
    navSelectedContent = Color(0xFF003926),
    navUnselectedContent = Color(0xFF505050),
    navTabUnselected = Color(0x1AF0F3F4),
    pagerDotActive = Color.White,
    pagerDotInactive = Color.White.copy(alpha = 0.5f),
    lightSystemBars = true
)

/**
 * Image 4 (334:41) → оновлено за Figma "App concept", node 347:3037 ("Home screen", за прямим
 * запитом користувача, сесія з двома рядками заголовка/чіпсами легенди/горизонтальною сіткою
 * активностей): кремовий градієнтний фон (`from-[#f9f0df] to-[#ede9e8]`) з двома розмитими плямами
 * (Ellipse 5 — тепла бежева зверху праворуч, Ellipse 7 — бузкова знизу ліворуч; ті самі позиція/
 * радіус/сигма, що в legacy-механізмі фону, лише нові кольори/прозорість — звірено з реальних SVG
 * фільтрів, не з піксельного заміру скріншота). Навбар перефарбовано під той самий фрейм: пігулка
 * #003926 (Text/text-brand-dark), вибрана вкладка #DCF6ED (Surface/surface-brand-light) з текстом/
 * іконкою #003926, невибрані іконки — суцільний білий (звірено з fill у SVG ballot/leaderboard).
 */
val RedesignLightColors = TeperaColors(
    isDark = false,
    backgroundTop = Color(0xFFF9F0DF),
    backgroundBottom = Color(0xFFEDE9E8),
    backgroundBlobTopRight = Color(0xFFFFF2D2), // Ellipse 5, fill-opacity 1
    backgroundBlobBottomLeft = Color(0x4DC2BDF6), // Ellipse 7, fill-opacity 0.3
    ink = Color(0xFF003926),
    textSecondary = Color(0xFF767676), // підпис "5/8" у image 4
    brand = Color(0xFF006944),
    surfaceBrandLight = Color(0xFFFEFEFE),
    switchTrackOff = Color(0xFFF0F3F4), // трек прогресу "0/1" у image 4
    card = Color(0xFFEDF1E5),
    cardActive = Color(0xFFFCFCFB),
    // За прямим запитом користувача: рядки-активності Щоденника (HistoryEntryRow) на тій самій
    // rgba(255,255,255,0.7), що решта "скляних" карток/рядків застосунку.
    listItem = Color(0xB3FFFFFF),
    primaryFill = Color(0xFF003926),
    onPrimary = Color.White,
    chipFill = Color(0xFFFEFEFE),
    chipContent = Color(0xFF003926),
    iconButtonFill = Color(0xFFFCFBF7),
    iconButtonContent = Color(0xFF003926),
    moreTimeFill = Color(0xFFFCFBF7),
    // Навбар за Figma "App concept" node 395:1011: скляний контейнер Surface/surface-card-transparent (#FFFFFF4D),
    // вибрана вкладка Surface/surface-brand (#006944) з вмістом Surface/surface-brand-light (#DCF6ED), невибрані —
    // суцільний Surface/surface-card (#FFFFFF) з іконкою Text/text-secondary (#505050).
    navPill = Color(0x4DFFFFFF),
    navSelected = Color(0xFF006944),
    navSelectedContent = Color(0xFFDCF6ED),
    navUnselectedContent = Color(0xFF505050),
    navTabUnselected = Color(0xFFFFFFFF),
    pagerDotActive = Color(0xFF003926),
    pagerDotInactive = Color(0xFF003926).copy(alpha = 0.25f),
    lightSystemBars = true
)

/** Image 1-3 (334:31/34/37): темний зелений градієнт, кремові акценти. */
val RedesignDarkColors = TeperaColors(
    isDark = true,
    // Темна тема за макетом 362:516 (узгоджено 03.10): фон градієнт #062814 → #11322E, картки #164233,
    // чіпи #003926, навбар лишається світлим (#F1F0F1) з темно-зеленою вибраною вкладкою.
    backgroundTop = Color(0xFF062814),
    backgroundBottom = Color(0xFF11322E),
    ink = Color(0xFFFEFFEF),
    textSecondary = Color(0xFFC5DCD9), // текст повідомлення в image 1
    brand = Color(0xFFFEFFEF),
    surfaceBrandLight = Color(0xFF1A3D38),
    switchTrackOff = Color(0xFF1A3D38),
    card = Color(0xFF164233),
    cardActive = Color(0xFF1A3D38),
    listItem = Color(0xFF164233),
    primaryFill = Color(0xFFFEFFEF),
    onPrimary = Color(0xFF062924),
    chipFill = Color(0xFF003926),
    chipContent = Color(0xFFFEFFEF),
    iconButtonFill = Color(0xFF164233),
    iconButtonContent = Color(0xFFFEFFEF),
    moreTimeFill = Color(0xFFC5E2CB),
    // Темний навбар: той самий скляний контейнер; вибрана вкладка — кремова (як основна кнопка темної теми),
    // невибрані — темна картка. Токенів темного фрейму в Figma немає — підібрано під палітру темної теми.
    navPill = Color(0x4DFFFFFF),
    navSelected = Color(0xFFFEFFEF),
    navSelectedContent = Color(0xFF062924),
    navUnselectedContent = Color(0xFFFEFFEF),
    navTabUnselected = Color(0xFF1A3D38),
    pagerDotActive = Color(0xFFFEFFEF),
    pagerDotInactive = Color(0xFFFEFFEF).copy(alpha = 0.3f),
    lightSystemBars = false
)

val LocalTeperaColors = staticCompositionLocalOf { LegacyTeperaColors }

/**
 * Обгортка екрана в набір редизайну: [LocalTeperaColors], Material-схема з тими самими ролями (щоб текст без
 * явного кольору, діалоги й поля M3 не лишались чорними на темному фоні) і колір тексту за замовчуванням.
 */
@Composable
fun RedesignScope(colors: TeperaColors, content: @Composable () -> Unit) {
    val base = if (colors.isDark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = colors.primaryFill,
        onPrimary = colors.onPrimary,
        background = colors.backgroundTop ?: base.background,
        onBackground = colors.ink,
        surface = colors.card,
        onSurface = colors.ink,
        onSurfaceVariant = colors.textSecondary
    )
    CompositionLocalProvider(LocalTeperaColors provides colors) {
        MaterialTheme(colorScheme = scheme, shapes = MaterialTheme.shapes, typography = MaterialTheme.typography) {
            CompositionLocalProvider(LocalContentColor provides colors.ink, content = content)
        }
    }
}

/** Темні (true) чи світлі (false) іконки статус-бару й навігаційної смуги — під фон поточного екрана. */
@Composable
fun SystemBarsAppearance(lightBars: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }
}

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
import androidx.compose.ui.graphics.lerp as lerpColor
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
    /** Фон/рамка єдиного чипа "підпис-значення" [TeperaChip] (Figma "App concept", node 408:2185 — темна тема,
     * 407:1882 — світла) — окремо від [chipFill]/[chipContent] вище (ті лишаються для картки категорії/чипів
     * воріт/вибору категорії, їх не чіпаємо). Текст чипа бере [textPrimary], не окремий токен. */
    val labelChipFill: Color,
    val labelChipBorder: Color,
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
    /** Прогрес-бар "Твій день" (BalanceCard): фон і рамка рамки, трек незайнятої частини, штрихи, сегмент "Решта дня", орієнтир. */
    val barFrame: Color,
    val barFrameBorder: Color,
    val barTrack: Color,
    val barHatchStripe: Color,
    val barRestSegment: Color,
    val barTargetLine: Color,
    val barTargetFill: Color,
    /** Тло діалогів і календаря (TeperaDialog, TeperaPickers): окремо від surfaceBrandLight. */
    val dialogSurface: Color,
    /** Основний текст (LocalContentColor, onSurface, onBackground) — за docs/design-tokens-figma.md, п. 3 (раніше ink). */
    val textPrimary: Color,
    /** Білий текст на темних фото / темно-бірюзових поверхнях. НЕ для тексту на brand/primary — там onPrimary. */
    val textOnDark: Color,
    val textPlaceholder: Color,
    val textDisabled: Color,
    val textError: Color,
    /** Рамка за замовчуванням (контраст ≥3:1). */
    val borderDefault: Color,
    val borderStrong: Color,
    val borderError: Color,
    val borderBrand: Color,
    /** Темні іконки системних барів (true) чи світлі (false). */
    val lightSystemBars: Boolean,
    // Home FINAL (docs/HANDOFF.md, Figma "App concept" сторінка UI design, Tepera Theme): hero-картка "Наповнення".
    /** `hero/surface-alt` — фон hero-картки. */
    val heroSurface: Color,
    /** `hero/on-light` — темний текст/іконки на світлих елементах hero (м'ятна "вода", біла пілюля). */
    val heroOnLight: Color,
    /** `hero/border` — контур hero (1dp). */
    val heroBorder: Color,
    /** `hero/track` — порожня доріжка графіків, фон кнопок/пілюль/скелетонів у hero. */
    val heroTrack: Color,
    /** `button/add-time-tonal` — кружок "додати час" на рядку активності. */
    val addTimeTonal: Color,
    // «Світанок» (Налаштування → Настрій застосунку): значення за замовчуванням лишають решту наборів без змін.
    /** true — набір «Світанок»: небесний градієнт за порою доби, скляні картки, hero без картки з сонцем/місяцем. */
    val isDawn: Boolean = false,
    /** Багатоколірний вертикальний градієнт фону; якщо задано — замість пари [backgroundTop]/[backgroundBottom]. */
    val backgroundStops: List<Color>? = null,
    /** Головна кнопка на hero (кремова в класичних наборах, персикова в «Світанку») і її текст. */
    val heroCta: Color = Color(0xFFFEFFEF),
    val heroCtaText: Color = Color(0xFF062924)
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
    labelChipFill = Color.White,
    labelChipBorder = Color.Transparent,
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
    barFrame = Color(0xFFFFFFFF),
    barFrameBorder = Color(0xFFDDE2E4),
    barTrack = Color(0xFFE2DED1),
    barHatchStripe = Color(0x80FFFFFF),
    barRestSegment = Color(0xFF006944),
    barTargetLine = Color(0xFFF5C401),
    barTargetFill = Color(0x33F5C401),
    dialogSurface = Color(0xFFDCF6ED),
    textPrimary = Color(0xFF0F0F10),
    textOnDark = Color.White,
    textPlaceholder = Color(0xFF767676),
    textDisabled = Color(0xFFADADAD),
    textError = Color(0xFFE70F1A),
    borderDefault = Color(0xFFDDE2E4),
    borderStrong = Color(0xFF505050),
    borderError = Color(0xFFE70F1A),
    borderBrand = Color(0xFF006944),
    lightSystemBars = true,
    heroSurface = Color(0xFF11322E),
    heroOnLight = Color(0xFF003926),
    heroBorder = Color(0x00FFFFFF),
    heroTrack = Color(0x1AFFFFFF),
    addTimeTonal = Color(0xFFDCF6ED)
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
    // Figma node 407:1882: surface/default #FFFFFF + border input/fill #F0F3F4 (get_variable_defs).
    labelChipFill = Color(0xFFFFFFFF),
    labelChipBorder = Color(0xFFF0F3F4),
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
    barFrame = Color(0xFFFFFFFF),
    barFrameBorder = Color(0xFFDDE2E4),
    barTrack = Color(0xFFE2DED1),
    barHatchStripe = Color(0x80FFFFFF),
    barRestSegment = Color(0xFF006944),
    barTargetLine = Color(0xFFF5C401),
    barTargetFill = Color(0x33F5C401),
    dialogSurface = Color(0xFFFEFEFE), // як surfaceBrandLight у світлій темі (поточне значення)
    textPrimary = Color(0xFF0F0F10), // docs/design-tokens-figma.md, п. 3 — рішення власника
    textOnDark = Color.White,
    textPlaceholder = Color(0xFF6B6B6B),
    textDisabled = Color(0xFFADADAD),
    textError = Color(0xFFE70F1A),
    borderDefault = Color(0xFF888888),
    borderStrong = Color(0xFF505050),
    borderError = Color(0xFFE70F1A),
    borderBrand = Color(0xFF006944),
    lightSystemBars = true,
    heroSurface = Color(0xFF11322E),
    heroOnLight = Color(0xFF003926),
    heroBorder = Color(0x00FFFFFF),
    heroTrack = Color(0x1AFFFFFF),
    addTimeTonal = Color(0xFFDCF6ED)
)

/** Image 1-3 (334:31/34/37): темний зелений градієнт, кремові акценти. */
val RedesignDarkColors = TeperaColors(
    isDark = true,
    // Темна тема за макетом 362:516 (узгоджено 03.10): фон градієнт #062814 → #11322E, картки #0F0F10,
    // чіпи #003926, навбар лишається світлим (#F1F0F1) з темно-зеленою вибраною вкладкою.
    // docs/design-tokens-figma.md, розділ A (темна колонка): градієнт, плями, текст, картки, чіпи, рамки.
    backgroundTop = Color(0xFF0F0F10),
    backgroundBottom = Color(0xFF03190C),
    // Розмиті кола прибрано за запитом користувача — лишився лише градієнт (null = не малюються).
    backgroundBlobTopRight = null,
    backgroundBlobBottomLeft = null,
    ink = Color(0xFFFEFFEF),
    textSecondary = Color(0xFFC5DCD9), // текст повідомлення в image 1
    brand = Color(0xFFFEFFEF),
    surfaceBrandLight = Color(0x24FFFFFF),
    switchTrackOff = Color(0x24FFFFFF),
    card = Color(0xFF0F0F10),
    cardActive = Color(0x24FFFFFF),
    listItem = Color(0x24FFFFFF),
    primaryFill = Color(0xFFFEFFEF),
    onPrimary = Color(0xFF062924),
    chipFill = Color(0x24FFFFFF),
    chipContent = Color(0xFFFEFFEF),
    // Figma node 408:2185: surface/card #505050 + border/default #767676 (get_variable_defs).
    labelChipFill = Color(0xFF505050),
    labelChipBorder = Color(0xFF767676),
    iconButtonFill = Color(0x24FFFFFF),
    iconButtonContent = Color(0xFFFEFFEF),
    moreTimeFill = Color(0xFF71CCA4),
    // Темний навбар: той самий скляний контейнер; вибрана вкладка — кремова (як основна кнопка темної теми),
    // невибрані — темна картка. Токенів темного фрейму в Figma немає — підібрано під палітру темної теми.
    navPill = Color(0x4DFFFFFF),
    navSelected = Color(0xFFFEFFEF),
    navSelectedContent = Color(0xFF062924),
    navUnselectedContent = Color(0xFFFEFFEF),
    navTabUnselected = Color(0x24FFFFFF),
    pagerDotActive = Color(0xFFFEFFEF),
    pagerDotInactive = Color(0xFFFEFFEF).copy(alpha = 0.3f),
    barFrame = Color(0xFF0F0F10),
    barFrameBorder = Color(0xFF767676),
    barTrack = Color(0x24FFFFFF),
    barHatchStripe = Color(0x24FFFFFF),
    barRestSegment = Color(0xFF71CCA4),
    barTargetLine = Color(0xFFF5C401),
    barTargetFill = Color(0x33F5C401),
    dialogSurface = Color(0xFF1B1B1D),
    textPrimary = Color(0xFFFFFFFF),
    textOnDark = Color(0xFFFFFFFF),
    textPlaceholder = Color(0xFFA7A7A7),
    textDisabled = Color(0x24FFFFFF),
    textError = Color(0xFFFF5A61),
    borderDefault = Color(0x24FFFFFF), // FFFFFF 14%
    borderStrong = Color(0xFF888888),
    borderError = Color(0xFFFF5A61),
    borderBrand = Color(0xFF26A377),
    lightSystemBars = false,
    heroSurface = Color(0x4D575757), // Figma 721:23487: #575757 @30% (напівпрозорий — крізь нього видно фон екрана)
    heroOnLight = Color(0xFF003926),
    heroBorder = Color(0x24FFFFFF), // FFFFFF 14%
    heroTrack = Color(0x2EFFFFFF), // FFFFFF 18%
    addTimeTonal = Color(0x38DCF6ED) // DCF6ED 22%
)

/**
 * «Світанок» (варіант 2 з макета 31.10.2026): небо за порою доби, скляні картки, персиковий акцент. Кольори залежать
 * лише від годинника, а не від даних людини (жодного оцінювання). Склад: градієнт фону, текст, поверхні, навбар, hero.
 */
private val DawnInkLight = Color(0xFF3A2340)
private val DawnPeach = Color(0xFFFF9F6B)

private val DawnLightBase = TeperaColors(
    isDark = false,
    backgroundTop = Color(0xFFFFD9C0),
    backgroundBottom = Color(0xFFD7EBFF),
    ink = DawnInkLight,
    textSecondary = Color(0xFF5E4262),
    brand = Color(0xFF8F3A14),
    switchTrackOff = Color(0x333A2340),
    surfaceBrandLight = Color(0x99FFFFFF),
    card = Color(0x8CFFFFFF),
    cardActive = Color(0xCCFFFFFF),
    listItem = Color(0x99FFFFFF),
    primaryFill = DawnPeach,
    onPrimary = Color(0xFF3A1D0D),
    chipFill = Color(0xA6FFFFFF),
    chipContent = DawnInkLight,
    labelChipFill = Color(0xB3FFFFFF),
    labelChipBorder = Color(0xFFFFFFFF),
    iconButtonFill = Color(0xB0FFFFFF),
    iconButtonContent = DawnInkLight,
    moreTimeFill = Color(0xFFFFD9C0),
    navPill = Color(0x80FFFFFF), // білий 50% (за запитом)
    navSelected = DawnInkLight,
    navSelectedContent = Color.White,
    navUnselectedContent = Color(0xFF5E4262),
    navTabUnselected = Color(0x59FFFFFF),
    pagerDotActive = DawnInkLight,
    pagerDotInactive = DawnInkLight.copy(alpha = 0.25f),
    barFrame = Color(0xB3FFFFFF),
    barFrameBorder = Color(0x99FFFFFF),
    barTrack = Color(0x1F3A2340),
    barHatchStripe = Color(0x80FFFFFF),
    barRestSegment = Color(0xFFFFB78F),
    barTargetLine = Color(0xFFF5C401),
    barTargetFill = Color(0x33F5C401),
    dialogSurface = Color(0xFFFFF3EC),
    textPrimary = DawnInkLight,
    textOnDark = DawnInkLight,
    textPlaceholder = Color(0xFF6E5472),
    textDisabled = Color(0xFF9E8AA2),
    textError = Color(0xFFB3261E),
    borderDefault = Color(0x663A2340),
    borderStrong = DawnInkLight,
    borderError = Color(0xFFB3261E),
    borderBrand = Color(0xFF8F3A14),
    lightSystemBars = true,
    heroSurface = Color(0xA6FFFFFF),
    heroOnLight = DawnInkLight,
    heroBorder = Color(0xE6FFFFFF),
    heroTrack = Color(0x223A2340),
    addTimeTonal = Color(0xB3FFFFFF),
    isDawn = true,
    heroCta = DawnPeach,
    heroCtaText = Color(0xFF3A1D0D)
)

private val DawnDarkBase = TeperaColors(
    isDark = true,
    backgroundTop = Color(0xFF1B1B3A),
    backgroundBottom = Color(0xFF11322E),
    ink = Color(0xFFFEFFEF),
    textSecondary = Color(0xFFC9BFE0),
    brand = Color(0xFFFFB78F),
    switchTrackOff = Color(0x2EFFFFFF),
    surfaceBrandLight = Color(0x24FFFFFF),
    card = Color(0x14FFFFFF),
    cardActive = Color(0x26FFFFFF),
    listItem = Color(0x1FFFFFFF),
    primaryFill = DawnPeach,
    onPrimary = Color(0xFF2B1304),
    chipFill = Color(0x1FFFFFFF),
    chipContent = Color(0xFFFEFFEF),
    labelChipFill = Color(0x1FFFFFFF),
    labelChipBorder = Color(0x2EFFFFFF),
    iconButtonFill = Color(0x1FFFFFFF),
    iconButtonContent = Color(0xFFFEFFEF),
    moreTimeFill = Color(0x40FFB78F),
    navPill = Color(0x1FFFFFFF),
    navSelected = Color(0xFFFEFFEF),
    navSelectedContent = Color(0xFF2A1F45),
    navUnselectedContent = Color(0xFFC9BFE0),
    navTabUnselected = Color(0x14FFFFFF),
    pagerDotActive = Color(0xFFFEFFEF),
    pagerDotInactive = Color(0xFFFEFFEF).copy(alpha = 0.3f),
    barFrame = Color(0x14FFFFFF),
    barFrameBorder = Color(0x2EFFFFFF),
    barTrack = Color(0x24FFFFFF),
    barHatchStripe = Color(0x24FFFFFF),
    barRestSegment = DawnPeach,
    barTargetLine = Color(0xFFF5C401),
    barTargetFill = Color(0x33F5C401),
    dialogSurface = Color(0xFF2A2342),
    textPrimary = Color(0xFFFEFFEF),
    textOnDark = Color(0xFFFEFFEF),
    textPlaceholder = Color(0xFFB4A9CC),
    textDisabled = Color(0x40FFFFFF),
    textError = Color(0xFFFF8A80),
    borderDefault = Color(0x40FFFFFF),
    borderStrong = Color(0xFFFEFFEF),
    borderError = Color(0xFFFF8A80),
    borderBrand = Color(0xFFFFB78F),
    lightSystemBars = false,
    heroSurface = Color(0x29FFFFFF),
    heroOnLight = Color(0xFF2A1F45),
    heroBorder = Color(0x40FFFFFF),
    heroTrack = Color(0x2EFFFFFF),
    addTimeTonal = Color(0x38FFB78F),
    isDawn = true,
    heroCta = DawnPeach,
    heroCtaText = Color(0xFF2B1304)
)

/** Небо «Світанку» за годиною: ранок 5–10, день 11–16, вечір 17–21, ніч 22–4. Зверху вниз. */
private fun dawnSky(dark: Boolean, hour: Int): List<Color> = when (hour) {
    in 5..10 -> if (dark) listOf(0xFF2B2A4A, 0xFF4F3556, 0xFF2E3B3A) else listOf(0xFFFFEFD0, 0xFFFFD7B5, 0xFFF9C9D6, 0xFFE3D6F7)
    in 11..16 -> if (dark) listOf(0xFF12304A, 0xFF15434F, 0xFF11322E) else listOf(0xFFDDF1FF, 0xFFCFEAF6, 0xFFD7F3E6, 0xFFF4F0D8)
    in 17..21 -> if (dark) listOf(0xFF1B1B3A, 0xFF3A2A55, 0xFF11322E) else listOf(0xFFFFD9C0, 0xFFF7C6D9, 0xFFCFC6F5, 0xFFD7EBFF)
    else -> if (dark) listOf(0xFF0E1226, 0xFF1B1B3A, 0xFF0B1F1C) else listOf(0xFFD9D6F2, 0xFFC3BFE8, 0xFFB2B6E0, 0xFFCFE0F2)
}.map { Color(it) }

/** Набір «Світанок» для світлої/темної теми й години доби. */
fun dawnColors(dark: Boolean, hour: Int): TeperaColors {
    val sky = dawnSky(dark, hour)
    return (if (dark) DawnDarkBase else DawnLightBase).copy(
        backgroundTop = sky.first(), backgroundBottom = sky.last(), backgroundStops = sky
    )
}

// ---------- Тема «За часом доби» (Налаштування → Загальні → Тема) ----------

/** «Поточний» настрій, ступінь 3 (вночі): темніший за [RedesignDarkColors] — майже чорний фон, приглушений текст. */
val RedesignNightColors = RedesignDarkColors.copy(
    backgroundTop = Color(0xFF050506),
    backgroundBottom = Color(0xFF010603),
    card = Color(0xFF09090A),
    cardActive = Color(0x1AFFFFFF),
    listItem = Color(0x1AFFFFFF),
    chipFill = Color(0x1AFFFFFF),
    iconButtonFill = Color(0x1AFFFFFF),
    barFrame = Color(0xFF09090A),
    dialogSurface = Color(0xFF111113),
    textSecondary = Color(0xFFA9BFBC),
    heroSurface = Color(0x33454545)
)

/** Ступені теми за часом для «Поточного» настрою: день — світла, вечір — темна, ніч — найтемніша. */
enum class TimePhase { DAY, EVENING, NIGHT }

/** День 05:00–19:00, вечір 19:00–23:00, ніч 23:00–05:00. */
fun timePhaseAt(minuteOfDay: Int): TimePhase = when {
    minuteOfDay < 5 * 60 -> TimePhase.NIGHT
    minuteOfDay < 19 * 60 -> TimePhase.DAY
    minuteOfDay < 23 * 60 -> TimePhase.EVENING
    else -> TimePhase.NIGHT
}

fun classicTimeColors(minuteOfDay: Int): TeperaColors = when (timePhaseAt(minuteOfDay)) {
    TimePhase.DAY -> RedesignLightColors
    TimePhase.EVENING -> RedesignDarkColors
    TimePhase.NIGHT -> RedesignNightColors
}

private val DawnNightBase = DawnDarkBase.copy(
    card = Color(0x0FFFFFFF),
    listItem = Color(0x18FFFFFF),
    heroSurface = Color(0x22FFFFFF),
    dialogSurface = Color(0xFF1B1730),
    textSecondary = Color(0xFFA99FC4),
    textPlaceholder = Color(0xFF9388AD)
)

private fun sky(vararg argb: Long): List<Color> = argb.map { Color(it) }

private val SkyNight = sky(0xFF070A18, 0xFF0E1226, 0xFF0A1424, 0xFF06120F)
private val SkyMorning = sky(0xFFFFEFD0, 0xFFFFD7B5, 0xFFF9C9D6, 0xFFE3D6F7)
private val SkyDay = sky(0xFFDDF1FF, 0xFFCFEAF6, 0xFFD7F3E6, 0xFFF4F0D8)
private val SkyEveningLight = sky(0xFFFFD9C0, 0xFFF7C6D9, 0xFFCFC6F5, 0xFFD7EBFF)
private val SkyEveningDark = sky(0xFF1B1B3A, 0xFF2A2147, 0xFF3A2A55, 0xFF11322E)

/** Небо за хвилиною доби: повільне перетікання між опорними кадрами (повертається до ночі наприкінці доби). */
private fun skyAt(minute: Int): List<Color> {
    // Захід сонця тримається до 18:30 і темніє за ~45 хв, щоб небо швидко проходило середню яскравість, на якій
    // текст не читається (момент зміни кольору тексту — 18:50–19:00, див. [uiDarkness]).
    val frames = listOf(
        0 to SkyNight, 4 * 60 to SkyNight, 6 * 60 to SkyMorning, 11 * 60 to SkyDay,
        17 * 60 to SkyEveningLight, 18 * 60 + 30 to SkyEveningLight, 19 * 60 + 15 to SkyEveningDark,
        23 * 60 to SkyNight, 24 * 60 to SkyNight
    )
    val i = frames.indexOfLast { it.first <= minute }.coerceIn(0, frames.size - 2)
    val (t0, a) = frames[i]
    val (t1, b) = frames[i + 1]
    val f = ((minute - t0).toFloat() / (t1 - t0)).coerceIn(0f, 1f)
    return a.indices.map { lerpColor(a[it], b[it], f) }
}

/**
 * Ступінь «темряви» інтерфейсу: 0 — світлий, 1 — вечірній, 2 — нічний. Переходи тексту/поверхонь короткі (10–15 хв)
 * і припадають на момент, коли небо пройшло ~55% шляху між світлим і темним, щоб текст на небі не «тонув» (перевірено
 * на пристрої: при зміні тексту о 19:00–19:20, коли небо вже темне, заголовки зникали).
 */
private fun uiDarkness(minute: Int): Float {
    fun ramp(from: Int, to: Int) = ((minute - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    return when {
        minute < 5 * 60 + 10 -> 2f
        minute < 5 * 60 + 25 -> 2f * (1f - ramp(5 * 60 + 10, 5 * 60 + 25))
        minute < 18 * 60 + 50 -> 0f
        minute < 19 * 60 -> ramp(18 * 60 + 50, 19 * 60)
        minute < 22 * 60 + 30 -> 1f
        minute < 23 * 60 + 30 -> 1f + ramp(22 * 60 + 30, 23 * 60 + 30)
        else -> 2f
    }
}

/** «Світанок» + тема «За часом»: небо й кольори інтерфейсу плавно змінюються протягом доби. */
fun dawnColorsForMinute(minute: Int): TeperaColors {
    val d = uiDarkness(minute)
    val ui = if (d <= 1f) lerpTeperaColors(DawnLightBase, DawnDarkBase, d) else lerpTeperaColors(DawnDarkBase, DawnNightBase, d - 1f)
    val skyColors = skyAt(minute)
    return ui.copy(backgroundTop = skyColors.first(), backgroundBottom = skyColors.last(), backgroundStops = skyColors)
}

private fun lerpTeperaColors(a: TeperaColors, b: TeperaColors, f: Float): TeperaColors {
    if (f <= 0f) return a
    if (f >= 1f) return b
    fun l(x: Color, y: Color) = lerpColor(x, y, f)
    return a.copy(
        isDark = if (f < 0.5f) a.isDark else b.isDark,
        lightSystemBars = if (f < 0.5f) a.lightSystemBars else b.lightSystemBars,
        ink = l(a.ink, b.ink), textSecondary = l(a.textSecondary, b.textSecondary), brand = l(a.brand, b.brand),
        switchTrackOff = l(a.switchTrackOff, b.switchTrackOff), surfaceBrandLight = l(a.surfaceBrandLight, b.surfaceBrandLight),
        card = l(a.card, b.card), cardActive = l(a.cardActive, b.cardActive), listItem = l(a.listItem, b.listItem),
        primaryFill = l(a.primaryFill, b.primaryFill), onPrimary = l(a.onPrimary, b.onPrimary),
        chipFill = l(a.chipFill, b.chipFill), chipContent = l(a.chipContent, b.chipContent),
        labelChipFill = l(a.labelChipFill, b.labelChipFill), labelChipBorder = l(a.labelChipBorder, b.labelChipBorder),
        iconButtonFill = l(a.iconButtonFill, b.iconButtonFill), iconButtonContent = l(a.iconButtonContent, b.iconButtonContent),
        moreTimeFill = l(a.moreTimeFill, b.moreTimeFill), navPill = l(a.navPill, b.navPill),
        navSelected = l(a.navSelected, b.navSelected), navSelectedContent = l(a.navSelectedContent, b.navSelectedContent),
        navUnselectedContent = l(a.navUnselectedContent, b.navUnselectedContent), navTabUnselected = l(a.navTabUnselected, b.navTabUnselected),
        pagerDotActive = l(a.pagerDotActive, b.pagerDotActive), pagerDotInactive = l(a.pagerDotInactive, b.pagerDotInactive),
        barFrame = l(a.barFrame, b.barFrame), barFrameBorder = l(a.barFrameBorder, b.barFrameBorder),
        barTrack = l(a.barTrack, b.barTrack), barHatchStripe = l(a.barHatchStripe, b.barHatchStripe),
        barRestSegment = l(a.barRestSegment, b.barRestSegment), barTargetLine = l(a.barTargetLine, b.barTargetLine),
        barTargetFill = l(a.barTargetFill, b.barTargetFill), dialogSurface = l(a.dialogSurface, b.dialogSurface),
        textPrimary = l(a.textPrimary, b.textPrimary), textOnDark = l(a.textOnDark, b.textOnDark),
        textPlaceholder = l(a.textPlaceholder, b.textPlaceholder), textDisabled = l(a.textDisabled, b.textDisabled),
        textError = l(a.textError, b.textError), borderDefault = l(a.borderDefault, b.borderDefault),
        borderStrong = l(a.borderStrong, b.borderStrong), borderError = l(a.borderError, b.borderError),
        borderBrand = l(a.borderBrand, b.borderBrand), heroSurface = l(a.heroSurface, b.heroSurface),
        heroOnLight = l(a.heroOnLight, b.heroOnLight), heroBorder = l(a.heroBorder, b.heroBorder),
        heroTrack = l(a.heroTrack, b.heroTrack), addTimeTonal = l(a.addTimeTonal, b.addTimeTonal),
        heroCta = l(a.heroCta, b.heroCta), heroCtaText = l(a.heroCtaText, b.heroCtaText)
    )
}

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
        onBackground = colors.textPrimary,
        surface = colors.card,
        onSurface = colors.textPrimary,
        onSurfaceVariant = colors.textSecondary
    )
    CompositionLocalProvider(LocalTeperaColors provides colors) {
        MaterialTheme(colorScheme = scheme, shapes = MaterialTheme.shapes, typography = MaterialTheme.typography) {
            CompositionLocalProvider(LocalContentColor provides colors.textPrimary, content = content)
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

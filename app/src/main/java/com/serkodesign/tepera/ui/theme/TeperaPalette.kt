package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Фаза 6: палітра й типографіка перенесені з Figma-фрейму "Everyday_Designs" (Home screen,
 * node 1930:233) — конкретні значення, не загальна тема застосунку. Тримаються окремо від
 * MaterialTheme.colorScheme/typography (Theme.kt), а не як розширення ColorScheme: це значення
 * лише для нового вигляду Home/Статистика, а не перепроєктування всієї теми застосунку — інші
 * екрани (Налаштування, Категорії, Додати активність) свідомо лишаються на дефолтній Material 3
 * темі, доки для них немає окремого дизайну.
 *
 * Шрифти з фрейму — "Lora" (заголовки) і "Satoshi Variable" (текст) — жоден не додається як
 * реальна залежність: Lora замінена системним FontFamily.Serif (той самий "сериф" характер без
 * Google Fonts Downloadable API — там потрібен сертифікат Google Play Services, копіювати його
 * наосліп з пам'яті занадто ризиковано), Satoshi замінений дефолтним sans (пропрієтарний шрифт,
 * недоступний через Google Fonts).
 */
object TeperaPalette {
    val backgroundBase = Color(0xFFBEDAC9) // bg-[#bed9c9] у фреймі
    val backgroundPeachBlob = Color(0xFFFFB58A)
    val backgroundLavenderBlob = Color(0xFFC7BFE8)

    val offlineCard = Color(0x8000C567) // rgba(0,197,103,0.5) — лишається лише для addButtonBackground
    val onlineCard = Color(0x80FF9162) // rgba(255,145,98,0.5)

    // SRS v2.5, розділ 4.4: "теплий сірий" для нейтрального сегмента "Решта дня" — НЕ traffic-
    // light колір (не червоний/зелений), не читається як оцінка (FR-4.3).
    val restOfDayCard = Color(0xFFDCD5C6)

    val cardTranslucent = Color(0x80FFFFFF) // rgba(255,255,255,0.5)
    val cardTranslucentLight = Color(0x4DFFFFFF) // rgba(255,255,255,0.3) — обгортка "Life balance"
    val cardActive = Color(0xFFFFFFFF)

    val navPill = Color(0x80FFFFFF)
    val addButtonBackground = offlineCard // за запитом: той самий зелений, що й Offline-блок балансу

    // Оновлений нижній навбар (Home screen, node 1951:4017): суцільна темно-зелена "таблетка"
    // замість напівпрозорої білої, вибрана вкладка підсвічена світлішим напівпрозорим кружком.
    val navPillDark = Color(0xB2073634) // rgba(7,54,52,0.7)
    val navPillSelectedHighlight = Color(0x40FFFFFF) // rgba(255,255,255,0.25)

    // Фрейми Settings/Categories/Excluded apps/Backup and restore (node 1951:909 сторінка) —
    // фірмовий темно-зелений для іконок-навігаторів у Налаштуваннях (Категорії/Виключені
    // застосунки/Резервне копіювання) і для ввімкненого стану перемикачів (той самий колір,
    // що на toggle track у фреймі, #005e3e).
    val brandAccent = Color(0xFF005E3E)
    val brandAccentSoft = Color(0x1A005E3E) // rgba(0,94,62,0.1) — фон кружка-іконки
    val switchTrackOff = Color(0x80FFFFFF) // той самий "скляний" відтінок, що інші картки

    val headlineFont: FontFamily = FontFamily.Serif
}

/**
 * Три м'які радіальні "плями" поверх базового кольору — наближення до трьох розмитих еліпсів
 * фрейму (Ellipse3/4/5), без імпорту важких blurred PNG-асетів: для декоративного фону градієнта
 * такого наближення досить, точна відповідність пікселя тут не критична.
 */
fun Modifier.teperaGradientBackground(): Modifier = this
    .fillMaxSize()
    .background(TeperaPalette.backgroundBase)
    .drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(TeperaPalette.backgroundPeachBlob.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.75f, size.height * 0.1f),
                radius = size.width * 0.95f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(TeperaPalette.backgroundLavenderBlob.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.68f),
                radius = size.width * 1.15f
            )
        )
    }

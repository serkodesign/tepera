package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.serkodesign.tepera.R

/**
 * Фаза 6: палітра й типографіка перенесені з Figma-фрейму "Everyday_Designs" (Home screen,
 * node 1930:233) — конкретні значення, не загальна тема застосунку. Тримаються окремо від
 * MaterialTheme.colorScheme/typography (Theme.kt), а не як розширення ColorScheme: це значення
 * лише для нового вигляду Home/Статистика, а не перепроєктування всієї теми застосунку — інші
 * екрани (Налаштування, Категорії, Додати активність) свідомо лишаються на дефолтній Material 3
 * темі, доки для них немає окремого дизайну.
 *
 * **Виправлено (за прямим запитом користувача):** заголовки досі рендерились системним
 * `FontFamily.Serif` — засічковий шрифт, хоча в актуальному Figma-документі заголовки набрані
 * шрифтом **Golos Text** (без засічок). Помилка сталась через застарілий висновок попередньої
 * сесії, що фрейм використовує "Lora". `res/font/golos_text.ttf` — офіційний варіативний файл із
 * репозиторію `google/fonts` (ліцензія OFL, текст — `docs/licenses/golos-text-OFL.txt`), НЕ через
 * Downloadable Fonts API — той підхід і раніше відхилявся через ризик сертифіката Google Play
 * Services, а пряме бандлення файлу шрифту цього ризику взагалі не має. "Satoshi" (текст, не
 * заголовки) лишається дефолтним sans — про це запиту не було.
 */
object TeperaPalette {
    val backgroundBase = Color(0xFFA7D2B0) // Figma "App concept", node 274:531 — база фону
    val backgroundCreamBlob = Color(0xFFFFFFB1) // Ellipse 6 (50%)
    val backgroundGreenBlob = Color(0xFF004C51) // Ellipse 7 (20%)

    val offlineCard = Color(0x8000C567) // rgba(0,197,103,0.5) — лишається лише для addButtonBackground
    val onlineCard = Color(0xFFF5C401) // #F5C401 — новий колір Online (палітра категорій), непрозорий

    // Легкий зелений для сегмента "Решта дня" (за запитом користувача) — попередній "теплий
    // сірий" (0xFFDCD5C6) занадто зливався з напівпрозорою карткою поверх градієнтного фону,
    // через що сегмент (найбільша частина шкали) практично не було видно. Це звичайний
    // категорійний колір сегмента, як і решта на шкалі (Online, кожна категорія), не умовна
    // traffic-light оцінка — колір завжди той самий, незалежно від значення (FR-4.3 лишається
    // чинним).
    val restOfDayCard = Color(0xFFC5E2CB) // #C5E2CB — колір "Офлайн-життя" (палітра категорій)

    val cardTranslucent = Color(0x80FFFFFF) // rgba(255,255,255,0.5)
    val cardTranslucentLight = Color(0x4DFFFFFF) // rgba(255,255,255,0.3) — обгортка "Life balance"
    val cardActive = Color(0xFFFFFFFF)

    val navPill = Color(0x80FFFFFF)
    val addButtonBackground = offlineCard // за запитом: той самий зелений, що й Offline-блок балансу

    // Навбар (Home/Diary/Stats), node 2146:320 — заміна попередньої темно-зеленої "таблетки"
    // (node 1951:4017, `navPillDark`/`navPillSelectedHighlight` нижче, лишені як історія
    // рішення на випадок відкату) на білу картку з вибраною вкладкою на м'якому м'ятному
    // фоні — точні токени фрейму (get_variable_defs): Surface/surface-card, Brand/200, Brand/800.
    val navPillCard = Color(0xFFFFFFFF)
    val navPillSelected = Color(0xFFB2E5D3) // Brand/200
    val navPillSelectedContent = Color(0xFF003926) // Brand/800 — текст+іконка вибраної вкладки
    val navPillUnselectedIcon = Color(0xFF505050) // Text/text-secondary — іконка невибраної вкладки

    val navPillDark = Color(0xB2073634) // rgba(7,54,52,0.7) — попередній варіант, не використовується
    val navPillSelectedHighlight = Color(0x40FFFFFF) // rgba(255,255,255,0.25) — попередній варіант

    // Фрейми Settings/Categories/Excluded apps/Backup and restore (node 1951:909 сторінка) —
    // фірмовий темно-зелений для іконок-навігаторів у Налаштуваннях (Категорії/Виключені
    // застосунки/Резервне копіювання) і для ввімкненого стану перемикачів (той самий колір,
    // що на toggle track у фреймі, #005e3e).
    val brandAccent = Color(0xFF005E3E)
    val brandAccentSoft = Color(0x1A005E3E) // rgba(0,94,62,0.1) — фон кружка-іконки
    // Вимкнений перемикач за Material 3: суцільний світлий трек + рамка й ручка кольору outline (раніше —
    // напівпрозорий білий трек, біла ручка й без рамки, тож на "скляному" рядку майже зникав). Outline
    // #006944 — брендовий, контраст з треком/фоном значно вищий за 3:1 (WCAG 1.4.11).
    val switchTrackOff = Color(0xFFE3EAE5)
    val switchOutlineOff = Color(0xFF006944) // брендовий (buttonBrand) замість сірого, за запитом користувача

    // Кнопки дизайн-системи (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 190:639):
    // Surface/surface-brand + Text/text-brand (#006944) і Surface/surface-brand-dark + Text/
    // text-brand-dark (#003926). Свідомо ІНШІ відтінки, ніж brandAccent (#005E3E) вище.
    val surfaceBrandLight = Color(0xFFDCF6ED) // Figma Surface/surface-brand-light — чіпи записів Щоденника
    val buttonBrand = Color(0xFF006944)
    val buttonBrandDark = Color(0xFF003926)

    // Home (Figma "App concept", node 192:726): картка категорії й кнопки в шапці.
    val activityCardIdle = Color(0xB3EBFAE6) // #EBFAE6 з непрозорістю 70% (за запитом користувача)
    val activityCardActive = Color(0xFF006944)
    val activityMoreTime = Color(0xFFB4D8BC)
    val headerButtonFill = Color(0x80FFFFFF) // білий 50% (було 80% за Figma; змінено за запитом користувача)
    val navTabIdleFill = Color(0xFFF0F3F4) // Surface/surface-background — невибрана вкладка навбару
    val navTabSelectedContent = Color(0xFFDCF6ED) // Surface/surface-brand-light — текст/іконка вибраної
    val homeCardFill = Color(0xB3FFFFFF) // білий 70% (було 65% за Figma; за запитом користувача) — Патерн / Цей тиждень
    val homeCardFillMyDay = Color(0xB3FFFFFF) // білий 70% (було 50% за Figma; за запитом користувача) — Твій день

    // "Новий екран додавання активності" (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node
    // 61:3516) — фіолетовий акцент лише для чіпів часу (Початок/Фініш, підсумок тривалості),
    // точні токени фрейму (#220d99 текст/рамка, rgba(34,13,153,0.05) фон, rgba(34,13,153,0.3)
    // рамка). Не той самий colorAccent, що фірмовий зелений brandAccent — свідомо інший відтінок
    // САМЕ для значень часу, за дизайном фрейму.
    val timeChipText = Color(0xFF006944)
    val timeChipBackground = Color(0x0D006944)
    val timeChipBorder = Color(0x4D006944)

    // Тепловий патерн доби, редизайн за Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR, node
    // 154:287, "Day usage") — дискретні кошики хвилин/годину замість неперервної альфа-шкали
    // попередньої версії (яка перевикористовувала onlineCard, інший відтінок). Точний бурштиновий
    // з фрейму, НЕ той самий колір, що onlineCard (#FF9162) — свідомо інший, за дизайном.
    val heatmapAmber = Color(0xFFE69D00)
    // За прямим запитом користувача кошик "0-15" — окремий колір (не найсвітліша альфа amber
    // з макета), непрозорість 100%.
    val heatmapLowBucket = Color(0xFFC3C3C3)
    val heatmapNoDataFill = Color(0xFFF3F3F3)
    val heatmapNoDataBorder = Color(0xFFD4DADD)

    val headlineFont: FontFamily = FontFamily(
        Font(R.font.golos_text, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.golos_text, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.golos_text, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
    )
}

/**
 * Фон застосунку — Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 274:531 (Home screen): база
 * `#A7D2B0` і два розмиті кола (Ellipse 6 — кремове `#FFFFB1` @50%, зверху праворуч; Ellipse 7 —
 * темно-зелене `#004C51` @20%, знизу ліворуч; радіус 302, розмиття σ = 97.55). Положення взято з
 * кадру 375x812, 1px = 1dp: центр кремового кола — 6dp від правого й 20dp від верхнього краю,
 * темно-зеленого — 9dp від лівого й 31dp від нижнього. Гауссове розмиття наближено радіальним
 * градієнтом ([drawBlurredBlob]) — працює однаково на всіх API-рівнях (26+), без RenderEffect.
 * Замінює попередній градієнт (персикова й лавандова плями) на ВЕСЬ застосунок за запитом користувача.
 */
fun Modifier.teperaGradientBackground(): Modifier = this
    .fillMaxSize()
    .background(TeperaPalette.backgroundBase)
    .drawBehind {
        val d = density
        drawBlurredBlob(
            center = Offset(size.width - 6f * d, 20f * d),
            radius = 302f * d, sigma = 97.55f * d,
            color = TeperaPalette.backgroundCreamBlob.copy(alpha = 0.5f)
        )
        drawBlurredBlob(
            center = Offset(9f * d, size.height - 31f * d),
            radius = 302f * d, sigma = 97.55f * d,
            color = TeperaPalette.backgroundGreenBlob.copy(alpha = 0.2f)
        )
    }

/**
 * Наближення гауссово розмитого диска: повна непрозорість до `radius - σ`, половина на самому
 * краї диска, нуль на `radius + 2σ` (за цією межею гаусс уже майже нульовий).
 */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlurredBlob(
    center: Offset,
    radius: Float,
    sigma: Float,
    color: Color
) {
    val outer = radius + 2f * sigma
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color,
                ((radius - sigma) / outer) to color,
                (radius / outer) to color.copy(alpha = color.alpha * 0.5f),
                1f to Color.Transparent
            ),
            center = center,
            radius = outer
        ),
        radius = outer,
        center = center
    )
}

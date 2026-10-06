package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
    val backgroundBase = Color(0xFFC5E2CB) // Figma "App concept", node 274:531 — база фону
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
    // docs/design-tokens-figma.md, розділ D (одна схема): #71CCA4 замість попереднього #C5E2CB.
    val restOfDayCard = Color(0xFF71CCA4)

    // За прямим запитом користувача: усі "скляні" картки/рядки (GlassRow, контекстні картки Home)
    // у світлому редизайні тепер на тій самій rgba(255,255,255,0.7), що й My day/Pattern/Weekly
    // digest вище — раніше бралась роль `card` (опаковий кремовий #EDF1E5), темну не чіпаємо.
    val cardTranslucent: Color @Composable @ReadOnlyComposable get() = themed(Color(0xB3FFFFFF)) { if (isDark) listItem else Color(0xB3FFFFFF) } // rgba(255,255,255,0.5)
    val cardTranslucentLight: Color @Composable @ReadOnlyComposable get() = themed(Color(0x4DFFFFFF)) { if (isDark) listItem else Color(0xB3FFFFFF) } // rgba(255,255,255,0.3) — обгортка "Life balance"
    /** Картки-рядки Налаштувань (GlassRow): білий 50% (за запитом), у темній — як інші картки. */
    val settingsCardFill: Color @Composable @ReadOnlyComposable get() = themed(Color(0xB3FFFFFF)) { if (isDark) listItem else Color(0xB3FFFFFF) }
    val cardActive: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFFFFFFF)) { cardActive }

    val navPill = Color(0xB3FFFFFF)
    // Легка сіра рамка (Figma Border/border-light, #DDE2E4): таб-бари, степпер орієнтира часу.
    val borderLight = Color(0xFFDDE2E4)
    val addButtonBackground = offlineCard // за запитом: той самий зелений, що й Offline-блок балансу

    // Навбар (Home/Diary/Stats), node 2146:320 — заміна попередньої темно-зеленої "таблетки"
    // (node 1951:4017, `navPillDark`/`navPillSelectedHighlight` нижче, лишені як історія
    // рішення на випадок відкату) на білу картку з вибраною вкладкою на м'якому м'ятному
    // фоні — точні токени фрейму (get_variable_defs): Surface/surface-card, Brand/200, Brand/800.
    // Скляний контейнер навбару (Figma node 395:1011, surface-card-transparent): напівпрозорий, фон видно крізь нього.
    val navPillCard: Color @Composable @ReadOnlyComposable get() = themed(Color(0x4DFFFFFF)) { navPill }
    val navPillSelected: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFB2E5D3)) { navSelected } // Brand/200
    val navPillSelectedContent: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF003926)) { navSelectedContent } // Brand/800 — текст+іконка вибраної вкладки
    val navPillUnselectedIcon: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF505050)) { navUnselectedContent } // Text/text-secondary — іконка невибраної вкладки
    // Фон невибраної таблетки навбару: світла тема — F0F3F4 з 10% на темному навбарі; темна — білий (макет 347:3151 dark).
    val navTabUnselectedFill: Color @Composable @ReadOnlyComposable get() = themed(Color(0x1AF0F3F4)) { navTabUnselected }


    // Фрейми Settings/Categories/Excluded apps/Backup and restore (node 1951:909 сторінка) —
    // фірмовий темно-зелений для іконок-навігаторів у Налаштуваннях (Категорії/Виключені
    // застосунки/Резервне копіювання) і для ввімкненого стану перемикачів (той самий колір,
    // що на toggle track у фреймі, #006944).
    val brandAccent: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF006944)) { brand }
    val brandAccentSoft: Color @Composable @ReadOnlyComposable get() = themed(Color(0x1A006944)) { brand.copy(alpha = 0.1f) } // rgba(0,94,62,0.1) — фон кружка-іконки
    // Вимкнений перемикач за Material 3: суцільний світлий трек + рамка й ручка кольору outline (раніше —
    // напівпрозорий білий трек, біла ручка й без рамки, тож на "скляному" рядку майже зникав). Outline
    // #006944 — брендовий, контраст з треком/фоном значно вищий за 3:1 (WCAG 1.4.11).
    val switchTrackOff: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFF0F3F4)) { switchTrackOff } 
    val switchOutlineOff: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF006944)) { brand } // брендовий (buttonBrand) замість сірого, за запитом користувача

    // Кнопки дизайн-системи (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 190:639):
    // Surface/surface-brand + Text/text-brand (#006944) і Surface/surface-brand-dark + Text/
    // text-brand-dark (#003926). Свідомо ІНШІ відтінки, ніж brandAccent (#006944) вище.
    val surfaceBrandLight: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFDCF6ED)) { surfaceBrandLight } // Figma Surface/surface-brand-light — чіпи записів Щоденника
    val buttonBrand: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF006944)) { brand }
    val buttonBrandDark: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF003926)) { ink }

    // Home: картка категорії й кнопки в шапці. Світла редизайн-гілка оновлена під Figma "App
    // concept" node 347:3037/3042/3052 — буквальні значення фрейму (не узагальнені ролі `card`/
    // `primaryFill`/`moreTimeFill`, які тягнуться з іншого, старішого референсу 334:41); темну
    // тему свідомо не чіпаємо (лишається на попередніх ролях).
    // За запитом: білий 70% (rgba(255,255,255,0.7)) — як картки Щоденника й Налаштувань у світлій темі.
    // Темна тема (макет 362:516): звичайна картка #1D5945, активна (таймер) #DCF6ED з темним текстом.
    val activityCardIdle: Color @Composable @ReadOnlyComposable get() = themed(Color(0xB3FFFFFF)) { if (isDark) listItem else Color(0xB3FFFFFF) }
    val activityCardActive: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF006944)) { if (isDark) Color(0xFFDCF6ED) else Color(0xFF006944) }
    // За прямим запитом (Figma 336:537 "more_time"): #C5E2CB у світлій темі; темна — #DCF6ED з прозорістю 30%.
    val activityMoreTime: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFC5E2CB)) { if (isDark) Color(0x4DDCF6ED) else Color(0xFFC5E2CB) }
    // Іконка на кнопці "додати час": у темній темі темна (на світлій #C5E2CB), у світлій — як buttonBrandDark.
    val activityMoreTimeContent: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF003926)) { if (isDark) Color(0xFFDCF6ED) else Color(0xFF003926) }
    /** Рамка 1dp ідлу-картки активності (node 347:3052: `border border-white`) — лише світлий редизайн, активна картка й легасі її не мають. */
    val activityCardIdleBorder: Color? @Composable @ReadOnlyComposable get() = LocalTeperaColors.current.let { if (it === LegacyTeperaColors) null else if (it.isDark) it.listItem else Color(0xB3FFFFFF) }
    val headerButtonFill: Color @Composable @ReadOnlyComposable get() = themed(Color(0xB3FFFFFF)) { iconButtonFill } // білий 50% (було 80% за Figma; змінено за запитом користувача)
    val homeCardFill: Color @Composable @ReadOnlyComposable get() = themed(Color(0xB3FFFFFF)) { if (isDark) listItem else card } // білий 70% (було 65% за Figma; за запитом користувача) — Патерн / Цей тиждень
    // Твій день (node 347:3166, "My day"): rgba(255,255,255,0.7) буквально — редизайн світлої теми
    // тепер теж на цьому значенні (раніше брав роль `card`, інший колір), темну не чіпаємо (лишається `card`).
    val homeCardFillMyDay: Color @Composable @ReadOnlyComposable get() = themed(Color(0xB3FFFFFF)) { if (isDark) listItem else Color(0xB3FFFFFF) }
    // Патерн доби: та сама заливка, що й "Твій день" (rgba(255,255,255,0.7)) — за запитом користувача,
    // щоб обидві картки пейджера Home виглядали однаково. Біла рамка 1dp лишається (див. нижче).
    val homeCardFillPattern: Color @Composable @ReadOnlyComposable get() = themed(Color(0xB3FFFFFF)) { if (isDark) listItem else Color(0xB3FFFFFF) }
    // Темна тема — без рамки, як "Мій день" (MyDayCard): інформаційні картки лише заливкою listItem.
    val homeCardBorderPattern: Color? @Composable @ReadOnlyComposable get() = LocalTeperaColors.current.let { if (it === LegacyTeperaColors || it.isDark) null else Color(0xB3FFFFFF) }
    // Цей тиждень (node 347:3267, "This week"): rgba(255,255,255,0.65), без рамки — лише світлий редизайн.
    val homeCardFillWeeklyDigest: Color @Composable @ReadOnlyComposable get() = themed(Color(0xB3FFFFFF)) { if (isDark) listItem else Color(0xB3FFFFFF) }

    // "Новий екран додавання активності" (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node
    // 61:3516) — фіолетовий акцент лише для чіпів часу (Початок/Фініш, підсумок тривалості),
    // точні токени фрейму (#220d99 текст/рамка, rgba(34,13,153,0.05) фон, rgba(34,13,153,0.3)
    // рамка). Не той самий colorAccent, що фірмовий зелений brandAccent — свідомо інший відтінок
    // САМЕ для значень часу, за дизайном фрейму.
    val timeChipText: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF006944)) { brand }

    // Тепловий патерн доби, редизайн за Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR, node
    // 154:287, "Day usage") — дискретні кошики хвилин/годину замість неперервної альфа-шкали
    // попередньої версії (яка перевикористовувала onlineCard, інший відтінок). Точний бурштиновий
    // з фрейму, НЕ той самий колір, що onlineCard (#FF9162) — свідомо інший, за дизайном.
    val heatmapAmber = Color(0xFFE69D00)
    // За прямим запитом користувача кошик "0-15" — окремий колір (не найсвітліша альфа amber
    // з макета), непрозорість 100%.
    val heatmapLowBucket = Color(0xFFC3C3C3)
    /** Кошик "0-1 хв": у темному редизайні #FFFFFF @14% (surface-glass), інакше @see heatmapLowBucket. */
    val heatmapEmptyBucket: Color @Composable @ReadOnlyComposable get() =
        LocalTeperaColors.current.let { if (it.isDark) Color(0x24FFFFFF) else heatmapLowBucket }
    val heatmapNoDataFill: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFF0F3F4)) { if (isDark) chipFill else Color(0xFFF0F3F4) }
    val heatmapNoDataBorder: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFD4DADD)) { if (isDark) textSecondary.copy(alpha = 0.3f) else Color(0xFFD4DADD) }

    /** Поточний набір кольорів теми — для ролей, яких нема серед іменованих токенів нижче. */
    val colors: TeperaColors @Composable @ReadOnlyComposable get() = LocalTeperaColors.current

    // Редизайн (TeperaColors.kt): ролі, що в legacy були прописані в коді як конкретні кольори.
    // Основний текст: docs/design-tokens-figma.md, п. 3 (рішення власника) — раніше повертав ink.
    val textPrimary: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF0F0F10)) { textPrimary }
    val dialogSurface: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFDCF6ED)) { dialogSurface }
    val barFrame: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFFFFFFF)) { barFrame }
    val barFrameBorder: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFDDE2E4)) { barFrameBorder }
    val barTrack: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFE2DED1)) { barTrack }
    val barHatchStripe: Color @Composable @ReadOnlyComposable get() = themed(Color(0x80FFFFFF)) { barHatchStripe }
    val barRestSegment: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF006944)) { barRestSegment }
    val barTargetLine: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFF5C401)) { barTargetLine }
    val barTargetFill: Color @Composable @ReadOnlyComposable get() = themed(Color(0x33F5C401)) { barTargetFill }
    val textOnDark: Color @Composable @ReadOnlyComposable get() = themed(Color.White) { textOnDark }
    val textPlaceholder: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF767676)) { textPlaceholder }
    val textDisabled: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFADADAD)) { textDisabled }
    val textError: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFE70F1A)) { textError }
    val borderDefault: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFDDE2E4)) { borderDefault }
    val borderStrong: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF505050)) { borderStrong }
    val borderError: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFE70F1A)) { borderError }
    val borderBrand: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF006944)) { borderBrand }
    val textSecondary: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF505050)) { textSecondary }
    // node 347:3052: ідлу-картка — "text-black", не узагальнена роль `ink`.
    val categoryName: Color @Composable @ReadOnlyComposable get() = themed(Color.Black) { textPrimary }
    val onPrimary: Color @Composable @ReadOnlyComposable get() = themed(Color.White) { onPrimary }
    // Кнопка play/pause на картці категорії (Figma node 347:3042/3052, "pause"): бейдж білий В
    // ОБОХ станах (не інверсія активна/ідлу, як припускала попередня редизайн-роль `primaryFill`/
    // `chipFill`), іконка — той самий near-black `#0F0F10` в обох станах (звірено з SVG pause/play
    // іконок, не з узагальненої ролі `onPrimary`/`chipContent`). Темну тему не чіпаємо.
    val playFill: Color @Composable @ReadOnlyComposable get() = themed(Color.White) { if (isDark) Color(0xFFDCF6ED) else Color.White }
    val playContent: Color @Composable @ReadOnlyComposable get() = themed(Color.Black) { if (isDark) onPrimary else Color(0xFF0F0F10) }
    // Активна картка — кнопка паузи: у світлій темі біла з темною іконкою (як play на ідлу), у темній — green/925 (#062924) з кремовою іконкою (ink).
    val playActiveFill: Color @Composable @ReadOnlyComposable get() = themed(Color.White) { if (isDark) Color(0xFF062924) else Color.White }
    val playActiveContent: Color @Composable @ReadOnlyComposable get() = themed(Color.Black) { if (isDark) ink else Color(0xFF0F0F10) }
    /** Біла 80% плашка всередині картки (рядки пауз, смуга активного таймера). */
    val innerSurface: Color @Composable @ReadOnlyComposable get() = themed(Color.White.copy(alpha = 0.7f)) { cardActive }
    /** Тон "оцінки" в GuessRevealRow — 8% основного тексту, як і в legacy (#003926 @ 0x14). */
    val guessFill: Color @Composable @ReadOnlyComposable get() = themed(Color(0x14003926)) { ink.copy(alpha = 0.08f) }
    val pagerDotActive: Color @Composable @ReadOnlyComposable get() = themed(Color.White) { pagerDotActive }
    val pagerDotInactive: Color @Composable @ReadOnlyComposable get() = themed(Color.White.copy(alpha = 0.5f)) { pagerDotInactive }
    val chipSurface: Color @Composable @ReadOnlyComposable get() = themed(Color.White) { chipFill }
    /** Фон текстових полів (пошук, поля редагування): F0F3F4 у світлій темі, як за запитом; у темній — chipFill. */
    // Темна — #003926 (docs/design-tokens-figma.md, розділ B), не chipFill: той тепер #505050.
    val inputSurface: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFFF0F3F4)) { if (isDark) Color(0x24FFFFFF) else Color(0xFFF0F3F4) }
    /** Чип запису Щоденника: лежить на білому рядку списку, тож у світлій темі — колір картки, а не білий. */
    val entryChipSurface: Color @Composable @ReadOnlyComposable get() = themed(Color.White) { if (isDark) chipFill else card }
    /** Рамка поля часу (Налаштування, розклад воріт): legacy #DDE2E4, у темній — приглушений другорядний текст. */
    val fieldBorder: Color @Composable @ReadOnlyComposable get() = themed(borderLight) { if (isDark) borderDefault else borderLight }
    // За прямим запитом користувача: той самий rgba(255,255,255,0.7), що решта "скляних" карток (вище).
    val cardSurface: Color @Composable @ReadOnlyComposable get() = themed(Color.White.copy(alpha = 0.7f)) { if (isDark) listItem else Color(0xB3FFFFFF) }
    /** Primary-кнопка Medium/Small: legacy — біла з #006944, редизайн — головна дія (темно-зелена / кремова). */
    val primaryButtonFill: Color @Composable @ReadOnlyComposable get() = themed(Color.White) { primaryFill }
    val primaryButtonContent: Color @Composable @ReadOnlyComposable get() = themed(Color(0xFF006944)) { onPrimary }

    /**
     * Колір, що залежить від теми: у [LegacyTeperaColors] — точне попереднє значення [legacy] (решта застосунку
     * не змінюється), у наборах редизайну — відповідне поле [TeperaColors].
     */
    @Composable
    @ReadOnlyComposable
    private inline fun themed(legacy: Color, pick: TeperaColors.() -> Color): Color {
        val colors = LocalTeperaColors.current
        return if (colors === LegacyTeperaColors) legacy else colors.pick()
    }

    val headlineFont: FontFamily = FontFamily(
        Font(R.font.golos_text, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.golos_text, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.golos_text, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
    )
}

/**
 * Фон застосунку — Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 274:531 (Home screen): база
 * `#C5E2CB` і два розмиті кола (Ellipse 6 — кремове `#FFFFB1` @50%, зверху праворуч; Ellipse 7 —
 * темно-зелене `#004C51` @20%, знизу ліворуч; радіус 302, розмиття σ = 97.55). Положення взято з
 * кадру 375x812, 1px = 1dp: центр кремового кола — 6dp від правого й 20dp від верхнього краю,
 * темно-зеленого — 9dp від лівого й 31dp від нижнього. Гауссове розмиття наближено радіальним
 * градієнтом ([drawBlurredBlob]) — працює однаково на всіх API-рівнях (26+), без RenderEffect.
 * Замінює попередній градієнт (персикова й лавандова плями) на ВЕСЬ застосунок за запитом користувача.
 */
@Composable
fun Modifier.teperaGradientBackground(): Modifier {
    val colors = LocalTeperaColors.current
    val top = colors.backgroundTop
    val bottom = colors.backgroundBottom
    // Редизайн (TeperaColors.kt): вертикальний градієнт, за запитом (node 347:3037) — з двома
    // розмитими плямами поверх, тим самим механізмом (drawBlurredBlob), що легасі-фон нижче, та
    // ж позиція/радіус/сигма (302/97.55dp), лише нові кольори набору.
    if (top != null && bottom != null) {
        var modifier = this.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))
        val blobTopRight = colors.backgroundBlobTopRight
        val blobBottomLeft = colors.backgroundBlobBottomLeft
        if (blobTopRight != null || blobBottomLeft != null) {
            modifier = modifier.drawBehind {
                val d = density
                blobTopRight?.let {
                    drawBlurredBlob(center = Offset(size.width - 6f * d, 20f * d), radius = 302f * d, sigma = 97.55f * d, color = it)
                }
                blobBottomLeft?.let {
                    drawBlurredBlob(center = Offset(9f * d, size.height - 31f * d), radius = 302f * d, sigma = 97.55f * d, color = it)
                }
            }
        }
        return modifier
    }
    return legacyGradientBackground()
}

private fun Modifier.legacyGradientBackground(): Modifier = this
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
 * Заставка при запуску — Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 294:1901: та сама
 * техніка розмитих плям, що [teperaGradientBackground], база темніша (`#8AC396`) і плям п'ять
 * замість двох (координати/радіуси/сигма — 1:1 з SVG-експортів кожного Ellipse-вузла, кут
 * округлено від найближчого краю, як і два вже наявні). Ellipse 6/7 тут — ТІ САМІ плями, що на
 * звичайному фоні застосунку (той самий колір і позиція відносно країв) — лише решта три (3/4/5)
 * унікальні для заставки. Хвилясті лінії (node 294:1904, "Group 3") домальовує окремий `Image` у
 * `SplashScreen.kt` (той самий SVG-актив, що вже лежить у проєкті як `perm_bg_waves.xml` —
 * підтверджено побайтовим порівнянням pathData/viewBox, це один і той самий Figma-вузол,
 * повторно використаний тут і в колишньому темному екрані дозволів).
 */
fun Modifier.splashGradientBackground(): Modifier = this
    .fillMaxSize()
    .background(Color(0xFF8AC396))
    .drawBehind {
        val d = density
        // Ellipse 5 — найбільша й найрозмитіша (сигма 282.7), вгорі, за правим краєм.
        drawBlurredBlob(
            center = Offset(size.width + 64f * d, -39f * d),
            radius = 302f * d, sigma = 282.7f * d,
            color = Color(0xFF65FF93).copy(alpha = 0.3f)
        )
        // Ellipse 3 — вгорі ліворуч.
        drawBlurredBlob(
            center = Offset(78f * d, 84f * d),
            radius = 277f * d, sigma = 97.55f * d,
            color = Color(0xFFFDFFD2).copy(alpha = 0.1f)
        )
        // Ellipse 6 — та сама кремова пляма, що на звичайному фоні застосунку.
        drawBlurredBlob(
            center = Offset(size.width - 6f * d, 20f * d),
            radius = 302f * d, sigma = 97.55f * d,
            color = TeperaPalette.backgroundCreamBlob.copy(alpha = 0.5f)
        )
        // Ellipse 4 — внизу ліворуч, за нижнім краєм.
        drawBlurredBlob(
            center = Offset(-58f * d, size.height + 18f * d),
            radius = 302f * d, sigma = 97.55f * d,
            color = Color(0xFFBFC1EB).copy(alpha = 0.2f)
        )
        // Ellipse 7 — та сама темно-зелена пляма, що на звичайному фоні застосунку.
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

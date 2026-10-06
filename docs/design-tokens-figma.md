# Tepera — кольори з Figma → код (v2, 6 жовтня 2026)

Джерело: Figma **App concept**, колекції `Tepera Theme` (Light/Dark), `Tepera Accents`, `Tepera Primitives`. У кожної змінної теми в Figma прописано code syntax (Android) — та сама назва, що в колонці «Kotlin». Цей файл замінює попередню версію.

## Інструкція для Claude Code

1. Набори кольорів — `ui/theme/TeperaColors.kt`: `RedesignLightColors` / `RedesignDarkColors`. Похідні кольори — `ui/theme/TeperaPalette.kt`. `LegacyTeperaColors` не чіпай.
2. Формат — Compose `0xAARRGGBB`. **Жирним** позначено значення, яке треба поставити; звичайним — те, що вже збігається.
3. **Рішення власника:** основний текст — нейтральний `#0F0F10` (світла) / білий (темна). Зараз `TeperaPalette.textPrimary` у редизайні повертає `ink`, тож додай у `TeperaColors` окреме поле `textPrimary` і поверни його з `TeperaPalette.textPrimary`. Темно-зелений `ink` (`#003926`) лишається як брендовий темний (`buttonBrandDark`) — для заголовків/акцентів, де він зараз стоїть навмисно; звичайний текст, що бере `LocalContentColor` / `onSurface` / `onBackground`, має стати `textPrimary` (див. `RedesignScope`).
4. Розділ C — нові поля: додай у `TeperaColors` (обидва набори; у legacy — розумний відповідник) і геттери в `TeperaPalette` з тими ж назвами. Потім заміни жорсткі літерали в екранах (`0xFF0F0F10`, `Color.Black` для тексту, червоні помилки тощо) на ці токени.
5. Правило пар: текст/іконка на `primaryFill` або `brand` — `onPrimary`; на `navSelected` — `navSelectedContent`. Білий `textOnDark` на цих заливках не використовувати (у темній темі вони кремові).
6. Після змін онови `docs/palette.md` і збери `./gradlew assembleDebug`.

## A. Тема: що змінити (Figma ≠ код)

| Figma | Kotlin | Світла: код → Figma | Темна: код → Figma |
|---|---|---|---|
| `bg/gradient-top` | `colors.backgroundTop` | `0xFFF9F0DF` | `0xFF062814` → **`0xFF0F0F10`** |
| `bg/gradient-bottom` | `colors.backgroundBottom` | `0xFFEDE9E8` | `0xFF11322E` → **`0xFF062814`** |
| `bg/blob-top-right` | `colors.backgroundBlobTopRight` | `0xFFFFF2D2` | немає → **`0x1AFFF2D2`** |
| `bg/blob-bottom-left` | `colors.backgroundBlobBottomLeft` | `0x4DC2BDF6` | немає → **`0x1AC2BDF6`** |
| `text/primary` | `TeperaPalette.textPrimary` | `0xFF003926` (= ink) → **`0xFF0F0F10`** | `0xFFFEFFEF` (= ink) → **`0xFFFFFFFF`** |
| `text/secondary` | `colors.textSecondary` | `0xFF767676` → **`0xFF6B6B6B`** | `0xFFC5DCD9` |
| `surface/default` | `colors.cardActive` | `0xFFFCFCFB` → **`0xFFFFFFFF`** | `0xFF1A3D38` |
| `surface/glass` | `colors.listItem / TeperaPalette.cardSurface` | `0xB3FFFFFF` | `0xFF164233` → **`0x24FFFFFF`** |
| `surface/brand-light` | `colors.surfaceBrandLight` | `0xFFFEFEFE` → **`0xFFDCF6ED`** | `0xFF1A3D38` |
| `chip/unselected` | `colors.chipFill` | `0xFFFEFEFE` | `0xFF003926` → **`0xFF1A3D38`** |
| `button/add-time` | `colors.moreTimeFill + TeperaPalette.activityMoreTime` | `0xFFFCFBF7` (moreTimeFill; activityMoreTime = 0xFFC5E2CB) → **`0xFF71CCA4`** | `0xFFC5E2CB` → **`0xFF71CCA4`** |
| `input/border` | `TeperaPalette.fieldBorder` | `0xFFDDE2E4` → **`0xFF888888`** | `0x4DC5DCD9` (textSecondary 30%) → **`0xFF767676`** |
| `card/activity-border` | `TeperaPalette.activityCardIdleBorder` | `0xFFFFFFFF` | немає → **`0x1FFFFFFF`** |
| `heatmap/zero` | `TeperaPalette.heatmapLowBucket` | `0xFFC3C3C3` → **`0xFFADADAD`** | `0xFFC3C3C3` → **`0xFF505050`** |

Нотатки:
- `surface/default` (`cardActive`): світла `#FCFCFB` → чистий білий. Різниця мізерна, можна лишити як є.
- `surface/brand-light`: у коді світла зараз `#FEFEFE`, хоча `TeperaPalette.surfaceBrandLight` у legacy і коментар кажуть `#DCF6ED`. Figma фіксує `#DCF6ED`.
- `button/add-time`: у світлому наборі `moreTimeFill = #FCFBF7`, а `activityMoreTime` повертає `#C5E2CB`. Обидва → `#71CCA4`.
- `input/border` (`fieldBorder`): світла `borderLight #DDE2E4` → `#888888`, темна — суцільний `#767676` замість `textSecondary` 30%. Причина — контраст рамки поля ≥3:1.
- `card/activity-border`: у темній темі тепер є рамка — білий 12% (`0x1FFFFFFF`) замість `null`; аналогічно варто зробити `homeCardBorderPattern`.

## B. Тема: вже збігається

| Figma | Kotlin | Світла | Темна |
|---|---|---|---|
| `text/brand-dark` | `colors.ink / TeperaPalette.buttonBrandDark` | `0xFF003926` | `0xFFFEFFEF` |
| `brand` | `colors.brand / brandAccent` | `0xFF006944` | `0xFFFEFFEF` |
| `surface/card` | `colors.card` | `0xFFEDF1E5` | `0xFF164233` |
| `button/primary` | `colors.primaryFill` | `0xFF003926` | `0xFFFEFFEF` |
| `button/on-primary` | `colors.onPrimary` | `0xFFFFFFFF` | `0xFF062924` |
| `chip/on-unselected` | `colors.chipContent` | `0xFF003926` | `0xFFFEFFEF` |
| `header/circle-button` | `colors.iconButtonFill` | `0xFFFCFBF7` | `0xFF164233` |
| `navbar/pill` | `colors.navPill` | `0x4DFFFFFF` | `0x4DFFFFFF` |
| `navbar/tab-selected` | `colors.navSelected` | `0xFF006944` | `0xFFFEFFEF` |
| `navbar/on-tab-selected` | `colors.navSelectedContent` | `0xFFDCF6ED` | `0xFF062924` |
| `navbar/tab-unselected` | `colors.navTabUnselected` | `0xFFFFFFFF` | `0xFF1A3D38` |
| `navbar/on-tab-unselected` | `colors.navUnselectedContent` | `0xFF505050` | `0xFFFEFFEF` |
| `switch/track-off` | `colors.switchTrackOff` | `0xFFF0F3F4` | `0xFF1A3D38` |
| `pager/dot` | `colors.pagerDotInactive` | `0x40003926` | `0x4DFEFFEF` |
| `input/fill` | `TeperaPalette.inputSurface` | `0xFFF0F3F4` | `0xFF003926` |

## C. Нові поля (у коді ще немає)

| Figma | Kotlin (TeperaColors / TeperaPalette) | Світла | Темна | Призначення |
|---|---|---|---|---|
| `text/primary` | `textPrimary` (поле) | `0xFF0F0F10` | `0xFFFFFFFF` | Основний текст та іконки — див. п. 3 інструкції |
| `text/on-dark` | `textOnDark` | `0xFFFFFFFF` | `0xFFFFFFFF` | Білий текст на темних фото / темно-бірюзових поверхнях. НЕ для тексту на brand / primary — там onPrimary. |
| `text/placeholder` | `textPlaceholder` | `0xFF6B6B6B` | `0xFFA7A7A7` | Плейсхолдер у полях вводу |
| `text/disabled` | `textDisabled` | `0xFFADADAD` | `0xFF505050` | Неактивний текст |
| `text/error` | `textError` | `0xFFE70F1A` | `0xFFFF5A61` | Текст помилки |
| `border/default` | `borderDefault` | `0xFF888888` | `0xFF767676` | Рамка за замовчуванням (≥3:1) |
| `border/strong` | `borderStrong` | `0xFF505050` | `0xFF888888` | Контрастна рамка |
| `border/error` | `borderError` | `0xFFE70F1A` | `0xFFFF5A61` | Рамка поля з помилкою |
| `border/brand` | `borderBrand` | `0xFF006944` | `0xFF26A377` | Брендова рамка: фокус / вибране |

## D. Accents (одна схема)

| Figma | Kotlin | Код → Figma |
|---|---|---|
| `data/offline-life` | `TeperaPalette.restOfDayCard` | `0xFFC5E2CB` → **`0xFF71CCA4`** |
| `heatmap/zero` (тема) | `TeperaPalette.heatmapLowBucket` | `0xFFC3C3C3` → **світла `0xFFADADAD` / темна `0xFF505050`** — зроби геттер themed |
| `data/online` | `TeperaPalette.onlineCard` | `0xFFF5C401` |
| `data/heatmap-peak` | `TeperaPalette.heatmapAmber` | `0xFFE69D00` |
| `data/heatmap-no-data` | `TeperaPalette.heatmapNoDataFill` | `0xFFF0F3F4` |
| `data/heatmap-no-data-border` | `TeperaPalette.heatmapNoDataBorder` | `0xFFD4DADD` |

Категорії, віджет, заставка, іконка застосунку — без змін (див. `docs/palette.md`).
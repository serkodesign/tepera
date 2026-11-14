# Tepera 1.2.2 (versionCode 42) — flavor `beta`, build: `:app:bundleBetaRelease` (copy: `release/tepera-1.2.2-42.aab`)

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Short tips walk you through the app the first time
- "Scrolling hours" moved higher on Home; during the first week you can pick your own hours right away
- Settings: one "Digital detox" section; language has its own page
- Shorter, clearer texts and tighter line spacing

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Короткі підказки проведуть через застосунок при першому запуску
- «Години скролінгу» вище на Головній; у перший тиждень можна одразу обрати свої години
- Налаштування: один розділ «Цифровий детокс»; мова на окремій сторінці
- Коротші й чіткіші тексти, менший міжрядковий інтервал

## For the developer (not for Play)
- Version: beta `1.2.1 (41)` → `1.2.2 (42)`. `standard` flavor unchanged. Room schema unchanged.
- Tooltip tutorial (`ui/tutorial`, `util/TutorialStore`, reset in Settings → "Show tips again"); Home order changed; outlined "feed apps" icon.
- i18n base: `res/values` is now ENGLISH (default), Ukrainian is `res/values-uk`; `generateLocaleConfig` on; language list in `util/AppLanguages.kt`; language chooser is a page (`LanguageSettingsScreen`). "System" language picks the first supported device locale, else English — devices in unsupported languages (e.g. Russian) now get English, previously Ukrainian.
- Body line height 1.25 (`TeperaTypography`). Shortened strings (uk/en).
- Verified on Huawei P9 (Android 8) only; light theme, English locale of every screen and other devices not checked. `lintBetaDebug` still fails on the old `QuickLogTileService` error.

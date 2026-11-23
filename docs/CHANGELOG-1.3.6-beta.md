# Tepera 1.3.6-beta (versionCode 49) — flavor `beta`, build: `:app:bundleBetaRelease` (copy: `release/tepera-1.3.6-beta-49.aab`)

## What's new (EN) — Play "Release notes", ≤ 500 characters
- 8 new widgets: next detox hour, idea instead of scrolling, running activity, "Not today", pause delays, add entry, this week, phone-free pauses
- The day-grid widget is gone
- Delayed apps now work through Accessibility only
- Widgets settings: Settings → App → Widgets
- Shorter, friendlier texts

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- 8 нових віджетів: наступна година детоксу, ідея замість скролу, активність, що триває, «Не сьогодні», пауза затримок, додати запис, цей тиждень, паузи без телефону
- Віджет із сіткою доби прибрано
- Застосунки з затримкою тепер лише через Доступність
- Налаштування віджетів: Налаштування → Застосунок → Віджети
- Коротші й дружніші тексти

## For the developer (not for Play)
- Version: beta `1.3.5-beta (48)` → `1.3.6-beta (49)`. `standard` unchanged. Room schema unchanged.
- Widgets: see CLAUDE.md, section "Віджети: нові, без 4x2 (23.11.2026)".
- Texts: Ukrainian tone unified to «ти»; explanatory texts shortened (points 1–2 of the text audit). Remaining audit points (a11y disclosure sections, privacy dedupe, backup notes, dead shortcut strings, Knowledge Base restructure) are saved in memory and not done.
- Unit tests: `LogicalDayRolloverTest.home_flow_switches_to_the_new_day…` failed once in a full run, passes alone (flaky, not widget-related).
- Checked on Huawei P9 (Android 8): widgets, taps, settings toggles, placeholders (temporary `v3=false`, reverted).

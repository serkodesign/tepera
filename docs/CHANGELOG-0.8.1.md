# Tepera 0.8.1 (versionCode 14) — build for testing

File: `app/build/outputs/bundle/release/app-release.aab` (signed with the release key; file SHA-256 starts `6a67c530…`).

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Home tiles for yesterday and the week now show Online hours, clearly labelled "online", so the number is not mistaken for something else
- A small (i) next to the day bar explains the segments and the quiet reference tick
- Statistics: a note on why Online can differ from system screen time, and how the start of the day is defined
- Clearer wording: "Delay" instead of "gates", "When you're on your phone" for the pattern card, "From your phone's data" in the weekly guess

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Плитки «Вчора» і «Тиждень» на головній тепер показують Online-години з чітким підписом «online», щоб число не сприймали як щось інше
- Біля смуги дня з'явився ⓘ: пояснює сегменти й тиху риску орієнтира
- У Статистиці — примітка, чому Online може відрізнятись від екранного часу в системі, і як визначається початок дня
- Зрозуміліші формулювання: «затримка» замість «ворота», «Коли ти з телефоном» для картки патерну, «За даними телефону» в тижневій оцінці

## For the developer (not for Play)
- Version: `0.8.0 (13)` → `0.8.1 (14)`. Room schema version unchanged (**17**), so no migration.
- Home teasers (`SummaryModel.kt`, `SummaryTeasers.kt`, `HomeScreen.kt`): `DayBar` gained `onlineMinutes`, `PeriodSummary` gained `averageOnlineMinutesPerDay`, `buildPeriod(..., todayOnline)`. `TeaserTile.Yesterday` now carries Online minutes and hourly Online; the Week/Month average and mini-bars are Online (yellow). The sheet still shows both numbers. Label and value are stacked on two lines in the tile (a single line overlapped on 1080 px).
- Wording (both locales): `summary_*_online`, `stats_online_differs_note`, `stats_day_start_note`, `target_onboarding_later_hint`, `hero_structure_info_*`; "gates" → "delay" in user-visible strings (the `gate_*` keys and code names are unchanged); `stats_last_phone_use_week_label` is now "Зазвичай востаннє брав телефон" / "Usually last on phone"; `settings_target_info` says "Online" instead of "screen time"; pattern card title is the same for both periods, with a "yesterday" subtitle for the day view.
- New `DayStructureRow` in `HomeHero.kt`: the day bar plus an (i) that opens `HomeInfoDialog`; the body mentions the reference tick only when a target is set.
- Deliberately not changed (owner decision): the "without phone" orb and Statistics row meaning, and the overlapping answer ranges ("3–5" / "5+").
- Verified on Samsung S23 (debug build): Home teasers and (i) dialog, Statistics Day and Week texts. Unit tests, `bundleRelease` and `jarsigner -verify` pass. Not verified: the release AAB was **not installed**; English locale; the target onboarding hint; the renamed "delay" strings on the delayed-apps screens; other devices.
- Known lint errors (10, unchanged): `QuickLogTileService`, `AboutScreen`, `BackupRestoreScreen`, `ProInterestScreen`.

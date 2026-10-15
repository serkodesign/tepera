# Tepera 0.7.0 (versionCode 12) — build for testing

File: `app/build/outputs/bundle/release/app-release.aab` (signed with the release key, SHA-256 `C6:BB:72:F3:…:0F:66:86`).

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Redesigned Home: a "phone-free" orb, a day-structure bar, yesterday / week / month summaries and compact activity rows
- A mini player on other tabs while an activity is running
- Unified category icons and cards across the app; dark theme refinements
- The weekly check-in now asks about your daily average; settings regrouped
- Gentle in-app update prompt and "Rate the app" in Settings
- Fix: a crash when the system failed to deliver usage events

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Новий головний екран: орб «без телефону», смуга структури дня, підсумки за вчора / тиждень / місяць і компактні рядки активностей
- Міні-плеєр на інших вкладках, поки йде активність
- Єдиний вигляд іконок і карток категорій у всьому застосунку; доопрацьована темна тема
- Тижнева рефлексія питає про середнє на день; налаштування згруповано
- М'яка пропозиція оновитись у застосунку й «Оцінити застосунок» у Налаштуваннях
- Виправлено падіння, коли система не віддавала події використання

## For the developer (not for Play)
- Version: `0.6.6 (11)` → `0.7.0 (12)`. Room schema version is **17** (`day_summaries` added after 0.6.6) — check an update **over 0.6.6** from Play on a real device (V-27): migration must not crash.
- Home no longer shows the "Pattern of yesterday" and "This week" cards (pattern lives in the summary sheet; the weekly-summary notification opens the sheet on the "Week" tab).
- Release manifest permissions: `PACKAGE_USAGE_STATS`, `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS` (+ the internal receiver permission). No `BILLING`. Compare with Data Safety before uploading.
- Not verified on devices other than Samsung S23; the release AAB itself was **not installed** (verified: unit tests, debug build on S23, `jarsigner -verify`, signer is the release key).
- Known lint errors (10, not from this work): `QuickLogTileService`, `AboutScreen`, `BackupRestoreScreen`, `ProInterestScreen`.

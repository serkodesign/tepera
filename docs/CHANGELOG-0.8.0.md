# Tepera 0.8.0 (versionCode 13) — build for testing

File: `app/build/outputs/bundle/release/app-release.aab` (signed with the release key, SHA-256 `C6:BB:72:F3:…:0F:66:86`).

## What's new (EN) — Play "Release notes", ≤ 500 characters
- A new first-run flow: a short welcome ("a tracker without judgement"), then categories, a quick guess of yesterday's online time, a day reference on a ruler, and the permission step last
- You can skip the categories step and keep the defaults
- The delayed-apps intro now shows the same breathing flower as the real pause screen
- The Home orb is alive: water moves with your phone, numbers count up, and an "Extra animations" switch turns it all off
- The widget suggestion is now a card on Home

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Новий перший запуск: короткий вступ («Трекер без суду»), категорії, здогадка про вчорашній онлайн, орієнтир на лінійці, а дозвіл — останнім кроком
- Крок категорій можна пропустити й лишити дефолтні
- Вступ про застосунки з затримкою показує ту саму «квітку», що й справжній екран паузи
- Орб на головній ожив: вода рухається разом із телефоном, числа докручуються, а перемикач «Додаткові анімації» вимикає все це
- Пропозиція віджета тепер картка на головній

## For the developer (not for Play)
- Version: `0.7.0 (12)` → `0.8.0 (13)`. Room schema version unchanged (**17**), so no migration this time.
- Onboarding order changed: Welcome → Categories → Online estimate → Target → Gates → Permissions (Permissions only when usage access is missing). `WidgetSuggestionScreen` is gone; the widget suggestion is a `WIDGET_SUGGESTION` card in the Home context stack. New `welcome_onboarding_seen` flag defaults to "seen" for installs that already passed the categories step, so existing users do not see Welcome.
- The first-launch splash now uses the current `ic_launcher_*` drawables (the old `splash_logo_*` were removed).
- Target step: the "average over 7 days" hint is gone (usage access is requested after this step now). Its ruler spans 1–8 h like Settings → Tracking; "Set" is Primary, "Later" is Secondary.
- Included since 0.7.0: living orb liquid and the "Extra animations" switch (`549a85b`), dark-theme Home hero styling (`afccebc`, `dc1e682`).
- Verified: unit tests, `bundleRelease`, `jarsigner -verify`, and the whole new onboarding on Samsung S23 (dark theme; light theme for steps 1–6). Not verified: the release AAB itself was **not installed**; other devices; English locale; the onboarding on Android < 12 (system splash).
- Known lint errors (10, unchanged): `QuickLogTileService`, `AboutScreen`, `BackupRestoreScreen`, `ProInterestScreen`.

# Tepera 1.0.4 (versionCode 19) — release for Play Store (flavor `standard`, without Accessibility)

Build: `./gradlew.bat :app:bundleStandardRelease` → `app/build/outputs/bundle/standardRelease/app-standard-release.aab`.

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Widget is easier to read on light wallpapers: denser plates, outlines and dark labels
- Taller 4x2 widget no longer stretches on large launcher grids
- Fixed the search field: Back now closes the search instead of reopening the keyboard

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Віджет краще читається на світлих шпалерах: щільніші підкладки, обвідки й темні підписи
- Високий віджет 4x2 більше не розтягується на великих сітках лаунчера
- Виправлено пошук: «Назад» тепер закриває пошук, а не знову відкриває клавіатуру

## For the developer (not for Play)
- Version: `1.0.3 (18)` → `1.0.4 (19)`; `a11y` flavor: `1.0.4-a11y (20)`. Room schema unchanged, no migration.
- Changes vs `v1.0.3` that reach `standard`: widget styling (`9e707e5`), search field focus fix (`a7dc0ba`).
- The a11y work (`12522f9`, `3b3bb8a`, `388d725`) is gated by `BuildConfig.GATE_ACCESSIBILITY = false`: no service, no
  permission, no UI. Shared-code additions (SettingsStore free opens, GateRepository, strings) are inert in `standard`.
- Not verified on a device: 4x2 height cap (P9 launcher does not report real size).

# Tepera 1.0.4 (versionCode 19) — release for Play Store (flavor `standard`, without Accessibility)

Build: `./gradlew.bat :app:bundleStandardRelease` → `app/build/outputs/bundle/standardRelease/app-standard-release.aab`.
Test build (flavor `beta`, `1.0.4-beta`, versionCode 20): `./gradlew.bat :app:assembleBetaRelease` → `app/build/outputs/apk/beta/release/`.

## What's new (EN) — Play "Release notes", ≤ 500 characters
- 7 new info widgets: Online ring, Online day bar, active now, day start, time without phone, last activity, top activities of the week
- "Categories" are now called "Activities" across the app
- Export/Import renamed to "statistics"; the screen now lists exactly what is saved
- Widget easier to read on light wallpapers; fixed search field Back button
- Clearer wording in several hints and cards
- Bottom navigation bar: frosted-glass background for better contrast

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- 7 нових інформаційних віджетів: кільце Online, структура доби, активна зараз, початок дня, час без телефону, остання активність, топ активностей тижня
- «Категорії» тепер називаються «Активності» у всьому застосунку
- Експорт/імпорт тепер «статистика»; на екрані точно сказано, що зберігається
- Віджет краще читається на світлих шпалерах; виправлено кнопку «Назад» у пошуку
- Точніші формулювання в підказках і картках
- Нижня навігація: матове скло для кращого контрасту

## Changes since the first 1.0.4 cut (`ed6fbd0`)
- **Info widgets** (`fbf38f1`): `InfoWidgets.kt`, `InfoWidgetData.kt`, `InfoWidgetRender.kt`, 7 providers (`widget_info_*.xml`) with
  generated previews (`WIDGET_PREVIEW_VERSION` 4 → 5). Refreshed from `WidgetUpdateWorker` and after timer start/stop (`ActiveTimerStore`).
- **Rename "Categories" → "Activities"** in UI strings (UA); screen title "Нова активність" → "Додати час"; the title no longer
  switches to the category name when the category is changed in the form.
- **Gate pause** (`9d1d5c5`): buttons stay above the navigation bar. **Time dialog** opens in keyboard mode.
- **Export/Import** renamed to "statistics"; the note and the import confirmation list the exact contents (activities, entries,
  excluded apps, Online target, daily Online history).
- **Text pass from the owner's spreadsheet** (`docs/app-texts.csv`): 15 UA strings reworded, plus matching EN strings
  (gate-events card in plural forms 1 / 2–4 / 5+; "Put the phone down at" labels; shorter hints; "Without judgment" wording).
- **Flavor `a11y` renamed to `beta`** (`c5f6238`): source set `app/src/beta`, version suffix `-beta`.
- **Bottom dock frosted glass:** stronger blur (24 → 40 dp), noise and a thin light edge; on Android < 12 (no blur in Haze) a dense
  theme-aware fallback tint, so content no longer shows through the dock. Verified on Huawei P9 (dark theme); light theme and real
  blur (Android 12+) not verified on a device.

## For the developer (not for Play)
- Version: `1.0.3 (18)` → `1.0.4 (19)`; `beta` flavor: `1.0.4-beta (20)`. Room schema unchanged, no migration.
- Changes vs `v1.0.3` that reach `standard`: widget styling (`9e707e5`), search field focus fix (`a7dc0ba`), plus everything above.
- The Accessibility work (`12522f9`, `3b3bb8a`, `388d725`) is gated by `BuildConfig.GATE_ACCESSIBILITY = false` in `standard`: no service,
  no permission, no UI. Shared-code additions are inert there.
- Not verified on a device: the 7 info widgets on launchers other than the ones used for development, 4x2 height cap (P9 launcher
  does not report real size), the reworded strings in the English locale, the plural forms of the gate-events card.

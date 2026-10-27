# Tepera 1.0.5 (versionCode 24) — release for Play Store (flavor `standard`, without Accessibility)

Build: `./gradlew.bat :app:bundleStandardRelease` → `app/build/outputs/bundle/standardRelease/app-standard-release.aab`.
Test build (flavor `beta`, `1.0.5-beta`, versionCode 25): `./gradlew.bat :app:assembleBetaRelease` → `app/build/outputs/apk/beta/release/`.

## What's new (EN) — Play "Release notes", ≤ 500 characters
- New calendar of activities: month and week views, with a sticker for every logged activity
- Tap a day to see its entries and edit them
- In the week view, each sticker shows its duration as h:mm (for example 1:15)
- The calendar button replaces the book icon in the Home header; the Knowledge base is still in Settings → About Tepera

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Новий календар активностей: перегляд за місяць і тиждень, для кожної записаної активності — стікер
- Торкніться дня, щоб побачити його записи й відредагувати їх
- У тижневому перегляді під стікером — тривалість у форматі г:хв (наприклад, 1:15)
- Кнопка календаря замінила іконку книги в шапці Home; База знань лишилась у Налаштування → Про Tepera

## Changes since 1.0.4 (versionCode 22)
- **Calendar** (`ui/calendar/`, opened from the Home header; route `calendar`):
  - Month grid: each day is a rounded tile, 88 dp tall. Up to 4 stickers (30 dp) overlap at the corners, 5–6 form a pile (24 dp),
    7+ show five and a "+N" chip. Today is a dark circle.
  - Week: a day label on the left and a white day card with the stickers, wrapping to new rows. Under each sticker a duration chip
    "h:mm" (Figma node 961:29840) overlaps its bottom edge.
  - Tapping a day with entries opens a bottom sheet with the day's `HistoryEntryRow` list; tapping the pencil edits an entry.
    Empty days are not clickable.
  - Stickers: one per entry; 6 shapes (circle, squircle, cookie, flower, folded note, rounded heart); category colour (40% over white)
    and the app's own category icons. Shape and tilt depend only on the entry id, so a sticker looks the same in month and week.
  - Colours come from the existing palette (`chipSurface`, `cardTranslucent`, `fieldBorder`); the Home header button uses the existing
    Figma `icon/calendar_month` (505:713).
- **Home header:** the book button (Knowledge base) is replaced by the calendar button. The Knowledge base stays in Settings → About Tepera.
- **Tests:** `CalendarModelTest` — month grid, sticker slots, shapes, glyph fit, duration format.

## For the developer (not for Play)
- Version: `1.0.4 (22)` → `1.0.5 (24)`; `beta` flavor: `1.0.5-beta (25)` (`1.0.4-beta` was 23). Room schema unchanged, no migration.
- Built from `release/1.0.5` (= `release/1.0.4` + the calendar commit). `release/1.0.4` already includes the Play In-App Update flow,
  the In-App Review prompt and the Accessibility disclosure docs, so they ship in 1.0.5 too if they were not in the uploaded 1.0.4 build.
- `PREVIEW_FAKE_STICKERS` in `CalendarModel.kt` must stay `false` (fake entries for visual preview only).
- Verified on a device: Huawei P9 (Android 8), light theme — month, week, day sheet, sticker shapes and pile, duration chips.
  Not verified: dark theme of the new tiles/chips, the English locale, other devices, the calendar in `beta`.

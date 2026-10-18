# Tepera 1.0.0 (versionCode 15) — release for Play Store

File: `app/build/outputs/bundle/release/app-release.aab` (signed with the release key).

## What's new (EN) — Play "Release notes", ≤ 500 characters
- First public release of Tepera
- Home: the "without phone" orb now follows your phone's tilt more responsively
- More accurate hourly Online numbers: yesterday/week tiles now match the Online total on Home
- Fixed the "today" tiles between 00:00 and 01:00 showing the previous day twice

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Перший публічний випуск Tepera
- Головна: орб «без телефону» жвавіше реагує на нахил телефону
- Точніші погодинні Online-числа: плитки «Вчора»/«Тиждень» тепер збігаються з Online на головній
- Виправлено «сьогодні» між 00:00 і 01:00, коли попередня доба показувалась двічі

## For the developer (not for Play)
- Version: `0.8.1 (14)` → `1.0.0 (15)`. Room schema unchanged (17), no migration.
- Orb (`HomeHero.kt`): tilt spring ω ×1.3 (k 7→11.83, c 5.5→7.15, same damping ratio), accelerometer low-pass 0.08→0.104.
- `BalanceRepository.getOnlineMinutesPerSlot` now uses `foregroundSessions` + `mergeTimeSpans` (same as `getOnlineMinutes`); the old per-package "last foreground wins" pairing undercounted multi-activity apps, ignored screen-off and double-counted overlapping apps. It feeds `day_summaries.onlineByHour` (Home teasers/sheet) and the widget grid.
- `logicalToday()` (`TimeUtils.kt`) replaces `LocalDate.now()` in `HomeScreen`/`HomeSummaryViewModel`: between 00:00 and 01:00 Home is still on the previous logical day.
- Known: `day_summaries` rows already written with the old hourly logic are immutable and stay as they were (only matters for test devices). Known lint errors (10) unchanged.

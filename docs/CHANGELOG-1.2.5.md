# Tepera 1.2.5 (versionCode 43) — flavor `beta`, build: `:app:bundleBetaRelease` (copy: `release/tepera-1.2.5-43.aab`)

## What's new (EN) — Play "Release notes", ≤ 500 characters
- "Online today" now shows what is usual for this time of day, next to your reference
- Delete any scrolling-hours window, not just pause it
- Feed apps: the "when you open a feed" settings moved here, search stays pinned
- No tips after updating from 1.1 / 1.2.1
- The "Don't want to wait?" card disappears once you add your own hours
- Shorter text under "Starting point", thinner phone icon

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- «Online сьогодні» показує, скільки зазвичай набігає до цього часу, поруч з орієнтиром
- Будь-яке вікно годин скролінгу можна видалити, а не лише поставити на паузу
- «Застосунки-стрічки»: сюди переїхали налаштування «коли відкриваєш стрічку», пошук залишається вгорі
- Після оновлення з 1.1 / 1.2.1 підказки не з'являються
- Картка «Не хочеш чекати» зникає, щойно ви додали власні години
- Коротший текст під «Точкою відліку», тонша іконка телефона

## For the developer (not for Play)
- Version: beta `1.2.2 (42)` → `1.2.5 (43)` (1.2.3/1.2.4 were not released). `standard` flavor unchanged. Room schema unchanged.
- Tutorial: `TutorialStore.migrateIfNeeded()` (called in `TeperaApp.onCreate`) marks all tips as seen when `whats_new_121_seen` is already set, i.e. the update comes from 1.1.x/1.2.1. Known gap: a 1.2.1 user who never dismissed the "What's new" dialog still sees the tips.
- `OnlineTodayCard`: "usually by now" = MEAN over up to 7 previous days from `PatternRepository.hourlyOnlineMinutes` (`typicalOnlineSoFar()`), shown from 3 days of history; second quiet marker on the bar (no colour change, FR-3.10).
- Feed picker: `FeedOpeningSettings()` (was a group on the main Settings screen) is the first item of `FeedPickerScreen` in Settings mode (not in onboarding); search is a `stickyHeader` without its own background.
- Windows: `RiskWindowRepository.delete()` deletes any window. Bounds of deleted automatic windows go to `DeletedWindowStore` (SharedPreferences, not in export/backup) and `saveDetected` skips matching candidates. Delete button + confirm dialog on `WindowWhyScreen`.
- "Don't want to wait?" card hidden when any manual window exists (also disabled).
- Settings phone icon redrawn thinner (stroke 60 instead of 80), hand-made path in `TeperaSymbols`, not checked against an official SVG.
- Verified on Huawei P9 (Android 8) only, and only by installing; nothing was walked through by Claude. Not checked: light theme, English locale, other devices, delete flow, sticky search over the gradient. `lintBetaDebug` still fails on the old `QuickLogTileService` error.

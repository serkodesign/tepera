# Tepera 1.3.0-beta (versionCode 44) — flavor `beta`, build: `:app:bundleBetaRelease` (copy: `release/tepera-1.3.0-beta-44.aab`)

## What's new (EN) — Play "Release notes", ≤ 500 characters
- New Berserk mode (off by default): after a long stretch in feeds, a funny screen appears and the feed closes for a while. Stretch and block length are adjustable; it can be switched off once a day for important things
- New "How to count time" setting: each app on its own or all feeds together (also applies to "Open for 5 min")
- Backup now includes the hours you deleted
- Small fixes

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Новий берсерк-режим (за замовчуванням вимкнений): після довгого сидіння в стрічках з'являється жартівливий екран, і стрічка закривається на деякий час. Тривалість і блок налаштовуються; раз на добу можна вимкнути для важливих справ
- Нове налаштування «Як рахувати час»: кожен застосунок окремо або всі стрічки разом (стосується й «Відкрити на 5 хв»)
- Резервна копія тепер містить видалені вами години
- Дрібні виправлення

## For the developer (not for Play)
- Version: beta `1.2.5 (43)` → `1.3.0-beta (44)` (`versionName` is the literal string `1.3.0-beta`). `standard` flavor unchanged. Room schema unchanged (everything new is SharedPreferences).
- Berserk mode (beta only, lives in `GateAccessibilityService`): `BerserkController` + pure `BerserkTracker` (unit-tested, `BerserkTrackerTest`), settings/blocks in `util/BerserkStore`, block screen `BerserkActivity` / `BerserkBlockScreen`. Counts CONTINUOUS time in feed apps; a break longer than 3 min (internal, not in the menu) resets the count. Defaults: off, 60 min stretch, 60 min block (options 30–120 / 15–180). "Important thing" = `BerserkStore.skipToday` (off until 01:00 next logical day, clears blocks; button on the block screen and a row in settings). The count is in process memory (resets if the service/process restarts); blocks persist.
- Counting type (`BerserkStore.CountMode`, default PER_APP = old behaviour): ALL makes one shared count/block for all feeds, and `SoftExitController` keeps the "Open for 5 min" session when moving between feeds (`continuesInto`), without a new plan sheet.
- UI: Settings → Feed apps → bottom of the "when you open a feed" block. Tester card "Berserk: test" in Tester statistics (fast values 1 min / 2 min, show the block screen, reset).
- From 1.2.5 branch fixes: backup includes `deletedWindows`; tutorial migration also trusts `v3_onboarding_seen`; lint is clean (0 errors).
- NOT verified on a device: the whole berserk flow (block after the stretch, re-opening a blocked feed, "Important thing", shared counting during "5 min"), block screen on Android 8 (P9, translucent theme), English strings of the new screens exist only in `values` (other locales fall back to English). Jokes are in `berserk_jokes` (uk/en) and have not been reviewed by the owner.

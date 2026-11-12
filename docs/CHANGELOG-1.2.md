# Tepera 1.2 (versionCode 39) — відкритий тест / реліз (flavor `beta`: Години скролінгу + служба Спеціальних можливостей)

Збірка: `./gradlew.bat :app:bundleBetaRelease` → `app/build/outputs/bundle/betaRelease/app-beta-release.aab`
(копія — `release/tepera-1.2-beta-39.aab`). Попередня збірка в Play — 1.1 (32); проміжні 1.1.1 (33–38) у Play не завантажувались.

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Redesigned Home: a card for your next scrolling hour, today's hours, and this week at a glance
- Settings regrouped: "Your hours", "When you open a feed", "App", "More"
- New Dimming strength: Soft or Noticeable
- Clearer Accessibility explanation, shown as the second step of onboarding
- Redesigned Stats; winter evenings start earlier in the time theme
- Fixes: dimming no longer shows on the lock screen or after a back gesture; fewer, better widgets

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Нова Головна: картка наступної години скролінгу, сьогоднішні години й тиждень коротко
- Налаштування згруповано: «Години детоксу», «У момент відкриття», «Застосунок», «Ще»
- Нове налаштування приглушення: М'яке або Помітне
- Зрозуміліше пояснення про Спеціальні можливості — другим кроком онбордингу
- Оновлена Статистика; у темі за часом доби взимку вечір настає раніше
- Виправлено: приглушення більше не з'являється на екрані блокування й після жесту «назад»; віджетів стало менше, але кращих

## Changes since 1.1 (32)
- **Home (v3 mode):** header with date and settings; idea card for the next hour ("Not today" keeps that window quiet until its
  nearest occurrence ends); pauses; Online today; today's hours; scrolling-hours carousel with "+"; "This week in your detox hours"
  with a shift suggestion. Activity chips moved to the Diary; the calendar button moved to the Diary title.
- **Settings:** grouped Material 3 list ("Your hours", "When you open a feed", "App", "More"); a "Digital detox" group;
  long hints behind an (i) popup; language is a dialog; new Dimming strength (Soft 50% / Noticeable 70%).
- **Accessibility (Play review fixes):** prominent disclosure is now the second onboarding step (before any permission); existing users
  see it once from Home; shortened to fit one screen; says which entry to look for in system settings ("Tepera: pauses and ideas");
  "Redo onboarding" also resets the disclosure and consent. Declaration, privacy policy and store listing aligned with it.
- **Soft exit (dimming / "5 min are up" pill):** hidden over the notification shade and the lock screen (Moto G84), reminder waits
  for unlock and shows only if you are still in the same feed; back gesture no longer hides it for 30–60 s; dim is neutral black.
- **Stats:** yesterday as a two-row timeline ribbon with legend and tap-to-highlight; "Rest of day" is a separate row; today's partial
  trend segment is dashed; dark-theme heat map in one colour with alpha.
- **Theme:** "By time" evening starts up to 2 h earlier in winter (smooth curve by date, no geolocation); hemisphere selector in
  Settings → General → Theme. Dawn theme: restored glass card background and fixed tints.
- **Onboarding:** back-stack fixes (Continue on the feed picker no longer bounces back to the usage-access step; finishing always
  returns to Home); onboarding screens moved to the v3.1 tokens.
- **Widgets:** reduced from 11 to 6 (activity buttons 1x1/2x1/4x1, buttons + day grid 4x2, Online ring 2x1, "Without phone" 1x1);
  new previews. **Removed:** Online day bar, Active now, Start of day, Last activity, Week top — placed copies will disappear.

## For the developer (not for Play)
- Version: beta `1.1.1 (38)` → `1.2 (39)`. `standard` flavor is unchanged (`1.0.5 (24)`, no v3, no Accessibility). Room schema unchanged.
- Check before upload: versionCode 39 is higher than any code already in Play Console (all tracks).
- Not verified on a device: light theme and Ukrainian of the new Home/Settings, "hour in progress"/"all done today" states,
  lock-screen fix on G84, release (R8) build on a device, new widget previews on other devices.
- The Accessibility demo video and reviewer instructions (declaration, section 8) still describe the old path
  "Settings → Digital detox → Apps with a delay"; in 1.2 it is Settings → "When you open a feed" → Accessibility.

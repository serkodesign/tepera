# Чернетка release notes — аналітика, рівень 1 (версія ще не призначена, НЕ опубліковано)

## What's new (EN) — Play "Release notes", ≤ 500 characters
- New: Settings → Data and privacy → "Share anonymous statistics". Tepera counts how often features are used, on your phone only. If you want to help, you see the exact text and send it yourself by email. No app names, no ID, nothing is sent automatically.

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Нове: Налаштування → Дані й приватність → «Поділитися анонімною статистикою». Tepera рахує, як часто користуються функціями, лише на твоєму телефоні. Якщо хочеш допомогти, бачиш точний текст і сам надсилаєш його листом. Без назв застосунків і ID, нічого не йде автоматично.

## For the developer (not for Play)
- Room schema: v23 → v24 (`daily_event_counters`, additive, `MIGRATION_23_24`). No new permissions; no new network access (INTERNET already present via Firebase).
- Event catalog (`analytics/AnalyticsEvent.kt`): category A everywhere, category B (Accessibility/overlay-derived) only in the `beta` flavor and only counted locally.
- Retention: counters older than 90 days are deleted daily (`AnalyticsCleanupWorker`); "Delete all data" clears them.
- Before release: the level-1 policy bullet is now in `privacy-policy.md`; Data safety — see `play-console-declarations.md` (combined level 1 + 2). The manual checklist was run on Huawei P9 (26.11.2026).
- Remote analytics (PostHog, opt-in) is a separate level 2 — see `CHANGELOG-analytics-level2-draft.md`; this level-1 feature itself sends nothing automatically.

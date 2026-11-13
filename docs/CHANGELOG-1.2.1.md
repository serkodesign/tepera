# Tepera 1.2.1 (versionCode 41) — flavor `beta`, build: `:app:bundleBetaRelease` (copy: `release/tepera-1.2.1-beta-41.aab`)

## What's new (EN) — Play "Release notes", ≤ 500 characters
- "Scrolling hours" is now the main mode; the switch is gone
- Settings regrouped: "Digital detox" (tracking, apps with a delay), "App" (now with Data & privacy and Knowledge base), "More"
- "Look" is now "Appearance"; language is in one place only
- Dark theme: icon circles in Settings are lighter
- After updating from an older version, a short note explains what has changed since 1.0.5

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- «Години скролінгу» тепер основний режим, перемикач прибрано
- Налаштування перегруповано: «Цифровий детокс» (відстеження, застосунки з затримкою), «Застосунок» (тепер із «Дані й приватність» і «Базою знань»), «Додатково»
- «Вигляд» → «Оформлення»; мова лише в одному місці
- У темній темі кружки іконок у налаштуваннях світліші
- Після оновлення зі старої версії — коротке вікно про те, що змінилось від 1.0.5

## For the developer (not for Play)
- Version: beta `1.2 (39)` → `1.2.1 (41)`. `standard` flavor unchanged. Room schema unchanged.
- `SettingsStore.v3ModeEnabled` is now always `true` (stored value ignored). `V3ModeToggle` no longer has the mode switch.
- What's-new dialog: `ui/onboarding/WhatsNewDialog`, flag `whats_new_121_seen`, shown only when `lastUpdateTime > firstInstallTime`; it runs before the v3 welcome screen.
- Not verified on a device (none connected).

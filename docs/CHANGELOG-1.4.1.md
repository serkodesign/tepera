# Tepera 1.4.1 (versionCode 54) — flavor `beta`, build: `:app:bundleBetaRelease` (copy: `release/tepera-1.4.1-54.aab`)

Версія без суфікса `-beta` у `versionName`; flavor той самий.

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Cards across the app now share one look: same corners, spacing and outline
- Home cards, statistics and settings feel more consistent
- Observation progress bar is softer in dark theme

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Картки в застосунку тепер мають єдиний вигляд: однакові кути, відступи й рамка
- Картки на Головній, у Статистиці та Налаштуваннях виглядають узгодженіше
- Смужка прогресу спостереження в темній темі стала м'якшою

## For the developer (not for Play)
- Version: beta `1.4.0 (53)` → `1.4.1 (54)`. `standard` unchanged. Room schema unchanged.
- Cards: one component `TeperaCard(variant = Standard 16 / Context 24 / Hero 28)`, gap always 12, one border token `TeperaPalette.cardBorder`. Outer spacing is set by the screen (`ContextCardSlot` for Home cards), not by the card.
- Removed: `V3Card`, `M3Card` (file deleted), `contextInfoSurface()`, palette tokens `homeCardBorderPattern` / `homeCardFillPattern` / `homeCardFillWeeklyDigest`. `HomeCardSurface` is now a thin wrapper (only used by the unused `PatternMiniCard` / `WeeklyDigestCard`).
- Visual changes to check: gap 8/16 → 12 in context cards; `StatCard` ~12dp taller; `TimeCard` radius 20 → 16; Statistics cards got the outline; in dark theme the outline equals the fill (invisible).
- Observation progress bar: empty segments are white 10% in dark theme (light theme unchanged; `heatmapNoDataFill` is shared with the heat map and untouched).
- Checked: `:app:bundleBetaRelease` builds and is signed with the upload key (`jarsigner -verify`), merged manifest `versionName="1.4.1"` / `versionCode="54"`. Debug build installed and launched on Huawei P9 without crashes; not run on a device from the release AAB (R8). Screens not visually checked after the refactor.

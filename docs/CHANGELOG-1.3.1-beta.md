# Tepera 1.3.1-beta (versionCode 45) — flavor `beta`, build: `:app:bundleBetaRelease` (copy: `release/tepera-1.3.1-beta-45.aab`)

## What's new (EN) — Play "Release notes", ≤ 500 characters
- The Berserk screen now has a friendly illustration: a bear cub in a viking helmet (light and dark versions)
- Settings and lists: group headings are a bit larger; shorter hints in a few rows; language shown as a short code
- Dialog buttons are compact and aligned to the right
- Small fixes

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- На екрані берсерка з'явилась дружня ілюстрація: ведмежа у шоломі вікінга (світла й темна версії)
- Налаштування й списки: підписи груп трохи більші; коротші підказки в кількох рядках; мова показується коротким кодом
- Кнопки в діалогах компактні й праворуч
- Дрібні виправлення

## For the developer (not for Play)
- Version: beta `1.3.0-beta (44)` → `1.3.1-beta (45)`. `standard` flavor unchanged. Room schema unchanged.
- Berserk block screen: `berserk_bear.png` / `berserk_bear_dark.png` (`drawable-nodpi`, 660px, from Figma "1.3.0"), variant picked by app theme, content scrollable.
- UI per Figma "1.3.0": `M3GroupLabel` and hours header are 16sp; `TeperaDialog` buttons use new `TeperaButtonSize.Compact` (40dp, hug, right-aligned); language value is a short code (UA/EN); texts for Berserk toggle and "Turn off until tomorrow" updated; no supporting text on "Suggest a feature".
- NOT verified on a device: the whole berserk flow (still unverified from 1.3.0), the illustration on small screens (P9) and the compact dialog buttons. English strings of the new Berserk texts exist only in `values` (other locales fall back to English).

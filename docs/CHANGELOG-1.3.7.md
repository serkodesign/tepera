# Tepera 1.3.7 (versionCode 50) — flavor `beta`, build: `:app:bundleBetaRelease` (copy: `release/tepera-1.3.7-50.aab`)

Версія без суфікса `-beta` у `versionName` (показується в Налаштуваннях і в статистиці для тестувальників); flavor той самий.

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Clearer Accessibility explanation: it now also covers Berserk mode
- Berserk mode: simpler description, and "How to count time" moved into its settings
- "This week" and "Phone-free pauses" widgets always show their numbers
- Shorter, friendlier texts in Settings and Apps with a delay

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Зрозуміліше пояснення Спеціальних можливостей: тепер воно охоплює й Берсерк-режим
- Берсерк-режим: простіший опис, «Як рахувати час» тепер у його налаштуваннях
- Віджети «Цей тиждень» і «Паузи без телефону» завжди показують числа
- Коротші й дружніші тексти в Налаштуваннях і «Застосунках з затримкою»

## For the developer (not for Play)
- Version: beta `1.3.6-beta (49)` → `1.3.7 (50)`. `standard` unchanged. Room schema unchanged.
- Texts: dead shortcut strings removed (`gates_mode_*`), Accessibility disclosure and gate card shortened and now name Berserk mode and the return to the home screen; `settings_dim_info`, `weekly_summary_hint`, Knowledge Base section 2 and "What's new" aligned with the real behaviour.
- Widgets "This week" / "Phone-free pauses": the "show numbers" toggles and `SettingsStore` keys are gone; the numbers are always shown. This replaces the earlier rule from `tepera-dev-spec.md` 2.2 for these two widgets (owner's decision, 23.11.2026).
- Berserk settings: "How to count time" lives under the Berserk toggle, its hint string was removed.
- **Privacy Policy NOT changed** in this release (owner's decision: edit only if Google sends a warning). The rewritten version is kept in git history (`git show ffed68d:docs/privacy-policy.md`). The in-app disclosure now says more than the published policy.
- Store listing and Accessibility declaration drafts (`store-listing-*.md`, `play-accessibility-declaration.md`) already describe Berserk mode but are NOT published; the demo video does not show Berserk.
- Checked: `:app:bundleBetaRelease` builds and is signed with the upload key (`jarsigner -verify`), merged manifest `versionName="1.3.7"` / `versionCode="50"`. Not run on a device from the release AAB (R8). Texts not checked on screen after the last change.

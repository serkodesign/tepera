# Tepera 1.0.3 (versionCode 18) — release for Play Store

File: `app/build/outputs/bundle/release/app-release.aab` (signed with the release key).

## What's new (EN) — Play "Release notes", ≤ 500 characters
- You can now edit default categories too: rename them, change the icon and colour
- New category editor: live preview, scrollable icon grid, colour row
- Home summary sheet: Week and Month show online hours with an hour axis

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Тепер можна редагувати й стандартні категорії: змінювати назву, іконку та колір
- Новий редактор категорій: попередній перегляд, прокручувана сітка іконок, ряд кольорів
- Шторка підсумків на Home: «Тиждень» і «Місяць» показують онлайн-години з віссю годин

## For the developer (not for Play)
- Version: `1.0.2 (17)` → `1.0.3 (18)`. Room schema unchanged, no migration.
- Commit `94d32fc`: Home summary sheet Week/Month with hour axis.
- Default categories editable (`CategoryViewModel.updateCategory`): a renamed default drops `nameKey` and keeps the literal text
  (no longer follows the app language); unrenamed ones keep translating. `ensureDefaultsSeeded()` no longer re-syncs `colorHex`
  every launch (it would overwrite user edits) — existing installs keep their current colours.
- `EditCategoryDialog` redesigned; `TeperaSymbols` extended; edit button shown for default categories in the category history screen.
- Not verified on a device in this session.

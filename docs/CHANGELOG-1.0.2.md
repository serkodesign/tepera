# Tepera 1.0.2 (versionCode 17) — release for Play Store

File: `app/build/outputs/bundle/release/app-release.aab` (signed with the release key).

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Dialogs, date/time pickers and the summary sheet now follow the in-app language
- Ukrainian text now addresses you formally ("ви")

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Діалоги, вибір дати й часу та шторка підсумків тепер відповідають мові застосунку
- Український текст тепер звертається до вас на «ви»

## For the developer (not for Play)
- Version: `1.0.1 (16)` → `1.0.2 (17)`. Room schema unchanged, no migration.
- Dialog/DatePickerDialog/ModalBottomSheet windows wrapped in `ProvideAppLocale` (commit `aee0ca3`).
- Ukrainian `strings.xml` and `docs/store-listing-ua.md` switched from "ти" to "ви".

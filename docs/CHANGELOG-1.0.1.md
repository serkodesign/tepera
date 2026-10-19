# Tepera 1.0.1 (versionCode 16) — release for Play Store

File: `app/build/outputs/bundle/release/app-release.aab` (signed with the release key).

## What's new (EN) — Play "Release notes", ≤ 500 characters
- Smaller and faster app: code is now optimised and shrunk
- Under-the-hood improvements for stability and security

## Що нового (UA) — «Примітки до випуску» в Play, ≤ 500 символів
- Менший і швидший застосунок: код оптимізовано та стиснуто
- Внутрішні покращення стабільності та безпеки

## For the developer (not for Play)
- Version: `1.0.0 (15)` → `1.0.1 (16)`. Room schema unchanged, no migration.
- R8 enabled for release (`isMinifyEnabled`, `isShrinkResources`, `proguard-android-optimize.txt`) — fixes the Play Console warning "DEX code optimization: Obfuscation 1%".
- `app/proguard-rules.pro` (new): only `-keepattributes SourceFile,LineNumberTable` + `-renamesourcefileattribute SourceFile` for readable Crashlytics traces. No custom keep rules — libraries ship consumer rules.
- Crashlytics mapping is uploaded automatically (`uploadCrashlyticsMappingFileRelease`); mapping files in `app/build/outputs/mapping/release/`.
- AAB ≈ 7.3 MB, release APK ≈ 3.7 MB.
- **Not yet verified on a device** — R8 can break reflection-dependent code. Check before upload: usage-stats access/Online, timers, manual entry, all widget sizes, notifications, gates (shortcut + pause screen), export/import, Stats. On a crash add a targeted `-keep`.
- Note: the S23 has a debug-signed install, so a release APK cannot update it (signature mismatch) — needs uninstall (data loss) or another device.

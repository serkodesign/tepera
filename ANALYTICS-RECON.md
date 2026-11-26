# ANALYTICS-RECON — Фаза 0 (лише читання, код не змінювався)

Дата: 2026-11-26. Гілка: `feature/v3-mvp` (`feature/analytics` ще не створена).

## 1. Архітектура
- Один Gradle-модуль `app`, Kotlin 2.3.21, KSP 2.3.11, Room 2.8.4, WorkManager 2.11.2, Firebase BOM 34.18 (Crashlytics + **Remote Config**), Compose.
- **DI немає** (CLAUDE.md: ручний factory). Репозиторії створюються у `TeperaApp.kt` (`Application.onCreate`), ViewModel-и — через factory. Singleton-у `Analytics` природне місце — `TeperaApp`.
- Навігація: один `TeperaNavHost.kt` (`Routes`: HOME, ONBOARDING, CATEGORY_ONBOARDING, ONLINE_ESTIMATE_ONBOARDING, TARGET_ONBOARDING, WELCOME_ONBOARDING, GATES_ONBOARDING, A11Y_ONBOARDING, …). Ланцюжок онбордингу — `nextOnboardingRoute()` + `advanceOnboarding()`.
- Flavor-и: `standard` (Play-реліз, без Accessibility, `V3_MVP=false`) і `beta` (`V3_MVP=true`, `GateAccessibilityService`, Берсерк, години скролінгу; зараз 1.3.8 / 51, відкрите тестування). Сервіс і overlay-екрани лише в `app/src/beta/`.
- `minSdk 26`, `targetSdk 36`, `compileSdk 37`.

## 2. Room
- **Версія БД 23**, `exportSchema = true`, схеми в `app/schemas`, `androidTest` бере їх як assets. Auto-migrations нема — ручні `MIGRATION_1_2 … MIGRATION_22_23` у `data/local/Migrations.kt`, усі додаються в `TeperaApp.kt`.
- Тести міграцій: `src/test/.../data/MigrationTest.kt` (є). Правило проєкту: bump версії + Migration щоразу.
- Для ітерації 2.2 це буде v23→v24, нова таблиця `daily_event_counters` (адитивна).

## 3. DataStore / SharedPreferences
- `SettingsStore` (DataStore) — основні налаштування; `DeviceIdProvider` (DataStore `device_id`, UUID, зараз нікуди не надсилається — можна використати як основу для PostHog distinct_id, але краще окремий ID, щоб не змішувати); `ActiveTimerStore`, `SubMinuteStore`.
- SharedPreferences: `PlanOverlayStore` (лічильники листа/м'якого виходу), `A11yConsentStore`, `LocaleStore`, `MotionStore`, `WindowSkipStore`, `CrashReporting` (перемикач Crashlytics).

## 4. Маніфест (змерджений betaRelease)
Дозволи: `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, **`INTERNET`** (транзитивно від Firebase).
Сервіси/компоненти: `GateAccessibilityService` (beta), `PlanOverlayActivity`, `BerserkActivity`, Nudge-ресівери, ~15 віджет-ресівери, `QuickLogTileService`. `firebase_crashlytics_collection_enabled=false` до вибору користувача.

## 5. Онбординг
Кроки: Welcome → (Accessibility-розкриття, beta, A11Y_ONBOARDING) → категорії → оцінка Online → дозволи (usage access + сповіщення) → пропозиція віджета/воріт → Home. Перемикач «Звіти про збої» на першому екрані. Розкриття Accessibility — окремий екран + `AccessibilityDisclosureDialog`; це стоп-точка (не чіпати).

## 6. Налаштування
`SettingsHomeScreen.kt` (групи), «Дані й приватність» = `BackupRestoreScreen` (експорт/імпорт, сповіщення, звіти про збої, «Видалити всі дані» → `TeperaApp.wipeAllData()` — таблиця лічильників має туди потрапити). «Запропонувати функцію» — `util/FeedbackForm.kt`, `ACTION_VIEW` на Google Form (UA/EN). `ACTION_SEND` у проєкті ще не використовується.

## 7. Мапінг каталогу подій на код
| Подія | Є в коді? | Де / примітка | Кат. |
|---|---|---|---|
| A1–A3 onboarding_* | так | `TeperaNavHost.advanceOnboarding`, `skipAllOnboarding`; кроки — константи `Routes.*_ONBOARDING` | A |
| A4 accessibility_service_enabled | так | `util/GateAccessibility` (перевірка системних налаштувань), `A11yConsentStore`; звіряти при поверненні в застосунок | A |
| A4b usage_access_granted | так | дозвіл перевіряється в `BalanceRepository`/онбордингу | A |
| A5/A6 plan_* | **частково** | CLAUDE.md: таблиця `plans` «не використовується в UI» (планів «якщо-то» немає, рішення 11.11). Замість них — «ідеї»/вікна: `RiskWindowRepository` (вікно додано вручну/видалено/вимкнено). **Пропоную перейменувати на `window_added/edited/deleted`** (+`is_first`) | A |
| A7 activity_added | так | `AddEntryViewModel.save`, `ActivityRepository` (без назви) | A |
| A8 tab_viewed | так | вкладки навбару (Home/Щоденник/Статистика/Налаштування) в `TeperaNavHost` | A |
| A9 gate_paused | так | `toggleTodayPause` (віджет «Пауза затримок» і екран воріт); зараз є лише «на сьогодні», варіантів weekend/until_date **немає** → `duration` треба звузити до `today` | A |
| A10 widget_added | **ні** | 15 віджет-класів, `onEnabled`/`onUpdate` не фіксує «додано»; можна рахувати в `GlanceAppWidgetReceiver.onEnabled` (спрацьовує на перший екземпляр) | A |
| A11 qs_tile_added | так | `QuickLogTileService.onTileAdded` | A |
| A12 feedback_form_opened | так | `FeedbackForm` | A |
| A13 stats_share_* | ні (нова фіча 2.5) | — | A |
| A14 consent_changed | ні | з'явиться у Фазі 3 | A |
| B1 risk_window_started | так | `RiskWindow`/`NextWindow` у `GateAccessibilityService` | B |
| B2 intention_sheet_shown | так | `PlanOverlayStore.recordShown` | B |
| B3 intention_selected | частково | є accepted/declined; «5/15/30 хв» — таймер в `PlannedTimer.kt` | B |
| B4/B5 soft_exit_* | так | `recordSoftExitStarted/OnTime/Extended`, `recordLeft/Continued` | B |
| B6–B8 puzzle_* | так | `QuickPuzzleViews.kt`, `PlanOverlayScreen`; «покинуто» треба перевірити | B |

**Важливо:** B-події вже частково існують як лічильники в SharedPreferences (`PlanOverlayStore`) і читаються екраном `TesterStatsScreen` / `TesterStatsRepository` (екран тестувальників, beta). Нову `Analytics` треба або поверх них, або з міграцією цих лічильників — щоб не дублювати.

## 8. Політика і магазин
- `docs/privacy-policy.md`, дві мови (UA + EN в одному файлі); публікується на GitHub Pages (джерело — CLAUDE.md каже, не підтверджене).
- Політика каже: «offline-first … we do not collect», «застосунок сам не робить мережевих запитів, окрім анонімних crash-звітів … тому є дозвіл на мережу». Фраз «дані не залишають пристрій» у явному вигляді нема, але «offline-first» і «Tepera itself sends nothing» доведеться змінити при Фазі 3.
- `docs/store-listing-en|ua.md`: слова «offline» вживаються у сенсі офлайн-активностей; прямого «без інтернету» не знайдено (потрібна ручна перевірка опису в Play Console).

## 9. Тести
- Юніт-тести: ~50 файлів у `src/test`, у т.ч. `MigrationTest`. `androidTest` — тести міграцій на пристрої (за CLAUDE.md).

## 10. Ризики і питання до тебе
1. **Передумова плану невірна:** застосунок вже має `INTERNET` (транзитивно від Firebase Crashlytics + Remote Config). Отже «додавання INTERNET» у Фазі 3.1 — не потрібне; а твердження «зараз без INTERNET» треба прибрати з плану й документів. Це спрощує Фазу 3 (немає нового дозволу), але перевірка «нуль запитів до PostHog до згоди» лишається.
2. **Існує `firebase-config` (Remote Config)** — вже ходить у мережу (`UpdateChecker`). Політику/Data safety це має покривати; звір, що поточна Data safety це декларує.
3. **Plans «якщо-то» немає** — перейменувати A5/A6 (див. п. 7). Підтверди.
4. **`duration` для A9** — лише `today`.
5. **Дубль лічильників:** `PlanOverlayStore` + `TesterStatsRepository` вже збирають B-метрики локально. Варіанти: (а) `LocalAnalytics` лишити окремо, нічого не чіпати в старих; (б) згодом перевести старі. Рекомендація: (а), без ризику для beta.
6. **Flavor:** B-події існують лише в `beta`. `standard` матиме тільки A. Пропоную цей поділ лишити.
7. **D-рішення D1–D6** досі не заповнені; для Фази 2 блокує лише D6 (ітерація 2.5) і D2 (реліз). Рекомендації з плану приймаю для чернеток.
8. **Email підтримки:** у політиці `serkodesign@gmail.com` — використати як `[SUPPORT_EMAIL]`? Підтверди.
9. **Гілка:** `feature/analytics` створити від `feature/v3-mvp` (там усі потрібні фічі), не від `main`. Підтверди.

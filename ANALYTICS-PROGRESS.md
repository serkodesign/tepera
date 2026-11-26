# ANALYTICS-PROGRESS

Гілка `feature/analytics` (від `feature/v3-mvp`). План — `CLAUDE-CODE-PLAN-analytics.md`, розвідка — `ANALYTICS-RECON.md`.

## Рішення
- Гілка від `v3-mvp` — так. Планів «якщо-то» нема → A5/A6 = `window_added/edited/deleted`. `duration` для `gate_paused` лише `today`.
- B-події існують лише у flavor `beta` (там сервіс/оверлеї); `standard` має лише A. Окремих перевірок flavor у коді нема.
- Старі `PlanOverlayStore`/`TesterStatsRepository` не чіпаємо.
- INTERNET уже в маніфесті (від Firebase) — у Фазі 3 новий дозвіл не потрібен.
- D1–D6: приймаємо рекомендації плану для чернеток/фіче-флагу; D2, D6 уточнити перед 2.5 / релізом.

## Фаза 1 (вручну, Серхій)
- [ ] Play Console → Dashboard → «Your KPIs»: DAU, 28-day active, returning, user loss.
- [ ] Statistics: зберегти звіти «Активні користувачі по тижнях» і «Нові vs втрачені».
- [ ] Android vitals: базова лінія crashes/ANR.
- [ ] Записати базові значення тут: ______

## Фаза 2
- [x] 2.1 Інтерфейс і каталог (`analytics/`, тести)
- [x] 2.2 Сховище: `daily_event_counters`, БД v24, `MIGRATION_23_24`, схема 24.json. Компіляція + юніт-тести зелені; **androidTest (MigrationTest, DailyEventCounterDaoTest) ще не запускались — потрібен пристрій**
- [x] 2.3 `LocalAnalytics` (фоновий запис, збої ігноруються), `AnalyticsCleanupWorker` (щодня, >90 днів), `TeperaApp.analytics` (Composite з Local). «Видалити всі дані» вже чистить таблицю через `database.clearAllTables()`; воркер перепланується в `wipeAllData`. Юніт-тести зелені
- [x] 2.4 Інструментування (3 коміти). Усе локально, без мережі. Факти й відомі спрощення:
  - A1–A3 онбординг: за зміною маршруту в `TeperaNavHost` («завершено» = вихід на Home, тож «Пропустити все» і повторно відкритий крок теж рахуються).
  - A4/A4b: `AccessStateProbe` у `MainActivity.onResume`, лише перехід ні→так (стан у SharedPreferences `analytics_state`, чиститься при «Видалити всі дані»).
  - A5/A6 → `window_added/edited/deleted` у `RiskWindowRepository` (`is_first` = першого ручного вікна). A7 — `AddEntryViewModel` лише для нових записів. A8 — `tab_viewed` (Home/Щоденник/Статистика/Налаштування; кожне повернення на екран рахується).
  - A9 `gate_paused`: `today|weekend|until_date` (розвідка помилково казала «лише today»). A10 `widget_added` — `onEnabled` (перший екземпляр кожного типу віджета). A11 — `QuickLogTileService.onTileAdded`. A12 — обидва місця «Запропонувати функцію».
  - B (лише beta): B1 раз на вікно за добу (пам'ять сервісу), B2/B4/B5 — у `PlanOverlayStore.record*`, B3 — бакет за тривалістю «Так, роблю це», B6–B8 — `PuzzleStep` (є тип `intention` — питання, не задачка; прев'ю з екрана тестувальників не рахується).
  - Не інструментовано: `IdeaHero` на Головній (`recordAccepted` там не дає B3), A13/A14 — з'являться в 2.5 / Фазі 3.
- [x] 2.5 Екран «Поділитися анонімною статистикою» (Налаштування → Дані й приватність → у розділі «Приватність»): пояснення, точний текст у моноширинному блоці, кнопка «Надіслати листом» (`ACTION_SEND` + `serkodesign@gmail.com`, тема «Tepera stats»; людина сама обирає поштовий застосунок і сама натискає «Надіслати»). Звіт (`StatsShareReport`): період 30 днів, версії застосунку й Android, **підсумки за весь період без розбивки по днях**, без ID/моделі/назв застосунків; завжди англійською (читає розробник). Кнопка неактивна, поки нема подій. A13: `stats_share_opened` при відкритті екрана, `stats_shared` лише коли чат-вибір реально відкрито. Рядки UA+EN. Юніт-тести зелені; на пристрої не відкривали.
- [x] 2.6 Документи (код не чіпали): `docs/privacy-policy-analytics-draft.md`, `docs/analytics-level1-checklist.md` (ручний чекліст, обмеження, Data safety за D2), `docs/CHANGELOG-analytics-level1-draft.md`. **Нічого не опубліковано.** Автоматична частина: юніт-тести зелені (compile standard+beta). **Не виконано (потрібен пристрій): androidTest, ручний чекліст.**

### Перед релізом Фази 2 (робить Серхій)
- [ ] Виконати ручний чекліст на пристрої (`docs/analytics-level1-checklist.md`) і `connectedStandardDebugAndroidTest`.
- [ ] Оновити політику на GitHub Pages за чернеткою (і підтвердити джерело Pages).
- [ ] Вирішити Data safety (D2) і внести в Play Console.
- [ ] Призначити версію, злити `feature/analytics` у `feature/v3-mvp`, зібрати AAB.

## Відкриті питання
- Email підтримки для 2.5: `serkodesign@gmail.com` — підтверджено

---

## Передача на інший комп'ютер: перенести аналітику на базу `tepera-1.x` (1.4.2-beta, 55)

**Чому:** фази 2.1–2.6 зроблено на старій базі (`feature/v3-mvp` = 1.3.8/51). Справжня актуальна гілка — `origin/tepera-1.x`
(перейменована `feature/v3-mvp`, +15 комітів: 1.3.9 → 1.4.2-beta, нові картки `TeperaCard`, нова іконка, зміни налаштувань).
На тестовому Huawei P9 стоїть 1.4.1 (54), БД v23. Гілка `feature/v3-mvp` з аналітикою **застаріла — не використовувати**.

**Де лежить робота:** гілка `origin/feature/analytics` (чисті коміти 2.1–2.6 поверх `0e0a127`, без злиття). Брати саме її, не `feature/v3-mvp`.

**Кроки (для Claude Code на іншому комп'ютері):**
1. `git fetch`; `git checkout -b feature/analytics-on-1x origin/tepera-1.x`; `git merge --no-ff origin/feature/analytics`.
2. Розв'язати конфлікти уважно, зберігаючи НОВИЙ вигляд з `tepera-1.x` і додаючи лише виклики аналітики. Очікувані місця: `TeperaNavHost.kt` (вкладки/онбординг/маршрут `STATS_SHARE`), `BackupRestoreScreen.kt` (рядок «Поділитися…», параметр `onOpenStatsShare`), `strings.xml` (uk/en, блок `stats_share_*`), `TeperaApp.kt` (БД v24, `MIGRATION_23_24`, `analytics`, `AnalyticsCleanupWorker`), `AppDatabase.kt`, `MainActivity.kt` (`onResume` → `AccessStateProbe`), віджети (`TrackedWidgetReceiver`), `PlanOverlayStore`/`PlanOverlayScreen`/`QuickPuzzleViews`/`GateAccessibilityService`, `SettingsHomeScreen.kt`. Якщо `tepera-1.x` перейменувала/перенесла екрани — перенести виклики `track()` на нові місця за `ANALYTICS-RECON.md` розділ 7 і цей файл (пункт 2.4).
3. Версія БД на `tepera-1.x` — перевірити (була 23). Якщо там з'явилась міграція, перенумерувати `MIGRATION_23_24` і `daily_event_counters` так, щоб ланцюжок був без прогалин; `schemas/*.json` згенерувати заново.
4. `sh gradlew :app:compileStandardDebugKotlin :app:compileBetaDebugKotlin :app:testStandardDebugUnitTest :app:testBetaDebugUnitTest` (на Windows — `gradlew`). `lintStandardDebug` має 2 помилки в старому коді (TesterStatsScreen) — не наші.
5. Підключити пристрій (adb за повним шляхом, див. CLAUDE.md): `connectedStandardDebugAndroidTest` (`MigrationTest` має ланцюг до 24 і `migrate23to24_...`, `DailyEventCounterDaoTest`).
6. Поставити поверх наявної установки з даними (`installBetaDebug`; на P9 1.4.1/54 < нової збірки, даунґрейду нема), пройти ручний чекліст `docs/analytics-level1-checklist.md`.
7. Оновити цей файл результатами, закомітити в `feature/analytics-on-1x`. Не пушити в `tepera-1.x` і не зливати туди без явного «так» власника. Нічого не публікувати (політика, Data safety, Play).

**Рішення власника, що лишаються:** оновлення політики за чернеткою (`docs/privacy-policy-analytics-draft.md`), Data safety за D2, версія релізу, Фаза 3 (PostHog: D1/D3/D4) — не починати без «починай Фазу 3».

**Підводні камені:** на macOS `sed -i` потребує `''` (краще Edit); у zsh `--include=*.kt` без лапок ламає grep; `&&` після невдалої команди не спиняє `;`-ланцюжок — перевіряти код виходу перед комітом.

### Результати перенесення на `tepera-1.x` (26.11.2026, гілка `feature/analytics-on-1x`)
- Злиття `origin/feature/analytics` у `origin/tepera-1.x` (1.4.2-beta, 55): один конфлікт — `TeperaNavHost.kt` (`GRADIENT_ROUTES`); збережено `FEED_OPENING`/`BERSERK_SETTINGS` з 1.x і додано `STATS_SHARE`.
- БД на `tepera-1.x` лишалась v23 → `MIGRATION_23_24` без перенумерації, `24.json` без змін.
- `compile{Standard,Beta}DebugKotlin` + `test{Standard,Beta}DebugUnitTest` — зелені.
- androidTest на Huawei P9 (EVA-DL00, Android 8): 23 тести OK (MigrationTest, DailyEventCounterDaoTest, RiskScannerTest, RiskWindowRepositoryTest). Запущено через `installBetaDebug` + `installBetaDebugAndroidTest` + `am instrument` (не `connectedAndroidTest` — той видаляє застосунок і дані).
- `installBetaDebug` поверх 1.4.1 (54): запуск без FATAL, `PRAGMA user_version = 24`, `daily_event_counters` уже містить рядки.
- **Ще не виконано:** ручний чекліст `docs/analytics-level1-checklist.md` (пункти 3–10: лічильники по екранах, екран «Поділитися…», видалення даних, воркер, онбординг, beta-події, локалі). Нічого не опубліковано, у `tepera-1.x` не злито й не запушено.

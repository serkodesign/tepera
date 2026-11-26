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

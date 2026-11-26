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
- [ ] 2.5 Екран «Поділитися статистикою»
- [ ] 2.6 Перевірка і документи

## Відкриті питання
- Email підтримки для 2.5: `serkodesign@gmail.com` — підтверджено

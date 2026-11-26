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
- [ ] 2.3 LocalAnalytics + очищення
- [ ] 2.4 Інструментування
- [ ] 2.5 Екран «Поділитися статистикою»
- [ ] 2.6 Перевірка і документи

## Відкриті питання
- Email підтримки для 2.5: `serkodesign@gmail.com` — підтверджено

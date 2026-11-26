# ЧЕРНЕТКА змін до політики конфіденційності — аналітика, рівень 1 (НЕ опублікована)

Статус: чернетка для власника. Нічого не опубліковано. Політику на GitHub Pages оновлює власник вручну; за
рішенням 23.11.2026 політика змінюється лише якщо потрібно (попередження Google) — ця зміна потрібна, лише
якщо випускається збірка з екраном «Поділитися анонімною статистикою» (див. «Чи потрібна зміна» нижче).

## Чи потрібна зміна
- Застосунок сам нічого не надсилає: лічильники лежать на пристрої, лист людина відправляє сама поштовим застосунком.
  Тому твердження «Tepera itself sends nothing» / «сама Tepera нічого не надсилає» лишається правдивим.
- Але з'являється новий вид даних (лічильники використання) і нова дія «надіслати розробнику». Якщо в Data safety
  декларуємо «App interactions — optional» (D2), політика має це пояснювати, інакше декларація й текст розходяться.
- Не змінювати без потреби фрази «offline-first» — вони й далі правдиві.

## Що додати

### Українською — новий пункт у списку «Які дані обробляються» (після пункту про UsageStatsManager)

> - **Статистика використання (лічильники на пристрої).** Tepera веде прості щоденні лічильники використання функцій (наприклад, скільки разів додано активність чи відкрито вкладку). Вони зберігаються лише на вашому пристрої, не містять назв застосунків, введеного вами тексту, часу доби чи ідентифікаторів, автоматично видаляються через 90 днів і разом з іншими даними при «Видалити всі дані». Нам вони надходять лише якщо ви самі обираєте «Поділитися анонімною статистикою» в Налаштування → Дані й приватність: застосунок показує точний текст (підсумки за 30 днів, версії застосунку й Android), і ви самі надсилаєте його листом зі свого поштового застосунку — ви можете прочитати й змінити текст до надсилання. Сама Tepera нічого не надсилає.

Також оновити дату «Останнє оновлення».

### English — new bullet in "What data is processed" (after the UsageStatsManager bullet)

> - **Usage statistics (on-device counters).** Tepera keeps simple daily counters of how its features are used (for example, how many times an activity was added or a tab was opened). They are stored only on your device, contain no app names, no text you enter, no times of day and no identifiers, and are deleted automatically after 90 days or when you use "Delete all data". They reach us only if you choose "Share anonymous statistics" in Settings → Data and privacy: the app shows you the exact text (30-day totals, app and Android versions) and you send it yourself by email from your own mail app — you can read and edit it before sending. Tepera itself sends nothing.

## Що НЕ змінюється
- Блок «Аналітика й мережа» (crash-звіти Crashlytics) — без змін у рівні 1. Він зміниться лише у Фазі 3 (PostHog) — окрема чернетка.
- Блок про Accessibility — без змін (рівень 1 нічого не бере з даних сервісу: події категорії B — лише лічильники кількості).

## Звірити перед публікацією
- Джерело GitHub Pages для політики (у `play-console-declarations.md` згадана гілка, якої вже нема) — підтвердити, звідки політика публікується.
- Фактична поведінка збігається з текстом: див. `docs/analytics-level1-checklist.md`, розділ «Ручний чекліст».

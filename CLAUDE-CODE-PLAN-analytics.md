# Tepera — план: відстеження використання і продуктові метрики

> **Для Claude Code.** Прочитай цей файл повністю. Почни з **Фази 0** (лише читання і звіт, без змін у коді) і зупинись, чекаючи на моє підтвердження. Далі працюй по одній ітерації за раз; після кожної — компіляція, тести, коміт, оновлення `ANALYTICS-PROGRESS.md`, коротка відповідь мені.

---

## 0. Контекст і мета

- Tepera — Android (Kotlin, Compose), застосунок про час у телефоні: години ризику, плани «якщо-то», м'який вихід, intention sheet, гейти через AccessibilityService, задачки як тертя, «Мої заняття».
- **Зараз застосунок працює БЕЗ дозволу INTERNET.** Це свідома властивість продукту і частина довіри.
- Мета на 3 місяці: активні користувачі (встановлення + утримання + фідбек). Потрібно розуміти воронку активації, використання ядра і де люди відпадають.
- Підхід — три рівні, які вмикаються послідовно:
  - **Рівень 0** — Play Console (без коду).
  - **Рівень 1** — локальні лічильники на пристрої + добровільне «Поділитися статистикою» (без INTERNET).
  - **Рівень 2** — PostHog (EU Cloud), анонімно, лише за згодою.

---

## 1. Правила роботи (обов'язкові)

1. **Маленькі ітерації.** У мене часті відключення світла. Кожна ітерація — завершений стан, що компілюється, з комітом. Ніяких великих незакомічених змін.
2. **Гілка:** `feature/analytics` від актуальної основної гілки. Не чіпай гілку з білінгом/Pro.
3. **Файл прогресу** `ANALYTICS-PROGRESS.md` у корені: що зроблено, що далі, відкриті питання. Оновлюй після кожної ітерації і комітом разом із кодом.
4. **Стоп-точки (нічого не роби без мене):**
   - будь-яка Room-міграція — спершу покажи міграцію і тест до неї;
   - додавання будь-якого дозволу в маніфест (у т.ч. INTERNET);
   - будь-яка зміна текстів розкриття Accessibility / onboarding-згод;
   - будь-яка подія, що походить з AccessibilityService або UsageStatsManager і має йти назовні (див. категорію B);
   - видалення або зміна поведінки існуючих фіч;
   - все, що може спричинити відхилення в Google Play.
   У таких місцях: опиши ризик, варіанти, постав питання — і чекай.
5. **Ніколи не надсилати назовні:** package name, назви застосунків, список встановлених застосунків, вільний текст користувача (назви занять, тексти планів), точні тривалості (тільки бакети), що-небудь з вмісту екрана.
6. **Не публікувати нічого** (Play Console, GitHub Pages, політика) — лише готувати чернетки у файлах. Це роблю я вручну.
7. Якщо в репозиторії є документи spec / decision log / verification log — додавай туди записи про рішення і перевірки в існуючому форматі.
8. Каталог подій тримай платформо-нейтральним (ті самі назви знадобляться для майбутнього iOS-порту).

---

## 2. Рішення, які ще за мною (заповню перед відповідними фазами)

| ID | Питання | Рекомендація | Моє рішення | Блокує |
|---|---|---|---|---|
| D1 | PostHog: opt-in (вимкнено за замовчуванням) чи opt-out | opt-in | ☐ | Фаза 3 |
| D2 | Рівень 1: декларувати в Data safety «App interactions — optional» через кнопку «Поділитися» | так (обережно) | ☐ | реліз Фази 2 |
| D3 | GeoIP у PostHog: вимкнути чи декларувати Approximate location | вимкнути | ☐ | Фаза 3 |
| D4 | Події категорії B (похідні від Accessibility/UsageStats) — тільки локально чи також у PostHog (потребує оновлення декларації Accessibility і повторного рев'ю) | тільки локально | ☐ | Фаза 3 |
| D5 | Регіон PostHog | EU (Франкфурт) | EU | — |
| D6 | Канал «Поділитися статистикою»: ACTION_SEND (email) чи попередньо заповнена Google Form | ACTION_SEND | ☐ | ітерація 2.5 |

Якщо рішення не заповнене — використовуй рекомендацію лише для **чернеток і коду за фіче-флагом**, але не вмикай у релізній збірці й нагадай мені.

---

## 3. Метрики, які хочемо бачити

| Метрика | Як рахуємо | Рівень |
|---|---|---|
| DAU, 28-day active, повернення, втрата користувачів | Play Console Statistics | 0 |
| Активація: онбординг → Accessibility увімкнено → перший план «якщо-то» | воронка з подій A1–A5 | 1 (сумарно), 2 (воронка) |
| Ядро: показано intention sheet / м'який вихід, вибір наміру, результат | події B1–B5 | 1 (локально); 2 лише якщо D4 |
| Тертя: задачки показано / розв'язано / покинуто | B6–B8 | 1; 2 лише якщо D4 |
| Використання розділів | A8 | 1, 2 |
| Утримання D1/D7/D30 за когортами | PostHog retention (анонімний distinct_id) | 2 |
| Пауза гейтів, віджети, плитка QS | A9–A11 | 1, 2 |

---

## 4. Каталог подій (початковий — звір із реальним кодом у Фазі 0)

Назви: `snake_case`, формат `об'єкт_дія`. Властивості — тільки з allowlist нижче.

### Категорія A — власний UI Tepera (дозволено для Рівнів 1 і 2)

| ID | Подія | Властивості |
|---|---|---|
| A1 | `onboarding_started` | — |
| A2 | `onboarding_step_completed` | `step` (enum назв кроків) |
| A3 | `onboarding_completed` | — |
| A4 | `accessibility_service_enabled` | — (факт, що власний сервіс Tepera увімкнено, перевірений через системні налаштування; не дані, зібрані сервісом) |
| A4b | `usage_access_granted` | — |
| A5 | `plan_created` | `is_first` (bool) |
| A6 | `plan_edited` / `plan_deleted` | — |
| A7 | `activity_added` (Мої заняття) | — (без назви заняття) |
| A8 | `tab_viewed` | `tab`: today / activities / data / settings |
| A9 | `gate_paused` | `duration`: today / weekend / until_date |
| A10 | `widget_added` | `widget` (enum типу віджета) |
| A11 | `qs_tile_added` | — |
| A12 | `feedback_form_opened` | — |
| A13 | `stats_share_opened` / `stats_shared` | — |
| A14 | `analytics_consent_changed` | `enabled` (bool) — надсилати назовні тільки після opt-in |

### Категорія B — тригериться Accessibility / UsageStats (за замовчуванням ТІЛЬКИ локально)

| ID | Подія | Властивості |
|---|---|---|
| B1 | `risk_window_started` | — |
| B2 | `intention_sheet_shown` | — |
| B3 | `intention_selected` | `bucket`: 5 / 15 / 30 / other |
| B4 | `soft_exit_shown` | — |
| B5 | `soft_exit_outcome` | `outcome`: left / continued |
| B6 | `puzzle_shown` | `type`: sudoku / math / digits / … (enum) |
| B7 | `puzzle_solved` | `type` |
| B8 | `puzzle_abandoned` | `type` |

### Allowlist властивостей

`step`, `is_first`, `tab`, `duration`, `widget`, `enabled`, `bucket`, `outcome`, `type` + службові `app_version`. Будь-що інше відкидається на рівні коду (не лише конвенцією).

---

## 5. Архітектура

```
interface Analytics {
    fun track(event: AnalyticsEvent)
}

sealed class AnalyticsEvent(val name: String, val category: Category, val props: Map<String, Any>)
enum class Category { A_OWN_UI, B_SYSTEM_DERIVED }

NoOpAnalytics          // тести, вимкнений стан
LocalAnalytics         // Room: денні лічильники
RemoteAnalytics        // PostHog, фільтр: category == A (або B, якщо D4), allowlist props, consent
CompositeAnalytics     // Local + Remote
```

- Події — типізовані (sealed class), щоб неможливо було передати довільну назву чи властивість.
- Впровадження через наявний DI (з'ясуй у Фазі 0; якщо DI немає — простий singleton у Application).
- Виклики `track()` — з ViewModel / сервісного шару, не з Composable напряму.

---

## Фаза 0 — Розвідка (без змін у коді)

Прочитай репозиторій і створи `ANALYTICS-RECON.md`:

1. Архітектура: модулі, DI (Hilt/Koin/ручний), навігація, де Application-клас.
2. Room: версія БД, чи експортуються схеми (`exportSchema`), чи є auto-migrations, наявні міграції і тести до них.
3. DataStore / SharedPreferences для налаштувань.
4. Маніфест: поточні дозволи, сервіси (AccessibilityService), віджети, QS tile.
5. Онбординг: кроки, де розкриття Accessibility.
6. Налаштування: структура екрана, чи є «видалити всі дані», де «Запропонувати функцію» (Google Form через ACTION_VIEW).
7. Мапінг каталогу подій (розділ 4) на реальні місця в коді: що існує, чого немає, що варто додати/перейменувати. Позначай категорію A/B для кожної точки.
8. Де лежить політика конфіденційності (файл у цьому чи окремому репо), якою мовою/мовами, чи є фрази «без інтернету» / «дані не залишають пристрій» — і в описі магазину теж.
9. minSdk / targetSdk, версія Kotlin/AGP, чи є unit/instrumented тести.
10. Ризики і питання до мене.

**Стоп. Чекай на підтвердження.**

---

## Фаза 1 — Рівень 0 (робить Серхій вручну, коду немає)

Чекліст для мене (Claude Code: просто додай його в `ANALYTICS-PROGRESS.md`):

- [ ] Play Console → Dashboard → «Your KPIs»: закріпити DAU, 28-day active users, returning users, user loss.
- [ ] Statistics: зберегти звіт «Активні користувачі по тижнях» і «Нові vs втрачені».
- [ ] Android vitals: перевірити crashes/ANR як базову лінію.
- [ ] Записати базові значення на сьогодні в `ANALYTICS-PROGRESS.md`.

---

## Фаза 2 — Рівень 1: локальні лічильники + «Поділитися статистикою»

Нових дозволів **немає**.

### Ітерація 2.1 — Інтерфейс і каталог
- `Analytics`, `AnalyticsEvent` (sealed), `Category`, `NoOpAnalytics`, allowlist.
- Unit-тести: недозволена властивість відкидається; категорія визначена для кожної події.
- Коміт.

### Ітерація 2.2 — Сховище (СТОП-ТОЧКА: міграція)
- Room-сутність `DailyEventCounter(date: LocalDate, eventName: String, propsKey: String, count: Int)`, PK = (date, eventName, propsKey). Жодних окремих подій із точним часом.
- DAO: increment (upsert), query за період, deleteOlderThan.
- Адитивна міграція (нова таблиця, без змін існуючих). **Покажи мені міграцію + тест міграції до застосування.**
- Коміт після мого OK.

### Ітерація 2.3 — LocalAnalytics + очищення
- `LocalAnalytics` пише через DAO у фоні (корутини, без блокування UI).
- WorkManager: щоденне видалення лічильників старших за 90 днів.
- Якщо є «видалити всі дані» — включити туди таблицю лічильників.
- Тести DAO.
- Коміт.

### Ітерація 2.4 — Інструментування
- Розставити `track()` за мапою з Фази 0. Категорія B — лише локально.
- Не додавати подій у гарячі цикли AccessibilityService без дебаунсу.
- Коміт (можна кількома комітами по екранах).

### Ітерація 2.5 — Екран «Статистика використання»
- Налаштування → «Поділитися анонімною статистикою».
- Екран попереднього перегляду: людською мовою + який саме текст буде надіслано (період, версія застосунку, версія Android, лічильники). **Без будь-якого ID.**
- Кнопка «Надіслати»: за D6 — ACTION_SEND (chooser, тема «Tepera stats», адреса `[SUPPORT_EMAIL]`) або Google Form через ACTION_VIEW.
- Рядки UA + EN.
- Події A13.
- Коміт.

### Ітерація 2.6 — Перевірка і документи
- Прогнати тести; ручний чекліст: лічильники ростуть, очищення працює, екран надсилання показує саме те, що надсилається.
- Чернетка змін до політики (розділ 8.1) у файлі `docs/privacy-policy-analytics-draft.md` (або поруч із політикою).
- Чернетка release notes.
- Оновити `ANALYTICS-PROGRESS.md`. Коміт.

**Перед релізом Фази 2 (робить Серхій):** оновити політику на GitHub Pages; Data safety за D2 (розділ 7).

---

## Фаза 3 — Рівень 2: PostHog

**Вхідна умова:** заповнені D1, D3, D4; я явно написав «починай Фазу 3». Додавання INTERNET — стоп-точка.

### Ітерація 3.1 — Залежність і конфігурація
- `implementation("com.posthog:posthog-android:<актуальна 3.x>")` — перевір актуальну версію.
- `<uses-permission android:name="android.permission.INTERNET"/>` — **після мого OK.**
- Ключ проєкту — через `gradle.properties`/`local.properties` → `BuildConfig.POSTHOG_KEY`; хост `https://eu.i.posthog.com`.
- Debug-збірка: або окремий dev-проєкт PostHog, або NoOp (ключ порожній → `NoOpAnalytics`). Метрики продакшну не забруднювати.
- Ініціалізація в `Application.onCreate`:
  ```kotlin
  val config = PostHogAndroidConfig(apiKey = BuildConfig.POSTHOG_KEY, host = "https://eu.i.posthog.com").apply {
      optOut = true                        // D1 = opt-in: нічого не надсилається до згоди
      personProfiles = PersonProfiles.NEVER // тільки анонімні події
      sessionReplay = false                 // не вмикати
      // captureScreenViews / autocapture — вимкнути, якщо увімкнені за замовчуванням; звір із документацією
  }
  ```
- `identify()` / `alias()` — **не викликати ніде.**
- Перевірити **merged manifest**: SDK не повинен додати інших дозволів (зокрема `com.google.android.gms.permission.AD_ID`). Якщо додає — `tools:node="remove"` і повідомити мене.
- Коміт.

### Ітерація 3.2 — RemoteAnalytics
- `RemoteAnalytics`: пропускає тільки категорію A (B — тільки якщо D4 = так), тільки allowlist властивостей, тільки коли згода надана.
- `CompositeAnalytics` = Local + Remote.
- Unit-тести: подія B не йде в Remote при D4 = ні; невідома властивість відкидається; без згоди — нуль викликів SDK.
- Коміт.

### Ітерація 3.3 — Згода і керування
- Крок в онбордингу «Допомогти покращити Tepera» (окремий від розкриття Accessibility, не змінює його текст): коротко що збирається / що ні, кнопки «Так» / «Ні, дякую». Без dark patterns, однакова вага кнопок.
- Налаштування: перемикач → `PostHog.optIn()` / `PostHog.optOut()`; показ «ID аналітики» (distinct_id) з копіюванням; кнопка «Скинути ID» (`PostHog.reset()`).
- Існуючим користувачам після оновлення: один ненав'язливий екран/картка з тим самим вибором; за замовчуванням — вимкнено (D1).
- Рядки UA + EN.
- Коміт.

### Ітерація 3.4 — Верифікація
- Debug-логи SDK + мережевий інспектор: до згоди — нуль запитів до posthog.com; після — тільки події з allowlist; жодних package name.
- Перевірити на Android 8, 9, 15, 16 (мої пристрої: Huawei P9, Sony XZ1 Compact, Motorola G84, Samsung S23) — що нічого не падає без мережі, події буферизуються.
- Записати результати у verification log / `ANALYTICS-PROGRESS.md`.
- Коміт.

### Ітерація 3.5 — Документи (без публікації)
- Чернетка оновленої політики (розділ 8.2) — зміни відносно поточної, UA + EN.
- Заповнена таблиця відповідей Data safety (розділ 7) для мене.
- Чернетка release notes.
- Коміт.

**Перед релізом Фази 3 (робить Серхій):**
1. Налаштувати проєкт PostHog EU: **Discard client IP data = ON**; GeoIP-трансформацію вимкнути (D3) або задекларувати Approximate location.
2. Опублікувати оновлену політику і прибрати «без інтернету»/«дані не залишають пристрій» з політики та опису в магазині.
3. Оновити Data safety **до** викочування білда (декларація має покривати всі версії в Play).
4. Декларацію Accessibility — без змін, якщо D4 = ні. Якщо D4 = так — оновити і готуватися до повторного рев'ю.
5. Реліз спершу в Open testing, потім Production.

---

## 7. Data safety — відповіді (для Серхія)

### Після Фази 2 (якщо D2 = так)
| Пункт | Відповідь |
|---|---|
| App activity → App interactions | Collected · Optional · Purpose: Analytics |
| Shared | No |
| Encrypted in transit | Так (передача через email/форму користувачем) — уточнити формулювання під канал D6 |
| Запит на видалення | Так — `[SUPPORT_EMAIL]` |

### Після Фази 3
| Пункт | Відповідь |
|---|---|
| App activity → App interactions | Collected · Optional (opt-in) · Analytics |
| Device or other IDs | Collected · Optional · Analytics (анонімний ID PostHog, обережне декларування) |
| Approximate location | Лише якщо GeoIP не вимкнено (D3) |
| Crash logs / Diagnostics | Лише якщо ввімкнено error tracking (зараз — ні) |
| Shared with third parties | No (PostHog — постачальник послуг) |
| Encrypted in transit | Yes |
| Users can request deletion | Yes — email + ID аналітики з налаштувань |
| Collection optional | Yes — доступно всім користувачам у всіх регіонах |

---

## 8. Чернетки для політики конфіденційності

Адаптуй під структуру і мову(и) поточної політики. `[SUPPORT_EMAIL]` — плейсхолдер.

### 8.1 Рівень 1 — Usage statistics (on-device)

**EN**
> **Usage statistics.** Tepera keeps simple daily counters of how its features are used (for example, how many times the onboarding was completed or a plan was created). These counters are stored only on your device, contain no app names, no text you enter and no identifiers, and are deleted automatically after 90 days or when you delete the app's data. They are sent to us only if you choose "Share anonymous statistics" in Settings; before sending, the app shows you exactly what will be shared. You can use Tepera without ever sharing them.

**UA**
> **Статистика використання.** Tepera веде прості щоденні лічильники використання функцій (наприклад, скільки разів пройдено онбординг чи створено план). Вони зберігаються лише на вашому пристрої, не містять назв застосунків, введеного вами тексту чи ідентифікаторів і автоматично видаляються через 90 днів або разом із даними застосунку. Нам вони надходять лише тоді, коли ви самі обираєте «Поділитися анонімною статистикою» в налаштуваннях; перед надсиланням застосунок показує, що саме буде передано.

### 8.2 Рівень 2 — Analytics (PostHog)

**EN**
> **Product analytics (optional).** If you turn on "Help improve Tepera", the app sends anonymous usage events (for example, which screen was opened or that a plan was created) to PostHog, our analytics provider, hosted in the EU (Frankfurt, Germany). Events are linked to a random analytics ID generated by the app, not to your name, email or Google account. We do not send names of other apps, your activity texts, screen content or any data obtained through the Accessibility service. Device model, Android version, app version and language are included. IP addresses are not stored. Analytics is off until you turn it on; you can turn it off or reset your analytics ID at any time in Settings. To request deletion of analytics data, email [SUPPORT_EMAIL] with the analytics ID shown in Settings. PostHog processes this data on our behalf as a data processor.

**UA**
> **Продуктова аналітика (необов'язково).** Якщо ви вмикаєте «Допомогти покращити Tepera», застосунок надсилає анонімні події використання (наприклад, який екран відкрито чи що створено план) сервісу аналітики PostHog із серверами в ЄС (Франкфурт, Німеччина). Події прив'язані до випадкового ID аналітики, який генерує застосунок, а не до вашого імені, email чи облікового запису Google. Ми не надсилаємо назви інших застосунків, тексти ваших занять, вміст екрана чи будь-які дані, отримані через службу спеціальних можливостей (Accessibility). Передаються модель пристрою, версія Android, версія застосунку і мова. IP-адреси не зберігаються. Аналітика вимкнена, доки ви її не ввімкнете; вимкнути її чи скинути ID можна будь-коли в налаштуваннях. Щоб видалити дані аналітики, напишіть на [SUPPORT_EMAIL], вказавши ID аналітики з налаштувань. PostHog обробляє ці дані від нашого імені як обробник даних.

> Примітка для Серхія: формулювання щодо GDPR (правова підстава — згода, право на видалення, міжнародна передача) варто звірити з шаблоном або юристом.

---

## 9. Definition of Done

- Фаза 2: лічильники пишуться і чистяться; екран «Поділитися» показує рівно те, що надсилається; немає нових дозволів; тести зелені; чернетка політики готова.
- Фаза 3: до згоди — нуль мережевих запитів; назовні тільки категорія A (або B за D4) з allowlist; немає `identify()`, session replay, AD_ID; перевірено на 4 пристроях; чернетки політики й Data safety готові; нічого не опубліковано без мене.

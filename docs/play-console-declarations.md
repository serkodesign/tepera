# Play Console — чернетка відповідей (Data Safety / Content Rating / Target audience)

Це не сама форма — Play Console доступний лише через браузер з акаунтом власника, Claude Code
туди не має доступу. Цей файл — чернетка відповідей на основі реального коду (маніфест,
залежності, наявні екрани), яку можна швидко перенести в Play Console вручну. Перевір кожен пункт
проти актуального інтерфейсу Play Console перед подачею — форми Google міняються з часом.

## Privacy Policy URL

`https://serkodesign.github.io/tepera/privacy-policy` — GitHub Pages з гілки `w-0-audit`, папка
`/docs` (`docs/privacy-policy.md`, оновлено 28.09.2026, включно з перемикачем "Звіти про збої",
D-20). Перевірено 28.09.2026: сторінка відкривається без входу в акаунт і відповідає актуальному
тексту файлу. Цю URL вписати в Play Console **Data Safety → Privacy policy link** і **App content /
Store listing → Privacy Policy**, а також на екран "Про застосунок" (FR-6.9), якщо там ще стоїть
заглушка. **Коли `w-0-audit` змерджиться в `main`** — варто перемкнути джерело Pages назад на
`main`/`/docs` (Settings → Pages), інакше сайт продовжить збиратись зі старої гілки.

## Важлива знахідка перед заповненням: реальний список дозволів

`app/src/main/AndroidManifest.xml` сам по собі декларує лише `PACKAGE_USAGE_STATS` і
`POST_NOTIFICATIONS`. Але Google Play показує користувачам і враховує в Data Safety **фінальний
змерджений маніфест** APK/AAB — а він містить додатково:

- `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` — підтягнуті бібліотекою `firebase-crashlytics`
  (Фаза 5) для завантаження crash-звітів.
- `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` — підтягнуті WorkManager/Crashlytics
  інфраструктурою.
- `com.android.vending.BILLING` — БІЛЬШЕ НЕ присутній: RevenueCat вилучено з релізу 1.0 (D-01).
  Release-маніфест перевірено 21.09.2026 (`merged_manifest/release/…`): PACKAGE_USAGE_STATS,
  POST_NOTIFICATIONS, WAKE_LOCK, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, INTERNET
  (останні п'ять — транзитивно). Після збірки release-AAB перевірити ще раз (V-08).

Перевірено прямим переглядом `app/build/intermediates/merged_manifest/release/…/AndroidManifest.xml`
після збірки. Попередні сесії кілька разів фіксували "INTERNET не додається" (CLAUDE.md,
ROADMAP.md) — це лишається правильним ЛИШЕ щодо конкретної фічі "Запропонувати функцію" (яка
справді не потребує цього дозволу сама), не про застосунок в цілому. Для Data Safety форми
важливий саме повний список.

## Data Safety form

### Чи застосунок збирає чи передає дані?
**Так** (через Crashlytics) — не можна відповісти суцільним "Ні".

### Покупки — не застосовується (оновлено 21.09.2026, SRS v4 / D-01)
Платіжний шар (RevenueCat, "Пригостити кавою", Tepera Pro) вилучено з релізу 1.0 (гілка
`feature/monetization-revenuecat`). Release-маніфест більше не містить `com.android.vending.BILLING`.
Financial info / Purchase history і RevenueCat-ідентифікатори в Data Safety **не декларуються**; на питання
про in-app purchases у Content Rating — "Ні"; Payments-декларація не потрібна.

### Дані, що НЕ вважаються "зібраними" (обробляються лише на пристрої, ніколи не передаються)
За офіційним визначенням Google ("Data isn't collected if it's processed ephemerally or stored
locally on the device, and isn't transmitted off the device") — ці типи можна позначити як
**"Дані не збираються"**:

- **Встановлені застосунки / активність в інших застосунках** (`UsageStatsManager` — Online-час,
  тепловий патерн, детекція пауз, лічильник розблокувань). Обробляється виключно локально
  (`BalanceRepository`/`PatternRepository`/`PauseRepository`/`UnlockRepository`), ніколи не
  передається — підтверджено відсутністю мережевого коду в цих класах і відсутністю власного
  бекенда взагалі.
- **Записи активностей користувача** (категорія, тривалість, нотатка) — лише Room, локально.
- **Оцінки користувача** (`user_estimates`, T-3/T-14/T-10) — лише Room, локально.

### Дані, що вважаються "зібраними" — Crash logs
- **Тип даних:** App info and performance → Crash logs (і, за замовчуванням Crashlytics SDK, App
  info and performance → Diagnostics; можливо Device or other IDs — Firebase Installation ID,
  який Crashlytics використовує для групування звітів з одного пристрою).
- **Чи передається за межі пристрою:** Так, у Firebase/Google.
- **Чи збір необов'язковий:** Так. Перемикач «Звіти про збої» — на першому екрані онбордингу й
  завжди в Налаштування → Загальні (`util/CrashReporting.kt`, D-20); людина може вимкнути будь-
  коли, і збір справді зупиняється (`setCrashlyticsCollectionEnabled`). На час закритого тесту
  вимкнений стан — НЕ дефолт: перемикач увімкнений за замовчуванням, вимкнення — свідома дія
  користувача.
- **Мета використання:** App functionality (виправлення збоїв). Не Analytics, не Advertising,
  не Personalization — застосунок свідомо БЕЗ `firebase-analytics` (build.gradle.kts,
  коментар "СВІДОМО без firebase-analytics").
- **"Поширюється з третіми особами" чи ні:** Google офіційно кваліфікує Firebase як
  service provider, коли дані використовуються лише для функціональності застосунку й не для
  власних цілей Google/Firebase — типова практика: позначати "Зібрано, але НЕ передається третім
  особам" (service-provider виняток), не "Так, передається". **Звір це з офіційною таблицею
  Firebase → Play Data Safety** (Firebase Console зазвичай має пряме посилання "Data disclosure"
  саме для цього) — вона авторитетніша за цю чернетку й оновлюється незалежно.
- **Шифрування при передачі:** Так (Firebase завжди по TLS).
- **Можливість видалення на запит користувача:** Дані анонімні, не прив'язані до акаунту чи
  особи (немає системи акаунтів). Типова практика для такого випадку — позначити "Ні" (нема
  механізму видалення КОНКРЕТНОГО користувача, бо його неможливо ідентифікувати) або залишити
  порожнім, якщо форма це дозволяє. Остаточне рішення — за власником акаунту Play Console.

### Практики безпеки
- Дані шифруються при передачі: Так (стосується лише crash-звітів, це єдине, що передається).
- Можна запросити видалення даних: див. вище, по типу даних окремо.
- Застосунок проходить незалежну перевірку безпеки: Ні (немає підстав відповідати "Так" без
  реального аудиту).

## Content Rating Questionnaire (IARC)

Застосунок не має: насильства, сексуального контенту, азартних ігор, контенту, створеного
користувачами й видимого іншим користувачам, спілкування між користувачами, реклами. **Оновлено
21.09.2026:** платежів у релізі 1.0 немає (D-01) — на питання про **цифрові покупки / in-app purchases**
відповідати "Ні". Очікуваний результат анкети — найнижча категорія віку (PEGI 3 / "Everyone").
Відповідати "Ні" на питання про насильство/контент для дорослих/азартні ігри/соціальні функції.

## Target audience and content

Продукт свідомо для дорослих (CLAUDE.md: "Аудиторія: дорослі 20-45, втомлені від скролінгу") —
не спроєктований і не позиціонується для дітей. Рекомендація: НЕ позначати застосунок як
"Designed for children" / не обирати вікові групи молодше 18 у "primary audience", навіть якщо
форма вимагає вказати якийсь діапазон для технічної відповідності — орієнтуватись на "18 and
over" як основну аудиторію. Це звільняє від додаткових вимог Families policy (обмеження на
рекламу/трекінг для дитячого контенту), які й так нерелевантні (реклами й трекінгу немає
взагалі).

## App access

Функція, що потребує явного пояснення рецензенту: `PACKAGE_USAGE_STATS` — не runtime-дозвіл,
надається вручну через Налаштування Android (Спеціальний доступ). Play Console "App access"
секція типово питає, чи всі функції доступні без спеціальних кроків — тут відповідь "Ні", з
поясненням: без цього дозволу Online-час/патерн/паузи не рахуються, але ручне логування
активностей лишається повністю доступним і тестованим без жодних додаткових кроків.

## Ads declaration
Немає реклами в застосунку (CLAUDE.md). Позначити "No ads." Платежів у релізі 1.0 теж немає.

## Видалення даних (закрито 21.09.2026)

Раніше `privacy-policy.md` обіцяв "видалити всі свої дані через налаштування", а кнопки не було. Тепер
є: Налаштування → Резервне копіювання → "Видалити всі дані" (два кроки підтвердження: пояснення, потім
слово-підтвердження). Політику оновлено з точним шляхом.


## Data safety: підсумок рівнів 1+2 (аналітика, 27.11.2026) — ЗАПОВНЮВАТИ ЦЕЙ РОЗДІЛ

Заповнюється **до** викочування збірки з ключем PostHog (декларація має покривати всі версії в треку). Рівень 1 (лічильники
на пристрої + лист) і рівень 2 (PostHog, opt-in) декларуються разом. Усе нижче звірити з поточним текстом форми Play Console —
формулювання категорій Google інколи змінює.

**Загальні питання**
| Питання форми | Відповідь |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** (було б «No» без Crashlytics і PostHog) |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (Crashlytics і PostHog — лише HTTPS) |
| Do you provide a way for users to request that their data be deleted? | **Yes** — в застосунку: Налаштування → Дані й приватність → «Видалити мої дані аналітики» (скидає ID + готовий лист); «Видалити всі дані» — локальні дані. Для Crashlytics окремого механізму нема (дані без акаунта) — це пояснюється в політиці |
| Data deletion URL (якщо форма просить) | URL політики: `https://serkodesign.github.io/tepera/privacy-policy` (розділ «Продуктова аналітика»). Окремої сторінки видалення нема; якщо Google вимагатиме — додати розділ-якір чи сторінку |

**Типи даних, які позначаємо «Collected»** (усе інше — «Not collected»)
| Категорія → тип | Collected | Shared | Optional | Purpose | Звідки |
|---|---|---|---|---|---|
| App activity → App interactions | Yes | **No** (PostHog — service provider/обробник) | **Yes** (opt-in) | **Analytics** | події категорії A у PostHog (за згодою); рівень 1 лічильники — лише на пристрої/лист користувача, окремо не додають |
| Device or other IDs | Yes | **No** | **Yes** (аналітика) | **Analytics** (PostHog anonymous ID); **App functionality** (Firebase installation ID для Crashlytics) | випадковий distinct_id PostHog + Crashlytics |
| App info and performance → Crash logs | Yes | No (service provider) | Yes (перемикач «Звіти про збої») | **App functionality** | Crashlytics — без змін |
| App info and performance → Diagnostics | Yes | No | Yes | App functionality (і Analytics для device/OS даних PostHog — модель, версія ОС, мова, часовий пояс, розмір екрана) | Crashlytics + службові властивості PostHog; якщо форма не має окремого пункту для моделі/ОС, віднести до Diagnostics/Device or other IDs |

**Явно «Not collected»:** Location (approximate і precise — GeoIP вимкнено на кожній події, IP не зберігається), Personal info (ім'я,
email, акаунт), Financial info, Health and fitness, Messages, Photos/Videos, Audio, Files, Calendar, Contacts, Web browsing, Search
history, **Installed apps / app usage на пристрої** (UsageStatsManager обробляється лише локально — Google не вважає це збором,
бо не передається), користувацький текст (нотатки, плани).
**Важливо:** події категорії B (Accessibility/UsageStats-похідні) у PostHog НЕ йдуть (`allowCategoryB = false`) — тому декларація
Accessibility лишається без змін.

**Practices:** Data is encrypted in transit — Yes. Independent security review — No. Committed to Play Families Policy — Not applicable
(аудиторія 18+).

**Порядок дій у Play Console (по кроках)**
1. Play Console → App content → **Data safety** → Manage → пройти форму за таблицею вище, зберегти.
2. App content → **Privacy policy**: URL має відкривати оновлений текст (перевірити дату «27.11.2026» і розділи «Продуктова аналітика»
   та «Статистика використання»). Спершу опублікувати політику (merge у гілку GitHub Pages), потім змінювати Data safety.
3. Store listing: вставити оновлений абзац «Privacy» з `store-listing-en.md` / `store-listing-ua.md` (рядок 49).
4. Release notes для збірки: `CHANGELOG-analytics-level2-draft.md` (розділ «Для Play Console»).
5. Decl. Accessibility — **без змін** (D4 = ні), нічого не перезавантажувати.
6. Перед викочуванням: PostHog → Discard client IP data (ON), GeoIP-трансформація OFF, Data retention 12 міс. (див. `posthog-setup-steps.md`, розділ 6а);
   у збірці — реальний ключ у `local.properties`, `bundleBetaRelease`, версія без `-beta`.
7. Після відправки на Open testing — перевірити, що Google не повернув попередження щодо розбіжності Data safety і політики.

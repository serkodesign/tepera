# Tepera: виправлення Accessibility і повторне подання (1.1, Open testing)

Версія плану: v2, 05.11.2026. Замінює v1 (build 31). Бриф для коду — `CLAUDE-CODE-accessibility-fix.md` (v2), аудит — `AUDIT-accessibility.md`.

Google відхилив build 30 з двох причин: у застосунку немає окремого екрана розкриття, а в описі на Google Play не згадано Accessibility. Обидві закриваються одним поданням збірки **1.1 (versionCode 32)** у трек **Open testing**. Production лишається на 1.0.5.

## Рішення від 05.11.2026

1. Firebase лишається. Скрізь пишемо «Tepera нікуди не передає дані про відкриті застосунки», а не «немає доступу до інтернету» (D-49).
2. Затемнення не змінюємо: кнопки «Ще 5» і «Вийти», дотики проходять крізь нього. Виправляємо лише те, що воно могло лишитися над Tepera, налаштуваннями, Google Play, інсталятором і діалогами дозволів.
3. Тексти описують те, що є в коді: ідеї замість стрічки і фіксовані 5 хвилин, без плану «якщо… то…».
4. Тексти згадують обидві функції служби: «Підказка поверх стрічки» і «Затримка при будь-якому відкритті».
5. Опис у Play: текст 1.0.5 плюс розділ про службу з позначкою «тестова версія 1.1». Опис v3 ставимо, коли 1.1 піде в Production.

## Чекліст

- [ ] **Код (Claude Code, кроки 1–9 брифу, по коміту на крок):** назва й опис служби, згода, повноекранне розкриття, одна точка входу, служба без згоди нічого не робить, затемнення над системними екранами, стан «вимкнено», версія 1.1 (32), Privacy Policy.
- [ ] **Перевірка на S23 і Moto G84** за чеклістом з брифу.
- [ ] **Відео** за сценарієм нижче → YouTube, доступ «за посиланням». Оновити субтитри `docs/play-store/accessibility-demo.en.srt`.
- [ ] **Privacy Policy на GitHub Pages:** переконатися, що зміни кроку 9 опубліковані.
- [ ] **Play Console → App content → Accessibility API:** відповіді й текст нижче, посилання на нове відео.
- [ ] **Store listing:** повний опис UA та EN нижче. Короткий опис і назва — без змін.
- [ ] **Data safety:** нічого не міняти. Звіти про збої (Crashlytics) там уже задекларовані — так і лишається.
- [ ] **Open testing → новий реліз 1.1 (32)** з release notes → Publishing overview → Send for review.

- [ ] **Коли 1.1 піде в Production:** окремо вирішити, яким варіантом збірки (зараз Production — `standard` без v3 і служби), і замінити опис на опис v3.

Опис на Google Play спільний для всіх треків. Його бачать і люди з Production 1.0.5, і учасники відкритого тесту. Тому основний текст лишається від 1.0.5, а функції служби позначено «тестова версія 1.1».

## Опис на Google Play

Короткий опис і назву не змінюємо. Повний опис нижче — це текст 1.0.5 з `docs/store-listing-*.md` (тег `v1.0.5`), до якого додано розділ «Паузи й підказки поверх застосунків» перед «Дозволами» і одне речення в «Дозволах». Якщо зараз у Play Console стоїть інший текст (наприклад, опис v3 від 03.11), заміни його цим. Ліміт — 4000 символів.

### Повний опис — українська (3757/4000)

```text
**Tepera — спокійний трекер екранного часу й журнал офлайн-активностей. Не блокувальник екрана і не лічильник провини.**

Застосунок показує структуру вашого дня — скільки часу онлайн, що ви самі відмітили офлайн, і скільки лишається на все інше — і дає за 1-2 тапи зафіксувати, чим наповнене життя поза телефоном.

**Що показує застосунок:**

📊 **Структура дня, а не протиставлення** — одна шкала, три нейтральні частини: Online (рахується автоматично за використанням телефону), ваші відмітки за категоріями (Природа, Читання, Хобі/Творчість, Рух/Спорт, Живе спілкування, Справи — плюс до двох власних) і решта дня. Решта дня ніколи не подається як прогалина чи втрачений час.

⚡ **1-2 тапи, і назад до життя** — швидкий таймер активності або ручний запис, жодних відволікаючих екранів.

📱 **Використання телефону чесно** — Online-час за день, патерн «коли ви з телефоном», кількість розблокувань і час, коли ви востаннє брали телефон. Ці числа видно, коли ви самі заходите подивитись, а не як нав'язливі сповіщення.

🧭 **Орієнтир, не ціль** — тиха позначка на шкалі, яку ви самі налаштовуєте. Вона ніколи не змінює колір і нічого не «провалює». Порівняння — лише з власною історією, ніколи з чужими середніми.

☕ **Паузи без телефону** — якщо між використаннями минає достатньо часу, застосунок пропонує позначити цю паузу категорією, замість покладатись лише на ручний журнал.

⏳ **Застосунки з затримкою (за бажанням)** — поставте собі коротку паузу перед відкриттям обраних застосунків, наприклад соцмереж. Це свідома затримка, яку ви ставите самі собі, не технічний бар'єр, і вимкнути її на сьогодні можна одним тапом.

🧩 **Віджети на домашньому екрані** — 1x1, 2x1, 4x1 і 4x2: запускайте таймер активності просто з домашнього екрана й бачте день одним поглядом.

📈 **Статистика** — день, тиждень або місяць: час за категоріями й тренд Online.

🚫 **Список виключень** — карти, е-книги, банкінг не рахуються в Online-часі.

🌗 **Світла й темна тема, українська та англійська.**

🔒 **Приватність за замовчуванням** — статистика використання обробляється лише на пристрої й нікуди не передається. Жодних акаунтів, соціальної стрічки чи реклами. Необов'язкові звіти про збої вимикаються в Налаштування → Дані й приватність. «База знань» у застосунку пояснює, які саме дані використовуються і навіщо.

Створено в «монастирському» стилі — без бейджів, без стріків, без балів за використання. Просто спокійний погляд на день для тих, хто втомився від нескінченного гортання й хоче здоровішого балансу між телефоном і реальним життям.

🔔 **Паузи й підказки поверх застосунків (тестова версія 1.1)** — у тестовій версії 1.1 дві необов'язкові функції, «Підказка поверх стрічки» і «Затримка при будь-якому відкритті», використовують службу спеціальних можливостей Android (Accessibility Service API). Служба визначає лише, який застосунок ви відкрили, щоб у цей момент показати коротку паузу перед застосунками, які ви обрали, або ідею замість стрічки у ваші години скролінгу, а через 5 хвилин м'яко затемнити екран з кнопками «Ще 5» і «Вийти». Служба вмикається лише після вашої згоди на окремому екрані в застосунку. Вона не читає вміст екрана, повідомлення, текст чи паролі, не натискає нічого замість вас, не змінює налаштування і не заважає закрити, вимкнути чи видалити будь-який застосунок. Tepera нікуди не передає дані про відкриті застосунки. Без цієї служби решта Tepera працює як звичайно.

**Дозволи:** Tepera просить доступ до статистики використання (Налаштування → Спеціальний доступ) для підрахунку Online-часу і, за бажанням, сповіщення. У тестовій версії 1.1 — також службу спеціальних можливостей (необов'язково). Без доступу офлайн-журнал працює повністю.

---

*Tepera безкоштовна й без реклами. Без платних функцій і покупок у застосунку.*
```

### Full description — English (3643/4000)

```text
**Tepera is a calm screen time tracker and offline activity log — not a screen blocker and not a guilt counter.**

It shows your day's structure — how much of it was online, what you logged offline, and how much is left for everything else — and lets you log what your offline life is made of in 1-2 taps.

**What the app shows:**

📊 **A structure, not an opposition** — one scale, three neutral parts: Online (counted automatically from your phone usage), what you've logged by category (Nature, Reading, Hobbies, Movement, Time with people, Errands — plus up to two of your own), and the rest of the day. The rest of the day is never shown as a gap or lost time.

⚡ **1-2 taps, then back to life** — a quick activity timer or a manual entry, no distracting screens. Log it and get back to the moment.

📱 **Phone usage, honestly** — daily online time, a pattern of when you use your phone, unlock count and when you last picked it up. You see these numbers when you look for them, not as pushy alerts.

🧭 **A reference point, not a goal** — a quiet mark on the scale that you set yourself. It never changes color and never "fails". Comparisons are only ever against your own history, never against an average.

☕ **Phone-free pauses** — when enough time passes between uses, the app offers to label that gap with a category, instead of relying on you to remember to log it.

⏳ **Apps with a delay (optional)** — add a short pause of your own choosing before opening specific apps, such as social media. It is a deliberate delay you set for yourself, not a technical barrier, and you can turn it off for the day with one tap.

🧩 **Home-screen widgets** — 1x1, 2x1, 4x1 and 4x2: start a timer for an activity right from the home screen and see the day at a glance.

📈 **Statistics** — a day, week or month view of your time by category and your online trend.

🚫 **Exclusion list** — maps, e-books, banking apps don't count toward Online time.

🌗 **Light and dark theme, English and Ukrainian.**

🔒 **Private by design** — usage statistics are processed on your device only and are never sent anywhere. No accounts, no social feed, no ads. Optional crash reports can be switched off in Settings → Data and privacy. The in-app Knowledge base explains exactly what data is used and why.

Built in a "monastic" style — no badges, no streaks, no points for usage. Just a calm look at your day, for people tired of endless scrolling who want a healthier balance between phone time and real life.

🔔 **Pauses and ideas over apps (test version 1.1)** — in test version 1.1, two optional features, "Idea over the feed" and "Delay on any app launch", use Android's Accessibility Service API. The service only detects which app you open, so that at that moment it can show a short pause before the apps you chose, or an idea instead of the feed during your scrolling hours, and gently dim the screen after 5 minutes with "5 more" and "Leave" buttons. The service turns on only after you agree on a separate screen in the app. It doesn't read screen content, messages, text or passwords, doesn't tap anything for you, doesn't change settings, and never stops you from closing, disabling or uninstalling any app. Tepera doesn't send data about opened apps anywhere. Without this service, the rest of Tepera works as usual.

**Permissions:** Tepera asks for usage access (Settings → Special access) to count Online time, and optionally for notifications. Test version 1.1 can also use the accessibility service (optional). Without usage access, offline logging still works fully.

---

*Tepera is free, with no ads. No paid features and no in-app purchases.*
```

## Екран розкриття

Окремий повноекранний крок у стилі Tepera v3.1. З'являється перед тим, як застосунок відкриє налаштування Accessibility, з будь-якої з трьох точок входу: рядок «Затримка перед застосунками» в онбордингу, перемикач «Підказка поверх стрічки», картка «Затримка при будь-якому відкритті». Макет — у Figma, файл «Tepera». Тексти й правила показу — у брифі, крок 3 (джерело правди для коду).

| Елемент | UA | EN |
| --- | --- | --- |
| Заголовок | Паузи й підказки працюють через службу спеціальних можливостей | Pauses and ideas need Android's accessibility service |
| Навіщо | Ця служба Android (Accessibility Service API) дає Tepera знати, коли ви відкриваєте застосунок. Тільки так Tepera може в момент відкриття показати коротку паузу перед застосунками, які ви обрали, або ідею замість стрічки у ваші години скролінгу, а через 5 хвилин — м'яко затемнити екран. | This Android service (Accessibility Service API) lets Tepera know when you open an app. That's the only way Tepera can show a short pause before the apps you chose, or an idea instead of the feed during your scrolling hours, right when you open them, and gently dim the screen after 5 minutes. |
| Що Tepera бачить | Лише назву застосунку, який зараз відкритий. | Only the name of the app that's currently open. |
| Чого Tepera не бачить і не робить | Не читає повідомлення, текст, паролі чи те, що на екрані. Не натискає нічого замість вас. Не заважає закрити чи видалити будь-який застосунок. | It doesn't read messages, text, passwords or anything on your screen. It doesn't tap anything for you. It never stops you from closing or uninstalling any app. |
| Куди йдуть дані | Нікуди. Tepera обробляє назви відкритих застосунків лише на вашому телефоні й нікуди їх не передає. | Nowhere. Tepera processes the names of opened apps only on your phone and doesn't send them anywhere. |
| Підпис | Вимкнути службу можна будь-коли в налаштуваннях Android. Без неї решта Tepera працює як звичайно. | You can turn it off anytime in Android settings. Without it, the rest of Tepera works as usual. |
| Кнопки | Погоджуюсь / Не зараз | Agree / Not now |

Назва служби в налаштуваннях Android: «Tepera: паузи й підказки» / "Tepera: pauses and ideas".

## Декларація Accessibility в Play Console

Tepera описується як застосунок, що **не є** інструментом для людей з інвалідністю, тому `isAccessibilityTool` не встановлюємо. Поля написані за переліком питань із [довідки Google](https://support.google.com/googleplay/android-developer/answer/10964491). Якщо формулювання у формі відрізняється, скинь скрін.

| Питання | Відповідь |
| --- | --- |
| Is your app an accessibility tool? | No |
| Why does your app need to use the Accessibility Services API? | App functionality |
| Do you collect or share user data via the API? | No. Назви пакетів обробляються лише на пристрої й нікуди не передаються. Якщо форма все ж просить тип даних, найближчий — «App activity / App interactions», з поясненням, що це обробка лише на пристрої. |
| Video link | Посилання на нове відео |

### Текст для полів з описом і обґрунтуванням

```text
Tepera is a screen time app. Two optional features use the AccessibilityService API: "Idea over the feed" and "Delay on any app launch". The service only receives TYPE_WINDOW_STATE_CHANGED events and reads the package name of the app that comes to the foreground.

When the user opens an app they chose in Tepera, Tepera shows a short pause screen before it (delay), or, during the user's own "scrolling hours", a suggestion of something to do instead of the feed with an "Open for 5 min" option. After 5 minutes the screen is gently dimmed (the dim layer does not intercept touches) with "5 more" and "Leave" buttons. On repeated re-entries, continuing may ask for a short confirmation or a simple puzzle; leaving or closing the app is always available.

The service does not retrieve window content (canRetrieveWindowContent=false), does not perform actions or gestures, does not change settings, and never prevents the user from closing, disabling or uninstalling any app. Nothing is shown over system settings, the launcher, Google Play, the package installer, permission dialogs or Tepera itself.

These features turn on only after a separate full-screen in-app prominent disclosure and an explicit "Agree" tap. If the user declines, or later turns the service off, the rest of the app keeps working and only these features stay off. If the service is enabled without consent, it does nothing until the user agrees.

Package names of opened apps are processed only on the device and are never sent anywhere. Tepera has no account and no ads. The app's network access is used only by optional crash reporting (Firebase Crashlytics), a "what's new" text (Firebase Remote Config) and Google Play in-app update and review prompts; none of them receives data from the accessibility service.

Why a narrower API is not enough: Tepera already uses UsageStatsManager for statistics, but it has no real-time callback for app launches. Detecting the moment an app is opened with it would require constant background polling, and the pause or suggestion would appear after the user is already in the app. The accessibility event is the only way to show it at the moment of opening without polling.
```

## Сценарій відео

Одне відео на 1,5–2 хвилини записуєш на S23 вбудованим записом екрана і викладаєш на YouTube з доступом «за посиланням». Мова застосунку й субтитри — англійською. Перед записом: очисти дані Tepera або вимкни службу й скинь згоду (свіжа установка надійніше), задай годину скролінгу, що охоплює час зйомки, обери як «стрічку» нейтральний застосунок без особистих даних, увімкни «Не турбувати». Очікування 5 хвилин вирізаєш і ставиш субтитр «5 minutes later».

| # | На екрані | Субтитр |
| --- | --- | --- |
| 1 | Відкриваєш Tepera з домашнього екрана | Opening Tepera |
| 2 | Налаштування → перемикач «Idea over the feed» | Turning on "Idea over the feed" |
| 3 | Екран розкриття тримаєш 3–4 секунди | Prominent disclosure shown before the permission request |
| 4 | **Not now**: екран закривається, перемикач лишається вимкненим | User declines: feature stays off, the app keeps working |
| 5 | Знову вмикаєш перемикач: екран розкриття з'являється знову | Disclosure shown again when the user retries |
| 6 | **Agree**: налаштування Accessibility → «Tepera: pauses and ideas» → увімкнути → підтвердити системний діалог | User agrees and turns on the service in Android settings |
| 7 | Повертаєшся в Tepera, перемикач увімкнено | Feature is now active |
| 8 | Відкриваєш обрану стрічку: з'являється ідея, натискаєш **Open for 5 min** | App opened during a scrolling hour → a suggestion instead of the feed |
| 9 | Склейка, потім м'яке затемнення з **5 more** / **Leave**; торкаєшся застосунку крізь затемнення, потім **Leave** | After 5 minutes → gentle dim, touches still work, the user decides |
| 10 | Відкриваєш налаштування Android і вимикаєш службу Tepera | The service can be turned off at any time |

## Privacy Policy, Data safety, release notes

Privacy Policy вже має розділ про службу. Крок 9 брифу оновлює в ньому три речі: «тестова версія 1.1», «через 5 хвилин» замість «після обраного вами таймера» і речення про згоду на окремому екрані. Перевір, що опублікована сторінка на GitHub Pages оновилась.

Data safety не змінюється: дані служби обробляються лише на пристрої й не вважаються «зібраними», а звіти про збої вже задекларовані.

### Release notes для 1.1 (32), Open testing

```text
<uk-UA>
Нове в тестовій версії 1.1: паузи й підказки поверх застосунків тепер вмикаються через окремий екран згоди. Служба спеціальних можливостей працює лише після вашого «Погоджуюсь», а затемнення більше не з'являється над налаштуваннями, Google Play і самою Tepera.
</uk-UA>
<en-US>
New in test version 1.1: pauses and ideas over apps now turn on through a separate consent screen. The accessibility service works only after you tap "Agree", and the dimming no longer appears over settings, Google Play or Tepera itself.
</en-US>
```

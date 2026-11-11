# Play Console: Accessibility declaration + сценарій відео (flavor `beta`)

Заготовки для Permissions Declaration Form (AccessibilityService API) і демо-відео. Відправляє власник; текст нижче
відповідає фактичному коду (`app/src/beta/`, `AccessibilityGateUi.kt`) — якщо код зміниться, звірити.

## 1. Факти про сервіс (для відповідей у формі)

| Пункт | Значення |
|---|---|
| Сервіс | `GateAccessibilityService`, лише flavor `beta` |
| Події | тільки `TYPE_WINDOW_STATE_CHANGED` |
| `canRetrieveWindowContent` | `false` — вміст екрана недоступний |
| `isAccessibilityTool` | не виставлено (Tepera не інструмент для людей з інвалідністю) |
| Що читається | лише `packageName` вікна, що вийшло на передній план |
| Що робиться | (1) «Застосунки з затримкою»: якщо пакет у списку, обраному користувачем, — запускається екран паузи Tepera (кнопки «Не зараз»/«Відкрити»); (2) «Години скролінгу» (v3): коли відкрито обрану стрічку у власну годину ризику — підказка Tepera поверх екрана (з 3-го повторного відкриття — коротка головоломка до 30 с, якщо ввімкнено «Puzzles on re-entry»; завжди можна вийти), а після обраного таймера на 5 хв — тимчасове затемнення (`TYPE_ACCESSIBILITY_OVERLAY`) з кнопками «5 more»/«Leave» |
| Зберігається | локально: пакет + результат (PROCEEDED/CANCELLED) у `gate_events` |
| Передається | нічого, нікому |
| Вмикається | лише користувачем. Екран розкриття — другий крок онбордингу (одразу після вступного екрана «Start», до будь-яких дозволів); тим, хто оновився з попередньої версії, він показується один раз з Home; далі — з Налаштувань → Digital detox → «Apps with a delay» → «Turn on». «I agree» → системні налаштування доступності, де службу названо «Tepera: pauses and ideas» (Installed apps) |

## 2. Відповіді для форми (EN — форма англомовна)

**Core functionality that requires the AccessibilityService API**
> Tepera is a digital-wellbeing app with two features that depend on the moment an app opens. "Apps with a delay" shows a
> short, calm pause screen before apps that the user has chosen themselves (for example a social network). "Scrolling hours"
> shows a short suggestion instead of the feed when the user opens a feed app they chose, during an hour they marked as
> risky, and gently dims the screen after a timer the user chose. To do this at the moment an app opens, the app needs to know
> the package name of the app that just came to the foreground. The AccessibilityService is used only for this.

**How the API is used**
> The service listens only to TYPE_WINDOW_STATE_CHANGED and reads only the package name of the window. It has
> canRetrieveWindowContent=false, so it cannot read text, views, passwords or messages, and it does not perform gestures or
> change any settings. If the package is on the user's own list, Tepera opens its own pause screen with "Not now" and
> "Open <app>" buttons (the open button becomes active after a short countdown). The user can always continue to the app.
>
> In the "Scrolling hours" mode, when the user opens a feed app they selected during an hour they marked as risky, Tepera
> shows its own suggestion screen over the app (an alternative action, a timer, or, from the third repeated opening, a short
> puzzle of up to 30 seconds if the user has that option on; the user can always leave instead). After a timer chosen by the user ends, Tepera draws a temporary dimming layer with two
> buttons ("5 more" / "Leave") using TYPE_ACCESSIBILITY_OVERLAY. These elements belong to Tepera only; the service still reads
> nothing but the package name, does not read other apps' content, and does not click, scroll or type in other apps. The user can
> close every element, continue to the app, and turn the feature off in Tepera or the service off in Android Settings.

**Why other APIs are not enough**
> UsageStatsManager reports usage only after the fact (with a delay) and cannot trigger a pause at the moment of opening.
> Pinned launcher shortcuts work only when the user taps the shortcut, not when the app is opened by other routes
> (notification, another app, the original icon).

**Data handling**
> Package names are processed on the device only. The user's decision on the pause screen (continue / not now) is stored
> locally. Nothing is transmitted or shared with third parties.

**User control / disclosure**
> The service is off by default. The prominent in-app disclosure is the second step of the app's onboarding (right after the
> welcome screen with the "Start" button), before any permission is requested. Users who updated from an earlier version see it once
> from the home screen. It explains why the service is
> needed, what Tepera sees (only the app name), what it does not see or do, and where the data goes (nowhere), and also tells
> the user which entry to look for in Android settings ("Tepera: pauses and ideas"). It has "I agree" and "Not now" buttons;
> only "I agree" opens Android Accessibility settings. Declining records nothing and turns nothing on; the dialog can be opened
> again from Settings → Digital detox → Apps with a delay → "Turn on". The service can be disabled any time in Android Settings → Accessibility.

**Is the service designed to help people with disabilities?** — No (`isAccessibilityTool` is not set).

## 3. Текст розкриття в застосунку (як у демо)

Фактичний текст (`a11y_disclosure_*`, build 37): заголовок + розділи Why / What Tepera sees / What Tepera doesn't see or do / Where the data goes + один підсумковий рядок; увесь текст влазить на один екран без прокрутки (перевірено на Samsung S23).

- EN: «Pauses and ideas use Android accessibility. **Why:** This Android service (Accessibility Service API) tells Tepera when you open an app. That's how it shows a short pause before the apps you chose, or an idea instead of the feed in your scrolling hours, and dims the screen after 5 minutes. **What Tepera sees:** Only the name of the app that's currently open. **What Tepera doesn't see or do:** It doesn't read messages, text, passwords or anything on your screen, and doesn't tap for you. You can always close or uninstall any app. **Where the data goes:** Nowhere. App names are processed only on your phone and never sent. You can turn it off anytime in Android settings. The rest of Tepera works without it.» — кнопки «I agree» / «Not now».
- UA: те саме в `values/strings.xml` (`a11y_disclosure_*`, кнопки «Погоджуюсь» / «Не зараз»).

Показується як другий крок онбордингу (`AccessibilityOnboardingStep`, після Welcome у режимі v3), один раз; далі — лише з Налаштувань → Digital detox → Apps with a delay → Turn on і з перемикача режиму v3. Тим, хто оновився, — один раз з Home. Тексти тут і в застосунку мають збігатися; якщо міняєш один — міняй обидва.

## 4. Сценарій відео — як знято фактично (10.11.2026, build 37; ≈ 3 хв; English; субтитри вшиті в кадр)

Файли: `release/tepera-accessibility-demo-37.mp4` (субтитри вшиті на чорній смузі) і `release/tepera-accessibility-demo-37.srt` (ті самі, окремо).
Знято на Samsung S23, release-збірка 37, нейтральні застосунки (Calculator, Clock, Display settings); робочий стіл і особисті
дані з кадру прибрано обрізанням, іконки сповіщень у статус-барі замазано.

Вимоги Play: показати відкриття застосунку і шлях до розкриття → повний текст розкриття → згоду і надання дозволу →
**відмову і як знову відкрити розкриття** → основну функцію з субтитрами. Службовий опис (`a11y_service_description`) НЕ
замінює розкриття в застосунку.

| # | Кадр | Субтитр (скорочено) | ~час |
|---|---|---|---|
| 1 | Перший запуск: Welcome | «First launch: the Welcome screen explains what Tepera is» | 7 с |
| 2 | Розкриття (другий крок онбордингу), текст повністю на екрані | «Prominent disclosure, shown before any permission is requested» | 10 с |
| 3 | **Відмова:** «Not now» → вибір Calculator і Clock, Home | «"Not now" enables nothing» | 13 с |
| 4 | **Повторне розкриття:** Налаштування → Digital detox → Apps with a delay → Accessibility → «Turn on» → те саме розкриття | «The same disclosure is shown first…» | 31 с |
| 5 | **Згода:** «I agree» → системна сторінка «Tepera: pauses and ideas» → перемикач → діалог → «Allow» → On | «The user turns the switch on themselves» | 14 с |
| 6 | Пауза перед Calculator (вибрано користувачем) → «Not now» | «Opening Calculator, an app the user chose…» | 14 с |
| 7 | Display settings (не вибрано) — пауза не з'являється | «Display settings open with no pause» | 10 с |
| 8 | Година скролінгу: Clock → ідея замість стрічки → «Open for 5 min» → задачка → Clock | «…an idea is shown instead of the feed» | 25 с |
| 9 | Титр «… 5 minutes later» → затемнення, плашка «5 more / Leave» | «After 5 minutes the screen gently dims…» | 27 с |
| 10 | Налаштування Android → Accessibility → «Tepera: pauses and ideas» → Off | «Turning the service off, at any time» | 24 с |

**Нотатки для перезйомки:** (1) не запускати `uiautomator dump` у 5-хвилинну сесію — вона скидає службу доступності, і
затемнення не з'являється; (2) після «Open for 5 min» нова підказка не показується 10 хв (`OPEN_FIVE_QUIET_MILLIS`);
(3) швидкі повторні відкриття дають задачку («Puzzles on re-entry»); (4) пауза воріт має дві кнопки: «Not now» (ліворуч)
і «Open <застосунок>»; (5) записувати через `adb screenrecord` сегментами ≤ 3 хв, потім привести до сталої частоти кадрів
(`fps=30`) перед обрізанням — записи мають змінну частоту.

## 5. Рішення

- **Той самий applicationId** (`com.serkodesign.tepera`), подаємо на основі flavor `beta` (рішення власника, 23.10.2026).
  Усвідомлений ризик: відмова Google вражає весь застосунок (декларація прив'язана до пакета).
- Тримати `standard` придатним до релізу. Не завантажувати `beta` на треки, де лежить `standard` для всіх; `beta` — лише
  закрите тестування. `versionCode` вищого AAB виграє: після кожного підвищення `standard` перевіряти, що воно не обганяє
  `beta` на спільному треку.

## 6. План робіт

**Етап 0 — підготовка (я).**
1. Закомітити тексти розкриття/політику (зроблено, не закомічено).
2. Зібрати `installBetaDebug` на S23 та P9; перевірити, що діалог повний, «I agree» не обрізається (особливо P9), немає
   дебаг-елементів у кадрі (рядок Spike T-1 у Налаштуваннях видно лише в debuggable — не заходити туди).
3. Перевірити на S23 (Android 16) запуск паузи з фону: у CLAUDE.md це не перевірено (обмеження запуску активності з фону).

**Етап 1 — репетиція на debug (я + ти).**
1. Пройти сценарій розділу 4 по кроках на S23, виправити все, що не читається/спрацьовує нестабільно.
2. Зафіксувати результат у `VERIFICATION-Tepera.md` (окремий рядок «Accessibility beta»).
3. Тест на release: `assembleBetaRelease` (R8 на пристрої ще не перевірений) — той самий прогін, бо фінальний запис краще
   робити з release-збірки без debuggable.

**Етап 2 — запис (ти; я готую матеріали).**
1. Я даю файл субтитрів (.srt) за таблицею розділу 4.
2. Ти записуєш екран (вбудований запис або `adb shell screenrecord`, 1080p), додаєш субтитри, заливаєш на YouTube (unlisted).
3. Перевірка відео за чеклістом Play: розкриття видно повністю, відмова + повторний показ, згода, функція.

**Етап 3 — Play Console (ти; тексти я вже дав).**
1. Опублікувати оновлену політику (`privacy-policy.md`) за тим самим URL.
2. App content → Accessibility API declaration: відповіді з розділу 2, посилання на відео.
3. Data Safety / App content: перевірити, що відповіді не суперечать політиці (Tepera нікуди не надсилає дані; Auto Backup лишається керованим Android).
4. Підготувати `bundleBetaRelease` (я), завантажити на закритий трек (ти).

**Етап 4 — очікування.**
- Відмова: прочитати причину, виправити (текст, відео, функціональність) і подати знову. Не подавати відразу кілька разів поспіль.
- Схвалення: далі лише тест; рішення про перенесення служби в `standard` — окремо, після досвіду.

## 7. Доповнення 03.11.2026: v3 «Години скролінгу» теж використовує сервіс

**Розбіжність, знайдена при підготовці відкритого тесту:** розділи 1–4 описують лише «Застосунки з затримкою». Збірка `beta`
(1.0.4-beta / 24) з увімкненим v3 робить із тим самим сервісом більше: (а) коли відкрито обрану стрічку у власну годину
скролінгу, `PlanOverlayActivity` показує поверх екрана підказку (з 4-го поспіль відкриття — одразу коротку головоломку);
(б) після обраного таймера `SoftExitController` малює вікнами `TYPE_ACCESSIBILITY_OVERLAY` затемнення й кнопки «Ще 5» /
«Вийти». Якщо форма й відео покажуть тільки паузу, Google побачить у збірці поведінку, якої нема в декларації.

**Додати до відповідей форми (EN), у «How the API is used»:**
> In the "Scrolling hours" mode, when the user opens a feed app they selected during an hour they marked as risky, Tepera
> shows its own suggestion screen over the app (an alternative action or a timer). After a timer chosen by the user ends,
> Tepera draws a temporary dimming layer with two buttons ("5 more" / "Leave") using TYPE_ACCESSIBILITY_OVERLAY. These
> elements belong to Tepera only; the service still reads nothing but the package name, does not read other apps' content,
> and does not click, scroll or type in other apps. The user can close every element, continue to the app, and turn the
> feature off in Tepera or the service off in Android Settings.

**Додати до відео (після кроку 11, ≈ +40 с):**

| # | Кадр | Субтитр |
|---|---|---|
| 11a | Обрана стрічка (напр. Firefox) + година скролінгу, що охоплює «зараз» → відкрити стрічку | «Scrolling hours: a Tepera suggestion appears over the chosen feed» |
| 11b | Тап «Відкрити на 5 хв» → стрічка відкривається | «The user can always continue to the app» |
| 11c | Через 5 хв з'являється затемнення з «Ще 5» / «Вийти» | «After the timer: dimming with "5 more" / "Leave"» |
| 11d | Тап «Вийти» → робочий стіл | «The user can leave in one tap» |

`play-store/accessibility-demo.en.srt` цих кадрів ще не має — дописати після запису. Також звірити розділ 3 з реальним
`a11y_disclosure_*`: поточний текст розкриття згадує лише «коротку паузу», а не підказку поверх стрічки й затемнення —
**це потрібно виправити в коді/рядках до запису відео** (розкриття має описувати всі дії служби).

## 8. Інструкція для рецензента (поле в декларації, EN) — оновлено для build 37

> How to find the prominent disclosure: install the app and open it. Tap "Start" on the first screen. The next screen is the
> disclosure for the Accessibility service ("Pauses and ideas use Android accessibility"), with "I agree" and "Not now"
> buttons. Nothing is requested before the user taps "I agree". The service can be turned on later in Settings (gear icon) →
> Digital detox → Apps with a delay → Accessibility tab → "Turn on", which shows the same disclosure first. In Android settings
> the service is listed as "Tepera: pauses and ideas" (Installed apps).

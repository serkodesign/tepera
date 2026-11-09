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

Фактичний текст (`a11y_disclosure_*`, build 35): заголовок + розділи Why / What Tepera sees / What Tepera doesn't see or do / Where the data goes + два підсумкові абзаци.

- EN: «Pauses and ideas need Android's accessibility service. **Why:** This Android service (Accessibility Service API) lets Tepera know when you open an app. That's the only way Tepera can show a short pause before the apps you chose, or an idea instead of the feed during your scrolling hours, right when you open them, and gently dim the screen after 5 minutes. **What Tepera sees:** Only the name of the app that's currently open. **What Tepera doesn't see or do:** It doesn't read messages, text, passwords or anything on your screen. It doesn't tap anything for you. It never stops you from closing or uninstalling any app. **Where the data goes:** Nowhere. Tepera processes the names of opened apps only on your phone and doesn't send them anywhere. You can turn it off anytime in Android settings. Without it, the rest of Tepera works as usual. After you tap "I agree", Android Accessibility settings will open. Find "Tepera: pauses and ideas" in the list (it may be under "Installed apps" or "Downloaded apps") and turn it on.» — кнопки «I agree» / «Not now».
- UA: те саме в `values/strings.xml` (`a11y_disclosure_*`, кнопки «Погоджуюсь» / «Не зараз»).

Показується як другий крок онбордингу (`AccessibilityOnboardingStep`, після Welcome у режимі v3), один раз; далі — лише з Налаштувань → Digital detox → Apps with a delay → Turn on і з перемикача режиму v3. Тим, хто оновився, — один раз з Home. Тексти тут і в застосунку мають збігатися; якщо міняєш один — міняй обидва.

## 4. Сценарій відео (≈ 2 хв; мова застосунку — English; субтитри EN)

Вимоги Play (сторінка політики): показати відкриття застосунку і шлях до розкриття → повний текст розкриття (якщо
треба скролити — повільно) → згоду і надання дозволу → **відмову і як знову відкрити розкриття** → основну функцію з
субтитрами. Службовий опис (`a11y_service_description`) НЕ замінює розкриття в застосунку.

**Перед записом:** чисте встановлення (дані застосунку стерті, щоб показався онбординг), вимкнені сповіщення, прибрані
особисті дані з кадру, Tepera ще НЕ в списку Accessibility, жодного застосунку у воротах. Для відеоряду — запис екрана
зі «show touches».

| # | Кадр | Субтитр | Тривалість |
|---|---|---|---|
| 1 | Титр | «Tepera — Delayed apps. AccessibilityService demo» | 3 с |
| 2 | Запуск Tepera, проходимо онбординг до екрана дозволів («Delay before apps» — другий рядок, «Usage access» — перший) | «Permissions screen. The Accessibility row is optional» | 6 с |
| 3 | Тап рядка «Delay before apps» → діалог розкриття на весь екран. **Затримка 8–10 с**, щоб прочитати весь текст | «Prominent disclosure: what the service sees, why, and that Tepera sends nothing anywhere» | 10 с |
| 4 | **Відмова:** тап «Not now». Рядок лишився, служба не ввімкнена | «User declines. Accessibility stays off» | 4 с |
| 5 | **Повторне розкриття:** тап того ж рядка → діалог з'являється знову | «The disclosure can be opened again» | 4 с |
| 6 | **Згода:** тап «I agree» → системні «Accessibility» → «Tepera: delay before apps» → показати опис служби → перемикач → системне підтвердження | «User gives consent and grants access in system settings» | 15 с |
| 7 | Повернення в Tepera → рядок із галкою → «Skip»/далі на Home | «Access granted by the user» | 4 с |
| 8 | Налаштування → «Delayed apps» → таб «Accessibility» (картка вже не показується, бо увімкнено) → додати Firefox (або будь-який безпечний застосунок) | «The user chooses which apps get a pause» | 10 с |
| 9 | Робочий стіл → відкрити цей застосунок → пауза Tepera. Тап «Not now» → робочий стіл | «Core feature: a short pause before a chosen app» | 10 с |
| 10 | Відкрити його знову → пауза → дочекатись кінця відліку → «Open Firefox» → застосунок відкрився | «The user can always continue» | 12 с |
| 11 | Відкрити **інший** застосунок, якого нема в списку → пауза не з'являється | «Other apps are not affected» | 6 с |
| 12 | Налаштування Android → Accessibility → Tepera → вимкнути | «The user can turn it off at any time» | 8 с |

Другий шлях до розкриття (Налаштування → «Delayed apps» → таб «Accessibility» → «Turn on») у відео не обов'язковий: достатньо
онбордингу. Його можна зняти запасним дублем на випадок, якщо Google попросить показати розкриття «під час звичайного
використання». Картка «Turn on» зникає після ввімкнення служби, тому знімати її треба до кроку 6.

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

## 8. Інструкція для рецензента (поле в декларації, EN) — оновлено для build 36

> How to find the prominent disclosure: install the app and open it. Tap "Start" on the first screen. The next screen is the
> disclosure for the Accessibility service ("Pauses and ideas need Android's accessibility service"), with "I agree" and "Not now"
> buttons. Nothing is requested before the user taps "I agree". The service can be turned on later in Settings (gear icon) →
> Digital detox → Apps with a delay → Accessibility tab → "Turn on", which shows the same disclosure first. In Android settings
> the service is listed as "Tepera: pauses and ideas" (Installed apps).

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
| Що робиться | якщо пакет у списку, обраному користувачем, — запускається екран паузи Tepera (кнопки «Продовжити»/«Вийти») |
| Зберігається | локально: пакет + результат (PROCEEDED/CANCELLED) у `gate_events` |
| Передається | нічого, нікому |
| Вмикається | лише користувачем: екран «Застосунки з затримкою» або онбординг → розкриття → системні налаштування |

## 2. Відповіді для форми (EN — форма англомовна)

**Core functionality that requires the AccessibilityService API**
> Tepera is a digital-wellbeing app. Its "Delayed apps" feature shows a short, calm pause screen before apps that the user
> has chosen themselves (for example a social network). To do this on the moment an app opens, the app needs to know the
> package name of the app that just came to the foreground. The AccessibilityService is used only for this.

**How the API is used**
> The service listens only to TYPE_WINDOW_STATE_CHANGED and reads only the package name of the window. It has
> canRetrieveWindowContent=false, so it cannot read text, views, passwords or messages, and it does not perform gestures or
> change any settings. If the package is on the user's own list, Tepera opens its own pause screen with "Continue" and
> "Not now" buttons. The user can always continue to the app.

**Why other APIs are not enough**
> UsageStatsManager reports usage only after the fact (with a delay) and cannot trigger a pause at the moment of opening.
> Pinned launcher shortcuts work only when the user taps the shortcut, not when the app is opened by other routes
> (notification, another app, the original icon).

**Data handling**
> Package names are processed on the device only. The user's decision on the pause screen (continue / not now) is stored
> locally. Nothing is transmitted or shared with third parties.

**User control / disclosure**
> The service is off by default. Before sending the user to system settings, the app shows a prominent in-app disclosure
> dialog (what is seen, why, that nothing leaves the device) with "I agree" and "Not now". Declining keeps the feature off;
> the dialog can be opened again. The service can be disabled any time in Android Settings → Accessibility.

**Is the service designed to help people with disabilities?** — No (`isAccessibilityTool` is not set).

## 3. Текст розкриття в застосунку (як у демо)

- UA: «Tepera використовує службу Спеціальних можливостей лише для того, щоб побачити назву застосунку, який щойно відкрився, і показати коротку паузу перед тими, що ви самі обрали. Служба не читає вміст екрана, текст, паролі чи повідомлення. Дані про відкриті застосунки лишаються на вашому пристрої і нікому не передаються. …» — кнопки «Погоджуюсь» / «Не зараз».
- EN: «Tepera uses an Accessibility service only to see the name of the app that has just opened … I agree / Not now».

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
| 3 | Тап рядка «Delay before apps» → діалог розкриття на весь екран. **Затримка 8–10 с**, щоб прочитати весь текст | «Prominent disclosure: what the service sees, why, and that nothing leaves the device» | 10 с |
| 4 | **Відмова:** тап «Not now». Рядок лишився, служба не ввімкнена | «User declines. Accessibility stays off» | 4 с |
| 5 | **Повторне розкриття:** тап того ж рядка → діалог з'являється знову | «The disclosure can be opened again» | 4 с |
| 6 | **Згода:** тап «I agree» → системні «Accessibility» → «Tepera: delay before apps» → показати опис служби → перемикач → системне підтвердження | «User gives consent and grants access in system settings» | 15 с |
| 7 | Повернення в Tepera → рядок із галкою → «Skip»/далі на Home | «Access granted by the user» | 4 с |
| 8 | Налаштування → «Delayed apps» → таб «Accessibility» (картка вже не показується, бо увімкнено) → додати Firefox (або будь-який безпечний застосунок) | «The user chooses which apps get a pause» | 10 с |
| 9 | Робочий стіл → відкрити цей застосунок → пауза Tepera. Тап «Exit» → робочий стіл | «Core feature: a short pause before a chosen app» | 10 с |
| 10 | Відкрити його знову → пауза → дочекатись → «Continue» → застосунок відкрився | «The user can always continue» | 12 с |
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
3. Data Safety / App content: перевірити, що відповіді не суперечать політиці (дані лишаються на пристрої).
4. Підготувати `bundleBetaRelease` (я), завантажити на закритий трек (ти).

**Етап 4 — очікування.**
- Відмова: прочитати причину, виправити (текст, відео, функціональність) і подати знову. Не подавати відразу кілька разів поспіль.
- Схвалення: далі лише тест; рішення про перенесення служби в `standard` — окремо, після досвіду.

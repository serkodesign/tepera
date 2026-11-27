# Tepera: виправлення Accessibility для повторного подання (build 31)

## Контекст

Google Play відхилив build 30 (closed testing) з двох причин:
1. **Missing prominent disclosure**: у застосунку немає окремого екрана розкриття використання AccessibilityService API, який з'являється до запиту дозволу.
2. **Missing description in Play Listing**: опис у Store listing. Це НЕ в коді, виправляється в Play Console.

Ціль брифу: код build 31, у якому:
- є екран розкриття і явна згода;
- сервіс отримує мінімум даних;
- затемнення ніколи не блокує користувача;
- без дозволу решта застосунку працює як звичайно.

## Правила роботи

- **Одна задача = один крок нижче.** Після кожного кроку: збірка проходить → `git commit` → рядок у `PROGRESS-accessibility-fix.md` (що зроблено, що далі). Світло може зникнути будь-коли, тож незакомічена робота не повинна перевищувати одного кроку.
- Не чіпай код, не пов'язаний з кроком. Не рефактор «заодно».
- **Не додавай дозвіл `INTERNET`** і жодних мережевих бібліотек.
- **Не став `android:isAccessibilityTool="true"`.** Tepera не є інструментом для людей з інвалідністю, і політика Google це забороняє.
- Тексти беруться тільки з цього брифу. Не переписуй їх «краще», бо вони мають збігатися з описом у Play Console.
- Якщо реальний код суперечить припущенню з брифу, зупинись і опиши розбіжність. Не вгадуй.

---

## Крок 0. Аудит (лише читання, без змін у коді)

Знайди і запиши в `AUDIT-accessibility.md`:
1. Клас, що наслідує `AccessibilityService`, і його запис у `AndroidManifest.xml` (permission `BIND_ACCESSIBILITY_SERVICE`, `meta-data`).
2. XML-конфіг сервісу: `accessibilityEventTypes`, `canRetrieveWindowContent`, `accessibilityFlags`, `canPerformGestures` та інші `can*`, `description`.
3. **Усі** місця, де викликається `Settings.ACTION_ACCESSIBILITY_SETTINGS` або інший шлях до налаштувань Accessibility (`grep -rn "ACCESSIBILITY_SETTINGS"`).
4. Як реалізовано затемнення: `TYPE_ACCESSIBILITY_OVERLAY` через `WindowManager` сервісу чи `SYSTEM_ALERT_WINDOW`. Чи є на ньому кнопка закриття або продовження.
5. Над якими пакетами затемнення може з'явитися: чи виключені власний пакет, Settings, SystemUI, лаунчер.
6. Що сервіс читає з `AccessibilityEvent`: тільки `packageName` чи ще `text`, `source`, `contentDescription`.
7. Чи немає в маніфесті `INTERNET`. Поточні `versionCode` і `versionName`.

**Зупинись після аудиту і покажи звіт.** Далі не йди без підтвердження.

## Крок 1. Мінімальний конфіг сервісу

У XML-конфігу сервісу:
```xml
android:accessibilityEventTypes="typeWindowStateChanged"
android:accessibilityFeedbackType="feedbackGeneric"
android:canRetrieveWindowContent="false"
android:notificationTimeout="100"
android:description="@string/a11y_service_description"
```
- Прибери `canPerformGestures`, `canRequestFilterKeyEvents` та інші `can*`, якщо вони не потрібні.
- Прибери `flagRetrieveInteractiveWindows` і `flagReportViewIds`, якщо вони є.
- У `onAccessibilityEvent` читай **тільки** `event.packageName`. Не звертайся до `event.text`, `event.source`, `rootInActiveWindow`.

Рядки:
- `values-uk/strings.xml` → `a11y_service_description`:
  «Вихід вчасно»: визначає лише, який застосунок відкрито, щоб у вибрані години нагадати твій план і м'яко затемнити екран, коли час вийшов. Не читає вміст екрана. Дані не залишають телефон.
- `values/strings.xml` (EN):
  "Leave on time": only detects which app is open, so it can remind you of your plan during the hours you chose and gently dim the screen when your time is up. Doesn't read screen content. Data never leaves your phone.

Commit: `a11y: minimal service config`

## Крок 2. Зберігання згоди і стан сервісу

- DataStore: ключі `a11y_disclosure_accepted: Boolean` і `a11y_disclosure_accepted_at: Long`.
- Функція `isTeperaA11yServiceEnabled(context)` перевіряє `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` на наш `ComponentName`.
- Похідний стан `LeaveOnTimeState`: `Off` | `NeedsDisclosure` | `NeedsServiceEnabled` | `Active`.

Commit: `a11y: consent storage + service state`

## Крок 3. Екран розкриття (Compose)

Повноекранний екран у стилі Tepera: фон, типографіка Golos Text, ті самі кольори, що й решта застосунку. **Не** схожий на системний `AlertDialog`. Кнопки закріплені внизу. Текст прокручується, якщо не вміщається.

| Елемент | UA (`values-uk`) | EN (`values`) |
|---|---|---|
| title | Щоб «Вихід вчасно» працював, Tepera потрібна служба спеціальних можливостей | "Leave on time" needs Android's accessibility service |
| why_label / why | Навіщо / Ця служба Android (Accessibility Service API) дає Tepera знати, коли ти відкриваєш застосунок. Тільки так Tepera може в момент відкриття нагадати твій план, а коли час вийшов, — м'яко затемнити екран. | Why / This Android service (Accessibility Service API) lets Tepera know when you open an app. That's the only way Tepera can remind you of your plan right when you open it, and gently dim the screen when your time is up. |
| sees_label / sees | Що Tepera бачить / Лише назву застосунку, який зараз відкритий. | What Tepera sees / Only the name of the app that's currently open. |
| not_label / not | Чого Tepera не бачить і не робить / Не читає повідомлення, текст, паролі чи те, що на екрані. Не натискає нічого замість тебе. Не заважає закрити чи видалити будь-який застосунок. | What Tepera doesn't see or do / It doesn't read messages, text, passwords or anything on your screen. It doesn't tap anything for you. It never stops you from closing or uninstalling any app. |
| data_label / data | Куди йдуть дані / Нікуди. Усе залишається на твоєму телефоні — Tepera не має доступу до інтернету. | Where the data goes / Nowhere. Everything stays on your phone — Tepera has no internet access. |
| footer | Вимкнути можна будь-коли в налаштуваннях Android. Без цієї служби решта Tepera працює як звичайно. | You can turn it off anytime in Android settings. Without it, the rest of Tepera works as usual. |
| btn_agree | Погоджуюсь | Agree |
| btn_not_now | Не зараз | Not now |

Поведінка:
- **Погоджуюсь** → записати згоду (крок 2) → відкрити `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- **Не зараз**, системне «Назад», свайп → закрити екран, згоду **не** записувати, функція лишається вимкненою.
- Екран не з'являється сам по собі. Без автоповторів після «Не зараз».

Commit: `a11y: prominent disclosure screen`

## Крок 4. Єдина точка входу

- Створи одну функцію `requestLeaveOnTime()`. **Усі** місця з кроку 0.3 (онбординг, налаштування, банери) викликають тільки її:
  - сервіс увімкнено й згода є → увімкнути функцію;
  - інакше → показати екран розкриття (крок 3).
- Жодного іншого прямого виклику `ACTION_ACCESSIBILITY_SETTINGS` у коді. Перевір: `grep -rn "ACCESSIBILITY_SETTINGS"` має знайти лише екран розкриття.

Commit: `a11y: single entry point via disclosure`

## Крок 5. Сервіс без згоди нічого не робить

- Якщо `a11y_disclosure_accepted == false` (наприклад, людина ввімкнула службу вручну в налаштуваннях, оминувши екран), `onAccessibilityEvent` одразу повертається: нічого не читає, нічого не показує.
- Наступного разу, коли людина відкриє Tepera, показати екран розкриття, щоб вона могла погодитися.

Commit: `a11y: service gated by consent`

## Крок 6. Затемнення ніколи не блокує

- На затемненні завжди видно кнопку **«Продовжити все одно» / "Continue anyway"**, яка одразу прибирає затемнення до кінця цієї сесії в застосунку.
- Затемнення **ніколи** не показується над такими пакетами:
  - власний пакет `com.serkodesign.tepera`;
  - `com.android.settings` і налаштування виробника (Samsung: `com.android.settings`, `com.samsung.android.*settings*` — перевір на S23);
  - `com.android.systemui`;
  - поточний лаунчер (визначити через `ACTION_MAIN` + `CATEGORY_HOME`);
  - `com.google.android.packageinstaller`, `com.android.packageinstaller`, `com.google.android.permissioncontroller`;
  - `com.android.vending` (Google Play).
- Затемнення прибирається одразу, щойно на передньому плані з'являється інший застосунок.
- Якщо затемнення зараз через `SYSTEM_ALERT_WINDOW`, перейди на `TYPE_ACCESSIBILITY_OVERLAY` з `WindowManager` сервісу і прибери `SYSTEM_ALERT_WINDOW` з маніфесту. Якщо це складно, зупинись і опиши.

Commit: `a11y: non-blocking dim + excluded packages`

## Крок 7. Робота без дозволу

- Якщо служба вимкнена або згоди немає, «Вихід вчасно» показує стан «Вимкнено» з кнопкою «Увімкнути», яка викликає `requestLeaveOnTime()`.
- Решта застосунку (статистика, ризикові години, віджети) працює без змін, без помилок і без нав'язливих банерів.
- Стан перевіряється в `onResume`, бо людина могла вимкнути службу в налаштуваннях.

Commit: `a11y: graceful degradation`

## Крок 8. Версія і збірка

- `versionCode = 31`, `versionName` → наступна версія (уточни в автора).
- Зібрати release AAB.

Commit: `release: build 31`

---

## Ручна перевірка (S23 + Moto G84)

- [ ] Свіжа установка → увімкнути «Вихід вчасно» → з'являється екран розкриття.
- [ ] «Не зараз» → функція вимкнена, решта працює.
- [ ] Ще раз увімкнути → екран розкриття знову.
- [ ] «Погоджуюсь» → налаштування Accessibility → увімкнути Tepera → повернутися → функція активна.
- [ ] Відкрити відстежуваний застосунок у ризикову годину → нагадування → коли час вийшов, затемнення → «Продовжити все одно» працює.
- [ ] Затемнення не з'являється над Налаштуваннями, лаунчером, Google Play і самою Tepera.
- [ ] Вимкнути службу в налаштуваннях → Tepera показує «Вимкнено», нічого не падає.
- [ ] Увімкнути службу вручну, без екрана розкриття → сервіс нічого не робить, Tepera просить згоду.
- [ ] `aapt dump permissions` / Merged Manifest: немає `INTERNET`.

# Tepera: виправлення Accessibility для повторного подання (1.1, versionCode 32)

Версія брифу: v2, 05.11.2026. Замінює v1. Аудит (крок 0) уже зроблено: `AUDIT-accessibility.md`.

## Контекст

Google Play відхилив build 30 (`1.0.5-beta`) з двох причин:
1. **Missing prominent disclosure**: немає окремого екрана розкриття використання AccessibilityService API, який з'являється до запиту дозволу.
2. **Missing description in Play Listing**: в описі Store listing не згадано Accessibility. Це не в коді, виправляється в Play Console.

Нова збірка: **`versionName "1.1"`, `versionCode 32`**, flavor `beta`, трек **Open testing**. Production лишається на 1.0.5 (flavor `standard`, versionCode 24) — його не чіпаємо. versionCode 31 уже використано (`1.0.6-beta`).

Ціль брифу — код 1.1, у якому:
- є повноекранний екран розкриття і явна згода;
- служба нічого не робить без згоди;
- затемнення ніколи не лишається поверх Tepera, налаштувань, Google Play, інсталятора й діалогів дозволів;
- без служби решта застосунку працює як звичайно.

### Рішення власника (05.11.2026), на яких стоїть бриф

1. Firebase (Crashlytics, Remote Config) і Play-бібліотеки оновлень і відгуків **лишаються**. `INTERNET` у змердженому маніфесті є — це очікувано (D-49). У текстах не пишемо «немає доступу до інтернету»; пишемо «Tepera нікуди не передає дані про відкриті застосунки».
2. Затемнення лишається як є: шар без дотиків (`FLAG_NOT_TOUCHABLE`), плашка з кнопками «Ще 5» і «Вийти», підтвердження другого «Ще 5», задачки з 3-го повторного входу. Виправляємо лише пакети, над якими воно може лишитися.
3. Код функцій не міняємо під тексти: тексти описують те, що є (ідеї замість стрічки, фіксовані 5 хв), а не план «якщо… то…».
4. Служба обслуговує дві функції — «Підказка поверх стрічки» і «Затримка при будь-якому відкритті» (ворота). Тексти згадують обидві.
5. Опис у Play — не код, робиться в Play Console.

## Правила роботи

- **Одна задача = один крок нижче.** Після кожного кроку: збірка `beta` компілюється (`.\gradlew.bat :app:compileBetaDebugKotlin`), юніт-тести `beta` проходять → `git commit` → рядок у `PROGRESS-accessibility-fix.md` (що зроблено, що далі). Світло може зникнути будь-коли, тож незакомічена робота не повинна перевищувати одного кроку.
- Працюй у гілці `fix/a11y-disclosure`, створеній від поточної `ui/m3-v31`. Не мерджи в інші гілки без запиту.
- Не чіпай код, не пов'язаний з кроком. Не рефактор «заодно». Flavor `standard` поводиться так само, як зараз.
- Не додавай нових дозволів і нових мережевих бібліотек. Не прибирай наявні.
- **Не став `android:isAccessibilityTool="true"`.**
- Тексти беруться тільки з цього брифу. Не переписуй їх «краще», бо вони мають збігатися з описом у Play Console.
- Мовні папки в проєкті: `values` = **українська** (за замовчуванням), `values-en` = англійська. Рядки служби — у `app/src/beta/res/values*`, рядки екрана розкриття — у `app/src/main/res/values*`.
- Якщо реальний код суперечить припущенню з брифу, зупинись і опиши розбіжність. Не вгадуй.

---

## Крок 0. Аудит — зроблено

Див. `AUDIT-accessibility.md`. Головне для наступних кроків:
- конфіг служби вже мінімальний (`typeWindowStateChanged`, `canRetrieveWindowContent="false"`, без `can*` і прапорців);
- розкриття зараз — діалог `AccessibilityDisclosureDialog` (`ui/gates/AccessibilityGateUi.kt`), згода не зберігається;
- три точки входу: `OnboardingScreen.kt:176`, `GatesScreen.kt:182`, `V3ModeToggle.kt:74` і `:94`; усі ведуть у `GateAccessibility.openSettings`;
- у `V3ModeToggle` перемикач вмикається до розкриття і лишається увімкненим після «Не зараз»;
- для власного пакета й пакетів з `ignoredPackages` подія ігнорується, і вже показане затемнення не прибирається.

## Крок 1. Назва й опис служби

Конфіг `app/src/beta/res/xml/gate_accessibility_service.xml` **не змінювати** — лише перевірити, що він такий:
```xml
android:accessibilityEventTypes="typeWindowStateChanged"
android:accessibilityFeedbackType="feedbackGeneric"
android:canRetrieveWindowContent="false"
android:notificationTimeout="100"
android:description="@string/a11y_service_description"
```

У `GateAccessibilityService.onAccessibilityEvent` прибери `event.className` з рядка логу (рядок 94): служба читає лише `eventType` і `packageName`.

Рядки (`app/src/beta/res/values/strings.xml` — UA, `values-en/strings.xml` — EN):

| Ключ | UA | EN |
|---|---|---|
| `a11y_service_label` | Tepera: паузи й підказки | Tepera: pauses and ideas |
| `a11y_service_description` | Визначає лише, який застосунок відкрито, щоб показати коротку паузу перед застосунками, які ви обрали, або ідею замість стрічки у ваші години скролінгу, і через 5 хвилин м'яко затемнити екран. Не читає вміст екрана. Tepera нікуди не передає дані про відкриті застосунки. | Only detects which app is open, so it can show a short pause before the apps you chose, or an idea instead of the feed during your scrolling hours, and gently dim the screen after 5 minutes. Doesn't read screen content. Tepera doesn't send data about opened apps anywhere. |

Commit: `a11y: service label and description`

## Крок 2. Збереження згоди і стан функції

- `util/A11yConsentStore.kt` на `SharedPreferences` (як `PlanOverlayStore`: службі потрібне синхронне читання). Ключі `a11y_disclosure_accepted: Boolean` і `a11y_disclosure_accepted_at: Long`. Функції `isAccepted(context)`, `accept(context)`.
- Перевірка служби — наявна `GateAccessibility.isEnabled(context)`, нову не створювати.
- Похідний стан `A11yFeatureState`: `Unavailable` (flavor без служби) | `NeedsDisclosure` | `NeedsServiceEnabled` | `Active`.
- Юніт-тест на обчислення стану.

Commit: `a11y: consent storage + feature state`

## Крок 3. Повноекранний екран розкриття (Compose)

`ui/gates/AccessibilityDisclosureScreen.kt` (main source set). Повноекранний, у стилі гілки v3.1: `M3Background`, Golos Text, `M3Button` (Filled — «Погоджуюсь», Text або Outlined — «Не зараз»). **Не** схожий на системний `AlertDialog` і не картка-діалог. Кнопки закріплені внизу, текст над ними прокручується, якщо не вміщається. На S23 зі шрифтом 100% усе має бути видно без прокручування.

Тексти (`values` — UA, `values-en` — EN):

| Ключ | UA | EN |
|---|---|---|
| `a11y_disclosure_title` | Паузи й підказки працюють через службу спеціальних можливостей | Pauses and ideas need Android's accessibility service |
| `a11y_disclosure_why_label` | Навіщо | Why |
| `a11y_disclosure_why` | Ця служба Android (Accessibility Service API) дає Tepera знати, коли ви відкриваєте застосунок. Тільки так Tepera може в момент відкриття показати коротку паузу перед застосунками, які ви обрали, або ідею замість стрічки у ваші години скролінгу, а через 5 хвилин — м'яко затемнити екран. | This Android service (Accessibility Service API) lets Tepera know when you open an app. That's the only way Tepera can show a short pause before the apps you chose, or an idea instead of the feed during your scrolling hours, right when you open them, and gently dim the screen after 5 minutes. |
| `a11y_disclosure_sees_label` | Що Tepera бачить | What Tepera sees |
| `a11y_disclosure_sees` | Лише назву застосунку, який зараз відкритий. | Only the name of the app that's currently open. |
| `a11y_disclosure_not_label` | Чого Tepera не бачить і не робить | What Tepera doesn't see or do |
| `a11y_disclosure_not` | Не читає повідомлення, текст, паролі чи те, що на екрані. Не натискає нічого замість вас. Не заважає закрити чи видалити будь-який застосунок. | It doesn't read messages, text, passwords or anything on your screen. It doesn't tap anything for you. It never stops you from closing or uninstalling any app. |
| `a11y_disclosure_data_label` | Куди йдуть дані | Where the data goes |
| `a11y_disclosure_data` | Нікуди. Tepera обробляє назви відкритих застосунків лише на вашому телефоні й нікуди їх не передає. | Nowhere. Tepera processes the names of opened apps only on your phone and doesn't send them anywhere. |
| `a11y_disclosure_footer` | Вимкнути службу можна будь-коли в налаштуваннях Android. Без неї решта Tepera працює як звичайно. | You can turn it off anytime in Android settings. Without it, the rest of Tepera works as usual. |
| `a11y_disclosure_continue` (наявний ключ) | Погоджуюсь | Agree |
| `a11y_disclosure_cancel` (наявний ключ) | Не зараз | Not now |

Старий ключ `a11y_disclosure_body` видалити разом з `AccessibilityDisclosureDialog` у кроці 4, коли зникнуть усі виклики.

Поведінка:
- **Погоджуюсь** → `A11yConsentStore.accept()` → `GateAccessibility.openSettings()` → закрити екран.
- **Не зараз**, системне «Назад», жест назад → закрити екран; згоду **не** записувати; функція, яку людина вмикала, лишається вимкненою.
- Екран не з'являється сам по собі (єдиний виняток — крок 5). Без автоповторів після «Не зараз».

Commit: `a11y: full-screen disclosure`

## Крок 4. Єдина точка входу

- Одна функція `requestAccessibilityFeature(...)` (або подія ViewModel з тим самим змістом). Усі три місця з аудиту викликають тільки її:
  - стан `Active` → одразу увімкнути функцію;
  - стан `NeedsServiceEnabled` (згода є, службу ще не увімкнено) → показати екран розкриття ще раз (рецензент має бачити його з будь-якої точки);
  - стан `NeedsDisclosure` → показати екран розкриття.
- `V3ModeToggle`: перемикач «Підказка поверх стрічки» викликає `PlanOverlayStore.setEnabled(true)` **лише після** «Погоджуюсь» (або одразу в стані `Active`). Після «Не зараз» перемикач лишається вимкненим. Вимикання перемикача працює як раніше.
- `GatesScreen` (картка «Затримка при будь-якому відкритті») і рядок дозволу в онбордингу — через ту саму функцію.
- `AccessibilityDisclosureDialog` і рядок `a11y_disclosure_body` видалити.
- Перевірка: `grep -rn "openSettings\|ACCESSIBILITY_SETTINGS" app/src` знаходить лише `GateAccessibility.kt` і виклик з екрана розкриття.

Commit: `a11y: single entry point via disclosure`

## Крок 5. Служба без згоди нічого не робить

- Якщо `A11yConsentStore.isAccepted == false` (наприклад, служба ввімкнена вручну в налаштуваннях, або це тестувальник, що ввімкнув її до 1.1), `onAccessibilityEvent` одразу повертається: нічого не читає далі, нічого не показує, не запускає активностей і затемнення.
- Значення кешувати в службі й оновлювати через `OnSharedPreferenceChangeListener`, а не читати диск на кожну подію.
- Коли людина відкриває Tepera, служба ввімкнена, а згоди немає — один раз за запуск показати екран розкриття. Це єдиний випадок, коли екран з'являється без натискання.

Commit: `a11y: service gated by consent`

## Крок 6. Затемнення не лишається над системними екранами і Tepera

Не змінювати: шар затемнення (`FLAG_NOT_TOUCHABLE`), кнопки «Ще 5» / «Вийти», підтвердження, задачки, таймінги.

Пакети-виключення (над ними служба ніколи не показує паузу, лист, задачки чи затемнення):
- `com.android.settings` і налаштування Samsung — на S23 знайди фактичні пакети: `adb shell pm list packages | findstr /i settings`, додай ті, що відкриваються як екрани налаштувань;
- `com.android.vending` (Google Play);
- поточний лаунчер (наявне `resolveLauncherPackages`);
- `com.android.packageinstaller`, `com.google.android.packageinstaller`, `com.android.permissioncontroller`, `com.google.android.permissioncontroller`;
- власний пакет `com.serkodesign.tepera`.

Поведінка:
- Відкрито налаштування, Google Play, інсталятор або діалог дозволів → **завершити сесію м'якого виходу і прибрати затемнення та плашку одразу** (як для лаунчера). Нових листів і пауз над ними не показувати, навіть якщо людина обрала такий пакет як стрічку чи ворота.
- Власний пакет: вікна `PlanOverlayActivity` і екрана паузи воріт — частина сценарію, сесію не завершувати. Коли відкривається головний екран Tepera (`MainActivity.onResume`) — завершити сесію і прибрати затемнення через виклик у тому ж процесі (як `SoftExitPuzzleBridge`), не через `className` події.
- `com.android.systemui` і клавіатури: сесію не завершувати (шторка й клавіатура — на секунди). Перевір на S23, чи затемнення малюється поверх відкритої шторки сповіщень. Якщо так — зупинись і опиши.
- `SYSTEM_ALERT_WINDOW` у проєкті немає — не додавати.
- Юніт-тест на правило «пакет → завершити / ігнорувати / звичайна обробка».

Commit: `a11y: dim never stays over system screens or Tepera`

## Крок 7. Робота без служби

- «Підказка поверх стрічки» і «Затримка при будь-якому відкритті» показують стан «вимкнено» з кнопкою «Увімкнути доступ» / «Увімкнути», яка викликає `requestAccessibilityFeature`.
- Решта застосунку (статистика, години скролінгу, віджети, ворота через ярлики) працює без змін, без помилок і без нав'язливих банерів.
- Стан перевіряється в `onResume` (наявний `LifecycleResumeEffect`), бо службу могли вимкнути в налаштуваннях.

Commit: `a11y: graceful degradation`

## Крок 8. Версія і збірка

- У flavor `beta` (`app/build.gradle.kts`): `versionName = "1.1"`, `versionCode = 32`, **прибрати** `versionNameSuffix = "-beta"`, щоб у Play була саме «1.1». `defaultConfig` / `standard` (1.0.5, 24) не чіпати.
- Перевір, чи `versionName` / суфікс «-beta» ніде не використовується в коді для логіки (наприклад, показ службових екранів). Якщо використовується — зупинись і опиши.
- Тести: `.\gradlew.bat :app:testBetaReleaseUnitTest`.
- Збірка: `.\gradlew.bat :app:bundleBetaRelease` → `release/tepera-1.1-32-play.aab` (`*.aab` у `.gitignore`, у git не додавати).
- Перелік дозволів змердженого маніфесту (`app/build/intermediates/merged_manifest/betaRelease/...`) має збігатися з build 30: нових дозволів немає. `INTERNET` там є через Firebase — це очікувано.

Commit: `release: 1.1 (32)`

## Крок 9. Privacy Policy в репозиторії

`docs/privacy-policy.md` (GitHub Pages) уже має розділ про службу. Онови в ньому лише:
- «Лише у тестовій (beta) збірці» → «Лише у тестовій версії 1.1 (Open testing)»; EN так само;
- «після обраного вами таймера» → «через 5 хвилин»; EN «after a timer you picked» → «after 5 minutes»;
- додай речення: «Служба вмикається лише після вашої згоди на окремому екрані в застосунку; без згоди вона нічого не робить.» / «The service turns on only after you agree on a separate screen in the app; without consent it does nothing.»
- дату оновлення політики.

Нічого іншого в політиці не змінювати. Зміну треба донести до гілки, з якої публікується GitHub Pages (Settings → Pages); якщо це не поточна гілка — зупинись і спитай.

Commit: `docs: privacy policy for 1.1 accessibility consent`

---

## Ручна перевірка (S23 + Moto G84, release-збірка 1.1)

- [ ] Свіжа установка → онбординг → рядок «Затримка перед застосунками» → повноекранний екран розкриття.
- [ ] «Не зараз» → екран закривається, нічого не ввімкнено, решта працює.
- [ ] Налаштування → «Підказка поверх стрічки» → екран розкриття; «Не зараз» → перемикач лишається вимкненим.
- [ ] Ще раз → екран розкриття знову.
- [ ] «Погоджуюсь» → налаштування Accessibility → увімкнути Tepera → повернутися → функція активна.
- [ ] Ворота → картка «Затримка при будь-якому відкритті» → той самий екран розкриття.
- [ ] Відкрити обрану стрічку в годину скролінгу → ідея → «Відкрити на 5 хв» → через 5 хв затемнення з «Ще 5» / «Вийти»; дотики проходять крізь затемнення.
- [ ] Під час затемнення відкрити Налаштування, Google Play, Tepera → затемнення зникає одразу.
- [ ] Вимкнути службу в налаштуваннях → Tepera показує «вимкнено», нічого не падає.
- [ ] Увімкнути службу вручну, без екрана розкриття (після очищення даних) → служба нічого не показує; при відкритті Tepera з'являється екран розкриття.
- [ ] Merged manifest: дозволи ті самі, що в build 30.
- [ ] У Play Console версія показується як «1.1 (32)».

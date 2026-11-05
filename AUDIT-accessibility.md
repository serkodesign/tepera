# AUDIT-accessibility — крок 0 брифу `CLAUDE-CODE-accessibility-fix.md`

Дата: 05.11.2026. Лише читання, код не змінювався.
Перевірено гілку `ui/m3-v31` @ `8b9701d` (містить увесь `feature/v3-mvp` @ `ae5cdd7`; локальний репозиторій збігається з GitHub).
Увесь код служби лежить у source set `beta`; у flavor `standard` (Production 1.0.5) служби немає (`GATE_ACCESSIBILITY=false`, `V3_MVP=false`).

## 1. Клас служби і маніфест

- Клас: `app/src/beta/java/com/serkodesign/tepera/gates/GateAccessibilityService.kt` (272 рядки).
- Маніфест: `app/src/beta/AndroidManifest.xml`, `<service android:name=".gates.GateAccessibilityService" android:exported="false" android:label="@string/a11y_service_label" android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">`, intent-filter `android.accessibilityservice.AccessibilityService`, `meta-data android:resource="@xml/gate_accessibility_service"`.
- Поруч у тому ж маніфесті: `PlanOverlayActivity` (прозора activity з листом плану й задачками) і три ресивери нагадувань.
- `a11y_service_label` зараз: «Tepera: затримка перед застосунками» — назва ще про ворота, а не про «Вихід вчасно».

## 2. XML-конфіг служби

`app/src/beta/res/xml/gate_accessibility_service.xml`:

```xml
android:accessibilityEventTypes="typeWindowStateChanged"
android:accessibilityFeedbackType="feedbackGeneric"
android:canRetrieveWindowContent="false"
android:notificationTimeout="100"
android:description="@string/a11y_service_description"
```

- `accessibilityFlags` — немає. `canPerformGestures`, `canRequestFilterKeyEvents` та інші `can*` — немає. `isAccessibilityTool` — немає ніде.
- Конфіг уже відповідає кроку 1. Змінити треба лише текст `a11y_service_description` (зараз про «коротку паузу або підказку»).

## 3. Шляхи до налаштувань Accessibility

`grep -rn "ACCESSIBILITY_SETTINGS"` — один виклик: `util/GateAccessibility.kt:29` (`GateAccessibility.openSettings`). Інших шляхів (`ACTION_ACCESSIBILITY_DETAILS_SETTINGS` тощо) немає.

`openSettings` викликається з трьох місць, кожне — після діалогу `AccessibilityDisclosureDialog`:

| Місце | Що відкриває розкриття |
|---|---|
| `ui/onboarding/OnboardingScreen.kt:176, 204` | рядок дозволу «Затримка перед застосунками» (крок 6 онбордингу) |
| `ui/gates/GatesScreen.kt:182, 318` | картка «Затримка при будь-якому відкритті» (кнопки «Увімкнути» / «Налаштування») |
| `ui/risk/V3ModeToggle.kt:74, 94, 86` | перемикач «Підказка поверх стрічки» і кнопка «Увімкнути доступ» |

Стан розкриття зараз:
- це `TeperaDialog` (Compose `Dialog`, картка з заокругленням), **не** окремий екран на весь дисплей;
- згода **ніде не зберігається**;
- у `V3ModeToggle` перемикач вмикає `PlanOverlayStore.setEnabled(true)` **до** показу розкриття, тож після «Не зараз» перемикач лишається увімкненим.

## 4. Затемнення

- Малюється вікнами служби: `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` (`SoftExitController.kt`). `SYSTEM_ALERT_WINDOW`, `canDrawOverlays`, `TYPE_APPLICATION_OVERLAY` у проєкті немає.
- Шар затемнення має `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`: дотики проходять крізь нього, застосунок під ним працює.
- Знизу — плашка з кнопками **«Ще 5»** і **«Вийти»**. Кнопки «Продовжити все одно» немає.
- «Ще 5»: друге натискання просить підтвердження («Так, ще 5 хв»); з 3-го повторного входу можуть вимагатися 1–2 задачки (`QuickPuzzles.puzzlesRequired`, увімкнені за замовчуванням, `KEY_PUZZLES_ENABLED = true`). Задачки показуються в `PlanOverlayActivity`.
- Тривалість сесії фіксована: `SESSION_MINUTES = 5` (кнопка «Відкрити на 5 хв»), користувач її не обирає.

## 5. Над якими пакетами може з'явитися

Логіка в `GateAccessibilityService.onAccessibilityEvent` + `SoftExitController.onForeground`:

| Пакет | Що відбувається |
|---|---|
| власний `com.serkodesign.tepera` | подія ігнорується одразу (`pkg == packageName`) — нового листа немає, **але вже показане затемнення не прибирається**, тобто лишається поверх Tepera |
| лаунчер (`ACTION_MAIN` + `CATEGORY_HOME`) | сесія завершується, затемнення прибирається (після перевірки «фантомної» події Samsung, 700 мс) |
| `com.android.systemui`, `android`, `com.google.android.permissioncontroller`, `com.android.permissioncontroller`, `com.android.packageinstaller`, `com.google.android.packageinstaller`, `com.android.intentresolver`, `com.google.android.gms`, `com.samsung.android.sidegesturepad`, клавіатури | подія ігнорується — **затемнення, якщо є, лишається** (зокрема над інсталятором і діалогом дозволів) |
| `com.android.settings`, Samsung-налаштування, `com.android.vending` | у списку виключень **немає**; вони — «справжні застосунки», тож відкриття завершує сесію й прибирає затемнення. Але якщо людина обере їх як стрічку або ворота, лист/пауза над ними з'явиться |
| будь-який інший застосунок з іконкою | сесія завершується, затемнення прибирається (крім перших 4 с після старту) |

## 6. Що служба читає з `AccessibilityEvent`

- `event.eventType` і `event.packageName` (рядок 92).
- `event.className` — лише в лог (рядок 94); `log()` пише тільки в debug-збірці (`if (BuildConfig.DEBUG)`).
- `event.text`, `event.source`, `contentDescription`, `rootInActiveWindow`, `getWindows` — не використовуються.
- Окремо служба читає `UsageStatsManager.queryEvents` (не Accessibility) для перевірки фантомних подій лаунчера.
- Служба також показує паузу воріт (`launchTepera` → activity) — це друга функція на тій самій службі, у текстах брифу її немає.

## 7. INTERNET і версія

- У вихідних маніфестах (`main`, `beta`) `INTERNET` немає.
- У **змердженому** маніфесті `INTERNET` і `ACCESS_NETWORK_STATE` **є**: їх додають `firebase-crashlytics`, `firebase-config`, `play:app-update`, `play:review` (`app/build.gradle.kts`, рядки 11–12, 152–165). Задокументовано в D-49 (25.10.2026) і `docs/play-console-declarations.md`.
- Версії (`app/build.gradle.kts`):
  - `defaultConfig` / `standard`: `versionCode = 24`, `versionName = "1.0.5"` (= Production).
  - `beta`: `versionCode = 31`, `versionName = "1.0.6"` + `versionNameSuffix = "-beta"` → **«1.0.6-beta»** (коміт `ae5cdd7`, 03.11.2026). Build 30 = «1.0.5-beta», коміт `578d5fd`.

---

## Розбіжності з брифом (за правилом «зупинись і опиши»)

1. **INTERNET.** Пункт «Merged Manifest: немає `INTERNET`» не виконується без видалення Firebase (Crashlytics, Remote Config) і Play-бібліотек оновлень і відгуків. Тексти «Tepera не має доступу до інтернету» / «no internet access» на екрані розкриття (`data`), в описі Play, декларації й Privacy Policy зараз неправдиві. Рішення: або прибрати ці бібліотеки з `beta`, або замінити тексти формулюванням D-49 («Tepera нікуди не передає дані про відкриті застосунки»).
2. **Data safety.** Через Crashlytics застосунок збирає звіти про збої; у `play-console-declarations.md` відповідь «Так». Позначка «No data collected» з плану була б помилкою.
3. **Папки рядків.** Бриф: UA у `values-uk`, EN у `values`. У проєкті навпаки: `values` = українська (за замовчуванням), `values-en` = англійська. Рядки служби — у `app/src/beta/res/values*`.
4. **DataStore.** Бриф просить DataStore. Схожі прапорці (`PlanOverlayStore`) зберігаються в `SharedPreferences`; DataStore у проєкті теж є (`SettingsStore`). Потрібно вибрати одне.
5. **«Продовжити все одно».** Зараз «Ще 5» з підтвердженням і задачками. Крок 6 вимагає одну кнопку, що одразу прибирає затемнення до кінця сесії, тобто прибрати або обійти задачки й підтвердження. Це продуктове рішення.
6. **План «якщо… то…» і «на скільки заходиш».** Редактор плану видалено 29.10 (`6858b55`), лист показує «Ідею на цей час». Тривалість — фіксовані 5 хв. Тексти брифу («нагадати твій план», «коли час, який ти обрав, вийшов») не відповідають коду.
7. **Ворота на тій самій службі.** Служба також показує паузу перед обраними застосунками й задачки. Тексти брифу описують лише нагадування й затемнення. Треба або описати ворота в текстах, або прибрати їх зі служби.
8. **Затемнення поверх виключених пакетів.** Для власного пакета, SystemUI, інсталятора й діалогу дозволів подія ігнорується, тож уже показане затемнення не зникає. Крок 6 вимагає протилежного.
9. **versionCode 31 уже стоїть** у flavor `beta`. Якщо 31 уже завантажувався в Play — потрібен 32. `versionName` зараз «1.0.6-beta», не «1.1». **Вирішено 05.11:** 31 використано, нова збірка — `1.1` (32), трек Open testing.
10. **Production.** Якщо build 31 переводити в Production, туди піде flavor `beta` (з суфіксом «-beta», службовими екранами тестувальника). Треба вирішити, яким варіантом іде Production.

**Рішення власника від 05.11.2026 щодо пунктів 1, 2, 5–8** — у брифі v2, розділ «Рішення власника». Пункти 3 і 4 враховано в брифі v2 (мовні папки; згода в `SharedPreferences`). Пункт 10 відкладено до переходу 1.1 у Production.

## Що з цього було в 1.0.5 (versionCode 24, тег `v1.0.5` = `d8b9dc3`, flavor `standard`)

| Функція з нового опису | 1.0.5 (Production) | Build 31 зараз |
|---|---|---|
| Ризикові години | ні (є лише погодинний патерн використання) | так |
| План «якщо… то…» | ні | ні (видалено 29.10) |
| Питання «на скільки заходиш» | ні | ні (фіксовані 5 хв) |
| Затемнення з кнопкою продовження | ні | так («Ще 5» / «Вийти») |
| Служба спеціальних можливостей | ні | так |
| Без акаунта, без реклами | так | так |
| Доступ до статистики використання | так | так |

Усі функції v3 (65 комітів, 25.10–03.11) працюють лише у flavor `beta`.

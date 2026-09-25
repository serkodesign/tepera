# Tepera — аудит CC-0 (підготовка до закритого тесту)

Дата: 25.09.2026. Гілка: `cc-0-audit`. Зміни в коді: немає. Джерело завдання: `CLAUDE-CODE-PLAN-closed-test_1.md`.
Посилання на код — `файл:рядок` на момент аудиту (шлях від `app/src/main/java/com/serkodesign/tepera/`, якщо не вказано інше).

## 0. Стан робочих документів
- Специфікація — `docs/SRS-Tepera-v4.1.md`, журнал рішень — `docs/DECISIONS-Tepera.md`, журнал перевірки — `docs/VERIFICATION-Tepera.md`. Порядок із плану (спека → код → журнал перевірки) застосовний.
- **`POSITIONING-Tepera.md` у репозиторії немає** (план вимагає його прочитати). Потрібен від власника.
- D-04 (заморозка скоупу до кінця закритого тесту) суперечить плану, який додає нові функції. Потрібне явне рішення в DECISIONS.

## 1. Орієнтир онлайн-часу (180 хв) і Grace Period Buffer
Окремого «співвідношення Online/Offline» у продукті вже немає (прибрано в v2.2). Число-орієнтир показується лише як тиха засічка на шкалі.

| Що | Де |
| --- | --- |
| Дефолт 180 хв | `data/local/SettingsStore.kt:33` (`DEFAULT_TARGET_MINUTES`), читання `:47-48` |
| Дефолт у стані UI | `ui/home/BalanceViewModel.kt:44-45` (`targetMinutes = 180`, `denominatorMinutes = 180`), підписка `:79`, передача `:121` |
| Grace Period Buffer (`max(180, хв від старту дня)`) | `data/repository/BalanceRepository.kt:174-179`, виклик `BalanceViewModel.kt:122` |
| Показ орієнтира на Home | `ui/home/BalanceCard.kt:131` (параметр), `:174-175` (позиція засічки на шкалі, без підпису) |
| Налаштування (слайдер 1–8 год) | `ui/settings/TrackingSettingsScreen.kt:74`; рядки `settings_target_label`/`settings_target_info` (`res/values/strings.xml:165,182`) |
| Резервна копія (експорт/імпорт орієнтира) | `data/repository/BackupRepository.kt:35,40,68-81` |
| Віджет | у `widget/` орієнтира немає взагалі (пошук `target` порожній) |

Висновки для CC-1:
- «Вимкнути» орієнтир зараз неможливо: значення завжди є, слайдер має мінімум 1 год.
- Знаменник Grace Period у стані UI є, але на екрані ніде не показується як відношення. Прибирати чи ні — рішення при CC-1.
- Віджет вже «без орієнтира», тож вимога «віджет показує час без телефону» стосується лише Home.
- Imports/експорт: файл із `targetMinutes` треба навчити «немає орієнтира» (`null`).

## 2. Дозволи
Власний маніфест (`app/src/main/AndroidManifest.xml`): `PACKAGE_USAGE_STATS` (`:11`), `POST_NOTIFICATIONS` (`:21`).
Змерджений release-маніфест (`app/build/intermediates/merged_manifest/release/...`):
`ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, **`INTERNET`**, `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (службовий).

- **`INTERNET` є** — приходить транзитивно від `firebase-crashlytics` (`app/build.gradle.kts:11-12,110-113`; `com.google.gms.google-services` + `com.google.firebase.crashlytics`). Це прямо суперечить жорсткому обмеженню плану («заборонено INTERNET, SDK крешрепортингу»).
- **`POST_NOTIFICATIONS` є вже зараз**, і запитується у трьох місцях, а не «лише в CC-8»:
  - екран дозволів онбордингу (`ui/onboarding/OnboardingScreen.kt:84,122,215`);
  - запобіжник на Home (`ui/home/HomeScreen.kt:352-381`);
  - воркер таймера «Усе ще цим займаєшся?» (`data/TimerCheckWorker.kt:72`, рядки `timer_check_*`, `strings.xml:84-87`).
  Це єдине наявне сповіщення застосунку; план каже «інших сповіщень немає».
- Файлових дозволів і `FileProvider` немає (потрібен для CC-10).

## 3. Ворота
- Тривалість паузи: варіанти `3 / 5 / 10 с` (`ui/gates/GatesScreen.kt:73`, `DELAY_OPTIONS`), у діалозі створення обрано `DELAY_OPTIONS[1]` = 5 с (`:277`), а параметр за замовчуванням у ViewModel — 10 с (`ui/gates/GatesViewModel.kt:20,96`). Тобто між UI і ViewModel розбіжність дефолтів (реально користувач бачить 5 с). У `CLAUDE.md` написано «5/10/20 с» — застаріло. План (CC-6) називає 5/10/15 с.
- Дебаунс: повторний тап протягом 30 с після проходу пропускає паузу (`data/repository/GateRepository.kt:140-150,172`). Пауза «вимкнути на сьогодні» — у `GatesScreen` (`gates_pause_today_label`). Розкладу і паузи «на вихідні/до дати» немає.
- Логіка екрана: `ui/gates/GatePauseViewModel.kt` — зворотний відлік `delaySeconds…1` (`:89-98`), потім `canContinue`; «Вийти» і системна «назад» → `cancel()` (`:117-123`); «Продовжити» → `proceed()` (`:125-129`).
- Тексти екрана: `gate_pause_inhale`/`exhale` (Вдих…/Видих…), `gate_pause_continue_action` «Продовжити», `gate_pause_exit_action` «Вийти» (`strings.xml:254-257`). Ротації текстів немає; кнопки називаються інакше, ніж у плані («Не зараз» / «Відкрити {app}»). Рядок «Ти намагався відкрити N разів» прибраний раніше.
- Що зберігається:
  - `app_gates` (`data/local/entity/AppGateEntity.kt`): `packageName`, `delaySeconds`, `originalIconHandled`, `createdAt`, `lastProceedAtMillis`, `shortcutId`;
  - `gate_events` (`GateEventEntity.kt`): `id`, `packageName`, `result` (`PROCEEDED`/`CANCELLED`), `atMillis` — по одному запису на кожне показане рішення; пропущена пауза (дебаунс/вимкнено) подію не пише.
- **Конфлікт з CC-9:** «назви пакетів цільових застосунків не зберігати» — нова таблиця метрик може це виконати (лише `type`+`timestamp`), але `app_gates`/`gate_events` пакет вже зберігають (для воріт це необхідно).
- Події `gate_bypassed` зараз рахувати нема з чого: немає ні запису «ворота були активні», ні фонового знімка.
- Картка `GateEventsSummaryCard` («Цей місяць: N разів ти вирішив не зараз», `strings.xml:152-153`) — по суті лічильник скасувань; план забороняє «лічильники спроб». Рішення потрібне.

## 4. Дані подій і глибина історії
- Room (версія 10, `data/local/AppDatabase.kt:70-82`) зберігає лише власні дані: `categories`, `activity_entries`, `excluded_apps`, `detected_gaps`, `sleep_windows`, `user_estimates`, `app_gates`, `card_show_history`, `gate_events`. **Сирі usage events і денні агрегати не зберігаються** — Online, розблокування, паузи щоразу рахуються з `UsageStatsManager`.
- Глибина історії: S23 (Android 16) — 9 днів (`ROADMAP.md:1014`, найдавніша подія 03.09.2026); практична межа документована ~7 днів (`ROADMAP.md:1525`). На G84, P9, XZ1 глибину окремо не вимірювали. Отже, щоденний знімок агрегатів (CC-4) справді потрібен.
- Готове для повторного використання: `data/local/DeviceIdProvider.kt` — анонімний UUID (можна взяти за «ідентифікатор установки» для CC-10, але його треба показати в налаштуваннях і вирішити, чи скидати при «Видалити всі дані»: там `clearAll()`).

## 5. Онбординг
Порядок першого запуску (`ui/home/HomeScreen.kt:305-381`, навігація `ui/navigation/TeperaNavHost.kt:307-310,399-424`):
1. вибір категорій — `CategoryOnboardingScreen` (прапорець `category_onboarding_seen`);
2. «Скільки, по-твоєму, ти був онлайн учора» — `OnlineEstimateOnboardingScreen` (`online_estimate_onboarding_seen`);
3. екран дозволів — `OnboardingScreen`: доступ до статистики використання + сповіщення, «Пропустити» (`onboarding_usage_access_seen`); умова: `hasUsageAccess == false && !onboardingSeen` (`HomeScreen.kt:326`);
4. пропозиція віджета — `WidgetSuggestionScreen` (`widget_suggestion_seen`);
5. запит `POST_NOTIFICATIONS` на Home-запобіжник із затримкою 1 с (`HomeScreen.kt:352-381`).
Кроку «Орієнтир на день» немає. Логічне місце — після кроку 3 і після реальної появи доступу; ланцюжок гейтиться каскадом `LaunchedEffect`-ів, і кожен новий крок вимагає власного прапорця в `SettingsStore` та врахування в решті гейтів (крихке місце, вже було 2 баги послідовності).

## 6. Тижнева картка і «Поділитися»
- Тижнева картка: `ui/home/WeeklyDigestCard.kt` + `WeeklyDigestViewModel.kt` (рядки `weekly_digest_*`, `strings.xml:146-149,365-367`) та `WeeklyReflectionCard.kt` («оцінка → реальність»).
- **Кнопки «Поділитися» і шерингу немає взагалі** (пошук `ACTION_SEND`/`share`/`Поділит` у `app/src/main` порожній; `FileProvider` теж). Прибирати нічого — цю частину CC-8 виконано «за замовчуванням». Тижневого сповіщення теж немає.
- Зовнішні виходи наявні лише як `ACTION_VIEW` (`ui/settings/SettingsScreen.kt:95`, `AboutScreen.kt:83`, `GateRepository.kt:76`) і `ACTION_SENDTO` mailto (`AboutScreen.kt:97`).

## 7. Рядки UA/EN з оцінкою чи тиском (за списком плану)
«Офлайн»/"Offline" (заміна на «без телефону»/"away from the phone"):
- UA: `stats_offline_label` (`values/strings.xml:59`), `balance_rest_of_day_label` «Офлайн-життя» (`:61`), `onboarding_explanation` (`:67`, двічі), `settings_sleep_window_info` (`:185`), `stats_day_legend_offline` (`:331`).
- EN: `stats_offline_label` (`values-en/strings.xml:52`), `balance_rest_of_day_label` "Offline life" (`:54`), `onboarding_explanation` (`:60`, двічі), `settings_sleep_window_info` (`:176`), `stats_day_legend_offline` (`:320`).
- Також коментарі коду з «Offline» (не користувацькі, не змінюємо).

Інші збіги зі списком заборонених:
- EN `gates_pin_failed`: "try again" (`values-en:239`) — збіг із "again". UA-версія «спробуй ще раз» (`values:248`) не збігається.
- `settings_target_info` (UA `:182`, EN `:173`): «без оцінки перевищення» / "no judgment for going over" — містить слово-тригер («перевищення»), хоч і заперечує. Переписати разом із CC-1.
- База знань `knowledge_base_section_4_body` (UA `:358`, EN `:347`): згадує «ціль»/"goal" лише щоб заперечити; варто перечитати після CC-1, бо там написано «орієнтир», який може зникнути.
- Технічні «ліміт»: `widget_settings_limit` (EN `:222`), `category_custom_limit_reached` (EN `:257`) — про кількість кнопок/категорій, не про оцінку; лишаємо.
- Лічильники: `gate_events_summary_text` (див. розд. 3); лічильники розблокувань (`stats_unlock_*`, `diary_unlock_*`) — це метрика, не «спроби», але за принципом пасивного сорому вони вже прибрані з Home; лишаємо.
- Кнопок із соромленням («Так, хочу згаяти час») не знайдено.
- Червоний колір: у цьому аудиті не перевірявся (потрібен окремий прохід по `TeperaPalette` і `MaterialTheme.colorScheme.error` під CC-2).
- Довгий рядок `strings.xml:354` (UA) утворений збігом — перевірити вручну при CC-2.

## 8. Ризики і блокери
1. **Crashlytics/INTERNET.** Обмеження плану порушується вже зараз. Прибрати Crashlytics = змінити D-рішення (SRS v4.1 його залишає), оновити Data Safety і `docs/play-console-declarations.md`, втратити звіти про падіння в закритому тесті. Потрібне рішення власника.
2. **POST_NOTIFICATIONS.** Наявний запит в онбордингу й сповіщення таймера суперечать «лише в CC-8, після ввімкнення підсумку». Варіант: прибрати запит з онбордингу/Home, запитувати лише при ввімкненні підсумку; сповіщення таймера залишити (як «інше сповіщення») чи прибрати — рішення.
3. **Заморозка скоупу D-04** та відсутній `POSITIONING-Tepera.md`.
4. **Незафіксований журнал даних.** Нема таблиці метрик і денних знімків (CC-9, CC-4): потрібні нові Room-таблиці → версія БД 11, `Migration`, юніт-тести.
5. **`gate_bypassed`** залежить від `UsageStatsManager.queryEvents` (глибина історії ~7–9 днів, точність `MOVE_TO_FOREGROUND` на P9/EMUI не перевірена) і від розкладу/паузи з CC-5; порядок реалізації — CC-9 (схема) → CC-5 → CC-4 (знімок).
6. **Онбординг-каскад** (розд. 5): додавання кроку «Орієнтир» — місце, де вже були баги послідовності. Вимагає живої перевірки на пристроях.
7. **Пристрої.** Перевірка на 4 пристроях, Gmail/Telegram на S23 і P9 — лише з підключеними пристроями (у сесії жоден не підключений); `adb` у PATH — старий, використовувати `platform-tools/adb.exe`.
8. **Дедлайн 01.10** — 6 днів при ~2 год/день; найважчий P0 — CC-5. P1 (CC-6/8/11) реально вирізати, якщо не встигаємо.
9. **Розбіжності плану і коду:** тривалість затримки 3/5/10 (не 5/10/20); Vico вже видалений; `GateEventsSummaryCard` як лічильник; віджет уже без орієнтира.

# RevenueCat — Tepera Pro, Paywall, Customer Center і "Пригостити кавою"

Стан: **інтегровано й перевірено в debug на Samsung S23 через Test Store** (без грошей): SDK
налаштовується, Paywall показує monthly/yearly/lifetime, тестова покупка активує entitlement
`tepera_pro`, екран Pro показує стан, Customer Center відкривається. Що лишилось — налаштування
дашборда й release-ключ (розділ "Перед публікацією").

## Що є в коді

| Що | Де |
|---|---|
| Залежності `purchases-kmp-core` + `purchases-kmp-ui` 3.9.0 (KMP-артефакти в Android-модулі, без реструктуризації) | `app/build.gradle.kts` |
| Ключ SDK з `local.properties` → `resValue revenuecat_api_key` (debug: `revenuecat.apiKey.debug`, release: `revenuecat.apiKey.release`) | `app/build.gradle.kts` |
| Налаштування SDK один раз у `Application.onCreate()`, анонімний користувач | `data/billing/RevenueCatConfig.kt`, `TeperaApp.kt` |
| Pro: стан із `CustomerInfo.entitlements["tepera_pro"]`, делегат `onCustomerInfoUpdated`, `refresh()`, `restore()` | `data/billing/ProRepository.kt`, `RevenueCatProRepository.kt` |
| Підтримка ("Пригостити кавою"): пакети offering `support`, покупка `awaitPurchase` | `data/billing/SupportRepository.kt`, `RevenueCatSupportRepository.kt` |
| Екрани: Pro (стан, Paywall, Customer Center, відновлення), Paywall, Customer Center, підтримка | `ui/pro/`, `ui/support/`, рядки в Налаштуваннях → Загальні |
| Фейки для розробки UI без дашборда | `FakeSupportRepository.kt` (`FakeSupportRepository`, `FakeProRepository`) |

Рішення (за відповідями користувача): **Pro поки нічого не гейтить** — лише інфраструктура (стан,
Paywall, Customer Center); Paywall не з'являється сам. Кава-донат перенесено на RevenueCat.
**Кава → підписка пізніше:** екран підтримки будується від пакетів offering `support`, а не від
жорстких id — щоб замінити разові рівні на "щомісячну підтримку", достатньо змінити продукти/пакети
в offering `support` у дашборді (нові package identifier без локалізації показують назву зі стору).

## Налаштування дашборда RevenueCat (робить власник)

1. **Проєкт і застосунок.** Test Store вже працює (продукти `monthly`, `yearly`, `lifetime` створені).
   Для реальних покупок додати **Google Play app** (`com.serkodesign.tepera`) і завантажити service
   account credentials.
2. **Продукти в Google Play Console** (Monetize): підписки `monthly` і `yearly` (base plans), one-time
   `lifetime`; потім імпортувати їх у RevenueCat. Ідентифікатори у Play можуть відрізнятись від Test
   Store — прив'язка йде через дашборд.
3. **Entitlement `tepera_pro`** — прикріпити всі три продукти.
4. **Offering `default`** (current) з пакетами `$rc_monthly`, `$rc_annual`, `$rc_lifetime` — його
   показує Paywall.
5. **Paywall:** зараз показується дефолтний шаблон ("Displaying default template because paywall is
   missing for offering 'default'"). Створити й **опублікувати** Paywall для offering `default`
   (Paywalls → шаблон, тексти, кольори під Monastic Style: без таймерів, знижкових "-50%" і тиску).
6. **Customer Center:** налаштувати (Customer Center → шляхи скасування, повернення коштів, підтримка).
7. **Offering `support`** для кави: пакети з package identifier `support_small`, `support_medium`,
   `support_large` (мають локалізовані назви в застосунку), продукти — consumable one-time в Play.
   Без цього offering екран підтримки чесно показує "Поки що недоступно".

## Перед публікацією (обов'язково)

- **Release-ключ.** Test Store ключ (`test_...`) у release SDK відхиляє (крашить). У release-збірці ключ
  береться з `revenuecat.apiKey.release` у `local.properties` — покласти туди **Google Play публічний
  SDK ключ** (`goog_...`). Без нього release-збірка запускається, але SDK вимкнений (Pro й підтримка
  "недоступні"), а не падає. Ключ у репозиторій не комітиться (`local.properties` в `.gitignore`).
- **Data Safety / політика конфіденційності:** RevenueCat отримує анонімний App User ID, історію
  покупок і технічні дані пристрою (через мережу RevenueCat). Форму Data Safety й `privacy-policy.md`
  треба оновити (Purchase history, Device or other IDs; мета — функціональність застосунку/покупки).
  Play отримає мітку "Contains in-app purchases".
- **Політика Google Play:** цифрові покупки — лише через Play Billing (RevenueCat його використовує);
  не додавати зовнішні посилання на донати.
- **Тестування на Play:** License testers у Play Console + збірка з тестового треку; `adb install`
  debug-збірка ходить у Test Store, не в Play.

## Відомі нюанси

- Тестова покупка в Test Store лишила на анонімному App User ID S23 активний тестовий `monthly`; вона
  нічого не гейтить і видима лише в дашборді (Customers). Прострочується сама (Test Store прискорює
  періоди).
- Paywall закриває сам себе через `dismissRequest` і після успішної покупки; окремого слухача для
  закриття не додавати (давало подвійне закриття).
- Моделі KMP на Android — typealias нативних типів (`CustomerInfo`, `Package`, `PurchasesException` з
  полем `error`, `expirationDateMillis`).

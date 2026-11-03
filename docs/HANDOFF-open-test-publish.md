# Tepera — передача публікації відкритого тесту на інший комп'ютер

Складено 03.11.2026 (гілка `feature/v3-mvp`, коміт `4528afb` + незакомічені правки документів).
Повний чекліст — `CHECKLIST-open-test-play.md` (від 01.11); цей файл — що **вже зроблено**, що **перенести** і що **лишилось**.
Рішення з чекліста чинні: варіант A (збірка `beta` = v3 + Accessibility, той самий `applicationId`), Auto Backup не чіпаємо.

## 1. Що готово на цьому комп'ютері

| Що | Де | Стан |
|---|---|---|
| Підписаний AAB для Play | `release/tepera-1.0.4-beta-24-play.aab` (8,3 МБ) | зібрано 03.11 з коміту `4528afb` (`bundleBetaRelease`, R8), `jarsigner`: verified, юніт-тести `beta` пройшли |
| Відбиток upload-ключа (SHA-256) | `C6:BB:72:F3:FE:E1:24:9F:7E:38:2E:06:D5:75:C4:C3:B1:A5:B1:AB:64:18:DB:93:78:CE:6E:00:82:0F:66:86` | збігається з `keystore/ПРОЧИТАЙ-ПРО-КЛЮЧ.txt` |
| Версія | `1.0.4-beta`, versionCode **24** | див. п. 4.1 |
| Іконка 512, feature graphic UA/EN, скріншоти | `docs/play-store/` | **застарілі** (див. п. 3) |
| Описи сторінки магазину UA/EN | `docs/store-listing-ua.md`, `store-listing-en.md` | про v3 нічого не кажуть (п. 3) |
| Політика конфіденційності | `docs/privacy-policy.md` | оновлена 03.11 (v3 + Accessibility), **не опублікована** (п. 2) |
| Відповіді для форм Play | `docs/play-console-declarations.md`, `play-accessibility-declaration.md`, `v3-privacy-datasafety-draft.md` | declaration доповнено розділом 7 (v3-оверлей) |
| Субтитри відео | `docs/play-store/accessibility-demo.en.srt` | без кадрів v3 (п. 3) |
| Інструкція тестувальникам | `docs/TESTER-GUIDE-v3-beta.md` | чернетка з `[дужками]` (п. 3) |

Застарілий файл `release/tepera-1.0.5-24.aab` (27.10) **не завантажувати** — старіший за поточний код.

## 2. Що перенести на інший комп'ютер

Android Studio там не потрібна: усе, що лишилось, робиться в браузері (Play Console, GitHub, YouTube).

1. `release/tepera-1.0.4-beta-24-play.aab` — `*.aab` в `.gitignore`, **через git не поїде**: USB / хмарний диск.
2. Папка `docs/` (або весь репозиторій після `git push`; зараз правки документів **не закомічені**).
3. **Keystore НЕ потрібен**, щоб завантажити готовий AAB — він уже підписаний. Але переконайтесь, що копія
   `keystore/tepera-upload.jks` + паролі лежать поза цим комп'ютером (менеджер паролів + носій). Не пересилайте файл
   ключа поштою/месенджером без потреби; пароль у `ПРОЧИТАЙ-ПРО-КЛЮЧ.txt` лежить відкритим текстом.
4. Звіт-доказ для рецензента (за потреби): відео з п. 3.

## 3. Що лишилось зробити (по порядку)

**3.1 Опублікувати політику.** Жива сторінка `https://serkodesign.github.io/tepera/privacy-policy` досі показує
«Last updated 28.09.2026» — без v3, без Auto Backup, без Accessibility-розділів. Треба змерджити/запушити
`docs/privacy-policy.md` у ту гілку, з якої збирається GitHub Pages (**[звірити]** Settings → Pages; зазвичай `main` + `/docs`),
і переконатися, що URL відкриває нову дату. Це дія назовні — без неї Data Safety і декларація Accessibility суперечать
публічному тексту.

**3.2 ⚠ Розкриття в застосунку не покриває v3 (блокер для відео й рев'ю).** Діалог `a11y_disclosure_body` каже лише:
«побачити назву застосунку… і показати коротку паузу перед тими, що ви обрали». А в `beta` сервіс також показує підказку
поверх стрічки, головоломку і затемнення «Ще 5 / Вийти». Google порівнює розкриття, декларацію й поведінку. Потрібне
рішення власника щодо тексту (можу застосувати й перезібрати AAB). Пропозиція EN:
«…to see the name of the app that has just opened, and to show a short pause or a suggestion over the apps and feeds you
chose yourself, and a dimming with "5 more / Leave" buttons after a timer you set.» (UA — симетрично). Після зміни
рядків AAB з п. 1 треба замінити новим.

**3.3 Демо-відео для Accessibility** (≈ 2,5 хв, EN-субтитри, з **release**-збірки). Сценарій — розділ 4 +
доповнення розділу 7 у `play-accessibility-declaration.md`. Потрібен телефон і запис екрана; на іншому ПК лише змонтувати
субтитри й залити на YouTube (unlisted). Дописати в `.srt` кадри 11a–11d. Без відео форму не подати.

**3.4 Скріншоти й описи під v3.** Поточні скріншоти (вересень–жовтень) показують старий інтерфейс. Для відкритого треку
потрібні ≥2 скріншоти телефону UA/EN з v3-головної («Коли ви з телефоном», години скролінгу), підказки, підсумку тижня.
У `store-listing-*.md` додати абзац про години скролінгу й підказки + **чесно про Accessibility** (Google вимагає
згадувати у картці, навіщо він). Пристрою зараз не підключено — знімати треба на телефоні.

**3.5 `TESTER-GUIDE-v3-beta.md`:** підставити посилання на Play-тест (замість APK), форму звіту, контакт; прибрати блок
«Для автора».

## 4. Play Console — порядок дій (на іншому комп'ютері) **[звірити з актуальним інтерфейсом]**

1. **Policy → App content:** Privacy policy URL; **Accessibility API declaration** (відповіді: розділ 2 + 7 декларації,
   посилання на відео); Data Safety (за `play-console-declarations.md` + розділ 4 `v3-privacy-datasafety-draft.md`);
   Content rating; Target audience 18+; Ads — немає; App access (інструкція: Usage access + Accessibility вмикаються вручну).
2. Якщо консоль питає про `PACKAGE_USAGE_STATS` / foreground service — відповіді в чеклісті, п. 3.2.
3. **Testing → Open testing → Create new release** → завантажити AAB → «Що нового» (UA/EN, нижче) → країни → надіслати.
4. Очікувати рев'ю: для оновлення, що **додає** Accessibility вже існуючому застосунку, воно суворіше й довше.
   Відмова — прочитати причину, виправити, не подавати повторно поспіль.

**4.1 versionCode.** Beta = 24, standard = 22 (`app/build.gradle.kts`). Перед завантаженням порівняйте з номерами,
які вже є в Play Console (усі треки): якщо там уже є 24 або вище — потрібно підняти й перезібрати
(`./gradlew.bat :app:bundleBetaRelease`; Android Studio на цьому комп'ютері лишається потрібною для збірки).

### «Що нового» (чернетка)
- UA: «Нова версія з режимом «Години скролінгу»: Tepera знаходить години, коли ви зазвичай зависаєте в стрічках, і тихо пропонує іншу дію саме тоді. Для цього потрібна служба Спеціальних можливостей (вмикається лише вами). Без оцінок і серій.»
- EN: "New "Scrolling hours" mode: Tepera finds the hours when you usually get stuck in feeds and quietly suggests another action just then. It needs an Accessibility service, which only you can turn on. No scores, no streaks."

## 5. Що ще не перевірено на пристроях (з чекліста, розділ 5 і 8)
Release `beta` на Samsung S23 проходив 01.11 (запуск, онбординг, розкриття, ворота). **Після цього додано 3 коміти оверлея**
(`e4eb2c3`, `f78c154`, `4528afb`) — головоломки, фантомні події лаунчера, пропуск шторки з 4-го відкриття. Їх на
release-збірці не проганяли. Також не перевірено: Moto G84 і Huawei P9 на release, Crashlytics-звіт з release, сповіщення
тижневого підсумку, віджети на release, TalkBack, імпорт/експорт v3 наживо, `wipeAllData()` v3. Рекомендовано: Internal
testing → встановити з Play на S23/P9 → короткий прогін перед відкритим треком; прочитати Pre-launch report.

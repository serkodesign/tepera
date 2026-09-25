# Tepera 0.2.0 (versionCode 2) — збірка для закритого тесту

Дата збірки: 25.09.2026. Файл: `app/build/outputs/bundle/release/app-release.aab` (підписаний release-ключем).
Після 07.10 у цій збірці — лише критичні виправлення; кожне записується в `VERIFICATION-Tepera.md` з датою.

## Що нового (для тестувальників, UA)
- **Орієнтир на день — на твій вибір.** Після дозволу на статистику застосунок пропонує задати орієнтир (стартове значення — твоє власне середнє) або відкласти. Орієнтир можна вимкнути зовсім у Налаштуваннях → Відстеження.
- **Ворота: пауза й розклад.** Паузу можна поставити на сьогодні, на вихідні або до дати; розклад — по днях тижня, до двох проміжків на день (можна через північ). Поза розкладом і під час паузи ярлик одразу відкриває застосунок.
- **Екран паузи воріт:** 27 коротких фраз (кожна по одному разу, нічні — лише вночі), кнопка «Не зараз» одразу, «Відкрити …» — після затримки. За бажанням — зростаюча затримка.
- **Після перерви від 3 днів** — спокійний підсумок цих днів на Home.
- **Тижневий підсумок** (за бажанням): одне тихе сповіщення в неділю ввечері. Дозвіл на сповіщення просимо лише коли ти сам увімкнеш.
- **Тексти без оцінок:** «без телефону» замість «офлайн», прибрані слова, що звучать як докір.
- **Звіти про збої** — можна вимкнути в Налаштуваннях; про це сказано на початку.
- **Tepera Pro** — лише питання до тебе: що було б корисним (без цін і оплат).

## What's new (for testers, EN)
- **A daily reference, your choice.** After granting usage access the app offers to set a reference (starting from your own average) or to leave it for later. You can turn it off completely in Settings → Tracking.
- **Gates: pause and schedule.** Pause for today, for the weekend or until a date; a schedule by weekday with up to two periods a day (can cross midnight). Outside the schedule and while paused the shortcut opens the app right away.
- **Gate pause screen:** 27 short lines (each once; night lines only at night), a "Not now" button right away, "Open …" after the delay. An optional growing delay.
- **After a break of 3+ days** — a calm summary of those days on Home.
- **Weekly summary** (optional): one quiet notification on Sunday evening. We ask for notification permission only when you turn it on.
- **Wording without judgment:** "away from the phone" instead of "offline", judgmental words removed.
- **Crash reports** can be turned off in Settings; this is stated at the start.
- **Tepera Pro** — just a question for you: what would be useful (no prices, no payments).

## Що перевірено перед збіркою
- `testDebugUnitTest` — 72 тести, 0 збоїв; `bundleRelease` — успішно, `jarsigner -verify` — «jar verified».
- Змерджений release-маніфест: `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `INTERNET`, `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (+ службовий). `BILLING` немає.
- Перевірки на пристроях (S23; G84, P9, XZ1 ще ні) — див. `VERIFICATION-Tepera.md`, протоколи V-20…V-26.

## Відомі обмеження (для власника)
- `INTERNET` лишається через Crashlytics (звіти про збої — опційні, за замовчуванням увімкнені на час тесту, D-15/D-20).
- Форма для «Tepera Pro» ще тимчасово веде на форму «Запропонувати функцію» (D-31).
- `privacy-policy.md` і `play-console-declarations.md` ще описують Crashlytics як безумовний і не згадують `POST_NOTIFICATIONS` — оновити перед завантаженням у Play.

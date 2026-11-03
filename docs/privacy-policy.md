---
title: Privacy Policy — Tepera
---

# Privacy Policy / Політика конфіденційності — Tepera

*Останнє оновлення / Last updated: 03.11.2026*

---

## Українською

**Tepera** — це офлайн-first застосунок. Ми не збираємо, не продаємо і не передаємо треті
особам жодних персональних даних.

**Які дані обробляються:**
- Усі записи про активність (категорія, тривалість, нотатки) зберігаються **лише локально на вашому
  пристрої**, в захищеному сховищі застосунку.
- Резервне копіювання відбувається виключно через стандартний механізм **Android Auto Backup**
  (керується Google Account на вашому пристрої) або через ручний експорт файлу, який ви самі контролюєте.
  Android може зберігати дані застосунку в резервній копії вашого облікового запису Google за
  налаштуваннями вашого телефона; сама Tepera нічого не надсилає.
- **Що входить у файл експорту** (Налаштування → Дані й приватність → «Експортувати статистику»):
  ваші активності (назва, іконка, колір, порядок, архівація), усі записи часу (початок, тривалість,
  нотатка), виключені застосунки, орієнтир Online-часу та щоденна історія Online (хвилини Online,
  розблокування, початок і кінець дня, Online по годинах). **У тестовій (beta) збірці з режимом
  «Години скролінгу»** до файлу також потрапляють: обрані вами застосунки-стрічки, час у них за
  15-хвилинними проміжками доби, знайдені години скролінгу, **ваші плани «якщо — то» (текст, який ви
  пишете самі)**, час нагадувань із вашими відповідями на них і тижні спостереження. Файл створюється
  на вашому пристрої й зберігається там, куди ви його збережете; **Tepera його нікуди не надсилає**.
  Файл не зашифрований, тож зберігайте його так, як зберігаєте особисті дані, і не передавайте
  стороннім. Імпорт файлу повністю замінює відповідні дані на пристрої; налаштування (мова, тема,
  вікно сну, ворота, віджет) у файл не входять.
- Щоб показати співвідношення екранного й реального часу, застосунок читає **статистику
  використання застосунків на вашому пристрої** через системний API `UsageStatsManager` (доступ
  надається вручну через Налаштування Android). Ці дані обробляються **виключно на пристрої**,
  ніколи не передаються на сервер і не включаються в crash-звіти. Ви можете виключити окремі
  застосунки зі списку врахованих (Exclusion List) або взагалі не надавати цей доступ — офлайн-
  логування активностей залишиться повністю робочим.
- **Лише у тестовій (beta) збірці — служба Спеціальних можливостей (Accessibility) для «Застосунків
  з затримкою».** Якщо ви самі її вмикаєте, служба бачить **тільки назву пакета** застосунку, що
  вийшов на передній план, і показує коротку паузу перед тими застосунками, які ви обрали в Tepera.
  Вона **не читає** вміст екрана, текст, паролі, повідомлення чи введення. Рішення на екрані паузи
  («Продовжити» / «Не зараз») зберігаються **лише локально** на пристрої, нічого не передається на
  сервер і нікому не надається. **У режимі «Години скролінгу»** та сама служба, коли ви відкриваєте
  обрану вами стрічку у власну годину скролінгу, показує поверх екрана підказку Tepera (альтернативна
  дія або таймер), а після обраного вами таймера — тимчасове затемнення з кнопками «Ще 5» / «Вийти».
  Служба так само бачить лише назву пакета й малює тільки елементи Tepera; вона не читає вміст чужих
  застосунків і не керує ними. Службу можна вимкнути будь-коли в Налаштуваннях Android →
  Спеціальні можливості. Без неї застосунок працює повністю; у звичайній (не beta) збірці цієї служби немає.
- Ми **не** запитуємо дозвіл на геолокацію (в жодній формі, включно з фоновою).
- Ми **не** створюємо облікових записів і не вимагаємо реєстрації.

**Аналітика й мережа:** застосунок сам не робить мережевих запитів, окрім анонімних crash-звітів (через Firebase Crashlytics, для чого в застосунку є дозвіл на доступ до мережі) у разі
збою. Звіт містить лише модель телефону, версію Android і технічний опис самого збою — без
записів активностей, категорій чи часу використання застосунків, без ідентифікації користувача
та без трекінгу поведінки в застосунку. **Це можна вимкнути:** перемикач «Звіти про збої»
показується на першому екрані онбордингу і завжди доступний у Налаштування → Загальні; на час
закритого тестування він увімкнений за замовчуванням.

**Ваш контроль:** ви можете в будь-який момент експортувати свої дані та імпортувати їх назад
(Налаштування → Дані й приватність) або видалити їх усі (Налаштування → Дані й приватність →
«Видалити всі дані»; потрібно двічі підтвердити). Видалення очищає й дані режиму «Години скролінгу». Видалення застосунку видаляє всі локальні дані (крім копії в Android Auto
Backup, яку можна вимкнути в системних налаштуваннях Android).

**Контакт:** serkodesign@gmail.com

---

## English

**Tepera** is an offline-first app. We do not collect, sell, or share any personal data
with third parties.

**What data is processed:**
- All activity entries (category, duration, notes) are stored **locally on your device only**, in the
  app's protected storage.
- Backups happen only through the standard **Android Auto Backup** mechanism (tied to your Google
  Account on-device) or through a manual file export that you fully control. Android may keep the
  app's data in your Google account backup according to your phone settings; Tepera itself sends
  nothing.
- **What the export file contains** (Settings → Data and privacy → "Export statistics"): your
  activities (name, icon, colour, order, archived state), all activity entries (start, duration,
  note), excluded apps, the Online-time reference and the daily Online history (Online minutes,
  unlocks, day start and end, Online by hour). **In the test (beta) build with the "Scrolling
  hours" mode** the file also includes: the feed apps you chose, the time spent in them in
  15-minute slots of the day, the scrolling hours found, **your "if — then" plans (text you write
  yourself)**, reminder times with your replies to them, and observation weeks. The file is created
  on your device and stays wherever you save it; **Tepera does not send it anywhere**. The file is
  not encrypted, so keep it as you would any personal data and do not share it with others.
  Importing a file fully replaces the corresponding data on the device; settings (language, theme,
  sleep window, gates, widget) are not part of the file.
- To show your screen-time vs. real-life balance, the app reads **app usage statistics on your
  device** through the system `UsageStatsManager` API (access is granted manually via Android
  Settings). This data is processed **entirely on-device**, never transmitted to a server, and never
  included in crash reports. You can exclude specific apps from being counted (Exclusion List), or
  choose not to grant this access at all — offline activity logging remains fully functional either way.
- **Test (beta) build only — an Accessibility service for "Delayed apps".** If you turn it on
  yourself, the service sees **only the package name** of the app that came to the foreground and
  shows a short pause before the apps you picked in Tepera. It does **not** read screen content,
  text, passwords, messages or anything you type. Your choices on the pause screen ("Continue" /
  "Not now") are stored **only locally** on the device; nothing is sent to a server or shared with
  anyone. **With the "Scrolling hours" mode**, the same service, when you open a feed you chose during
  your own scrolling hour, shows a Tepera suggestion over the screen (an alternative action or a timer)
  and, after a timer you picked, a temporary dimming with "5 more" / "Leave" buttons. It still sees only
  the package name and only draws Tepera's own elements; it does not read the content of other apps or
  control them. You can turn the service off at any time in Android Settings → Accessibility. The app
  works fully without it; the regular (non-beta) build contains no such service.
- We do **not** request location permissions, in any form, including background location.
- We do **not** create user accounts or require registration.

**Analytics and network:** the app makes no network requests of its own except anonymous crash reports (via Firebase Crashlytics, which is why the app holds the network permission) when something
goes wrong. A crash report contains only the phone model, Android version, and a technical
description of the crash — never your activity entries, categories, or app-usage time, no user
identification, and no in-app behavior tracking. **You can turn this off:** the "Crash reports"
toggle is shown on the first onboarding screen and always available in Settings → General; it is
on by default during the closed test.

**Your control:** you can export your data at any time and import it back (Settings → Data and
privacy) or delete all of it (Settings → Data and privacy → "Delete all data"; you have to confirm
twice). Deleting also clears the "Scrolling hours" data. Uninstalling the app removes all local data (except any Android Auto Backup copy, which can be
disabled in Android system settings).

**Contact:** serkodesign@gmail.com

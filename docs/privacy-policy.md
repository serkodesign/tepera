---
title: Privacy Policy — Tepera
---

# Privacy Policy / Політика конфіденційності — Tepera

*Останнє оновлення / Last updated: 23.11.2026*

---

## Українською

**Tepera** — це офлайн-first застосунок. Ми не збираємо, не продаємо і не передаємо треті
особам жодних персональних даних.

**Які дані обробляються:**
- Усі записи про активність (категорія, тривалість, нотатки) зберігаються **лише локально на вашому
  пристрої**, в захищеному сховищі застосунку. Tepera нічого нікуди не надсилає, і нижче це
  стосується всіх даних, окрім анонімних звітів про збої (див. «Аналітика й мережа»).
- Резервне копіювання відбувається виключно через стандартний механізм **Android Auto Backup**
  (за налаштуваннями вашого телефона й облікового запису Google; його можна вимкнути в системних
  налаштуваннях Android) або через ручний експорт файлу, який ви самі контролюєте.
- **Що входить у файл експорту** (Налаштування → Дані й приватність → «Експортувати статистику»):
  ваші активності (назва, іконка, колір, порядок, архівація), усі записи часу (початок, тривалість,
  нотатка), виключені застосунки, орієнтир Online-часу та щоденна історія Online (хвилини Online,
  розблокування, початок і кінець дня, Online по годинах). **У тестовій (beta) збірці з режимом
  «Години скролінгу»** також: обрані вами застосунки-стрічки, час у них за 15-хвилинними проміжками
  доби, знайдені години скролінгу, список планів «якщо — то» (поточна версія застосунку їх не
  створює, тож він зазвичай порожній), час нагадувань із вашими відповідями на них і тижні
  спостереження. Файл не зашифрований, тож зберігайте його як особисті дані й не передавайте
  стороннім. Імпорт повністю замінює відповідні дані на пристрої; налаштування (мова, тема, вікно
  сну, ворота, віджет) у файл не входять.
- Щоб показати ваш екранний час, застосунок читає **статистику
  використання застосунків на вашому пристрої** через системний API `UsageStatsManager` (доступ
  надається вручну через Налаштування Android). Ці дані обробляються **виключно на пристрої**,
  ніколи не передаються на сервер і не включаються в crash-звіти. Ви можете виключити окремі
  застосунки зі списку врахованих (Exclusion List) або взагалі не надавати цей доступ — офлайн-
  логування активностей залишиться повністю робочим.
- **Лише в beta-збірці — служба Спеціальних можливостей (Accessibility).** Якщо ви самі її вмикаєте,
  вона бачить **тільки назву застосунку**, що вийшов на передній план. Tepera показує коротку паузу
  перед застосунками, які ви обрали для «Застосунків з затримкою». У режимі «Години скролінгу», коли
  ви відкриваєте обрану стрічку у власну годину, вона показує підказку (альтернативну дію або
  таймер; з третього повторного відкриття, якщо ви не вимкнули цю опцію, — коротку головоломку до
  30 секунд; вийти можна завжди), а через 5 хвилин — тимчасове затемнення з кнопками «Ще 5» /
  «Вийти». **Берсерк-режим** (необов'язковий, вимкнений за замовчуванням): якщо ви його ввімкнули й
  провели в обраних стрічках заданий вами час, Tepera на заданий вами час показує поверх стрічки
  екран блоку й відправляє вас на робочий стіл; «Важлива справа» вимикає режим до завтра. Рішення
  на екранах паузи й блоку зберігаються лише на пристрої. Служба не читає вміст екрана, текст,
  паролі, повідомлення чи введення й не натискає, не прокручує та не вводить текст у чужих
  застосунках. Єдина її дія в них — повернення на робочий стіл (за вашим «Вийти» або коли Берсерк-режим
  завершує перебування в стрічці); решту вона лише малює елементами Tepera. Вона вмикається лише після вашої згоди на окремому екрані (другий крок
  початкового налаштування; знову доступний у Налаштування → Цифровий детокс); якщо натиснути «Не
  зараз», нічого не вмикається. Вимкнути можна будь-коли в Налаштуваннях Android → Спеціальні
  можливості. Без неї застосунок працює повністю; у звичайній (не beta) збірці цієї служби немає.
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
Backup, якщо вона ввімкнена).

**Контакт:** serkodesign@gmail.com

---

## English

**Tepera** is an offline-first app. We do not collect, sell, or share any personal data
with third parties.

**What data is processed:**
- All activity entries (category, duration, notes) are stored **locally on your device only**, in the
  app's protected storage. Tepera sends nothing anywhere, and this applies to all the data below,
  except anonymous crash reports (see "Analytics and network").
- Backups happen only through the standard **Android Auto Backup** mechanism (according to your
  phone and Google account settings; it can be turned off in Android system settings) or through a
  manual file export that you fully control.
- **What the export file contains** (Settings → Data and privacy → "Export statistics"): your
  activities (name, icon, colour, order, archived state), all activity entries (start, duration,
  note), excluded apps, the Online-time reference and the daily Online history (Online minutes,
  unlocks, day start and end, Online by hour). **In the test (beta) build with the "Scrolling
  hours" mode** also: the feed apps you chose, the time spent in them in 15-minute slots of the day,
  the scrolling hours found, a list of "if — then" plans (the current version of the app no longer
  creates them, so it is usually empty), reminder times with your replies to them, and observation
  weeks. The file is not encrypted, so keep it as you would any personal data and do not share it
  with others. Importing fully replaces the corresponding data on the device; settings (language,
  theme, sleep window, gates, widget) are not part of the file.
- To show your screen time, the app reads **app usage statistics on your
  device** through the system `UsageStatsManager` API (access is granted manually via Android
  Settings). This data is processed **entirely on-device**, never transmitted to a server, and never
  included in crash reports. You can exclude specific apps from being counted (Exclusion List), or
  choose not to grant this access at all — offline activity logging remains fully functional either way.
- **Beta build only — an Accessibility service.** If you turn it on yourself, it sees **only the
  name of the app** that came to the foreground. Tepera shows a short pause before the apps you
  picked for "Delayed apps". With the "Scrolling hours" mode, when you open a feed you chose during
  your own scrolling hour, it shows a suggestion (an alternative action or a timer; from the third
  repeated opening, unless you turn this option off, a short puzzle of up to 30 seconds; you can
  always leave) and, after 5 minutes, a temporary dimming with "5 more" / "Leave" buttons. **Berserk
  mode** (optional, off by default): if you turned it on and spent the time you set in your chosen
  feeds, Tepera shows a block screen over the feed for the time you set and takes you to the home
  screen; "Important thing" turns the mode off until tomorrow. Your choices on the pause and block
  screens are stored only on the device. The service does not read screen content, text, passwords,
  messages or anything you type, and does not tap, scroll or type in other apps. Its only action in
  them is returning to the home screen (on your "Leave", or when Berserk mode ends a stay in a feed);
  everything else it only draws as Tepera's own elements. It turns on only after you agree on a separate
  screen (the second step of the initial setup; available again in Settings → Digital detox); if you
  tap "Not now", nothing is turned on. You can turn it off at any time in Android Settings →
  Accessibility. The app works fully without it; the regular (non-beta) build contains no such
  service.
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
twice). Deleting also clears the "Scrolling hours" data. Uninstalling the app removes all local data (except an Android Auto Backup copy, if enabled).

**Contact:** serkodesign@gmail.com

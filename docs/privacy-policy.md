---
title: Privacy Policy — Tepera
---

# Privacy Policy / Політика конфіденційності — Tepera

*Останнє оновлення / Last updated: 28.09.2026*

---

## Українською

**Tepera** — це офлайн-first застосунок. Ми не збираємо, не продаємо і не передаємо треті
особам жодних персональних даних.

**Які дані обробляються:**
- Усі записи про активність (категорія, тривалість, нотатки) зберігаються **лише локально на вашому
  пристрої**, в захищеному сховищі застосунку.
- Резервне копіювання відбувається виключно через стандартний механізм **Android Auto Backup**
  (керується Google Account на вашому пристрої) або через ручний експорт файлу, який ви самі контролюєте.
- Щоб показати співвідношення екранного й реального часу, застосунок читає **статистику
  використання застосунків на вашому пристрої** через системний API `UsageStatsManager` (доступ
  надається вручну через Налаштування Android). Ці дані обробляються **виключно на пристрої**,
  ніколи не передаються на сервер і не включаються в crash-звіти. Ви можете виключити окремі
  застосунки зі списку врахованих (Exclusion List) або взагалі не надавати цей доступ — офлайн-
  логування активностей залишиться повністю робочим.
- Ми **не** запитуємо дозвіл на геолокацію (в жодній формі, включно з фоновою).
- Ми **не** створюємо облікових записів і не вимагаємо реєстрації.

**Аналітика й мережа:** застосунок сам не робить мережевих запитів, окрім анонімних crash-звітів (через Firebase Crashlytics, для чого в застосунку є дозвіл на доступ до мережі) у разі
збою. Звіт містить лише модель телефону, версію Android і технічний опис самого збою — без
записів активностей, категорій чи часу використання застосунків, без ідентифікації користувача
та без трекінгу поведінки в застосунку. **Це можна вимкнути:** перемикач «Звіти про збої»
показується на першому екрані онбордингу і завжди доступний у Налаштування → Загальні; на час
закритого тестування він увімкнений за замовчуванням.

**Ваш контроль:** ви можете в будь-який момент експортувати свої дані (Налаштування → Резервне
копіювання) або видалити їх усі (Налаштування → Резервне копіювання → «Видалити всі дані»; потрібно
двічі підтвердити). Видалення застосунку видаляє всі локальні дані (крім копії в Android Auto
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
  Account on-device) or through a manual file export that you fully control.
- To show your screen-time vs. real-life balance, the app reads **app usage statistics on your
  device** through the system `UsageStatsManager` API (access is granted manually via Android
  Settings). This data is processed **entirely on-device**, never transmitted to a server, and never
  included in crash reports. You can exclude specific apps from being counted (Exclusion List), or
  choose not to grant this access at all — offline activity logging remains fully functional either way.
- We do **not** request location permissions, in any form, including background location.
- We do **not** create user accounts or require registration.

**Analytics and network:** the app makes no network requests of its own except anonymous crash reports (via Firebase Crashlytics, which is why the app holds the network permission) when something
goes wrong. A crash report contains only the phone model, Android version, and a technical
description of the crash — never your activity entries, categories, or app-usage time, no user
identification, and no in-app behavior tracking. **You can turn this off:** the "Crash reports"
toggle is shown on the first onboarding screen and always available in Settings → General; it is
on by default during the closed test.

**Your control:** you can export your data at any time (Settings → Backup and restore) or delete all
of it (Settings → Backup and restore → "Delete all data"; you have to confirm twice). Uninstalling the app removes all local data (except any Android Auto Backup copy, which can be
disabled in Android system settings).

**Contact:** serkodesign@gmail.com

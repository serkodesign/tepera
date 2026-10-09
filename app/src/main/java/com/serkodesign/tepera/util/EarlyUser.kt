package com.serkodesign.tepera.util

/**
 * P-3 (`CLAUDE-CODE-PLAN-pro.md`): grandfathering (Ж-5) — хто встановив Tepera ДО того, як Pro
 * вийшов, зберігає весь наявний безкоштовний функціонал назавжди. Чиста логіка, без Room/Context
 * — тестується без пристрою. Єдиний споживач поки немає ("нічого в UI не змінюється") — P-4
 * (`ProAccess.isEarlyUser()`) підключить це пізніше.
 */
object EarlyUser {

    /**
     * [firstSeenVersionCode] — `SettingsStore.firstSeenVersionCode`, наш власний запис із
     * першого запуску коду, що це вміє рахувати. У звичайній роботі `null` не трапляється
     * (`TeperaApp.onCreate()` сіє щозапуску, якщо ще не засіяно), але перевстановлення чи
     * "Видалити всі дані" скидає DataStore — тоді рахуємо за запасним джерелом:
     * [firstInstallTimeMillis] (`PackageInfo.firstInstallTime`, завжди доступний, без дозволів,
     * не зберігається застосунком — читається наживо в момент перевірки) проти
     * [proLaunchAtMillis] (коли Pro реально вийшов за календарем).
     *
     * Межовий випадок: `firstSeenVersionCode == proLaunchVersionCode` — НЕ рання установка
     * (людина отримала застосунок РАЗОМ із версією, де Pro вже існує, а не до неї).
     */
    fun isEarlyUser(
        firstSeenVersionCode: Int?,
        proLaunchVersionCode: Int,
        firstInstallTimeMillis: Long,
        proLaunchAtMillis: Long
    ): Boolean {
        if (firstSeenVersionCode != null) return firstSeenVersionCode < proLaunchVersionCode
        return firstInstallTimeMillis < proLaunchAtMillis
    }
}

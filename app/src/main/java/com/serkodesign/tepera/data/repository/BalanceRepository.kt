package com.serkodesign.tepera.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager
import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * FR-3.1–3.12 (SRS v2.5): доступ до статистики використання (UsageStatsManager) + Grace Period
 * Buffer, тепер відлічений від точки старту дня (перше суттєве розблокування), а не від півночі.
 *
 * ВАЖЛИВО: PACKAGE_USAGE_STATS — protected permission. Коли доступ НІКОЛИ не надавався,
 * queryUsageStats() справді повертає порожній список, без винятку. Але якщо доступ був
 * наданий, а тоді користувач вручну вимкнув його в Налаштуваннях (Спеціальний доступ) —
 * queryUsageStats() кидає SecurityException, а не повертає порожній список (підтверджено
 * крашем на реальному пристрої). Обидва методи нижче явно ловлять SecurityException, інакше
 * відкликаний посеред сесії доступ ламає fallback з FR-3.6 замість вмикати його.
 */
class BalanceRepository(
    private val context: Context,
    private val excludedAppDao: ExcludedAppDao
    // FR-3.4: сам таргет (дефолт 180 хв, налаштовується) живе в SettingsStore, не тут —
    // BalanceRepository лише рахує Online-хвилини й знаменник Grace Period Buffer.
) {

    /**
     * Перевірка, чи наданий доступ. Стандартний спосіб — спробувати запит і подивитись,
     * чи є хоч якийсь результат за коротким контрольним інтервалом; альтернатива —
     * AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS). Обрати підхід на етапі реалізації.
     */
    suspend fun hasUsageAccess(): Boolean = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 1000 * 60 * 60 // остання година як контрольний інтервал
        try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            stats.isNotEmpty()
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * FR-3.1, FR-3.11: сумарний Online-час від точки старту дня (FR-3.5) до зараз, за
     * вирахуванням застосунків зі списку виключень.
     */
    suspend fun getOnlineMinutesToday(dayStartMillis: Long): Int =
        getOnlineMinutes(dayStartMillis, System.currentTimeMillis())

    /**
     * FR-5.3: узагальнена версія getOnlineMinutesToday() для довільного інтервалу — потрібна
     * для тижневого тренду балансу (по одному запиту на кожен з минулих днів, той самий підхід,
     * що й для "сьогодні", а не один запит з INTERVAL_DAILY на весь тиждень: бакетизація
     * queryUsageStats() по межах діб не гарантовано збігається з локальною північчю).
     */
    suspend fun getOnlineMinutes(from: Long, to: Long): Int = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        // Лаунчер і клавіатура виключені за замовчуванням, поверх ручного списку виключень
        // (FR-3.5): це системні застосунки, час у яких — не "екранний час" у побутовому
        // розумінні (гортання додатків, набір тексту), Digital Wellbeing теж їх не рахує.
        // Визначаються динамічно через PackageManager/InputMethodManager, а не жорстко
        // захардкоджені імена пакетів — на тестових пристроях стоять різні лаунчери
        // (One UI Home, Microsoft Launcher, ROADMAP Фаза 3).
        val excluded = excludedAppDao.getExcludedPackageNames().toSet() + systemExclusionPackages()

        // ВАЖЛИВО: queryUsageStats(INTERVAL_DAILY, from, to) рахує totalTimeInForeground для
        // ЦІЛОГО бакета статистики, а не строго обрізаний на [from, to] — межі бакетів не
        // гарантовано збігаються з локальною північчю чи "зараз", тож для часткової доби він
        // систематично завищував Online-час (підтверджено користувачем: 159 хв Tepera проти
        // 47 хв у системному Samsung Digital Wellbeing за той самий момент). Точний підрахунок —
        // по сирих подіях MOVE_TO_FOREGROUND/MOVE_TO_BACKGROUND, обрізаних рівно по [from, to],
        // той самий підхід, що й системні лічильники екранного часу.
        try {
            val events = usm.queryEvents(from, to)
            val foregroundSince = HashMap<String, Long>()
            var totalMs = 0L
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val packageName = event.packageName ?: continue
                if (packageName in excluded) continue
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND ->
                        foregroundSince[packageName] = event.timeStamp
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val start = foregroundSince.remove(packageName)
                        if (start != null) {
                            totalMs += (event.timeStamp - start).coerceAtLeast(0)
                        }
                    }
                }
            }
            // Застосунок, що й досі на передньому плані на момент "to" (напр. "зараз"), ніколи
            // не отримає завершального MOVE_TO_BACKGROUND у вибірці — рахуємо його до "to".
            for (start in foregroundSince.values) {
                totalMs += (to - start).coerceAtLeast(0)
            }

            (totalMs / 60_000L).toInt()
        } catch (e: SecurityException) {
            0
        }
    }

    /**
     * Пакети поточного лаунчера (усіх, хто відповідає на CATEGORY_HOME — на випадок кількох
     * встановлених лаунчерів, не лише активного за замовчуванням) та ввімкнених клавіатур.
     * Викликається на кожен запит (не кешується): і лаунчер, і клавіатура можуть змінитись
     * протягом життя процесу, а сам запит — лише пара дешевих системних викликів.
     */
    private fun systemExclusionPackages(): Set<String> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launcherPackages = context.packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .toSet()

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val keyboardPackages = imm.enabledInputMethodList.map { it.packageName }.toSet()

        return launcherPackages + keyboardPackages
    }

    /** FR-3.9: Grace Period Buffer — знаменник ніколи не менший за 180 хв. */
    fun calculateDenominatorMinutes(dayStartMillis: Long): Int =
        maxOf(180, minutesSince(dayStartMillis))

    /** FR-3.7: скільки хвилин уже триває сьогоднішній день (для "Твій день триває X"). */
    fun calculateDayLengthMinutes(dayStartMillis: Long): Int =
        minutesSince(dayStartMillis).coerceAtLeast(0)

    /**
     * Повний діапазон шкали "Мій день" (за запитом користувача) — від точки старту дня
     * (пробудження) до найближчої півночі (00:00), НЕ лише до "зараз". "Твій день триває X"
     * (calculateDayLengthMinutes) лишається зростаючою величиною для заголовка — цей діапазон
     * лише для розрахунку часток сегментів шкали, щоб "Решта дня" сягала кінця шкали (півночі),
     * а не обривалась на "зараз".
     */
    fun calculateDaySpanMinutes(dayStartMillis: Long): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val nextMidnight = cal.timeInMillis
        return ((nextMidnight - dayStartMillis) / 60_000L).toInt().coerceAtLeast(1)
    }

    private fun minutesSince(millis: Long): Int =
        ((System.currentTimeMillis() - millis) / 60_000L).toInt()

    /** FR-3.3: локальна північ (узгоджується з відомим timezone-обмеженням, SRS розділ 11). */
    private fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * FR-3.2, FR-3.5: точка старту дня — перше "суттєве" розблокування (перший MOVE_TO_FOREGROUND
     * будь-якого застосунку, включно з лаунчером) після півночі. Сесія, що починається до
     * [sleepWindowEndHour] (дефолт 6, FR-3.2) і триває коротше 5 хв, ігнорується як нічна
     * перевірка годинника (узгоджено зі стейкхолдером — SRS текстом називав поріг 2-3 хв і
     * окрему межу 05:00; тут обидва об'єднані в один редагований параметр). Якщо сьогодні ще
     * не було жодного "суттєвого" розблокування (напр. щойно прокинулись) — день ще не почався,
     * повертаємо "зараз": знаменник і Online-хвилини тоді коректно виходять ~0.
     */
    suspend fun calculateDayStartMillis(sleepWindowEndHour: Int): Long = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val midnight = startOfTodayMillis()
        val now = System.currentTimeMillis()
        try {
            val events = usm.queryEvents(midnight, now)
            val event = UsageEvents.Event()
            var pendingStart: Long? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (pendingStart == null) pendingStart = event.timeStamp
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val start = pendingStart
                        pendingStart = null
                        if (start != null && isSignificantUnlock(start, event.timeStamp, sleepWindowEndHour)) {
                            return@withContext start
                        }
                    }
                }
            }
            // Останнє розблокування ще триває (нема завершального MOVE_TO_BACKGROUND у вибірці).
            val start = pendingStart
            if (start != null && isSignificantUnlock(start, now, sleepWindowEndHour)) {
                return@withContext start
            }
            now // ще жодного суттєвого розблокування сьогодні — день ще не почався
        } catch (e: SecurityException) {
            midnight
        }
    }

    private fun isSignificantUnlock(startMillis: Long, endMillis: Long, sleepWindowEndHour: Int): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = startMillis }.get(Calendar.HOUR_OF_DAY)
        if (hour >= sleepWindowEndHour) return true
        val durationMinutes = (endMillis - startMillis) / 60_000L
        return durationMinutes >= 5
    }
}

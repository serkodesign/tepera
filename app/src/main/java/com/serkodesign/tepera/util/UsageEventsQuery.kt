package com.serkodesign.tepera.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "UsageEventsQuery"

/** Скільки разів [queryEventsOrNull] не вдалося отримати події навіть після повтору (за весь час процесу). */
private val failureCount = AtomicInteger()

/**
 * [UsageStatsManager.queryEvents], що переживає збій передачі через binder.
 *
 * Системний сервіс віддає події одним Parcelable-списком. Якщо сервіс у цей момент перевантажений або
 * перезапускається, передача обривається: `BadParcelableException: Failure retrieving array`, спричинене
 * `DeadObjectException` ("Transaction failed on small parcel"). Це не `SecurityException`, тож раніше виняток
 * падав прямо в корутину і валив застосунок — відтворено двічі на Samsung S23 (12.10.2026,
 * `BalanceRepository.calculateDayStartMillis`). Збій зазвичай разовий, тому запит повторюється один раз; якщо й
 * повтор не вдався — повертаємо `null`, і викликач підставляє той самий запасний результат, що й "нема доступу"
 * (порожньо/нуль), до наступного оновлення екрана.
 *
 * Такий запасний результат НЕ МОЖНА зберігати в базу (він був би назавжди хибним фактом) — код, що пише
 * пораховане з подій, обгортається в [withReliableUsageEvents].
 *
 * `SecurityException` (доступ до статистики відкликано) навмисно НЕ перехоплюється — у викликачів для нього вже
 * є власна обробка.
 */
fun UsageStatsManager.queryEventsOrNull(beginTime: Long, endTime: Long): UsageEvents? {
    repeat(2) { attempt ->
        try {
            return queryEvents(beginTime, endTime)
        } catch (e: SecurityException) {
            throw e
        } catch (e: RuntimeException) {
            Log.w(TAG, "queryEvents failed (attempt ${attempt + 1})", e)
        }
    }
    failureCount.incrementAndGet()
    return null
}

/**
 * Виконує [block] і повертає його результат, або `null`, якщо під час виконання хоч один запит подій не вдався
 * (тоді результат міг бути порахований із запасних нулів). Для коду, що ЗБЕРІГАЄ пораховане: при `null` запис
 * просто пропускається й повториться при наступному оновленні.
 *
 * Лічильник спільний для процесу, тож збій у паралельній корутині теж дасть `null` — це лише зайвий пропуск
 * запису, а не хибні дані.
 */
suspend fun <T> withReliableUsageEvents(block: suspend () -> T): T? {
    val before = failureCount.get()
    val result = block()
    return if (failureCount.get() == before) result else null
}

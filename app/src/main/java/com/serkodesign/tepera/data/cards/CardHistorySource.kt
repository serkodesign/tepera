package com.serkodesign.tepera.data.cards

/**
 * Межа, яку [CardEngine] бачить замість справжнього Room/DataStore — дає змогу юніт-тестувати
 * рушій (T-13, приймання: "юніт-тести на симуляції 60 днів") на чистому JVM, без Android-рантайму,
 * через просту фейкову реалізацію в тестах. `CardHistoryRepository` — єдина продакшн-реалізація.
 */
interface CardHistorySource {
    /** Момент останнього [CardResult.ANSWERED]/[CardResult.SKIPPED] запису для цього типу, якщо є. */
    suspend fun lastResolvedAt(type: CardType): Long?

    /** Чи показувалась БУДЬ-ЯКА картка-оцінка ([CardType.isEstimate]) з моменту [sinceMillis]. */
    suspend fun estimateShownSince(sinceMillis: Long): Boolean

    /** Скільки разів поспіль подієва картка витіснила тижневу — для правила "не більше двічі поспіль". */
    suspend fun eventDisplacementStreak(): Int
    suspend fun setEventDisplacementStreak(value: Int)
}

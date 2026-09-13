package com.serkodesign.tepera.data.cards

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val WEEK_MILLIS = 7 * DAY_MILLIS

/** T-13, розділ "Глобальний бюджет" — ⚠ звірено з чинним рішенням (FR-D.10): максимум 3 картки. */
const val MAX_SIMULTANEOUS_CARDS = 3

/** Скільки разів поспіль подієва картка сміє витіснити тижневу, перш ніж рушій форсує тижневу. */
private const val MAX_EVENT_DISPLACEMENTS_IN_A_ROW = 2

/**
 * T-13 (tepera-dev-spec.md) — "рушій карток", рівень 2 "глобальний бюджет". Чиста функція від
 * [CardSource] + [CardHistorySource] до множини видимих типів — жодної залежності від Android чи
 * конкретних карток, тому й тестується без Room/Compose (`CardEngineTest`, симуляція 60 днів).
 *
 * Порядок застосування правил відтворює список документа буквально:
 * 1. Готовність даних ([CardSource.dataReady]) — фільтр джерела, рушій лише читає прапорець.
 * 2. Мінімальний інтервал повтору конкретного типу ([CardSource.minIntervalDays]), рахований від
 *    останнього [CardResult.ANSWERED]/[CardResult.SKIPPED] — "після пропуску картка йде в кінець
 *    черги, не повторюється наступного дня" виконується автоматично: [CardHistorySource.
 *    lastResolvedAt] оновлюється і при пропуску, і при відповіді, тож обидва однаково зсувають
 *    наступну появу на весь [CardSource.minIntervalDays], не лише на добу.
 * 3. Глобальний бюджет "не більше 1 картки-оцінки на тиждень, сумарно по всіх типах" — і по всій
 *    щойно зібраній історії ([CardHistorySource.estimateShownSince]), і всередині цього самого
 *    виклику (дві картки-оцінки, готові одночасно, — вище пріоритетом виграє, друга чекає свого
 *    тижня, не "займає" другий слот сьогодні ж).
 * 4. [MAX_SIMULTANEOUS_CARDS] і правило "подієві не витісняють тижневі більш ніж двічі поспіль" —
 *    рахунок витіснень зберігається в [CardHistorySource.eventDisplacementStreak] МІЖ викликами
 *    (не лише в межах одного дня): на третє поспіль реальне витіснення рушій форсує включення
 *    витісненої тижневої картки, за потреби прибираючи найнижчу пріоритетом з уже відібраних.
 */
class CardEngine(private val history: CardHistorySource) {

    suspend fun selectVisible(sources: List<CardSource>, now: Long = System.currentTimeMillis()): Set<CardType> {
        val ready = sources.filter { it.dataReady }.sortedBy { it.priority }
        val estimateBudgetUsedThisWeek = history.estimateShownSince(now - WEEK_MILLIS)

        val eligible = mutableListOf<CardSource>()
        var estimatePickedThisPass = false
        for (source in ready) {
            if (source.type.isEstimate) {
                if (estimateBudgetUsedThisWeek || estimatePickedThisPass) continue
            }
            if (source.minIntervalDays != null) {
                val last = history.lastResolvedAt(source.type)
                if (last != null && now - last < source.minIntervalDays * DAY_MILLIS) continue
            }
            eligible += source
            if (source.type.isEstimate) estimatePickedThisPass = true
        }

        return applySimultaneousLimitAndDisplacement(eligible)
    }

    private suspend fun applySimultaneousLimitAndDisplacement(eligible: List<CardSource>): Set<CardType> {
        if (eligible.size <= MAX_SIMULTANEOUS_CARDS) {
            history.setEventDisplacementStreak(0)
            return eligible.map { it.type }.toSet()
        }

        val kept = eligible.take(MAX_SIMULTANEOUS_CARDS)
        val dropped = eligible.drop(MAX_SIMULTANEOUS_CARDS)
        // "Тижневі" в сенсі цього правила — ті самі картки-оцінки (WeeklyReflection/UnlockEstimate/
        // LastPhoneUseEstimate), єдині в системі з природним тижневим/двотижневим ритмом.
        val droppedWeekly = dropped.filter { it.type.isEstimate }
        val eventDisplaces = kept.any { it.type.isEvent } && droppedWeekly.isNotEmpty()

        if (!eventDisplaces) {
            history.setEventDisplacementStreak(0)
            return kept.map { it.type }.toSet()
        }

        val streak = history.eventDisplacementStreak()
        if (streak < MAX_EVENT_DISPLACEMENTS_IN_A_ROW) {
            history.setEventDisplacementStreak(streak + 1)
            return kept.map { it.type }.toSet()
        }

        // Третє поспіль витіснення — форсуємо найпріоритетнішу витіснену тижневу картку, за
        // потреби прибираючи найнижчу пріоритетом з уже відібраних, АЛЕ НІКОЛИ саму подієву —
        // інваріант FR-D.3 "зараз або ніколи" для пауз лишається сильнішим за це правило рахунку.
        // Просте "об'єднати й відсортувати" тут не працює: forced має найнижчий пріоритет серед
        // усіх претендентів (інакше не був би dropped узагалі), тож звичайний take(N) після
        // сортування знову відкинув би саме його — точку явно прибираємо конкурента вручну.
        val evictionCandidates = kept.filterNot { it.type.isEvent }
        history.setEventDisplacementStreak(0)
        if (evictionCandidates.isEmpty()) {
            return kept.map { it.type }.toSet()
        }
        val forced = droppedWeekly.minBy { it.priority }
        val toEvict = evictionCandidates.maxBy { it.priority }
        val newKept = (kept - toEvict) + forced
        return newKept.map { it.type }.toSet()
    }
}

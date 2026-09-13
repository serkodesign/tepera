package com.serkodesign.tepera.data.cards

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/** Проста in-memory реалізація [CardHistorySource] — рушій тестується без Room/DataStore/Android. */
private class FakeCardHistorySource : CardHistorySource {
    private val resolvedAt = mutableMapOf<CardType, Long>()
    private val shownAt = mutableListOf<Pair<CardType, Long>>()
    private var streak = 0

    fun recordShown(type: CardType, at: Long) {
        shownAt += type to at
    }

    fun recordResolved(type: CardType, at: Long) {
        resolvedAt[type] = at
    }

    override suspend fun lastResolvedAt(type: CardType): Long? = resolvedAt[type]

    override suspend fun estimateShownSince(sinceMillis: Long): Boolean =
        shownAt.any { (type, at) -> type.isEstimate && at > sinceMillis }

    override suspend fun eventDisplacementStreak(): Int = streak

    override suspend fun setEventDisplacementStreak(value: Int) {
        streak = value
    }
}

/** T-13 (tepera-dev-spec.md), "рушій карток" — юніт-тести рівня 2 (глобальний бюджет). */
class CardEngineTest {

    @Test
    fun `empty queue when nothing is data-ready is empty state, not filler`() = runBlocking {
        val engine = CardEngine(FakeCardHistorySource())
        val sources = listOf(
            CardSource(CardType.PATTERN, priority = 6, minIntervalDays = null, dataReady = false)
        )
        assertTrue(engine.selectVisible(sources, now = 0L).isEmpty())
    }

    @Test
    fun `at most one estimate card even when two are ready the same day`() = runBlocking {
        val engine = CardEngine(FakeCardHistorySource())
        val now = 100 * DAY_MILLIS
        val sources = listOf(
            CardSource(CardType.WEEKLY_REFLECTION, priority = 2, minIntervalDays = 7, dataReady = true),
            CardSource(CardType.UNLOCK_ESTIMATE, priority = 3, minIntervalDays = 14, dataReady = true),
            CardSource(CardType.LAST_PHONE_USE_ESTIMATE, priority = 4, minIntervalDays = 14, dataReady = true)
        )
        val visible = engine.selectVisible(sources, now)
        assertEquals(1, visible.count { it.isEstimate })
        // Пріоритет вирішує, яка саме — найвищий пріоритет (найменше число) виграє.
        assertEquals(setOf(CardType.WEEKLY_REFLECTION), visible)
    }

    @Test
    fun `resolved estimate card is not shown again before its own minimum interval`() = runBlocking {
        val history = FakeCardHistorySource()
        val engine = CardEngine(history)
        val source = CardSource(CardType.WEEKLY_REFLECTION, priority = 2, minIntervalDays = 7, dataReady = true)

        val day0 = 0L
        assertEquals(setOf(CardType.WEEKLY_REFLECTION), engine.selectVisible(listOf(source), day0))
        history.recordShown(CardType.WEEKLY_REFLECTION, day0)
        history.recordResolved(CardType.WEEKLY_REFLECTION, day0)

        val day3 = 3 * DAY_MILLIS
        assertTrue(engine.selectVisible(listOf(source), day3).isEmpty())

        val day7 = 7 * DAY_MILLIS
        assertEquals(setOf(CardType.WEEKLY_REFLECTION), engine.selectVisible(listOf(source), day7))
    }

    @Test
    fun `event card displacing a weekly card three times in a row gets overridden on the third`() = runBlocking {
        val history = FakeCardHistorySource()
        val engine = CardEngine(history)
        // Заповнюємо решту 2 слоти чимось незмінним, щоб pause+weekly змагались за останній слот.
        val filler1 = CardSource(CardType.ONLINE_ESTIMATE_REVEAL, priority = 0, minIntervalDays = null, dataReady = true)
        val filler2 = CardSource(CardType.WEEKLY_DIGEST, priority = 1, minIntervalDays = null, dataReady = true)
        val pause = CardSource(CardType.PAUSE, priority = 2, minIntervalDays = null, dataReady = true)
        val weekly = CardSource(CardType.WEEKLY_REFLECTION, priority = 3, minIntervalDays = null, dataReady = true)
        val sources = listOf(filler1, filler2, pause, weekly)

        val visible1 = engine.selectVisible(sources, now = 0L)
        assertTrue(CardType.PAUSE in visible1 && CardType.WEEKLY_REFLECTION !in visible1)
        assertEquals(1, history.eventDisplacementStreak())

        val visible2 = engine.selectVisible(sources, now = DAY_MILLIS)
        assertTrue(CardType.PAUSE in visible2 && CardType.WEEKLY_REFLECTION !in visible2)
        assertEquals(2, history.eventDisplacementStreak())

        // Третє поспіль — тижнева картка форсується, найнижчий пріоритет (pause) прибирається.
        val visible3 = engine.selectVisible(sources, now = 2 * DAY_MILLIS)
        assertTrue(CardType.WEEKLY_REFLECTION in visible3)
        assertEquals(0, history.eventDisplacementStreak())
    }

    /**
     * Приймання T-13: "юніт-тести на симуляції 60 днів: жодного дня з двома оцінками, жодного
     * тижня без картки за наявності даних". Дані для всіх трьох карток-оцінок готові щодня з
     * першого дня симуляції (найгірший випадок для перевірки бюджету — весь час є що показати),
     * кожна показана картка одразу "відповідається" тим самим днем (найшвидший можливий цикл
     * повтору — перевіряє, що інтервал і бюджет усе одно не дають показати частіше дозволеного).
     */
    @Test
    fun `60-day simulation never shows two estimate cards same day and never skips a week`() = runBlocking {
        val history = FakeCardHistorySource()
        val engine = CardEngine(history)

        val estimateShownDays = mutableListOf<Int>()
        val shownCountByDay = mutableMapOf<Int, Int>()

        for (day in 0 until 60) {
            val now = day * DAY_MILLIS + DAY_MILLIS / 2 // середина доби, як реальний "зараз"
            val sources = listOf(
                CardSource(CardType.ONLINE_ESTIMATE_REVEAL, priority = 0, minIntervalDays = null, dataReady = false),
                CardSource(CardType.PAUSE, priority = 1, minIntervalDays = null, dataReady = day % 3 == 0),
                CardSource(CardType.WEEKLY_REFLECTION, priority = 2, minIntervalDays = 7, dataReady = true),
                CardSource(CardType.UNLOCK_ESTIMATE, priority = 3, minIntervalDays = 14, dataReady = true),
                CardSource(CardType.LAST_PHONE_USE_ESTIMATE, priority = 4, minIntervalDays = 14, dataReady = true),
                CardSource(CardType.WEEKLY_DIGEST, priority = 5, minIntervalDays = null, dataReady = true),
                CardSource(CardType.PATTERN, priority = 6, minIntervalDays = null, dataReady = true)
            )

            val visible = engine.selectVisible(sources, now)
            val estimatesShownToday = visible.count { it.isEstimate }
            assertTrue("Day $day showed $estimatesShownToday estimate cards", estimatesShownToday <= 1)
            shownCountByDay[day] = estimatesShownToday

            visible.forEach { type ->
                history.recordShown(type, now)
                if (type.isEstimate) {
                    history.recordResolved(type, now)
                    estimateShownDays += day
                }
            }
        }

        // Жодного дня з двома оцінками — вже перевірено вище на кожній ітерації.
        // Жодного тижня без картки-оцінки за наявності даних (дані готові щодня від початку):
        for (weekStart in 0 until 60 step 7) {
            val weekEnd = minOf(weekStart + 7, 60)
            val shownThisWeek = estimateShownDays.any { it in weekStart until weekEnd }
            assertTrue("Week starting day $weekStart had no estimate card shown", shownThisWeek)
        }
    }
}

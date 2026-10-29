package com.serkodesign.tepera.domain.risk

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPuzzlesTest {

    @Test
    fun puzzlesStartAtThirdReentryAndCapAtTwo() {
        assertEquals(0, QuickPuzzles.puzzlesRequired(1, enabled = true))
        assertEquals(0, QuickPuzzles.puzzlesRequired(2, enabled = true))
        assertEquals(1, QuickPuzzles.puzzlesRequired(3, enabled = true))
        assertEquals(1, QuickPuzzles.puzzlesRequired(5, enabled = true))
        assertEquals(2, QuickPuzzles.puzzlesRequired(6, enabled = true))
        assertEquals(2, QuickPuzzles.puzzlesRequired(40, enabled = true))
    }

    @Test
    fun disabledMeansNoPuzzles() {
        assertEquals(0, QuickPuzzles.puzzlesRequired(10, enabled = false))
    }

    @Test
    fun pickKindsAreDistinctAndSkipLast() {
        repeat(200) { seed ->
            val last = QuickPuzzles.Kind.entries[seed % QuickPuzzles.Kind.entries.size]
            val kinds = QuickPuzzles.pickKinds(2, last, Random(seed))
            assertEquals(2, kinds.size)
            assertEquals(2, kinds.toSet().size)
            assertNotEquals(last, kinds.first())
        }
    }

    @Test
    fun choiceOptionsAreFourDistinctPositiveAndContainAnswer() {
        repeat(500) { seed ->
            val arithmetic = QuickPuzzles.generate(QuickPuzzles.Kind.ARITHMETIC, Random(seed)) as QuickPuzzles.Arithmetic
            assertValidOptions(arithmetic.options, arithmetic.answer)
            val sequence = QuickPuzzles.generate(QuickPuzzles.Kind.SEQUENCE, Random(seed)) as QuickPuzzles.Sequence
            assertValidOptions(sequence.options, sequence.answer)
            assertEquals(4, sequence.shown.size)
        }
    }

    @Test
    fun arithmeticAnswerMatchesExpression() {
        repeat(300) { seed ->
            val p = QuickPuzzles.generate(QuickPuzzles.Kind.ARITHMETIC, Random(seed)) as QuickPuzzles.Arithmetic
            assertEquals(evaluate(p.expression), p.answer)
        }
    }

    @Test
    fun schulteHasAllNumbersOnce() {
        repeat(100) { seed ->
            val p = QuickPuzzles.generate(QuickPuzzles.Kind.SCHULTE, Random(seed)) as QuickPuzzles.Schulte
            assertEquals((1..9).toList(), p.cells.sorted())
            assertEquals(9, p.order.size)
        }
    }

    @Test
    fun sudokuHasExactlyOneSolutionAndMatchesIt() {
        repeat(150) { seed ->
            val p = QuickPuzzles.generate(QuickPuzzles.Kind.SUDOKU, Random(seed)) as QuickPuzzles.Sudoku
            assertTrue("valid grid", isValidSolution(p.solution))
            assertEquals(1, QuickPuzzles.sudokuSolutions(p.cells, limit = 2))
            assertEquals(5, p.cells.count { it == 0 })
            p.cells.forEachIndexed { i, v -> if (v != 0) assertEquals(p.solution[i], v) }
        }
    }

    @Test
    fun levelIsEasyOnlyAfterStayingAway() {
        assertEquals(QuickPuzzles.Level.NORMAL, QuickPuzzles.levelFor(null))
        assertEquals(QuickPuzzles.Level.NORMAL, QuickPuzzles.levelFor(0))
        assertEquals(QuickPuzzles.Level.NORMAL, QuickPuzzles.levelFor(QuickPuzzles.EASY_AFTER_AWAY_MINUTES - 1L))
        assertEquals(QuickPuzzles.Level.EASY, QuickPuzzles.levelFor(QuickPuzzles.EASY_AFTER_AWAY_MINUTES.toLong()))
        assertEquals(QuickPuzzles.Level.EASY, QuickPuzzles.levelFor(120))
    }

    @Test
    fun easyPuzzlesAreValidAndSimpler() {
        val easy = QuickPuzzles.Level.EASY
        repeat(300) { seed ->
            val arithmetic = QuickPuzzles.generate(QuickPuzzles.Kind.ARITHMETIC, Random(seed), easy) as QuickPuzzles.Arithmetic
            assertValidOptions(arithmetic.options, arithmetic.answer)
            assertEquals(evaluate(arithmetic.expression), arithmetic.answer)
            assertTrue(arithmetic.answer < 70)

            val sequence = QuickPuzzles.generate(QuickPuzzles.Kind.SEQUENCE, Random(seed), easy) as QuickPuzzles.Sequence
            assertValidOptions(sequence.options, sequence.answer)

            val schulte = QuickPuzzles.generate(QuickPuzzles.Kind.SCHULTE, Random(seed), easy) as QuickPuzzles.Schulte
            assertEquals(false, schulte.descending)

            val memory = QuickPuzzles.generate(QuickPuzzles.Kind.DIGIT_MEMORY, Random(seed), easy) as QuickPuzzles.DigitMemory
            assertEquals(3, memory.digits.size)

            val sudoku = QuickPuzzles.generate(QuickPuzzles.Kind.SUDOKU, Random(seed), easy) as QuickPuzzles.Sudoku
            assertEquals(1, QuickPuzzles.sudokuSolutions(sudoku.cells, limit = 2))
            assertEquals(3, sudoku.cells.count { it == 0 })
        }
    }

    private fun assertValidOptions(options: List<Int>, answer: Int) {
        assertEquals(4, options.size)
        assertEquals(4, options.toSet().size)
        assertTrue(options.all { it > 0 })
        assertTrue(answer in options)
    }

    private fun isValidSolution(g: List<Int>): Boolean {
        val full = setOf(1, 2, 3, 4)
        val rows = (0..3).all { r -> (0..3).map { c -> g[r * 4 + c] }.toSet() == full }
        val cols = (0..3).all { c -> (0..3).map { r -> g[r * 4 + c] }.toSet() == full }
        val boxes = (0..3).all { b ->
            val r0 = b / 2 * 2
            val c0 = b % 2 * 2
            listOf(0, 1).flatMap { dr -> listOf(0, 1).map { dc -> g[(r0 + dr) * 4 + c0 + dc] } }.toSet() == full
        }
        return rows && cols && boxes
    }

    /** Обчислює вирази виду «a + b», «a − b», «a × b», «a × b + c» (× старший за + і −). */
    private fun evaluate(expr: String): Int {
        val t = expr.split(" ")
        var acc = t[0].toInt()
        var i = 1
        if (t.size > 1 && t[1] == "×") {
            acc *= t[2].toInt()
            i = 3
        }
        while (i < t.size) {
            val v = t[i + 1].toInt()
            acc = when (t[i]) { "+" -> acc + v; "−" -> acc - v; else -> error("op ${t[i]}") }
            i += 2
        }
        return acc
    }
}

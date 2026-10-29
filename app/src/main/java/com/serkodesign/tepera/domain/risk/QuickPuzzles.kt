package com.serkodesign.tepera.domain.risk

import kotlin.random.Random

/**
 * v3.0 MVP (beta): короткі задачки (10–30 с) на екрані «я за тобою слідкую», щоб повторне відкриття стрічки не стало
 * автоматичною дією «ще 5 хвилин». Чиста логіка без Android: правила появи й генератори, тестуються без пристрою.
 *
 * Принципи: задачка стоїть лише на шляху «Відкрити» (вихід завжди вільний); тип щоразу випадковий і не повторює попередній,
 * вміст випадковий (м'язова пам'ять не формується); помилка нічого не рахує й не червоніє — просто нова задача.
 */
object QuickPuzzles {
    /** З якого за рахунком показу екрана «я за тобою слідкую» (для одних воріт, у межах однієї обіцянки) починаються задачки. */
    const val FIRST_PUZZLE_REENTRY = 3

    /** Стеля ескалації: з цього показу — дві задачки, більше ніколи (інакше це покарання). */
    const val DOUBLE_FROM_REENTRY = 6

    fun puzzlesRequired(reentryNumber: Int, enabled: Boolean): Int = when {
        !enabled || reentryNumber < FIRST_PUZZLE_REENTRY -> 0
        reentryNumber < DOUBLE_FROM_REENTRY -> 1
        else -> 2
    }

    enum class Kind { ARITHMETIC, SCHULTE, DIGIT_MEMORY, SEQUENCE, SUDOKU, INTENTION }

    sealed interface Puzzle
    data class Arithmetic(val expression: String, val options: List<Int>, val answer: Int) : Puzzle
    data class Sequence(val shown: List<Int>, val options: List<Int>, val answer: Int) : Puzzle

    /** Сітка 3×3 з перемішаними числами 1–9; торкатись треба по порядку (за зростанням або за спаданням). */
    data class Schulte(val cells: List<Int>, val descending: Boolean) : Puzzle {
        val order: List<Int> get() = if (descending) (9 downTo 1).toList() else (1..9).toList()
    }

    data class DigitMemory(val digits: List<Int>) : Puzzle

    /** Судоку 4×4: [cells] — 16 клітинок, 0 = порожньо; [solution] — єдиний розв'язок. */
    data class Sudoku(val cells: List<Int>, val solution: List<Int>) : Puzzle

    /** Не задача, а питання «для чого відкриваєш?» — теж розриває автопілот, але без відчуття іспиту. Відповідь ніде не зберігається. */
    data object Intention : Puzzle

    /** [count] різних типів; перший не збігається з [last] (тим, що був минулого разу). */
    fun pickKinds(count: Int, last: Kind?, random: Random = Random): List<Kind> =
        Kind.entries.filter { it != last }.shuffled(random).take(count)

    fun generate(kind: Kind, random: Random = Random): Puzzle = when (kind) {
        Kind.ARITHMETIC -> arithmetic(random)
        Kind.SCHULTE -> Schulte((1..9).shuffled(random), descending = random.nextBoolean())
        Kind.DIGIT_MEMORY -> DigitMemory(List(4) { random.nextInt(10) })
        Kind.SEQUENCE -> sequence(random)
        Kind.SUDOKU -> sudoku(random)
        Kind.INTENTION -> Intention
    }

    private fun arithmetic(r: Random): Arithmetic {
        val (text, answer) = when (r.nextInt(4)) {
            0 -> r.nextInt(12, 60).let { a -> r.nextInt(11, 40).let { b -> "$a + $b" to a + b } }
            1 -> r.nextInt(40, 99).let { a -> r.nextInt(11, a - 9).let { b -> "$a − $b" to a - b } }
            2 -> r.nextInt(3, 10).let { a -> r.nextInt(6, 13).let { b -> "$a × $b" to a * b } }
            else -> r.nextInt(3, 10).let { a -> r.nextInt(4, 10).let { b -> r.nextInt(2, 20).let { c -> "$a × $b + $c" to a * b + c } } }
        }
        return Arithmetic(text, options(answer, listOf(1, 2, 10), r), answer)
    }

    private fun sequence(r: Random): Sequence {
        val terms: List<Int>
        val step: Int
        when (r.nextInt(3)) {
            0 -> { // арифметична прогресія
                step = r.nextInt(2, 10)
                val start = r.nextInt(1, 21)
                terms = List(5) { start + it * step }
            }
            1 -> { // подвоєння
                step = 2
                val start = r.nextInt(1, 6)
                terms = List(5) { start shl it }
            }
            else -> { // різниці ростуть: +d, +d+1, +d+2…
                step = r.nextInt(1, 4)
                val start = r.nextInt(1, 10)
                terms = (0 until 4).runningFold(start) { acc, i -> acc + step + i }
            }
        }
        val answer = terms.last()
        return Sequence(terms.dropLast(1), options(answer, listOf(1, 2, step), r), answer)
    }

    /** Відповідь + 3 різні правдоподібні неправильні (завжди додатні), у випадковому порядку. */
    private fun options(answer: Int, offsets: List<Int>, r: Random): List<Int> {
        val wrong = offsets.flatMap { listOf(answer + it, answer - it) }.distinct().filter { it > 0 && it != answer }
        val extra = (1..6).flatMap { listOf(answer + it, answer - it) }.filter { it > 0 && it != answer }
        return ((wrong.shuffled(r) + extra.shuffled(r)).distinct().take(3) + answer).shuffled(r)
    }

    // ---------- Судоку 4×4 ----------

    private const val SUDOKU_BLANKS = 5

    private fun sudoku(r: Random): Sudoku {
        val perm = (1..4).shuffled(r)
        val rows = listOf(0, 1).shuffled(r).flatMap { b -> listOf(0, 1).shuffled(r).map { b * 2 + it } }
        val cols = listOf(0, 1).shuffled(r).flatMap { b -> listOf(0, 1).shuffled(r).map { b * 2 + it } }
        val solution = List(16) { i ->
            val row = rows[i / 4]
            val col = cols[i % 4]
            perm[(2 * (row % 2) + row / 2 + col) % 4 ]
        }
        val cells = solution.toMutableList()
        var blanks = 0
        for (i in (0 until 16).shuffled(r)) {
            if (blanks == SUDOKU_BLANKS) break
            val keep = cells[i]
            cells[i] = 0
            if (sudokuSolutions(cells, limit = 2) == 1) blanks++ else cells[i] = keep
        }
        return Sudoku(cells.toList(), solution)
    }

    /** Скільки розв'язків має сітка (рахуємо не більше [limit]). */
    internal fun sudokuSolutions(cells: List<Int>, limit: Int): Int {
        val grid = cells.toIntArray()
        fun fits(i: Int, d: Int): Boolean {
            val row = i / 4
            val col = i % 4
            for (k in 0 until 4) {
                if (grid[row * 4 + k] == d || grid[k * 4 + col] == d) return false
            }
            val r0 = row / 2 * 2
            val c0 = col / 2 * 2
            for (dr in 0..1) for (dc in 0..1) if (grid[(r0 + dr) * 4 + c0 + dc] == d) return false
            return true
        }
        fun count(): Int {
            val i = grid.indexOf(0)
            if (i < 0) return 1
            var found = 0
            for (d in 1..4) {
                if (!fits(i, d)) continue
                grid[i] = d
                found += count()
                grid[i] = 0
                if (found >= limit) break
            }
            return found
        }
        return count()
    }
}

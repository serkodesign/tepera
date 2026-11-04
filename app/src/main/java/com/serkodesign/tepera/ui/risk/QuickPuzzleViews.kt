package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.domain.risk.QuickPuzzles
import com.serkodesign.tepera.ui.theme.M3Button
import com.serkodesign.tepera.ui.theme.M3ButtonSize
import com.serkodesign.tepera.ui.theme.M3ButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaSpacing
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.util.PlanOverlayStore


/**
 * Крок «задачка» на темному листі плану: [total] задач підряд (кожна випадкового типу, без повтору типу минулого разу).
 * «Гаразд, іду» завжди видно й воно головна кнопка — вийти простіше, ніж розв'язувати. Помилка тихо дає нову задачу того ж типу.
 */
@Composable
fun PuzzleStep(total: Int, level: QuickPuzzles.Level, onAllSolved: () -> Unit, onLeave: () -> Unit) {
    val context = LocalContext.current
    val kinds = remember { QuickPuzzles.pickKinds(total, PlanOverlayStore.lastPuzzleKind(context)) }
    LaunchedEffect(Unit) { kinds.lastOrNull()?.let { PlanOverlayStore.setLastPuzzleKind(context, it) } }
    var index by remember { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(0) }
    val puzzle = remember(index, attempt) { QuickPuzzles.generate(kinds[index], level = level) }

    Column(verticalArrangement = Arrangement.spacedBy(TeperaSpacing.xs), modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.v3_puzzle_title), color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }
        )
        Text(
            if (total > 1) stringResource(R.string.v3_puzzle_hint_steps, index + 1, kinds.size) else stringResource(R.string.v3_puzzle_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
        )
    }
    key(index, attempt) {
        PuzzleBody(
            puzzle = puzzle,
            onSolved = { if (index + 1 < kinds.size) index++ else onAllSolved() },
            onWrong = { attempt++ }
        )
    }
    M3Button(M3ButtonType.Filled, M3ButtonSize.M, stringResource(R.string.v3_watch_leave), onLeave, Modifier.fillMaxWidth())
}

@Composable
private fun PuzzleBody(puzzle: QuickPuzzles.Puzzle, onSolved: () -> Unit, onWrong: () -> Unit) {
    when (puzzle) {
        is QuickPuzzles.Arithmetic -> ChoiceBody(
            prompt = stringResource(R.string.v3_puzzle_arith), shown = puzzle.expression + " = ?",
            options = puzzle.options, answer = puzzle.answer, onSolved = onSolved, onWrong = onWrong
        )
        is QuickPuzzles.Sequence -> ChoiceBody(
            prompt = stringResource(R.string.v3_puzzle_seq), shown = puzzle.shown.joinToString("  ") + "  ?",
            options = puzzle.options, answer = puzzle.answer, onSolved = onSolved, onWrong = onWrong
        )
        is QuickPuzzles.Schulte -> SchulteBody(puzzle, onSolved)
        is QuickPuzzles.DigitMemory -> MemoryBody(puzzle, onSolved, onWrong)
        is QuickPuzzles.Sudoku -> SudokuBody(puzzle, onSolved, onWrong)
        QuickPuzzles.Intention -> IntentionBody(onSolved)
    }
}

@Composable
private fun ChoiceBody(prompt: String, shown: String, options: List<Int>, answer: Int, onSolved: () -> Unit, onWrong: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(prompt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
        Text(shown, color = MaterialTheme.colorScheme.onSurface, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { value ->
                    TeperaButton(
                        text = value.toString(),
                        onClick = { if (value == answer) onSolved() else onWrong() },
                        size = TeperaButtonSize.Medium,
                        type = TeperaButtonType.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SchulteBody(puzzle: QuickPuzzles.Schulte, onSolved: () -> Unit) {
    val order = puzzle.order
    var next by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(if (puzzle.descending) R.string.v3_puzzle_schulte_desc else R.string.v3_puzzle_schulte_asc),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            puzzle.cells.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { value ->
                        val done = order.indexOf(value) < next
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                                .clickable(role = Role.Button) {
                                    // Хибний дотик мовчки ігнорується: штрафу й червоного немає.
                                    if (!done && value == order[next]) {
                                        next++
                                        if (next == order.size) onSolved()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Medium,
                                color = if (done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryBody(puzzle: QuickPuzzles.DigitMemory, onSolved: () -> Unit, onWrong: () -> Unit) {
    var hidden by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    val expected = puzzle.digits.joinToString("")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(if (hidden) R.string.v3_puzzle_memory_enter else R.string.v3_puzzle_memory_look),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp
        )
        val shown = if (hidden) typed.padEnd(expected.length, '_') else expected
        Text(
            shown.toList().joinToString("  "), color = MaterialTheme.colorScheme.onSurface, fontSize = 40.sp, lineHeight = 48.sp,
            fontWeight = FontWeight.Medium
        )
        if (!hidden) {
            // Цифри не зникають самі: відлік починає людина, коли запам'ятала (раніше єдина кнопка на екрані була «Гаразд, іду»).
            TeperaButton(
                text = stringResource(R.string.v3_puzzle_memory_ready),
                onClick = { hidden = true },
                size = TeperaButtonSize.Medium,
                type = TeperaButtonType.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("←", "0", "")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { label ->
                        if (label.isEmpty()) {
                            Box(Modifier.weight(1f))
                        } else {
                            TeperaButton(
                                text = label,
                                onClick = {
                                    if (label == "←") typed = typed.dropLast(1) else {
                                        typed += label
                                        if (typed.length == expected.length) {
                                            if (typed == expected) onSolved() else onWrong()
                                        }
                                    }
                                },
                                size = TeperaButtonSize.Medium,
                                type = TeperaButtonType.Secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuBody(puzzle: QuickPuzzles.Sudoku, onSolved: () -> Unit, onWrong: () -> Unit) {
    val cells = remember { mutableStateListOf<Int>().apply { addAll(puzzle.cells) } }
    var selected by remember { mutableIntStateOf(puzzle.cells.indexOf(0)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.v3_puzzle_sudoku), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center
        )
        Column {
            for (r in 0..3) {
                Row(modifier = Modifier.padding(bottom = if (r == 1) 8.dp else 2.dp)) {
                    for (c in 0..3) {
                        val i = r * 4 + c
                        val given = puzzle.cells[i] != 0
                        val isSelected = !given && i == selected
                        Box(
                            modifier = Modifier
                                .padding(end = if (c == 1) 8.dp else 2.dp)
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (given) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(10.dp)) else Modifier)
                                .clickable(enabled = !given, role = Role.Button) { selected = i },
                            contentAlignment = Alignment.Center
                        ) {
                            if (cells[i] != 0) {
                                Text(
                                    cells[i].toString(), fontSize = 26.sp, fontWeight = FontWeight.Medium,
                                    color = if (given) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (1..4).forEach { digit ->
                TeperaButton(
                    text = digit.toString(),
                    onClick = {
                        if (selected < 0 || puzzle.cells[selected] != 0) return@TeperaButton
                        cells[selected] = digit
                        if (cells.none { it == 0 }) {
                            if (cells.toList() == puzzle.solution) onSolved() else onWrong()
                        } else {
                            selected = puzzle.cells.indices.firstOrNull { puzzle.cells[it] == 0 && cells[it] == 0 } ?: selected
                        }
                    },
                    size = TeperaButtonSize.Medium,
                    type = TeperaButtonType.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IntentionBody(onSolved: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.v3_puzzle_intent_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
        listOf(R.string.v3_puzzle_intent_1, R.string.v3_puzzle_intent_2, R.string.v3_puzzle_intent_3).forEach { res ->
            TeperaButton(
                text = stringResource(res),
                onClick = onSolved,
                size = TeperaButtonSize.Medium,
                type = TeperaButtonType.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

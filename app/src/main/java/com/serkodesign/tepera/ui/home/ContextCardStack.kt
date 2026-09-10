package com.serkodesign.tepera.ui.home

import androidx.compose.runtime.Composable
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.pattern.PatternUiState

private const val MAX_CONTEXT_CARDS = 3 // FR-D.10

private data class StackCard(val priority: Int, val content: @Composable () -> Unit)

/**
 * FR-D.10/D.11 (SRS v2.6): верх Home — вертикальний стек, максимум 3 картки одночасно, за
 * пріоритетом актуальності (не карусель — горизонтальна прокрутка ховає вміст за першою карткою).
 * Пріоритет нижче = вищий у стеку: `PauseCard` (0) — вузьке часове вікно, дія "зараз або ніколи";
 * `WeeklyReflectionCard` (1) — доступна кілька днів, але цього тижня "нові дані"; `PatternMiniCard`
 * (2) — пасивна довідкова інформація, завжди актуальна, коли розблокована. З трьома можливими
 * картками ліміт "макс 3" наразі не відкидає жодної — залишається як явний запобіжник на випадок,
 * якщо з'явиться четверта картка.
 */
@Composable
fun ContextCardStack(
    pauseState: PauseCardUiState,
    categories: List<CategoryEntity>,
    onLabelGap: (PauseUiGap, String) -> Unit,
    onDismissGap: (PauseUiGap) -> Unit,
    weeklyState: WeeklyReflectionUiState,
    onSelectGuess: (WeeklyOnlineGuess) -> Unit,
    onDismissWeekly: () -> Unit,
    patternState: PatternUiState
) {
    val cards = listOfNotNull(
        if (pauseState.visible) {
            StackCard(0) {
                PauseCard(state = pauseState, categories = categories, onLabel = onLabelGap, onDismissGap = onDismissGap)
            }
        } else null,
        if (weeklyState.isDue) {
            StackCard(1) {
                WeeklyReflectionCard(state = weeklyState, onSelectGuess = onSelectGuess, onDismiss = onDismissWeekly)
            }
        } else null,
        if (patternState.visible) {
            StackCard(2) { PatternMiniCard(state = patternState) }
        } else null
    )

    cards.sortedBy { it.priority }.take(MAX_CONTEXT_CARDS).forEach { it.content() }
}

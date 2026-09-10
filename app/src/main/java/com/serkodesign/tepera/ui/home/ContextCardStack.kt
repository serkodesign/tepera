package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.pattern.PatternUiState

private const val MAX_CONTEXT_CARDS = 3 // FR-D.10

private data class StackCard(val priority: Int, val content: @Composable () -> Unit)

/**
 * FR-D.10/D.11 (SRS v2.6): верх Home — вертикальний стек, максимум 3 картки одночасно, за
 * пріоритетом актуальності (не карусель — горизонтальна прокрутка ховає вміст за першою карткою).
 * Пріоритет нижче = вищий у стеку: `PauseCard` (0) — вузьке часове вікно, дія "зараз або ніколи";
 * `WeeklyReflectionCard` (1) — доступна кілька днів, але цього тижня "нові дані"; `WeeklyDigestCard`
 * (2) — досліджено з Figma-макета (node 2062:2862), пасивний тижневий дайджест лічильників;
 * `PatternMiniCard` (3) — пасивна довідкова інформація, завжди актуальна, коли розблокована.
 * З чотирма можливими картками ліміт "макс 3" тепер РЕАЛЬНО може відкидати одну (найнижчий
 * пріоритет) — саме той запобіжник, на який FR-D.10 і розраховував наперед.
 *
 * **Закриття карток (за прямим запитом користувача, не в SRS):** `PauseCard` (ціла картка,
 * окремо від per-gap "×"), `WeeklyDigestCard`, `PatternMiniCard` мають "×" у заголовку —
 * ховає картку до наступного релевантного вікна даних (день/вікно опитування), а не назавжди.
 * `WeeklyReflectionCard` вже мала власний "Можна пропустити"/"Гаразд" — окремого "×" не додано,
 * щоб не дублювати той самий жест двома різними кнопками.
 */
@Composable
fun ContextCardStack(
    pauseState: PauseCardUiState,
    categories: List<CategoryEntity>,
    onLabelGap: (PauseUiGap, String) -> Unit,
    onDismissGap: (PauseUiGap) -> Unit,
    onDismissPauseCard: () -> Unit,
    weeklyState: WeeklyReflectionUiState,
    onSelectGuess: (WeeklyOnlineGuess) -> Unit,
    onDismissWeekly: () -> Unit,
    digestState: WeeklyDigestUiState,
    onDismissDigest: () -> Unit,
    patternState: PatternUiState,
    onDismissPattern: () -> Unit
) {
    val cards = listOfNotNull(
        if (pauseState.visible) {
            StackCard(0) {
                PauseCard(
                    state = pauseState,
                    categories = categories,
                    onLabel = onLabelGap,
                    onDismissGap = onDismissGap,
                    onDismissCard = onDismissPauseCard
                )
            }
        } else null,
        if (weeklyState.isDue) {
            StackCard(1) {
                WeeklyReflectionCard(state = weeklyState, onSelectGuess = onSelectGuess, onDismiss = onDismissWeekly)
            }
        } else null,
        if (digestState.visible) {
            StackCard(2) { WeeklyDigestCard(state = digestState, onDismiss = onDismissDigest) }
        } else null,
        if (patternState.visible) {
            StackCard(3) { PatternMiniCard(state = patternState, onDismiss = onDismissPattern) }
        } else null
    )

    cards.sortedBy { it.priority }.take(MAX_CONTEXT_CARDS).forEach { it.content() }
}

/** Заголовок + "×" — спільний для карток, які можна закрити (Pattern/WeeklyDigest/Pause). */
@Composable
internal fun ContextCardHeader(title: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.context_card_dismiss_action),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

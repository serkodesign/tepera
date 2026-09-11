package com.serkodesign.tepera.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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

/**
 * FR-D.10/D.10a/D.11 (SRS v2.6, черга пріоритетів оновлена в v2.8): верх Home — вертикальний
 * стек, максимум 3 картки одночасно, за пріоритетом актуальності (не карусель — горизонтальна
 * прокрутка ховає вміст за першою карткою). Пріоритет нижче = вищий у стеку:
 * `PauseCard` (0) — непозначені паузи, вузьке часове вікно, дія "зараз або ніколи" (FR-D.10a,
 * пункт 2); `WeeklyReflectionCard` (1) — тижнева "оцінка → реальність" (FR-D.10a, пункт 3);
 * `LastPhoneUseCard` (2) — час останнього використання телефону (FR-D.7, FR-D.10a, пункт 4);
 * `WeeklyDigestCard` (3) — досліджено з Figma-макета (node 2062:2862), не в SRS буквально;
 * `PatternMiniCard` (4) — пасивна довідкова інформація, завжди актуальна, коли розблокована.
 * З п'ятьма можливими картками ліміт "макс 3" (FR-D.10b: без наповнювача в порожніх слотах)
 * тепер РЕАЛЬНО відкидає найнижчі за пріоритетом — саме той запобіжник, на який FR-D.10
 * розраховував наперед.
 *
 * **Закриття карток (за прямим запитом користувача, не в SRS):** `PauseCard` (ціла картка,
 * окремо від per-gap "×"), `LastPhoneUseCard`, `WeeklyDigestCard`, `PatternMiniCard` мають "×"
 * у заголовку — ховає картку до наступного релевантного вікна даних, а не назавжди.
 * `WeeklyReflectionCard` вже мала власний "Можна пропустити"/"Гаразд" — окремого "×" не додано,
 * щоб не дублювати той самий жест двома різними кнопками.
 *
 * **Артефакт при закритті картки (виправлено, не в SRS):** раніше картка, чий стан ставав
 * невидимим (напр. після тапу "×"), одразу зникала зі списку `cards` і повністю видалялась із
 * дерева композиції в той самий кадр — на деяких пристроях (відтворено на Samsung S23, Huawei P9)
 * різке видалення заокругленої (`clip`+`background`) картки не встигало коректно інвалідувати
 * область екрана, лишаючи на кадр-два візуальний "привид" картки. Кожна картка тепер ЗАВЖДИ
 * присутня в дереві композиції, обгорнута в `AnimatedVisibility` — вона сама коректно керує
 * появою/зникненням (згортання+прозорість) і лише ПІСЛЯ завершення анімації прибирає вміст із
 * композиції, без різкого "вирізання" LayoutNode.
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
    lastPhoneUseState: LastPhoneUseUiState,
    onDismissLastPhoneUse: () -> Unit,
    digestState: WeeklyDigestUiState,
    onDismissDigest: () -> Unit,
    patternState: PatternUiState,
    onDismissPattern: () -> Unit
) {
    // Пріоритет нижче = вищий у стеку (FR-D.10a): pause=0, weekly=1, lastPhoneUse=2, digest=3, pattern=4.
    val shown = listOfNotNull(
        0.takeIf { pauseState.visible },
        1.takeIf { weeklyState.isDue },
        2.takeIf { lastPhoneUseState.visible },
        3.takeIf { digestState.visible },
        4.takeIf { patternState.visible }
    ).sorted().take(MAX_CONTEXT_CARDS).toSet()

    ContextCardSlot(visible = 0 in shown) {
        PauseCard(
            state = pauseState,
            categories = categories,
            onLabel = onLabelGap,
            onDismissGap = onDismissGap,
            onDismissCard = onDismissPauseCard
        )
    }
    ContextCardSlot(visible = 1 in shown) {
        WeeklyReflectionCard(state = weeklyState, onSelectGuess = onSelectGuess, onDismiss = onDismissWeekly)
    }
    ContextCardSlot(visible = 2 in shown) {
        LastPhoneUseCard(state = lastPhoneUseState, onDismiss = onDismissLastPhoneUse)
    }
    ContextCardSlot(visible = 3 in shown) {
        WeeklyDigestCard(state = digestState, onDismiss = onDismissDigest)
    }
    ContextCardSlot(visible = 4 in shown) {
        PatternMiniCard(state = patternState, onDismiss = onDismissPattern)
    }
}

@Composable
private fun ContextCardSlot(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        content()
    }
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

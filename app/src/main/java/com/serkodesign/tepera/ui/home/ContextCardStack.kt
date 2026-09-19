package com.serkodesign.tepera.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import com.serkodesign.tepera.ui.theme.TeperaMotion
import com.serkodesign.tepera.ui.theme.TeperaSpecs
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.cards.CardType
import com.serkodesign.tepera.data.local.entity.CategoryEntity

/**
 * FR-D.10/D.10a/D.11 (SRS v2.6, черга пріоритетів оновлена в v2.8, T-3 tepera-dev-spec.md
 * додала ще одну картку): верх Home — вертикальний стек, максимум 3 картки одночасно, за
 * пріоритетом актуальності (не карусель — горизонтальна прокрутка ховає вміст за першою карткою).
 *
 * **T-13 (tepera-dev-spec.md), "рушій карток": який саме набір карток видно, вирішує тепер
 * `CardEngine`/`CardStackViewModel` (глобальний бюджет "не більше 1 картки-оцінки на тиждень",
 * ліміт 3 картки одночасно, правило "подієві не витісняють тижневі більш ніж двічі поспіль") —
 * цей composable лише рендерить готовий [visibleCards], сам більше не рахує пріоритет/ліміт.**
 * Порядок пріоритету (нижче = вищий у стеку, той самий, що вже діяв): `OnlineEstimateRevealCard`
 * (0) — одноразове розкриття "оцінка/реальність" онбордингу (T-3), найвищий пріоритет: пряме
 * продовження дії, яку людина щойно зробила (надала дозвіл); `PauseCard` (1) — непозначені
 * паузи, вузьке часове вікно, дія "зараз або ніколи" (FR-D.10a, пункт 2); `WeeklyReflectionCard`
 * (2) — тижнева "оцінка → реальність" (FR-D.10a, пункт 3); `UnlockEstimateCard` (3, T-14) — та
 * сама форма, що WeeklyReflectionCard, тому одразу під нею; `LastPhoneUseEstimateCard` (4,
 * T-10) — той самий формат, замінив ПОСТІЙНИЙ показ часу останнього використання
 * (`LastPhoneUseCard`, видалено T-10 — розділ 2.2 "принцип пасивного сорому" забороняє пасивний
 * показ такого числа); `WeeklyDigestCard` (5) — досліджено з Figma-макета (node 2062:2862), не
 * в SRS буквально; `PatternMiniCard` (6) — пасивна довідкова інформація, завжди актуальна, коли
 * розблокована; `GateEventsSummaryCard` (7, T-6) — "свідчення спроможності" (FR-P.3), найнижчий
 * пріоритет: раз на місяць, найменш термінова з усіх.
 *
 * **Закриття карток (за прямим запитом користувача, не в SRS):** `PauseCard` (ціла картка,
 * окремо від per-gap "×"), `WeeklyDigestCard`, `PatternMiniCard` мають "×" у заголовку — ховає
 * картку до наступного релевантного вікна даних, а не назавжди. `WeeklyReflectionCard`,
 * `UnlockEstimateCard`, `LastPhoneUseEstimateCard` вже мають власний "Можна пропустити"/"Гаразд"
 * (той самий формат "оцінка → реальність") — окремого "×" не додано, щоб не дублювати той самий
 * жест двома різними кнопками.
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
    visibleCards: Set<CardType>,
    onlineEstimateRevealState: OnlineEstimateRevealUiState,
    onDismissOnlineEstimateReveal: () -> Unit,
    pauseState: PauseCardUiState,
    categories: List<CategoryEntity>,
    onLabelGap: (PauseUiGap, String) -> Unit,
    onDismissGap: (PauseUiGap) -> Unit,
    onDismissPauseCard: () -> Unit,
    weeklyState: WeeklyReflectionUiState,
    onSelectGuess: (WeeklyOnlineGuess) -> Unit,
    onDismissWeekly: () -> Unit,
    unlockEstimateState: UnlockEstimateUiState,
    onSelectUnlockGuess: (UnlockCountGuess) -> Unit,
    onDismissUnlockEstimate: () -> Unit,
    lastPhoneUseEstimateState: LastPhoneUseEstimateUiState,
    onSelectLastPhoneUseGuess: (LastPhoneUseGuess) -> Unit,
    onDismissLastPhoneUseEstimate: () -> Unit,
    gateEventsSummaryState: GateEventsSummaryUiState,
    onDismissGateEventsSummary: () -> Unit
) {
    ContextCardSlot(visible = CardType.ONLINE_ESTIMATE_REVEAL in visibleCards) {
        OnlineEstimateRevealCard(state = onlineEstimateRevealState, onDismiss = onDismissOnlineEstimateReveal)
    }
    ContextCardSlot(visible = CardType.PAUSE in visibleCards) {
        PauseCard(
            state = pauseState,
            categories = categories,
            onLabel = onLabelGap,
            onDismissGap = onDismissGap,
            onDismissCard = onDismissPauseCard
        )
    }
    ContextCardSlot(visible = CardType.WEEKLY_REFLECTION in visibleCards) {
        WeeklyReflectionCard(state = weeklyState, onSelectGuess = onSelectGuess, onDismiss = onDismissWeekly)
    }
    ContextCardSlot(visible = CardType.UNLOCK_ESTIMATE in visibleCards) {
        UnlockEstimateCard(state = unlockEstimateState, onSelectGuess = onSelectUnlockGuess, onDismiss = onDismissUnlockEstimate)
    }
    ContextCardSlot(visible = CardType.LAST_PHONE_USE_ESTIMATE in visibleCards) {
        LastPhoneUseEstimateCard(state = lastPhoneUseEstimateState, onSelectGuess = onSelectLastPhoneUseGuess, onDismiss = onDismissLastPhoneUseEstimate)
    }
    ContextCardSlot(visible = CardType.GATE_EVENTS_SUMMARY in visibleCards) {
        GateEventsSummaryCard(state = gateEventsSummaryState, onDismiss = onDismissGateEventsSummary)
    }
}

@Composable
private fun ContextCardSlot(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(TeperaMotion.SHORT4, easing = TeperaMotion.EmphasizedDecelerate)) +
            expandVertically(TeperaSpecs.spatial()),
        exit = fadeOut(tween(TeperaMotion.SHORT3, easing = TeperaMotion.EmphasizedAccelerate)) +
            shrinkVertically(TeperaSpecs.spatial())
    ) {
        content()
    }
}

/**
 * Заголовок + "×" — спільний для карток, які можна закрити (Pattern/WeeklyDigest/Pause).
 * [titleStyle] за замовчуванням лишається попереднім виглядом (`bodyMedium`) для решти карток —
 * `PatternMiniCard` за прямим запитом користувача передає стиль заголовка "Активності"
 * (`titleMedium` + Bold), решта карток цей параметр не зачіпає.
 */
@Composable
internal fun ContextCardHeader(
    title: String,
    onDismiss: () -> Unit,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = titleStyle)
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.context_card_dismiss_action),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

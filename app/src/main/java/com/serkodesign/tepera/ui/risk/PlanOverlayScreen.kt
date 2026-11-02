package com.serkodesign.tepera.ui.risk

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.SuggestionPicker
import com.serkodesign.tepera.data.startPlannedCategoryTimer
import com.serkodesign.tepera.data.suggestionTextRes
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.domain.risk.ActivitySuggestions
import com.serkodesign.tepera.domain.risk.QuickPuzzles
import com.serkodesign.tepera.ui.theme.RedesignDarkColors
import com.serkodesign.tepera.ui.theme.RedesignScope
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.util.PlanOverlayStore
import kotlinx.coroutines.launch

private data class OverlayContent(
    val appLabel: String,
    val startMinute: Int,
    val endMinute: Int,
    val suggestion: ActivitySuggestions.Suggestion,
    /** Людина щойно сказала «Так, роблю це» і знову відкрила стрічку: замість нової ідеї — «я бачу тебе». */
    val watching: Boolean = false,
    /** Скільки задачок треба розв'язати, перш ніж «Відкрити на 5 хв» спрацює (0 — жодної). Лише для екрана «я за тобою слідкую». */
    val puzzles: Int = 0,
    val puzzleLevel: QuickPuzzles.Level = QuickPuzzles.Level.NORMAL
)

private val DURATIONS_MINUTES = listOf(15, 30, 60)

/** Скільки діє обіцянка, коли для ідеї нема таймера (прогулянка тощо) і тривалість не питали. */
private const val DEFAULT_COMMITMENT_MINUTES = 30

/**
 * v3.0 MVP, Етап 8 (beta), макет `v3/PlanSheet`: підказка «що зробити замість» поверх стрічки, відкритої всередині
 * години ризику. Показує сервіс доступності ([com.serkodesign.tepera.gates.GateAccessibilityService]), ніколи — застосунок
 * сам. Одна пропозиція за порою доби ([SuggestionPicker]); без таймера очікування й оцінок — людина сама вирішує.
 *
 * «Так, роблю це» — для пропозиції з категорією спершу питаємо тривалість (15/30/60 хв) і запускаємо таймер категорії,
 * далі на робочий стіл. «Інша ідея» — інша пропозиція без виходу з листа. «Відкрити на 5 хв» — стрічка відкривається, а
 * за 5 хв починається м'який вихід ([com.serkodesign.tepera.gates.SoftExitController]). «Не сьогодні» — стрічка відкривається
 * й до кінця вечора підказка більше не показується. Відмова ховає саму пропозицію на кілька днів; усі вибори лише
 * рахуються для екрана тестувальників.
 */
@Composable
fun PlanOverlayScreen(
    gateRepository: GateRepository,
    packageName: String,
    windowId: String,
    onDone: () -> Unit,
    /** Людина зробила вибір («Так», «Відкрити на 5 хв»); «назад» і вихід з листа вибором не вважаються. */
    onDecided: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf<OverlayContent?>(null) }
    var shown by remember { mutableStateOf(emptySet<ActivitySuggestions.Id>()) }
    var choosingDuration by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var solving by remember { mutableStateOf(false) }

    LaunchedEffect(windowId) {
        val window = app.riskWindowRepository.getWindow(windowId)
        val committed = PlanOverlayStore.activeCommitment(context)
        val suggestion = if (committed != null) ActivitySuggestions.Suggestion(committed, null) else SuggestionPicker.pickFor(context, windowId)
        content = if (window == null || suggestion == null) null else {
            shown = setOf(suggestion.id)
            // Рахуємо КОЖЕН показ листа для цих воріт (з обіцянкою чи без); з 3-го разу «Відкрити на 5 хв» вимагає задачки.
            val reentry = PlanOverlayStore.recordReentry(context, packageName)
            val puzzles = QuickPuzzles.puzzlesRequired(reentry.number, PlanOverlayStore.arePuzzlesEnabled(context))
            // Попереднє відкриття теж закінчувалось задачкою — наступна йде одразу за нею, без плашки з пропозицією
            // і кнопкою «Відкрити на 5 хв» (вона все одно вела б до задачки). «Назад» з задачки повертає до плашки.
            solving = puzzles > 0 && reentry.number > QuickPuzzles.FIRST_PUZZLE_REENTRY
            OverlayContent(
                resolveLabel(context.packageManager, packageName), window.startMinute, window.endMinute, suggestion,
                watching = committed != null,
                puzzles = puzzles,
                // Людина певний час не поверталась до стрічки — задачки легші.
                puzzleLevel = QuickPuzzles.levelFor(reentry.minutesSincePrevious)
            )
        }
        // Вікно зникло між показом сервісу й відкриттям екрана — нічого показувати; пускаємо людину в застосунок.
        if (content == null) {
            gateRepository.getLaunchIntent(packageName)?.let { context.startActivity(it) }
            finished = true
        }
    }

    fun leave() {
        // Підказку показано поверх вже запущеного застосунку, тому вихід веде на робочий стіл (як у воріт через доступність).
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finished = true
    }

    fun commit(idea: ActivitySuggestions.Id, minutes: Int) =
        PlanOverlayStore.setCommitment(context, idea, System.currentTimeMillis() + minutes * 60_000L)

    fun accept(minutes: Int) {
        val suggestion = content?.suggestion ?: return
        val categoryId = suggestion.categoryId ?: return
        onDecided()
        PlanOverlayStore.recordAccepted(context)
        commit(suggestion.id, minutes)
        scope.launch {
            startPlannedCategoryTimer(context, categoryId, minutes)
            leave()
        }
    }

    fun anotherIdea() {
        val c = content ?: return
        PlanOverlayStore.recordDeclined(context, c.suggestion.id)
        scope.launch {
            val next = SuggestionPicker.pickFor(context, windowId, exclude = shown) ?: return@launch
            shown = shown + next.id
            content = c.copy(suggestion = next)
        }
    }

    fun openForFiveMinutes() {
        val c = content ?: return
        onDecided()
        // Обіцяну ідею не ховаємо на кілька днів: людина від неї не відмовлялась, лише зайшла раніше часу.
        if (!c.watching) PlanOverlayStore.recordDeclined(context, c.suggestion.id)
        PlanOverlayStore.recordContinued(context)
        PlanOverlayStore.setPendingSoftExit(
            context,
            PlanOverlayStore.PendingSoftExit(packageName, c.appLabel, context.getString(suggestionTextRes(c.suggestion.id)), System.currentTimeMillis())
        )
        gateRepository.getLaunchIntent(packageName)?.let { context.startActivity(it) }
        finished = true
    }

    LaunchedEffect(finished) {
        if (finished) onDone()
    }

    BackHandler(enabled = content != null && !finished) {
        if (choosingDuration) choosingDuration = false else if (solving) solving = false else {
            PlanOverlayStore.recordLeft(context)
            leave()
        }
    }

    val c = content ?: return
    // Лист завжди темний, як у макеті (фон #1b1b1d), незалежно від теми застосунку.
    RedesignScope(RedesignDarkColors) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFF1B1B1D))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.14f)))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFA65D7C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(c.appLabel.take(1).uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text(c.appLabel, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(R.string.v3_overlay_sheet_sub, formatClock(c.startMinute) + "–" + formatClock(c.endMinute)),
                            color = Color(0xFFC5DCD9), fontSize = 12.sp, lineHeight = 16.sp
                        )
                    }
                }
                if (solving) {
                    PuzzleStep(
                        total = c.puzzles,
                        level = c.puzzleLevel,
                        onAllSolved = ::openForFiveMinutes,
                        onLeave = { onDecided(); leave() }
                    )
                } else if (c.watching) {
                    WatchingStep(
                        suggestion = c.suggestion,
                        onLeave = { onDecided(); leave() },
                        onOpenFive = { if (c.puzzles > 0) solving = true else openForFiveMinutes() }
                    )
                } else if (choosingDuration) {
                    DurationStep(onPick = ::accept, onBack = { choosingDuration = false })
                } else {
                    SuggestionStep(
                        suggestion = c.suggestion,
                        onYes = {
                            if (c.suggestion.categoryId != null) choosingDuration = true
                            else { onDecided(); PlanOverlayStore.recordAccepted(context); commit(c.suggestion.id, DEFAULT_COMMITMENT_MINUTES); leave() }
                        },
                        onAnother = ::anotherIdea,
                        onOpenFive = { if (c.puzzles > 0) solving = true else openForFiveMinutes() }
                    )
                }
            }
        }
    }
}

/**
 * Задачки на шляху «Ще 5» м'якого виходу (з 3-го натискання за сесію; те саме правило, що для «Відкрити на 5 хв» на екрані
 * «я за тобою слідкую»). «Гаразд, іду» вільний, «назад» — повернення до плашки.
 */
@Composable
fun SoftExitPuzzleScreen(
    appLabel: String,
    puzzles: Int,
    level: QuickPuzzles.Level,
    onSolved: () -> Unit,
    onLeave: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    RedesignScope(RedesignDarkColors) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFF1B1B1D))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.14f)))
                Text(appLabel, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
                PuzzleStep(total = puzzles, level = level, onAllSolved = onSolved, onLeave = onLeave)
            }
        }
    }
}

@Composable
private fun SuggestionStep(
    suggestion: ActivitySuggestions.Suggestion,
    onYes: () -> Unit,
    onAnother: () -> Unit,
    onOpenFive: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.v3_overlay_sheet_title_suggest), color = Color(0xFFC5DCD9), fontSize = 14.sp, lineHeight = 20.sp
        )
        Text(
            stringResource(suggestionTextRes(suggestion.id)), color = Color.White, fontSize = 27.sp, lineHeight = 34.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.semantics { heading() }
        )
        Text(stringResource(R.string.v3_sg_hint), color = Color(0xFFC5DCD9), fontSize = 14.sp, lineHeight = 20.sp)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TeperaButton(
            text = stringResource(R.string.v3_sg_yes),
            onClick = onYes,
            size = TeperaButtonSize.Medium,
            type = TeperaButtonType.Primary,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TeperaButton(
                text = stringResource(R.string.v3_sg_another),
                onClick = onAnother,
                size = TeperaButtonSize.Medium,
                type = TeperaButtonType.Secondary,
                modifier = Modifier.weight(1f)
            )
            TeperaButton(
                text = stringResource(R.string.v3_overlay_open_five),
                onClick = onOpenFive,
                size = TeperaButtonSize.Medium,
                type = TeperaButtonType.Secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private class WatchVariant(val emoji: Int, val title: Int, val body: Int)

/** Екрани «я бачу тебе» для повторного входу в стрічку після «Так, роблю це»; щоразу випадковий. */
private val WATCH_VARIANTS = listOf(
    WatchVariant(R.string.v3_watch_1_emoji, R.string.v3_watch_1_title, R.string.v3_watch_1_body),
    WatchVariant(R.string.v3_watch_2_emoji, R.string.v3_watch_2_title, R.string.v3_watch_2_body),
    WatchVariant(R.string.v3_watch_3_emoji, R.string.v3_watch_3_title, R.string.v3_watch_3_body),
    WatchVariant(R.string.v3_watch_4_emoji, R.string.v3_watch_4_title, R.string.v3_watch_4_body),
    WatchVariant(R.string.v3_watch_5_emoji, R.string.v3_watch_5_title, R.string.v3_watch_5_body)
)

/** Людина пообіцяла зайнятись справою і вже знову відкрила стрічку: жартівливе, але чесне «я бачу тебе». */
@Composable
private fun WatchingStep(
    suggestion: ActivitySuggestions.Suggestion,
    onLeave: () -> Unit,
    onOpenFive: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Варіант обираємо один раз на показ листа, щоб він не мінявся при перекомпонуванні.
        val variant = remember { WATCH_VARIANTS.random() }
        Text(stringResource(variant.emoji), fontSize = 56.sp, lineHeight = 64.sp)
        Text(
            stringResource(variant.title), color = Color.White, fontSize = 27.sp, lineHeight = 34.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(variant.body, stringResource(suggestionTextRes(suggestion.id))),
            color = Color(0xFFC5DCD9), fontSize = 14.sp, lineHeight = 20.sp
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TeperaButton(
            text = stringResource(R.string.v3_watch_leave),
            onClick = onLeave,
            size = TeperaButtonSize.Medium,
            type = TeperaButtonType.Primary,
            modifier = Modifier.fillMaxWidth()
        )
        TeperaButton(
            text = stringResource(R.string.v3_overlay_open_five),
            onClick = onOpenFive,
            size = TeperaButtonSize.Medium,
            type = TeperaButtonType.Secondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Другий крок для пропозиції з категорією: на скільки запускати таймер. Зупинка — вчасно воркером або кнопкою «Закінчив» на Home. */
@Composable
private fun DurationStep(onPick: (Int) -> Unit, onBack: () -> Unit) {
    Text(
        stringResource(R.string.v3_sg_duration_title), color = Color.White, fontSize = 27.sp, lineHeight = 34.sp,
        fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().semantics { heading() }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DURATIONS_MINUTES.forEach { minutes ->
            TeperaButton(
                text = if (minutes >= 60) stringResource(R.string.v3_dur_hours, minutes / 60) else stringResource(R.string.v3_dur_minutes, minutes),
                onClick = { onPick(minutes) },
                size = TeperaButtonSize.Medium,
                type = TeperaButtonType.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
    TeperaButton(
        text = stringResource(R.string.v3_sg_duration_back),
        onClick = onBack,
        size = TeperaButtonSize.Medium,
        type = TeperaButtonType.Tertiary,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun resolveLabel(pm: PackageManager, packageName: String): String = try {
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (e: PackageManager.NameNotFoundException) {
    packageName
}

private fun formatClock(minute: Int) = String.format(java.util.Locale.ROOT, "%02d:%02d", minute / 60 % 24, minute % 60)

package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.SuggestionPicker
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.startPlannedCategoryTimer
import com.serkodesign.tepera.data.suggestionTextRes
import com.serkodesign.tepera.domain.risk.ActivitySuggestions
import com.serkodesign.tepera.domain.risk.NextWindow
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.home.HomeViewModel
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.heroMoodBackground
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.util.PlanOverlayStore
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale

/** Скільки діє обіцянка після «Почати» (та сама, що в листі плану поверх стрічки). */
private const val COMMITMENT_MINUTES = 30

private val HeroCream = Color(0xFFFEFFEF)
private val HeroCreamText = Color(0xFF062924)

/**
 * Герой головного екрана, коли година скролінгу триває зараз: відлік до кінця з тихою смугою, ОДНА ідея «що зробити
 * замість» і одна головна дія — «Почати: <активність>» (запускає таймер і ставить обіцянку, як «Так, роблю це» на листі
 * плану). «Інша ідея» міняє пропозицію на місці. Якщо людина вже обрала ідею — показуємо її без кнопок: рішення прийнято.
 * Поза тижнем спостереження (там ідей не показуємо взагалі).
 */
@Composable
fun IdeaHero(
    windowId: String,
    occurrence: NextWindow.Occurrence,
    now: LocalDateTime,
    categories: List<CategoryEntity>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var suggestion by remember(windowId) { mutableStateOf<ActivitySuggestions.Suggestion?>(null) }
    var shown by remember(windowId) { mutableStateOf(emptySet<ActivitySuggestions.Id>()) }
    var committed by remember(windowId) { mutableStateOf<ActivitySuggestions.Id?>(null) }
    LaunchedEffect(windowId) {
        committed = PlanOverlayStore.activeCommitment(context)
        if (committed == null) {
            suggestion = SuggestionPicker.pickFor(context, windowId)
            shown = setOfNotNull(suggestion?.id)
        }
    }

    val onDark = TeperaPalette.textOnDark
    val total = Duration.between(occurrence.startAt, occurrence.endAt).toMinutes().coerceAtLeast(1)
    val passed = Duration.between(occurrence.startAt, now).toMinutes().coerceIn(0, total)
    val left = (total - passed).coerceAtLeast(0)

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).heroMoodBackground()
            .then(if (TeperaPalette.colors.isDawn) Modifier.border(1.dp, TeperaPalette.heroBorder, RoundedCornerShape(24.dp)) else Modifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.v3_next_label_inside), color = onDark.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
            Text(
                stringResource(R.string.v3_next_until_format, clock(occurrence.endAt)), color = onDark,
                fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.v3_hero_left_format, durationText(left)), color = onDark.copy(alpha = 0.8f),
                fontSize = 14.sp, lineHeight = 20.sp
            )
        }
        // Тиха смуга минулого часу години: без кольорових порогів і без підпису.
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(onDark.copy(alpha = 0.18f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(passed.toFloat() / total).background(onDark.copy(alpha = 0.7f)))
        }

        val picked = suggestion
        val committedId = committed
        when {
            committedId != null -> Text(
                stringResource(R.string.v3_hero_committed_format, stringResource(suggestionTextRes(committedId))),
                color = onDark, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium
            )
            picked != null -> {
                Text(
                    stringResource(suggestionTextRes(picked.id)), color = onDark, fontSize = 22.sp, lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium
                )
                val categoryName = categories.firstOrNull { it.id == picked.categoryId }?.let { categoryDisplayName(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HeroButton(
                        text = if (categoryName != null) stringResource(R.string.v3_hero_start_format, categoryName) else stringResource(R.string.v3_hero_do_it),
                        filled = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        PlanOverlayStore.recordAccepted(context)
                        PlanOverlayStore.setCommitment(context, picked.id, System.currentTimeMillis() + COMMITMENT_MINUTES * 60_000L)
                        committed = picked.id
                        picked.categoryId?.let { categoryId -> scope.launch { startPlannedCategoryTimer(context, categoryId, COMMITMENT_MINUTES) } }
                    }
                    HeroButton(text = stringResource(R.string.v3_sg_another), filled = false) {
                        PlanOverlayStore.recordDeclined(context, picked.id)
                        scope.launch {
                            SuggestionPicker.pickFor(context, windowId, exclude = shown)?.let { next ->
                                shown = shown + next.id
                                suggestion = next
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Кнопка на темному герої: кремова заливка (головна) або контур; ≥48dp за висотою. */
@Composable
private fun HeroButton(text: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    // «Світанок» бере кольори з теми; класичний вигляд лишається буквально як був (кремова кнопка на темному hero).
    val dawn = TeperaPalette.colors.isDawn
    val filledBg = if (dawn) TeperaPalette.heroCta else HeroCream
    val filledText = if (dawn) TeperaPalette.heroCtaText else HeroCreamText
    val plainBg = if (dawn) TeperaPalette.heroTrack else Color.White.copy(alpha = 0.12f)
    val plainText = if (dawn) TeperaPalette.textOnDark else HeroCream
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(if (filled) Modifier.background(filledBg) else Modifier.background(plainBg))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = if (filled) filledText else plainText, fontSize = 14.sp, lineHeight = 20.sp,
            fontWeight = FontWeight.Medium, maxLines = 2
        )
    }
}

/**
 * Ряд чіпів активностей: один тап — старт або зупинка таймера (одночасно йде лише один), активний чіп залитий. Час
 * кожної активності — у Щоденнику й Статистиці, а не тут: на головному лише дія. Остання кнопка — додати запис вручну.
 */
@Composable
fun ActivityChips(onAddEntry: () -> Unit) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.categoryRepository, app.activityRepository, app.activeTimerStore)
    )
    val summary by vm.todaySummary.collectAsState()
    val loaded by vm.summaryLoaded.collectAsState()
    if (!loaded || summary.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.activities_title), color = TeperaPalette.textPrimary, fontSize = 18.sp, lineHeight = 24.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 24.dp).semantics { heading() }
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            summary.forEach { item ->
                val active = item.trackingStartTime != null
                val container = if (active) TeperaPalette.activityCardActive else TeperaPalette.activityCardIdle
                val content = if (active) TeperaPalette.onPrimary else TeperaPalette.textPrimary
                Row(
                    Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(container)
                        .toggleable(value = active, role = Role.Switch) { vm.toggleTimer(item.category.id) }
                        .padding(start = 14.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lineArt = categoryLineArtIconRes(item.category.iconName)
                    if (lineArt != null) {
                        Icon(painterResource(lineArt), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(categoryIcon(item.category.iconName), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                    }
                    Text(categoryDisplayName(item.category), color = content, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    if (active) Box(Modifier.size(8.dp).clip(CircleShape).background(content))
                }
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(TeperaPalette.activityCardIdle)
                    .clickable(role = Role.Button, onClick = onAddEntry),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    TeperaSymbols.Add, contentDescription = stringResource(R.string.v3_chip_add_entry),
                    tint = TeperaPalette.textPrimary, modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun clock(dt: LocalDateTime): String = String.format(Locale.ROOT, "%02d:%02d", dt.hour, dt.minute)

@Composable
private fun durationText(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.v3_dur_hours_minutes, hours.toInt(), minutes.toInt())
        hours > 0 -> stringResource(R.string.v3_dur_hours, hours.toInt())
        else -> stringResource(R.string.v3_dur_minutes, minutes.toInt())
    }
}

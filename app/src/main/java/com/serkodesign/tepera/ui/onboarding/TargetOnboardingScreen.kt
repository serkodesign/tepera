package com.serkodesign.tepera.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.OnboardingTopBar
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaMotion
import com.serkodesign.tepera.ui.theme.TeperaOnboardingTitle
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.TargetSuggestion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * CC-1 / Figma "App concept", Onboarding / Target: крок «Орієнтир на день». З переносом дозволу в кінець
 * онбордингу історії використання тут ще немає — підказки зі середнім більше нема, лише нейтральний стартовий
 * вибір. Велике число + лінійка з рисками й маркером, що тягнеться пальцем (замість степпера).
 * Дві РІВНОЦІННІ кнопки («Пізніше» і «Задати» — той самий тип і розмір). «Пізніше» лишає орієнтир порожнім —
 * ніде не з'являється число-ціль; змінити можна в Налаштуваннях → Відстеження.
 */
@Composable
fun TargetOnboardingScreen(
    settingsStore: SettingsStore,
    onDone: () -> Unit,
    onSkipAll: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var hours by remember { mutableStateOf(TargetSuggestion.NEUTRAL_START_HOURS) }

    fun finish(setTarget: Boolean) {
        scope.launch {
            settingsStore.setTargetMinutes(if (setTarget) hours * 60 else null)
            settingsStore.setTargetOnboardingSeen()
            onDone()
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TeperaOnboardingTitle(text = stringResource(R.string.target_onboarding_title))
                Text(
                    text = stringResource(R.string.target_onboarding_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                TargetRuler(
                    hours = hours,
                    onHoursChange = { hours = it },
                    modifier = Modifier.padding(top = 40.dp)
                )
                // Рівноцінні варіанти: однаковий тип і розмір.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeperaButton(
                        text = stringResource(R.string.target_onboarding_later),
                        onClick = { finish(setTarget = false) },
                        modifier = Modifier.weight(1f),
                        type = TeperaButtonType.Secondary
                    )
                    TeperaButton(
                        text = stringResource(R.string.target_onboarding_set),
                        onClick = { finish(setTarget = true) },
                        modifier = Modifier.weight(1f),
                        type = TeperaButtonType.Secondary
                    )
                }
            }
            OnboardingTopBar(
                step = 4,
                onSkipAll = onSkipAll,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

private const val RULER_MIN_HOURS = 0

/**
 * Велике значення + лінійка 0–[TargetSuggestion.MAX_HOURS] год з рискою на кожну годину (довші — кожні 4 год) і
 * маркером-повзунком. Значення обмежене [TargetSuggestion.MIN_HOURS]..[TargetSuggestion.MAX_HOURS] — як слайдер у
 * Налаштуваннях → Відстеження; тап і перетягування, плюс семантика повзунка для скрінрідера.
 */
@Composable
private fun TargetRuler(
    hours: Int,
    onHoursChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val minHours = TargetSuggestion.MIN_HOURS
    val maxHours = TargetSuggestion.MAX_HOURS
    val ink = TeperaPalette.buttonBrandDark
    val brand = TeperaPalette.buttonBrand
    val valueText = stringResource(R.string.settings_target_hours_format, hours)
    val scaleDescription = stringResource(R.string.onboarding_target_scale_description)

    val markerFraction by animateFloatAsState(
        targetValue = (hours - RULER_MIN_HOURS).toFloat() / (maxHours - RULER_MIN_HOURS),
        animationSpec = androidx.compose.animation.core.tween(TeperaMotion.SHORT4),
        label = "rulerMarker"
    )

    fun hoursAt(x: Float, width: Float): Int =
        (RULER_MIN_HOURS + (x / width).coerceIn(0f, 1f) * (maxHours - RULER_MIN_HOURS))
            .roundToInt()
            .coerceIn(minHours, maxHours)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = hours.toString(),
                color = ink,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium,
                fontSize = 96.sp,
                lineHeight = 100.sp
            )
            Text(
                text = stringResource(R.string.onboarding_target_unit),
                color = ink.copy(alpha = 0.7f),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 18.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(top = 16.dp)
                .semantics {
                    contentDescription = scaleDescription
                    stateDescription = valueText
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = hours.toFloat(),
                        range = minHours.toFloat()..maxHours.toFloat(),
                        steps = (maxHours - minHours - 1).coerceAtLeast(0)
                    )
                    setProgress { target ->
                        onHoursChange(target.roundToInt().coerceIn(minHours, maxHours))
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> onHoursChange(hoursAt(offset.x, size.width.toFloat())) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onHoursChange(hoursAt(change.position.x, size.width.toFloat()))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tickWidth = 2.dp.toPx()
                val usable = size.width - tickWidth
                val steps = maxHours - RULER_MIN_HOURS
                for (i in 0..steps) {
                    val major = i % 4 == 0
                    val tickHeight = if (major) 26.dp.toPx() else 14.dp.toPx()
                    drawRoundRect(
                        color = ink.copy(alpha = if (major) 0.55f else 0.28f),
                        topLeft = Offset(usable * i / steps, size.height - tickHeight),
                        size = Size(tickWidth, tickHeight),
                        cornerRadius = CornerRadius(tickWidth / 2f)
                    )
                }
                val markerWidth = 6.dp.toPx()
                val markerHeight = 40.dp.toPx()
                drawRoundRect(
                    color = brand,
                    topLeft = Offset(
                        (usable * markerFraction + tickWidth / 2f - markerWidth / 2f)
                            .coerceIn(0f, size.width - markerWidth),
                        size.height - markerHeight
                    ),
                    size = Size(markerWidth, markerHeight),
                    cornerRadius = CornerRadius(markerWidth / 2f)
                )
            }
        }
        Text(
            text = stringResource(R.string.onboarding_target_chip),
            color = ink,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(ink.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

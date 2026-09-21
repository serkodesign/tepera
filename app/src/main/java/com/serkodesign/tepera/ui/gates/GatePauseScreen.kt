package com.serkodesign.tepera.ui.gates

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.GateEventRepository
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.util.findActivity
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlin.math.cos
import kotlin.math.sin
/**
 * T-5 (tepera-dev-spec.md, FR-G частина 2). Малюється на тому самому градієнтному фоні, що решта
 * застосунку (`Routes.GATE_PAUSE` у `GRADIENT_ROUTES`, `TeperaNavHost`).
 *
 * **Редизайн за прямим запитом користувача, за наданим макетом — свідома зміна попередньо
 * задокументованої вимоги приймання T-5 ("нічого, крім назви застосунку, лічильника й
 * кнопки"):** дихальний бейдж-"квітка" ([BreathingBadge]) — росте й стискається БЕЗПЕРЕРВНО, з
 * першого кадру екрана, суто візуально (`rememberBreathState`, не пов'язано з
 * `GatePauseViewModel`); лічильник спроб відкрити застосунок сьогодні; дві кнопки замість
 * однієї. **Пріоритет кнопок навмисно інвертований відносно макета** (також за прямим запитом):
 * "Продовжити" — контурна (тиха, другорядна дія, напівпрозора — `Modifier.alpha` — доки
 * неактивна), "Вийти" — суцільна (візуально основна) — узгоджується з Trampoline UX-філософією
 * застосунку (тихе заохочення НЕ відкривати).
 *
 * **T-6 (tepera-dev-spec.md): `BackHandler` перехоплює системну "назад" і викликає ТОЙ САМИЙ
 * `viewModel.cancel()`, що кнопка "Вийти"** — інакше вихід "назад" лишався б непорахованим
 * "рішенням" для події T-6, хоча продуктово це те саме скасування.
 */
@Composable
fun GatePauseScreen(
    gateRepository: GateRepository,
    gateEventRepository: GateEventRepository,
    packageName: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GatePauseViewModel = viewModel(
        factory = GatePauseViewModel.Factory(context, gateRepository, gateEventRepository, packageName)
    )
    val state by viewModel.uiState.collectAsState()

    // За прямим запитом користувача: незалежно від того, скасування чи продовження, Tepera не
    // повинна лишатись видимою — вихід з паузи веде на робочий стіл, а не на власний Home
    // застосунку. onDone() спершу прибирає GATE_PAUSE (і будь-що під ним у back stack, напр.
    // GatesScreen, якщо ворота відкрились, поки застосунок уже був на цьому екрані) — інакше
    // moveTaskToBack показав би цей проміжний екран при наступному відкритті Tepera з лаунчера.
    LaunchedEffect(state.finished) {
        if (state.finished) {
            onDone()
            context.findActivity()?.moveTaskToBack(true)
        }
    }

    BackHandler(enabled = !state.loading && !state.finished) { viewModel.cancel() }

    if (state.loading) return

    // Розкладка за Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, вузли 2:3553 (Inhale) і 2:3521
    // (Exhale), кадр 375x784, 1px = 1dp: поля 20, "Instagram" по центру 18sp, бейдж 215dp,
    // підпис дихання 27sp, кнопки Big (TeperaButton) з проміжком 8.
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 120.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = state.appLabel,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TeperaPalette.buttonBrand
        )

        val breath = rememberBreathState()

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BreathingBadge(scale = breath.scale, number = state.remainingSeconds)
            Text(
                text = stringResource(if (breath.inhaling) R.string.gate_pause_inhale else R.string.gate_pause_exhale),
                fontSize = 27.sp,
                fontWeight = FontWeight.Medium,
                color = TeperaPalette.buttonBrandDark,
                modifier = Modifier.padding(top = 32.dp)
            )
        }

        Spacer(Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "Продовжити" неактивна (напівпрозора — це вже вбудований disabled-стан TeperaButton),
            // доки йде очікування. "Вийти" — суцільна Primary (текст лишається "Вийти", не "Do not
            // open" з макета — окреме рішення користувача, підтверджене цією сесією).
            TeperaButton(
                text = stringResource(R.string.gate_pause_continue_action),
                onClick = { viewModel.continueToApp() },
                enabled = state.canContinue,
                size = TeperaButtonSize.Big,
                type = TeperaButtonType.Secondary,
                modifier = Modifier.weight(1f)
            )
            TeperaButton(
                text = stringResource(R.string.gate_pause_exit_action),
                onClick = { viewModel.cancel() },
                size = TeperaButtonSize.Big,
                type = TeperaButtonType.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Тривалість одного напрямку (тільки ріст АБО тільки стиск) дихальної анімації — за прямим
// запитом користувача: 4 с в кожен бік, повний цикл вдих+видих = 8 с.
private const val BREATH_DIRECTION_MILLIS = 4000
// Крайні масштаби — не 0/1, а звужений діапазон навколо 1.0: бейдж помітно "дихає", але не
// з'їжджає з-під центрованого тексту/підпису нижче й не виглядає карикатурно.
// Figma: бейдж 145dp на вдиху (початок) і 215dp на видиху — базовий розмір 215dp, малий масштаб
// 145/215.
private const val BREATH_SCALE_SMALL = 145f / 215f
private const val BREATH_SCALE_LARGE = 1f

private val BADGE_SIZE = 215.dp
// Figma: заливка бейджа #DCF6ED (з SVG-ассета Star 1), цифра #005E3E 96sp — цифра НЕ масштабується.
private val BADGE_FILL = Color(0xFFDCF6ED)
private val BADGE_NUMBER = Color(0xFF005E3E)

private data class BreathState(val scale: Animatable<Float, AnimationVector1D>, val inhaling: Boolean)

/**
 * Дихальна анімація — чисто UI-стан (`Animatable`, не `ViewModel`): починає рости ВІДРАЗУ з
 * першого кадру екрана (за прямим запитом користувача — попередня версія на `animateFloatAsState`
 * стояла нерухомо до першої зміни фази) і крутиться безперервно, поки екран видимий, цілком
 * незалежно від таймера очікування `GatePauseViewModel` (раніше було навпаки — саме це число й
 * було "дихальним циклом"; тепер число рахує очікування, а дихання — суто атмосферний фон).
 *
 * **Повертає сам [Animatable], НЕ `.value`** — реальний баг, знайдений користувачем на Huawei P9
 * (Android 8, Kirin 955, значно слабший за Samsung S23): попередня версія читала `scale.value`
 * ТУТ, у тілі цієї `@Composable`-функції, викликаної прямо зі scope `GatePauseScreen` — кожен
 * кадр анімації (до 60/с) перечитував це значення й перекомпоновував УВЕСЬ `GatePauseScreen`
 * (кнопки, текст спроб, усе), а `BreathingBadge` наново рахував 200-точковий тригонометричний
 * контур квітки щокадру, хоча він ніколи не змінюється. На S23 це губилось у запасі
 * продуктивності; на P9 перевантажений рекомпозицією рендер губив кадри й виглядав як мигання
 * ЗАМІСТЬ плавної зміни розміру. Тепер `scale.value` читається лише всередині
 * `Modifier.graphicsLayer { }` у [BreathingBadge] — це відкладає читання на фазу
 * layout/draw і НЕ викликає перекомпозицію composable-дерева взагалі, лише дешеве оновлення
 * трансформації шару.
 */
@Composable
private fun rememberBreathState(): BreathState {
    val scale = remember { Animatable(BREATH_SCALE_SMALL) }
    var inhaling by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            inhaling = true
            scale.animateTo(BREATH_SCALE_LARGE, tween(BREATH_DIRECTION_MILLIS, easing = LinearEasing))
            inhaling = false
            scale.animateTo(BREATH_SCALE_SMALL, tween(BREATH_DIRECTION_MILLIS, easing = LinearEasing))
        }
    }
    return BreathState(scale, inhaling)
}

/**
 * Дихальний бейдж — м'яка "квіткова" пляма (параметрична крива radius(kut) = R·(1 - w + w·cos(k·kut)),
 * не справжня SVG-графіка) з числом усередині. Форма контуру ЗАВЖДИ однакова (детермінована, без
 * випадковості) — кешується через `remember` (не рахується щокадру, див. [rememberBreathState] —
 * реальний баг, знайдений на Huawei P9). [scale] керується ззовні через `Modifier.graphicsLayer`,
 * без перекомпозиції. За прямим запитом користувача: [number] — `null`, щойно очікування минає
 * (цифра зникає, дихальна анімація лишається й далі крутиться).
 */
@Composable
private fun BreathingBadge(scale: Animatable<Float, AnimationVector1D>, number: Int?, modifier: Modifier = Modifier) {
    val diameterPx = with(LocalDensity.current) { BADGE_SIZE.toPx() }
    val path = remember(diameterPx) {
        scallopedBlobPath(diameter = diameterPx, lobes = 12, wobbleFraction = 0.07f)
    }
    Box(modifier = modifier.size(BADGE_SIZE), contentAlignment = Alignment.Center) {
        // Масштабується лише сама форма; цифра нижче лишається фіксованого розміру (Figma: "5" —
        // 96px в обох станах).
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {
            drawPath(path, color = BADGE_FILL)
        }
        if (number != null) {
            Text(
                text = number.toString(),
                fontSize = 96.sp,
                fontWeight = FontWeight.Medium,
                color = BADGE_NUMBER
            )
        }
    }
}

private fun scallopedBlobPath(diameter: Float, lobes: Int, wobbleFraction: Float): Path {
    val path = Path()
    val center = diameter / 2f
    val baseRadius = diameter / 2f
    val segments = 200
    for (i in 0..segments) {
        val t = i / segments.toFloat()
        val angle = t * 2f * Math.PI.toFloat()
        val r = baseRadius * (1f - wobbleFraction + wobbleFraction * cos(lobes * angle))
        val x = center + r * cos(angle)
        val y = center + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

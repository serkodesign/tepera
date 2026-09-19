package com.serkodesign.tepera.ui.gates

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.GateEventRepository
import com.serkodesign.tepera.data.repository.GateRepository
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
            (context as? Activity)?.moveTaskToBack(true)
        }
    }

    BackHandler(enabled = !state.loading && !state.finished) { viewModel.cancel() }

    if (state.loading) return

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 40.dp)
    ) {
        Text(
            text = state.appLabel,
            style = MaterialTheme.typography.labelLarge,
            color = TeperaPalette.brandAccent
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
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        Text(
            text = pluralStringResource(R.plurals.gate_pause_attempts_text, state.attemptsToday, state.attemptsToday),
            style = MaterialTheme.typography.bodyMedium,
            color = TeperaPalette.brandAccent.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.continueToApp() },
                enabled = state.canContinue,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, TeperaPalette.brandAccent),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TeperaPalette.brandAccent,
                    disabledContentColor = TeperaPalette.brandAccent
                ),
                // За прямим запитом користувача: непрозорість 0.5, доки таймер очікування йде;
                // 1.0, щойно кнопка стає активною — Modifier.alpha дim'ить УСЮ кнопку (рамку,
                // фон, текст) як єдине ціле, а не лише колір тексту (disabledContentColor вище
                // тому лишається БЕЗ власної альфи — інакше подвійне затемнення).
                modifier = Modifier.weight(1f).height(52.dp).alpha(if (state.canContinue) 1f else 0.5f)
            ) {
                Text(stringResource(R.string.gate_pause_continue_action))
            }
            Button(
                onClick = { viewModel.cancel() },
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeperaPalette.brandAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text(stringResource(R.string.gate_pause_exit_action))
            }
        }
    }
}

// Тривалість одного напрямку (тільки ріст АБО тільки стиск) дихальної анімації — за прямим
// запитом користувача: 4 с в кожен бік, повний цикл вдих+видих = 8 с.
private const val BREATH_DIRECTION_MILLIS = 4000
// Крайні масштаби — не 0/1, а звужений діапазон навколо 1.0: бейдж помітно "дихає", але не
// з'їжджає з-під центрованого тексту/підпису нижче й не виглядає карикатурно.
private const val BREATH_SCALE_SMALL = 0.82f
private const val BREATH_SCALE_LARGE = 1.18f

private val BADGE_SIZE = 180.dp

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
    Box(
        modifier = modifier
            .size(BADGE_SIZE)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPath(path, color = Color.White.copy(alpha = 0.55f))
        }
        if (number != null) {
            Text(
                text = number.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = TeperaPalette.brandAccent
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

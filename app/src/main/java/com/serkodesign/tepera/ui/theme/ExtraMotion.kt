package com.serkodesign.tepera.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.serkodesign.tepera.util.MotionStore
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * "Додаткові анімації": тумблер із Налаштувань І системна шкала анімацій (0 = вимкнено в розробницьких опціях або
 * "Прибрати анімації" в доступності). Усе декоративне (рідина, числа, пульс) питає цей прапор; базові переходи — ні.
 */
@Composable
fun extraAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    MotionStore.load(context)
    val systemOn = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    return MotionStore.extraAnimations() && systemOn
}

/** Одноразові "вступні" анімації за життя процесу: повторні відкриття Home (вкладки) не програють їх знову. */
object IntroPlayed {
    var counters = false
    var orb = false
}

/** Ціле число, що плавно "докручується" до [target] (вперше за сесію — від нуля). Без анімацій — одразу [target]. */
@Composable
fun rememberCountedInt(target: Int): Int {
    val motion = extraAnimationsEnabled()
    val anim = remember { Animatable(if (motion && !IntroPlayed.counters) 0f else target.toFloat()) }
    LaunchedEffect(target, motion) {
        if (motion) {
            anim.animateTo(target.toFloat(), tween(900, easing = TeperaMotion.EmphasizedDecelerate))
            IntroPlayed.counters = true
        } else {
            anim.snapTo(target.toFloat())
        }
    }
    return anim.value.roundToInt()
}

/** Фаза хвилі 0..2π, що безшовно біжить; без анімацій — стоїть на місці й не будить кадри. */
@Composable
fun rememberWavePhase(motion: Boolean, durationMillis: Int): State<Float> =
    if (motion) {
        rememberInfiniteTransition(label = "wavePhase").animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
            label = "phase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

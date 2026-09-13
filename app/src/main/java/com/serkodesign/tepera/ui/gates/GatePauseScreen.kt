package com.serkodesign.tepera.ui.gates

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.GateEventRepository
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * T-5 (tepera-dev-spec.md, FR-G частина 2). Буквальна вимога приймання: "на екрані немає нічого,
 * крім назви застосунку, лічильника і кнопки. Ні заклику, ні цитати, ні 'ти впевнений?', ні
 * прогресу 'ти вже 5 разів сьогодні'." **За прямим запитом користувача екран тепер малюється на
 * тому самому градієнтному фоні, що решта застосунку** (`Routes.GATE_PAUSE` додано в
 * `GRADIENT_ROUTES`, TeperaNavHost) — компонування лишається буквально мінімальним (жодного
 * нового елемента), тонується лише колір прогрес-бару.
 *
 * **T-6 (tepera-dev-spec.md): `BackHandler` перехоплює системну "назад" і викликає ТОЙ САМИЙ
 * `viewModel.cancel()`, що кнопка "Не зараз"** — раніше (T-5) "назад" просто спливала
 * `NavBackStackEntry` без виклику `cancel()`, і це давало однаковий РЕЗУЛЬТАТ (пауза закривалась,
 * цільовий застосунок не запускався), але тепер `cancel()` ще й пише подію T-6 — без явного
 * перехоплення подія фіксувалась би лише для тапу по кнопці, а вихід "назад" (продуктово те
 * саме "не зараз") лишався б непорахованим.
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

    // За прямим запитом користувача: незалежно від того, "Не зараз" чи автозапуск, Tepera не
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

    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(state.appLabel, style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(
                progress = { (state.delaySeconds - state.remainingSeconds).toFloat() / state.delaySeconds },
                modifier = Modifier.fillMaxWidth(),
                color = TeperaPalette.brandAccent,
                trackColor = TeperaPalette.cardTranslucent
            )
            Text(state.remainingSeconds.toString(), style = MaterialTheme.typography.displayLarge)
            TextButton(onClick = { viewModel.cancel() }) {
                Text(stringResource(R.string.gate_pause_not_now))
            }
        }
    }
}

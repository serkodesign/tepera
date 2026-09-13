package com.serkodesign.tepera.ui.gates

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.entity.GateEventResult
import com.serkodesign.tepera.data.repository.GateEventRepository
import com.serkodesign.tepera.data.repository.GateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GatePauseUiState(
    val loading: Boolean = true,
    val appLabel: String = "",
    val delaySeconds: Int = 1,
    val remainingSeconds: Int = 1,
    val finished: Boolean = false
)

/**
 * T-5 (tepera-dev-spec.md, FR-G частина 2): екран паузи воріт. Тап по закріпленому ярлику
 * (T-4) відкриває `MainActivity` з `GateRepository.GATE_TARGET_PACKAGE_EXTRA` в extras —
 * `TeperaNavHost` одразу навігує сюди, а цей ViewModel сам вирішує, показувати паузу чи ні.
 *
 * **Дебаунс і "вимкнено на сьогодні" перевіряються ДО першого кадру UI** (`GateRepository.
 * shouldSkipPause()`) — доки перевірка не завершилась, `uiState.loading = true` й екран не
 * малює нічого (буквальна вимога приймання: "нічого, крім назви застосунку, лічильника і
 * кнопки" — під час перевірки немає жодного з цього, тож порожній кадр її не порушує).
 *
 * **T-6 (tepera-dev-spec.md): [GateEventRepository.record] пишеться ЛИШЕ коли пауза реально
 * була показана** ([screenShown]) — шлях "пропустити паузу" (дебаунс/вимкнено на сьогодні) не
 * створює події: там не було моменту вибору, нема що фіксувати як "рішення" людини.
 */
class GatePauseViewModel(
    private val context: Context,
    private val gateRepository: GateRepository,
    private val gateEventRepository: GateEventRepository,
    private val packageName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GatePauseUiState())
    val uiState: StateFlow<GatePauseUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var screenShown = false

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        if (gateRepository.shouldSkipPause(packageName)) {
            launchTarget()
            return
        }
        val delaySeconds = gateRepository.getDelaySeconds(packageName)
        if (delaySeconds == null) {
            launchTarget()
            return
        }
        screenShown = true
        _uiState.value = GatePauseUiState(
            loading = false,
            appLabel = resolveLabel(packageName),
            delaySeconds = delaySeconds,
            remainingSeconds = delaySeconds
        )
        countdownJob = viewModelScope.launch {
            for (remaining in delaySeconds - 1 downTo 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
            }
            proceed()
        }
    }

    /**
     * "Не зараз" — і кнопка, і системна кнопка "назад" (`GatePauseScreen` перехоплює `BackHandler`
     * і викликає САМЕ цей метод, не покладається на дефолтне згортання `NavBackStackEntry`): без
     * цього подія T-6 фіксувалась би лише для тапу по кнопці, а вихід "назад" лишався б
     * непорахованим "рішенням", хоча продуктово це те саме "не зараз".
     */
    fun cancel() {
        countdownJob?.cancel()
        if (screenShown) {
            viewModelScope.launch { gateEventRepository.record(packageName, GateEventResult.CANCELLED) }
        }
        _uiState.value = _uiState.value.copy(finished = true)
    }

    private suspend fun proceed() {
        gateEventRepository.record(packageName, GateEventResult.PROCEEDED)
        gateRepository.recordProceed(packageName)
        launchTarget()
    }

    private fun launchTarget() {
        gateRepository.getLaunchIntent(packageName)?.let { context.startActivity(it) }
        _uiState.value = _uiState.value.copy(finished = true)
    }

    private fun resolveLabel(packageName: String): String {
        val pm = context.packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    class Factory(
        private val context: Context,
        private val gateRepository: GateRepository,
        private val gateEventRepository: GateEventRepository,
        private val packageName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GatePauseViewModel(context.applicationContext, gateRepository, gateEventRepository, packageName) as T
    }
}

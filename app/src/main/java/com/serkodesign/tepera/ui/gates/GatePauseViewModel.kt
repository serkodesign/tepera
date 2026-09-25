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
    // null = очікування завершилось (канонічний "нема чого рахувати" стан) — цифра ховається
    // (за прямим запитом користувача).
    val remainingSeconds: Int? = null,
    // CC-6: індекс тексту з `R.array.gate_texts` (ротація «мішком»); сам рядок підставляє екран — з урахуванням мови застосунку.
    val textIndex: Int = 0,
    val canContinue: Boolean = false,
    val finished: Boolean = false
)

/**
 * T-5 (tepera-dev-spec.md, FR-G частина 2): екран паузи воріт. Тап по закріпленому ярлику
 * (T-4) відкриває `MainActivity` з `GateRepository.GATE_TARGET_PACKAGE_EXTRA` в extras —
 * `TeperaNavHost` одразу навігує сюди, а цей ViewModel сам вирішує, показувати паузу чи ні.
 *
 * **Дебаунс і "вимкнено на сьогодні" перевіряються ДО першого кадру UI** (`GateRepository.
 * shouldSkipPause()`) — доки перевірка не завершилась, `uiState.loading = true` й екран не
 * малює нічого.
 *
 * **T-6 (tepera-dev-spec.md): [GateEventRepository.record] пишеться ЛИШЕ коли пауза реально
 * була показана** ([screenShown]) — шлях "пропустити паузу" (дебаунс/вимкнено на сьогодні) не
 * створює події: там не було моменту вибору, нема що фіксувати як "рішення" людини.
 *
 * **Редизайн за прямим запитом користувача (не T-5/T-6, свідома зміна попередньо задокументованої
 * вимоги приймання "нічого, крім назви застосунку, лічильника й кнопки"):**
 * 1. [remainingSeconds] рахує ГОЛОВНИЙ таймер очікування (5/10/20 с, обране в GatesScreen)
 *    щосекунди вниз, доки не стане активною кнопка "Продовжити" — тоді ховається (`null`).
 *    **Дихальна анімація "квітки" (ріст/стиск) — суто візуальна, живе цілком у `GatePauseScreen`
 *    (Compose `Animatable`), ніяк не пов'язана з цим лічильником** — за прямим запитом
 *    користувача (раніше число саме й було "дихальним циклом", крутилось по колу незалежно від
 *    таймера очікування; тепер навпаки — число рахує ОЧІКУВАННЯ, а дихання крутиться само по
 *    собі, безперервно, з першого кадру екрана).
 * 2. Автозапуск цільового застосунку по завершенню очікування ПРИБРАНО — таймер лише знімає
 *    [canContinue] у false→true, а сам перехід відбувається виключно по тапу "Продовжити"
 *    ([continueToApp]), кнопка неактивна (і напівпрозора — `GatePauseScreen`), доки очікування
 *    не мине.
 */
class GatePauseViewModel(
    private val context: Context,
    private val gateRepository: GateRepository,
    private val gateEventRepository: GateEventRepository,
    private val packageName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GatePauseUiState())
    val uiState: StateFlow<GatePauseUiState> = _uiState.asStateFlow()

    private var waitJob: Job? = null
    private var screenShown = false

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        if (gateRepository.shouldSkipPause(packageName)) {
            launchTarget()
            return
        }
        val delaySeconds = gateRepository.delayForShow(packageName)
        if (delaySeconds == null) {
            launchTarget()
            return
        }
        screenShown = true
        _uiState.value = GatePauseUiState(
            loading = false,
            appLabel = resolveLabel(packageName),
            remainingSeconds = delaySeconds,
            textIndex = gateRepository.nextTextIndex()
        )

        // Показує delaySeconds..1 (ніколи 0) — по секунді на значення, і лише ПІСЛЯ останньої
        // секунди вмикає "Продовжити" й ховає число.
        waitJob = viewModelScope.launch {
            for (remaining in delaySeconds - 1 downTo 1) {
                delay(1000)
                _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
            }
            delay(1000)
            _uiState.value = _uiState.value.copy(canContinue = true, remainingSeconds = null)
        }
    }

    /**
     * "Продовжити" — активна лише коли [GatePauseUiState.canContinue]; заміняє попередній
     * автозапуск по завершенню таймера (за прямим запитом користувача).
     */
    fun continueToApp() {
        if (!_uiState.value.canContinue) return
        waitJob?.cancel()
        viewModelScope.launch { proceed() }
    }

    /**
     * "Вийти" — і кнопка, і системна кнопка "назад" (`GatePauseScreen` перехоплює `BackHandler`
     * і викликає САМЕ цей метод, не покладається на дефолтне згортання `NavBackStackEntry`): без
     * цього подія T-6 фіксувалась би лише для тапу по кнопці, а вихід "назад" лишався б
     * непорахованим "рішенням", хоча продуктово це те саме скасування.
     */
    fun cancel() {
        waitJob?.cancel()
        if (screenShown) {
            viewModelScope.launch {
                gateEventRepository.record(packageName, GateEventResult.CANCELLED)
            }
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
            GatePauseViewModel(
                context.applicationContext, gateRepository, gateEventRepository, packageName
            ) as T
    }
}

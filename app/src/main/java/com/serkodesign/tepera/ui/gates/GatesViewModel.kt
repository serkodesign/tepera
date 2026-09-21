package com.serkodesign.tepera.ui.gates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.AppGateEntity
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private const val DEFAULT_DELAY_SECONDS = 10

data class GateUiState(
    val gate: AppGateEntity,
    val app: InstalledAppInfo
)

data class GatesUiState(
    val loading: Boolean = true,
    val pinShortcutSupported: Boolean = true,
    val gates: List<GateUiState> = emptyList(),
    val availableApps: List<InstalledAppInfo> = emptyList(),
    val gatesPausedUntilMillis: Long = 0L
)

/**
 * T-4 (tepera-dev-spec.md): екран "Застосунки з затримкою". `availableApps` — застосунки з
 * реальною історією використання (`InstalledAppsProvider`, той самий підхід, що Exclusion List),
 * за винятком уже загейчених — вибір із довільної довгої системної номенклатури пакетів людині
 * не по силах, а список використаних застосунків уже й так відфільтрований і відсортований.
 */
class GatesViewModel(
    private val gateRepository: GateRepository,
    private val installedAppsProvider: InstalledAppsProvider,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _availableApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _loading = MutableStateFlow(true)

    val uiState: StateFlow<GatesUiState> = combine(
        gateRepository.gates,
        _availableApps,
        _loading,
        settingsStore.gatesPausedUntilMillis
    ) { gates, availableApps, loading, pausedUntil ->
        val appsByPackage = availableApps.associateBy { it.packageName }
        val gatedPackages = gates.map { it.packageName }.toSet()
        GatesUiState(
            loading = loading,
            pinShortcutSupported = gateRepository.isPinShortcutSupported(),
            gates = gates.mapNotNull { gate ->
                val app = appsByPackage[gate.packageName]
                    ?: InstalledAppInfo(gate.packageName, gate.packageName, null)
                GateUiState(gate, app)
            },
            availableApps = availableApps.filterNot { it.packageName in gatedPackages },
            gatesPausedUntilMillis = pausedUntil
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GatesUiState())

    init {
        refresh()
    }

    /**
     * Реальний баг, знайдений користувачем: `pruneRemovedShortcuts()` викликався лише тут, у
     * `init{}` — раз за життя ViewModel. Якщо людина створює ворота, виходить із застосунку (без
     * закриття самого `GatesScreen` у навстеку), вилучає ярлик з робочого столу і повертається —
     * той самий екземпляр `GatesViewModel` лишається живим, `init{}` вдруге не спрацьовує, і рядок
     * воріт зостається "активним" у списку, хоча ярлика вже нема. Фікс — той самий патерн, що
     * `PauseViewModel`/`BalanceViewModel`: публічний `refresh()`, викликаний і тут, і з
     * `LifecycleResumeEffect` у `GatesScreen` — синхронізація тепер відбувається щоразу, коли
     * екран повертається на передній план, не лише один раз при створенні.
     */
    fun refresh() {
        viewModelScope.launch {
            // За прямим запитом користувача: синхронізувати список воріт з реальним станом
            // закріплених ярликів ПЕРЕД тим, як список узагалі відобразиться — інакше рядок
            // для вже видаленого з робочого столу ярлика встиг би на мить показатись.
            gateRepository.pruneRemovedShortcuts()
            _availableApps.value = installedAppsProvider.listUsedApps()
            _loading.value = false
        }
    }

    suspend fun createGate(app: InstalledAppInfo, delaySeconds: Int = DEFAULT_DELAY_SECONDS): Boolean =
        gateRepository.createGate(app.packageName, app.label, delaySeconds)

    fun removeGate(packageName: String) {
        viewModelScope.launch { gateRepository.removeGate(packageName) }
    }

    fun setDelaySeconds(packageName: String, delaySeconds: Int) {
        viewModelScope.launch { gateRepository.setDelaySeconds(packageName, delaySeconds) }
    }

    fun markOriginalIconHandled(packageName: String) {
        viewModelScope.launch { gateRepository.markOriginalIconHandled(packageName) }
    }

    /** Розділ 2.3 документа ("автономія важливіша за ефективність") — один тап, без підтвердження. */
    fun toggleGatesPausedForToday() {
        viewModelScope.launch {
            val currentlyPaused = uiState.value.gatesPausedUntilMillis > System.currentTimeMillis()
            settingsStore.setGatesPausedUntilMillis(if (currentlyPaused) 0L else endOfTodayMillis())
        }
    }

    private fun endOfTodayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    class Factory(
        private val gateRepository: GateRepository,
        private val installedAppsProvider: InstalledAppsProvider,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GatesViewModel(gateRepository, installedAppsProvider, settingsStore) as T
    }
}

package com.serkodesign.tepera.ui.home

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.serkodesign.tepera.data.UpdateChecker
import com.serkodesign.tepera.data.WhatsNew
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.util.LocaleStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/** [visible] — пропозиція оновитись; [restartReady] — оновлення вже завантажено, лишилось перезапустити. */
data class UpdateUiState(
    val visible: Boolean = false,
    val whatsNew: String = "",
    val restartReady: Boolean = false
)

/**
 * CC-9: м'яка пропозиція оновитись через Google Play In-App Updates (ГНУЧКИЙ режим): людина
 * погоджується в нашому діалозі → Play завантажує оновлення у фоні, застосунок працює далі → після
 * завантаження — тиха пропозиція перезапуститись. НЕ блокує користування. "Не зараз" запам'ятовує
 * саме цю версію ([SettingsStore.updateDismissedVersionCode]) — повторно з'явиться лише для
 * новішого релізу, не при кожному відкритті Home.
 */
class UpdateViewModel(
    private val appContext: Context,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)
    private var playInfo: AppUpdateInfo? = null
    private var pendingVersionCode: Long = 0L

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            _uiState.value = UpdateUiState(restartReady = true)
        }
    }

    init {
        manager.registerListener(installListener)
        viewModelScope.launch {
            val info = UpdateChecker.checkPlay(manager) ?: return@launch
            // Оновлення вже завантажене в попередній сесії — лишилось лише завершити.
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _uiState.value = UpdateUiState(restartReady = true)
                return@launch
            }
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@launch
            if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return@launch
            val versionCode = info.availableVersionCode().toLong()
            if (versionCode <= settingsStore.updateDismissedVersionCode.first()) return@launch
            playInfo = info
            pendingVersionCode = versionCode
            _uiState.value = UpdateUiState(visible = true, whatsNew = whatsNewText(UpdateChecker.fetchWhatsNew()))
        }
    }

    private fun whatsNewText(info: WhatsNew): String {
        val explicitTag = LocaleStore.getLanguageTag(appContext)
        val languageTag = explicitTag.ifEmpty { Locale.getDefault().language }
        val text = if (languageTag.startsWith("uk")) info.uk else info.en
        return text.ifBlank { info.uk.ifBlank { info.en } }
    }

    /** "Не зараз" — тиха відмова, та сама версія вдруге не спливе. */
    fun dismiss() {
        val versionCode = pendingVersionCode
        if (versionCode > 0) {
            viewModelScope.launch { settingsStore.setUpdateDismissedVersionCode(versionCode) }
        }
        _uiState.value = UpdateUiState()
    }

    /** "Оновити" — Play питає підтвердження й качає у фоні; про готовність скаже [installListener]. */
    fun startUpdate(activity: Activity) {
        val info = playInfo
        _uiState.value = UpdateUiState()
        if (info == null) return
        runCatching {
            manager.startUpdateFlow(info, activity, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
        }
    }

    /** "Перезапустити" — Play встановлює завантажене оновлення й перезапускає застосунок. */
    fun completeUpdate() {
        _uiState.value = UpdateUiState()
        manager.completeUpdate()
    }

    /** "Пізніше" на етапі перезапуску — Play однаково встановить оновлення при наступному закритті застосунку. */
    fun postponeRestart() {
        _uiState.value = UpdateUiState()
    }

    override fun onCleared() {
        manager.unregisterListener(installListener)
    }

    class Factory(
        private val appContext: Context,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UpdateViewModel(appContext, settingsStore) as T
    }
}

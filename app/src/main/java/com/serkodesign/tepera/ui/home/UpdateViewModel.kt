package com.serkodesign.tepera.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.UpdateChecker
import com.serkodesign.tepera.data.UpdateInfo
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.util.LocaleStore
import com.serkodesign.tepera.util.PlayStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

data class UpdateUiState(val visible: Boolean = false, val whatsNew: String = "")

/**
 * CC-9: м'яка пропозиція оновитись — [UpdateChecker] питає Firebase Remote Config, чи вийшла
 * новіша версія. НЕ блокує користування (за прямим запитом користувача: "м'яка пропозиція з
 * повідомленням, що додалось", не обов'язковий форс-апдейт). Закриття ("Не зараз") запам'ятовує
 * саме цю версію ([SettingsStore.updateDismissedVersionCode]) — повторно з'явиться лише для
 * новішого релізу, не при кожному відкритті Home.
 */
class UpdateViewModel(
    private val appContext: Context,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private var pendingVersionCode: Long = 0L

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val info = UpdateChecker.fetchLatest(appContext) ?: return@launch
            val dismissed = settingsStore.updateDismissedVersionCode.first()
            if (info.versionCode > dismissed) {
                pendingVersionCode = info.versionCode
                _uiState.value = UpdateUiState(visible = true, whatsNew = whatsNewText(info))
            }
        }
    }

    private fun whatsNewText(info: UpdateInfo): String {
        val explicitTag = LocaleStore.getLanguageTag(appContext)
        val languageTag = explicitTag.ifEmpty { Locale.getDefault().language }
        val text = if (languageTag.startsWith("uk")) info.whatsNewUk else info.whatsNewEn
        return text.ifBlank { info.whatsNewUk.ifBlank { info.whatsNewEn } }
    }

    /** "Не зараз" — тиха відмова, та сама версія вдруге не спливе. */
    fun dismiss() {
        val versionCode = pendingVersionCode
        if (versionCode > 0) {
            viewModelScope.launch { settingsStore.setUpdateDismissedVersionCode(versionCode) }
        }
        _uiState.value = UpdateUiState()
    }

    /** "Оновити" — відкриває лістинг Tepera в Play Store; закриває діалог одразу (сам перехід триває поза застосунком). */
    fun openStoreListing() {
        PlayStore.openListing(appContext)
        _uiState.value = UpdateUiState()
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

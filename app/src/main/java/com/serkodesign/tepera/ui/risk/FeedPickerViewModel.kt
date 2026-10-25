package com.serkodesign.tepera.ui.risk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedPickerState(
    val loading: Boolean = true,
    val apps: List<InstalledAppInfo> = emptyList(),
    val suggested: Set<String> = emptySet(),
    val feed: Set<String> = emptySet()
)

class FeedPickerViewModel(private val app: TeperaApp) : ViewModel() {
    private val _state = MutableStateFlow(FeedPickerState())
    val state: StateFlow<FeedPickerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            app.feedAppRepository.importLegacyIfEmpty(app.settingsStore.feedPackages.first())
            val feed = app.feedAppRepository.packages()
            val apps = app.installedAppsProvider.listUsedApps()
            _state.update { it.copy(apps = apps, feed = feed, loading = false) }
            // Підказки рахуються довше за список — підтягуються окремо й лише міняють порядок.
            val suggested = app.feedSuggestionProvider.suggest()
            _state.update { it.copy(suggested = suggested) }
        }
    }

    fun toggle(info: InstalledAppInfo) {
        viewModelScope.launch {
            val on = info.packageName !in _state.value.feed
            app.feedAppRepository.setConfirmed(info.packageName, info.label, on)
            _state.update { it.copy(feed = app.feedAppRepository.packages()) }
        }
    }

    class Factory(private val app: TeperaApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FeedPickerViewModel(app) as T
    }
}

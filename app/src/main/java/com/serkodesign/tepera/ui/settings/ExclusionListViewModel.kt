package com.serkodesign.tepera.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** FR-3.5: список застосунків з історією використання (30 днів) + перемикачі виключення. */
class ExclusionListViewModel(
    private val installedAppsProvider: InstalledAppsProvider,
    private val excludedAppRepository: ExcludedAppRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val apps: StateFlow<List<InstalledAppInfo>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val excludedPackageNames: StateFlow<Set<String>> = excludedAppRepository.observeAll()
        .map { list -> list.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            _apps.value = installedAppsProvider.listUsedApps()
            _loading.value = false
        }
    }

    fun setExcluded(app: InstalledAppInfo, excluded: Boolean) {
        viewModelScope.launch {
            if (excluded) {
                excludedAppRepository.add(app.packageName, app.label)
            } else {
                excludedAppRepository.removeByPackageName(app.packageName)
            }
        }
    }

    class Factory(
        private val installedAppsProvider: InstalledAppsProvider,
        private val excludedAppRepository: ExcludedAppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExclusionListViewModel(installedAppsProvider, excludedAppRepository) as T
    }
}

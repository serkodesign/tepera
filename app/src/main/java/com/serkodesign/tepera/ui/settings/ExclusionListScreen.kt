package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import com.serkodesign.tepera.data.repository.InstalledAppsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExclusionListScreen(
    installedAppsProvider: InstalledAppsProvider,
    excludedAppRepository: ExcludedAppRepository,
    onBack: () -> Unit
) {
    val viewModel: ExclusionListViewModel = viewModel(
        factory = ExclusionListViewModel.Factory(installedAppsProvider, excludedAppRepository)
    )
    val apps by viewModel.apps.collectAsState()
    val excludedPackageNames by viewModel.excludedPackageNames.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exclusion_list_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            apps.isEmpty() -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.exclusion_list_empty)) }

            else -> LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                item {
                    Text(
                        text = stringResource(R.string.exclusion_list_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(apps, key = { it.packageName }) { app ->
                    val isExcluded = app.packageName in excludedPackageNames
                    ListItem(
                        headlineContent = { Text(app.label) },
                        leadingContent = { AppIcon(app) },
                        trailingContent = {
                            Switch(
                                checked = isExcluded,
                                onCheckedChange = { checked -> viewModel.setExcluded(app, checked) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIcon(app: InstalledAppInfo) {
    val icon = app.resolvedIcon
    if (icon != null) {
        Image(
            bitmap = icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
    } else {
        Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(40.dp))
    }
}

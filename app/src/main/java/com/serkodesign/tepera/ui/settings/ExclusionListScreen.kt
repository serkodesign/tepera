package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.teperaSwitchColors

/**
 * Стиль перенесений з Figma-фрейму Everyday_Designs (node 1951:2025, "Setings - Excluded
 * apps"): дві секції — "Виключені" (уже виключені застосунки) і "Усі" (решта) — замість
 * єдиного плаского списку. У мокапі демо-рядки показують ПОРОЖНІ кольорові кружки без іконки
 * (Figma не мала реальних даних для них) — за запитом користувача тут завжди РЕАЛЬНА іконка
 * застосунку (InstalledAppsProvider вже резолвить її через PackageManager), і лише коли
 * резолвінг справді не вдався (рідкісний край, задокументований у InstalledAppsProvider),
 * показуємо нейтральну заглушку-іконку — не порожній кружок.
 */
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

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.exclusion_list_screen_title), onBack = onBack)

            when {
                loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                apps.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.exclusion_list_empty))
                }

                else -> {
                    val (excluded, others) = apps.partition { it.packageName in excludedPackageNames }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.exclusion_list_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                        if (excluded.isNotEmpty()) {
                            item { GlassSectionHeader(stringResource(R.string.exclusion_list_section_excluded)) }
                            items(excluded, key = { it.packageName }) { app ->
                                AppRow(app = app, isExcluded = true, onToggle = { viewModel.setExcluded(app, false) })
                            }
                        }
                        item { GlassSectionHeader(stringResource(R.string.exclusion_list_section_all)) }
                        items(others, key = { it.packageName }) { app ->
                            AppRow(app = app, isExcluded = false, onToggle = { viewModel.setExcluded(app, true) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledAppInfo, isExcluded: Boolean, onToggle: () -> Unit) {
    GlassRow(
        label = app.label,
        leading = { AppIcon(app) },
        trailing = {
            Switch(
                checked = isExcluded,
                onCheckedChange = { onToggle() },
                colors = teperaSwitchColors()
            )
        }
    )
}

@Composable
private fun AppIcon(app: InstalledAppInfo) {
    val icon = app.resolvedIcon
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(TeperaPalette.brandAccentSoft),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
        } else {
            Icon(Icons.Filled.Apps, contentDescription = null, tint = TeperaPalette.brandAccent)
        }
    }
}

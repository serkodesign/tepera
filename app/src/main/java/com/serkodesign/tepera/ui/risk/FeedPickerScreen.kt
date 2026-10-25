package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.InstalledAppInfo
import com.serkodesign.tepera.ui.settings.AppIcon
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.TeperaHint
import com.serkodesign.tepera.ui.theme.TeperaLoadingIndicator
import com.serkodesign.tepera.ui.theme.TeperaSearchField
import com.serkodesign.tepera.ui.theme.teperaSwitchColors

/**
 * v3.0 MVP, Етап 3 («Вибір стрічок»): людина сама підтверджує, які застосунки враховуються. Підказки (вечірні
 * застосунки) лише піднімають рядки вгору й нічого не вмикають.
 */
@Composable
fun FeedPickerScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val viewModel: FeedPickerViewModel = viewModel(factory = FeedPickerViewModel.Factory(app))
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    val searchActive = searchFocused || query.isNotEmpty()

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!searchActive) GlassScreenHeader(title = stringResource(R.string.v3_feed_title), onBack = onBack)
            if (!state.loading && state.apps.isNotEmpty()) {
                TeperaSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.search_apps_placeholder),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    active = searchActive,
                    onFocusChange = { searchFocused = it }
                )
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { TeperaLoadingIndicator() }
                state.apps.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.v3_feed_empty), style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    val needle = query.trim()
                    val visible = state.apps.filter { needle.isEmpty() || it.label.contains(needle, ignoreCase = true) }
                    val (suggested, others) = visible.partition { it.packageName in state.suggested }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!searchActive) {
                            item { TeperaHint(stringResource(R.string.v3_feed_hint), Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) }
                        }
                        if (suggested.isNotEmpty()) {
                            item { GlassSectionHeader(stringResource(R.string.v3_feed_section_suggested)) }
                            items(suggested, key = { "s_" + it.packageName }) { FeedRow(it, it.packageName in state.feed) { viewModel.toggle(it) } }
                        }
                        if (others.isNotEmpty()) {
                            item { GlassSectionHeader(stringResource(R.string.v3_feed_section_all)) }
                            items(others, key = { it.packageName }) { FeedRow(it, it.packageName in state.feed) { viewModel.toggle(it) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedRow(app: InstalledAppInfo, selected: Boolean, onToggle: () -> Unit) {
    GlassRow(
        label = app.label,
        leading = { AppIcon(app) },
        trailing = { Switch(checked = selected, onCheckedChange = { onToggle() }, colors = teperaSwitchColors()) }
    )
}

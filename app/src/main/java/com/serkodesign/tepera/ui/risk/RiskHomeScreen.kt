package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import java.text.SimpleDateFormat
import java.util.Date

/**
 * v3.0 MVP, Етап 3: головний екран режиму v3 — L3 «Тиждень спостереження» (Figma 911:415), поки немає тижня даних, і
 * L4 «Знайдено вікна» (911:739), коли вікна є. Без вікон після тижня — спокійний порожній стан. Кнопки «Скласти план»
 * (L5) з'являться разом із конструктором плану.
 */
@Composable
fun RiskHomeScreen(
    onOpenSettings: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onOpenFeedPicker: () -> Unit,
    onOpenAccess: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val vm: RiskHomeViewModel = viewModel(factory = RiskHomeViewModel.Factory(app))
    val state by vm.state.collectAsState()

    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose {}
    }

    val observing = state.hasAccess && state.feedCount > 0 && state.windows.isEmpty() && !state.enoughData
    val label = stringResource(if (observing) R.string.v3_home_label_observing else R.string.v3_home_label_today)
    val title = if (observing) {
        stringResource(R.string.v3_home_day_format, state.observedDays.coerceIn(1, 7))
    } else {
        val locale = LocalConfiguration.current.locales[0]
        remember(locale) { SimpleDateFormat("d MMMM", locale).format(Date()) }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RiskHeader(label, title, onOpenKnowledgeBase, onOpenSettings)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    state.loading -> Unit
                    !state.hasAccess -> NoticeCard(
                        R.string.v3_home_need_access_title, R.string.v3_home_need_access_body, R.string.v3_access_grant, onOpenAccess
                    )
                    state.feedCount == 0 -> NoticeCard(
                        R.string.v3_home_need_feeds_title, R.string.v3_home_need_feeds_body, R.string.v3_feed_picker_action, onOpenFeedPicker
                    )
                    observing -> ObservingContent(state)
                    else -> FoundContent(state)
                }
            }
        }
    }
}

@Composable
private fun RiskHeader(label: String, title: String, onOpenKnowledgeBase: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            Text(
                title, color = TeperaPalette.textPrimary, fontSize = 27.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics { heading() }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TeperaIconButton(
                icon = TeperaIcons.Book,
                contentDescription = stringResource(R.string.settings_knowledge_base_action),
                onClick = onOpenKnowledgeBase,
                shape = CircleShape,
                containerColor = TeperaPalette.headerButtonFill,
                contentColor = TeperaPalette.textPrimary
            )
            TeperaIconButton(
                icon = TeperaIcons.Settings,
                contentDescription = stringResource(R.string.settings_nav_action),
                onClick = onOpenSettings,
                shape = CircleShape,
                containerColor = TeperaPalette.headerButtonFill,
                contentColor = TeperaPalette.textPrimary
            )
        }
    }
}

@Composable
private fun NoticeCard(titleRes: Int, bodyRes: Int, buttonRes: Int, onClick: () -> Unit) {
    V3Card(radius = 24.dp, padding = PaddingValues(16.dp), spacing = 12.dp) {
        Text(stringResource(titleRes), color = TeperaPalette.textPrimary, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
        Text(stringResource(bodyRes), color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 18.sp)
        TeperaButton(text = stringResource(buttonRes), onClick = onClick)
    }
}

/** L3: прогрес 7 днів, пояснення «нічого робити не треба», теплова карта й, якщо вже щось видно, підказка. */
@Composable
private fun ObservingContent(state: RiskHomeState) {
    V3Card(radius = 24.dp, padding = PaddingValues(16.dp), spacing = 8.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(7) { i ->
                Box(
                    Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (i < state.observedDays) TeperaPalette.brandAccent else TeperaPalette.heatmapNoDataFill)
                )
            }
        }
        Text(stringResource(R.string.v3_observing_hint), color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 18.sp)
    }
    HeatWeek(state.dates, state.grid, emptyList(), stringResource(R.string.v3_heat_description))
    state.teaser?.let { t ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TeperaPalette.guessFill)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(TeperaSymbols.AutoAwesome, contentDescription = null, tint = TeperaPalette.textSecondary, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.v3_teaser_format, formatWindowTime(t.startMinute, t.endMinute), t.daysLeft),
                color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp
            )
        }
    }
}

/** L4: пояснення, теплова карта з пунктирними вікнами й картки вікон; без вікон — спокійне «і це теж добре». */
@Composable
private fun FoundContent(state: RiskHomeState) {
    val windows = state.windows
    Text(
        text = if (windows.isEmpty()) {
            stringResource(R.string.v3_windows_empty)
        } else {
            stringResource(R.string.v3_windows_intro_format, windows.size)
        },
        color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
    val desc = if (windows.isEmpty()) {
        stringResource(R.string.v3_heat_description)
    } else {
        stringResource(
            R.string.v3_heat_window_description,
            stringResource(R.string.v3_heat_description),
            windows.joinToString(", ") { formatWindowTime(it.startMinute, it.endMinute) }
        )
    }
    HeatWeek(state.dates, state.grid, windows.map { it.startMinute to it.endMinute }, desc)
    windows.forEach { RiskWindowCard(it) }
}

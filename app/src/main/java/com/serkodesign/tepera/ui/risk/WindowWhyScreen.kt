package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.teperaSwitchColors

/**
 * v3.0 MVP, Етап 3, M6 «Чому саме це вікно?» (Figma 912:1089): теплова карта з цим вікном, три речення-факти
 * (повторюваність і середній час, топ застосунків, дні без вікна), підпис «це не оцінка» і перемикач, який вимикає
 * вікно (вимкнене лишається в списку приглушеним, нагадувань про нього не буде).
 */
@Composable
fun WindowWhyScreen(windowId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val vm: WindowWhyViewModel = viewModel(key = "why_$windowId", factory = WindowWhyViewModel.Factory(app, windowId))
    val ui by vm.ui.collectAsState()

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val data = ui
            GlassScreenHeader(
                title = if (data == null) "" else stringResource(R.string.v3_why_title_format, formatWindowTime(data.startMinute, data.endMinute)),
                onBack = onBack
            )
            if (data != null) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeatWeek(
                        data.dates, data.grid, listOf(data.startMinute to data.endMinute),
                        stringResource(
                            R.string.v3_heat_window_description, stringResource(R.string.v3_heat_description),
                            formatWindowTime(data.startMinute, data.endMinute)
                        )
                    )
                    Explanation(data)
                    Text(
                        stringResource(R.string.v3_why_footer), color = TeperaPalette.textSecondary,
                        fontSize = 12.sp, lineHeight = 16.sp
                    )
                    EnabledRow(data, onChange = vm::setEnabled)
                }
            }
        }
    }
}

@Composable
private fun Explanation(data: WhyUi) {
    val primary = TeperaPalette.textPrimary
    val and = stringResource(R.string.v3_why_and)
    val appTemplate = stringResource(R.string.v3_why_app_format)
    val weekdayNames = stringArrayResource(R.array.v3_weekdays_accusative)
    V3Card(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            stringResource(
                if (data.dayType == DayType.WEEKDAYS) R.string.v3_why_line1_weekdays else R.string.v3_why_line1_days,
                data.hitCount, data.observedDays,
                formatClock(data.startMinute), formatClock(data.endMinute), data.avgMinutes
            ),
            color = primary, fontSize = 14.sp, lineHeight = 18.sp
        )
        if (data.apps.isNotEmpty()) {
            val apps = joinNatural(data.apps.map { (name, minutes) -> String.format(appTemplate, name, minutes) }, and)
            Text(stringResource(R.string.v3_why_line2_format, apps), color = primary, fontSize = 14.sp, lineHeight = 18.sp)
        }
        if (data.missedWeekdays.isNotEmpty()) {
            val days = joinNatural(data.missedWeekdays.map { weekdayNames[it.value - 1] }, and)
            Text(stringResource(R.string.v3_why_line3_format, days), color = primary, fontSize = 14.sp, lineHeight = 18.sp)
        }
    }
}

private fun formatClock(minute: Int) = String.format(java.util.Locale.ROOT, "%02d:%02d", minute / 60 % 24, minute % 60)

/** `GlassRow` макета: коло з іконкою, підпис 16sp і перемикач; зміна одразу зберігається. */
@Composable
private fun EnabledRow(data: WhyUi, onChange: (Boolean) -> Unit) {
    V3Card(radius = 16.dp, padding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(TeperaPalette.surfaceBrandLight),
                contentAlignment = Alignment.Center
            ) {
                val night = data.partOfDay == PartOfDay.EVENING || data.partOfDay == PartOfDay.NIGHT
                Icon(
                    if (night) TeperaSymbols.Bedtime else TeperaSymbols.Schedule, contentDescription = null,
                    tint = TeperaPalette.brandAccent, modifier = Modifier.size(24.dp)
                )
            }
            Text(
                stringResource(R.string.v3_why_toggle), color = TeperaPalette.textPrimary, fontSize = 16.sp, lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = data.enabled, onCheckedChange = onChange, colors = teperaSwitchColors(),
                modifier = Modifier.semantics { role = Role.Switch }
            )
        }
    }
}

/** «а, б і в»: усе, крім останнього, через кому, останнє — через [and]; для одного й двох елементів — просто [and]. */
internal fun joinNatural(items: List<String>, and: String): String = when (items.size) {
    0 -> ""
    1 -> items[0]
    else -> items.dropLast(1).joinToString(", ") + and + items.last()
}

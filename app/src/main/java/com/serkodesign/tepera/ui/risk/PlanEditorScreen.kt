package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaTextField
import kotlinx.coroutines.launch
import java.util.Locale

private const val MAX_OWN_LENGTH = 80

/** Підказки «Якщо… / …то я» за частиною доби. Вечір/ніч і обід — з макета v3.0; ранок/день — нейтральний запасний набір. */
private data class PlanPresets(val ifs: List<Int>, val thens: List<Int>)

private fun presetsFor(part: PartOfDay): PlanPresets = when (part) {
    PartOfDay.EVENING, PartOfDay.NIGHT -> PlanPresets(
        listOf(R.string.v3_plan_if_bed, R.string.v3_plan_if_sofa),
        listOf(R.string.v3_plan_then_book, R.string.v3_plan_then_charge, R.string.v3_plan_then_podcast, R.string.v3_plan_then_todo)
    )
    PartOfDay.LUNCH -> PlanPresets(
        listOf(R.string.v3_plan_if_lunch, R.string.v3_plan_if_eat),
        listOf(R.string.v3_plan_then_walk15, R.string.v3_plan_then_eat_no_phone)
    )
    PartOfDay.MORNING, PartOfDay.DAY -> PlanPresets(
        listOf(R.string.v3_plan_if_break, R.string.v3_plan_if_urge),
        listOf(R.string.v3_plan_then_walk5, R.string.v3_plan_then_water, R.string.v3_plan_then_breathe)
    )
}

/**
 * v3.0 MVP, Етап 3, L5 «План "якщо — то"» (Figma "App concept" 911:1066): людина збирає план із підказок або пише
 * свій варіант. Тексти зберігаються дослівно; підказки лише допомагають. Один план на вікно.
 */
@Composable
fun PlanEditorScreen(windowId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val vm: PlanEditorViewModel = viewModel(key = windowId, factory = PlanEditorViewModel.Factory(app, windowId))
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val data = ui
            GlassScreenHeader(
                title = if (data == null) "" else stringResource(R.string.v3_plan_title_format, formatStart(data.startMinute)),
                onBack = onBack
            )
            if (data != null) {
                PlanEditorContent(
                    data = data,
                    onSave = { ifText, thenText -> scope.launch { vm.save(ifText, thenText); onBack() } },
                    onLater = onBack,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatStart(minute: Int) = String.format(Locale.ROOT, "%02d:%02d", minute / 60 % 24, minute % 60)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PlanEditorContent(
    data: PlanEditorUi,
    onSave: (String, String) -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember(data.partOfDay) { presetsFor(data.partOfDay) }
    val ifPresets = presets.ifs.map { stringResource(it) }
    val thenPresets = presets.thens.map { stringResource(it) }

    // Початковий вибір: збережений план, що збігається з підказкою, — її чіп; інакше — «свій варіант» з текстом.
    var ifIndex by remember(data.windowId) { mutableIntStateOf(initialIndex(data.existingIf, ifPresets)) }
    var thenIndex by remember(data.windowId) { mutableIntStateOf(initialIndex(data.existingThen, thenPresets)) }
    var ifOwn by remember(data.windowId) { mutableStateOf(if (ifIndex == ifPresets.size) data.existingIf.orEmpty() else "") }
    var thenOwn by remember(data.windowId) { mutableStateOf(if (thenIndex == thenPresets.size) data.existingThen.orEmpty() else "") }

    val ifText = when {
        ifIndex < 0 -> ""
        ifIndex < ifPresets.size -> ifPresets[ifIndex]
        else -> ifOwn.trim()
    }
    val thenText = when {
        thenIndex < 0 -> ""
        thenIndex < thenPresets.size -> thenPresets[thenIndex]
        else -> thenOwn.trim()
    }
    val complete = ifText.isNotBlank() && thenText.isNotBlank()

    val recurrence = stringResource(
        if (data.dayType == DayType.WEEKDAYS) R.string.v3_card_recurrence_weekdays else R.string.v3_card_recurrence_days,
        data.recurrenceDays, data.observedDays
    )
    val part = stringResource(partOfDayRes(data.partOfDay))

    Column(modifier) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.v3_plan_intro_format, part, recurrence),
                color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 18.sp
            )
            ChipSection(
                title = stringResource(R.string.v3_plan_if_title),
                presets = ifPresets, selected = ifIndex, onSelect = { ifIndex = it },
                own = ifOwn, onOwnChange = { ifOwn = it.take(MAX_OWN_LENGTH) }
            )
            ChipSection(
                title = stringResource(R.string.v3_plan_then_title),
                presets = thenPresets, selected = thenIndex, onSelect = { thenIndex = it },
                own = thenOwn, onOwnChange = { thenOwn = it.take(MAX_OWN_LENGTH) }
            )
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TeperaPalette.surfaceBrandLight)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(R.string.v3_plan_preview_label), color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                Text(
                    text = if (complete) stringResource(R.string.v3_plan_format, ifText, thenText) else stringResource(R.string.v3_plan_preview_empty),
                    color = if (complete) TeperaPalette.buttonBrandDark else TeperaPalette.textSecondary,
                    fontSize = if (complete) 18.sp else 14.sp,
                    lineHeight = if (complete) 20.sp else 18.sp,
                    fontWeight = if (complete) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TeperaButton(
                text = stringResource(R.string.v3_plan_save),
                onClick = { onSave(ifText, thenText) },
                enabled = complete,
                modifier = Modifier.fillMaxWidth()
            )
            TeperaButton(
                text = stringResource(R.string.v3_plan_later),
                onClick = onLater,
                modifier = Modifier.fillMaxWidth(),
                type = TeperaButtonType.Tertiary
            )
        }
    }
}

/** -1 — нічого не обрано; індекс підказки; або `presets.size` — «свій варіант» (збережений текст не збігся з підказкою). */
internal fun initialIndex(existing: String?, presets: List<String>): Int = when {
    existing == null -> -1
    existing in presets -> presets.indexOf(existing)
    else -> presets.size
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ChipSection(
    title: String,
    presets: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    own: String,
    onOwnChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = TeperaPalette.textPrimary, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEachIndexed { i, label -> PlanChip(label, selected == i) { onSelect(i) } }
            PlanChip(stringResource(R.string.v3_plan_own), selected == presets.size) { onSelect(presets.size) }
        }
        if (selected == presets.size) {
            TeperaTextField(value = own, onValueChange = onOwnChange, label = stringResource(R.string.v3_plan_own_hint))
        }
    }
}

/** Чіп 36dp у зоні дотику 48dp (макет: 9/14 відступів, радіус 100). */
@Composable
private fun PlanChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        Modifier.heightIn(min = 48.dp).clip(shape).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.clip(shape)
                .background(if (selected) TeperaPalette.primaryButtonFill else TeperaPalette.chipSurface)
                .let { if (selected) it else it.border(BorderStroke(1.dp, TeperaPalette.fieldBorder), shape) }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                label,
                color = if (selected) TeperaPalette.primaryButtonContent else TeperaPalette.buttonBrandDark,
                fontSize = 14.sp, lineHeight = 18.sp
            )
        }
    }
}

package com.serkodesign.tepera.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.GapSensitivity
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.HourRangeSlider
import com.serkodesign.tepera.ui.theme.NavChevron
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.util.FeedbackForm
import com.serkodesign.tepera.util.LocaleStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Стиль перенесений з Figma-фрейму Everyday_Designs (сторінка "Tepera", node 1951:1106):
 * власний заголовок (GlassScreenHeader) замість TopAppBar, "скляні" картки-рядки для навігації
 * замість ListItem. Резервне копіювання (Export/Import JSON) переїхало на окремий екран
 * (BackupRestoreScreen) — у фреймі це окремий "Backup and restore" з власним заголовком,
 * а не інлайн-дії тут.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    sleepWindowRepository: SleepWindowRepository,
    onOpenExclusionList: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenSpikeT1: () -> Unit = {},
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Слайдер оперує цілими годинами (1-8, за запитом користувача — мокап показує саме таку
    // капсулу-слайдер, не текстове поле з довільними хвилинами); SettingsStore і решта
    // застосунку (BalanceRepository, віджет) далі працюють у хвилинах — конвертація лише тут.
    var targetHours by remember { mutableStateOf(3) }
    var showTargetInfo by remember { mutableStateOf(false) }
    // T-12 (tepera-dev-spec.md): вікно сну — тепер справжній діапазон (початок+кінець), не
    // єдина "межа кінця". Слот 2 — друге вікно для плаваючого графіка, вимкнене за замовчуванням.
    // Години (0-23), не хвилини від півночі — конвертація в SleepWindowEntity лише на збереженні,
    // той самий принцип, що targetHours/targetMinutes нижче.
    var window1StartHour by remember { mutableStateOf(0) }
    var window1EndHour by remember { mutableStateOf(6) }
    var window2Enabled by remember { mutableStateOf(false) }
    var window2StartHour by remember { mutableStateOf(0) }
    var window2EndHour by remember { mutableStateOf(6) }
    var showSleepWindowInfo by remember { mutableStateOf(false) }
    // T-11 (tepera-dev-spec.md): пресет, не числові поля — числа тут вимагали б від людини
    // розуміння алгоритму детекції пауз.
    var gapSensitivity by remember { mutableStateOf(GapSensitivity.NORMAL) }
    var showGapSensitivityInfo by remember { mutableStateOf(false) }

    // Один одноразовий зчит з DataStore/Room на вхід — на відміну від reactive collectAsState,
    // це навмисно НЕ синхронізується з полем повторно після кожної власної зміни (інакше
    // повзунок "стрибав" би під час перетягування).
    LaunchedEffect(Unit) {
        targetHours = (settingsStore.targetMinutes.first() / 60f).roundToInt().coerceIn(1, 8)
        val windows = sleepWindowRepository.getWindows()
        windows.find { it.slot == 1 }?.let {
            window1StartHour = (it.startMinuteOfDay / 60).coerceIn(0, 23)
            window1EndHour = (it.endMinuteOfDay / 60).coerceIn(0, 23)
        }
        windows.find { it.slot == 2 }?.let {
            window2Enabled = it.enabled
            window2StartHour = (it.startMinuteOfDay / 60).coerceIn(0, 23)
            window2EndHour = (it.endMinuteOfDay / 60).coerceIn(0, 23)
        }
        gapSensitivity = settingsStore.gapSensitivity.first()
    }

    // LocaleStore (SharedPreferences), не AppCompatDelegate: AppCompatDelegate.
    // setApplicationLocales() застосовує збережену мову до ресурсів лише для AppCompatActivity
    // (через власний attachBaseContext-хук) — MainActivity звичайний ComponentActivity, тож
    // виклик лише запам'ятовував вибір, а UI лишався тою самою мовою (підтверджено на
    // Samsung S23: вибір "English" позначався, але текст лишався українською).
    var selectedLanguageTag by remember { mutableStateOf(LocaleStore.getLanguageTag(context)) }

    // FR-6.4: чесне попередження перед відкриттям браузера — тап не веде туди одразу.
    var showSuggestFeatureConfirm by remember { mutableStateOf(false) }

    if (showTargetInfo) {
        AlertDialog(
            onDismissRequest = { showTargetInfo = false },
            confirmButton = {
                TextButton(onClick = { showTargetInfo = false }) { Text(stringResource(R.string.dialog_ok)) }
            },
            text = { Text(stringResource(R.string.settings_target_info)) }
        )
    }
    if (showSleepWindowInfo) {
        AlertDialog(
            onDismissRequest = { showSleepWindowInfo = false },
            confirmButton = {
                TextButton(onClick = { showSleepWindowInfo = false }) { Text(stringResource(R.string.dialog_ok)) }
            },
            text = { Text(stringResource(R.string.settings_sleep_window_info)) }
        )
    }
    if (showGapSensitivityInfo) {
        AlertDialog(
            onDismissRequest = { showGapSensitivityInfo = false },
            confirmButton = {
                TextButton(onClick = { showGapSensitivityInfo = false }) { Text(stringResource(R.string.dialog_ok)) }
            },
            text = { Text(stringResource(R.string.settings_gap_sensitivity_info)) }
        )
    }
    // FR-6.4/6.5: чесне попередження, що зараз відкриється браузер (форму приймає Google),
    // перш ніж передати намір системі — і спокійна обробка відсутності браузера (FR-6.5).
    if (showSuggestFeatureConfirm) {
        AlertDialog(
            onDismissRequest = { showSuggestFeatureConfirm = false },
            title = { Text(stringResource(R.string.settings_suggest_feature_confirm_title)) },
            text = { Text(stringResource(R.string.settings_suggest_feature_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showSuggestFeatureConfirm = false
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FeedbackForm.urlFor(context))))
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, context.getString(R.string.settings_suggest_feature_no_browser), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.settings_suggest_feature_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showSuggestFeatureConfirm = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.settings_screen_title), onBack = onBack)

            // verticalScroll: без нього нижні рядки ("Резервне копіювання", "Запропонувати
            // функцію") виходять за межі екрана й лишаються недосяжними для дотику на пристроях
            // з високою щільністю контенту (підтверджено на Samsung S23) — Column сам по собі
            // не скролиться.
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Таргет Online-часу: слайдер по цілих годинах 1-8 (за запитом користувача,
                // замість довільних хвилин у текстовому полі) — той самий "заповнений чіп"
                // капсули, що й у фреймі.
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_target_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showTargetInfo = true }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.settings_target_info))
                        }
                    }
                    HourRangeSlider(
                        hours = targetHours,
                        onHoursChange = { hours ->
                            targetHours = hours
                            scope.launch { settingsStore.setTargetMinutes(hours * 60) }
                        },
                        valueLabel = { hours -> stringResource(R.string.settings_target_hours_format, hours) },
                        minHours = 1,
                        maxHours = 8
                    )
                }

                // T-12: вікно сну — тепер справжній діапазон (початок+кінець), до 2 вікон.
                // Технічний параметр розрахунку (не сегмент шкали) — заголовок і опис навмисно
                // без слів-оцінок, лише опис ЩО вікно робить з розрахунком.
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_sleep_window_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showSleepWindowInfo = true }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.settings_sleep_window_info))
                        }
                    }
                    Text(
                        stringResource(R.string.settings_sleep_window_start_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HourRangeSlider(
                        hours = window1StartHour,
                        onHoursChange = { hour ->
                            window1StartHour = hour
                            scope.launch { sleepWindowRepository.setWindow(1, hour * 60, window1EndHour * 60, enabled = true) }
                        },
                        valueLabel = { hour -> stringResource(R.string.settings_sleep_window_hour_format, hour) },
                        minHours = 0,
                        maxHours = 23
                    )
                    Text(
                        stringResource(R.string.settings_sleep_window_end_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HourRangeSlider(
                        hours = window1EndHour,
                        onHoursChange = { hour ->
                            window1EndHour = hour
                            scope.launch { sleepWindowRepository.setWindow(1, window1StartHour * 60, hour * 60, enabled = true) }
                        },
                        valueLabel = { hour -> stringResource(R.string.settings_sleep_window_hour_format, hour) },
                        minHours = 0,
                        maxHours = 23
                    )

                    // Друге вікно — для плаваючого графіка (T-12), вимкнене за замовчуванням.
                    // Виправлено (за прямим запитом користувача): Column без Modifier.weight(1f)
                    // мав НЕОБМЕЖЕНУ ширину під SpaceBetween-розкладкою Row — довший підпис
                    // ("Для плаваючого графіка...") тоді насувався на сам Switch замість
                    // перенесення рядка, і перемикач "ледь влазив" у праву межу екрана.
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(stringResource(R.string.settings_sleep_window_second_label), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.settings_sleep_window_second_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = window2Enabled,
                            onCheckedChange = { enabled ->
                                window2Enabled = enabled
                                scope.launch { sleepWindowRepository.setWindow(2, window2StartHour * 60, window2EndHour * 60, enabled) }
                            },
                            colors = teperaSwitchColors()
                        )
                    }
                    if (window2Enabled) {
                        Text(
                            stringResource(R.string.settings_sleep_window_start_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HourRangeSlider(
                            hours = window2StartHour,
                            onHoursChange = { hour ->
                                window2StartHour = hour
                                scope.launch { sleepWindowRepository.setWindow(2, hour * 60, window2EndHour * 60, enabled = true) }
                            },
                            valueLabel = { hour -> stringResource(R.string.settings_sleep_window_hour_format, hour) },
                            minHours = 0,
                            maxHours = 23
                        )
                        Text(
                            stringResource(R.string.settings_sleep_window_end_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HourRangeSlider(
                            hours = window2EndHour,
                            onHoursChange = { hour ->
                                window2EndHour = hour
                                scope.launch { sleepWindowRepository.setWindow(2, window2StartHour * 60, hour * 60, enabled = true) }
                            },
                            valueLabel = { hour -> stringResource(R.string.settings_sleep_window_hour_format, hour) },
                            minHours = 0,
                            maxHours = 23
                        )
                    }
                }

                // T-11 (tepera-dev-spec.md): пресет чутливості детекції пауз — три варіанти,
                // без числових полів (документ, пункт 2).
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_gap_sensitivity_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showGapSensitivityInfo = true }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.settings_gap_sensitivity_info))
                        }
                    }
                    val sensitivityOptions = listOf(
                        GapSensitivity.RARE to stringResource(R.string.settings_gap_sensitivity_rare),
                        GapSensitivity.NORMAL to stringResource(R.string.settings_gap_sensitivity_normal),
                        GapSensitivity.FREQUENT to stringResource(R.string.settings_gap_sensitivity_frequent)
                    )
                    PillSegmentedControl(
                        options = sensitivityOptions,
                        selected = gapSensitivity,
                        onSelect = { sensitivity ->
                            gapSensitivity = sensitivity
                            scope.launch { settingsStore.setGapSensitivity(sensitivity) }
                        }
                    )
                }

                // Мова застосунку.
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.settings_language_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    val languageOptions = listOf(
                        "" to stringResource(R.string.settings_language_system),
                        "uk" to stringResource(R.string.settings_language_uk),
                        "en" to stringResource(R.string.settings_language_en)
                    )
                    PillSegmentedControl(
                        options = languageOptions,
                        selected = selectedLanguageTag,
                        onSelect = { tag ->
                            selectedLanguageTag = tag
                            LocaleStore.setLanguageTag(context, tag)
                            // recreate() перезапускає Activity — attachBaseContext() зчитує
                            // щойно збережений тег і обгортає нові ресурси одразу, без
                            // повного перезапуску процесу.
                            (context as? Activity)?.recreate()
                        }
                    )
                }

                // Інше — навігаційні рядки на під-екрани.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassSectionHeader(stringResource(R.string.settings_other_section))
                    GlassRow(
                        label = stringResource(R.string.settings_categories_action),
                        onClick = onOpenCategories,
                        leading = { TeperaIconCircle(Icons.Filled.Category) },
                        trailing = { NavChevron() }
                    )
                    GlassRow(
                        label = stringResource(R.string.settings_exclusion_list_action),
                        onClick = onOpenExclusionList,
                        leading = { TeperaIconCircle(Icons.Filled.VisibilityOff) },
                        trailing = { NavChevron() }
                    )
                    GlassRow(
                        label = stringResource(R.string.settings_gates_action),
                        onClick = onOpenGates,
                        leading = { TeperaIconCircle(Icons.Filled.Timer) },
                        trailing = { NavChevron() }
                    )
                    GlassRow(
                        label = stringResource(R.string.settings_backup_restore_action),
                        onClick = onOpenBackupRestore,
                        leading = { TeperaIconCircle(Icons.Filled.Archive) },
                        trailing = { NavChevron() }
                    )
                    // FR-6.4: розміщується безпосередньо під резервним копіюванням. Свідомо без
                    // бейджа й без самостійного нагадування (FR-6.6) — лежить тут і чекає.
                    GlassRow(
                        label = stringResource(R.string.settings_suggest_feature_action),
                        onClick = { showSuggestFeatureConfirm = true },
                        leading = { TeperaIconCircle(Icons.Filled.Feedback) },
                        trailing = { NavChevron() }
                    )
                    // Debug-only вхід у T-1 (tepera-dev-spec.md) — інструмент спайку, не
                    // продакшн-функція. Перевірка FLAG_DEBUGGABLE, а не BuildConfig.DEBUG:
                    // buildFeatures.buildConfig не увімкнено в app/build.gradle.kts, а вмикати
                    // його заради одного прапорця в debug-only коді — зайва зміна білд-конфігу.
                    val isDebuggable = (context.applicationInfo.flags and
                        android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    if (isDebuggable) {
                        GlassRow(
                            label = "T-1: спайк видимості пакетів (debug)",
                            onClick = onOpenSpikeT1,
                            leading = { TeperaIconCircle(Icons.Filled.Info) },
                            trailing = { NavChevron() }
                        )
                    }
                }
            }
        }
    }
}

package com.serkodesign.tepera.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.HourRangeSlider
import com.serkodesign.tepera.ui.theme.NavChevron
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
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
    onOpenExclusionList: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Слайдер оперує цілими годинами (1-8, за запитом користувача — мокап показує саме таку
    // капсулу-слайдер, не текстове поле з довільними хвилинами); SettingsStore і решта
    // застосунку (BalanceRepository, віджет) далі працюють у хвилинах — конвертація лише тут.
    var targetHours by remember { mutableStateOf(3) }
    var showTargetInfo by remember { mutableStateOf(false) }
    // FR-3.2 (SRS v2.5): межа "вікна сну" — до цієї години коротка нічна перевірка телефону
    // не рахується стартом дня (BalanceRepository.calculateDayStartMillis()).
    var sleepWindowEndHour by remember { mutableStateOf(6) }
    var showSleepWindowInfo by remember { mutableStateOf(false) }

    // Один одноразовий зчит з DataStore на вхід — на відміну від reactive collectAsState,
    // це навмисно НЕ синхронізується з полем повторно після кожної власної зміни (інакше
    // повзунок "стрибав" би під час перетягування).
    LaunchedEffect(Unit) {
        targetHours = (settingsStore.targetMinutes.first() / 60f).roundToInt().coerceIn(1, 8)
        sleepWindowEndHour = settingsStore.sleepWindowEndHour.first().coerceIn(0, 11)
    }

    // LocaleStore (SharedPreferences), не AppCompatDelegate: AppCompatDelegate.
    // setApplicationLocales() застосовує збережену мову до ресурсів лише для AppCompatActivity
    // (через власний attachBaseContext-хук) — MainActivity звичайний ComponentActivity, тож
    // виклик лише запам'ятовував вибір, а UI лишався тою самою мовою (підтверджено на
    // Samsung S23: вибір "English" позначався, але текст лишався українською).
    var selectedLanguageTag by remember { mutableStateOf(LocaleStore.getLanguageTag(context)) }

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

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.settings_screen_title), onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
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

                // FR-3.2: межа вікна сну — той самий слайдер-компонент, 0-11 год.
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
                    HourRangeSlider(
                        hours = sleepWindowEndHour,
                        onHoursChange = { hour ->
                            sleepWindowEndHour = hour
                            scope.launch { settingsStore.setSleepWindowEndHour(hour) }
                        },
                        valueLabel = { hour -> stringResource(R.string.settings_sleep_window_hour_format, hour) },
                        minHours = 0,
                        maxHours = 11
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
                        label = stringResource(R.string.settings_backup_restore_action),
                        onClick = onOpenBackupRestore,
                        leading = { TeperaIconCircle(Icons.Filled.Archive) },
                        trailing = { NavChevron() }
                    )
                }
            }
        }
    }
}

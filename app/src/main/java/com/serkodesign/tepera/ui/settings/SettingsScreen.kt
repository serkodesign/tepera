package com.serkodesign.tepera.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BackupRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    backupRepository: BackupRepository,
    onOpenExclusionList: () -> Unit,
    onOpenCategories: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var targetText by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = backupRepository.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("no output stream")
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.backup_export_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.backup_export_failure), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // FR-6.2: імпорт замінює ВСІ локальні дані — питаємо підтвердження, перш ніж читати файл.
    val importPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri!!
                    pendingImportUri = null
                    scope.launch {
                        runCatching {
                            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: error("no input stream")
                            backupRepository.importFromJson(String(json))
                        }.onSuccess {
                            Toast.makeText(context, context.getString(R.string.backup_import_success), Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, context.getString(R.string.backup_import_failure), Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text(stringResource(R.string.backup_import_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }

    // Один одноразовий зчит з DataStore на вхід — на відміну від reactive collectAsState,
    // це навмисно НЕ синхронізується з полем повторно після кожного власного запису
    // (інакше курсор/позиція вводу "стрибали" б під час введення).
    LaunchedEffect(Unit) {
        targetText = settingsStore.targetMinutes.first().toString()
    }

    // AppCompatDelegate — джерело істини для поточної мови (переживає recreate), не DataStore:
    // сам виклик setApplicationLocales() уже персистує вибір і перезапускає Activity з новими
    // ресурсами, окреме зберігання в SettingsStore було б зайвим дублюванням стану.
    var selectedLanguageTag by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = targetText,
                onValueChange = { text ->
                    val digits = text.filter { it.isDigit() }.take(4)
                    targetText = digits
                    digits.toIntOrNull()?.takeIf { it > 0 }?.let { minutes ->
                        scope.launch { settingsStore.setTargetMinutes(minutes) }
                    }
                },
                label = { Text(stringResource(R.string.settings_target_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            HorizontalDivider()

            Text(
                stringResource(R.string.settings_language_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            val languageOptions = listOf(
                "" to R.string.settings_language_system,
                "uk" to R.string.settings_language_uk,
                "en" to R.string.settings_language_en
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                languageOptions.forEachIndexed { index, (tag, labelRes) ->
                    SegmentedButton(
                        selected = selectedLanguageTag == tag,
                        onClick = {
                            selectedLanguageTag = tag
                            val locales = if (tag.isEmpty()) {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(tag)
                            }
                            AppCompatDelegate.setApplicationLocales(locales)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = languageOptions.size)
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_categories_action)) },
                leadingContent = { Icon(Icons.Filled.Category, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCategories)
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_exclusion_list_action)) },
                leadingContent = { Icon(Icons.Filled.Apps, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenExclusionList)
            )

            HorizontalDivider()

            // FR-6.2: ручний JSON-експорт/імпорт — доповнення до Android Auto Backup (FR-6.1).
            ListItem(
                headlineContent = { Text(stringResource(R.string.backup_export_action)) },
                leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                        exportLauncher.launch("tepera-backup-$timestamp.json")
                    }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.backup_import_action)) },
                leadingContent = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { importPickerLauncher.launch(arrayOf("application/json")) }
            )
        }
    }
}

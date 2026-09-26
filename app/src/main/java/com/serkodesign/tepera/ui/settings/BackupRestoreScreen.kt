package com.serkodesign.tepera.ui.settings

import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.TeperaDialog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import com.serkodesign.tepera.ui.theme.TeperaPalette
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.data.repository.BackupRepository
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.NavChevron
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FR-6.2: ручний JSON-експорт/імпорт — окремий екран у стилі Figma-фрейму Everyday_Designs
 * (node 1951:2112, "Settings - Backup and restore"), раніше це були інлайн-дії в SettingsScreen.
 */
@Composable
fun BackupRestoreScreen(
    backupRepository: BackupRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    // Видалення всіх даних — ДВА кроки (захист від випадкового тапу): пояснення, потім введення слова-підтвердження.
    var deleteStep by remember { mutableStateOf(0) }
    var deleteWordInput by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }

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
        TeperaDialog(
            onDismissRequest = { pendingImportUri = null },
            title = stringResource(R.string.backup_import_confirm_title),
            text = stringResource(R.string.backup_import_confirm_body),
            confirmText = stringResource(R.string.backup_import_confirm_action),
            dismissText = stringResource(R.string.dialog_cancel),
            onConfirm = {
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
            }
        )
    }

    if (deleteStep == 1) {
        TeperaDialog(
            onDismissRequest = { deleteStep = 0 },
            title = stringResource(R.string.backup_delete_title),
            text = stringResource(R.string.backup_delete_body),
            confirmText = stringResource(R.string.backup_delete_next),
            onConfirm = {
                deleteWordInput = ""
                deleteStep = 2
            },
            dismissText = stringResource(R.string.dialog_cancel)
        )
    }
    if (deleteStep == 2) {
        val word = stringResource(R.string.backup_delete_word)
        val matches = deleteWordInput.trim().equals(word, ignoreCase = true)
        TeperaDialog(
            onDismissRequest = { if (!deleting) deleteStep = 0 },
            title = stringResource(R.string.backup_delete_final_title),
            text = stringResource(R.string.backup_delete_final_body, word),
            confirmText = stringResource(if (deleting) R.string.backup_delete_running else R.string.backup_delete_confirm),
            confirmEnabled = matches && !deleting,
            onConfirm = {
                deleting = true
                val app = context.applicationContext as com.serkodesign.tepera.TeperaApp
                scope.launch {
                    runCatching { app.wipeAllData() }
                        .onSuccess { app.restartApp() }
                        .onFailure {
                            deleting = false
                            deleteStep = 0
                            Toast.makeText(context, context.getString(R.string.backup_delete_failure), Toast.LENGTH_SHORT).show()
                        }
                }
            },
            dismissText = stringResource(R.string.dialog_cancel),
            onDismiss = { if (!deleting) deleteStep = 0 }
        ) {
            OutlinedTextField(
                value = deleteWordInput,
                onValueChange = { deleteWordInput = it },
                enabled = !deleting,
                singleLine = true,
                label = { Text(stringResource(R.string.backup_delete_word_label)) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = TeperaPalette.buttonBrand,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = TeperaPalette.buttonBrand,
                    unfocusedLabelColor = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f),
                    focusedTextColor = TeperaPalette.buttonBrandDark,
                    unfocusedTextColor = TeperaPalette.buttonBrandDark,
                    cursorColor = TeperaPalette.buttonBrand
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.settings_backup_restore_action), onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassSectionHeader(stringResource(R.string.categories_section_active))
                GlassRow(
                    label = stringResource(R.string.backup_export_action),
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                        exportLauncher.launch("tepera-backup-$timestamp.json")
                    },
                    leading = { TeperaIconCircle(TeperaSymbols.FileDownload) },
                    trailing = { NavChevron() }
                )
                GlassRow(
                    label = stringResource(R.string.backup_import_action),
                    onClick = { importPickerLauncher.launch(arrayOf("application/json")) },
                    leading = { TeperaIconCircle(TeperaSymbols.FileUpload) },
                    trailing = { NavChevron() }
                )
                // CH-06 / FR-6.2: чесно про те, що файл не містить усіх налаштувань.
                Text(
                    text = stringResource(R.string.backup_not_included_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = TeperaPalette.buttonBrandDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                GlassSectionHeader(stringResource(R.string.backup_delete_section))
                GlassRow(
                    label = stringResource(R.string.backup_delete_action),
                    onClick = { deleteStep = 1 },
                    leading = { TeperaIconCircle(TeperaSymbols.DeleteForever) },
                    trailing = { NavChevron() }
                )
            }
        }
    }
}

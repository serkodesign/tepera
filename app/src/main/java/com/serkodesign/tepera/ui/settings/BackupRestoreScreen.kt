package com.serkodesign.tepera.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
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
                    leading = { TeperaIconCircle(Icons.Filled.FileDownload) },
                    trailing = { NavChevron() }
                )
                GlassRow(
                    label = stringResource(R.string.backup_import_action),
                    onClick = { importPickerLauncher.launch(arrayOf("application/json")) },
                    leading = { TeperaIconCircle(Icons.Filled.FileUpload) },
                    trailing = { NavChevron() }
                )
            }
        }
    }
}

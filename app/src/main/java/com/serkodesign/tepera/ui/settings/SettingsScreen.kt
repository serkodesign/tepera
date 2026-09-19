package com.serkodesign.tepera.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.NavChevron
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.util.FeedbackForm

/**
 * Стиль перенесений з Figma-фрейму Everyday_Designs (сторінка "Tepera", node 1951:1106):
 * власний заголовок (GlassScreenHeader) замість TopAppBar, "скляні" картки-рядки для навігації
 * замість ListItem. Резервне копіювання (Export/Import JSON) — окремий екран (BackupRestoreScreen).
 * **За прямим запитом користувача (не в Figma-фреймі)**: "Орієнтир Online-часу"/"Вікно сну"/
 * "Чутливість детекції пауз" згруповано в окремий під-екран "Відстеження", "Мова застосунку" —
 * у свій під-екран "Мова" (`TrackingSettingsScreen`/`LanguageSettingsScreen`) — той самий патерн
 * навігації, що вже був у Категорій/Виключених застосунків/Воріт, замість інлайн-блоків тут.
 * **Два розділи (за прямим запитом користувача, звіряючись із Figma node 2156:84):** "Активності"
 * (Відстеження, Категорії, Виключені застосунки, Ворота) і "Загальні" (Мова, Резервне
 * копіювання, Запропонувати функцію, debug-only T-1 спайк) — замість єдиного "Інше". Порядок
 * розділів (Активності вище Загальних) — окремий прямий запит користувача.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenTracking: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenExclusionList: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenSpikeT1: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // FR-6.4: чесне попередження перед відкриттям браузера — тап не веде туди одразу.
    var showSuggestFeatureConfirm by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Згруповано за прямим запитом користувача: "Активності" (Відстеження,
                // Категорії, Виключені застосунки, Ворота) — над "Загальні" (Мова, Резервне
                // копіювання, Запропонувати функцію, T-1 спайк) — порядок розділів поміняно
                // місцями за прямим запитом користувача.
                GlassSectionHeader(stringResource(R.string.settings_activities_section))
                GlassRow(
                    label = stringResource(R.string.settings_tracking_action),
                    onClick = onOpenTracking,
                    leading = { TeperaIconCircle(Icons.Filled.TrackChanges) },
                    trailing = { NavChevron() }
                )
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

                GlassSectionHeader(stringResource(R.string.settings_general_section))
                GlassRow(
                    label = stringResource(R.string.settings_language_action),
                    onClick = onOpenLanguage,
                    leading = { TeperaIconCircle(Icons.Filled.Language) },
                    trailing = { NavChevron() }
                )
                GlassRow(
                    label = stringResource(R.string.settings_backup_restore_action),
                    onClick = onOpenBackupRestore,
                    leading = { TeperaIconCircle(Icons.Filled.Archive) },
                    trailing = { NavChevron() }
                )
                // FR-6.4: чесне попередження перед відкриттям браузера. Свідомо без бейджа й без
                // самостійного нагадування (FR-6.6) — лежить тут і чекає.
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

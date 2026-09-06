package com.serkodesign.tepera.ui.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onOpenExclusionList: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var targetText by remember { mutableStateOf("") }

    // Один одноразовий зчит з DataStore на вхід — на відміну від reactive collectAsState,
    // це навмисно НЕ синхронізується з полем повторно після кожного власного запису
    // (інакше курсор/позиція вводу "стрибали" б під час введення).
    LaunchedEffect(Unit) {
        targetText = settingsStore.targetMinutes.first().toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_exclusion_list_action)) },
                leadingContent = { Icon(Icons.Filled.Apps, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenExclusionList)
            )
        }
    }
}

package com.serkodesign.tepera.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * T-1 (tepera-dev-spec.md) — debug-only екран для збору "таблиці 4 пристрої × 5 пунктів",
 * якої вимагає приймання спайку. Доступ — лише через debug-only рядок у SettingsScreen.kt
 * (перевірка ApplicationInfo.FLAG_DEBUGGABLE), не для кінцевого користувача.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpikeT1Screen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SpikeT1ViewModel = viewModel(factory = SpikeT1ViewModel.Factory(context))
    val state by viewModel.uiState.collectAsState()
    var targetPackage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("T-1: спайк видимості пакетів") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SpikeSection(
                title = "1+2. Видимість пакетів і глибина історії",
                action = { Button(onClick = { viewModel.runPackageVisibilityAndHistoryDepth() }) { Text("Сканувати") } },
                result = listOf(state.visibilityResult, state.historyDepthResult).filter { it.isNotBlank() }
                    .joinToString("\n\n")
            )

            OutlinedTextField(
                value = targetPackage,
                onValueChange = { targetPackage = it },
                label = { Text("Package name цільового застосунку (напр. com.instagram.android)") },
                modifier = Modifier.fillMaxWidth()
            )

            SpikeSection(
                title = "3. Pin shortcut",
                action = { Button(onClick = { viewModel.testPinShortcut(targetPackage) }) { Text("Перевірити pin shortcut") } },
                result = state.pinShortcutResult
            )

            SpikeSection(
                title = "4. Запуск через getLaunchIntentForPackage()",
                action = { Button(onClick = { viewModel.testLaunch(targetPackage) }) { Text("Запустити застосунок") } },
                result = state.launchResult
            )

            SpikeSection(
                title = "5. KEYGUARD_HIDDEN (для T-14)",
                action = { Button(onClick = { viewModel.checkKeyguardEvents() }) { Text("Натисни, заблокуй/розблокуй, натисни ще раз") } },
                result = state.keyguardResult
            )
        }
    }
}

@Composable
private fun SpikeSection(title: String, action: @Composable () -> Unit, result: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        action()
        if (result.isNotBlank()) {
            Text(
                result,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }
    }
}

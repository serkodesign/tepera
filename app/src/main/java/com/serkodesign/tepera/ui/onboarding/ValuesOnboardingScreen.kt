package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import kotlinx.coroutines.launch

/**
 * FR-P.2: одне питання при першому запуску, ЗАВЖДИ перше (раніше за онбординг доступу до
 * статистики, FR-7.1 — HomeScreen узгоджує порядок через два послідовні LaunchedEffect).
 * "Пропустити" доступний (за узгодженням з монастирською філософією "ніколи не блокувати") —
 * SRS текст не згадує skip для цього питання явно, але й не забороняє.
 */
@Composable
fun ValuesOnboardingScreen(
    categoryRepository: CategoryRepository,
    settingsStore: SettingsStore,
    onDone: () -> Unit
) {
    val categories by categoryRepository.observeActiveCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    fun answer(categoryId: String?) {
        scope.launch {
            settingsStore.setValuesOnboardingAnswer(categoryId)
            onDone()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.values_onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Column(modifier = Modifier.padding(top = 24.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { answer(category.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(categoryIcon(category.iconName), contentDescription = null)
                                Text(categoryDisplayName(category), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = { answer(null) },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
    }
}

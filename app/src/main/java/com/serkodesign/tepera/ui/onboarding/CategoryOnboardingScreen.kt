package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import kotlinx.coroutines.launch

/**
 * T-8 (tepera-dev-spec.md): "На онбордингу показати 5 дефолтних плюс нейтральну «Справи»;
 * користувач вмикає потрібні." Показується першим кроком онбордингу, перед онбординг-оцінкою
 * Online-часу (T-3, [OnlineEstimateOnboardingScreen]): спершу "що саме відмічатимеш", потім
 * Online-час й дозвіл.
 *
 * **Обґрунтування документа:** попередні 5 категорій самі по собі є нормою "чим варто
 * заповнювати вільний час" (FR-P.5 забороняє норми) — людина, чий день переважно робота/дорога/
 * побут, бачила своє життя цілком провалене в "Решту дня". Нова нейтральна "Справи"
 * (`DefaultCategories.kt`, "default-errands") дає такий день законне місце.
 *
 * Перемикачі стартують УВІМКНЕНИМИ (усі 6 сіються `isHidden = false`) — вимкнути можна будь-яку,
 * включно з початковими 5, не лише нову. Вимкнена тут категорія архівується (`archive()`, та
 * сама механіка, що й ручне архівування в `CategoriesScreen`, FR-2.3) — ніякого нового поля в
 * схемі, приймання "не показується ніде, включно з віджетом" уже виконується автоматично:
 * і `HomeScreen`, і `TeperaWidget` читають лише `observeActiveCategories()` (`isHidden = 0`).
 * Набір лишається змінюваним будь-коли з Налаштувань → Категорії (той самий екран) — цей крок
 * лише встановлює стартовий стан, не блокує подальші зміни.
 */
@Composable
fun CategoryOnboardingScreen(
    categoryRepository: CategoryRepository,
    settingsStore: SettingsStore,
    onDone: () -> Unit
) {
    val categories by categoryRepository.observeAllCategories().collectAsState(initial = emptyList())
    // Кастомної категорії на цьому кроці ще не існує (онбординг завжди при першому запуску,
    // до будь-якого створення) — фільтр isDefault лише для однозначності наміру.
    val defaultCategories = categories.filter { it.isDefault }.sortedBy { it.sortOrder }
    val scope = rememberCoroutineScope()

    // Локальний стан перемикачів, окремий від isHidden у БД, доки людина не натисне
    // "Продовжити" — узгоджується з поточним isHidden лише один раз на категорію (LaunchedEffect
    // нижче), щоб повторна рекомпозиція під час гортання списку не скидала вже змінений вибір.
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(defaultCategories) {
        defaultCategories.forEach { category ->
            if (category.id !in selected) selected[category.id] = !category.isHidden
        }
    }

    fun finish() {
        scope.launch {
            defaultCategories.forEach { category ->
                val enabled = selected[category.id] ?: true
                if (enabled && category.isHidden) categoryRepository.unarchive(category.id)
                if (!enabled && !category.isHidden) categoryRepository.archive(category.id)
            }
            settingsStore.setCategoryOnboardingSeen()
            onDone()
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.category_onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.category_onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                defaultCategories.forEach { category ->
                    val enabled = selected[category.id] ?: true
                    GlassRow(
                        label = categoryDisplayName(category),
                        leading = { TeperaIconCircle(icon = categoryIcon(category.iconName)) },
                        trailing = {
                            Switch(
                                checked = enabled,
                                onCheckedChange = { selected[category.id] = it },
                                colors = teperaSwitchColors()
                            )
                        }
                    )
                }
            }

            TeperaButton(
                text = stringResource(R.string.category_onboarding_continue),
                onClick = { finish() },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                type = TeperaButtonType.Primary
            )
        }
    }
}

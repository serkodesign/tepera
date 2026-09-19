package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.util.AppLocale

/**
 * "Мова" — за прямим запитом користувача виокремлено з головного екрана Налаштувань в окремий
 * під-екран (той самий патерн навігації, що Категорії/Виключені застосунки/Ворота). Зміст і
 * логіка перемикача не змінились — перенесено з `SettingsScreen` один в один.
 */
@Composable
fun LanguageSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // LocaleStore (SharedPreferences), не AppCompatDelegate: AppCompatDelegate.
    // setApplicationLocales() застосовує збережену мову до ресурсів лише для AppCompatActivity
    // (через власний attachBaseContext-хук) — MainActivity звичайний ComponentActivity, тож
    // виклик лише запам'ятовував вибір, а UI лишався тою самою мовою (підтверджено на
    // Samsung S23: вибір "English" позначався, але текст лишався українською).
    // Мова застосовується на місці (AppLocale — Compose-state + ProvideAppLocale у MainActivity),
    // без Activity.recreate(): перезапуск обривав анімацію перемикача й давав мигання.
    val selectedLanguageTag = AppLocale.tag(context)

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.settings_language_screen_title), onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                    onSelect = { tag -> AppLocale.set(context, tag) }
                )
            }
        }
    }
}

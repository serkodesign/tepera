package com.serkodesign.tepera.ui.settings

import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.TeperaDialog

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.util.AppLocale
import com.serkodesign.tepera.util.FeedbackForm
import com.serkodesign.tepera.util.PlayStore
import com.serkodesign.tepera.util.ThemeStore

/**
 * Стиль перенесений з Figma-фрейму Everyday_Designs (сторінка "Tepera", node 1951:1106):
 * власний заголовок (GlassScreenHeader) замість TopAppBar, "скляні" картки-рядки для навігації
 * замість ListItem.
 *
 * **Перегруповано 13.10.2026 за прямим запитом власника** (попередні "Активності" + перевантажені "Загальні"):
 * - "Облік часу": Відстеження (орієнтир, виключені застосунки, вікно сну, паузи), Категорії.
 * - "Звички й екран": Застосунки з затримкою, Кнопки віджета.
 * - "Загальні": Мова й Тема — перемикачі прямо тут (окремий екран "Мова" з одним перемикачем прибрано: зайвий тап),
 *   і "Дані й приватність" (резервна копія, тижневий підсумок, звіти про збої, видалення даних — `BackupRestoreScreen`).
 *   Довгі перемикачі з поясненнями (тижневий підсумок, звіти про збої) більше не займають головний екран.
 * - "Про Tepera": База знань (раніше на два рівні в глибину, через "Про застосунок"), Запропонувати функцію,
 *   Оцінити застосунок, Tepera Pro (за прямою вимогою власника лишається видимим рядком на цьому екрані), Про застосунок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenTracking: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenDataPrivacy: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenWidgetSettings: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenProInterest: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // FR-6.4: чесне попередження перед відкриттям браузера — тап не веде туди одразу.
    var showSuggestFeatureConfirm by remember { mutableStateOf(false) }

    // FR-6.4/6.5: чесне попередження, що зараз відкриється браузер (форму приймає Google),
    // перш ніж передати намір системі — і спокійна обробка відсутності браузера (FR-6.5).
    if (showSuggestFeatureConfirm) {
        TeperaDialog(
            onDismissRequest = { showSuggestFeatureConfirm = false },
            title = stringResource(R.string.settings_suggest_feature_confirm_title),
            text = stringResource(R.string.settings_suggest_feature_confirm_body),
            confirmText = stringResource(R.string.settings_suggest_feature_confirm_action),
            dismissText = stringResource(R.string.dialog_cancel),
            onConfirm = {
                    showSuggestFeatureConfirm = false
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FeedbackForm.urlFor(context))))
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, context.getString(R.string.settings_suggest_feature_no_browser), Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.settings_screen_title), onBack = onBack)

            // verticalScroll: без нього нижні рядки виходять за межі екрана й лишаються недосяжними
            // для дотику (підтверджено на Samsung S23) — Column сам по собі не скролиться.
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Видимі відстані заголовків: 16dp зверху, 8dp знизу; проміжок списку 6dp додається сам, тож відступи — 10 і 2
                // (у першого — 18, бо над ним нема попереднього рядка).
                GlassSectionHeader(stringResource(R.string.settings_time_tracking_section), topPadding = 18.dp, bottomPadding = 2.dp)
                GlassRow(
                    label = stringResource(R.string.settings_tracking_action),
                    onClick = onOpenTracking,
                    leading = { TeperaIconCircle(TeperaSymbols.TrackChanges) },
                    trailing = { NavChevron() }
                )
                GlassRow(
                    label = stringResource(R.string.settings_categories_action),
                    onClick = onOpenCategories,
                    leading = { TeperaIconCircle(TeperaSymbols.Category) },
                    trailing = { NavChevron() }
                )

                GlassSectionHeader(stringResource(R.string.settings_habits_section), topPadding = 10.dp, bottomPadding = 2.dp)
                GlassRow(
                    label = stringResource(R.string.settings_gates_action),
                    onClick = onOpenGates,
                    leading = { TeperaIconCircle(TeperaSymbols.Timer) },
                    trailing = { NavChevron() }
                )
                GlassRow(
                    label = stringResource(R.string.settings_widget_action),
                    onClick = onOpenWidgetSettings,
                    leading = { TeperaIconCircle(TeperaSymbols.Widgets) },
                    trailing = { NavChevron() }
                )

                GlassSectionHeader(stringResource(R.string.settings_general_section), topPadding = 10.dp, bottomPadding = 2.dp)
                // Мова застосовується на місці (AppLocale — Compose-state + ProvideAppLocale у MainActivity), без
                // Activity.recreate(): перезапуск обривав анімацію перемикача й давав мигання. LocaleStore, а не
                // AppCompatDelegate — пояснення в util/LocaleStore.kt і CLAUDE.md.
                TeperaCard(title = stringResource(R.string.settings_language_label)) {
                    PillSegmentedControl(
                        options = listOf(
                            "" to stringResource(R.string.settings_language_system),
                            "uk" to stringResource(R.string.settings_language_uk),
                            "en" to stringResource(R.string.settings_language_en)
                        ),
                        selected = AppLocale.tag(context),
                        onSelect = { tag -> AppLocale.set(context, tag) }
                    )
                }
                // Тема — Системна / Світла / Темна (ThemeStore). Вимикається одним прапорем (THEME_CHOICE_ENABLED).
                if (ThemeStore.THEME_CHOICE_ENABLED) {
                    TeperaCard(title = stringResource(R.string.settings_theme_action)) {
                        PillSegmentedControl(
                            options = listOf(
                                ThemeStore.Mode.SYSTEM to stringResource(R.string.settings_theme_system),
                                ThemeStore.Mode.LIGHT to stringResource(R.string.settings_theme_light),
                                ThemeStore.Mode.DARK to stringResource(R.string.settings_theme_dark)
                            ),
                            selected = ThemeStore.current(),
                            onSelect = { ThemeStore.set(context, it) }
                        )
                    }
                }
                ExtraAnimationsToggle()
                GlassRow(
                    label = stringResource(R.string.settings_data_privacy_action),
                    onClick = onOpenDataPrivacy,
                    leading = { TeperaIconCircle(TeperaSymbols.PrivacyTip) },
                    trailing = { NavChevron() }
                )

                GlassSectionHeader(stringResource(R.string.settings_about_section), topPadding = 10.dp, bottomPadding = 2.dp)
                GlassRow(
                    label = stringResource(R.string.settings_knowledge_base_action),
                    onClick = onOpenKnowledgeBase,
                    leading = { TeperaIconCircle(TeperaSymbols.MenuBook) },
                    trailing = { NavChevron() }
                )
                // FR-6.4: чесне попередження перед відкриттям браузера. Свідомо без бейджа й без
                // самостійного нагадування (FR-6.6) — лежить тут і чекає.
                GlassRow(
                    label = stringResource(R.string.settings_suggest_feature_action),
                    onClick = { showSuggestFeatureConfirm = true },
                    leading = { TeperaIconCircle(TeperaSymbols.Feedback) },
                    trailing = { NavChevron() }
                )
                // CC-10: "Оцінити застосунок" — пряме посилання на лістинг Play Store (не Play In-App Review API —
                // та бібліотека для контекстних підказок, не для явного пункту меню, деталі в PlayStore.kt).
                GlassRow(
                    label = stringResource(R.string.settings_rate_action),
                    onClick = { PlayStore.openListing(context, forReview = true) },
                    leading = { TeperaIconCircle(TeperaSymbols.Star) },
                    trailing = { NavChevron() }
                )
                // CC-11: fake door — «Tepera Pro — у розробці», без цін і оплат. За прямою вимогою власника — видимий
                // рядок на головному екрані Налаштувань, не всередині "Про застосунок".
                GlassRow(
                    label = stringResource(R.string.settings_pro_interest_action),
                    onClick = onOpenProInterest,
                    leading = { TeperaIconCircle(TeperaSymbols.AutoAwesome) },
                    trailing = { NavChevron() }
                )
                // GAP-8: посилання на Privacy Policy, версія й контакт — вимога Google Play.
                GlassRow(
                    label = stringResource(R.string.settings_about_action),
                    onClick = onOpenAbout,
                    leading = { TeperaIconCircle(TeperaSymbols.Info) },
                    trailing = { NavChevron() }
                )
            }
        }
    }
}

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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.BuildConfig
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.NavChevron
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.util.AppLocale
import com.serkodesign.tepera.util.FeedbackForm
import com.serkodesign.tepera.util.PlayStore
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import com.serkodesign.tepera.ui.theme.DebugClock
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.MoodStore
import com.serkodesign.tepera.util.ThemeStore

/**
 * Групи Налаштувань: головний екран — лише рядки-переходи, кожна група відкривається окремим екраном
 * ([SettingsGroupScreen]). Згруповано за прямим запитом власника (06.11.2026), було 18 пунктів на одному екрані.
 */
enum class SettingsGroup { DETOX, TIME, SCROLLING, GENERAL, ABOUT }

/**
 * Головний екран Налаштувань (Figma-фрейм Everyday_Designs, node 1951:1106: власний заголовок, "скляні" рядки).
 * Рядки: Облік часу, Години скролінгу (лише beta), Застосунки з затримкою, Загальні, Дані й приватність,
 * Tepera Pro (за прямою вимогою власника лишається видимим тут) і Про Tepera.
 */
@Composable
fun SettingsScreen(
    onOpenGroup: (SettingsGroup) -> Unit,
    settingsStore: com.serkodesign.tepera.data.local.SettingsStore,
    onOpenDataPrivacy: () -> Unit,
    onOpenProInterest: () -> Unit,
    onBack: () -> Unit
) {
    // Облік часу тепер містить лише "Категорії", які в режимі v3 приховані — тоді рядок не показуємо.
    val v3Mode by settingsStore.v3ModeEnabled.collectAsState(initial = BuildConfig.V3_MVP)
    val v3Active = BuildConfig.V3_MVP && v3Mode
    SettingsList(title = stringResource(R.string.settings_screen_title), onBack = onBack) {
        // Цифровий детокс (07.11.2026, за запитом власника): Відстеження + Години скролінгу + Застосунки з затримкою.
        GlassRow(
            label = stringResource(R.string.settings_detox_section),
            onClick = { onOpenGroup(SettingsGroup.DETOX) },
            leading = { TeperaIconCircle(TeperaSymbols.TrackChanges) },
            trailing = { NavChevron() }
        )
        if (!v3Active) GlassRow(
            label = stringResource(R.string.settings_time_tracking_section),
            onClick = { onOpenGroup(SettingsGroup.TIME) },
            leading = { TeperaIconCircle(TeperaSymbols.Schedule) },
            trailing = { NavChevron() }
        )
        GlassRow(
            label = stringResource(R.string.settings_general_section),
            onClick = { onOpenGroup(SettingsGroup.GENERAL) },
            leading = { TeperaIconCircle(TeperaSymbols.Palette) },
            trailing = { NavChevron() }
        )
        GlassRow(
            label = stringResource(R.string.settings_data_privacy_action),
            onClick = onOpenDataPrivacy,
            leading = { TeperaIconCircle(TeperaSymbols.PrivacyTip) },
            trailing = { NavChevron() }
        )
        // CC-11: fake door — «Tepera Pro — у розробці», без цін і оплат. За прямою вимогою власника — видимий
        // рядок на головному екрані Налаштувань, не всередині "Про Tepera".
        GlassRow(
            label = stringResource(R.string.settings_pro_interest_action),
            onClick = onOpenProInterest,
            leading = { TeperaIconCircle(TeperaSymbols.AutoAwesome) },
            trailing = { NavChevron() }
        )
        GlassRow(
            label = stringResource(R.string.settings_about_section),
            onClick = { onOpenGroup(SettingsGroup.ABOUT) },
            leading = { TeperaIconCircle(TeperaSymbols.Info) },
            trailing = { NavChevron() }
        )
    }
}

/** Спільний каркас екранів Налаштувань: заголовок + прокручуваний список рядків. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsList(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = title, onBack = onBack)
            // verticalScroll: без нього нижні рядки виходять за межі екрана й лишаються недосяжними
            // для дотику (підтверджено на Samsung S23) — Column сам по собі не скролиться.
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

/**
 * Екран однієї групи Налаштувань.
 * - TIME: Відстеження, Категорії (у режимі v3 категорії приховані; дані лишаються в базі).
 * - SCROLLING (лише beta): Режим v3, підказка поверх стрічки, задачки, Застосунки-стрічки.
 * - GENERAL: Мова, Тема, Настрій, Додаткові анімації, Кнопки віджета (у режимі v3 приховані).
 * - ABOUT: База знань, Запропонувати функцію, Оцінити застосунок, Про застосунок.
 */
@Composable
fun SettingsGroupScreen(
    group: SettingsGroup,
    onOpenTracking: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenGroup: (SettingsGroup) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenWidgetSettings: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFeedPicker: () -> Unit,
    settingsStore: com.serkodesign.tepera.data.local.SettingsStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Етап 6: у режимі v3 (beta) старі категорії й кнопки віджета приховані; дані в базі лишаються.
    val v3Mode by settingsStore.v3ModeEnabled.collectAsState(initial = BuildConfig.V3_MVP)
    val v3Active = BuildConfig.V3_MVP && v3Mode

    // FR-6.4/6.5: чесне попередження, що зараз відкриється браузер (форму приймає Google), перш ніж передати намір
    // системі — і спокійна обробка відсутності браузера.
    var showSuggestFeatureConfirm by remember { mutableStateOf(false) }
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

    val title = stringResource(
        when (group) {
            SettingsGroup.DETOX -> R.string.settings_detox_section
            SettingsGroup.TIME -> R.string.settings_time_tracking_section
            SettingsGroup.SCROLLING -> R.string.v3_section_title
            SettingsGroup.GENERAL -> R.string.settings_general_section
            SettingsGroup.ABOUT -> R.string.settings_about_section
        }
    )
    SettingsList(title = title, onBack = onBack) {
        when (group) {
            SettingsGroup.DETOX -> {
                GlassRow(
                    label = stringResource(R.string.settings_tracking_action),
                    onClick = onOpenTracking,
                    leading = { TeperaIconCircle(TeperaSymbols.TrackChanges) },
                    trailing = { NavChevron() }
                )
                // v3.0 MVP (docs/CLAUDE-CODE-PLAN-v3-MVP.md): лише beta-збірка.
                if (BuildConfig.V3_MVP) {
                    GlassRow(
                        label = stringResource(R.string.v3_section_title),
                        onClick = { onOpenGroup(SettingsGroup.SCROLLING) },
                        leading = { TeperaIconCircle(TeperaSymbols.Apps) },
                        trailing = { NavChevron() }
                    )
                }
                GlassRow(
                    label = stringResource(R.string.settings_gates_action),
                    onClick = onOpenGates,
                    leading = { TeperaIconCircle(TeperaSymbols.Timer) },
                    trailing = { NavChevron() }
                )
            }

            SettingsGroup.TIME -> {
                if (!v3Active) GlassRow(
                    label = stringResource(R.string.settings_categories_action),
                    onClick = onOpenCategories,
                    leading = { TeperaIconCircle(TeperaSymbols.Category) },
                    trailing = { NavChevron() }
                )
            }

            SettingsGroup.SCROLLING -> {
                com.serkodesign.tepera.ui.risk.V3ModeToggle(settingsStore)
                GlassRow(
                    label = stringResource(R.string.v3_feed_picker_action),
                    onClick = onOpenFeedPicker,
                    leading = { TeperaIconCircle(TeperaSymbols.Apps) },
                    trailing = { NavChevron() }
                )
            }

            SettingsGroup.GENERAL -> {
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
                // Тема — Системна / Світла / Темна / За часом (ThemeStore). Вимикається одним прапорем (THEME_CHOICE_ENABLED).
                if (ThemeStore.THEME_CHOICE_ENABLED) {
                    TeperaCard(title = stringResource(R.string.settings_theme_action)) {
                        PillSegmentedControl(
                            options = listOf(
                                ThemeStore.Mode.SYSTEM to stringResource(R.string.settings_theme_system),
                                ThemeStore.Mode.LIGHT to stringResource(R.string.settings_theme_light),
                                ThemeStore.Mode.DARK to stringResource(R.string.settings_theme_dark),
                                ThemeStore.Mode.TIME to stringResource(R.string.settings_theme_time)
                            ),
                            selected = ThemeStore.current(),
                            onSelect = { ThemeStore.set(context, it) }
                        )
                        if (ThemeStore.current() == ThemeStore.Mode.TIME) {
                            Text(
                                stringResource(R.string.settings_theme_time_hint),
                                color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
                            )
                            // Півкуля визначає, коли в році вечір настає раніше (взимку); без геолокації.
                            Text(
                                stringResource(R.string.settings_hemisphere_label),
                                color = TeperaPalette.textPrimary, fontSize = 14.sp, lineHeight = 20.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            PillSegmentedControl(
                                options = listOf(
                                    ThemeStore.Hemisphere.NORTH to stringResource(R.string.settings_hemisphere_north),
                                    ThemeStore.Hemisphere.SOUTH to stringResource(R.string.settings_hemisphere_south)
                                ),
                                selected = ThemeStore.hemisphere(),
                                onSelect = { ThemeStore.setHemisphere(context, it) }
                            )
                        }
                    }
                }
                // ТИМЧАСОВО (лише debug): повзунок години, щоб побачити тему «За часом» у будь-яку пору доби.
                if (BuildConfig.DEBUG) {
                    val debugMinute = DebugClock.override.value
                    val shown = debugMinute ?: java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                    TeperaCard(title = stringResource(R.string.settings_debug_clock_title)) {
                        Text(
                            String.format(java.util.Locale.ROOT, "%02d:%02d", shown / 60, shown % 60) +
                                if (debugMinute == null) " · " + stringResource(R.string.settings_debug_clock_real) else "",
                            color = TeperaPalette.textPrimary, fontSize = 20.sp, lineHeight = 28.sp
                        )
                        androidx.compose.material3.Slider(
                            value = shown.toFloat(),
                            onValueChange = { DebugClock.override.value = it.toInt().coerceIn(0, 1439) },
                            valueRange = 0f..1439f
                        )
                        TeperaButton(
                            text = stringResource(R.string.settings_debug_clock_reset),
                            onClick = { DebugClock.override.value = null },
                            type = TeperaButtonType.Secondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                // Настрій — Поточний / Світанок (MoodStore): новий стиль поряд зі старим, незалежно від теми вище.
                TeperaCard(title = stringResource(R.string.settings_mood_title)) {
                    PillSegmentedControl(
                        options = listOf(
                            MoodStore.Mood.CLASSIC to stringResource(R.string.settings_mood_classic),
                            MoodStore.Mood.DAWN to stringResource(R.string.settings_mood_dawn)
                        ),
                        selected = MoodStore.current(),
                        onSelect = { MoodStore.set(context, it) }
                    )
                }
                ExtraAnimationsToggle()
                if (!v3Active) GlassRow(
                    label = stringResource(R.string.settings_widget_action),
                    onClick = onOpenWidgetSettings,
                    leading = { TeperaIconCircle(TeperaSymbols.Widgets) },
                    trailing = { NavChevron() }
                )
            }

            SettingsGroup.ABOUT -> {
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

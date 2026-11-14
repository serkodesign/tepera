package com.serkodesign.tepera.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.serkodesign.tepera.ui.tutorial.TutorialHintCard
import com.serkodesign.tepera.util.TutorialStore
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import com.serkodesign.tepera.util.AppLanguages
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.serkodesign.tepera.BuildConfig
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.gates.rememberAccessibilityRequest
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.M3GroupLabel
import com.serkodesign.tepera.ui.theme.M3ListDivider
import com.serkodesign.tepera.ui.theme.M3ListGroup
import com.serkodesign.tepera.ui.theme.M3ListIcon
import com.serkodesign.tepera.ui.theme.M3ListItem
import com.serkodesign.tepera.ui.theme.PillSegmentedControl
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaInfoButton
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import com.serkodesign.tepera.util.A11yFeatureState
import com.serkodesign.tepera.util.AppLocale
import com.serkodesign.tepera.util.FeedbackForm
import com.serkodesign.tepera.util.GateAccessibility
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.util.PlayStore
import kotlinx.coroutines.launch

/**
 * Головний екран Налаштувань — Figma "Tepera", T2 · Налаштування (1066:4191). Групи з макета: «Твої години» і «У момент
 * відкриття» (лише режим годин скролінгу, beta), «Застосунок»; решта колишніх пунктів — тими самими рядками в групі «Ще»
 * (рішення власника, 11.11.2026: нічого не зникає).
 *
 * Відхилення від макета (рішення власника): планів «якщо — то» немає, тож «план» → «ідея»; рядків «Години ризику»
 * (вікна лишаються на Головній), «Нагадування до години» і «Підтримати Tepera» поки немає.
 */
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onOpenGroup: (SettingsGroup) -> Unit,
    onOpenFeedPicker: () -> Unit,
    onOpenTracking: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenDataPrivacy: () -> Unit,
    onOpenProInterest: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    val scope = rememberCoroutineScope()
    val v3Mode by settingsStore.v3ModeEnabled.collectAsState(initial = BuildConfig.V3_MVP)
    val v3Active = BuildConfig.V3_MVP && v3Mode
    val feeds by app.feedAppRepository.observeAll().collectAsState(initial = emptyList())

    // Стан служби доступності могли змінити в системних налаштуваннях — перечитуємо при поверненні.
    var a11yState by remember { mutableStateOf(A11yFeatureState.current(context)) }
    LifecycleResumeEffect(Unit) {
        a11yState = A11yFeatureState.current(context)
        onPauseOrDispose { }
    }
    val a11yOn = a11yState == A11yFeatureState.Active || a11yState == A11yFeatureState.Unavailable
    var overlayOn by remember { mutableStateOf(PlanOverlayStore.isEnabled(context)) }
    val requestA11y = rememberAccessibilityRequest(
        onActive = { overlayOn = true; PlanOverlayStore.setEnabled(context, true) },
        onAgreed = { overlayOn = true; PlanOverlayStore.setEnabled(context, true) }
    )

    var showSuggest by remember { mutableStateOf(false) }
    if (showSuggest) {
        // FR-6.4/6.5: чесне попередження, що зараз відкриється браузер (форму приймає Google).
        TeperaDialog(
            onDismissRequest = { showSuggest = false },
            title = stringResource(R.string.settings_suggest_feature_confirm_title),
            text = stringResource(R.string.settings_suggest_feature_confirm_body),
            confirmText = stringResource(R.string.settings_suggest_feature_confirm_action),
            dismissText = stringResource(R.string.dialog_cancel),
            onConfirm = {
                showSuggest = false
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FeedbackForm.urlFor(context))))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, context.getString(R.string.settings_suggest_feature_no_browser), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.settings_screen_title), onBack = onBack)
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (v3Active) {
                    var hintSeen by remember { mutableStateOf(TutorialStore.isSeen(context, TutorialStore.SETTINGS_HINT)) }
                    if (!hintSeen) {
                        TutorialHintCard(
                            R.string.tutorial_settings_hint_title, R.string.tutorial_settings_hint_body,
                            onDismiss = { hintSeen = true; TutorialStore.markSeen(context, TutorialStore.SETTINGS_HINT) }
                        )
                    }
                }
                // Одна група «Цифровий детокс» (рішення власника, 14.11.2026): що рахуємо й що відбувається при відкритті стрічки.
                if (BuildConfig.V3_MVP) {
                    M3GroupLabel(stringResource(R.string.settings_group_hours))
                    M3ListGroup {
                        // Режим годин скролінгу (beta): вимкнення повертає звичайну Головну; дані не видаляються.
                        M3ListItem(
                            title = stringResource(R.string.v3_mode_label),
                            icon = TeperaSymbols.AutoAwesome,
                            trailing = {
                                Switch(
                                    checked = v3Mode,
                                    onCheckedChange = { on -> scope.launch { settingsStore.setV3ModeEnabled(on) } },
                                    colors = teperaSwitchColors()
                                )
                            }
                        )
                        if (v3Active) {
                            M3ListDivider()
                            M3ListItem(
                                title = stringResource(R.string.v3_feed_picker_action),
                                icon = TeperaSymbols.Smartphone,
                                value = feeds.size.toString(),
                                onClick = onOpenFeedPicker
                            )
                        }
                    }
                }
                if (v3Active) {
                    // Підгрупа тієї самої секції: тихіший підпис без відступу, як продовження «Цифрового детоксу».
                    Text(
                        stringResource(R.string.settings_group_opening),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp).semantics { heading() }
                    )
                    M3ListGroup {
                        M3ListItem(
                            title = stringResource(R.string.settings_overlay_title),
                            supporting = stringResource(R.string.settings_overlay_supporting),
                            icon = TeperaIcons.OpenFeed,
                            trailing = {
                                Switch(
                                    checked = overlayOn && a11yOn,
                                    onCheckedChange = { on ->
                                        // Вмикається лише зі згодою й увімкненою службою; «Не зараз» лишає вимкненим.
                                        if (on) requestA11y() else { overlayOn = false; PlanOverlayStore.setEnabled(context, false) }
                                    },
                                    colors = teperaSwitchColors()
                                )
                            }
                        )
                        if (GateAccessibility.isAvailable) {
                            M3ListDivider()
                            M3ListItem(
                                title = stringResource(R.string.settings_a11y_title),
                                icon = TeperaSymbols.Check,
                                value = stringResource(if (a11yOn) R.string.settings_a11y_on else R.string.settings_a11y_off),
                                // Увімкнено — системні налаштування служби (там і вимикається); ні — спершу розкриття.
                                onClick = { if (a11yOn) GateAccessibility.openSettings(context) else requestA11y() }
                            )
                        }
                        M3ListDivider()
                        var puzzlesOn by remember { mutableStateOf(PlanOverlayStore.arePuzzlesEnabled(context)) }
                        M3ListItem(
                            title = stringResource(R.string.v3_puzzle_toggle_label),
                            icon = TeperaSymbols.Keyboard,
                            trailing = {
                                Switch(
                                    checked = puzzlesOn,
                                    onCheckedChange = { on -> puzzlesOn = on; PlanOverlayStore.setPuzzlesEnabled(context, on) },
                                    colors = teperaSwitchColors()
                                )
                            }
                        )
                        M3ListDivider()
                        DimSetting()
                    }
                }

                M3GroupLabel(stringResource(R.string.settings_group_app))
                M3ListGroup {
                    M3ListItem(
                        title = stringResource(R.string.settings_language_label),
                        icon = TeperaSymbols.Language,
                        value = languageName(AppLocale.tag(context)),
                        onClick = onOpenLanguage
                    )
                    M3ListDivider()
                    M3ListItem(
                        title = stringResource(R.string.settings_appearance_title),
                        icon = TeperaSymbols.Palette,
                        onClick = { onOpenGroup(SettingsGroup.GENERAL) }
                    )
                    M3ListDivider()
                    M3ListItem(
                        title = stringResource(R.string.settings_suggest_feature_action),
                        supporting = stringResource(R.string.settings_suggest_supporting),
                        icon = TeperaSymbols.Feedback,
                        onClick = { showSuggest = true }
                    )
                }

                M3GroupLabel(stringResource(R.string.settings_group_more))
                M3ListGroup {
                    M3ListItem(stringResource(R.string.settings_tracking_action), icon = TeperaSymbols.TrackChanges, onClick = onOpenTracking)
                    M3ListDivider()
                    M3ListItem(stringResource(R.string.settings_gates_action), icon = TeperaSymbols.PauseCircle, onClick = onOpenGates)
                    if (!v3Active) {
                        M3ListDivider()
                        M3ListItem(stringResource(R.string.settings_categories_action), icon = TeperaSymbols.Category, onClick = onOpenCategories)
                    }
                    M3ListDivider()
                    M3ListItem(stringResource(R.string.settings_data_privacy_action), icon = TeperaSymbols.PrivacyTip, onClick = onOpenDataPrivacy)
                    M3ListDivider()
                    // CC-11: fake door «Tepera Pro — у розробці», без цін і оплат; за вимогою власника — видимий рядок.
                    M3ListItem(stringResource(R.string.settings_pro_interest_action), icon = TeperaSymbols.Star, onClick = onOpenProInterest)
                    M3ListDivider()
                    M3ListItem(stringResource(R.string.settings_knowledge_base_action), icon = TeperaSymbols.MenuBook, onClick = onOpenKnowledgeBase)
                    M3ListDivider()
                    M3ListItem(
                        stringResource(R.string.settings_rate_action), icon = TeperaSymbols.Favorite,
                        onClick = { PlayStore.openListing(context, forReview = true) }
                    )
                    M3ListDivider()
                    if (v3Active) {
                        // Скидає всі підказки; повертаємось на Головну, де ланцюжок стартує знову.
                        M3ListItem(
                            stringResource(R.string.settings_replay_tutorial), icon = TeperaSymbols.Info,
                            onClick = { TutorialStore.reset(context); onBack() }
                        )
                        M3ListDivider()
                    }
                    M3ListItem(stringResource(R.string.settings_about_action), icon = TeperaSymbols.Info, onClick = onOpenAbout)
                }

                Text(
                    stringResource(R.string.settings_footer_format, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        }
    }
}

/** `SettingCard` «Приглушення»: заголовок з ⓘ, підпис і перемикач М'яке / Помітне (Figma 1066:4201). */
@Composable
private fun DimSetting() {
    val context = LocalContext.current
    var strong by remember { mutableStateOf(PlanOverlayStore.isDimStrong(context)) }
    // Ті самі поля, проміжок і висота рядка, що в M3ListItem (16×8dp, 12dp між іконкою й текстом, 56dp мінімум), без
    // додаткового проміжку між назвою й підписом; ⓘ — праворуч окремим слотом (як шеврон), тож не збільшує висоту тексту.
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            M3ListIcon(TeperaSymbols.PauseCircle)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_dim_title),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.settings_dim_hint),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TeperaInfoButton(stringResource(R.string.settings_dim_title), stringResource(R.string.settings_dim_info))
        }
        PillSegmentedControl(
            options = listOf(false to stringResource(R.string.settings_dim_soft), true to stringResource(R.string.settings_dim_strong)),
            selected = strong,
            onSelect = { strong = it; PlanOverlayStore.setDimStrong(context, it) }
        )
    }
}

@Composable
private fun languageName(tag: String): String = AppLanguages.endonym(tag) ?: stringResource(R.string.settings_language_system)

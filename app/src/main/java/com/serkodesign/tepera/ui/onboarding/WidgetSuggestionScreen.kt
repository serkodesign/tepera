package com.serkodesign.tepera.ui.onboarding

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.OnboardingSkipAllButton
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.widget.FIGMA_TINTS
import com.serkodesign.tepera.widget.TeperaWidgetReceiver
import com.serkodesign.tepera.widget.WIDGET_CIRCLE_UNSELECTED
import com.serkodesign.tepera.widget.WIDGET_CIRCLE_UNSELECTED_NIGHT
import com.serkodesign.tepera.widget.WIDGET_GLYPH_UNSELECTED_NIGHT
import com.serkodesign.tepera.widget.WIDGET_GLYPH_UNSELECTED
import com.serkodesign.tepera.widget.widgetIconRes

/**
 * "Пропозиція віджета" — останній крок онбордингу (Figma user-flow k6s4prQ9oK9x2uUvzHRghR,
 * node 14:791: ОБИДВІ гілки "доступ надано? так/ні" сходяться сюди, перед Home). Черговість кроків
 * визначає `TeperaNavHost` (`nextOnboardingRoute`) — цей екран показується після кроку дозволу
 * незалежно від того, чи доступ реально надано.
 *
 * Розкладка за M3: вміст по центру (міні-прев'ю віджета + заголовок + текст), головні дії
 * закріплені внизу — як на екрані дозволів. Прев'ю — не картинка, а той самий вигляд віджета,
 * зібраний із його ж гліфів і відтінків ([WidgetMiniPreview]), тож не розходиться з реальним.
 *
 * `requestPinAppWidget()` — той самий принцип, що `GateRepository.createGate()` для ярликів
 * воріт: викликається одразу на диспетчері виклику (тут — Main, композиційний потік), без
 * перемикання на IO, бо лаунчер перевіряє, що застосунок щойно на передньому плані від дії
 * користувача, перш ніж показати системний діалог розміщення.
 */
@Composable
fun WidgetSuggestionScreen(
    settingsStore: SettingsStore,
    onDone: () -> Unit,
    onSkipAll: () -> Unit
) {
    val context = LocalContext.current

    // "Показано" фіксується одразу при відкритті — незалежно від того, чи користувач натисне
    // "Додати віджет", чи "Пропустити" (той самий принцип, що OnboardingScreen).
    LaunchedEffect(Unit) {
        settingsStore.setWidgetSuggestionSeen()
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WidgetMiniPreview()
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.widget_suggestion_title),
                    color = TeperaPalette.buttonBrandDark,
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 27.sp,
                    lineHeight = 29.7.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.widget_suggestion_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TeperaPalette.buttonBrandDark.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TeperaButton(
                    text = stringResource(R.string.widget_suggestion_add_action),
                    onClick = {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, TeperaWidgetReceiver::class.java)
                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                            appWidgetManager.requestPinAppWidget(provider, null, null)
                        }
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    type = TeperaButtonType.Primary
                )
                TeperaButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    type = TeperaButtonType.Tertiary
                )
            }
        }
        OnboardingSkipAllButton(
            onClick = onSkipAll,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        }
    }
}

private const val PREVIEW_GRID_COLUMNS = 12
private const val PREVIEW_GRID_ROWS = 4

/**
 * Зменшена копія реального віджета (кнопки категорій + сітка доби, `widget/TeperaWidget.kt`) — ті
 * самі гліфи/відтінки ([FIGMA_TINTS], [widgetIconRes]), тож прев'ю не розходиться з дійсністю.
 * Лише ілюстрація: не інтерактивна і не читається скрінрідером (сам віджет описано текстом нижче).
 */
@Composable
private fun WidgetMiniPreview() {
    val previewCategories = DefaultCategories.all.take(5)
    // ui-redesign: прев'ю повторює реальний віджет — у темній системній темі темні кола й капсула (drawable-night).
    val dark = TeperaPalette.colors.isDark
    val surface = if (dark) Color(0x4D062924) else Color.White.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF7FBF9C), Color(0xFF1F5A44))))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(surface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                previewCategories.forEachIndexed { index, category ->
                    val selected = index == 0
                    val tint = if (selected) FIGMA_TINTS[category.nameKey] else null
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tint?.circle ?: if (dark) WIDGET_CIRCLE_UNSELECTED_NIGHT else WIDGET_CIRCLE_UNSELECTED),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(widgetIconRes(category.iconName, selected = selected)),
                            contentDescription = null,
                            tint = tint?.glyph ?: if (dark) WIDGET_GLYPH_UNSELECTED_NIGHT else WIDGET_GLYPH_UNSELECTED,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surface)
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(PREVIEW_GRID_ROWS) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            repeat(PREVIEW_GRID_COLUMNS) { col ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(previewCellColor(row, col))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Демо-розфарбування прев'ю сітки: ніч на початку, кілька категорій і Online ближче до вечора. */
private fun previewCellColor(row: Int, col: Int): Color {
    val index = row * PREVIEW_GRID_COLUMNS + col
    return when {
        index < 14 -> Color(0xFFA172FF) // до точки старту дня
        index in 16..18 || index in 25..26 -> TeperaPalette.onlineCard
        index in 20..22 -> Color(0xFFD28FDF) // читання
        index == 23 -> Color(0xFFFD5B5E) // рух/спорт
        index == 27 -> Color(0xFF00D8CD) // хобі
        index in 14..15 || index == 19 || index == 24 || index == 28 -> Color.White
        else -> Color.White.copy(alpha = 0.5f)
    }
}

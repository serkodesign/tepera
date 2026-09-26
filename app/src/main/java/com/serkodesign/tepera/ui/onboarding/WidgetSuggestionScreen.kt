package com.serkodesign.tepera.ui.onboarding

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.widget.FIGMA_TINTS
import com.serkodesign.tepera.widget.TeperaWidgetReceiver
import com.serkodesign.tepera.widget.WIDGET_CIRCLE_UNSELECTED
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
    onDone: () -> Unit
) {
    val context = LocalContext.current

    // "Показано" фіксується одразу при відкритті — незалежно від того, чи користувач натисне
    // "Додати віджет", чи "Пропустити" (той самий принцип, що OnboardingScreen).
    LaunchedEffect(Unit) {
        settingsStore.setWidgetSuggestionSeen()
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
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
    }
}

/**
 * Зменшена копія розширеного віджета (Figma "App concept", nodes 236:956 / 234:848): ряд із п'яти
 * кіл-кнопок у напівпрозорій капсулі та картка сітки доби 12×4 — на "шпалерах" (градієнтний фон,
 * бо віджет сам фону не має і лежить прямо на шпалерах). Гліфи й відтінки беруться з віджета
 * ([widgetIconRes], [FIGMA_TINTS]) — перша кнопка вибрана, як на макеті. Лише ілюстрація: не інтерактивна
 * і не читається скрінрідером (сам віджет описано текстом нижче).
 */
@Composable
private fun WidgetMiniPreview() {
    val keys = listOf("nature", "reading", "hobby", "movement", "social")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF7FBF9C), Color(0xFF1F5A44)))
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Капсула з кнопками: #FFFFFF@30%, радіус 100, відступ 8 (пропорційно зменшена).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                keys.forEachIndexed { index, key ->
                    val selected = index == 0
                    val tint = FIGMA_TINTS.getValue(key)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selected) tint.circle else WIDGET_CIRCLE_UNSELECTED),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(widgetIconRes(key, selected)),
                            contentDescription = null,
                            tint = if (selected) tint.glyph else WIDGET_GLYPH_UNSELECTED,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Картка сітки доби: #FFFFFF@30%, радіус 16, відступ 12; клітинки — квадрати з проміжком 3.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(PREVIEW_GRID_ROWS) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(PREVIEW_GRID_COLUMNS) { column ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(previewCellColor(row * PREVIEW_GRID_COLUMNS + column))
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val PREVIEW_GRID_COLUMNS = 12
private const val PREVIEW_GRID_ROWS = 4

/**
 * Демо-доба для прев'ю (те саме, що показує реальний віджет): клітинки до початку дня — фіолетові,
 * далі Online / записи категорій / порожні, решта — майбутнє (напівпрозорий сірий).
 */
private fun previewCellColor(index: Int): Color = when {
    index < 12 -> Color(0xFFA172FF)
    index in 12..14 -> TeperaPalette.onlineCard
    index in 15..17 -> Color(0xFF00B938)
    index in 18..19 -> Color.White
    index in 20..21 -> Color(0xFFD28FDF)
    index in 22..23 -> TeperaPalette.onlineCard
    index in 24..26 -> Color(0xFFFD5B5E)
    index in 27..28 -> Color.White
    index in 29..30 -> TeperaPalette.onlineCard
    else -> Color(0x80A7A7A7)
}

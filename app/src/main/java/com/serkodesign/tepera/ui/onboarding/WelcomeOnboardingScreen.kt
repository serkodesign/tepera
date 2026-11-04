package com.serkodesign.tepera.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.M3Button
import com.serkodesign.tepera.ui.theme.M3ButtonSize
import com.serkodesign.tepera.ui.theme.M3ButtonType
import com.serkodesign.tepera.ui.theme.OnboardingTopBar
import com.serkodesign.tepera.ui.theme.TeperaSpacing
import kotlinx.coroutines.launch

/**
 * Перший екран онбордингу (Figma "App concept", Onboarding / Welcome — переоформлений за референсом
 * node 607:2475): одним екраном пояснює цінність — тихий трекер без суду. Усе вирівняно по лівому краю: іконка
 * застосунку, великий заголовок, абзац і список цінностей БЕЗ карток — лише кругла плитка з лінійною іконкою й
 * великий підпис поруч (як у референсі). Три цінності: без оцінок і балів, без шеймінгу й поспіху, дані лишаються
 * на телефоні (розділи 2.1/2.2 ТЗ, FR-P.5). Одна дія — «Почати». "Показано" фіксується при натисканні.
 *
 * Дизайн-система v3.1 (I18b): заголовок `headlineLarge`/`primary`, абзац `bodyLarge`/`onSurfaceVariant`, підписи цінностей
 * `titleLarge`/`onSurface`, плитка `surfaceContainerHighest`, кнопка `M3Button(Filled, M)`, відступи `TeperaSpacing`.
 */
@Composable
fun WelcomeOnboardingScreen(
    settingsStore: SettingsStore,
    onDone: () -> Unit,
    onSkipAll: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = 48.dp,
                        start = TeperaSpacing.screenHorizontal,
                        end = TeperaSpacing.screenHorizontal,
                        bottom = TeperaSpacing.xxl
                    )
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    AppIconTile()
                    Text(
                        text = stringResource(R.string.welcome_onboarding_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .padding(top = TeperaSpacing.xxl)
                            .semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.welcome_onboarding_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = TeperaSpacing.m)
                    )
                    Column(
                        modifier = Modifier.padding(top = TeperaSpacing.xxxl),
                        verticalArrangement = Arrangement.spacedBy(TeperaSpacing.xxl)
                    ) {
                        ValueRow(R.drawable.ic_welcome_scores, stringResource(R.string.welcome_principle_no_scores))
                        ValueRow(R.drawable.ic_welcome_calm, stringResource(R.string.welcome_principle_no_shaming))
                        ValueRow(R.drawable.ic_welcome_private, stringResource(R.string.welcome_principle_private))
                    }
                }
                M3Button(
                    type = M3ButtonType.Filled,
                    size = M3ButtonSize.M,
                    text = stringResource(R.string.welcome_onboarding_start),
                    onClick = {
                        scope.launch {
                            settingsStore.setWelcomeOnboardingSeen()
                            onDone()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TeperaSpacing.l)
                )
            }
            OnboardingTopBar(
                step = 1,
                onSkipAll = onSkipAll,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/** Іконка застосунку (72dp, кут 20.6dp) — той самий візерунок, що на екрані дозволів, але менший. */
@Composable
internal fun AppIconTile() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(20.6.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Рядок цінності за референсом (Figma 607:2478): плитка-коло 56dp (`surfaceContainerHighest`), лінійна іконка по
 * центру (вектор 160dp масштабується до плитки, колір `primary`) і підпис `titleLarge`, проміжок 12dp. Без картки/фону рядка.
 */
@Composable
private fun ValueRow(@DrawableRes icon: Int, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TeperaSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

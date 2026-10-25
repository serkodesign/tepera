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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.OnboardingTopBar
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlinx.coroutines.launch

/**
 * Перший екран онбордингу (Figma "App concept", Onboarding / Welcome — переоформлений за референсом
 * node 607:2475): одним екраном пояснює цінність — тихий трекер без суду. Усе вирівняно по лівому краю: іконка
 * застосунку, великий заголовок, абзац і список цінностей БЕЗ карток — лише кругла напівпрозора плитка з
 * лінійною іконкою й великий підпис поруч (як у референсі). Три цінності: без оцінок і балів, без шеймінгу й
 * поспіху, дані лишаються на телефоні (розділи 2.1/2.2 ТЗ, FR-P.5). Одна дія — «Почати».
 * "Показано" фіксується при натисканні.
 */
@Composable
fun WelcomeOnboardingScreen(
    settingsStore: SettingsStore,
    onDone: () -> Unit,
    onSkipAll: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ink = TeperaPalette.buttonBrandDark

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
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
                        color = ink,
                        fontFamily = TeperaPalette.headlineFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 34.sp,
                        lineHeight = 36.sp,
                        modifier = Modifier
                            .padding(top = 28.dp)
                            .semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.welcome_onboarding_body),
                        color = ink.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Column(
                        modifier = Modifier.padding(top = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        ValueRow(R.drawable.ic_welcome_scores, stringResource(R.string.welcome_principle_no_scores))
                        ValueRow(R.drawable.ic_welcome_calm, stringResource(R.string.welcome_principle_no_shaming))
                        ValueRow(R.drawable.ic_welcome_private, stringResource(R.string.welcome_principle_private))
                    }
                }
                TeperaButton(
                    text = stringResource(R.string.welcome_onboarding_start),
                    onClick = {
                        scope.launch {
                            settingsStore.setWelcomeOnboardingSeen()
                            onDone()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    type = TeperaButtonType.Primary
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
 * Рядок цінності за референсом (Figma 607:2478): плитка-коло 56dp з 10% кольору тексту, лінійна іконка по центру
 * (вектор 160dp масштабується до плитки) і підпис 22sp Medium, проміжок 14dp. Без картки/фону рядка.
 */
@Composable
private fun ValueRow(@DrawableRes icon: Int, label: String) {
    val ink = TeperaPalette.buttonBrandDark
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ink.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = label,
            color = ink.copy(alpha = 0.88f),
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 24.2.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.OnboardingTopBar
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaOnboardingTitle
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import kotlinx.coroutines.launch

/**
 * Перший екран онбордингу (Figma "App concept", Onboarding / Welcome, світла й темна версії): одним екраном
 * пояснює цінність — тихий трекер без суду. Іконка застосунку в "орбіті", заголовок, один абзац і три принципи
 * (без оцінок, без шеймінгу, без порівнянь з нормою — розділи 2.1/2.2 ТЗ і FR-P.5). Одна дія — «Почати».
 * "Показано" фіксується при натисканні (як решта кроків, що мають власну кнопку).
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    WelcomeOrb()
                    TeperaOnboardingTitle(
                        text = stringResource(R.string.welcome_onboarding_title),
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    Text(
                        text = stringResource(R.string.welcome_onboarding_body),
                        color = ink.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Column(
                        modifier = Modifier.padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrincipleRow(stringResource(R.string.welcome_principle_no_scores))
                        PrincipleRow(stringResource(R.string.welcome_principle_no_shaming))
                        PrincipleRow(stringResource(R.string.welcome_principle_no_norm))
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

/** Іконка застосунку в тонкій орбіті — тиха, без анімації (Welcome має читатись миттєво). */
@Composable
private fun WelcomeOrb() {
    val ring = TeperaPalette.buttonBrandDark.copy(alpha = 0.12f)
    val glow = TeperaPalette.buttonBrand.copy(alpha = 0.14f)
    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = glow, radius = size.minDimension / 2f, center = Offset(size.width / 2f, size.height / 2f))
            drawCircle(
                color = ring,
                radius = size.minDimension / 2f - 6.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(27.4.dp))
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
}

/** Рядок принципу: темне коло з галкою + підпис, на скляній картці (як чекліст дозволів). */
@Composable
private fun PrincipleRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TeperaPalette.buttonBrandDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                TeperaSymbols.Check,
                contentDescription = null,
                tint = TeperaPalette.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = TeperaPalette.buttonBrandDark,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

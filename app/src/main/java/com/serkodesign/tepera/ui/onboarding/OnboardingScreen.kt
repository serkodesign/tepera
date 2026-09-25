package com.serkodesign.tepera.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.drawBlurredBlob
import com.serkodesign.tepera.util.findActivity
import kotlinx.coroutines.launch

/**
 * FR-7.1: онбординг-екран дозволів, показується один раз (і за "Дізнатись більше" з Home).
 *
 * **Перемальовано за Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR), секція "Permissions" (node
 * 208:1260; кадри 50:1592/50:1628/57:1749 — жодного/один/обидва дозволи), значення з MCP:** темний
 * екран (#12171F) з абстрактними хвилями й трьома розмитими колами, логотип "Tepera" 64sp
 * (#DCF6ED; у макеті Fascinate Inline — за рішенням користувача замінено на Indie Flower),
 * заголовок 27sp + текст 16sp білим по центру, картка з двома рядками-дозволами (білий 10%,
 * радіус 16, padding 12, gap 8, плитка 40dp: без дозволу — білий 10%, з дозволом — #DCF6ED з
 * галкою #003926) і кнопка "Продовжити" (TeperaButton Primary Medium; напівпрозора, поки
 * дозволені не обидва). За рішенням користувача під кнопкою лишено тихий текстовий
 * "Пропустити" — застосунок має працювати й без доступу до статистики (fallback).
 */
@Composable
fun OnboardingScreen(
    settingsStore: SettingsStore,
    balanceRepository: BalanceRepository,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // "Показано" фіксується одразу при відкритті екрана — незалежно від того, чи користувач
    // надасть дозвіл, натисне "Пропустити" чи піде назад системним back.
    LaunchedEffect(Unit) {
        settingsStore.setOnboardingUsageAccessSeen()
    }

    // Темний екран — іконки статус-/навбару світлі; після виходу повертаємо як було (решта
    // застосунку світла, іконки темні).
    DisposableEffect(view) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (previousStatus != null) controller.isAppearanceLightStatusBars = previousStatus
            if (previousNav != null) controller.isAppearanceLightNavigationBars = previousNav
        }
    }

    var usageGranted by remember { mutableStateOf(false) }

    // Дозволи змінюються поза застосунком (системні налаштування) — перечитуємо при поверненні.
    LifecycleResumeEffect(Unit) {
        scope.launch { usageGranted = balanceRepository.hasUsageAccess() }
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Figma 208:1230: логотип — 64sp, letter-spacing 0.64, #DCF6ED, верх на 179 від краю кадру.
        Text(
            text = "Tepera",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = (179.dp - WindowInsets.statusBars.asPaddingValues().calculateTopPadding()).coerceAtLeast(0.dp)),
            color = TeperaPalette.surfaceBrandLight,
            fontFamily = FontFamily(Font(R.font.indie_flower)),
            fontSize = 64.sp,
            lineHeight = 70.4.sp,
            letterSpacing = 0.64.sp,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Figma 50:1626: padding 8, gap 16, по центру, білий текст.
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.perm_title),
                    color = Color.White,
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 27.sp,
                    lineHeight = 29.7.sp,
                    letterSpacing = 0.027.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.perm_body),
                    color = Color.White,
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 20.8.sp,
                    letterSpacing = 0.016.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(31.dp))

            // Figma 50:1620: білий 10%, радіус 16, padding 12, gap 8.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionRow(
                    label = stringResource(R.string.perm_usage_label),
                    granted = usageGranted,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
            Spacer(Modifier.height(23.dp))

            TeperaButton(
                text = stringResource(R.string.perm_continue),
                onClick = onDone,
                enabled = usageGranted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            TeperaButton(
                text = stringResource(R.string.onboarding_skip),
                onClick = onDone,
                type = TeperaButtonType.Tertiary,
                contentColorOverride = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/** Рядок дозволу (Figma 57:1742): плитка 40dp (#DCF6ED з галкою #003926 / білий 10% з білою), підпис 16sp білим. */
@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (granted) TeperaPalette.surfaceBrandLight else Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(if (granted) R.drawable.ic_perm_check_on else R.drawable.ic_perm_check_off),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 17.6.sp,
            letterSpacing = 0.016.sp
        )
    }
}

/**
 * Фон екрана дозволів (Figma 50:1592): #12171F, хвилі "Group 3" (x -219, y 122.83, 730.641 x
 * 802.166) і три розмиті кола — 554px FDFFD2@10% (центр 78,84, σ 97.55), 604px BFC1EB@20% (центр
 * -58,830, σ 97.55), 604px 65FF93@30% (центр 439,-39, σ 282.7). Гауссове розмиття наближено
 * радіальним градієнтом (`drawBlurredBlob`), як і фон решти застосунку.
 */
@Composable
fun PermissionsBackground() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF12171F))) {
        Image(
            painter = painterResource(R.drawable.perm_bg_waves),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-219).dp, y = 122.83.dp)
                .requiredSize(730.641.dp, 802.166.dp)
        )
        Box(
            modifier = Modifier.fillMaxSize().drawBehind {
                val d = density
                drawBlurredBlob(Offset(78f * d, 84f * d), 277f * d, 97.55f * d, Color(0xFFFDFFD2).copy(alpha = 0.1f))
                drawBlurredBlob(Offset(-58f * d, 830f * d), 302f * d, 97.55f * d, Color(0xFFBFC1EB).copy(alpha = 0.2f))
                drawBlurredBlob(Offset(439f * d, -39f * d), 302f * d, 282.7f * d, Color(0xFF65FF93).copy(alpha = 0.3f))
            }
        )
    }
}

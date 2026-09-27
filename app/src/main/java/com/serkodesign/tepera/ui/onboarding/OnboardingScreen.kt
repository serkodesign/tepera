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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaOnboardingTitle
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlinx.coroutines.launch

/**
 * FR-7.1: онбординг-екран дозволів, показується один раз (і за "Дізнатись більше" з Home).
 *
 * **Перестилізовано за Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR), секція "Permissions" (node
 * 292:1587; кадри 292:1726/292:1771/292:1816 — жодного/один/обидва дозволи), значення з MCP —
 * СКАСОВУЄ попередній темний (#12171F) варіант з хвилями/розмитими колами й лого "Tepera" шрифтом
 * Indie Flower (`PermissionsBackground()` і статус-бар/навбар DisposableEffect видалені разом з
 * ним — не лишились непідключеним кодом, бо викликались лише звідси й з `TeperaNavHost`):** тепер
 * той самий світлий градієнтний фон, що на решті екранів
 * застосунку (`teperaGradientBackground()`, задається зовні в `TeperaNavHost` — тут нічого малювати
 * не треба), лого — не текст, а сама іконка застосунку (`ic_launcher_background`+
 * `ic_launcher_foreground`, 144dp, кут заокруглення 41.143/144 ≈ 28.6% — точний Figma-візерунок
 * "T"-монограми, а не новий SVG-ассет: іконка застосунку вже 1:1 той самий дизайн, MCP це
 * підтвердив звіркою кольорів/шляхів). Заголовок — спільний `TeperaOnboardingTitle` (як в інших
 * кроків онбордингу), текст картки-чекліста — `TeperaPalette.buttonBrandDark`, картка —
 * `cardTranslucentLight` (rgba(255,255,255,0.3), точний токен Figma). Кнопка "Продовжити" (`TeperaButton`
 * Primary Medium) уже 1:1 збігалась зі стилем макета (білий фон, текст #006944, alpha 0.5 disabled) —
 * не чіпали. **За прямою відповіддю користувача при уточненні обсягу зміни:** другий рядок
 * чекліста ("Notifications") з макета НЕ додано — застосунок і далі має лише "Usage permissions"
 * тут, дозвіл на сповіщення просить окремо, пізніше, з Home; тиху кнопку "Пропустити" під
 * "Продовжити" (якої в наданих 3 кадрах Figma нема) лишили — застосунок має працювати й без
 * дозволу (fallback), лише перефарбували під новий світлий фон (Tertiary-колір за замовчуванням,
 * без `contentColorOverride`, що раніше був потрібен на темному тлі).
 */
@Composable
fun OnboardingScreen(
    settingsStore: SettingsStore,
    balanceRepository: BalanceRepository,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // "Показано" фіксується одразу при відкритті екрана — незалежно від того, чи користувач
    // надасть дозвіл, натисне "Пропустити" чи піде назад системним back.
    LaunchedEffect(Unit) {
        settingsStore.setOnboardingUsageAccessSeen()
    }

    var usageGranted by remember { mutableStateOf(false) }

    // Дозволи змінюються поза застосунком (системні налаштування) — перечитуємо при поверненні.
    LifecycleResumeEffect(Unit) {
        scope.launch { usageGranted = balanceRepository.hasUsageAccess() }
        onPauseOrDispose { }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(RoundedCornerShape(41.143.dp))
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Figma 292:1767: padding 8, gap 16, по центру.
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TeperaOnboardingTitle(text = stringResource(R.string.perm_title))
                Text(
                    text = stringResource(R.string.perm_body),
                    color = TeperaPalette.buttonBrandDark.copy(alpha = 0.75f),
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 20.8.sp,
                    letterSpacing = 0.016.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(31.dp))

            // Figma 292:1756: rgba(255,255,255,0.3) — TeperaPalette.cardTranslucentLight, радіус 16, padding 12, gap 8.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TeperaPalette.cardTranslucentLight)
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
                type = TeperaButtonType.Tertiary
            )
        }
    }
}

/** Рядок дозволу (Figma 292:1757, "tile_small"): плитка 40dp (#DCF6ED з галкою #003926 / rgba(255,255,255,0.1) з ледь помітною білою), підпис 16sp. */
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
            Icon(
                painter = painterResource(if (granted) R.drawable.ic_perm_check_on else R.drawable.ic_perm_check_off),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = TeperaPalette.buttonBrandDark,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 17.6.sp,
            letterSpacing = 0.016.sp
        )
    }
}

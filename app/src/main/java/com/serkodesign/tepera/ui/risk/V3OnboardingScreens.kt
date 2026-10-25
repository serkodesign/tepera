package com.serkodesign.tepera.ui.risk

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.ui.onboarding.AppIconTile
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols

/**
 * v3.0 MVP, Етап 3, L1 «Вітання» (Figma "App concept" 911:162): іконка, заголовок 34sp, пояснення, одна кнопка.
 */
@Composable
fun V3WelcomeScreen(onStart: () -> Unit) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppIconTile()
                Text(
                    text = stringResource(R.string.v3_welcome_title),
                    color = TeperaPalette.buttonBrandDark,
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.68).sp,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.v3_welcome_body),
                    color = TeperaPalette.textSecondary, fontSize = 16.sp, lineHeight = 21.sp
                )
            }
            TeperaButton(
                text = stringResource(R.string.v3_welcome_start),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }
    }
}

/**
 * L2 «Доступ до статистики» (911:290): три блоки «що бачить / де живе / що не потрібно», «Надати доступ» відкриває
 * системні налаштування, «Що саме збирається» — діалог із поясненням. Щойно доступ з'явився (повернення в застосунок),
 * екран сам іде далі.
 */
@Composable
fun V3AccessScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    var details by remember { mutableStateOf(false) }
    var resumes by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose {}
    }
    LaunchedEffect(resumes) {
        if (resumes > 0 && app.balanceRepository.hasUsageAccess()) onGranted()
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.v3_access_title),
                    color = TeperaPalette.textPrimary, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.semantics { heading() }
                )
                InfoBlock(TeperaIcons.Smartphone, R.string.v3_access_sees_title, R.string.v3_access_sees_body)
                InfoBlock(TeperaSymbols.PhonelinkOff, R.string.v3_access_lives_title, R.string.v3_access_lives_body)
                InfoBlock(TeperaSymbols.Check, R.string.v3_access_not_needed_title, R.string.v3_access_not_needed_body)
            }
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TeperaButton(
                    text = stringResource(R.string.v3_access_grant),
                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                )
                TeperaButton(
                    text = stringResource(R.string.v3_access_details),
                    onClick = { details = true },
                    modifier = Modifier.fillMaxWidth(),
                    type = TeperaButtonType.Tertiary
                )
            }
        }
    }

    if (details) {
        TeperaDialog(
            onDismissRequest = { details = false },
            confirmText = stringResource(R.string.v3_dialog_close),
            onConfirm = { details = false },
            title = stringResource(R.string.v3_access_details),
            text = stringResource(R.string.v3_access_details_text)
        )
    }
}

/** Блок інформації L2: іконка у світлому колі 36dp + заголовок 14sp Medium + пояснення 12sp (макет 911:380). */
@Composable
private fun InfoBlock(icon: ImageVector, titleRes: Int, bodyRes: Int) {
    V3Card(radius = 20.dp, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(TeperaPalette.surfaceBrandLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TeperaPalette.brandAccent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(titleRes), color = TeperaPalette.textPrimary, fontSize = 14.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
                Text(stringResource(bodyRes), color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

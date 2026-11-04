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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.ui.onboarding.AppIconTile
import com.serkodesign.tepera.ui.theme.M3Button
import com.serkodesign.tepera.ui.theme.M3ButtonSize
import com.serkodesign.tepera.ui.theme.M3ButtonType
import com.serkodesign.tepera.ui.theme.M3Card
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaSpacing
import com.serkodesign.tepera.ui.theme.TeperaSymbols

/**
 * v3.0 MVP, Етап 3, L1 «Вітання» (Figma "App concept" 911:162): іконка, заголовок, пояснення, одна кнопка.
 * Дизайн-система v3.1 (I18a): заголовок `headlineLarge` (Onboarding/Title, таблиця стилів 2.3) кольору `primary`
 * (text/brand-dark → primary, таблиця 4), пояснення `bodyLarge`/`onSurfaceVariant`, кнопка `M3Button(Filled, M)`.
 */
@Composable
fun V3WelcomeScreen(onStart: () -> Unit) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = TeperaSpacing.screenHorizontal)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = TeperaSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(TeperaSpacing.l)
            ) {
                AppIconTile()
                Text(
                    text = stringResource(R.string.v3_welcome_title),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.v3_welcome_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            M3Button(
                type = M3ButtonType.Filled,
                size = M3ButtonSize.M,
                text = stringResource(R.string.v3_welcome_start),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(vertical = TeperaSpacing.l)
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
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = TeperaSpacing.screenHorizontal)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = TeperaSpacing.s),
                verticalArrangement = Arrangement.spacedBy(TeperaSpacing.betweenCards)
            ) {
                Text(
                    text = stringResource(R.string.v3_access_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                InfoBlock(TeperaIcons.Smartphone, R.string.v3_access_sees_title, R.string.v3_access_sees_body)
                InfoBlock(TeperaSymbols.PhonelinkOff, R.string.v3_access_lives_title, R.string.v3_access_lives_body)
                InfoBlock(TeperaSymbols.Check, R.string.v3_access_not_needed_title, R.string.v3_access_not_needed_body)
            }
            Column(Modifier.padding(vertical = TeperaSpacing.s), verticalArrangement = Arrangement.spacedBy(TeperaSpacing.betweenButtons)) {
                M3Button(
                    type = M3ButtonType.Filled,
                    size = M3ButtonSize.M,
                    text = stringResource(R.string.v3_access_grant),
                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                )
                M3Button(
                    type = M3ButtonType.Text,
                    size = M3ButtonSize.S,
                    text = stringResource(R.string.v3_access_details),
                    onClick = { details = true },
                    modifier = Modifier.fillMaxWidth()
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

/**
 * Блок інформації L2 (макет 911:380): іконка у колі 36dp (`surfaceContainerHighest`, іконка `primary`) + заголовок
 * `titleSmall` + пояснення `bodySmall` у картці `M3Card`.
 */
@Composable
private fun InfoBlock(icon: ImageVector, titleRes: Int, bodyRes: Int) {
    M3Card {
        Row(horizontalArrangement = Arrangement.spacedBy(TeperaSpacing.m), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TeperaSpacing.xxs)) {
                Text(stringResource(titleRes), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(bodyRes), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

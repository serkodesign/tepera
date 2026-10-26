package com.serkodesign.tepera.ui.risk

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.util.findActivity
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.PlanOverlayStore

private data class OverlayContent(val appLabel: String, val ifText: String, val thenText: String, val windowEnd: Int)

/**
 * v3.0 MVP, Етап 8 (beta): план «якщо — то» поверх стрічки, відкритої всередині години ризику. Показує сервіс
 * доступності (`GateAccessibilityService`), ніколи — застосунок сам. Тихо: без таймера очікування, без оцінок; людина
 * сама вирішує — вийти чи продовжити. Обидва вибори лише рахуються для екрана тестувальників.
 */
@Composable
fun PlanOverlayScreen(
    gateRepository: GateRepository,
    packageName: String,
    windowId: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    var content by remember { mutableStateOf<OverlayContent?>(null) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(windowId) {
        val window = app.riskWindowRepository.getWindow(windowId)
        val plan = app.riskWindowRepository.getPlan(windowId)
        content = if (window == null || plan == null) null else {
            OverlayContent(resolveLabel(context.packageManager, packageName), plan.ifText, plan.thenText, window.endMinute)
        }
        // Вікно чи план зникли між показом сервісу й відкриттям екрана — нічого показувати; пускаємо людину в застосунок.
        if (content == null) {
            gateRepository.getLaunchIntent(packageName)?.let { context.startActivity(it) }
            finished = true
        }
    }

    fun leave() {
        PlanOverlayStore.recordLeft(context)
        // План показано поверх вже запущеного застосунку, тому «Вийти» веде на робочий стіл (як у воріт через доступність).
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finished = true
    }

    fun proceed() {
        PlanOverlayStore.recordContinued(context)
        gateRepository.getLaunchIntent(packageName)?.let { context.startActivity(it) }
        finished = true
    }

    LaunchedEffect(finished) {
        if (finished) {
            onDone()
            context.findActivity()?.moveTaskToBack(true)
        }
    }

    BackHandler(enabled = content != null && !finished) { leave() }

    val c = content ?: return
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 20.dp).padding(top = 120.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.v3_overlay_label, c.appLabel),
                fontSize = 14.sp, lineHeight = 18.sp, color = TeperaPalette.textSecondary, textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.v3_overlay_title, formatClock(c.windowEnd)),
                fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, color = TeperaPalette.buttonBrandDark,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.v3_plan_format, c.ifText, c.thenText),
                fontSize = 20.sp, lineHeight = 28.sp, color = TeperaPalette.buttonBrandDark,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 28.dp)
            )
        }
        Spacer(Modifier.height(40.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TeperaButton(
                text = stringResource(R.string.gate_pause_exit_action),
                onClick = ::leave,
                size = TeperaButtonSize.Big,
                type = TeperaButtonType.Primary,
                modifier = Modifier.weight(1f)
            )
            TeperaButton(
                text = stringResource(R.string.gate_pause_open_format, c.appLabel),
                onClick = ::proceed,
                size = TeperaButtonSize.Big,
                type = TeperaButtonType.Secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun resolveLabel(pm: PackageManager, packageName: String): String = try {
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (e: PackageManager.NameNotFoundException) {
    packageName
}

private fun formatClock(minute: Int) = String.format(java.util.Locale.ROOT, "%02d:%02d", minute / 60 % 24, minute % 60)

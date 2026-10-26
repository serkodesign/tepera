package com.serkodesign.tepera.ui.risk

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.NudgeAction
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.ui.theme.RedesignDarkColors
import com.serkodesign.tepera.ui.theme.RedesignScope
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.util.findActivity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class OverlayContent(
    val appLabel: String,
    val ifText: String,
    val thenText: String,
    val startMinute: Int,
    val endMinute: Int,
    val writtenOn: String,
    val okYesterday: Boolean
)

/**
 * v3.0 MVP, Етап 8 (beta), макет `v3/PlanSheet`: план «якщо — то» поверх стрічки, відкритої всередині години ризику.
 * Показує сервіс доступності (`GateAccessibilityService`), ніколи — застосунок сам. Тихо: без таймера очікування, без
 * оцінок; людина сама вирішує. «Роблю за планом» — на робочий стіл, «Відкрити на 5 хв» — стрічка відкривається, а за
 * 5 хв починається м'який вихід (`SoftExitController`), «Не сьогодні» — стрічка відкривається і до кінця вечора
 * план більше не показується. Усі вибори лише рахуються для екрана тестувальників.
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
            val zone = ZoneId.systemDefault()
            val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val yesterdayStart = LocalDate.now(zone).minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            OverlayContent(
                appLabel = resolveLabel(context.packageManager, packageName),
                ifText = plan.ifText,
                thenText = plan.thenText,
                startMinute = window.startMinute,
                endMinute = window.endMinute,
                writtenOn = DateTimeFormatter.ofPattern("d MMMM", context.resources.configuration.locales[0])
                    .format(Instant.ofEpochMilli(plan.updatedAt).atZone(zone)),
                okYesterday = app.database.nudgeEventDao().countByActionForWindow(windowId, NudgeAction.OK, yesterdayStart, todayStart) > 0
            )
        }
        // Вікно чи план зникли між показом сервісу й відкриттям екрана — нічого показувати; пускаємо людину в застосунок.
        if (content == null) {
            gateRepository.getLaunchIntent(packageName)?.let { context.startActivity(it) }
            finished = true
        }
    }

    fun leave() {
        PlanOverlayStore.recordLeft(context)
        // План показано поверх вже запущеного застосунку, тому «Роблю за планом» веде на робочий стіл (як у воріт через доступність).
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finished = true
    }

    fun openForFiveMinutes() {
        val c = content ?: return
        PlanOverlayStore.recordContinued(context)
        PlanOverlayStore.setPendingSoftExit(
            context, PlanOverlayStore.PendingSoftExit(packageName, c.appLabel, c.thenText, System.currentTimeMillis())
        )
        gateRepository.getLaunchIntent(packageName)?.let { context.startActivity(it) }
        finished = true
    }

    fun notToday() {
        PlanOverlayStore.suppressToday(context)
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
    // Лист завжди темний, як у макеті (фон #1b1b1d), незалежно від теми застосунку.
    RedesignScope(RedesignDarkColors) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFF1B1B1D))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(Modifier.size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.14f)))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFA65D7C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(c.appLabel.take(1).uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text(c.appLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(R.string.v3_overlay_sheet_sub, formatClock(c.startMinute) + "–" + formatClock(c.endMinute)),
                            color = Color(0xFFC5DCD9), fontSize = 12.sp, lineHeight = 16.sp
                        )
                    }
                }
                Text(
                    stringResource(R.string.v3_overlay_sheet_title), color = Color.White, fontSize = 27.sp, lineHeight = 30.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.v3_plan_format, c.ifText, c.thenText),
                    color = Color(0xFFFEFFEF), fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A3D38))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
                // Лише те, що відомо точно: коли складено план і чи людина вчора сама відмітила «Добре». «Спрацювало» не міряємо.
                Text(
                    text = stringResource(R.string.v3_overlay_written, c.writtenOn) +
                        if (c.okYesterday) " " + stringResource(R.string.v3_overlay_ok_yesterday) else "",
                    color = Color(0xFFC5DCD9), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    TeperaButton(
                        text = stringResource(R.string.v3_overlay_take_plan),
                        onClick = ::leave,
                        size = TeperaButtonSize.Medium,
                        type = TeperaButtonType.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TeperaButton(
                            text = stringResource(R.string.v3_overlay_open_five),
                            onClick = ::openForFiveMinutes,
                            size = TeperaButtonSize.Medium,
                            type = TeperaButtonType.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        TeperaButton(
                            text = stringResource(R.string.v3_nudge_action_not_today),
                            onClick = ::notToday,
                            size = TeperaButtonSize.Medium,
                            type = TeperaButtonType.Tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun resolveLabel(pm: PackageManager, packageName: String): String = try {
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (e: PackageManager.NameNotFoundException) {
    packageName
}

private fun formatClock(minute: Int) = String.format(java.util.Locale.ROOT, "%02d:%02d", minute / 60 % 24, minute % 60)

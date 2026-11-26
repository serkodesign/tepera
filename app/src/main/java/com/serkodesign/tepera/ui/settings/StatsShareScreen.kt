package com.serkodesign.tepera.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.BuildConfig
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.analytics.AnalyticsEvent
import com.serkodesign.tepera.analytics.StatsShareReport
import com.serkodesign.tepera.analytics.track
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaHint
import java.time.LocalDate

/** Куди йде лист зі статистикою (людина сама обирає поштовий застосунок і сама натискає «Надіслати»). */
private const val STATS_EMAIL = "serkodesign@gmail.com"

/**
 * Рівень 1 аналітики: людина бачить точний текст і за бажання надсилає його листом. Нічого не йде в мережу з самого
 * застосунку; ID немає. Тексту звіту не торкається нічого, крім [StatsShareReport.build].
 */
@Composable
fun StatsShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    var report by remember { mutableStateOf<String?>(null) }
    var hasData by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        context.track(AnalyticsEvent.StatsShareOpened)
        val today = LocalDate.now()
        val rows = app.database.dailyEventCounterDao()
            .getRange(today.minusDays(StatsShareReport.PERIOD_DAYS - 1), today)
        hasData = StatsShareReport.hasData(rows, today)
        report = StatsShareReport.build(rows, today, BuildConfig.VERSION_NAME, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
    }

    val failToast = stringResource(R.string.stats_share_no_mail_app)
    val subject = stringResource(R.string.stats_share_mail_subject)
    val chooserTitle = stringResource(R.string.stats_share_chooser_title)

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.stats_share_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.stats_share_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TeperaCard(title = stringResource(R.string.stats_share_what_title)) {
                    Text(
                        stringResource(R.string.stats_share_what_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TeperaCard(title = stringResource(R.string.stats_share_preview_title)) {
                    Text(
                        report ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TeperaHint(text = stringResource(R.string.stats_share_hint))
                TeperaButton(
                    text = stringResource(R.string.stats_share_send),
                    enabled = hasData && report != null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val text = report ?: return@TeperaButton
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(STATS_EMAIL))
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, chooserTitle))
                            context.track(AnalyticsEvent.StatsShared)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, failToast, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

package com.serkodesign.tepera.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.NavChevron
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.TeperaPalette

private const val PRIVACY_POLICY_URL = "https://serkodesign.github.io/tepera/privacy-policy"
private const val CONTACT_EMAIL = "serkodesign@gmail.com"

/**
 * GAP-8 (SRS v3.1): "Про застосунок" — версія, посилання на Privacy Policy й контакт. Посилання на політику в самому
 * застосунку — вимога Google Play. Обидва рядки лише передають намір системі (браузер / поштовий клієнт) і не
 * потребують дозволу INTERNET; відсутність відповідного застосунку ловиться й показується спокійним повідомленням.
 */
@Composable
fun AboutScreen(
    onOpenKnowledgeBase: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.about_no_app), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.about_screen_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TeperaCard(
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.about_version_format, versionName)
                ) {
                    Text(
                        text = stringResource(R.string.about_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
                    )
                }
                GlassRow(
                    label = stringResource(R.string.about_privacy_policy),
                    onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) },
                    leading = { TeperaIconCircle(Icons.Filled.PrivacyTip) },
                    trailing = { NavChevron() }
                )
                GlassRow(
                    label = stringResource(R.string.settings_knowledge_base_action),
                    onClick = onOpenKnowledgeBase,
                    leading = { TeperaIconCircle(Icons.AutoMirrored.Filled.MenuBook) },
                    trailing = { NavChevron() }
                )
                GlassRow(
                    label = stringResource(R.string.about_contact),
                    onClick = {
                        open(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CONTACT_EMAIL"))
                                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                        )
                    },
                    leading = { TeperaIconCircle(Icons.Filled.Email) },
                    trailing = { NavChevron() }
                )
            }
        }
    }
}

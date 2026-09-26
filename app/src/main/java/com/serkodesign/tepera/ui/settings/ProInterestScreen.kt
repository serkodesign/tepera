package com.serkodesign.tepera.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.ProInterestForm

/**
 * CC-11: «Tepera Pro — у розробці» (fake door). Без цін, оплат, обіцянок дат і без жодної залежності від
 * платіжного шару. Єдина дія — «Хочу дізнатись»: чесне попередження про браузер, далі Google Form
 * через `ACTION_VIEW` (як «Запропонувати функцію»).
 */
@Composable
fun ProInterestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        TeperaDialog(
            onDismissRequest = { showConfirm = false },
            title = stringResource(R.string.settings_suggest_feature_confirm_title),
            text = stringResource(R.string.settings_suggest_feature_confirm_body),
            confirmText = stringResource(R.string.settings_suggest_feature_confirm_action),
            dismissText = stringResource(R.string.dialog_cancel),
            onConfirm = {
                showConfirm = false
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ProInterestForm.urlFor(context))))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, context.getString(R.string.settings_suggest_feature_no_browser), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.pro_interest_screen_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeperaCard(title = stringResource(R.string.pro_interest_title)) {
                    Text(
                        text = stringResource(R.string.pro_interest_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
                    )
                }
                TeperaButton(
                    text = stringResource(R.string.pro_interest_action),
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

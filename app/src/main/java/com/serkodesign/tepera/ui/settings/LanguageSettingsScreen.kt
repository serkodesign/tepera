package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.M3ListDivider
import com.serkodesign.tepera.ui.theme.M3ListGroup
import com.serkodesign.tepera.ui.theme.M3ListItem
import com.serkodesign.tepera.util.AppLanguages
import com.serkodesign.tepera.util.AppLocale

/** Окрема сторінка вибору мови: «Системна» і мови з [AppLanguages] (рідними назвами); застосовується на місці. */
@Composable
fun LanguageSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val current = AppLocale.tag(context)
    val options = listOf("" to stringResource(R.string.settings_language_system)) + AppLanguages.all.map { it.tag to it.endonym }
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.settings_language_label), onBack = onBack)
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp)
            ) {
                M3ListGroup {
                    options.forEachIndexed { i, (tag, label) ->
                        if (i > 0) M3ListDivider()
                        M3ListItem(
                            title = label,
                            modifier = Modifier.selectable(selected = current == tag, role = Role.RadioButton, onClick = { AppLocale.set(context, tag) }),
                            trailing = { RadioButton(selected = current == tag, onClick = null) }
                        )
                    }
                }
            }
        }
    }
}

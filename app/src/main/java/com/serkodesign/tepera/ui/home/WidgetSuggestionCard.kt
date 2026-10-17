package com.serkodesign.tepera.ui.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.widget.TeperaWidgetReceiver

/**
 * Пропозиція віджета — колишній останній крок онбордингу ("Пропозиція віджета", Figma user-flow), тепер звичайна
 * контекстна картка Home: показується один раз після завершення онбордингу й закривається "×" або дією.
 *
 * `requestPinAppWidget()` викликається прямо з кліку на потоці композиції (Main) — той самий принцип, що
 * `GateRepository.createGate()`: лаунчер перевіряє, що застосунок щойно на передньому плані від дії користувача,
 * перш ніж показати системний діалог розміщення. [onAdded] викликається і коли діалог не підтримано, щоб картка
 * не висіла вічно.
 */
@Composable
fun WidgetSuggestionCard(onAdded: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .contextInfoSurface()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ContextCardHeader(title = stringResource(R.string.widget_suggestion_title), onDismiss = onDismiss)
        Text(
            text = stringResource(R.string.widget_suggestion_body),
            style = ContextCardBodyStyle,
            color = TeperaPalette.buttonBrandDark
        )
        TeperaButton(
            text = stringResource(R.string.widget_suggestion_add_action),
            onClick = {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(ComponentName(context, TeperaWidgetReceiver::class.java), null, null)
                }
                onAdded()
            },
            modifier = Modifier.fillMaxWidth(),
            type = TeperaButtonType.Secondary
        )
    }
}

package com.serkodesign.tepera.ui.onboarding

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.widget.TeperaWidgetReceiver

/**
 * "Пропозиція віджета" — останній крок онбордингу (Figma user-flow k6s4prQ9oK9x2uUvzHRghR,
 * node 14:791: ОБИДВІ гілки "доступ надано? так/ні" сходяться сюди, перед Home). HomeScreen
 * вирішує, коли показати цей екран (після онбордингу цінностей/категорій/оцінки Online-часу і
 * після того, як крок дозволу вже розв'язаний — незалежно від того, чи доступ реально надано).
 *
 * `requestPinAppWidget()` — той самий принцип, що `GateRepository.createGate()` для ярликів
 * воріт: викликається одразу на диспетчері виклику (тут — Main, композиційний потік), без
 * перемикання на IO, бо лаунчер перевіряє, що застосунок щойно на передньому плані від дії
 * користувача, перш ніж показати системний діалог розміщення.
 */
@Composable
fun WidgetSuggestionScreen(
    settingsStore: SettingsStore,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    // "Показано" фіксується одразу при відкритті — незалежно від того, чи користувач натисне
    // "Додати віджет", чи "Пропустити" (той самий принцип, що OnboardingScreen).
    LaunchedEffect(Unit) {
        settingsStore.setWidgetSuggestionSeen()
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Widgets,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = TeperaPalette.brandAccent
            )
            Spacer(Modifier.padding(top = 16.dp))
            Text(
                text = stringResource(R.string.widget_suggestion_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.padding(top = 12.dp))
            Text(
                text = stringResource(R.string.widget_suggestion_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.padding(top = 32.dp))
            Button(
                onClick = {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val provider = ComponentName(context, TeperaWidgetReceiver::class.java)
                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                        appWidgetManager.requestPinAppWidget(provider, null, null)
                    }
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.widget_suggestion_add_action))
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
    }
}

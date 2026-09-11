package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FR-D.7 (SRS v2.8): "Востаннє брав телефон о HH:MM" — вся картка складається з ОДНОГО речення
 * (FR-D.11: компактність) у заголовку `ContextCardHeader`, без окремого тіла картки — без
 * висновків і коментарів (FR-D.7b), просто час.
 *
 * `lastKnownMillis` (замість прямого `state.lastUseMillis`) тримає останнє непорожнє значення
 * через `remember`+`LaunchedEffect`: при закритті ViewModel одразу скидає стан у дефолтний
 * (`lastUseMillis = null`), а `AnimatedVisibility` в `ContextCardStack` тримає цю картку в
 * композиції ще на час анімації згортання — без цього картка встигла б показати порожній текст
 * за мить до того, як зникнути.
 */
@Composable
fun LastPhoneUseCard(state: LastPhoneUseUiState, onDismiss: () -> Unit) {
    var lastKnownMillis by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.lastUseMillis) {
        state.lastUseMillis?.let { lastKnownMillis = it }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp)
    ) {
        ContextCardHeader(
            title = stringResource(R.string.last_phone_use_format, lastKnownMillis?.let(::formatTime) ?: ""),
            onDismiss = onDismiss
        )
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

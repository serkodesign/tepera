package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassScreenHeader

/** Підменю «Застосунки-стрічки» → що відбувається при відкритті стрічки: ідея, спеціальні можливості, задачки, приглушення. */
@Composable
fun FeedOpeningScreen(onBack: () -> Unit) = FeedSubScreen(stringResource(R.string.settings_feed_opening_menu), onBack) {
    FeedOpeningSettings()
}

/** Підменю «Застосунки-стрічки» → Берсерк-режим (лише збірка зі службою доступності). */
@Composable
fun BerserkScreen(onBack: () -> Unit) = FeedSubScreen(stringResource(R.string.berserk_title), onBack) {
    BerserkSettings()
}

@Composable
private fun FeedSubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = title, onBack = onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) { content() }
        }
    }
}

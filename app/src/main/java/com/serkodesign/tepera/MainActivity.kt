package com.serkodesign.tepera

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.serkodesign.tepera.ui.navigation.TeperaNavHost
import com.serkodesign.tepera.ui.theme.TeperaTheme

class MainActivity : ComponentActivity() {

    companion object {
        // FR-4.1: тап по кнопці категорії на віджеті. FR-4.4: тап по Quick Settings tile.
        const val EXTRA_OPEN_ADD_ENTRY = "open_add_entry"
        const val EXTRA_CATEGORY_ID = "category_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Фаза 6: градієнтний фон (TeperaPalette) має йти під статус-баром і навігаційною смугою
        // до самого краю екрана, як у Figma-фреймі — без edge-to-edge system bar area лишається
        // окремою суцільною смугою поверх контенту. Темні іконки статус-бару завжди (не
        // SystemBarStyle.auto): TeperaTheme свідомо ігнорує системну темну тему, тож іконки мають
        // лишатись темними незалежно від системних налаштувань, інакше вони зіллються зі світлим
        // градієнтом.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val app = application as TeperaApp
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
        val openAddEntry = intent.getBooleanExtra(EXTRA_OPEN_ADD_ENTRY, false) || categoryId != null
        setContent {
            TeperaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TeperaNavHost(
                        categoryRepository = app.categoryRepository,
                        activityRepository = app.activityRepository,
                        balanceRepository = app.balanceRepository,
                        excludedAppRepository = app.excludedAppRepository,
                        installedAppsProvider = app.installedAppsProvider,
                        settingsStore = app.settingsStore,
                        activeTimerStore = app.activeTimerStore,
                        backupRepository = app.backupRepository,
                        pendingOpenAddEntry = openAddEntry,
                        pendingCategoryId = categoryId
                    )
                }
            }
        }
    }
}

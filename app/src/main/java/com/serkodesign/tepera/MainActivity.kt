package com.serkodesign.tepera

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.ui.navigation.TeperaNavHost
import com.serkodesign.tepera.ui.theme.TeperaTheme
import com.serkodesign.tepera.util.LocaleStore
import com.serkodesign.tepera.util.ProvideAppLocale

class MainActivity : ComponentActivity() {

    companion object {
        // FR-4.1: тап по кнопці категорії на віджеті. FR-4.4: тап по Quick Settings tile.
        const val EXTRA_OPEN_ADD_ENTRY = "open_add_entry"
        const val EXTRA_CATEGORY_ID = "category_id"
    }

        // CC-8: тап по тижневому сповіщенню — відкрити Home з карткою підсумку тижня.
        const val EXTRA_OPEN_WEEKLY_SUMMARY = "open_weekly_summary"
    /**
     * T-5 (tepera-dev-spec.md): тап по закріпленому ярлику воріт (T-4) запускає ту саму
     * MainActivity, що вже, як правило, живе у фоні — без `onNewIntent()` система лише виносить
     * наявний інстанс наперед ("Warning: Activity not started, its current task has been brought
     * to the front", підтверджено на S23), а нові extras з intent НЕ доходять до вже
     * скомпонованого Compose-дерева. `nonce` — не сам packageName — бо той самий застосунок
     * можна відкрити воротами кілька разів поспіль; без унікального ключа повторний тап з тим
     * самим packageName не перезапустив би LaunchedEffect у TeperaNavHost.
     */
    data class GateRequest(val packageName: String, val nonce: Long)

    private var pendingGateRequest by mutableStateOf<GateRequest?>(null)

    // CC-8: nonce тапу по тижневому сповіщенню — TeperaNavHost повертає на Home.
    private var weeklySummaryNonce by mutableStateOf<Long?>(null)

    // CC-4: відкриття через ворота (це не "відкрив Tepera") і перестворення
    // активності (зміна мови) не рахуються.
    private var skipNextOpen = false

        super.attachBaseContext(LocaleStore.wrap(newBase))
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
        pendingGateRequest = gateRequestFromIntent(intent)
        setContent {
            ProvideAppLocale {
            TeperaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TeperaNavHost(
                        categoryRepository = app.categoryRepository,
        skipNextOpen = savedInstanceState != null || pendingGateRequest != null
        handleWeeklySummaryIntent(intent)
                        activityRepository = app.activityRepository,
                        balanceRepository = app.balanceRepository,
                        excludedAppRepository = app.excludedAppRepository,
                        installedAppsProvider = app.installedAppsProvider,
                        pauseRepository = app.pauseRepository,
                        patternRepository = app.patternRepository,
                        settingsStore = app.settingsStore,
                        sleepWindowRepository = app.sleepWindowRepository,
                        userEstimateRepository = app.userEstimateRepository,
                        unlockRepository = app.unlockRepository,
                        activeTimerStore = app.activeTimerStore,
                        backupRepository = app.backupRepository,
                        gateRepository = app.gateRepository,
                        gateEventRepository = app.gateEventRepository,
                        cardHistoryRepository = app.cardHistoryRepository,
                        pendingOpenAddEntry = openAddEntry,
                        pendingCategoryId = categoryId,
                        pendingGateTargetPackage = pendingGateRequest?.packageName,
                        pendingGateRequestNonce = pendingGateRequest?.nonce,
                        pendingWeeklySummaryNonce = weeklySummaryNonce
                    )
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingGateRequest = gateRequestFromIntent(intent)
    }

    private fun gateRequestFromIntent(intent: Intent): GateRequest? =
        intent.getStringExtra(GateRepository.GATE_TARGET_PACKAGE_EXTRA)
    override fun onStart() {
        super.onStart()
        if (skipNextOpen) {
            skipNextOpen = false
        } else {
            val welcomeBack = (application as TeperaApp).welcomeBackRepository
            lifecycleScope.launch {
                welcomeBack.onAppOpened() // CC-4: перерва ≥ 3 діб → підсумок на Home
            }
        }
    }

            ?.let { GateRequest(it, System.nanoTime()) }
}
        skipNextOpen = pendingGateRequest != null
        handleWeeklySummaryIntent(intent)
    }

    /** CC-8: тап по тижневому сповіщенню — знімаємо приховування картки «Цей тиждень» і повертаємо на Home. */
    private fun handleWeeklySummaryIntent(intent: Intent) {
        if (!intent.getBooleanExtra(EXTRA_OPEN_WEEKLY_SUMMARY, false)) return
        intent.removeExtra(EXTRA_OPEN_WEEKLY_SUMMARY)
        weeklySummaryNonce = System.nanoTime()
        lifecycleScope.launch { (application as TeperaApp).settingsStore.setWeeklyDigestCardDismissedKey(-1L) }
    }


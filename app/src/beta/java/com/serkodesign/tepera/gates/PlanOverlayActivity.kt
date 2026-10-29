package com.serkodesign.tepera.gates

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.ui.risk.PlanOverlayScreen
import com.serkodesign.tepera.ui.theme.TeperaTheme
import com.serkodesign.tepera.util.LocaleStore
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.util.ProvideAppLocale

/**
 * v3.0 MVP, Етап 8 (beta): лист плану поверх стрічки. Окрема прозора activity в власній задачі (`taskAffinity=""`), а не екран
 * `MainActivity`: стрічка лишається видимою під затемненням, а Tepera не виринає на передній план з чорним фоном. Закриття —
 * просто `finish()`: під листом уже стоїть сама стрічка (або робочий стіл, якщо людина обрала вихід).
 */
class PlanOverlayActivity : ComponentActivity() {

    private var windowId: String? = null
    private var decided = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Не в маніфесті: на Android 8.0 прозора activity з screenOrientation падає («Only fullscreen opaque activities can
        // request orientation»); з 8.1 це дозволено.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val packageName = intent.getStringExtra(GateRepository.GATE_TARGET_PACKAGE_EXTRA)
        val windowId = intent.getStringExtra(GateRepository.PLAN_WINDOW_EXTRA)
        if (packageName == null || windowId == null) {
            finish()
            return
        }
        this.windowId = windowId
        val app = application as TeperaApp
        setContent {
            ProvideAppLocale {
                TeperaTheme {
                    PlanOverlayScreen(
                        gateRepository = app.gateRepository,
                        packageName = packageName,
                        windowId = windowId,
                        onDone = { finish() },
                        onDecided = { decided = true }
                    )
                }
            }
        }
    }

    /** Людина пішла з листа (Home, «Недавні», блокування): нічого не лишаємо, наступний показ відкриє свіжий. */
    override fun onStop() {
        super.onStop()
        // Без вибору («назад», Home, блокування) показ не рахується: стрічка, відкрита знову, отримає підказку без очікування дебаунсу.
        if (!decided) windowId?.let { PlanOverlayStore.clearShown(this, it) }
        if (!isFinishing) finish()
    }
}

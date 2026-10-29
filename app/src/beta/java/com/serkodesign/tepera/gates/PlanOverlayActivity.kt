package com.serkodesign.tepera.gates

import android.content.Context
import android.graphics.Color
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
import com.serkodesign.tepera.util.ProvideAppLocale

/**
 * v3.0 MVP, Етап 8 (beta): лист плану поверх стрічки. Окрема прозора activity в власній задачі (`taskAffinity=""`), а не екран
 * `MainActivity`: стрічка лишається видимою під затемненням, а Tepera не виринає на передній план з чорним фоном. Закриття —
 * просто `finish()`: під листом уже стоїть сама стрічка (або робочий стіл, якщо людина обрала вихід).
 */
class PlanOverlayActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val app = application as TeperaApp
        setContent {
            ProvideAppLocale {
                TeperaTheme {
                    PlanOverlayScreen(
                        gateRepository = app.gateRepository,
                        packageName = packageName,
                        windowId = windowId,
                        onDone = { finish() }
                    )
                }
            }
        }
    }

    /** Людина пішла з листа (Home, «Недавні», блокування): нічого не лишаємо, наступний показ відкриє свіжий. */
    override fun onStop() {
        super.onStop()
        if (!isFinishing) finish()
    }
}

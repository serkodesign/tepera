package com.serkodesign.tepera.gates

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.risk.BerserkBlockScreen
import com.serkodesign.tepera.ui.theme.TeperaTheme
import com.serkodesign.tepera.util.BerserkStore
import com.serkodesign.tepera.util.LocaleStore
import com.serkodesign.tepera.util.ProvideAppLocale

/**
 * Берсерк-режим (beta): екран блокування стрічки поверх неї. Окрема activity в власній задачі, як лист плану. «Добре» й
 * «назад» ведуть на робочий стіл (під екраном стоїть сама стрічка, лишати її відкритою не можна); «Важлива справа»
 * вимикає берсерк до завтра й закриває екран.
 */
class BerserkActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val until = intent.getLongExtra(EXTRA_UNTIL, 0L)
        if (until <= System.currentTimeMillis()) {
            finish()
            return
        }
        val jokeIndex = intent.getIntExtra(EXTRA_JOKE, -1)
        val jokes = resources.getStringArray(R.array.berserk_jokes)
        val joke = jokes.getOrNull(jokeIndex)
        val appName = intent.getStringExtra(EXTRA_APP_NAME)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        setContent {
            ProvideAppLocale {
                TeperaTheme {
                    BerserkBlockScreen(
                        joke = joke,
                        blockedName = appName,
                        untilMillis = until,
                        canSkip = !BerserkStore.isSkippedToday(this@BerserkActivity),
                        onOk = { goHome() },
                        onSkipToday = {
                            BerserkStore.skipToday(this@BerserkActivity)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_UNTIL = "berserk_until"
        const val EXTRA_JOKE = "berserk_joke"
        const val EXTRA_APP_NAME = "berserk_app_name"
    }
}

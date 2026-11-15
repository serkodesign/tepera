package com.serkodesign.tepera.gates

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.serkodesign.tepera.R
import com.serkodesign.tepera.domain.berserk.BerserkTracker
import com.serkodesign.tepera.util.BerserkStore

/**
 * Берсерк-режим (beta, живе всередині служби доступності): рахує безперервний час у застосунках-стрічках і, коли набігла
 * година, блокує стрічку на заданий час екраном [BerserkActivity].
 *
 * Стан відліку тримається в пам'яті процесу ([sTracker]): Samsung іноді перестворює екземпляр служби, а втрата відліку
 * при перезапуску процесу — прийнятна. Блокування ([BerserkStore]) переживає перезапуск.
 *
 * @param feeds поточний список застосунків-стрічок.
 * @param foregroundPackage застосунок, що зараз на екрані за системною історією (для відновлення відліку після розблокування).
 */
class BerserkController(
    private val service: AccessibilityService,
    private val feeds: () -> Set<String>,
    private val foregroundPackage: () -> String?
) {
    private val handler = Handler(Looper.getMainLooper())
    private val power = service.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var lastLaunchAt = 0L
    private var checkRunnable: Runnable? = null
    private var checkKey: String? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                // Екран погас — людина не в стрічці, відрізок закривається.
                Intent.ACTION_SCREEN_OFF -> onLeft()
                Intent.ACTION_USER_PRESENT -> foregroundPackage()?.let { onForeground(it) }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(service, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    /**
     * Застосунок [pkg] вийшов на передній план. Повертає `true`, якщо це заблокована стрічка і замість неї показано екран
     * блокування — тоді решту обробки (лист плану тощо) треба пропустити.
     */
    fun onForeground(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        if (!BerserkStore.isEnabled(service) || BerserkStore.isSkippedToday(service, now)) {
            onLeft()
            return false
        }
        if (pkg !in feeds()) {
            onLeft()
            return false
        }
        val mode = BerserkStore.countMode(service)
        if (sMode != mode) {
            // Спосіб підрахунку змінили — старі відліки за іншими ключами не мають сенсу.
            sTracker.clear()
            sMode = mode
        }
        val key = BerserkStore.trackingKey(mode, pkg)
        val blockedUntil = BerserkStore.blockedUntil(service, pkg)
        if (blockedUntil > now) {
            sTracker.leave(now)
            cancelCheck()
            showBlock(pkg, blockedUntil, joke = false)
            return true
        }
        sTracker.enter(key, now)
        scheduleCheck(key, pkg)
        return false
    }

    /** Людина вийшла зі стрічок: робочий стіл, інший застосунок, екран вимкнено. */
    fun onLeft() {
        sTracker.leave(System.currentTimeMillis())
        cancelCheck()
    }

    fun destroy() {
        cancelCheck()
        runCatching { service.unregisterReceiver(screenReceiver) }
    }

    private fun scheduleCheck(key: String, pkg: String) {
        if (checkKey == key && checkRunnable != null) {
            // Той самий ключ (спільний режим, перехід між стрічками): таймер уже йде, лише оновлюємо пакет для екрана.
            sLastFeedPackage = pkg
            return
        }
        cancelCheck()
        sLastFeedPackage = pkg
        val now = System.currentTimeMillis()
        val hourMillis = BerserkStore.hourMinutes(service) * 60_000L
        val left = (hourMillis - sTracker.elapsed(key, now)).coerceAtLeast(0L)
        val r = Runnable { check(key) }
        checkRunnable = r
        checkKey = key
        handler.postDelayed(r, left + 500L)
    }

    private fun cancelCheck() {
        checkRunnable?.let { handler.removeCallbacks(it) }
        checkRunnable = null
        checkKey = null
    }

    private fun check(key: String) {
        checkRunnable = null
        checkKey = null
        val now = System.currentTimeMillis()
        if (!BerserkStore.isEnabled(service) || BerserkStore.isSkippedToday(service, now)) return onLeft()
        if (!power.isInteractive || !sTracker.isActive(key)) return
        val hourMillis = BerserkStore.hourMinutes(service) * 60_000L
        val elapsed = sTracker.elapsed(key, now)
        if (elapsed < hourMillis) {
            // Таймер спрацював раніше (обнулення чи зміна налаштування): переносимо на залишок.
            scheduleCheck(key, sLastFeedPackage.orEmpty())
            return
        }
        val pkg = sLastFeedPackage ?: return
        val until = now + BerserkStore.blockMinutes(service) * 60_000L
        BerserkStore.setBlock(service, pkg, until)
        sTracker.leave(now)
        sTracker.reset(key, now)
        showBlock(pkg, until, joke = true)
    }

    private fun showBlock(pkg: String, until: Long, joke: Boolean) {
        val now = System.currentTimeMillis()
        // Кілька подій відкриття поспіль не мають давати кілька екранів.
        if (now - lastLaunchAt < LAUNCH_DEDUPE_MILLIS) return
        lastLaunchAt = now
        val allMode = BerserkStore.countMode(service) == BerserkStore.CountMode.ALL
        val label = if (allMode) null else runCatching {
            service.packageManager.getApplicationLabel(service.packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
        val jokes = service.resources.getStringArray(R.array.berserk_jokes)
        val intent = Intent(service, BerserkActivity::class.java)
            .putExtra(BerserkActivity.EXTRA_UNTIL, until)
            .putExtra(BerserkActivity.EXTRA_APP_NAME, label)
            .putExtra(BerserkActivity.EXTRA_JOKE, if (joke && jokes.isNotEmpty()) (jokes.indices).random() else -1)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        service.startActivity(intent)
    }

    private companion object {
        val sTracker = BerserkTracker(BerserkStore.BREAK_MILLIS)
        @Volatile var sMode: BerserkStore.CountMode? = null
        @Volatile var sLastFeedPackage: String? = null
        const val LAUNCH_DEDUPE_MILLIS = 2_000L
    }
}

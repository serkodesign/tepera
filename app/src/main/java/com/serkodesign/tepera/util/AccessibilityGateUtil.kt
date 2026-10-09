package com.serkodesign.tepera.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.serkodesign.tepera.service.TeperaGateAccessibilityService

/**
 * Експериментально, НЕ для релізу (CLAUDE.md, "AccessibilityService-ворота" — рішення явно
 * відкладене post-MVP; ця гілка існує лише для бенчу на тестових пристроях).
 *
 * `AccessibilityManager.getEnabledAccessibilityServiceList()` вимагає сам дозвіл, щоб повернути
 * щось корисне на деяких OEM — надійніший спосіб перевірити "чи я сам увімкнений" (той самий
 * підхід, що інші Android-бібліотеки) — пошук власного ComponentName у рядку
 * `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (список "pkg/Service:pkg2/Service2").
 */
object AccessibilityGateUtil {

    fun isServiceEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${TeperaGateAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        for (segment in splitter) {
            if (segment.equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    /** Системний екран (не runtime-діалог) — той самий принцип, що ACTION_USAGE_ACCESS_SETTINGS. */
    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

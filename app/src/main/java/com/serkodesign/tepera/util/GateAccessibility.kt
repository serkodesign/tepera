package com.serkodesign.tepera.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.serkodesign.tepera.BuildConfig

/**
 * Тестова збірка (flavor a11y): ворота через сервіс доступності. Клас сервісу є лише в source set `a11y`, тому тут він
 * згадується тільки іменем компонента — main компілюється в обох flavor-ах.
 */
object GateAccessibility {
    private const val SERVICE_CLASS = "com.serkodesign.tepera.gates.GateAccessibilityService"

    /** true лише у збірці, що містить сервіс. Усе, що пов'язане з доступністю в UI, гейтиться цим. */
    val isAvailable: Boolean get() = BuildConfig.GATE_ACCESSIBILITY

    fun isEnabled(context: Context): Boolean {
        if (!isAvailable) return false
        val component = ComponentName(context.packageName, SERVICE_CLASS).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }

    /** Системний екран «Спеціальні можливості»; користувач сам знаходить там Tepera. */
    fun openSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

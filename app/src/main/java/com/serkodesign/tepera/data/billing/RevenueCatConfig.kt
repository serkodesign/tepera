package com.serkodesign.tepera.data.billing

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.serkodesign.tepera.R

/**
 * RevenueCat — єдина точка налаштування й ідентифікаторів (див. docs/revenuecat-setup.md).
 *
 * Ключ SDK береться з `local.properties` через `resValue` (`revenuecat_api_key`): у debug — Test Store
 * ключ (`test_...`), у release — окремий Google Play ключ (`goog_...`). Test Store ключ у release SDK
 * навмисно відхиляє, тому без власного release-ключа SDK просто лишається вимкненим — екрани Pro й
 * підтримки показують "поки недоступно" замість падіння.
 */
object RevenueCatConfig {

    /** Entitlement, що відкриває Tepera Pro (створюється в дашборді RevenueCat і прикріплюється до продуктів). */
    const val ENTITLEMENT_PRO = "tepera_pro"

    /** Offering з пакетами підписок/lifetime для Paywall — стандартний current-offering. */
    // Paywall без явного offering показує current offering з дашборда.

    /** Окремий offering добровільної підтримки ("Пригостити кавою"); пакети — за їхніми identifier. */
    const val OFFERING_SUPPORT = "support"

    /**
     * Чи показувати вхід у "Tepera Pro" (Налаштування). На фазі запуску Pro не буде — лише "Пригостити
     * кавою" (рішення користувача), тому `false`: екрани Pro/Paywall/Customer Center лишаються в коді, але
     * недосяжні, а Pro-репозиторій не ініціалізується при старті. Увімкнути — змінити на `true`.
     */
    const val PRO_ENTRY_ENABLED = false

    private const val TAG = "RevenueCat"

    /**
     * Викликається один раз із `Application.onCreate()`. Анонімний користувач (без входу — у застосунку
     * нема акаунтів, Pro прив'язується до Google Play акаунта й відновлюється через "Відновити покупки").
     * Повертає `true`, якщо SDK налаштовано.
     */
    fun configure(context: Context): Boolean {
        val apiKey = context.getString(R.string.revenuecat_api_key)
        if (apiKey.isBlank()) {
            Log.i(TAG, "API key is empty — RevenueCat is disabled (Pro and support are unavailable).")
            return false
        }
        if (Purchases.isConfigured) return true

        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        Purchases.logLevel = if (debuggable) LogLevel.DEBUG else LogLevel.WARN
        return try {
            Purchases.configure(PurchasesConfiguration(apiKey))
            true
        } catch (e: Exception) {
            // Некоректний/заборонений ключ не має ламати запуск застосунку — лише вимикає покупки.
            Log.e(TAG, "RevenueCat configure failed", e)
            false
        }
    }
}

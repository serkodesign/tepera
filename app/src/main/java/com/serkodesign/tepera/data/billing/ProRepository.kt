package com.serkodesign.tepera.data.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Стан Tepera Pro з інформації про клієнта RevenueCat. [isPro] — entitlement
 * [RevenueCatConfig.ENTITLEMENT_PRO] активний. Поки жодна функція не гейтиться (за рішенням
 * користувача — лише інфраструктура), це лише відображається в Налаштуваннях.
 */
data class ProState(
    /** SDK налаштований (є ключ) — інакше екрани Pro показують "поки недоступно". */
    val configured: Boolean = false,
    /** Інформацію про клієнта вже отримано принаймні раз. */
    val loaded: Boolean = false,
    val isPro: Boolean = false,
    /** Product id активного entitlement (наприклад lifetime/yearly/monthly з Play). */
    val productId: String? = null,
    /** null для lifetime (без дати завершення). */
    val expiresAtMillis: Long? = null,
    val willRenew: Boolean = false,
    /** Не вдалося отримати інформацію (мережа/помилка) — UI показує спокійне повідомлення. */
    val loadFailed: Boolean = false
) {
    val isLifetime: Boolean get() = isPro && expiresAtMillis == null
}

enum class RestoreOutcome { ProRestored, NothingToRestore, Failed }

interface ProRepository {
    val state: StateFlow<ProState>

    /** Оновити інформацію про клієнта (при відкритті екрана Pro й поверненні до застосунку). */
    fun refresh()

    /** "Відновити покупки" (Google Play → RevenueCat), потім повернути, чи став активним Pro. */
    suspend fun restore(): RestoreOutcome
}

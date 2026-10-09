package com.serkodesign.tepera.data.repository

import android.content.Context
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.util.EarlyUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * P-4 (`CLAUDE-CODE-PLAN-pro.md`): єдина точка доступу до стану Pro — усі майбутні перевірки
 * (P-11…P-16) йдуть ЛИШЕ через це, жодних розкиданих `if (BuildConfig…)`. `FreeOnlyProAccess`
 * нижче нічого не знає про платіжний шар (RevenueCat/Billing) — жодної залежності (Ж-1).
 * Реалізація на білінгу підміняє цю лише в P-20 (етап 2), ViewModel/UI цього не помічають.
 *
 * **Сигнатура `isEarlyUser()` — `suspend`, не чиста функція, як у начерку плану** ("напр.",
 * плановий начерк ілюстративний): реальний розрахунок читає `SettingsStore.firstSeenVersionCode`
 * (DataStore, inherently suspend) — повертати синхронний `Boolean` означало б або блокувати
 * потік, або кешувати значення поза чергою читань, обидва варіанти гірші за просто `suspend fun`.
 */
interface ProAccess {
    val isPro: StateFlow<Boolean>
    suspend fun isEarlyUser(): Boolean
}

/**
 * Поки платіжного шару немає (до етапу 2) — `isPro` завжди `false`. `isEarlyUser()` уже рахує
 * по-справжньому (`EarlyUser.isEarlyUser()`, P-3), з сентинельними [PRO_LAUNCH_VERSION_CODE]/
 * [PRO_LAUNCH_AT_MILLIS]: Pro ще не існує, тож будь-хто, хто встановив Tepera ДО сьогодні, за
 * визначенням "ранній" — `Int.MAX_VALUE`/`Long.MAX_VALUE` гарантують `true` для будь-якого
 * реального `firstSeenVersionCode`/`firstInstallTime`. P-20 замінить ці дві константи на реальні
 * значення в момент фактичного запуску Pro (версія й дата релізу стають відомі лише тоді).
 */
class FreeOnlyProAccess(
    private val context: Context,
    private val settingsStore: SettingsStore
) : ProAccess {

    override val isPro: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun isEarlyUser(): Boolean {
        val firstSeenVersionCode = settingsStore.firstSeenVersionCode.first()
        val firstInstallTimeMillis = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }.getOrDefault(0L)
        return EarlyUser.isEarlyUser(
            firstSeenVersionCode = firstSeenVersionCode,
            proLaunchVersionCode = PRO_LAUNCH_VERSION_CODE,
            firstInstallTimeMillis = firstInstallTimeMillis,
            proLaunchAtMillis = PRO_LAUNCH_AT_MILLIS
        )
    }

    companion object {
        // TODO(P-20): замінити на реальний versionCode/дату релізу Pro, коли вони стануть відомі.
        const val PRO_LAUNCH_VERSION_CODE: Int = Int.MAX_VALUE
        const val PRO_LAUNCH_AT_MILLIS: Long = Long.MAX_VALUE
    }
}

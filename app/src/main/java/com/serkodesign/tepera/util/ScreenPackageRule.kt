package com.serkodesign.tepera.util

/** Що службі доступності робити з пакетом вікна, що з'явилось. */
enum class PackageAction {
    /** Системний екран (налаштування, Google Play, інсталятор, дозволи): завершити сесію м'якого виходу й прибрати затемнення одразу. */
    END_SESSION,
    /** Службове вікно (власний пакет, SystemUI, клавіатура…): не рахується застосунком і сесію не завершує. */
    IGNORE,
    /** Звичайний застосунок. */
    NORMAL
}

/**
 * Правило «пакет → дія» для служби (чиста логіка, тестується без Android). Над [endSessionPackages] служба ніколи не
 * показує паузу, лист, задачки чи затемнення, навіть якщо людина обрала такий пакет стрічкою чи воротами.
 */
object ScreenPackageRule {
    const val OWN_PACKAGE = "com.serkodesign.tepera"

    val endSessionPackages: Set<String> = setOf(
        "com.android.settings",
        "com.android.settings.intelligence",
        "com.android.vending",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller"
    )

    /** Шторка, системні діалоги, клавіатури: на секунди, сесію не завершують. */
    val ignoredSystemPackages: Set<String> = setOf(
        "com.android.systemui",
        "android",
        "com.android.intentresolver",
        "com.google.android.gms",
        "com.samsung.android.sidegesturepad"
    )

    fun classify(
        packageName: String,
        ownPackage: String = OWN_PACKAGE,
        launcherPackages: Set<String> = emptySet(),
        inputMethodPackages: Set<String> = emptySet()
    ): PackageAction = when {
        packageName == ownPackage -> PackageAction.IGNORE
        packageName in launcherPackages -> PackageAction.END_SESSION
        packageName in endSessionPackages -> PackageAction.END_SESSION
        packageName in ignoredSystemPackages || packageName in inputMethodPackages -> PackageAction.IGNORE
        else -> PackageAction.NORMAL
    }
}

/** Зв'язок між MainActivity і службою (один процес): відкрився головний екран Tepera — сесію м'якого виходу завершено. */
object SoftExitSessionBridge {
    @Volatile var endSession: (() -> Unit)? = null
}

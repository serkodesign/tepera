package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenPackageRuleTest {
    private val launchers = setOf("com.sec.android.app.launcher")
    private val keyboards = setOf("com.samsung.android.honeyboard")

    private fun c(pkg: String) = ScreenPackageRule.classify(pkg, launcherPackages = launchers, inputMethodPackages = keyboards)

    @Test fun systemScreensEndSession() {
        listOf(
            "com.android.settings", "com.android.vending", "com.google.android.packageinstaller",
            "com.android.packageinstaller", "com.google.android.permissioncontroller", "com.android.permissioncontroller"
        ).forEach { assertEquals(it, PackageAction.END_SESSION, c(it)) }
    }

    @Test fun launcherEndsSession() = assertEquals(PackageAction.END_SESSION, c("com.sec.android.app.launcher"))

    @Test fun ownPackageIsIgnored() = assertEquals(PackageAction.IGNORE, c("com.serkodesign.tepera"))

    @Test fun systemUiAndKeyboardKeepSession() {
        assertEquals(PackageAction.IGNORE, c("com.android.systemui"))
        assertEquals(PackageAction.IGNORE, c("com.samsung.android.honeyboard"))
    }

    @Test fun ordinaryAppIsNormal() = assertEquals(PackageAction.NORMAL, c("com.instagram.android"))
}

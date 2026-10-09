package com.serkodesign.tepera.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** P-3 (`CLAUDE-CODE-PLAN-pro.md`): grandfathering. */
class EarlyUserTest {

    private val proLaunchVersionCode = 20
    private val proLaunchAt = 2_000_000L

    @Test
    fun installedBeforeProLaunch_isEarly() {
        assertTrue(EarlyUser.isEarlyUser(15, proLaunchVersionCode, 0, proLaunchAt))
    }

    @Test
    fun installedAfterProLaunch_isNotEarly() {
        assertFalse(EarlyUser.isEarlyUser(21, proLaunchVersionCode, 0, proLaunchAt))
    }

    @Test
    fun installedOnTheExactProLaunchVersion_isNotEarly() {
        // Отримав застосунок РАЗОМ із версією, де Pro вже існує, — не "до" неї.
        assertFalse(EarlyUser.isEarlyUser(proLaunchVersionCode, proLaunchVersionCode, 0, proLaunchAt))
    }

    @Test
    fun missingVersionCode_fallsBackToFirstInstallTime_beforeLaunch_isEarly() {
        assertTrue(EarlyUser.isEarlyUser(null, proLaunchVersionCode, proLaunchAt - 1, proLaunchAt))
    }

    @Test
    fun missingVersionCode_fallsBackToFirstInstallTime_atOrAfterLaunch_isNotEarly() {
        assertFalse(EarlyUser.isEarlyUser(null, proLaunchVersionCode, proLaunchAt, proLaunchAt))
        assertFalse(EarlyUser.isEarlyUser(null, proLaunchVersionCode, proLaunchAt + 1, proLaunchAt))
    }

    @Test
    fun versionCodePresent_firstInstallTimeIgnored() {
        // firstSeenVersionCode наявний — firstInstallTimeMillis/proLaunchAtMillis не беруться до уваги взагалі.
        assertTrue(EarlyUser.isEarlyUser(15, proLaunchVersionCode, Long.MAX_VALUE, 0))
        assertFalse(EarlyUser.isEarlyUser(25, proLaunchVersionCode, 0, Long.MAX_VALUE))
    }
}

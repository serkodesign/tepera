package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Test

class A11yFeatureStateTest {
    @Test fun unavailableWinsOverEverything() {
        assertEquals(A11yFeatureState.Unavailable, A11yFeatureState.compute(false, true, true))
        assertEquals(A11yFeatureState.Unavailable, A11yFeatureState.compute(false, false, false))
    }

    @Test fun noConsentNeedsDisclosureEvenIfServiceEnabled() {
        assertEquals(A11yFeatureState.NeedsDisclosure, A11yFeatureState.compute(true, false, false))
        assertEquals(A11yFeatureState.NeedsDisclosure, A11yFeatureState.compute(true, false, true))
    }

    @Test fun consentWithoutServiceNeedsServiceEnabled() {
        assertEquals(A11yFeatureState.NeedsServiceEnabled, A11yFeatureState.compute(true, true, false))
    }

    @Test fun consentAndServiceIsActive() {
        assertEquals(A11yFeatureState.Active, A11yFeatureState.compute(true, true, true))
    }
}

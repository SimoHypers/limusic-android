package com.simohypers.limusic.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompareVersionsTest {
    @Test
    fun ordersVersions() {
        assertTrue(compareVersions("1.0.1", "1.0.0") > 0)
        assertTrue(compareVersions("1.10.0", "1.9.0") > 0)
        assertTrue(compareVersions("1.0.0", "1.0.0-rc.2") > 0)
        assertTrue(compareVersions("1.0.0-rc.10", "1.0.0-rc.9") > 0)
        assertTrue(compareVersions("1.0.0-rc.3", "1.0.0-rc.2") > 0)
        assertTrue(compareVersions("1.0.1-rc.1", "1.0.0") > 0)
        assertTrue(compareVersions("0.1.0", "1.0.0-rc.2") < 0)
        assertEquals(0, compareVersions("1.0.0-rc.2", "1.0.0-rc.2"))
    }

    @Test
    fun onlyReleaseTagsAreVersions() {
        assertTrue(isVersion("1.0.0"))
        assertTrue(isVersion("1.0.0-rc.3"))
        assertFalse(isVersion("dev"))
        assertFalse(isVersion("1.0"))
    }
}

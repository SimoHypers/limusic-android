package com.simohypers.limusic.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectivityTest {
    @Test
    fun reconnectsOnlyWhenAWorkingNewConnectionAppears() =
        runBlocking {
            Connectivity.update(1, true)
            var reconnects = 0
            val watcher = launch(Dispatchers.Unconfined) { Connectivity.reconnects().collect { reconnects++ } }

            Connectivity.update(1, true) // the same network again
            Connectivity.lost(7) // an old network dropping after a switch
            assertEquals(0, reconnects)
            assertTrue(Connectivity.state.value.hasNetwork)

            Connectivity.lost(1)
            assertFalse(Connectivity.state.value.hasNetwork)
            Connectivity.update(2, false) // back, but not checked yet
            assertEquals(0, reconnects)
            Connectivity.update(2, true)
            assertEquals(1, reconnects)

            Connectivity.update(3, true) // Wi-Fi to cellular
            assertEquals(2, reconnects)
            Connectivity.update(3, false) // that network died while connected
            assertFalse(Connectivity.state.value.validated)
            Connectivity.update(3, true) // and came back
            assertEquals(3, reconnects)
            watcher.cancel()
        }
}

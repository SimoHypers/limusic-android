package com.simohypers.limusic.listentogether

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerClockTest {
    @Test
    fun extrapolatesFromTheAssumedServerTime() {
        var elapsedRealtime = 1_000L
        val clock = ServerClock { elapsedRealtime }

        assertNull(clock.now())
        assertEquals(500L, clock.positionAt(500L, 10_000L, isPlaying = true))

        clock.assumeServerTime(10_065L)
        assertEquals(10_065L, clock.now())
        assertEquals(565L, clock.positionAt(500L, 10_000L, isPlaying = true))

        elapsedRealtime += 100L
        assertEquals(10_165L, clock.now())
        assertEquals(665L, clock.positionAt(500L, 10_000L, isPlaying = true))
        assertEquals(500L, clock.positionAt(500L, 10_000L, isPlaying = false))
        // A time in the future never rewinds the position.
        assertEquals(500L, clock.positionAt(500L, 20_000L, isPlaying = true))
    }
}

package com.simohypers.limusic.extension

import com.simohypers.limusic.domain.data.model.metadata.Line
import kotlin.test.Test
import kotlin.test.assertEquals

class FarLinesTest {
    private fun lines(vararg agents: String?) = agents.map { Line(endTimeMs = "0", startTimeMs = "0", words = "", agent = it) }

    @Test
    fun theSecondSingerSitsFar() {
        // By number, not by who sings first: v2 opening the song is still the far side.
        assertEquals(setOf(0, 3), farLines(lines("v2", "v1", null, "v2", "v1000")))
        // A third singer comes back to the near side.
        assertEquals(setOf(1), farLines(lines("v1", "v2", "v3")))
    }

    @Test
    fun oneSingerOrNoneMovesNothing() {
        assertEquals(emptySet(), farLines(lines("v1", "v1000", null)))
        assertEquals(emptySet(), farLines(lines(null, null)))
        assertEquals(emptySet(), farLines(lines("v1", "x2", "v12345")))
    }
}

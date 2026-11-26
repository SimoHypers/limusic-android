package com.simohypers.limusic.data.listentogether

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayableStartTest {
    @Test
    fun localFilesBeforeTheTappedRowDontShiftIt() {
        // Two local files, then the tapped song: it is the first playable one.
        assertEquals(0, playableStart(listOf("", "", "a", "b"), 2))
        assertEquals(1, playableStart(listOf("a", "", "b", "", "c"), 2))
        assertEquals(2, playableStart(listOf("a", "", "b", "", "c"), 4))
    }

    @Test
    fun aTappedLocalFileStartsTheNextPlayableSong() {
        assertEquals(1, playableStart(listOf("a", "", "", "b", "c"), 1))
        assertEquals(1, playableStart(listOf("a", "", "", "b", "c"), 2))
    }

    @Test
    fun nothingPlayableFromTheTappedRowOnStartsNothing() {
        assertNull(playableStart(listOf("a", "b", ""), 2))
        assertNull(playableStart(listOf("", ""), 0))
        assertNull(playableStart(emptyList(), null))
    }

    @Test
    fun noStartIsTheFirstPlayableSong() {
        assertEquals(0, playableStart(listOf("a", "b"), null))
        assertEquals(0, playableStart(listOf("", "a", "b"), null))
    }

    @Test
    fun anOutOfRangeStartIsClamped() {
        assertEquals(1, playableStart(listOf("a", "b"), 9))
        assertEquals(0, playableStart(listOf("a", "b"), -3))
    }
}

package com.simohypers.limusic.domain.data.model.lyrics

import com.simohypers.limusic.domain.data.entities.LyricsEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LyricsRecheckTest {
    private val day = RECHECK_LIMUSIC_SECS + 1
    private val week = RECHECK_OTHER_SECS + 1

    private fun cached(
        provider: String,
        syncType: String = "RICH_SYNCED",
        pinned: Boolean = false,
    ) = LyricsEntity("aaaaaaaaaaa", error = false, lines = null, syncType = syncType, provider = provider, fetchedAt = 1, pinned = pinned)

    private fun due(
        l: LyricsEntity,
        age: Long,
        setting: String? = null,
    ) = LyricsRecheck.due(l, age, LyricsSource.order(setting))

    @Test
    fun limusicsOwnByRevisionAfterADay() {
        assertNull(due(cached("limusic"), RECHECK_LIMUSIC_SECS))
        assertEquals(LyricsRecheck.REVISION, due(cached("limusic"), day))
    }

    @Test
    fun anotherSourceAsksAfterAWeekWhereLimusicWouldWin() {
        assertNull(due(cached("boidu"), day))
        assertEquals(LyricsRecheck.ASK, due(cached("boidu"), week))
        // Synced from a source above Limusic: the walk would stop there again.
        assertNull(due(cached("lrclib", "LINE_SYNCED"), week, "lrclib,limusic"))
        // Plain text from that same source: Limusic's synced answer would win the walk.
        assertEquals(LyricsRecheck.ASK, due(cached("lrclib", "UNSYNCED"), week, "lrclib,limusic"))
        // A source switched off since: whatever a walk finds now, it isn't that.
        assertEquals(LyricsRecheck.ASK, due(cached("lrclib", "LINE_SYNCED"), week, "-lrclib,limusic"))
    }

    @Test
    fun neverPinnedNeverWithLimusicOff() {
        assertNull(due(cached("limusic", pinned = true), week))
        assertNull(due(cached("boidu"), week, "-limusic"))
        assertNull(due(cached("limusic").copy(error = true), week))
    }
}

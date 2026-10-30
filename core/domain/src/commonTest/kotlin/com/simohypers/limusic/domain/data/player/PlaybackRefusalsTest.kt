package com.simohypers.limusic.domain.data.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackRefusalsTest {
    @Test
    fun unblockForgetsOnlyTheSongsTheBlockWasRecordedAgainst() {
        val bot = "Sign in to confirm you're not a bot"
        PlaybackRefusals.record("premium", "This video is only available to Music Premium members")
        PlaybackRefusals.record("tripped", bot)
        PlaybackRefusals.block(bot)
        assertEquals(bot, PlaybackRefusals.blocked.value)

        PlaybackRefusals.unblock()

        assertNull(PlaybackRefusals.blocked.value)
        assertNull(PlaybackRefusals.reasonFor("tripped"))
        assertEquals("This video is only available to Music Premium members", PlaybackRefusals.reasonFor("premium"))
    }

    @Test
    fun forgettingRefusedDownloadsAlsoForgetsThisRunsRefusalsOfThem() {
        PlaybackRefusals.record("gone", "This video is not available")
        PlaybackRefusals.record("other", "This video is not available")
        PlaybackRefusals.rememberDownloadRefusals(listOf("gone", "earlier"))

        assertEquals(setOf("gone", "earlier"), PlaybackRefusals.forgetDownloadRefusals())

        assertTrue(PlaybackRefusals.refusedDownloads.value.isEmpty())
        assertNull(PlaybackRefusals.reasonFor("gone"))
        assertEquals("This video is not available", PlaybackRefusals.reasonFor("other"))
    }
}

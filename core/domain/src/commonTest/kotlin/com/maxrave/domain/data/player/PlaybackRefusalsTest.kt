package com.maxrave.domain.data.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}

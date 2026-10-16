package org.simpmusic.listentogether

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceLinkSessionTest {
    @Test
    fun theSendRule() {
        val actions =
            listOf(
                PlaybackActions.CHANGE_TRACK to true,
                PlaybackActions.CHANGE_TRACK to false,
                PlaybackActions.PLAY to null,
                PlaybackActions.PAUSE to null,
                PlaybackActions.SEEK to null,
                PlaybackActions.SYNC_QUEUE to null,
                PlaybackActions.SET_VOLUME to null,
            )
        // The player sends everything as is.
        actions.forEach { (a, playing) -> assertEquals(LinkSend.SENT, linkSendRule(true, a, playing), a) }
        // Anyone else may only take over.
        assertEquals(LinkSend.SENT, linkSendRule(false, PlaybackActions.CHANGE_TRACK, true))
        assertEquals(LinkSend.TAKEOVER, linkSendRule(false, PlaybackActions.PLAY, null))
        actions.drop(1).filter { it.first != PlaybackActions.PLAY }.forEach { (a, playing) ->
            assertEquals(LinkSend.DROPPED, linkSendRule(false, a, playing), a)
        }
    }

    @Test
    fun theMirrorFollowsThePlayer() {
        val track = TrackInfo(id = "dQw4w9WgXcQ")
        var s =
            LinkState().applied(
                PlaybackActionPayload(action = PlaybackActions.CHANGE_TRACK, position = 1000, trackInfo = track, playing = true),
                now = 10_000,
            )
        assertEquals(track, s.currentTrack)
        assertEquals(true, s.isPlaying)
        assertEquals(10_000, s.lastUpdate)
        s = s.applied(PlaybackActionPayload(action = PlaybackActions.PAUSE, position = 2500), now = 11_500)
        assertEquals(false, s.isPlaying)
        assertEquals(2500, s.position)
        s = s.applied(PlaybackActionPayload(action = PlaybackActions.SET_VOLUME, volume = 0.4f), now = 0)
        assertEquals(0.4f, s.volume)
    }
}

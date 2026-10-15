package org.simpmusic.listentogether

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the JSON against Limusic's `crates/listen-protocol` (serde, internally tagged on `type`,
 * snake_case). The server frames below are what that crate serializes.
 */
class MessageCodecTest {
    private val codec = MessageCodec()

    @Test
    fun changeTrackCarriesTrackQueueAndPlaying() {
        val json =
            codec.encode(
                MessageTypes.PLAYBACK_ACTION,
                PlaybackActionPayload(
                    action = PlaybackActions.CHANGE_TRACK,
                    position = 4200,
                    trackInfo = TrackInfo(id = "dQw4w9WgXcQ", title = "x", artist = "y", duration = 180_000),
                    queue = listOf(TrackInfo(id = "a-B_c123456")),
                    playing = true,
                ),
            )
        assertNotNull(json)
        assertTrue("\"type\":\"playback\"" in json)
        assertTrue("\"kind\":\"change_track\"" in json)
        assertTrue("\"duration_ms\":180000" in json)
        assertTrue("\"playing\":true" in json)
        assertTrue("\"queue\":[{\"id\":\"a-B_c123456\"" in json)
    }

    @Test
    fun unitMessagesAreJustTheTag() {
        assertEquals("{\"type\":\"leave_room\"}", codec.encode(MessageTypes.LEAVE_ROOM, LeaveRoomPayload()))
        assertEquals("{\"type\":\"ping\"}", codec.encode(MessageTypes.PING, null))
    }

    @Test
    fun messagesWithoutACounterpartAreNotSent() {
        assertNull(codec.encode("chat", null))
        assertNull(codec.encode(MessageTypes.PLAYBACK_ACTION, PlaybackActionPayload(action = "skip_next")))
    }

    @Test
    fun syncPlaybackDecodesWithServerTimeAndPlaying() {
        val (type, payload) =
            codec.decode(
                """{"type":"sync_playback","kind":"change_track","position_ms":1000,"server_time_ms":5000,""" +
                    """"track":{"id":"dQw4w9WgXcQ","title":"t","artist":"a","duration_ms":0},"queue":[],"playing":true,"volume":0.0}""",
            )!!
        assertEquals(MessageTypes.SYNC_PLAYBACK, type)
        val p = payload as PlaybackActionPayload
        assertEquals(PlaybackActions.CHANGE_TRACK, p.action)
        assertEquals(1000, p.position)
        assertEquals(5000, p.capturedAtServerTime)
        assertEquals("dQw4w9WgXcQ", p.trackInfo?.id)
        assertEquals(true, p.playing)
    }

    @Test
    fun joinApprovedTakesTheRoomCodeFromItsState() {
        val (type, payload) =
            codec.decode(
                """{"type":"join_approved","user_id":"u2","session_token":"tok","state":{"room_code":"ABC123","host_id":"u1",""" +
                    """"users":[{"user_id":"u1","username":"host","is_host":true,"is_connected":true}],"current_track":null,""" +
                    """"is_playing":false,"position_ms":0,"last_update_ms":0,"volume":1.0,"queue":[]}}""",
            )!!
        assertEquals(MessageTypes.JOIN_APPROVED, type)
        val p = payload as JoinApprovedPayload
        assertEquals("ABC123", p.roomCode)
        assertEquals("tok", p.sessionToken)
        assertEquals("host", p.state?.users?.single()?.username)
    }

    @Test
    fun unknownTypesAndGarbageDoNotThrow() {
        assertEquals("something_new" to null, codec.decode("""{"type":"something_new","x":1}"""))
        assertNull(codec.decode("not json"))
    }
}

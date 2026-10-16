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

    // Device link. Each line is byte for byte the one in `crates/listen-protocol`'s
    // `device_link_lines` test, so a drift fails on both sides.

    private val secret = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ"
    private val device = "0b6e1f9a-3c2d-4e5f-8a7b-9c0d1e2f3a4b"

    @Test
    fun deviceLinkClientLines() {
        assertEquals(
            """{"type":"join_link","secret":"$secret","device_id":"$device","name":"fedora"}""",
            codec.encode(MessageTypes.JOIN_LINK, JoinLinkPayload(secret = secret, deviceId = device, name = "fedora")),
        )
        assertEquals("""{"type":"pair_start"}""", codec.encode(MessageTypes.PAIR_START, null))
        assertEquals("""{"type":"pair_claim","code":"K7QX2M"}""", codec.encode(MessageTypes.PAIR_CLAIM, PairClaimPayload("K7QX2M")))
        assertEquals(
            """{"type":"remote","action":{"kind":"seek","position_ms":61000}}""",
            codec.encode(MessageTypes.REMOTE, RemotePayload(RemoteAction.Seek(61000))),
        )
        assertEquals(
            """{"type":"remote","action":{"kind":"set_volume","volume":0.5}}""",
            codec.encode(MessageTypes.REMOTE, RemotePayload(RemoteAction.SetVolume(0.5))),
        )
    }

    @Test
    fun deviceLinkServerLines() {
        val (t1, joined) =
            codec.decode(
                """{"type":"link_joined","state":{"room_code":"","host_id":"","users":[{"user_id":"$device","username":"fedora",""" +
                    """"is_host":false,"is_connected":true}],"current_track":null,"is_playing":false,"position_ms":0,""" +
                    """"last_update_ms":0,"volume":1.0,"queue":[]}}""",
            )!!
        assertEquals(MessageTypes.LINK_JOINED, t1)
        val state = (joined as LinkJoinedPayload).state
        assertEquals("", state.roomCode)
        assertEquals("fedora", state.users.single().username)
        assertEquals(1f, state.volume)

        val (t2, code) = codec.decode("""{"type":"pair_code","code":"K7QX2M","expires_in_s":120}""")!!
        assertEquals(MessageTypes.PAIR_CODE to PairCodePayload("K7QX2M", 120), t2 to code)

        val (t3, paired) = codec.decode("""{"type":"paired","secret":"$secret"}""")!!
        assertEquals(MessageTypes.PAIRED to PairedPayload(secret), t3 to paired)

        val (t4, remote) = codec.decode("""{"type":"remote","from":"$device","action":{"kind":"next"}}""")!!
        assertEquals(MessageTypes.REMOTE to RemotePayload(RemoteAction.Next, from = device), t4 to remote)
    }

    @Test
    fun aPlayTrackRemoteRoundTrips() {
        val track = TrackInfo(id = "dQw4w9WgXcQ", title = "x", artist = "y", duration = 1000)
        val json = codec.encode(MessageTypes.REMOTE, RemotePayload(RemoteAction.PlayTrack(track)))!!
        val (_, payload) = codec.decode(json.replace("\"type\":\"remote\"", "\"type\":\"remote\",\"from\":\"d\""))!!
        assertEquals(RemotePayload(RemoteAction.PlayTrack(track), from = "d"), payload)
    }
}

package com.simohypers.limusic.discord

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Desktop's discord.rs tests, on a fake clock. */
class PlannerTest {
    private var mono = 1_000_000L
    private var wall = 1_700_000_000_000L

    private fun tick(ms: Long) {
        mono += ms
        wall += ms
    }

    private fun track(id: String) = DiscordTrack(videoId = id, title = "Tell Ur Girlfriend", artists = "Lay Bankz")

    private fun planner(cfg: DiscordConfig = DiscordConfig()) = Planner(cfg, { mono }, { wall })

    private fun playing(
        id: String,
        posMs: Long,
        cfg: DiscordConfig = DiscordConfig(),
    ) = planner(cfg).apply {
        setTrack(track(id))
        setPlaying(true)
        setPosition(posMs)
    }

    /** As push_card leaves it: a card up for the current state, the floor clear. */
    private fun Planner.pushNow() {
        val (start, _) = timeline()
        noteFrame()
        pushed(start)
        tick(5_000)
        setPosition(estimate())
    }

    @Test
    fun aNewTrackWaitsForItsLengthThenPushesOnce() {
        val p = playing("old", 200_000).apply { setDuration(210_000) }
        p.pushNow()
        p.setTrack(track("new"))
        assertIs<Act.Wait>(p.plan(), "hold the push while the length is unknown")
        tick(TRACK_SETTLE + 10)
        p.setDuration(185_000)
        assertEquals(Act.Push, p.plan())
        p.pushNow()
        assertEquals(Act.Idle, p.plan(), "one push per track change")
    }

    /** A phone loads the next stream for seconds: the card waits for the audio, then starts at 0. */
    @Test
    fun aNewTrackWaitsForItsAudioToStart() {
        val p = playing("old", 200_000).apply { setDuration(210_000) }
        p.pushNow()
        p.setTrack(track("new"))
        p.setPlaying(true, moving = false)
        p.setDuration(185_000)
        tick(TRACK_SETTLE + 10)
        assertIs<Act.Wait>(p.plan(), "still loading: the old card stays")
        tick(2_500)
        assertEquals(0, p.estimate(), "loading does not age the new track's position")
        p.setPlaying(true, moving = true)
        assertEquals(Act.Push, p.plan())
        assertEquals(wall, p.timeline().first, "the bar starts when the audio does")
    }

    /** The skip the phone showed: the next song started before its metadata was read. */
    @Test
    fun lateMetadataRepushesWithoutRestartingTheClock() {
        val p = playing("abc", 30_000).apply { setDuration(185_000) }
        p.pushNow()
        p.updateTrack(DiscordTrack("other", "Nope", "Nobody", album = "X"))
        assertEquals(Act.Idle, p.plan(), "another track's metadata is dropped")
        val before = p.estimate()
        p.updateTrack(DiscordTrack("abc", "Tell Ur Girlfriend", "", album = "After 7"))
        assertEquals(Act.Push, p.plan())
        assertEquals(before, p.estimate())
        assertEquals("Lay Bankz", p.track?.artists, "a blank never overwrites what was known")
        assertEquals("After 7", p.track?.album)
    }

    @Test
    fun aRebufferKeepsTheCard() {
        val p = playing("abc", 30_000).apply { setDuration(185_000) }
        p.pushNow()
        p.setPlaying(true, moving = false)
        assertEquals(Act.Idle, p.plan(), "a stall is not a pause")
    }

    @Test
    fun repeatOneKeepsTheLength() {
        val p = playing("abc", 180_000).apply { setDuration(185_000) }
        p.pushNow()
        p.setTrack(track("abc"))
        assertEquals(Act.Push, p.plan(), "the bar restarts at once, with its length")
        assertEquals(wall + 185_000, p.timeline().second)
    }

    @Test
    fun theGraceExpiresRatherThanHanging() {
        val p = playing("abc", 0)
        tick(START_GRACE + 50)
        assertEquals(Act.Push, p.plan())
    }

    @Test
    fun aLateLengthRepushesTheBar() {
        val p = playing("abc", 1_000)
        tick(START_GRACE + 50)
        p.pushNow()
        p.setDuration(185_000)
        assertEquals(Act.Push, p.plan())
    }

    @Test
    fun theSendFloorDefersRatherThanDiscards() {
        val p = playing("abc", 1_000).apply { setDuration(185_000) }
        p.pushNow()
        p.noteFrame()
        p.setPosition(120_000)
        val wait = assertIs<Act.Wait>(p.plan())
        assertTrue(wait.ms <= SEND_FLOOR)
        tick(SEND_FLOOR + 10)
        assertEquals(Act.Push, p.plan(), "the deferred seek still goes out")
    }

    @Test
    fun steadyPlaybackDoesNotPush() {
        val p = playing("abc", 30_000).apply { setDuration(185_000) }
        p.pushNow()
        tick(1_000)
        p.setPosition(p.estimate())
        assertFalse(p.wantsPush())
        p.setPosition(120_000)
        assertTrue(p.wantsPush(), "a seek must push")
    }

    @Test
    fun pausingClearsTheCardByDefault() {
        val p = playing("abc", 30_000).apply { setDuration(185_000) }
        p.pushNow()
        p.setPlaying(false)
        assertEquals(Act.Clear, p.plan())
        p.cleared()
        assertEquals(Act.Idle, p.plan())
    }

    @Test
    fun pauseFreezesTheEstimate() {
        val p = playing("abc", 10_000)
        tick(3_000)
        p.setPlaying(false)
        tick(60_000)
        assertEquals(13_000, p.estimate())
    }

    @Test
    fun keepWhilePausedWaitsForRealPlayback() {
        val p = planner(DiscordConfig(showPaused = true))
        p.setTrack(track("abc"))
        p.setPosition(42_000)
        assertEquals(Act.Idle, p.plan(), "a restored queue nobody started posts nothing")
        p.setPlaying(true)
        p.setDuration(185_000)
        assertEquals(Act.Push, p.plan())
        p.pushNow()
        p.setPlaying(false)
        assertEquals(Act.Push, p.plan(), "a real pause keeps the card, re-pushed without its bar")
        p.pushNow()
        tick(600_000)
        assertEquals(Act.Idle, p.plan(), "ten minutes paused is not ten minutes of seeking")
        p.setPlaying(true)
        assertTrue(p.wantsPush(), "resuming brings the bar back")
    }

    @Test
    fun droppingKeepWhilePausedClearsThePausedCard() {
        val p = playing("abc", 30_000, DiscordConfig(showPaused = true)).apply { setDuration(185_000) }
        p.setPlaying(false)
        p.pushNow()
        assertEquals(Act.Idle, p.plan())
        p.setConfig(DiscordConfig())
        assertEquals(Act.Clear, p.plan())
    }

    @Test
    fun aConfigChangeRepushesOnce() {
        val p = playing("abc", 30_000).apply { setDuration(185_000) }
        p.pushNow()
        p.setConfig(DiscordConfig(line2 = "album"))
        assertEquals(Act.Push, p.plan())
        p.pushNow()
        p.setConfig(DiscordConfig(line2 = "album"))
        assertFalse(p.wantsPush(), "saving the same config is not a change")
    }

    @Test
    fun aFullWindowWaits() {
        val p = playing("abc", 10_000).apply { setDuration(185_000) }
        repeat(MAX_FRAMES) {
            p.noteFrame()
            tick(2_000)
        }
        p.setPosition(100_000)
        val wait = assertIs<Act.Wait>(p.plan())
        assertTrue(wait.ms in 1..FRAME_WINDOW)
    }

    @Test
    fun aSkipBurstSettlesBeforePushing() {
        val p = playing("old", 10_000).apply { setDuration(185_000) }
        p.pushNow()
        repeat(5) { p.setTrack(track("new_$it")) }
        p.setDuration(185_000)
        val wait = assertIs<Act.Wait>(p.plan())
        assertTrue(wait.ms in 1..TRACK_SETTLE)
    }

    @Test
    fun aPlayingCardRefreshesAfter45s() {
        val p = playing("abc", 10_000).apply { setDuration(185_000) }
        p.pushNow()
        tick(REFRESH_INTERVAL)
        p.setPosition(p.estimate())
        assertEquals(Act.Push, p.plan())
    }

    @Test
    fun speedScalesTheBar() {
        val p = playing("abc", 60_000).apply { setDuration(180_000) }
        p.setSpeed(2f)
        val (start, end) = p.timeline()
        assertEquals(wall - 30_000, start)
        assertEquals(start + 90_000, end)
    }

    @Test
    fun theDefaultCardIsDesktopsCard() {
        val t = DiscordTrack("vid", "Tell Ur Girlfriend", "Lay Bankz", "UCartist", "After 7", "MPREalbum")
        val a = activity(t, DiscordConfig(), playing = true, start = 1L, end = 2L, art = "mp:art", badge = "mp:badge")
        assertEquals("\"Tell Ur Girlfriend\"", a["details"].toString())
        assertEquals("\"Lay Bankz\"", a["state"].toString())
        assertEquals("1", a["status_display_type"].toString())
        assertNull(a["details_url"], "links are opt-in")
        assertTrue(a["assets"].toString().contains("After 7"))
        assertTrue(a["buttons"].toString().contains("Get Limusic"))
    }

    @Test
    fun missingMetadataDropsTheSlot() {
        val t = DiscordTrack("vid", "V", "Lay Bankz")
        assertNull(t.text("album"))
        assertNull(t.button("album"))
        assertEquals("Lay Bankz", t.text("artist_album"))
        assertEquals(2, field("V").length, "one-char fields are padded, not rejected")
        assertTrue(field("é".repeat(200)).length <= 128)
    }

    @Test
    fun hideDetailsSendsNothingAboutTheTrack() {
        val t = DiscordTrack("vid", "Song", "Artist", album = "Album", thumbnail = "https://x")
        val a = activity(t, DiscordConfig(hideDetails = true), playing = true, start = 1L, end = 2L, art = "mp:art", badge = null)
        assertEquals(setOf("name", "type", "status_display_type"), a.keys)
    }
}

package com.maxrave.discord

import kotlin.math.abs

/** A seek this far off the pushed timeline needs a re-push; anything smaller is tick noise. */
internal const val SEEK_DRIFT = 2_000L

/** Discord silently drops an update that lands too soon after the last one. */
internal const val SEND_FLOOR = 1_500L

/** A playing card is re-sent this often, so a silently dropped update heals within a minute. */
internal const val REFRESH_INTERVAL = 45_000L

/**
 * How long a new track's card waits for its audio to start with its length known, so one push
 * carries the right bar instead of a guess and a correction. Desktop waits 800ms for mpv's length;
 * a phone resolves and buffers the stream first, which takes seconds on mobile data.
 */
internal const val START_GRACE = 5_000L

/** "Clients may only update their game status 5 times per 20 seconds" (gateway docs), plus 1s slack. */
internal const val MAX_FRAMES = 5
internal const val FRAME_WINDOW = 21_000L

/** A skip burst arrives within about a second; waiting this long collapses it into one push. */
internal const val TRACK_SETTLE = 1_000L

/** Floor for any computed wait, so a zero never spins the loop. */
internal const val MIN_WAIT = 10L

internal sealed interface Act {
    data object Idle : Act

    data class Wait(
        val ms: Long,
    ) : Act

    data object Push : Act

    data object Clear : Act
}

/** What Discord is showing: the card we last pushed. Null means nothing is up. */
internal data class Sent(
    val videoId: String,
    val playing: Boolean,
    /** Epoch ms the pushed bar started at; `wall - startMs` is where Discord's bar is now. */
    val startMs: Long,
    /** The pushed bar's length in real ms (track length over speed), 0 when it went out without one. */
    val barMs: Long,
)

/**
 * The timing rules of desktop Limusic's discord.rs, as a state machine with no socket in it so
 * they can be tested. [now] is a monotonic clock, [wall] the epoch clock Discord's timestamps use.
 *
 * Positions are track ms. Discord draws its bar in real time, so at a playback speed other than 1x
 * the pushed bar is the track's length over the speed, and its position likewise.
 */
internal class Planner(
    var cfg: DiscordConfig,
    private val now: () -> Long,
    private val wall: () -> Long,
) {
    var track: DiscordTrack? = null
        private set
    private var trackAt = 0L
    private var durationMs = 0L
    /** The user wants playback: playing, or loading the next track. Decides whether a card shows. */
    var playing = false
        private set

    /** Audio is actually moving. Only this runs the clock, so loading a track doesn't age its position. */
    private var running = false

    /** Has anything played this session? The queue restored at launch arrives paused, and "keep
     *  while paused" must not post a card for a track nobody pressed play on. */
    private var played = false
    private var speed = 1f
    private var posMs = 0L
    private var posAt = 0L
    var sent: Sent? = null
        private set

    /** The layout changed since the last push. Kept apart from [sent], which has to stay the record
     *  of what is on screen, or turning "keep while paused" off while paused would leave the card up. */
    private var cfgDirty = false
    private var lastSend: Long? = null
    private val sends = ArrayDeque<Long>()

    /** Best current position: the last one reported, aged by the clock while playing. */
    fun estimate(): Long = if (running) posMs + ((now() - posAt) * speed).toLong() else posMs

    /** Every track starts from the top; the previous one's position must not leak into its card.
     *  The same track again (repeat-one) keeps its length, which the player may not report twice. */
    fun setTrack(t: DiscordTrack) {
        if (t.videoId != track?.videoId) durationMs = 0
        track = t
        trackAt = now()
        posMs = 0
        posAt = now()
    }

    /** Fuller metadata for the track already playing (album, artwork): no clock reset. A card already
     *  up for it is re-pushed; one still waiting goes out with the new data. Another track's is dropped. */
    fun updateTrack(t: DiscordTrack) {
        val old = track?.takeIf { it.videoId == t.videoId } ?: return
        // Fills gaps, never blanks: the queue row can know an album the database row doesn't.
        val merged =
            DiscordTrack(
                videoId = old.videoId,
                title = t.title.ifBlank { old.title },
                artists = t.artists.ifBlank { old.artists },
                artistId = t.artistId ?: old.artistId,
                album = t.album?.takeIf { it.isNotBlank() } ?: old.album,
                albumId = t.albumId ?: old.albumId,
                thumbnail = t.thumbnail ?: old.thumbnail,
            )
        if (merged == old) return
        track = merged
        if (sent?.videoId == t.videoId) cfgDirty = true
    }

    fun setDuration(ms: Long) {
        durationMs = ms.coerceAtLeast(0)
    }

    fun setPosition(ms: Long) {
        posMs = ms.coerceAtLeast(0)
        posAt = now()
    }

    /** [on]: the user wants playback (a buffering stall stays on). [moving]: audio is playing. */
    fun setPlaying(
        on: Boolean,
        moving: Boolean = on,
    ) {
        played = played || on
        if (running != moving) {
            posMs = estimate()
            posAt = now()
            running = moving
        }
        playing = on
    }

    fun setSpeed(s: Float) {
        if (s > 0f && s != speed) {
            posMs = estimate()
            posAt = now()
            speed = s
        }
    }

    fun setConfig(c: DiscordConfig) {
        if (c != cfg) {
            cfg = c
            cfgDirty = true
        }
    }

    /** A fresh or dead socket shows nothing, so everything has to go out again. */
    fun forgetSent() {
        sent = null
    }

    fun wantsCard(): Boolean = track != null && (playing || (cfg.showPaused && played))

    fun plan(): Act {
        val wantCard = wantsCard()
        if (wantCard) {
            if (!wantsPush()) return Act.Idle
            if (sent != null && isNewCard()) {
                val rem = TRACK_SETTLE - (now() - trackAt)
                if (rem > 0) return Act.Wait(rem.coerceAtLeast(MIN_WAIT))
            }
            // Push before the audio starts and the bar runs ahead of it; push before the length lands
            // and the card goes out bar-less. Either way a second push follows at once, which Discord
            // drops for arriving too soon.
            if (isNewCard() && (durationMs <= 0 || (playing && !running))) {
                val rem = START_GRACE - (now() - trackAt)
                if (rem > 0) return Act.Wait(rem.coerceAtLeast(MIN_WAIT))
            }
        } else if (sent == null) {
            return Act.Idle
        }
        // Too soon: come back when the floor clears and send whatever is current then. Never drop
        // the update, a dropped one is invisible.
        floorRemaining()?.let { return Act.Wait(it.coerceAtLeast(MIN_WAIT)) }
        return if (wantCard) Act.Push else Act.Clear
    }

    /** Would Discord's card differ from what we last pushed? Keeps position ticks from becoming pushes. */
    fun wantsPush(): Boolean {
        val t = track ?: return false
        val s = sent ?: return true
        if (cfgDirty) return true
        if (s.videoId != t.videoId) return true
        // Pausing or resuming takes the bar away or brings it back.
        if (s.playing != playing) return true
        if (abs(barMs() - s.barMs) > 1_000) return true
        // A paused card has no timeline to drift against.
        if (!playing) return false
        if (lastSend?.let { now() - it >= REFRESH_INTERVAL } == true) return true
        // Seek detection off the pushed timeline catches every seek, whoever made it.
        return abs(barPosition() - (wall() - s.startMs)) > SEEK_DRIFT
    }

    /** The bar to push: epoch ms it started at, and where it ends (null when the length is unknown). */
    fun timeline(): Pair<Long, Long?> {
        val start = wall() - barPosition().coerceAtLeast(0)
        return start to barMs().takeIf { it > 0 }?.let { start + it }
    }

    fun pushed(startMs: Long) {
        val t = track ?: return
        sent = Sent(t.videoId, playing, startMs, barMs())
        cfgDirty = false
    }

    fun cleared() {
        sent = null
    }

    /** Charged for every frame put on the wire, accepted or not. */
    fun noteFrame() {
        val n = now()
        sends.addLast(n)
        while (sends.isNotEmpty() && n - sends.first() > FRAME_WINDOW) sends.removeFirst()
        lastSend = n
    }

    private fun isNewCard(): Boolean {
        val s = sent ?: return true
        val t = track ?: return true
        return s.videoId != t.videoId
    }

    private fun barMs(): Long = if (durationMs > 0) (durationMs / speed).toLong() else 0

    private fun barPosition(): Long = (estimate() / speed).toLong()

    private fun floorRemaining(): Long? {
        val last = lastSend ?: return null
        val floor = (SEND_FLOOR - (now() - last)).takeIf { it > 0 }
        val window = if (sends.size >= MAX_FRAMES) (FRAME_WINDOW - (now() - sends.first())).takeIf { it > 0 } else null
        return listOfNotNull(floor, window).maxOrNull()
    }
}

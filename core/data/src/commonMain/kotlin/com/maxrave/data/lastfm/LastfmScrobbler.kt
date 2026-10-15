package com.maxrave.data.lastfm

import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.simpmusic.lastfm.LASTFM_CONFIG_KEY
import org.simpmusic.lastfm.LastfmOutcome
import org.simpmusic.lastfm.LastfmTrack
import org.simpmusic.lastfm.Resolved
import org.simpmusic.lastfm.ScrobbleConfig
import org.simpmusic.lastfm.ScrobbleTrack
import org.simpmusic.lastfm.isLastfmAvailable
import org.simpmusic.lastfm.resolve
import org.simpmusic.lastfm.scrobble
import org.simpmusic.lastfm.scrobbleAt
import org.simpmusic.lastfm.updateNowPlaying
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "LastfmScrobbler"

/**
 * Desktop Limusic's scrobbler (src-tauri/src/lastfm.rs): a now-playing ping when a track starts and
 * one scrobble once it has played the configured share or minutes, whichever comes first. What gets
 * sent is [resolve]d from the stored config at send time, so a rule saved mid-track already applies
 * to that track's scrobble.
 *
 * The threshold runs off the player's position, like desktop: seeking forward can trigger it early.
 * ponytail: position-based threshold and no offline queue, both as desktop; revisit if scrobbles
 * visibly go missing.
 */
class LastfmScrobbler(
    private val dataStoreManager: DataStoreManager,
) {
    private val mutex = Mutex()
    private var track: ScrobbleTrack? = null
    private var durationSec = 0.0
    private var startedAt = 0L
    private var scrobbled = false

    /**
     * The player moved to [t]. [restart] is repeat-one playing the same track again, which is a new
     * listen; without it, a second report of the track already playing changes nothing.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun onTrackStarted(
        t: ScrobbleTrack,
        restart: Boolean = false,
    ) {
        mutex.withLock {
            val same = track?.videoId == t.videoId
            if (same && !restart) return
            // The player may not report the length again for the same track.
            if (!same) durationSec = 0.0
            track = t
            startedAt = Clock.System.now().epochSeconds
            scrobbled = false
        }
        nowPlaying()
    }

    /**
     * Fuller metadata for the track already playing, from the database. Keeps the clock, and re-sends
     * now-playing when it brings an album the first one went out without (desktop's late album).
     */
    suspend fun updateTrack(t: ScrobbleTrack) {
        val gainedAlbum =
            mutex.withLock {
                val old = track?.takeIf { it.videoId == t.videoId } ?: return
                val merged =
                    old.copy(
                        title = t.title.ifBlank { old.title },
                        artists = t.artists.ifEmpty { old.artists },
                        album = t.album?.takeIf { it.isNotBlank() } ?: old.album,
                        isVideo = t.isVideo || old.isVideo,
                    )
                track = merged
                old.album.isNullOrBlank() && !merged.album.isNullOrBlank()
            }
        if (gainedAlbum) nowPlaying()
    }

    /** The player's length for the current track: the only real duration there is. */
    suspend fun onDuration(ms: Long) {
        if (ms > 0) mutex.withLock { durationSec = ms / 1000.0 }
    }

    /** Cheap on every tick: once scrobbled it is a field read. */
    suspend fun onProgress(positionMs: Long) {
        val cfg = config()
        mutex.withLock {
            if (scrobbled || track == null) return
            val at = scrobbleAt(durationSec, cfg) ?: return
            if (positionMs / 1000.0 < at) return
            // Latched before the network call, and even if it fails: never re-fire per tick.
            scrobbled = true
        }
        send(cfg) { sk, r, t ->
            scrobble(sk, r.toLastfm(t), startedAt).also { log("track.scrobble", it, r) }
        }
    }

    private suspend fun nowPlaying() {
        val cfg = config()
        if (!cfg.nowPlaying) return
        send(cfg) { sk, r, t -> updateNowPlaying(sk, r.toLastfm(t)).also { log("track.updateNowPlaying", it, r) } }
    }

    /** Resolves the current track and hands it over, unless scrobbling is off, signed out, or skipped. */
    private suspend fun send(
        cfg: ScrobbleConfig,
        call: suspend (sessionKey: String, r: Resolved, t: ScrobbleTrack) -> LastfmOutcome,
    ) {
        if (!isLastfmAvailable() || !cfg.enabled) return
        val sessionKey = dataStoreManager.lastfmSessionKey.first().takeIf { it.isNotEmpty() } ?: return
        val t = mutex.withLock { track } ?: return
        val r = resolve(t, cfg)
        if (r.skip != null) {
            Logger.d(TAG, "Not sent (${r.skip}): ${t.videoId}")
            return
        }
        call(sessionKey, r, t)
    }

    private suspend fun config(): ScrobbleConfig = ScrobbleConfig.parse(dataStoreManager.getString(LASTFM_CONFIG_KEY).first())

    private fun Resolved.toLastfm(t: ScrobbleTrack) =
        LastfmTrack(
            artist = artist,
            track = title,
            album = album.ifEmpty { null },
            durationSeconds = durationSec.toInt().takeIf { it > 0 },
        )

    private suspend fun log(
        method: String,
        outcome: LastfmOutcome,
        r: Resolved,
    ) {
        when (outcome) {
            is LastfmOutcome.Ok -> Logger.d(TAG, "$method: ${r.artist} - ${r.title}")
            // Codes 1 and 2: Last.fm filtered the artist or track name, which is how bad metadata shows.
            is LastfmOutcome.Ignored -> Logger.w(TAG, "$method ignored (${outcome.code}): ${outcome.message}")
            is LastfmOutcome.Error -> {
                Logger.e(TAG, "$method failed (${outcome.code}): ${outcome.message}")
                // A dead session key: the user revoked access on last.fm. Signing out puts the
                // settings back to "connect" instead of failing silently forever.
                if (outcome.needsReauth) dataStoreManager.setLastfmSession(sessionKey = "", username = "")
            }
        }
    }
}

package com.maxrave.data.listentogether

import com.maxrave.common.Config
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.simpmusic.listentogether.DeviceLinkSession
import org.simpmusic.listentogether.LinkConnection
import org.simpmusic.listentogether.LinkEvent
import org.simpmusic.listentogether.LinkSend
import org.simpmusic.listentogether.PlaybackActionPayload
import org.simpmusic.listentogether.PlaybackActions
import org.simpmusic.listentogether.TrackInfo
import kotlin.math.abs
import kotlin.time.TimeSource

private const val TAG = "DeviceLinkBridge"

/**
 * Joins the device link to the local player, next to [ListenTogetherPlaybackBridge] and apart
 * from it: being linked changes nothing about Listen Together.
 *
 * - Publishes what this phone plays (track changes with up to 50 upcoming tracks, play/pause,
 *   seeks) through the send rule, so it only reaches the room while this phone is the player, or
 *   as a takeover when it starts playing. No volume: the desktop can't control the phone.
 * - Pauses when another device takes over. That pause is not published: the send rule drops it,
 *   because this phone is no longer the player.
 * - Continue here: loads the room's track and queue at the live position, playing.
 *
 * Media3 wants the main thread, so every player read and write below is on [Dispatchers.Main].
 */
class DeviceLinkBridge(
    private val session: DeviceLinkSession,
    private val handler: MediaPlayerHandler,
    private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch { publishTrackChanges() }
        scope.launch { publishPlayPause() }
        scope.launch { publishSeeks() }
        scope.launch { followRoom() }
    }

    private val linked get() = session.state.value.connection == LinkConnection.CONNECTED

    /** Play what the player plays, from where it is now, with its upcoming queue. */
    fun continueHere() {
        scope.launch {
            val s = session.state.value
            val track = s.currentTrack?.takeIf { it.id.isNotBlank() } ?: return@launch
            if (s.isPlayer) return@launch
            val tracks = (listOf(track) + s.queue.filter { it.id.isNotBlank() && it.id != track.id }).map { it.toTrack() }
            val position = session.positionNow()
            val clicked = TimeSource.Monotonic.markNow()
            Logger.i(TAG, "Continue here: ${track.id} at $position ms, ${tracks.size - 1} upcoming")
            withContext(Dispatchers.Main) {
                // A list click's path (setQueueData + PLAYLIST_CLICK), so the queue screen, autoplay
                // and what this phone publishes next all see the handed-over queue.
                handler.clearQueued()
                handler.setQueueData(
                    QueueData.Data(
                        listTracks = ArrayList(tracks),
                        firstPlayedTrack = tracks.first(),
                        playlistId = null,
                        playlistName = null,
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = null,
                    ),
                )
                handler.loadMediaItem(tracks.first(), Config.PLAYLIST_CLICK, 0)
                if (position > 0L) handler.player.seekTo(position)
            }
            // The other device kept playing while the stream started here: catch up with it once
            // audio flows, so the handover lands where the music is.
            val audible = withTimeoutOrNull(HANDOFF_WAIT_MS) { while (!onMain { handler.player.isPlaying }) delay(POLL_MS) } != null
            if (audible && position > 0L) {
                val target = position + clicked.elapsedNow().inWholeMilliseconds
                onMain { if (abs(handler.player.currentPosition - target) > CATCH_UP_MS) handler.player.seekTo(target) }
            }
        }
    }

    /**
     * A new track: published once its audio flows (or after [AUDIBLE_WAIT_MS]), so a takeover
     * pauses the other device only when this phone can be heard. A track loaded paused (a restored
     * queue) goes out as change_track(playing=false), which the send rule drops unless this phone
     * is already the player.
     */
    private suspend fun publishTrackChanges() {
        handler.nowPlaying
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.mediaId == new.mediaId }
            // Latest: a skip while the last track is still starting replaces its publish.
            .collectLatest {
                if (!linked) return@collectLatest
                val audible = withTimeoutOrNull(AUDIBLE_WAIT_MS) { while (!onMain { handler.player.isPlaying }) delay(POLL_MS) } != null
                val playing = audible || onMain { handler.player.playWhenReady }
                onMain { changeTrack(playing) }?.let { session.publish(it) }
            }
    }

    private suspend fun publishPlayPause() {
        handler.controlState
            .map { it.isPlaying }
            .distinctUntilChanged()
            .collect { isPlaying ->
                if (!linked) return@collect
                val (intent, position) = onMain { handler.player.playWhenReady to handler.player.currentPosition }
                // Buffering reports isPlaying=false while still meaning to play: not a pause.
                if (isPlaying != intent) return@collect
                val p = PlaybackActionPayload(action = if (intent) PlaybackActions.PLAY else PlaybackActions.PAUSE, position = position)
                if (session.publish(p) == LinkSend.TAKEOVER) {
                    onMain { changeTrack(true) }?.let { session.publish(it) }
                }
            }
    }

    /** A position that moved further than the clock explains is a seek (see the LT bridge). */
    private suspend fun publishSeeks() {
        var lastProgress = 0L
        var lastAt = 0L
        handler.simpleMediaState.collect { mediaState ->
            val progress = (mediaState as? SimpleMediaState.Progress)?.progress ?: return@collect
            val now = PROCESS_START.elapsedNow().inWholeMilliseconds
            val previous = lastProgress
            val previousAt = lastAt
            lastProgress = progress
            lastAt = now
            if (!linked || previousAt == 0L) return@collect
            val playing = onMain { handler.player.isPlaying }
            val expected = previous + if (playing) now - previousAt else 0L
            if (abs(progress - expected) < SEEK_DETECT_MS) return@collect
            session.publish(PlaybackActionPayload(action = PlaybackActions.SEEK, position = progress))
        }
    }

    private suspend fun followRoom() {
        session.events.collect { event ->
            when (event) {
                is LinkEvent.HostChanged ->
                    if (event.hostId != session.state.value.selfId) {
                        onMain { if (handler.player.playWhenReady) handler.player.pause() }
                    }
                // Back in the room while playing: say so, or the room keeps what it last heard.
                LinkEvent.Joined -> onMain { if (handler.player.isPlaying) changeTrack(true) else null }?.let { session.publish(it) }
                LinkEvent.Kicked -> Unit
            }
        }
    }

    /** What plays now and up to 50 upcoming tracks. Call on the main thread. */
    private fun changeTrack(playing: Boolean): PlaybackActionPayload? {
        val item = handler.nowPlaying.value ?: return null
        val id = item.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
        if (id.isBlank()) return null
        val tracks = handler.queueData.value?.data?.listTracks.orEmpty()
        val index = handler.currentOrderIndex()
        val upcoming = if (index in tracks.indices) tracks.drop(index + 1).take(MAX_UPCOMING) else emptyList()
        return PlaybackActionPayload(
            action = PlaybackActions.CHANGE_TRACK,
            trackId = id,
            position = handler.player.currentPosition,
            trackInfo =
                TrackInfo(
                    id = id,
                    title = item.metadata.title.orEmpty(),
                    artist = item.metadata.artist.orEmpty(),
                    album = item.metadata.albumTitle.orEmpty(),
                    duration = handler.player.duration.coerceAtLeast(0L),
                    thumbnail = item.metadata.artworkUri.orEmpty(),
                ),
            queue = upcoming.map { it.toTrackInfo() },
            playing = playing,
        )
    }

    private suspend fun <T> onMain(block: () -> T): T = withContext(Dispatchers.Main) { block() }

    private fun Track.toTrackInfo(): TrackInfo =
        TrackInfo(
            id = videoId,
            title = title,
            artist = artists?.joinToString(", ") { it.name }.orEmpty(),
            album = album?.name.orEmpty(),
            duration = (durationSeconds?.toLong() ?: 0L) * 1000L,
            thumbnail = thumbnails?.lastOrNull()?.url.orEmpty(),
        )

    private fun TrackInfo.toTrack(): Track =
        Track(
            album = null,
            artists = artist.takeIf { it.isNotBlank() }?.let { listOf(Artist(id = null, name = it)) },
            duration = null,
            durationSeconds = (duration / 1000L).toInt().takeIf { it > 0 },
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails = thumbnail.takeIf { it.isNotBlank() }?.let { listOf(Thumbnail(height = 544, url = it, width = 544)) },
            title = title,
            videoId = id,
            videoType = null,
            category = null,
            feedbackTokens = null,
            resultType = null,
        )

    private companion object {
        val PROCESS_START = TimeSource.Monotonic.markNow()
        const val MAX_UPCOMING = 50
        const val POLL_MS = 100L
        const val AUDIBLE_WAIT_MS = 10_000L
        const val HANDOFF_WAIT_MS = 15_000L

        /** Behind the other device by more than this after a handover: seek to catch up. */
        const val CATCH_UP_MS = 1_000L

        /** The LT bridge's threshold: a jump larger than this is a seek. */
        const val SEEK_DETECT_MS = 2_500L
    }
}

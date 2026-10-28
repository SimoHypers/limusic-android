package com.maxrave.data.listentogether

import com.maxrave.common.Config
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.listentogether.DeviceLink
import com.maxrave.domain.data.model.listentogether.LinkRemote
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.NowPlayingTrackState
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.domain.repository.DeviceLinkRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The player the app sees. While this phone is the remote for another linked device
 * ([DeviceLink.controlling]), what plays, the timeline and the queue are that device's, and the
 * controls and song taps go to it, Spotify Connect style: the mini player and the full player are
 * the remote, not a second set of controls. Otherwise every call is [local]'s, untouched.
 *
 * The bridges (Listen Together, the device link's own publishing) and the media session work on
 * [local] directly: they are about what this phone plays.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class LinkAwareMediaPlayerHandler(
    private val local: MediaPlayerHandler,
    private val deviceLink: DeviceLinkRepository,
    private val songRepository: SongRepository,
    private val scope: CoroutineScope,
) : MediaPlayerHandler by local {
    /**
     * The last play/pause or seek sent, shown at once instead of after the round trip. It stands
     * until the device reports anything newer than [reportedAt], or [SENT_TTL_MS] passes.
     */
    private data class Sent(
        val playing: Boolean,
        val positionMs: Long,
        val atMs: Long,
        val reportedAt: Long,
    )

    private val sent = MutableStateFlow<Sent?>(null)
    private var sentExpiry: Job? = null

    /** The device this phone controls, as it last reported, with [sent] laid over it. */
    private val remote: StateFlow<DeviceLink?> =
        combine(deviceLink.link, sent) { link, s ->
            when {
                !link.controlling -> null
                s != null && s.reportedAt == link.updatedAtMs -> link.copy(isPlaying = s.playing, positionMs = s.positionMs, updatedAtMs = s.atMs)
                else -> link
            }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    private val remoteTrack: StateFlow<Track?> =
        remote
            .map { it?.track?.toTrack() }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** The phone's own row for the remote track (its like lives there), or one built from the track. */
    private val remoteSong: StateFlow<SongEntity?> =
        remoteTrack
            .flatMapLatest { track ->
                when {
                    track == null -> flowOf(null)
                    track.isLocalFile() -> flowOf(track.toSongEntity())
                    else ->
                        songRepository.getSongAsFlow(track.videoId).map { row ->
                            row?.copy(thumbnails = track.thumbnails?.lastOrNull()?.url ?: row.thumbnails) ?: track.toSongEntity()
                        }
                }
            }.stateIn(scope, SharingStarted.Eagerly, null)

    override val nowPlaying: StateFlow<GenericMediaItem?> =
        combine(remoteTrack, local.nowPlaying) { track, here -> track?.toGenericMediaItem() ?: here }
            .stateIn(scope, SharingStarted.Eagerly, local.nowPlaying.value)

    override val nowPlayingState: StateFlow<NowPlayingTrackState> =
        combine(remoteTrack, remoteSong, local.nowPlayingState) { track, song, here ->
            if (track == null) {
                here
            } else {
                NowPlayingTrackState(
                    mediaItem = track.toGenericMediaItem(),
                    track = track,
                    songEntity = song?.takeIf { it.videoId == track.videoId } ?: track.toSongEntity(),
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, local.nowPlayingState.value)

    override val controlState: StateFlow<ControlState> =
        combine(remote, remoteSong, local.controlState) { link, song, here ->
            if (link == null) {
                here
            } else {
                here.copy(
                    isPlaying = link.isPlaying,
                    isShuffle = false,
                    repeatState = RepeatState.None,
                    isLiked = song?.liked == true && song.videoId == link.track?.id,
                    isNextAvailable = true,
                    isPreviousAvailable = true,
                    isCrossfading = false,
                    volume = link.volume,
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, local.controlState.value)

    /** The remote position ticks here, extrapolated from the device's last report. */
    override val simpleMediaState: StateFlow<SimpleMediaState> =
        remote
            .map { it != null }
            .distinctUntilChanged()
            .flatMapLatest { controlling ->
                if (!controlling) {
                    local.simpleMediaState
                } else {
                    flow {
                        while (true) {
                            val link = remote.value ?: break
                            emit(SimpleMediaState.Progress(link.positionNow()))
                            delay(if (link.isPlaying) TICK_MS else PAUSED_TICK_MS)
                        }
                    }
                }
            }.stateIn(scope, SharingStarted.Eagerly, local.simpleMediaState.value)

    override val queueData: StateFlow<QueueData?> =
        combine(remote.map { link -> link?.track?.let { it to link.queue } }.distinctUntilChanged(), local.queueData) { remoteQueue, here ->
            if (remoteQueue == null) {
                here
            } else {
                val (current, upcoming) = remoteQueue
                val tracks = (listOf(current) + upcoming).map { it.toTrack() }
                QueueData(
                    queueState = QueueData.StateSource.STATE_INITIALIZED,
                    data = QueueData.Data(listTracks = tracks, firstPlayedTrack = tracks.first(), playlistType = PlaylistType.PLAYLIST),
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, local.queueData.value)

    override val currentSongIndex: StateFlow<Int> =
        combine(remote.map { it != null }.distinctUntilChanged(), local.currentSongIndex) { controlling, here -> if (controlling) 0 else here }
            .stateIn(scope, SharingStarted.Eagerly, local.currentSongIndex.value)

    override val format: StateFlow<NewFormatEntity?> =
        combine(remote.map { it != null }.distinctUntilChanged(), local.format) { controlling, here -> if (controlling) null else here }
            .stateIn(scope, SharingStarted.Eagerly, local.format.value)

    // ponytail: the queue is read-only while remote (the Listen Together guest's gate); editing it on the other device needs protocol actions it doesn't have yet.
    override var followingRoom: Boolean
        get() = remote.value != null || local.followingRoom
        set(value) {
            local.followingRoom = value
        }

    /** What [setQueueData] handed over for the next [loadMediaItem] while remote. */
    private var pendingQueue: QueueData.Data? = null

    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        val link = remote.value ?: return local.onPlayerEvent(playerEvent)
        when (playerEvent) {
            PlayerEvent.PlayPause -> setPlaying(link, !link.isPlaying)
            PlayerEvent.Stop -> setPlaying(link, false)
            PlayerEvent.Next -> deviceLink.remote(LinkRemote.Next)
            PlayerEvent.Previous, PlayerEvent.SkipToPrevious -> deviceLink.remote(LinkRemote.Previous)
            PlayerEvent.Backward -> seekTo(link, link.positionNow() - SKIP_MS)
            PlayerEvent.Forward -> seekTo(link, link.positionNow() + SKIP_MS)
            is PlayerEvent.UpdateProgress -> seekTo(link, (link.durationMs() * playerEvent.newProgress / 100).toLong())
            PlayerEvent.ToggleLike -> toggleLike()
            is PlayerEvent.UpdateVolume -> deviceLink.remote(LinkRemote.SetVolume(playerEvent.newVolume))
            PlayerEvent.Shuffle, PlayerEvent.Repeat -> Unit
        }
    }

    override fun toggleLike() {
        if (remote.value == null) return local.toggleLike()
        val track = remoteTrack.value?.takeIf { !it.isLocalFile() } ?: return
        val liked = remoteSong.value?.takeIf { it.videoId == track.videoId }?.liked == true
        scope.launch {
            // The heart lives on the phone's own row: make sure there is one to set.
            songRepository.insertSong(track.toSongEntity()).singleOrNull()
            if (!songRepository.updateLikeStatus(track.videoId, if (liked) 0 else 1)) showToast(ToastType.YouTubeWriteFailed)
        }
    }

    override fun like(liked: Boolean) {
        if (remote.value == null) local.like(liked)
    }

    override fun setQueueData(queueData: QueueData.Data) {
        if (remote.value != null) pendingQueue = queueData else local.setQueueData(queueData)
    }

    /** A song tapped anywhere in the app plays where the music is. */
    override suspend fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int?,
    ) {
        if (remote.value == null) return local.loadMediaItem(anyTrack, type, index)
        pendingQueue = null
        if (type == Config.RECOVER_TRACK_QUEUE) return
        val track =
            when (anyTrack) {
                is Track -> anyTrack
                is SongEntity -> anyTrack.toTrack()
                else -> return
            }
        // ponytail: one song, which the other device plays with its radio behind it; whole lists wait for a play_tracks action in the protocol.
        deviceLink.remote(LinkRemote.PlayTrack(track.toRoomTrack()))
    }

    override fun playMediaItemInMediaSource(index: Int) {
        if (remote.value == null) local.playMediaItemInMediaSource(index)
    }

    override fun currentOrderIndex(): Int = if (remote.value != null) 0 else local.currentOrderIndex()

    override fun getCurrentMediaItem(): GenericMediaItem? = if (remote.value != null) nowPlaying.value else local.getCurrentMediaItem()

    override fun getPlayerDuration(): Long = remote.value?.durationMs() ?: local.getPlayerDuration()

    override fun getProgress(): Long = remote.value?.positionNow() ?: local.getProgress()

    private fun setPlaying(
        link: DeviceLink,
        playing: Boolean,
    ) {
        deviceLink.remote(if (playing) LinkRemote.Play else LinkRemote.Pause)
        expect(link, playing, link.positionNow())
    }

    private fun seekTo(
        link: DeviceLink,
        positionMs: Long,
    ) {
        val target = positionMs.coerceIn(0L, link.durationMs().takeIf { it > 0L } ?: Long.MAX_VALUE)
        deviceLink.remote(LinkRemote.Seek(target))
        expect(link, link.isPlaying, target)
    }

    private fun expect(
        link: DeviceLink,
        playing: Boolean,
        positionMs: Long,
    ) {
        // Laid over the room as it stands now, which the device's answer replaces.
        val reportedAt = deviceLink.link.value.updatedAtMs
        val s = Sent(playing = playing, positionMs = positionMs, atMs = nowMs(), reportedAt = reportedAt)
        sent.value = s
        sentExpiry?.cancel()
        sentExpiry =
            scope.launch {
                delay(SENT_TTL_MS)
                sent.compareAndSet(s, null)
            }
    }

    private fun DeviceLink.positionNow(): Long = livePositionMs(nowMs())

    private fun DeviceLink.durationMs(): Long = track?.durationMs ?: 0L

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val TICK_MS = 250L

        /** Paused, the position only moves on a seek, which arrives as a new report anyway. */
        const val PAUSED_TICK_MS = 1_000L
        const val SKIP_MS = 10_000L

        /** How long a sent control is shown without the device confirming it. */
        const val SENT_TTL_MS = 3_000L
    }
}

/** A local file on the desktop: no id crosses the wire, only its name. */
private fun Track.isLocalFile() = videoId.startsWith(LOCAL_FILE_ID)

private const val LOCAL_FILE_ID = "link-local:"

internal fun RoomTrack.toTrack(): Track =
    Track(
        album = null,
        artists = artist.takeIf { it.isNotBlank() }?.let { listOf(Artist(id = null, name = it)) },
        duration = null,
        durationSeconds = (durationMs / 1000L).toInt().takeIf { it > 0 },
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = thumbnail.takeIf { it.isNotBlank() }?.let { listOf(Thumbnail(height = 544, url = it.largeCover(), width = 544)) },
        title = title,
        videoId = id.ifBlank { LOCAL_FILE_ID + title },
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
    )

private fun Track.toRoomTrack(): RoomTrack =
    RoomTrack(
        id = videoId,
        title = title,
        artist = artists?.joinToString(", ") { it.name }.orEmpty(),
        album = album?.name.orEmpty(),
        durationMs = (durationSeconds?.toLong() ?: 0L) * 1000L,
        thumbnail = thumbnails?.lastOrNull()?.url.orEmpty(),
    )

/** googleusercontent covers come sized in the URL; the player wants a big one. */
private fun String.largeCover(): String = replace(Regex("=w\\d+-h\\d+"), "=w544-h544")

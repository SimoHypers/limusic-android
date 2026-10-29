package com.maxrave.data.listentogether

import com.maxrave.common.Config
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.listentogether.DeviceLink
import com.maxrave.domain.data.model.listentogether.LinkQueueItem
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
import com.maxrave.domain.repository.ListenTogetherRepository
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
 * controls, queue edits and song taps go to it, Spotify Connect style: the mini player and the
 * full player are the remote, not a second set of controls. Otherwise every call is [local]'s,
 * untouched.
 *
 * The bridges (Listen Together, the device link's own publishing) and the song repository work on
 * [local] directly: they are about what this phone plays.
 *
 * A Listen Together guest owns no queue and no timeline, as on desktop: Play next, Add to queue and
 * a tapped song go to the room's host as suggestions, which the host adds; pause, volume and the
 * like button stay the guest's own; skipping, seeking, shuffle, repeat, queue edits and starting a
 * list say the host controls playback.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class LinkAwareMediaPlayerHandler(
    private val local: MediaPlayerHandler,
    private val deviceLink: DeviceLinkRepository,
    private val listenTogether: ListenTogetherRepository,
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

    // Shuffle and repeat sent, shown until the player's queue says so (or the TTL runs out).
    private val sentShuffle = MutableStateFlow<Boolean?>(null)
    private val sentRepeat = MutableStateFlow<String?>(null)

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

    /**
     * The queue view while remote: the player's own, or until it sends one, the room's track and
     * what follows it. [offset] is where `tracks[0]` is in the player's queue; -1 when unknown,
     * which leaves the rows read-only.
     */
    private data class RemoteQueue(
        val tracks: List<Track>,
        val current: Int,
        val offset: Int,
        val source: String?,
    )

    private val remoteQueue: StateFlow<RemoteQueue?> =
        remote
            .map { link ->
                val q = link?.playerQueue
                when {
                    link == null -> null
                    q != null && q.items.isNotEmpty() -> RemoteQueue(q.items.map { it.toTrack() }, q.current.coerceIn(0, q.items.lastIndex), q.offset, q.source)
                    link.track != null -> RemoteQueue((listOf(link.track!!) + link.queue).map { it.toTrack() }, 0, -1, null)
                    else -> null
                }
            }.distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** The phone's own row for the remote track (its like lives there), or one built from the track. */
    private val remoteSong: StateFlow<SongEntity?> =
        remoteTrack
            .flatMapLatest { track ->
                when {
                    track == null -> flowOf(null)
                    track.isLocalFile() -> flowOf(track.toRemoteSong())
                    else ->
                        songRepository.getSongAsFlow(track.videoId).map { row ->
                            row?.copy(thumbnails = track.cover() ?: row.thumbnails) ?: track.toRemoteSong()
                        }
                }
            }.stateIn(scope, SharingStarted.Eagerly, null)

    override val nowPlaying: StateFlow<GenericMediaItem?> =
        combine(remoteTrack, local.nowPlaying) { track, here -> track?.toRemoteMediaItem() ?: here }
            .stateIn(scope, SharingStarted.Eagerly, local.nowPlaying.value)

    override val nowPlayingState: StateFlow<NowPlayingTrackState> =
        combine(remoteTrack, remoteSong, local.nowPlayingState) { track, song, here ->
            if (track == null) {
                here
            } else {
                NowPlayingTrackState(
                    mediaItem = track.toRemoteMediaItem(),
                    track = track,
                    songEntity = song?.takeIf { it.videoId == track.videoId } ?: track.toRemoteSong(),
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, local.nowPlayingState.value)

    override val controlState: StateFlow<ControlState> =
        combine(remote, remoteSong, local.controlState, sentShuffle, sentRepeat) { link, song, here, shuffle, repeat ->
            if (link == null) {
                here
            } else {
                here.copy(
                    isPlaying = link.isPlaying,
                    isShuffle = shuffle ?: link.playerQueue?.shuffle ?: false,
                    repeatState = (repeat ?: link.playerQueue?.repeat).toRepeatState(),
                    isLiked = song?.liked == true && song.videoId == link.track?.id,
                    isNextAvailable = true,
                    isPreviousAvailable = true,
                    isCrossfading = false,
                    volume = link.volume,
                )
            }
        }.combine(listenTogether.room) { state, room ->
            if (room.inRoom && !room.isHost && remote.value == null) state.copy(isNextAvailable = false, isPreviousAvailable = false) else state
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
        combine(remoteQueue, local.queueData) { q, here ->
            if (q == null) {
                here
            } else {
                QueueData(
                    queueState = QueueData.StateSource.STATE_INITIALIZED,
                    data =
                        QueueData.Data(
                            listTracks = q.tracks,
                            firstPlayedTrack = q.tracks.getOrNull(q.current),
                            playlistName = q.source,
                            playlistType = PlaylistType.PLAYLIST,
                        ),
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, local.queueData.value)

    override val currentSongIndex: StateFlow<Int> =
        combine(remoteQueue, local.currentSongIndex) { q, here -> q?.current ?: here }
            .stateIn(scope, SharingStarted.Eagerly, local.currentSongIndex.value)

    override val format: StateFlow<NewFormatEntity?> =
        combine(remote.map { it != null }.distinctUntilChanged(), local.format) { controlling, here -> if (controlling) null else here }
            .stateIn(scope, SharingStarted.Eagerly, local.format.value)

    /** Rows can be edited once the player has said where they are in its queue. */
    override var followingRoom: Boolean
        get() = remote.value?.let { (remoteQueue.value?.offset ?: -1) < 0 } ?: local.followingRoom
        set(value) {
            local.followingRoom = value
        }

    /** What [setQueueData] handed over for the next [loadMediaItem] while remote. */
    private var pendingQueue: QueueData.Data? = null

    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        val own = playerEvent == PlayerEvent.PlayPause || playerEvent == PlayerEvent.Stop || playerEvent == PlayerEvent.ToggleLike || playerEvent is PlayerEvent.UpdateVolume
        if (!own && hostOnly()) return
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
            PlayerEvent.Shuffle -> {
                val on = !controlState.value.isShuffle
                deviceLink.remote(LinkRemote.SetShuffle(on))
                expectMode(sentShuffle, on)
            }
            PlayerEvent.Repeat -> {
                // The phone's own order: off, all, one.
                val mode =
                    when (controlState.value.repeatState) {
                        RepeatState.None -> "all"
                        RepeatState.All -> "one"
                        RepeatState.One -> "off"
                    }
                deviceLink.remote(LinkRemote.SetRepeat(mode))
                expectMode(sentRepeat, mode)
            }
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
        if (remote.value != null || isGuest()) pendingQueue = queueData else local.setQueueData(queueData)
    }

    override fun toggleRadio() {
        if (!hostOnly()) local.toggleRadio()
    }

    override fun backToPrevious() {
        if (!hostOnly()) local.backToPrevious()
    }

    /** Anything started anywhere in the app plays where the music is: one song, or the list it was tapped in. */
    override suspend fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int?,
    ) {
        if (remote.value == null && !isGuest()) return local.loadMediaItem(anyTrack, type, index)
        val data = pendingQueue
        pendingQueue = null
        if (type == Config.RECOVER_TRACK_QUEUE) return
        val track =
            when (anyTrack) {
                is Track -> anyTrack
                is SongEntity -> anyTrack.toTrack()
                else -> return
            }
        val list = data?.listTracks.orEmpty()
        val single = data == null || type !in LIST_CLICKS || list.size < 2
        if (remote.value == null) {
            // A guest, as on desktop: one song joins the host's queue; a list or a radio is the host's to start.
            if (!single) {
                hostOnly()
            } else if (suggestToHost(listOf(track))) {
                showToast(ToastType.SentToHost)
            }
            return
        }
        if (single) {
            deviceLink.remote(LinkRemote.PlayTrack(track.toRoomTrack()))
            return
        }
        // As the local player picks the tapped row: by id, some pages index a different list.
        val tapped =
            index?.takeIf { list.getOrNull(it)?.videoId == track.videoId }
                ?: list.indexOfFirst { it.videoId == track.videoId }.takeIf { it >= 0 }
                ?: 0
        // ponytail: a window of the list around the tap, under the server's message cap; the rest of a huge playlist doesn't follow.
        val from = (tapped - MAX_LIST / 2).coerceIn(0, (list.size - MAX_LIST).coerceAtLeast(0))
        val window = list.subList(from, (from + MAX_LIST).coerceAtMost(list.size))
        deviceLink.remote(
            LinkRemote.PlayTracks(
                tracks = window.map { it.toRoomTrack() },
                start = if (data.shuffle) null else tapped - from,
                sourceId = data.playlistId?.takeIf { data.playlistType != PlaylistType.LOCAL_PLAYLIST }?.toSourceId(),
                sourceName = data.playlistName,
                shuffle = data.shuffle,
            ),
        )
    }

    override fun playMediaItemInMediaSource(index: Int) {
        if (hostOnly()) return
        if (remote.value == null) return local.playMediaItemInMediaSource(index)
        row(index)?.let { (at, id) -> deviceLink.remote(LinkRemote.PlayIndex(at, id)) }
    }

    override fun removeMediaItem(position: Int) {
        if (hostOnly()) return
        if (remote.value == null) return local.removeMediaItem(position)
        row(position)?.let { (at, id) -> deviceLink.remote(LinkRemote.Remove(at, id)) }
    }

    override fun moveInQueue(
        from: Int,
        to: Int,
    ) {
        if (hostOnly()) return
        if (remote.value == null) return local.moveInQueue(from, to)
        val q = remoteQueue.value?.takeIf { it.offset >= 0 } ?: return
        deviceLink.remote(LinkRemote.Move(q.offset + from, q.offset + to))
    }

    override suspend fun moveItemToPlayNext(position: Int) {
        if (hostOnly()) return
        if (remote.value == null) return local.moveItemToPlayNext(position)
        val q = remoteQueue.value?.takeIf { it.offset >= 0 } ?: return
        deviceLink.remote(LinkRemote.Move(q.offset + position, q.offset + q.current + 1))
    }

    override suspend fun playNext(
        tracks: List<Track>,
        from: String?,
    ) {
        if (suggestToHost(tracks)) return
        if (remote.value == null) return local.playNext(tracks, from)
        deviceLink.remote(LinkRemote.PlayNext(tracks.map { it.toRoomTrack() }, from))
    }

    override suspend fun addToQueue(
        tracks: List<Track>,
        from: String?,
    ) {
        if (suggestToHost(tracks)) return
        if (remote.value == null) return local.addToQueue(tracks, from)
        deviceLink.remote(LinkRemote.AddToQueue(tracks.map { it.toRoomTrack() }, from))
    }

    override fun clearQueued() {
        if (hostOnly()) return
        if (remote.value == null) return local.clearQueued()
        deviceLink.remote(LinkRemote.ClearQueued)
    }

    override fun currentOrderIndex(): Int = remoteQueue.value?.current ?: local.currentOrderIndex()

    override fun getCurrentMediaItem(): GenericMediaItem? = if (remote.value != null) nowPlaying.value else local.getCurrentMediaItem()

    override fun getPlayerDuration(): Long = remote.value?.durationMs() ?: local.getPlayerDuration()

    override fun getProgress(): Long = remote.value?.positionNow() ?: local.getProgress()

    /** True when this phone is a guest in a room, after handing [tracks] to the host. */
    private fun suggestToHost(tracks: List<Track>): Boolean {
        if (!isGuest()) return false
        listenTogether.suggestTracks(tracks.map { it.toRoomTrack() })
        return true
    }

    private fun isGuest(): Boolean = listenTogether.room.value.let { it.inRoom && !it.isHost }

    /** True, after saying so, when this phone plays as a guest: the room's host drives playback. */
    private fun hostOnly(): Boolean {
        if (remote.value != null || !isGuest()) return false
        showToast(ToastType.HostControlsPlayback)
        return true
    }

    /** Queue row [index] of the view, as (index in the player's queue, its track id), when known. */
    private fun row(index: Int): Pair<Int, String>? {
        val q = remoteQueue.value?.takeIf { it.offset >= 0 } ?: return null
        val track = q.tracks.getOrNull(index) ?: return null
        // A local file on the computer crosses the wire without an id: its place names it.
        return q.offset + index to if (track.isLocalFile()) "" else track.videoId
    }

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

    private fun <T> expectMode(
        flow: MutableStateFlow<T?>,
        value: T,
    ) {
        flow.value = value
        scope.launch {
            delay(SENT_TTL_MS)
            flow.compareAndSet(value, null)
        }
    }

    private fun DeviceLink.positionNow(): Long = livePositionMs(nowMs())

    /** What the device announced, or until it knew it, the length its queue gives the playing row. */
    private fun DeviceLink.durationMs(): Long =
        track?.durationMs?.takeIf { it > 0L }
            ?: playerQueue?.let { q -> q.items.getOrNull(q.current) }?.track?.takeIf { it.id == track?.id }?.durationMs
            ?: 0L

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val TICK_MS = 250L

        /** Paused, the position only moves on a seek, which arrives as a new report anyway. */
        const val PAUSED_TICK_MS = 1_000L
        const val SKIP_MS = 10_000L

        /** How long a sent control is shown without the device confirming it. */
        const val SENT_TTL_MS = 3_000L

        /** Tracks of a tapped list sent along; ~250 B each against the server's 256 KiB message cap. */
        const val MAX_LIST = 500

        val LIST_CLICKS = setOf(Config.PLAYLIST_CLICK, Config.ALBUM_CLICK, Config.RADIO_CLICK)
    }
}

/**
 * The remote track as the player shows it: always as artwork. Its video, if it is one, plays on the
 * other device; the phone's own video surface would show an idle player or its last frame.
 */
private fun Track.toRemoteMediaItem(): GenericMediaItem =
    toGenericMediaItem().let { it.copy(metadata = it.metadata.copy(artworkUri = cover() ?: it.metadata.artworkUri, description = MERGING_DATA_TYPE.SONG)) }

/** The phone's row for a remote track, keeping its cover: toSongEntity turns sddefault into maxresdefault, which only HD videos have. */
private fun Track.toRemoteSong(): SongEntity = toSongEntity().copy(thumbnails = cover())

private fun Track.cover(): String? = thumbnails?.lastOrNull()?.url

/** A local file on the desktop: no id crosses the wire, only its name. */
private fun Track.isLocalFile() = videoId.startsWith(LOCAL_FILE_ID)

private const val LOCAL_FILE_ID = "link-local:"

private fun String?.toRepeatState(): RepeatState =
    when (this) {
        "all" -> RepeatState.All
        "one" -> RepeatState.One
        else -> RepeatState.None
    }

/**
 * A page's playlist id as the desktop's autoplay takes it: YouTube's own playlist ids, without the
 * browse prefix. An album's browse id means nothing there, so it goes without one.
 */
private fun String.toSourceId(): String? = removePrefix("VL").takeIf { !it.startsWith("MPRE") && it.isNotBlank() }

private fun LinkQueueItem.toTrack(): Track =
    track.toTrack().copy(queued = queued, queuedEnd = queuedEnd, queuedFrom = queuedFrom, autoplay = autoplay)

internal fun RoomTrack.toTrack(): Track =
    Track(
        album = null,
        artists = artist.takeIf { it.isNotBlank() }?.let { listOf(Artist(id = null, name = it)) },
        duration = null,
        durationSeconds = (durationMs / 1000L).toInt().takeIf { it > 0 },
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = listOf(Thumbnail(height = 544, url = coverUrl(), width = 544)).takeIf { it[0].url.isNotBlank() },
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

/**
 * A cover that loads: googleusercontent ones come sized in the URL and the player wants a big one;
 * with none sent, YouTube's own thumbnail, which every video has (maxresdefault only HD ones do).
 */
private fun RoomTrack.coverUrl(): String =
    when {
        thumbnail.isNotBlank() -> thumbnail.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
        id.isNotBlank() -> "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        else -> ""
    }

package com.simohypers.limusic.data.mediaservice

import com.simohypers.limusic.domain.mediaservice.queue.twinPositionMs
import com.simohypers.limusic.domain.mediaservice.queue.switchVersion
import com.simohypers.limusic.domain.mediaservice.queue.learnTwin
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import com.simohypers.limusic.common.Config.ALBUM_CLICK
import com.simohypers.limusic.common.Config.PLAYLIST_CLICK
import com.simohypers.limusic.common.Config.RADIO_CLICK
import com.simohypers.limusic.common.Config.RECOVER_TRACK_QUEUE
import com.simohypers.limusic.common.LOCAL_PLAYLIST_ID
import com.simohypers.limusic.common.MERGING_DATA_TYPE
import com.simohypers.limusic.data.lastfm.LastfmScrobbler
import com.simohypers.limusic.domain.data.entities.NewFormatEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint
import com.simohypers.limusic.domain.data.player.GenericCastState
import com.simohypers.limusic.domain.data.player.GenericCommandButton
import com.simohypers.limusic.domain.data.player.GenericMediaItem
import com.simohypers.limusic.domain.data.player.GenericPlaybackParameters
import com.simohypers.limusic.domain.data.player.GenericTracks
import com.simohypers.limusic.domain.data.player.PlayerConstants
import com.simohypers.limusic.domain.data.player.PlaybackRefusals
import com.simohypers.limusic.domain.data.player.PlayerError
import com.simohypers.limusic.domain.extension.isVideo
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.extension.toGenericMediaItem
import com.simohypers.limusic.domain.extension.toSongEntity
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.manager.DataStoreManager.Values.FALSE
import com.simohypers.limusic.domain.manager.DataStoreManager.Values.TRUE
import com.simohypers.limusic.domain.mediaservice.handler.ControlState
import com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler
import com.simohypers.limusic.domain.mediaservice.handler.NowPlayingTrackState
import com.simohypers.limusic.domain.mediaservice.handler.PlayerEvent
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.mediaservice.handler.RepeatState
import com.simohypers.limusic.domain.mediaservice.queue.PREVENT_DUPLICATES_KEY
import com.simohypers.limusic.domain.mediaservice.queue.QueueState
import com.simohypers.limusic.domain.mediaservice.queue.SHUFFLE_WHOLE_QUEUE_KEY
import com.simohypers.limusic.domain.mediaservice.queue.STICKY_SHUFFLE_KEY
import com.simohypers.limusic.domain.mediaservice.queue.absorbAutoplay
import com.simohypers.limusic.domain.mediaservice.queue.appendPage
import com.simohypers.limusic.domain.mediaservice.queue.absorbSongRadio
import com.simohypers.limusic.domain.mediaservice.queue.freshen
import com.simohypers.limusic.domain.mediaservice.queue.insertQueued
import com.simohypers.limusic.domain.mediaservice.queue.isMix
import com.simohypers.limusic.domain.mediaservice.queue.move
import com.simohypers.limusic.domain.mediaservice.queue.nextIndex
import com.simohypers.limusic.domain.mediaservice.queue.prevTrackTitle
import com.simohypers.limusic.domain.mediaservice.queue.removeAt
import com.simohypers.limusic.domain.mediaservice.queue.retainUpcoming
import com.simohypers.limusic.domain.mediaservice.queue.spliceRadio
import com.simohypers.limusic.domain.mediaservice.queue.startSong
import com.simohypers.limusic.domain.mediaservice.queue.startTracks
import com.simohypers.limusic.domain.mediaservice.queue.toggleShuffle
import com.simohypers.limusic.domain.utils.isRadioQueueId
import com.simohypers.limusic.domain.mediaservice.handler.SimpleMediaState
import com.simohypers.limusic.domain.mediaservice.handler.SleepTimerState
import com.simohypers.limusic.domain.mediaservice.handler.ToastType
import com.simohypers.limusic.domain.mediaservice.player.MediaPlayerInterface
import com.simohypers.limusic.domain.mediaservice.player.MediaPlayerListener
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.repository.StreamRepository
import com.simohypers.limusic.domain.utils.MusicVideoType
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.discord.DISCORD_CONFIG_KEY
import com.simohypers.limusic.discord.DiscordConfig
import com.simohypers.limusic.discord.DiscordPresence
import com.simohypers.limusic.discord.DiscordTrack
import com.simohypers.limusic.discord.discordAccessToken
import com.simohypers.limusic.lastfm.ScrobbleTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform.getKoin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

private val TAG = "Media3ServiceHandlerImpl"

/** How far into a track Previous stops meaning "go back" and starts meaning "start over". */
private const val PREV_REWIND_MS = 3_000L

/** DataStore key of the saved queue's pointer and source. */
private const val QUEUE_META_KEY = "queue_meta"

internal class MediaServiceHandlerImpl(
    private val dataStoreManager: DataStoreManager,
    private val songRepository: SongRepository,
    private val streamRepository: StreamRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val coroutineScope: CoroutineScope,
) : MediaPlayerHandler,
    MediaPlayerListener {
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val context: Context = getKoin().get()
    override val player: MediaPlayerInterface = getKoin().get()

    // Owns its socket and timing; this file only reports what the player does.
    private val discord = DiscordPresence(backgroundScope) { force -> dataStoreManager.discordAccessToken(force) }

    /**
     * Built here rather than injected: it needs nothing this handler does not already hold, and
     * threading it through [createMediaServiceHandler] would mean changing that expect signature
     * and all three actuals for one dependency.
     */
    private val lastfmScrobbler = LastfmScrobbler(dataStoreManager)
    override var onUpdateNotification: (List<GenericCommandButton>) -> Unit = {}
    override var showToast: (ToastType) -> Unit = {}
    override var pushPlayerError: (PlayerError) -> Unit = {}
    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    override val simpleMediaState: StateFlow<SimpleMediaState> = _simpleMediaState.asStateFlow()

    private val _nowPlaying = MutableStateFlow<GenericMediaItem?>(player.currentMediaItem)
    override val nowPlaying: StateFlow<GenericMediaItem?> = _nowPlaying.asStateFlow()

    private val _queueData =
        MutableStateFlow<QueueData>(
            QueueData(
                queueState = QueueData.StateSource.STATE_CREATED,
                data = QueueData.Data(),
            ),
        )
    override val queueData = _queueData.asStateFlow()

    private val _controlState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = player.isPlaying,
                isShuffle = false,
                repeatState =
                    when (player.repeatMode) {
                        PlayerConstants.REPEAT_MODE_ONE -> {
                            RepeatState.One
                        }

                        PlayerConstants.REPEAT_MODE_ALL -> {
                            RepeatState.All
                        }

                        PlayerConstants.REPEAT_MODE_OFF -> {
                            RepeatState.None
                        }

                        else -> {
                            RepeatState.None
                        }
                    },
                isLiked = false,
                isNextAvailable = player.hasNextMediaItem(),
                // Previous always does something once a track is loaded: past 3s or with nothing before it
                // (shuffle puts the current song first), seekToPrevious restarts the song.
                isPreviousAvailable = player.mediaItemCount > 0,
                isCrossfading = false,
                volume = 1f,
            ),
        )

    override val controlState: StateFlow<ControlState> = _controlState.asStateFlow()

    private val _nowPlayingState = MutableStateFlow<NowPlayingTrackState>(NowPlayingTrackState.initial())
    override val nowPlayingState: StateFlow<NowPlayingTrackState> = _nowPlayingState.asStateFlow()

    private val _sleepTimerState = MutableStateFlow<SleepTimerState>(SleepTimerState(false, 0))
    override val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    private val _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow<NewFormatEntity?>(null)
    override val format: StateFlow<NewFormatEntity?> = _format.asStateFlow()

    private val _currentSongIndex: MutableStateFlow<Int> = MutableStateFlow(player.currentMediaItemIndex)
    override val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _castState = MutableStateFlow(GenericCastState.NOT_CASTING)
    override val castState: StateFlow<GenericCastState> = _castState.asStateFlow()

    // List of Specific variables

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var secondLoudnessEnhancer: LoudnessEnhancer? = null

    private var skipSilent = false

    private var normalizeVolume = false

    /** Written by the equalizer-type collector; read on the player's thread by [shouldOpenOrCloseEqualizerIntent]. */
    @Volatile
    private var systemEqualizerSelected = false

    private var watchTimeList: ArrayList<Float> = arrayListOf()

    private var volumeNormalizationJob: Job? = null

    private var sleepTimerJob: Job? = null

    /** How long the sleep timer spends ramping the volume down before it stops playback. */
    private val sleepFadeDurationMs = 5_000L

    /** Steps in that ramp — 50, matching the crossfade ramp, so 100ms per step at 5 seconds. */
    private val sleepFadeSteps = 50

    /**
     * Silence held after the ramp before playback is actually stopped.
     *
     * The gain is applied in the audio pipeline, ahead of the sink, so it reaches the speaker one
     * buffer late — AudioTrack alone holds 250–750 ms. Pausing the moment the ramp hits zero would
     * therefore cut while the audible level is still around -12 dBFS, which is exactly the abrupt
     * stop this feature exists to remove. Sized above the largest buffer media3 will allocate.
     */
    private val sleepFadeTailMs = 800L


    private var getFormatJob: Job? = null

    private var progressJob: Job? = null

    private var bufferedJob: Job? = null

    private var updateNotificationJob: Job? = null

    private var toggleLikeJob: Job? = null

    private var loadJob: Job? = null
    private val restoreQueueMutex = Mutex()

    // Set by restoreQueueAndPlay before it contends for restoreQueueMutex, so a startup
    // restore already holding the lock can honour the play request itself instead of
    // finishing paused. One-way for the life of the process: mayBeRestoreQueue only ever
    // runs once, from init.
    @Volatile
    private var playRequestedDuringRestore = false

    // Set by restoreSavedQueue once the saved track is in the player - i.e. once a play request
    // can be honoured without the rest of the queue. Written after the addMediaItem that reads
    // [playRequestedDuringRestore] and re-read immediately afterwards, so the restore and a
    // resumption request racing it can never both miss the other's write: whichever runs second
    // sees the first, and exactly one of them starts playback.
    @Volatile
    private var restoreHasPrimedPlayer = false

    private var songEntityJob: Job? = null

    private var jobWatchtime: Job? = null

    private var getDataOfNowPlayingTrackStateJob: Job? = null

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

    // Queue state, declared above init: init starts the collectors and the save loop that use it.

    private val q = QueueState()

    /** Bumped by every load the user starts; a network answer for an older one is dropped. */
    private var generation = 0L

    /** Bumped on every change to the list, so saving rewrites the list only when it moved. */
    private var listVersion = 0

    /** What [setQueueData] handed in, for the [loadMediaItem] that follows it. */
    private var pending: QueueData.Data? = null

    /** The kind of list playing, as the page that started it said. */
    private var playlistType: PlaylistType? = null

    /** Autoplay's fetch is in flight: a second trigger meanwhile would fetch the same page. */
    private var extending = false

    override var followingRoom = false

    // Settings, kept current by collectors in init: read on every queue change, never blocking.
    @Volatile private var autoplay = true

    @Volatile private var stickyShuffle = false

    @Volatile private var shuffleWhole = false

    @Volatile private var preventDuplicates = false

    /** Saves land here and are written off the main thread, the newest only. */
    private val persistQueue = Channel<QueueSnapshot>(Channel.CONFLATED)

    //
    init {
        player.addListener(this)
        progressJob = Job()
        bufferedJob = Job()
        sleepTimerJob = Job()
        volumeNormalizationJob = Job()
        updateNotificationJob = Job()
        toggleLikeJob = Job()
        loadJob = Job()
        songEntityJob = Job()
        getFormatJob = Job()
        jobWatchtime = Job()
        skipSilent = runBlocking { dataStoreManager.skipSilent.first() == TRUE }
        // Collected rather than read once like the settings around it: the equalizer is adjusted
        // while music is playing, and a curve that only takes effect after a restart is useless
        // for judging what you just changed.
        backgroundScope.launch {
            // While the system equalizer is selected the built-in curve goes flat, the same as its own switch being off.
            combine(
                dataStoreManager.equalizerType,
                dataStoreManager.equalizerEnabled,
                dataStoreManager.equalizerBands,
                dataStoreManager.equalizerPreamp,
            ) { type, enabled, bands, preamp -> Triple(enabled == TRUE && type != DataStoreManager.EQUALIZER_TYPE_SYSTEM, bands, preamp) }
                .distinctUntilChanged()
                .collect { (enabled, bands, preamp) ->
                    // Switched off sends a flat curve rather than skipping the call: the filter
                    // has to actually come out of the audio chain, and the stored bands are left
                    // alone so switching back on returns to the user's own shape.
                    player.setEqualizer(
                        bandsDb =
                            if (enabled) bands.split(",").mapNotNull { it.trim().toFloatOrNull() } else emptyList(),
                        preampDb = if (enabled) preamp else 0f,
                    )
                }
        }
        // Switching while music plays must hand over at once, or both equalizers run together: the
        // adapter only asks to open or close when the player's own state changes, not the setting.
        backgroundScope.launch {
            dataStoreManager.equalizerType
                .map { it == DataStoreManager.EQUALIZER_TYPE_SYSTEM }
                .distinctUntilChanged()
                .collect { system ->
                    if (system == systemEqualizerSelected) return@collect
                    systemEqualizerSelected = system
                    // Main, like the adapter's own calls: both read the player's audio session.
                    withContext(Dispatchers.Main) {
                        if (!system) {
                            sendCloseEqualizerIntent()
                        } else if (player.isPlaying) {
                            sendOpenEqualizerIntent()
                        }
                    }
                }
        }
        normalizeVolume =
            runBlocking { dataStoreManager.normalizeVolume.first() == TRUE }
        _nowPlaying.value = player.currentMediaItem
        if (runBlocking { dataStoreManager.saveStateOfPlayback.first() } == TRUE) {
            Logger.d(TAG, "SaveStateOfPlayback TRUE")
            val repeatKey = runBlocking { dataStoreManager.repeatKey.first() }
            Logger.d(TAG, "Repeat: $repeatKey")
            val restoredRepeatMode =
                when (repeatKey) {
                    DataStoreManager.REPEAT_ONE -> PlayerConstants.REPEAT_MODE_ONE
                    DataStoreManager.REPEAT_ALL -> PlayerConstants.REPEAT_MODE_ALL
                    else -> PlayerConstants.REPEAT_MODE_OFF
                }
            // Shuffle comes back with the saved queue: it is part of the queue, not a player mode.
            player.repeatMode = restoredRepeatMode
            // Ensure controlState is in sync after restore, regardless of listener callbacks
            _controlState.value =
                _controlState.value.copy(
                    repeatState =
                        when (restoredRepeatMode) {
                            PlayerConstants.REPEAT_MODE_ONE -> RepeatState.One
                            PlayerConstants.REPEAT_MODE_ALL -> RepeatState.All
                            else -> RepeatState.None
                        },
                )
        }
        backgroundScope.launch {
            dataStoreManager.endlessQueue.map { it == TRUE }.distinctUntilChanged().collectIndexed { i, on ->
                autoplay = on
                if (i > 0) withContext(Dispatchers.Main) { autoplayChanged(on) }
            }
        }
        backgroundScope.launch { dataStoreManager.getString(STICKY_SHUFFLE_KEY).collect { stickyShuffle = it == "true" } }
        backgroundScope.launch { dataStoreManager.getString(SHUFFLE_WHOLE_QUEUE_KEY).collect { shuffleWhole = it == "true" } }
        backgroundScope.launch { dataStoreManager.getString(PREVENT_DUPLICATES_KEY).collect { preventDuplicates = it == "true" } }
        backgroundScope.launch {
            var written = -1
            for (snapshot in persistQueue) {
                runCatching { write(snapshot, list = snapshot.listVersion != written) }
                    .onSuccess { written = snapshot.listVersion }
                    .onFailure { Logger.e(TAG, "Saving the queue failed: ${it.message}") }
            }
        }
        mayBeRestoreQueue()
        coroutineScope.launch {
            val controlStateJob =
                launch {
                    controlState.collectLatest {
                        updateNotification()
                    }
                }
            val playbackJob =
                launch {
                    format.collectLatest { formatTemp ->
                        if (dataStoreManager.sendBackToGoogle.first() == TRUE) {
                            if (formatTemp != null) {
                                println("format in viewModel: $formatTemp")
                                Logger.d(TAG, "Collect format ${formatTemp.videoId}")
                                Logger.d(TAG, "Format expire at ${formatTemp.expiredTime}")
                                Logger.d(TAG, "AtrUrl ${formatTemp.playbackTrackingAtrUrl}")
                                initPlayback(
                                    formatTemp.playbackTrackingVideostatsPlaybackUrl,
                                    formatTemp.playbackTrackingAtrUrl,
                                    formatTemp.playbackTrackingVideostatsWatchtimeUrl,
                                    formatTemp.cpn,
                                )
                            }
                        }
                    }
                }
            val playbackSpeedPitchJob =
                launch {
                    combine(dataStoreManager.playbackSpeed, dataStoreManager.pitch) { speed, pitch ->
                        Pair(speed, pitch)
                    }.collectLatest { pair ->
                        Logger.d(TAG, "Playback speed: ${pair.first}, Pitch: ${pair.second}")
                        player.playbackParameters =
                            GenericPlaybackParameters(
                                pair.first,
                                2f.pow(pair.second.toFloat() / 12),
                            )
                        Logger.d(TAG, "Playback current speed: ${player.playbackParameters.speed}, Pitch: ${player.playbackParameters.pitch}")
                        discord.setSpeed(pair.first)
                    }
                }
            val discordJob =
                launch {
                    combine(
                        dataStoreManager.richPresenceEnabled,
                        dataStoreManager.discordToken,
                    ) { enabled, token -> enabled == TRUE && token.isNotBlank() }
                        .distinctUntilChanged()
                        .collect { discord.setEnabled(it) }
                }
            val discordConfigJob =
                launch {
                    dataStoreManager.getString(DISCORD_CONFIG_KEY).collect { discord.setConfig(DiscordConfig.parse(it)) }
                }
            controlStateJob.join()
            playbackJob.join()
            playbackSpeedPitchJob.join()
            discordJob.join()
            discordConfigJob.join()
        }
    }

    private fun getDataOfNowPlayingState(mediaItem: GenericMediaItem) {
        val videoId =
            if (mediaItem.isVideo()) {
                mediaItem.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
            } else {
                mediaItem.mediaId
            }
        val track =
            queueData.value.data.listTracks
                .find { it.videoId == videoId }
        // Now, not after the database read below: the player can start the next song within
        // milliseconds, and its length and position must not land on the previous track's card.
        discord.setTrack(
            DiscordTrack(
                videoId = videoId,
                title = track?.title ?: mediaItem.metadata.title.orEmpty(),
                artists = track?.artists?.joinToString(", ") { it.name } ?: mediaItem.metadata.artist.orEmpty(),
                artistId = track?.artists?.firstOrNull()?.id,
                album = track?.album?.name ?: mediaItem.metadata.albumTitle,
                albumId = track?.album?.id,
                thumbnail =
                    track?.thumbnails?.lastOrNull()?.url?.let { Regex("=w\\d+-h\\d+").replace(it, "=w544-h544") }
                        ?: mediaItem.metadata.artworkUri,
            ),
        )
        val scrobbleTrack =
            ScrobbleTrack(
                videoId = videoId,
                title = track?.title ?: mediaItem.metadata.title.orEmpty(),
                artists = track?.artists?.map { it.name } ?: listOfNotNull(mediaItem.metadata.artist),
                album = track?.album?.name ?: mediaItem.metadata.albumTitle,
                isVideo = MusicVideoType.isVideoSong(track?.videoType),
            )
        coroutineScope.launch { lastfmScrobbler.onTrackStarted(scrobbleTrack) }
        _nowPlayingState.update {
            it.copy(
                mediaItem = mediaItem,
                track = track,
            )
        }
        _format.value = null
        getDataOfNowPlayingTrackStateJob?.cancel()
        getDataOfNowPlayingTrackStateJob =
            coroutineScope.launch {
                Logger.d(TAG, "getDataOfNowPlayingState: $videoId")
                songRepository.getSongById(videoId).cancellable().singleOrNull().let { songEntity ->
                    if (songEntity != null) {
                        _controlState.update { it.copy(isLiked = songEntity.liked) }
                        var thumbUrl =
                            track?.thumbnails?.lastOrNull()?.url
                                ?: songEntity.thumbnails
                                ?: "http://i.ytimg.com/vi/${songEntity.videoId}/maxresdefault.jpg"
                        Logger.d(TAG, "getDataOfNowPlayingState before: $thumbUrl")
                        thumbUrl = Regex("=w\\d+-h\\d+").replace(thumbUrl, "=w544-h544")
                        Logger.d(TAG, "getDataOfNowPlayingState: $thumbUrl")
                        if (songEntity.thumbnails != thumbUrl) {
                            songRepository.updateThumbnailsSongEntity(thumbUrl, songEntity.videoId).singleOrNull()?.let {
                                Logger.d(TAG, "getDataOfNowPlayingState: Updated thumbs $it")
                            }
                        }
                        // Rows written before the parsers carried YouTube's real MUSIC_VIDEO_TYPE_*
                        // hold an invented label ("Song", "video", a view count). They are corrected
                        // here as the user plays them rather than by a migration. normalize() drops
                        // anything that is not a real type, so an unknown never overwrites a known one.
                        MusicVideoType.normalize(track?.videoType)?.let { freshVideoType ->
                            if (songEntity.videoType != freshVideoType) {
                                songRepository.updateVideoTypeSongEntity(freshVideoType, songEntity.videoId).singleOrNull()?.let {
                                    Logger.d(TAG, "getDataOfNowPlayingState: Updated videoType $it")
                                }
                            }
                        }
                        songRepository.updateSongInLibrary(now(), songEntity.videoId).singleOrNull().let {
                            Logger.d(TAG, "getDataOfNowPlayingState: $it")
                        }
                        songRepository.updateListenCount(songEntity.videoId)
                        _nowPlayingState.update {
                            it.copy(
                                songEntity =
                                    songEntity.copy(
                                        thumbnails = thumbUrl,
                                    ),
                            )
                        }
                        reportTrack(songEntity.copy(thumbnails = thumbUrl), restart = false)
                    } else {
                        _controlState.update { it.copy(isLiked = false) }
                        var thumbUrl =
                            track?.thumbnails?.lastOrNull()?.url
                                ?: "http://i.ytimg.com/vi/${track?.videoId}/maxresdefault.jpg"
                        Logger.d(TAG, "getDataOfNowPlayingState before: $thumbUrl")
                        thumbUrl = Regex("=w\\d+-h\\d+").replace(thumbUrl, "=w544-h544")
                        val songEntity =
                            (track?.toSongEntity() ?: mediaItem.toSongEntity()).copy(
                                thumbnails = thumbUrl,
                            )
                        songRepository
                            .insertSong(
                                songEntity,
                            ).singleOrNull()
                            ?.let {
                                Logger.d(TAG, "getDataOfNowPlayingState: $it")
                            }
                        _nowPlayingState.update {
                            it.copy(
                                songEntity = songEntity,
                            )
                        }
                        reportTrack(songEntity, restart = false)
                    }
                }
                songEntityJob?.cancel()
                songEntityJob =
                    coroutineScope.launch {
                        songRepository.getSongAsFlow(videoId).cancellable().filterNotNull().collectLatest { songEntity ->
                            if (dataStoreManager.explicitContentEnabled.first() == FALSE && songEntity.isExplicit) {
                                showToast(ToastType.ExplicitContent)
                                if (player.hasNextMediaItem()) {
                                    player.seekToNext()
                                } else if (player.hasPreviousMediaItem()) {
                                    player.seekToPrevious()
                                } else {
                                    player.stop()
                                }
                                return@collectLatest
                            }
                            _nowPlayingState.update {
                                it.copy(
                                    songEntity = songEntity,
                                )
                            }
                            _controlState.update {
                                it.copy(
                                    isLiked = songEntity.liked,
                                )
                            }
                        }
                    }
                if (dataStoreManager.sendBackToGoogle.first() == TRUE) {
                    getFormat(videoId)
                }
            }
    }

    private fun getFormat(mediaId: String?) {
        getFormatJob?.cancel()
        getFormatJob =
            coroutineScope.launch {
                if (mediaId != null) {
                    streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Logger.d(TAG, "Get format for $mediaId: itag ${f?.itag}, expires ${f?.expiredTime}")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                    }
                }
            }
    }

    private fun initPlayback(
        playback: String?,
        atr: String?,
        watchTime: String?,
        cpn: String?,
    ) {
        jobWatchtime?.cancel()
        coroutineScope.launch {
            if (playback != null && atr != null && watchTime != null && cpn != null) {
                watchTimeList = arrayListOf()
                streamRepository
                    .initPlayback(playback, atr, watchTime, cpn, queueData.value.data.playlistId)
                    .collect {
                        if (it.first == 204) {
                            Logger.d("Check initPlayback", "Success")
                            watchTimeList.add(0f)
                            watchTimeList.add(5.54f)
                            watchTimeList.add(it.second)
                            updateWatchTime()
                        }
                    }
            }
        }
    }

    private fun updateWatchTime() {
        coroutineScope.launch {
            jobWatchtime =
                launch {
                    simpleMediaState.collect { state ->
                        if (state is SimpleMediaState.Progress) {
                            val value = state.progress
                            if (value > 0 && watchTimeList.isNotEmpty()) {
                                val second = (value / 1000).toFloat()
                                if (second in watchTimeList.last()..watchTimeList.last() + 1.2f) {
                                    val watchTimeUrl =
                                        _format.value?.playbackTrackingVideostatsWatchtimeUrl
                                    val cpn = _format.value?.cpn
                                    if (second + 20.23f < (player.duration / 1000).toFloat()) {
                                        watchTimeList.add(second + 20.23f)
                                        if (watchTimeUrl != null && cpn != null) {
                                            streamRepository
                                                .updateWatchTime(
                                                    watchTimeUrl,
                                                    watchTimeList,
                                                    cpn,
                                                    queueData.value.data.playlistId,
                                                ).collect { response ->
                                                    if (response == 204) {
                                                        Logger.d("Check updateWatchTime", "Success")
                                                    }
                                                }
                                        }
                                    } else {
                                        watchTimeList.clear()
                                        if (watchTimeUrl != null && cpn != null) {
                                            streamRepository
                                                .updateWatchTimeFull(
                                                    watchTimeUrl,
                                                    cpn,
                                                    queueData.value.data.playlistId,
                                                ).collect { response ->
                                                    if (response == 204) {
                                                        Logger.d("Check updateWatchTimeFull", "Success")
                                                    }
                                                }
                                        }
                                    }
                                    Logger.d("Check updateWatchTime", watchTimeList.toString())
                                }
                            }
                        }
                    }
                }
            jobWatchtime?.join()
        }
    }

    private fun sendOpenEqualizerIntent() {
        // No local audio session to expose to an equalizer while casting (or before one exists).
        if (_castState.value.isRemote || player.audioSessionId == PlayerConstants.AUDIO_SESSION_ID_UNSET) return
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun sendCloseEqualizerIntent() {
        if (_castState.value.isRemote || player.audioSessionId == PlayerConstants.AUDIO_SESSION_ID_UNSET) return
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                // Mandatory per AudioEffect's javadoc, and missing from the pre-2.0.0 code this was
                // restored from. AOSP's MusicFX returns early on a null package and closes sessions
                // by package, so without it every CLOSE was ignored — and switching from the system
                // equalizer back to the built-in one left both running until the track ended.
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            },
        )
    }

    @SuppressLint("PrivateResource")
    private fun updateNotification() {
        updateNotificationJob?.cancel()
        updateNotificationJob =
            coroutineScope.launch {
                var id = (player.currentMediaItem?.mediaId ?: "")
                if (id.contains("Video")) {
                    id = id.removePrefix("Video")
                }
                val liked =
                    songRepository
                        .getSongById(id)
                        .singleOrNull()
                        ?.liked ?: false
                Logger.d("Check liked", liked.toString())
                _controlState.value = _controlState.value.copy(isLiked = liked)
                onUpdateNotification.invoke(
                    listOf(
                        GenericCommandButton.Like(liked),
                        GenericCommandButton.Shuffle(isShuffled = _controlState.value.isShuffle),
                        GenericCommandButton.Repeat(repeatState = _controlState.value.repeatState),
                        GenericCommandButton.Radio,
                    ),
                )
            }
    }

    // Region: Override functions
    override fun startProgressUpdate() {
        // Cancel any previous loop first: onIsPlayingChanged(true) can fire repeatedly
        // (crossfade swap, rebuffer→ready, resume-on-focus-gain) and a leaked loop would
        // otherwise multiply both the UI updates and the periodic position writes (#2152).
        progressJob?.cancel()
        progressJob =
            coroutineScope.launch {
                // Persist the playback position to DataStore on this interval so a sudden
                // service/process kill while playing in the background (aggressive OEMs)
                // still restores the correct position instead of restarting the track from
                // the beginning (#2152). The position is otherwise only saved on pause /
                // track change / release, which misses uninterrupted background playback.
                val positionPersistIntervalMs = 5_000L
                // Named so the persist counter below cannot drift out of step with it, which is
                // what a second hardcoded copy of the number did.
                val tickIntervalMs = 50L
                var sinceLastPositionSaveMs = 0L
                var sinceDiscordPositionMs = 0L
                while (true) {
                    delay(tickIntervalMs)
                    _simpleMediaState.value = SimpleMediaState.Progress(player.currentPosition)
                    sinceLastPositionSaveMs += tickIntervalMs
                    if (sinceLastPositionSaveMs >= positionPersistIntervalMs) {
                        sinceLastPositionSaveMs = 0
                        mayBeSaveRecentPosition()
                        // Riding the existing 5s tick instead of adding one: the scrobble point is
                        // half the track or four minutes, so five seconds of granularity is plenty
                        // and the 100ms loop stays as cheap as it was.
                        lastfmScrobbler.onProgress(player.currentPosition)
                    }
                    // Once a second, like desktop: Discord's seek check catches a stall that put
                    // playback behind the bar. Only a drift past 2s pushes anything.
                    sinceDiscordPositionMs += tickIntervalMs
                    if (sinceDiscordPositionMs >= 1_000) {
                        sinceDiscordPositionMs = 0
                        discord.setPosition(player.currentPosition)
                        // The adapter announces READY as it calls play(), before a fresh stream knows
                        // its length, and never again for that track: without this, Discord's card
                        // has no bar and Last.fm falls back to the 4-minute cap until a pause.
                        // Not mid-crossfade, where the length is still the outgoing track's.
                        player.duration.takeIf { it > 0 && !_controlState.value.isCrossfading }?.let {
                            discord.setDuration(it)
                            lastfmScrobbler.onDuration(it)
                        }
                    }
                }
            }
    }

    override fun startBufferedUpdate() {
        // Same reason as startProgressUpdate above: this is reached once per track load and once
        // per stall, and stopBufferedUpdate only cancels the newest job — so every earlier loop
        // survives and keeps pushing Loading every 500 ms for the rest of the session.
        bufferedJob?.cancel()
        bufferedJob =
            coroutineScope.launch {
                while (true) {
                    delay(500)
                    _simpleMediaState.value =
                        SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
                    val current = nowPlayingState.value.songEntity
                    if (current?.durationSeconds == 0 && player.duration > 0L) {
                        _nowPlayingState.update {
                            it.copy(
                                songEntity =
                                    current.copy(
                                        durationSeconds = (player.duration / 1000).toInt(),
                                    ),
                            )
                        }
                    }
                }
            }
    }

    override fun stopProgressUpdate() {
        progressJob?.cancel()
        Logger.d(TAG, "stopProgressUpdate: ${progressJob?.isActive}")
    }

    override fun stopBufferedUpdate() {
        bufferedJob?.cancel()
        // Deliberately emits nothing: this runs when buffering *ends*, so publishing Loading here
        // said the opposite of what happened.
    }

    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.UpdateVolume -> {}

            PlayerEvent.Backward -> {
                player.seekBack()
            }

            PlayerEvent.Forward -> {
                player.seekForward()
            }

            PlayerEvent.PlayPause -> {
                if (player.isPlaying) {
                    player.pause()
                    stopProgressUpdate()
                } else {
                    player.play()
                    startProgressUpdate()
                }
            }

            PlayerEvent.Next -> next()

            PlayerEvent.Previous -> previous(always = false)

            PlayerEvent.SkipToPrevious -> previous(always = true)

            PlayerEvent.Stop -> {
                stopProgressUpdate()
                player.stop()
                _nowPlayingState.value = NowPlayingTrackState.initial()
            }

            is PlayerEvent.UpdateProgress -> {
                player.seekTo((player.duration * playerEvent.newProgress / 100).toLong())
            }

            PlayerEvent.Shuffle -> toggleShuffle()

            PlayerEvent.Repeat -> {
                when (player.repeatMode) {
                    PlayerConstants.REPEAT_MODE_OFF -> {
                        player.repeatMode = PlayerConstants.REPEAT_MODE_ALL
                        _controlState.value = _controlState.value.copy(repeatState = RepeatState.All)
                    }

                    PlayerConstants.REPEAT_MODE_ONE -> {
                        player.repeatMode = PlayerConstants.REPEAT_MODE_OFF
                        _controlState.value = _controlState.value.copy(repeatState = RepeatState.None)
                    }

                    PlayerConstants.REPEAT_MODE_ALL -> {
                        player.repeatMode = PlayerConstants.REPEAT_MODE_ONE
                        _controlState.value = _controlState.value.copy(repeatState = RepeatState.One)
                    }

                    else -> {
                        when (controlState.first().repeatState) {
                            RepeatState.None -> {
                                player.repeatMode = PlayerConstants.REPEAT_MODE_ALL
                                _controlState.value = _controlState.value.copy(repeatState = RepeatState.All)
                            }

                            RepeatState.One -> {
                                player.repeatMode = PlayerConstants.REPEAT_MODE_ALL
                                _controlState.value = _controlState.value.copy(repeatState = RepeatState.All)
                            }

                            RepeatState.All -> {
                                player.repeatMode = PlayerConstants.REPEAT_MODE_ONE
                                _controlState.value = _controlState.value.copy(repeatState = RepeatState.One)
                            }
                        }
                    }
                }
            }

            PlayerEvent.ToggleLike -> {
                toggleLike()
            }
        }
    }

    override fun toggleLike() {
        Logger.d(TAG, "toggleLike: ${nowPlayingState.value.mediaItem.mediaId}")
        toggleLikeJob?.cancel()
        toggleLikeJob =
            coroutineScope.launch {
                var id = (player.currentMediaItem?.mediaId ?: "")
                if (id.contains("Video")) {
                    id = id.removePrefix("Video")
                }
                val written =
                    songRepository.updateLikeStatus(
                        id,
                        if (!(controlState.first().isLiked)) 1 else 0,
                    )
                if (!written) showToast(ToastType.YouTubeWriteFailed)
                delay(200)
            }
    }

    override fun like(liked: Boolean) {
        _controlState.value = _controlState.value.copy(isLiked = liked)
    }

    override fun sleepStart(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob =
            coroutineScope.launch(Dispatchers.Main) {
                var stoppedPlayback = false
                try {
                    if (minutes == Int.MAX_VALUE) {
                        // "End of current song" mode: use sentinel -1 to indicate this special state
                        _sleepTimerState.update {
                            it.copy(isDone = false, timeRemaining = -1)
                        }
                        // Poll until player duration is available (may be -1 initially)
                        var duration = player.duration
                        while (duration <= 0L) {
                            delay(500)
                            duration = player.duration
                        }
                        val remaining = (duration - player.currentPosition).coerceAtLeast(0L)
                        // Fade over the tail of the track rather than after it, so the song is
                        // already silent by the time it ends. A track with less time left than the
                        // fade gets a shorter one instead of bleeding into whatever plays next.
                        // Fade and tail together must fit inside what is left, or the timer would
                        // run past the end of the track and pause somewhere inside the next one.
                        val fadeMs = sleepFadeDurationMs.coerceAtMost(remaining)
                        val tailMs = sleepFadeTailMs.coerceAtMost(remaining - fadeMs)
                        delay(remaining - fadeMs - tailMs)
                        fadeOutForSleep(fadeMs)
                        delay(tailMs)
                        Logger.i("Playback", "sleep timer ran out")
                        player.pause()
                        stoppedPlayback = true
                        _sleepTimerState.update {
                            it.copy(isDone = true, timeRemaining = 0)
                        }
                    } else {
                        _sleepTimerState.update {
                            it.copy(isDone = false, timeRemaining = minutes)
                        }
                        var count = minutes
                        while (count > 0) {
                            // The fade belongs inside the final minute, so shorten that wait by its length.
                            val isFinalMinute = count == 1
                            delay(
                                if (isFinalMinute) 60 * 1000L - sleepFadeDurationMs - sleepFadeTailMs else 60 * 1000L,
                            )
                            if (isFinalMinute) {
                                fadeOutForSleep(sleepFadeDurationMs)
                                delay(sleepFadeTailMs)
                            }
                            count--
                            _sleepTimerState.update {
                                it.copy(isDone = false, timeRemaining = count)
                            }
                        }
                        Logger.i("Playback", "sleep timer ran out")
                        player.pause()
                        stoppedPlayback = true
                        _sleepTimerState.update {
                            it.copy(isDone = true, timeRemaining = 0)
                        }
                    }
                } finally {
                    // Only the cancelled path clears the attenuation here — sleepStop(), or the
                    // scope going away mid-fade. When the timer runs to completion the adapter
                    // clears it instead, from inside the pause it queued, because pause() is
                    // asynchronous and this coroutine cannot tell when playback actually stopped.
                    // Restoring it from here would lift the volume back over the last of the audio.
                    if (!stoppedPlayback) player.sleepFadeFactor = 1f
                }
            }
    }

    /**
     * Ramps [player]'s sleep-fade attenuation down to silence over [durationMs].
     *
     * Uses the same equal-power (cosine) curve as the crossfade ramp: loudness is perceived
     * logarithmically, so a linear ramp sounds like it drops away early and then lingers near the
     * bottom. Leaves the factor at zero: the caller holds that silence for [sleepFadeTailMs] so the
     * attenuated audio can drain through the sink, and only restores the factor after pausing.
     */
    private suspend fun fadeOutForSleep(durationMs: Long) {
        if (durationMs <= 0L) return
        // Fewer steps than the nominal 50 for a very short fade, so the ramp cannot outlast the
        // budget it was given: 50 steps at the 1ms floor would take 50ms regardless of duration.
        val steps = sleepFadeSteps.toLong().coerceAtMost(durationMs).toInt()
        val delayPerStep = (durationMs / steps).coerceAtLeast(1L)
        for (step in 1..steps) {
            val progress = step.toFloat() / steps
            player.sleepFadeFactor = cos(progress * PI / 2).toFloat()
            delay(delayPerStep)
        }
    }

    override fun sleepStop() {
        sleepTimerJob?.cancel()
        _sleepTimerState.value = SleepTimerState(false, 0)
    }

    override fun resetCrossfade() {
        _controlState.update {
            it.copy(
                isCrossfading = false,
            )
        }
    }

    override fun getCurrentMediaItem(): GenericMediaItem? = player.currentMediaItem

    // ========== Queue, ported from desktop Limusic (src-tauri/src/state.rs) ==========
    //
    // [q] is the queue; the player's playlist mirrors it ([sync]). Shuffle reorders [q] itself, so
    // the player's own shuffle mode stays off and there is one index space. All of it runs on Main.

    private fun autoplayOn() = autoplay && !followingRoom

    override fun setQueueData(queueData: QueueData.Data) {
        pending = queueData
    }

    override suspend fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int?,
    ) {
        val track =
            when (anyTrack) {
                is Track -> anyTrack
                is SongEntity -> anyTrack.toTrack()
                else -> return
            }
        Logger.i("Playback", "new queue from \"${track.title}\" (${track.videoId}), $type")
        val data = pending
        pending = null
        if (track.isExplicit && dataStoreManager.explicitContentEnabled.first() == FALSE) {
            showToast(ToastType.ExplicitContent)
            return
        }
        songRepository.insertSong(track.toSongEntity()).singleOrNull()
        track.durationSeconds?.let { songRepository.updateDurationSeconds(it, track.videoId) }
        val play = type != RECOVER_TRACK_QUEUE
        if (data == null || type !in listOf(PLAYLIST_CLICK, ALBUM_CLICK, RADIO_CLICK)) {
            playSong(track, play)
            return
        }
        val list = data.listTracks.ifEmpty { listOf(track) }
        // The clicked track by id: some pages hand an index into a different list than this one.
        val start =
            index?.takeIf { list.getOrNull(it)?.videoId == track.videoId }
                ?: list.indexOfFirst { it.videoId == track.videoId }.takeIf { it >= 0 }
        val seed = data.playlistId
        if (seed != null && data.playlistType == PlaylistType.RADIO && seed.isRadioQueueId()) {
            startRadio(list, start ?: 0, track.videoId, seed, data.playlistName, play)
        } else {
            playTracks(list, if (data.shuffle) null else start, data, play)
        }
    }

    /**
     * A song clicked outside the queue: it replaces the queue (kept for Previous), then its radio is
     * hydrated behind it, unless Autoplay is off, in which case the queue ends with it.
     */
    private suspend fun playSong(
        seed: Track,
        play: Boolean,
    ) {
        val gen = ++generation
        val radio = autoplayOn()
        q.startSong(seed, player.contentPosition, radio, stickyShuffle, gen)
        playlistType = PlaylistType.RADIO
        player.albumTrackIds = emptySet()
        sync(reload = true, playWhenReady = play)
        if (!radio) return
        // The radio playlist itself: a bare next(videoId) answers with the song and a pointer only.
        val page = radioPage(seed.videoId, "RDAMVM${seed.videoId}")
        if (q.hydrating == gen) q.hydrating = null
        if (page == null || generation != gen) return
        q.absorbSongRadio(freshened(page), shuffleWhole)
        sync()
    }

    /**
     * A finite list: a playlist, an album, a library list. The rest of a long playlist is walked in
     * the background ([fill]); when the list runs out, autoplay carries on with its radio.
     */
    private fun playTracks(
        list: List<Track>,
        start: Int?,
        data: QueueData.Data,
        play: Boolean,
        radio: Boolean = false,
    ) {
        generation++
        q.startTracks(list, start, data.playlistId, data.playlistName, data.shuffle, stickyShuffle, shuffleWhole, player.contentPosition)
        q.radio = radio
        playlistType = data.playlistType
        // Crossfade steps aside between an album's own tracks, which a shuffle no longer plays in order.
        player.albumTrackIds =
            if (data.playlistType == PlaylistType.ALBUM && !q.shuffled) list.mapTo(HashSet()) { it.videoId } else emptySet()
        sync(reload = true, playWhenReady = play)
        val source = data.playlistId
        val token = data.continuation
        if (source != null && !token.isNullOrEmpty() && !isMix(source)) fill(q.epoch, source, token, data.nextContinuation)
    }

    /**
     * A radio: YouTube generated the whole list. On the song that is already playing it goes in
     * behind it without a restart. A radio that came back as its seed alone is replaced by the
     * song's automix, the way desktop escalates.
     */
    private suspend fun startRadio(
        list: List<Track>,
        start: Int,
        seedVideoId: String,
        seed: String,
        name: String?,
        play: Boolean,
    ) {
        var page = list
        if (page.size <= 1 && seed.startsWith("RDAMVM")) {
            songRepository
                .getRelatedData(seedVideoId)
                .lastOrNull()
                ?.data
                ?.first
                ?.takeIf { it.isNotEmpty() }
                ?.let { page = page + it }
        }
        val items = page.toMutableList()
        val after = (start + 1).coerceAtMost(items.size)
        items.subList(after, items.size).let { rest -> freshen(rest, heardLately(rest)) }
        if (q.items.getOrNull(q.current)?.videoId == seedVideoId && player.mediaItemCount > 0) {
            if (!stickyShuffle) q.shuffleOrig = null
            q.spliceRadio(items, seed, name)
            playlistType = PlaylistType.RADIO
            sync()
            return
        }
        playTracks(items, start, QueueData.Data(playlistId = seed, playlistName = name, playlistType = PlaylistType.RADIO), play, radio = true)
    }

    override fun switchVersion() {
        val from = q.items.getOrNull(q.current) ?: return
        if (!q.switchVersion()) return
        val position = player.contentPosition
        // Only a track that has really started carries on; at its very start or end the other
        // version starts from the top.
        val resume = player.duration > 0 && position > 1_000 && position < player.duration
        sync(reload = true, startPositionMs = if (resume) from.twinSegments.twinPositionMs(position) else 0L)
    }

    override fun learnTwin(row: Track) {
        if (q.learnTwin(row)) sync()
    }

    /** The notification's Radio button: a radio on the playing song, behind it. */
    override fun toggleRadio() {
        coroutineScope.launch {
            val track = q.items.getOrNull(q.current) ?: return@launch
            val seed = "RDAMVM${track.videoId}"
            val page = radioPage(track.videoId, seed) ?: return@launch
            startRadio(page, page.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0), track.videoId, seed, "${track.title} Radio", true)
        }
    }

    /**
     * Walk the rest of the playing playlist page by page, so a long one starts now instead of after
     * every page has loaded. Skipping around doesn't stop it; another queue taking over does.
     */
    private fun fill(
        epoch: Long,
        playlistId: String,
        first: String,
        fromNext: Boolean,
    ) {
        coroutineScope.launch {
            var token: String = first
            // ponytail: 50 pages, ~5k tracks, like desktop. Raise it if a real playlist hits it.
            repeat(50) {
                val (page, more) = nextPage(playlistId, token, fromNext) ?: return@launch
                if (q.epoch != epoch || page.isEmpty()) return@launch
                q.appendPage(page, playing = true, whole = shuffleWhole)
                sync()
                token = more ?: return@launch
            }
        }
    }

    /** One more page of a playlist, and the token for the one after. */
    private suspend fun nextPage(
        playlistId: String,
        token: String,
        fromNext: Boolean,
    ): Pair<List<Track>, String?>? =
        // A phone playlist queues whole; a token from an older build has nothing to fetch.
        if (playlistId.startsWith(LOCAL_PLAYLIST_ID)) {
            null
        } else {
            songRepository.getContinueTrack(playlistId, token, fromPlaylist = !fromNext).lastOrNull()?.let { (list, more) ->
                list?.let { it to more }
            }
        }

    /** One page of a radio: `next` on [playlistId] around [videoId]. Null when it failed or was empty. */
    private suspend fun radioPage(
        videoId: String,
        playlistId: String,
    ): List<Track>? =
        songRepository
            .getRadioFromEndpoint(YouTubeWatchEndpoint(videoId = videoId, playlistId = playlistId))
            .lastOrNull()
            ?.data
            ?.first
            ?.takeIf { it.isNotEmpty() }

    /** When each of [tracks] was last played, for the ones played in the last 30 days. */
    private suspend fun heardLately(tracks: List<Track>): Map<String, LocalDateTime> {
        if (tracks.isEmpty()) return emptyMap()
        val since = now().date.minus(30, DateTimeUnit.DAY).atTime(0, 0)
        return songRepository
            .getSongsByListVideoId(tracks.map { it.videoId })
            .first()
            .filter { it.inLibrary > since }
            .associate { it.videoId to it.inLibrary }
    }

    private suspend fun freshened(page: List<Track>): List<Track> = page.toMutableList().also { freshen(it, heardLately(it)) }

    /**
     * Autoplay: with two or fewer tracks left, top the queue up from the playlist's radio, or the
     * last track's. Returns how many tracks landed. Nothing under repeat (the queue never ends) or
     * while this queue's own radio is still being fetched.
     */
    private suspend fun extendQueueRadio(gen: Long = generation): Int {
        if (extending || !autoplayOn() || q.hydrating == gen || player.repeatMode != PlayerConstants.REPEAT_MODE_OFF) return 0
        if (q.items.size - q.current > 2) return 0
        val last = q.items.lastOrNull() ?: return 0
        extending = true
        val page =
            try {
                radioPage(last.videoId, q.radioSeed ?: "RDAMVM${last.videoId}")?.let { freshened(it) }
            } finally {
                extending = false
            }
        // Switched off, or another queue started, while it was fetched.
        if (page == null || generation != gen || !autoplayOn()) return 0
        val added = q.absorbAutoplay(page)
        if (added > 0) sync()
        return added
    }

    /** The Autoplay switch: off takes autoplay's upcoming tracks away, on tops a short queue up now. */
    private suspend fun autoplayChanged(on: Boolean) {
        if (on) {
            extendQueueRadio()
        } else if (q.retainUpcoming { !it.autoplay }) {
            sync()
        }
    }

    /** Jump within the queue. [play] null keeps playing or paused as it was. */
    private fun playIndex(
        index: Int,
        play: Boolean? = null,
    ) {
        if (index !in q.items.indices) return
        generation++
        q.seekTo(index)
        player.seekTo(index, 0)
        play?.let { player.playWhenReady = it }
        publish()
        persist(0)
    }

    /** Next: wraps under any repeat (a manual skip leaves repeat-one), and at the end asks autoplay. */
    private suspend fun next() {
        resetCrossfade()
        val repeat = if (player.repeatMode == PlayerConstants.REPEAT_MODE_OFF) RepeatState.None else RepeatState.All
        val i = nextIndex(q.items.size, q.current, repeat)
        if (i != null) {
            playIndex(i)
        } else if (extendQueueRadio() > 0) {
            playIndex(q.current + 1)
        }
    }

    /**
     * Previous: early in the first track of a queue that replaced another, go back to that one;
     * otherwise the player's own rule (restart past 3 s, else the track before).
     */
    private fun previous(always: Boolean) {
        resetCrossfade()
        if (q.current == 0 && q.prev != null && (always || player.currentPosition <= PREV_REWIND_MS)) {
            restorePrev()
        } else if (always) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekToPrevious()
        }
    }

    private fun restorePrev(): Boolean {
        val prev = q.prev ?: return false
        q.prev = null
        generation++
        val (_, position) = q.restoreContext(prev)
        playlistType = null
        player.albumTrackIds = emptySet()
        sync(reload = true, startPositionMs = position)
        return true
    }

    override fun backToPrevious() {
        restorePrev()
    }

    private fun toggleShuffle() {
        q.toggleShuffle(shuffleWhole)
        sync()
        // Un-shuffling can leave nothing upcoming: a full pass drops every heard track.
        coroutineScope.launch { extendQueueRadio() }
    }

    override fun removeMediaItem(position: Int) {
        if (q.removeAt(position)) sync()
    }

    override fun moveInQueue(
        from: Int,
        to: Int,
    ) {
        if (q.move(from, to)) sync()
    }

    override suspend fun moveItemToPlayNext(position: Int) {
        if (position > q.current) {
            moveInQueue(position, q.current + 1)
        } else {
            q.items.getOrNull(position)?.let { playNext(listOf(it)) }
        }
    }

    override fun clearQueued() {
        if (q.retainUpcoming { !it.queued && !it.queuedEnd }) sync()
    }

    override suspend fun playNext(
        tracks: List<Track>,
        from: String?,
    ) = enqueue(tracks, next = true, from = from)

    override suspend fun addToQueue(
        tracks: List<Track>,
        from: String?,
    ) = enqueue(tracks, next = false, from = from)

    private fun enqueue(
        tracks: List<Track>,
        next: Boolean,
        from: String?,
    ) {
        if (tracks.isEmpty()) return
        val wasEmpty = q.items.isEmpty()
        q.insertQueued(tracks, next, from, preventDuplicates, shuffleWhole)
        if (!wasEmpty) {
            sync()
            return
        }
        // Nothing was playing: an add is the whole queue, so start it.
        generation++
        q.current = 0
        q.epoch++
        playlistType = null
        sync(reload = true, playWhenReady = true)
    }

    override fun playMediaItemInMediaSource(index: Int) {
        Logger.i("Playback", "jump to queue item ${index + 1}")
        playIndex(index, play = true)
    }

    override fun currentOrderIndex(): Int = q.current

    override fun playRoomQueue(
        tracks: List<Track>,
        playWhenReady: Boolean,
        positionMs: Long,
    ) {
        if (tracks.isEmpty()) return
        generation++
        resetQueue()
        q.items = tracks.toMutableList()
        playlistType = null
        sync(startPositionMs = positionMs, playWhenReady = playWhenReady)
    }

    override fun resetSongAndQueue() {
        generation++
        resetQueue()
        player.clearMediaItems()
        publish()
    }

    private fun resetQueue() {
        q.items = mutableListOf()
        q.current = 0
        q.shuffleOrig = null
        q.radioSeed = null
        q.sourceName = null
        q.sourceId = null
        q.radio = false
        q.hydrating = null
        q.prev = null
        q.epoch++
    }

    /**
     * Mirror the queue into the player, publish it, save it. [reload] starts the current track from
     * [startPositionMs] (a new queue); without it the playing track carries on and only the list
     * around it changes.
     */
    private fun sync(
        reload: Boolean = false,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean? = null,
    ) {
        listVersion++
        if (q.items.isEmpty()) {
            player.clearMediaItems()
        } else {
            player.setQueue(q.items.map { it.media() }, q.current, startPositionMs, reload, playWhenReady)
        }
        publish()
        persist(if (reload) startPositionMs else player.contentPosition)
    }

    /** A room's guests all play audio: never let the artwork guess turn a track into a video. */
    private fun Track.media(): GenericMediaItem =
        toGenericMediaItem().let {
            if (followingRoom) it.copy(metadata = it.metadata.copy(description = MERGING_DATA_TYPE.SONG)) else it
        }

    private fun publish() {
        _queueData.value =
            QueueData(
                queueState = QueueData.StateSource.STATE_INITIALIZED,
                data =
                    QueueData.Data(
                        listTracks = q.items.toList(),
                        playlistId = q.sourceId,
                        playlistName = q.sourceName,
                        playlistType = playlistType,
                        prevTrack = q.prevTrackTitle(),
                    ),
            )
        _currentSongIndex.value = q.current
        _controlState.update {
            it.copy(
                isShuffle = q.shuffled,
                isNextAvailable = player.repeatMode != PlayerConstants.REPEAT_MODE_OFF || q.current + 1 < q.items.size || autoplayOn(),
                // Previous always does something once a track is loaded: restart, or go back.
                isPreviousAvailable = q.items.isNotEmpty(),
            )
        }
    }

    @Serializable
    private data class QueueMeta(
        val current: Int,
        val sourceId: String? = null,
        val sourceName: String? = null,
        val radioSeed: String? = null,
        val radio: Boolean = false,
        val type: String? = null,
        /** The pre-shuffle order as video ids, while shuffled. */
        val shuffle: List<String>? = null,
    )

    private class QueueSnapshot(
        val items: List<Track>,
        val listVersion: Int,
        val meta: QueueMeta,
        val positionMs: Long,
    )

    private fun snapshot(positionMs: Long) =
        QueueSnapshot(
            items = q.items.toList(),
            listVersion = listVersion,
            meta =
                QueueMeta(
                    current = q.current,
                    sourceId = q.sourceId,
                    sourceName = q.sourceName,
                    radioSeed = q.radioSeed,
                    radio = q.radio,
                    type = playlistType?.name,
                    shuffle = q.shuffleOrig?.map { it.videoId },
                ),
            positionMs = positionMs,
        )

    private fun persist(positionMs: Long) {
        if (q.items.isNotEmpty() && !followingRoom) persistQueue.trySend(snapshot(positionMs))
    }

    /** Writes a saved queue: the pointer and where it came from always, the list only when it moved. */
    private suspend fun write(
        s: QueueSnapshot,
        list: Boolean,
    ) {
        val playing = s.items.getOrNull(s.meta.current) ?: return
        dataStoreManager.saveRecentSong(playing.videoId, s.positionMs)
        dataStoreManager.putString(QUEUE_META_KEY, json.encodeToString(s.meta))
        dataStoreManager.setPlaylistFromSaved(s.meta.sourceName.orEmpty())
        if (list) songRepository.recoverQueue(s.items)
    }

    override fun mayBeSaveRecentSong(runBlocking: Boolean) {
        if (q.items.isEmpty() || followingRoom) return
        if (runBlocking) runBlocking { write(snapshot(player.contentPosition), list = true) } else persist(player.contentPosition)
    }

    /** Puts the saved queue back, as it was left. See [restoreQueueAndPlay] for who calls it how. */
    private suspend fun restoreSavedQueue(playWhenReady: Boolean): Boolean =
        restoreQueueMutex.withLock {
            if (player.mediaItemCount > 0) {
                // Already restored, or the app was running all along.
                if (playWhenReady || playRequestedDuringRestore) requestPlayWhenReady()
                return@withLock true
            }
            val items =
                songRepository
                    .getSavedQueue()
                    .singleOrNull()
                    ?.firstOrNull()
                    ?.listTrack
                    .orEmpty()
            if (items.isEmpty()) return@withLock false
            val meta =
                dataStoreManager.getString(QUEUE_META_KEY).first()?.let {
                    runCatching { json.decodeFromString<QueueMeta>(it) }.getOrNull()
                }
            val recentId = dataStoreManager.recentMediaId.first()
            val current =
                meta?.current?.takeIf { items.getOrNull(it)?.videoId == recentId }
                    ?: items.indexOfFirst { it.videoId == recentId }.takeIf { it >= 0 }
                    ?: 0
            val position = if (items[current].videoId == recentId) dataStoreManager.recentPosition.first().toLongOrNull() ?: 0L else 0L
            resetQueue()
            q.items = items.toMutableList()
            q.current = current
            q.sourceId = meta?.sourceId
            q.sourceName = meta?.sourceName ?: dataStoreManager.playlistFromSaved.first().ifEmpty { null }
            q.radioSeed = meta?.radioSeed
            q.radio = meta?.radio == true
            q.shuffleOrig =
                meta?.shuffle?.let { ids ->
                    val byId = items.associateBy { it.videoId }
                    ids.mapNotNullTo(mutableListOf()) { byId[it] }
                }
            playlistType = meta?.type?.let { runCatching { PlaylistType.valueOf(it) }.getOrNull() }
            player.setQueue(items.map { it.media() }, current, position, reload = true, playWhenReady = playWhenReady || playRequestedDuringRestore)
            // From here the track is in the player, so restoreQueueAndPlay can start it itself.
            restoreHasPrimedPlayer = true
            publish()
            // Nothing plays after a startup restore, so no progress tick would show where it was.
            _simpleMediaState.value = SimpleMediaState.Progress(position)
            if (playWhenReady || playRequestedDuringRestore) requestPlayWhenReady()
            true
        }

    override fun getPlayerDuration(): Long = player.duration

    override fun getProgress(): Long = player.currentPosition

    /**
     * Lightweight periodic persistence of just the playback position (#2152).
     * Unlike [mayBeSaveRecentSong] this does NOT rewrite the whole saved queue, so it is
     * cheap enough to call every few seconds while a track plays uninterrupted. The saved
     * media id + position are what [mayBeRestoreQueue] reads to resume after a process kill.
     */
    private fun mayBeSaveRecentPosition() {
        coroutineScope.launch {
            val videoId = nowPlayingState.value.songEntity?.videoId ?: return@launch
            dataStoreManager.saveRecentSong(videoId, player.contentPosition)
        }
    }

    override fun mayBeNormalizeVolume() {
        runBlocking {
            normalizeVolume = dataStoreManager.normalizeVolume.first() == TRUE
        }
        if (!normalizeVolume) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            player.volume = 1f
            return
        }

        // Always recreate LoudnessEnhancer: CrossfadeExoPlayerAdapter creates a new ExoPlayer per
        // track, and they share one audio session only when the system handed one out. Otherwise
        // each has its own, and an old LoudnessEnhancer sits on a released session with no effect.
        // Skip entirely while casting: a Cast session has no local audio session, and
        // constructing a LoudnessEnhancer with session id 0 (AUDIO_SESSION_ID_UNSET) throws.
        if (!_castState.value.isRemote && player.audioSessionId != PlayerConstants.AUDIO_SESSION_ID_UNSET) {
            try {
                loudnessEnhancer?.release()
            } catch (_: Exception) {
            }
            try {
                loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
            } catch (e: Exception) {
                Logger.e(TAG, "mayBeNormalizeVolume: ${e.message}")
            }
        }

        player.currentMediaItem?.mediaId?.let { songId ->
            val videoId =
                if (songId.contains("Video")) {
                    songId.removePrefix("Video")
                } else {
                    songId
                }
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob =
                coroutineScope.launch(Dispatchers.Main) {
                    fun Float?.toMb() = ((this ?: 0f) * 100).toInt()
                    streamRepository
                        .getFormatFlow(videoId)
                        .cancellable()
                        .distinctUntilChanged()
                        .collectLatest { format ->
                            if (format != null) {
                                val loudnessMb =
                                    format.loudnessDb.toMb().let {
                                        if (it !in -2000..2000) {
                                            0
                                        } else {
                                            it
                                        }
                                    }
                                Logger.d(TAG, "Loudness: ${format.loudnessDb} db, $loudnessMb")
                                try {
                                    loudnessEnhancer?.setTargetGain(0f.toMb() - loudnessMb)
                                    loudnessEnhancer?.enabled = true
                                    Logger.d(
                                        TAG,
                                        "mayBeNormalizeVolume: ${loudnessEnhancer?.targetGain}",
                                    )
                                } catch (e: Exception) {
                                    Logger.e(TAG, "mayBeNormalizeVolume: ${e.message}")
                                }
                                try {
                                    secondLoudnessEnhancer?.setTargetGain(0f.toMb() - loudnessMb)
                                    secondLoudnessEnhancer?.enabled = true
                                    Logger.d(
                                        TAG,
                                        "mayBeNormalizeVolume: ${secondLoudnessEnhancer?.targetGain}",
                                    )
                                } catch (e: Exception) {
                                    Logger.e(TAG, "mayBeNormalizeVolume: ${e.message}")
                                }
                            }
                        }
                }
        }
    }

    override fun mayBeSavePlaybackState() {
        if (runBlocking { dataStoreManager.saveStateOfPlayback.first() } == TRUE) {
            runBlocking {
                dataStoreManager.recoverShuffleAndRepeatKey(
                    q.shuffled,
                    player.repeatMode,
                )
            }
        }
    }

    override fun mayBeRestoreQueue() {
        coroutineScope.launch { restoreSavedQueue(playWhenReady = false) }
    }

    override suspend fun restoreQueueAndPlay(): Boolean {
        playRequestedDuringRestore = true
        // A restore that is already past its addMediaItem has the saved track in the player, so
        // the request is served right here instead of queueing behind restoreQueueMutex. That
        // restore is sitting on loadJob.join(), and the load it joins paces itself in chunks and
        // can reach for the network, so waiting for the lock would put the whole queue in front
        // of the first note - and in front of the BUFFERING/READY that lets Media3 call
        // startForeground() before the system's deadline. Falling through is still correct when
        // the player has not been primed yet: the restore has not read the flag either, so it
        // starts the track playing itself.
        if (restoreHasPrimedPlayer && player.mediaItemCount > 0) {
            requestPlayWhenReady()
            return true
        }
        return restoreSavedQueue(playWhenReady = true)
    }

    /**
     * Asks the player to start, in a way that survives the track still being mid-load.
     *
     * [MediaPlayerInterface.play] alone does not. The adapter answers it out of its own state
     * machine and drops it outright while the track is still being set up, and a load that was
     * started with "do not play" ends by pausing whatever intent arrived during it. Writing
     * `playWhenReady` records the intent synchronously instead, and both the load and the READY
     * transition it ends on honour it - which is the whole point on the resumption path, where
     * the request routinely lands on a track the restore only just handed to the player.
     */
    private fun requestPlayWhenReady() {
        player.playWhenReady = true
    }

    override fun shouldReleaseOnTaskRemoved() =
        runBlocking {
            dataStoreManager.killServiceOnExit.first() == TRUE
        }

    override fun release() {
        Logger.d("ServiceHandler", "Starting release process")
        try {
            // Save state first
            mayBeSaveRecentSong(true)
            mayBeSavePlaybackState()

            // Stop and release player
            player.removeListener(this)

            // Release audio effects
            try {
                loudnessEnhancer?.enabled = false
                loudnessEnhancer?.release()
                loudnessEnhancer = null

                secondLoudnessEnhancer?.enabled = false
                secondLoudnessEnhancer?.release()
                secondLoudnessEnhancer = null
            } catch (e: Exception) {
                Logger.e("ServiceHandler", "Error releasing audio effects ${e.message}")
            }

            // Send close equalizer intent
            sendCloseEqualizerIntent()

            // Cancel all jobs
            progressJob?.cancel()
            progressJob = null
            bufferedJob?.cancel()
            bufferedJob = null
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = null
            toggleLikeJob?.cancel()
            toggleLikeJob = null
            updateNotificationJob?.cancel()
            updateNotificationJob = null
            loadJob?.cancel()
            loadJob = null
            songEntityJob?.cancel()
            songEntityJob = null
            getFormatJob?.cancel()
            getFormatJob = null
            jobWatchtime?.cancel()
            jobWatchtime = null
            getDataOfNowPlayingTrackStateJob?.cancel()
            getDataOfNowPlayingTrackStateJob = null
            // Cancel coroutine scope (cancelling backgroundScope also takes the Discord card down)
            coroutineScope.cancel()
            backgroundScope.cancel()

            Logger.d("ServiceHandler", "Handler released successfully. Scope active: ${coroutineScope.isActive}")
        } catch (e: Exception) {
            Logger.e("ServiceHandler", "Error during release ${e.message}")
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        val loaded =
            player.bufferedPosition.let {
                if (it > 0) {
                    it
                } else {
                    0
                }
            }
        val current =
            player.currentPosition.let {
                if (it > 0) {
                    it
                } else {
                    0
                }
            }
        // Covers what onIsPlayingChanged misses: loading the next track, a pause while loading.
        reportDiscordPlaying()
        when (playbackState) {
            PlayerConstants.STATE_IDLE -> {
                _simpleMediaState.value = SimpleMediaState.Initial
                Logger.d(TAG, "onPlaybackStateChanged: Idle")
            }

            PlayerConstants.STATE_ENDED -> {
                _simpleMediaState.value = SimpleMediaState.Ended
                Logger.d(TAG, "onPlaybackStateChanged: Ended")
                // Out of tracks before autoplay's early top-up landed: one more try, then it stops.
                coroutineScope.launch { if (extendQueueRadio() > 0) playIndex(q.current + 1, play = true) }
            }

            PlayerConstants.STATE_READY -> {
                Logger.d(TAG, "onPlaybackStateChanged: Ready")
                _simpleMediaState.value = SimpleMediaState.Ready(player.duration)
                if (player.duration > 0) {
                    discord.setDuration(player.duration)
                    val ms = player.duration
                    coroutineScope.launch { lastfmScrobbler.onDuration(ms) }
                }
            }

            else -> {
                if (current >= loaded) {
                    _simpleMediaState.value = SimpleMediaState.Buffering(player.currentPosition)
                    Logger.d(TAG, "onPlaybackStateChanged: Buffering")
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _controlState.value = _controlState.value.copy(isPlaying = isPlaying)
        discord.setPosition(player.currentPosition)
        reportDiscordPlaying()
        if (isPlaying) {
            startProgressUpdate()
        } else {
            stopProgressUpdate()
            mayBeSaveRecentSong()
            mayBeSavePlaybackState()
        }
    }

    override fun onSeeked(positionMs: Long) {
        discord.setPosition(positionMs)
        if (!player.isPlaying) {
            // Paused, the progress ticker is stopped, so nothing else tells the UI the position
            // moved: every seek bar, clock and lyric line would wait for play to catch up (#2480).
            _simpleMediaState.value = SimpleMediaState.Progress(positionMs)
        }
    }

    override fun onMediaItemTransition(
        mediaItem: GenericMediaItem?,
        reason: Int,
    ) {
        Logger.d(TAG, "Checking current state before transition ${simpleMediaState.value}")
        Logger.d(TAG, "Smooth Switching Transition Current Position: ${player.currentPosition}")
        mayBeNormalizeVolume()
        Logger.d(TAG, "REASON onMediaItemTransition: $reason")
        Logger.d(TAG, "Media Item Transition Media Item: ${mediaItem?.metadata?.title}")
        if (mediaItem?.mediaId != _nowPlaying.value?.mediaId) {
            _nowPlaying.value = mediaItem
            // No reason: the adapter reports every change as AUTO. The line before this one says why.
            Logger.i("Playback", "now playing \"${mediaItem?.metadata?.title}\" (${mediaItem?.mediaId})")
        }
        if (mediaItem?.mediaId != nowPlayingState.value.mediaItem.mediaId) {
            Logger.d(TAG, "onMediaItemTransition: ${mediaItem?.mediaId}")
            if (mediaItem != null) {
                getDataOfNowPlayingState(mediaItem)
            } else {
                _nowPlayingState.update {
                    NowPlayingTrackState
                        .initial()
                }
            }
        } else if (mediaItem != null) {
            // Repeat-one replays the same mediaId without reloading now-playing data: tell Discord the
            // track started over, or its bar keeps the previous play's timeline.
            nowPlayingState.value.songEntity?.let { reportTrack(it, restart = true) }
        }
        // The player moved on by itself: a track ended, a crossfade, the notification's buttons.
        val index = player.currentMediaItemIndex
        if (index != q.current && q.items.getOrNull(index)?.videoId == mediaItem?.mediaId) {
            q.seekTo(index)
            persist(0)
        }
        publish()
        updateNotification()
        if (q.current == 0) {
            resetCrossfade()
        }
        // Autoplay's early trigger: tops the queue up while two or fewer tracks are left.
        coroutineScope.launch { extendQueueRadio() }
    }

    /** The database's view of the track: a restart (repeat-one) or fuller data for the one playing. */
    private fun reportTrack(
        song: SongEntity,
        restart: Boolean,
    ) {
        val scrobbleTrack =
            ScrobbleTrack(
                videoId = song.videoId,
                title = song.title,
                artists = song.artistName.orEmpty(),
                album = song.albumName,
                isVideo = MusicVideoType.isVideoSong(song.videoType),
            )
        // Launched: now-playing is a network round trip. Main is a single queue, so these keep their
        // order with the start and the length reported around them.
        coroutineScope.launch {
            if (restart) lastfmScrobbler.onTrackStarted(scrobbleTrack, restart = true) else lastfmScrobbler.updateTrack(scrobbleTrack)
        }
        val track =
            DiscordTrack(
                videoId = song.videoId,
                title = song.title,
                artists = song.artistName?.joinToString(", ").orEmpty(),
                artistId = song.artistId?.firstOrNull(),
                album = song.albumName,
                albumId = song.albumId,
                thumbnail = song.thumbnails,
            )
        if (restart) discord.setTrack(track) else discord.updateTrack(track)
    }

    /**
     * The player's isPlaying already ignores a mid-track rebuffer (it stays PLAYING), so it is the
     * clock. A track loading after a skip or an advance reads as not playing but still wants its card.
     * playWhenReady can't be the flag: the player updates it after telling listeners about a pause.
     */
    private fun reportDiscordPlaying() {
        val moving = player.isPlaying
        val loading = player.playbackState == PlayerConstants.STATE_BUFFERING && player.playWhenReady
        discord.setPlaying(moving || loading, moving)
    }

    override fun onTracksChanged(tracks: GenericTracks) {
        Logger.d(TAG, "onTracksChanged: ${tracks.groups.size}")
    }

    override fun onPlayerError(error: PlayerError) {
        when (error.errorCode) {
            PlayerConstants.ERROR_CODE_TIMEOUT -> {
                Logger.e("Player Error", "onPlayerError (${error.errorCode}): ${error.message}")
                if (isAppInForeground()) {
                    showToast(ToastType.PlayerError(error.errorCodeName))
                } else {
                    Logger.d("Player Error", "App is not in foreground, skipping toast")
                }
                player.pause()
            }

            else -> {
                Logger.e("Player Error", "onPlayerError (${error.errorCode}): ${error.message}")
                pushPlayerError(error)
                // YouTube refused this song (taken down, region locked): skip it like desktop does.
                // Not on a bot check, which refuses every song (it is what blocked holds); not past the
                // last track, where repeat-all would spin on a queue that never plays; not as a room
                // guest, whose host decides what plays.
                val refusal = error.refusal
                val skipped = q.items.getOrNull(q.current)
                if (refusal != null && refusal != PlaybackRefusals.blocked.value && !followingRoom &&
                    skipped != null && q.current + 1 < q.items.size
                ) {
                    Logger.w("Player Error", "Skipping ${skipped.videoId}: $refusal")
                    if (isAppInForeground()) showToast(ToastType.PlaybackSkipped(skipped.title, refusal))
                    playIndex(q.current + 1)
                    return
                }
                if (isAppInForeground()) {
                    showToast(error.refusal?.let(ToastType::PlaybackRefused) ?: ToastType.PlayerError(error.errorCodeName))
                } else {
                    Logger.d("Player Error", "App is not in foreground, skipping toast")
                }
                player.pause()
            }
        }
    }

    override fun shouldOpenOrCloseEqualizerIntent(shouldOpen: Boolean) {
        // Built-in selected: the session is never handed to a system equalizer, so the two cannot run together.
        if (!systemEqualizerSelected) return
        if (shouldOpen) sendOpenEqualizerIntent() else sendCloseEqualizerIntent()
    }

    override fun onShuffleModeEnabledChanged(
        shuffleModeEnabled: Boolean,
        list: List<GenericMediaItem>,
    ) {
        // The player's own shuffle stays off: shuffle reorders the queue itself (toggleShuffle).
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        when (repeatMode) {
            PlayerConstants.REPEAT_MODE_OFF -> {
                _controlState.value =
                    _controlState.value.copy(repeatState = RepeatState.None)
            }

            PlayerConstants.REPEAT_MODE_ONE -> {
                _controlState.value =
                    _controlState.value.copy(repeatState = RepeatState.One)
            }

            PlayerConstants.REPEAT_MODE_ALL -> {
                _controlState.value =
                    _controlState.value.copy(repeatState = RepeatState.All)
            }
        }
        publish()
        // Repeat off again with the end in sight: autoplay may have something to add now.
        coroutineScope.launch { extendQueueRadio() }
    }

    /**
     * Publishes exactly one state, and it matches the argument.
     *
     * It used to write Loading unconditionally and then call stopBufferedUpdate, which wrote
     * Loading again. `_simpleMediaState` is a StateFlow collected from another thread, so those
     * writes conflate and the UI settled on Loading even when buffering had just *finished* —
     * leaving a spinner over playback that was already running.
     */
    override fun onIsLoadingChanged(isLoading: Boolean) {
        if (isLoading) {
            startBufferedUpdate()
            // Already holding more than the playhead needs: the stall is nominal, so do not put a
            // spinner over playback that is about to continue.
            if (player.bufferedPosition > player.currentPosition) {
                _simpleMediaState.value = SimpleMediaState.Ready(player.duration)
            } else {
                _simpleMediaState.value =
                    SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
            }
        } else {
            stopBufferedUpdate()
            _simpleMediaState.value = SimpleMediaState.Ready(player.duration)
        }
    }

    override fun onCrossfadeStateChanged(isCrossfading: Boolean) {
        _controlState.update {
            it.copy(isCrossfading = isCrossfading)
        }
    }

    override fun onTimelineChanged(
        list: List<GenericMediaItem>,
        reason: String,
    ) {
        super.onTimelineChanged(list, reason)
        Logger.d(TAG, "onTimelineChanged: Reason: $reason, Items: ${list.size}")
    }

    override fun onCastStateChanged(castState: GenericCastState) {
        _castState.value = castState
    }

}

private fun isAppInForeground(): Boolean {
    val appProcessInfo = RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(appProcessInfo)
    return appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
}
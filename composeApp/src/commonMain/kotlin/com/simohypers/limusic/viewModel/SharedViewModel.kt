package com.simohypers.limusic.viewModel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.AppIdentity
import com.simohypers.limusic.common.Config.DOWNLOAD_CACHE
import com.simohypers.limusic.common.Config.PLAYLIST_CLICK
import com.simohypers.limusic.common.Config.SONG_CLICK
import com.simohypers.limusic.common.LibraryChipType
import com.simohypers.limusic.common.SELECTED_LANGUAGE
import com.simohypers.limusic.common.STATUS_DONE
import com.simohypers.limusic.domain.data.entities.DownloadState
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.LyricsEntity
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.type.PlaylistType as LibraryPlaylist
import com.simohypers.limusic.domain.data.model.lyrics.LyricsSource
import com.simohypers.limusic.domain.data.entities.NewFormatEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.entities.SongInfoEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.canvas.CanvasResult
import com.simohypers.limusic.domain.data.model.download.DownloadProgress
import com.simohypers.limusic.domain.data.model.intent.GenericIntent
import com.simohypers.limusic.domain.data.model.metadata.Lyrics
import com.simohypers.limusic.domain.data.model.streams.TimeLine
import com.simohypers.limusic.domain.data.player.GenericCastState
import com.simohypers.limusic.domain.data.player.LiveStreamRegistry
import com.simohypers.limusic.domain.extension.decodeHtmlEntities
import com.simohypers.limusic.domain.extension.isSong
import com.simohypers.limusic.domain.extension.isVideo
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.data.model.lyrics.RomanizationLanguage
import com.simohypers.limusic.domain.manager.DataStoreManager.Values.TRUE
import com.simohypers.limusic.domain.mediaservice.handler.ControlState
import com.simohypers.limusic.domain.mediaservice.handler.NowPlayingTrackState
import com.simohypers.limusic.domain.mediaservice.handler.PlayerEvent
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.mediaservice.handler.RepeatState
import com.simohypers.limusic.domain.mediaservice.handler.SimpleMediaState
import com.simohypers.limusic.domain.mediaservice.handler.SleepTimerState
import com.simohypers.limusic.domain.repository.AlbumRepository
import com.simohypers.limusic.domain.repository.CacheRepository
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.repository.LyricsCanvasRepository
import com.simohypers.limusic.domain.repository.PlaylistRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.repository.StreamRepository
import com.simohypers.limusic.domain.utils.Resource
import com.simohypers.limusic.domain.utils.toLyrics
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.logger.LogLevel
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.expect.getDownloadFolderPath
import com.simohypers.limusic.expect.ui.toByteArray
import com.simohypers.limusic.utils.VersionManager
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.simohypers.limusic.domain.mediaservice.handler.DownloadHandler
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import com.simohypers.limusic.discord.DiscordOAuth
import com.simohypers.limusic.lastfm.LastfmAuth
import com.simohypers.limusic.lastfm.authorizeUrl
import com.simohypers.limusic.lastfm.checkApproval
import com.simohypers.limusic.lastfm.requestToken
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.added_to_queue
import limusic.composeapp.generated.resources.error
import limusic.composeapp.generated.resources.discord_login_failed
import limusic.composeapp.generated.resources.lastfm_connect_timeout
import limusic.composeapp.generated.resources.lastfm_login_failed
import limusic.composeapp.generated.resources.lastfm_scrobbling_as
import limusic.composeapp.generated.resources.login_success
import limusic.composeapp.generated.resources.play_next
import limusic.composeapp.generated.resources.shared
import limusic.composeapp.generated.resources.youtube_write_failed
import java.io.FileOutputStream
import kotlin.reflect.KClass


@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModel(
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
    private val cacheRepository: CacheRepository,
) : BaseViewModel() {
    var isFirstLiked: Boolean = false
    var isFirstMiniplayer: Boolean = false
    var isFirstSuggestions: Boolean = false

    private val appIdentity: AppIdentity by inject()

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    var isServiceRunning: Boolean = false

    private var _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private var regionCode: String? = null
    private var language: String? = null

    private var _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<NewFormatEntity?> = _format.asSharedFlow()

    /**
     * Which extractor and cipher decoder produced the current track's URLs, shown in the info sheet.
     *
     * Read alongside the format rather than stored with it: the format row is cached and reused,
     * while this describes the extraction that happened in this run of the app.
     */
    private val _extractSource: MutableStateFlow<String?> = MutableStateFlow(null)
    val extractSource: StateFlow<String?> = _extractSource.asStateFlow()

    private var _canvas: MutableStateFlow<CanvasResult?> = MutableStateFlow(null)
    val canvas: StateFlow<CanvasResult?> = _canvas

    private var canvasJob: Job? = null

    private val _intent: MutableStateFlow<GenericIntent?> = MutableStateFlow(null)
    val intent: StateFlow<GenericIntent?> = _intent

    private val _showNotificationPermissionDialog = MutableStateFlow(false)
    val showNotificationPermissionDialog: StateFlow<Boolean> = _showNotificationPermissionDialog

    // One-shot: the Desktop capsule asks the Now Playing panel, which hosts the page, to open
    // full-screen lyrics. The panel consumes it once shown.
    private val _fullscreenLyricsRequest = MutableStateFlow(false)
    val fullscreenLyricsRequest: StateFlow<Boolean> = _fullscreenLyricsRequest

    fun requestFullscreenLyrics() {
        _fullscreenLyricsRequest.value = true
    }

    fun consumeFullscreenLyricsRequest() {
        _fullscreenLyricsRequest.value = false
    }

    private var getFormatFlowJob: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)

    var isFullScreen: Boolean = false

    private var _nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val nowPlayingState: StateFlow<NowPlayingTrackState?> = _nowPlayingState

    fun getQueueDataState() = mediaPlayerHandler.queueData

    val castState: StateFlow<GenericCastState> get() = mediaPlayerHandler.castState

    private var _controllerState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false,
                volume = 1f,
            ),
        )
    val controllerState: StateFlow<ControlState> = _controllerState
    private val _getVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val getVideo: StateFlow<Boolean> = _getVideo

    private var _timeline =
        MutableStateFlow<TimeLine>(
            TimeLine(
                current = -1L,
                total = -1L,
                bufferedPercent = 0,
                loading = true,
            ),
        )
    val timeline: StateFlow<TimeLine> = _timeline

    private var _nowPlayingScreenData =
        MutableStateFlow<NowPlayingScreenData>(
            NowPlayingScreenData.initial(),
        )
    val nowPlayingScreenData: StateFlow<NowPlayingScreenData> = _nowPlayingScreenData


    /**
     * Which body of the Apple Music player was open last — held by ENUM NAME so this class stays
     * ignorant of the UI enum, which is internal to the player package.
     *
     * It cannot live in the composable: that player is inside a ModalBottomSheet, so dismissing
     * the sheet disposes the whole tree and takes any rememberSaveable with it — the tab snapped
     * back to the artwork every single time it was reopened. This class is a Koin `single`, so it
     * outlives the sheet while still resetting on app restart, which is the right lifetime for
     * "where I was a moment ago".
     */
    private val _lastPlayerViewTab = MutableStateFlow<String?>(null)
    val lastPlayerViewTab: StateFlow<String?> = _lastPlayerViewTab

    fun setLastPlayerViewTab(tabName: String) {
        _lastPlayerViewTab.value = tabName
    }

    init {
        viewModelScope.launch {
            log("SharedViewModel init")
            if (dataStoreManager.appVersion.first() != VersionManager.getVersionName()) {
                dataStoreManager.setAppVersion(
                    VersionManager.getVersionName(),
                )
            }
            val timeLineJob =
                launch {
                    nowPlayingState
                        .filterNotNull()
                        .flatMapLatest { nowPlayingState ->
                            timeline.map { timeLine ->
                                Pair(timeLine, nowPlayingState)
                            }
                        }.distinctUntilChanged { old, new ->
                            // A live stream's "total" is the seek window mpv reports, and it grows with
                            // every new segment — not a new length, so it must not re-run what a new
                            // length triggers (this fired every few seconds for as long as one played).
                            val sameLiveStream = old.first.isLive && new.first.isLive
                            (old.first.total.toString() + old.second.songEntity?.videoId).hashCode() ==
                                (new.first.total.toString() + new.second.songEntity?.videoId).hashCode() ||
                                (sameLiveStream && old.second.songEntity?.videoId == new.second.songEntity?.videoId)
                        }.collectLatest {
                            val nowPlaying = it.second
                            val timeline = it.first
                            // Lyrics and canvas both key off the track's length, which a live stream has none of.
                            if (timeline.isLive) return@collectLatest
                            log("Timeline job ${(it.first.total.toString() + it.second.songEntity?.videoId).hashCode()}")
                            if (timeline.total > 0 && nowPlaying.songEntity != null) {
                                if (nowPlaying.mediaItem.isSong() && nowPlayingScreenData.value.canvasData == null) {
                                    Logger.d(tag, "Duration is ${timeline.total}")
                                    Logger.d(tag, "MediaId is ${nowPlaying.mediaItem.mediaId}")
                                    getCanvas(nowPlaying.mediaItem.mediaId, (timeline.total / 1000).toInt())
                                }
                                nowPlaying.songEntity?.let { song ->
                                    if (nowPlayingScreenData.value.lyricsData == null) {
                                        loadLyrics(song, (timeline.total / 1000).toInt())
                                    }
                                }
                            }
                        }
                }
            val checkGetVideoJob =
                launch {
                    dataStoreManager.watchVideoInsteadOfPlayingAudio.collectLatest {
                        _getVideo.value = it == TRUE
                    }
                }
//            val controllerStateJob =
//                launch {
//                    controllerState.map { it.isLiked }.distinctUntilChanged().collectLatest {
//                        if (dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE) {
//                            nowPlayingState.value?.mediaItem?.mediaId?.let {
//                                getLikeStatus(it)
//                            }
//                        }
//                    }
//                }
            timeLineJob.join()
            checkGetVideoJob.join()
//            controllerStateJob.join()
        }

        runBlocking {
            dataStoreManager.getString("miniplayer_guide").first().let {
                isFirstMiniplayer = it != STATUS_DONE
            }
            dataStoreManager.getString("suggest_guide").first().let {
                isFirstSuggestions = it != STATUS_DONE
            }
            dataStoreManager.getString("liked_guide").first().let {
                isFirstLiked = it != STATUS_DONE
            }
        }
        viewModelScope.launch {
            mediaPlayerHandler.nowPlayingState
                .distinctUntilChangedBy {
                    it.songEntity?.videoId
                }.collectLatest { state ->
                    canvasJob?.cancel()
                    _nowPlayingState.value = state

                    // Seed the timeline from METADATA as soon as the track is known, instead of
                    // waiting for the player. SimpleMediaState.Ready carries a duration and only
                    // fires once the container is parsed; after a queue restore nothing plays, the
                    // position poll never starts (it runs only while isPlaying), and so nothing
                    // ever reports a length — which is why a restored queue showed no times at all.
                    // SongEntity has known the length since the song was first seen.
                    //
                    // Written on EVERY track change, not just when missing: leaving the old value
                    // in place would show the previous track's length over the new one.
                    // Order matters: metadata first because it is available immediately, then the
                    // player's own duration, and only -1 ("not known yet") when neither has one.
                    //
                    // Writing -1 whenever metadata is missing — which this did at first — makes
                    // every radio track and every first-time track flash NA:NA on the clock until
                    // the container is parsed, because those rows have no durationSeconds stored.
                    // Asking the player before giving up covers exactly that case: on a normal
                    // track change it usually already knows.
                    val metadataDurationMs = (state.songEntity?.durationSeconds ?: 0).toLong() * 1000L
                    val seededTotal =
                        metadataDurationMs.takeIf { it > 0L }
                            ?: mediaPlayerHandler.getPlayerDuration().takeIf { it > 0L }
                            ?: -1L
                    _timeline.update { it.copy(total = seededTotal) }
                    state.songEntity?.let { track ->
                        _nowPlayingScreenData.value =
                            NowPlayingScreenData(
                                nowPlayingTitle = track.title,
                                artistName =
                                    track
                                        .artistName
                                        ?.joinToString(", ") ?: "",
                                isVideo = false,
                                thumbnailURL = null,
                                canvasData = null,
                                lyricsData = null,
                                songInfoData = null,
                                playlistName =
                                    mediaPlayerHandler.queueData.value
                                        ?.data
                                        ?.playlistName ?: "",
                            )
                    }
                    state.mediaItem.let { now ->
                        _canvas.value = null
                        getLikeStatus(now.mediaId)
                        getSongInfo(now.mediaId)
                        getFormat(now.mediaId)
                        _nowPlayingScreenData.update {
                            it.copy(
                                thumbnailURL = now.metadata.artworkUri,
                                isVideo = now.isVideo(),
                            )
                        }
                    }
                    state.songEntity?.let { song ->
                        _liked.value = song.liked == true
                        _nowPlayingScreenData.update {
                            it.copy(
                                isExplicit = song.isExplicit,
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            val job1 =
                launch {
                    mediaPlayerHandler.simpleMediaState.collect { mediaState ->
                        when (mediaState) {
                            is SimpleMediaState.Buffering -> {
                                _timeline.update {
                                    it.copy(
                                        loading = true,
                                    )
                                }
                            }

                            SimpleMediaState.Initial -> {
                                _timeline.update { it.copy(loading = true) }
                            }

                            SimpleMediaState.Ended -> {
                                // Park at the end of the track rather than at -1. The only formatter
                                // for these fields renders any negative as "NA:NA", and nothing here
                                // is guaranteed to follow: at the end of the queue the player simply
                                // stays ended, so a -1 written here stays on screen. Worse, the
                                // Progress branch below ignores negative values and the Loading
                                // branch restores `total` without touching `current`, which is how
                                // the player ends up showing a correct duration next to "NA:NA".
                                _timeline.update {
                                    it.copy(
                                        current = it.total.coerceAtLeast(0L),
                                        bufferedPercent = 0,
                                        loading = false,
                                    )
                                }
                            }

                            is SimpleMediaState.Progress -> {
                                if (mediaState.progress >= 0L && mediaState.progress != _timeline.value.current) {
                                    // A live stream never reports a length (ExoPlayer: C.TIME_UNSET), so
                                    // a missing one means "loading" for everything except a live stream.
                                    if (_timeline.value.total > 0L || _timeline.value.isLive) {
                                        _timeline.update {
                                            it.copy(
                                                total = mediaPlayerHandler.getPlayerDuration().takeIf { d -> d > 0L } ?: it.total,
                                                current = mediaState.progress,
                                                loading = false,
                                            )
                                        }
                                    } else {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = true,
                                                total = mediaPlayerHandler.getPlayerDuration().takeIf { d -> d > 0L } ?: it.total,
                                            )
                                        }
                                    }
                                }
                                // When progress hasn't changed (same value polled again) or is negative,
                                // don't modify loading state. The loading flag is already managed by
                                // Buffering/Ready/Loading state events. Setting loading=true here would
                                // cause rapid flickering whenever the same value arrives twice,
                                // which it can: the handler's ticker and the adapter's position
                                // poll both run at 50ms and are not in step, so a poll is sometimes
                                // read twice. A repeat is not evidence of a stall, and the loading
                                // flag belongs to the Buffering/Ready events rather than to a
                                // guess made here.
                            }

                            is SimpleMediaState.Loading -> {
                                _timeline.update {
                                    it.copy(
                                        bufferedPercent = mediaState.bufferedPercentage,
                                        total = mediaState.duration,
                                        loading = true,
                                    )
                                }
                            }

                            is SimpleMediaState.Ready -> {
                                _timeline.update {
                                    it.copy(
                                        current = mediaPlayerHandler.getProgress(),
                                        loading = false,
                                        // The player's own duration wins, but ONLY when it has one.
                                        // ExoPlayer answers C.TIME_UNSET (a large negative, not
                                        // null) until it has parsed the container, and Ready is
                                        // also published from onIsLoadingChanged — which fires
                                        // before STATE_READY. Writing that would throw away the
                                        // metadata duration seeded on the track change above.
                                        total = mediaState.duration.takeIf { d -> d > 0L } ?: it.total,
                                    )
                                }
                            }
                        }
                    }
                }
            val controllerJob =
                launch {
                    Logger.d(tag, "ControllerJob is running")
                    mediaPlayerHandler.controlState.collectLatest {
                        Logger.d(tag, "ControlState is $it")
                        _controllerState.value = it
                        // Propagate crossfade state to timeline so UI can react
                        _timeline.update { timeline ->
                            timeline.copy(isCrossfading = it.isCrossfading)
                        }
                    }
                }
            val sleepTimerJob =
                launch {
                    mediaPlayerHandler.sleepTimerState.collectLatest {
                        _sleepTimerState.value = it
                    }
                }
            val playlistNameJob =
                launch {
                    mediaPlayerHandler.queueData.collectLatest {
                        _nowPlayingScreenData.update {
                            it.copy(playlistName = it.playlistName)
                        }
                    }
                }
            val liveStreamJob =
                launch {
                    // A track is only known to be live once its stream has been resolved, so this
                    // follows the registry as well as the track itself.
                    combine(mediaPlayerHandler.nowPlayingState, LiveStreamRegistry.liveVideoIds) { state, liveVideoIds ->
                        state.mediaItem.mediaId in liveVideoIds
                    }.distinctUntilChanged()
                        .collect { isLive -> _timeline.update { it.copy(isLive = isLive) } }
                }
            job1.join()
            controllerJob.join()
            sleepTimerJob.join()
            playlistNameJob.join()
            liveStreamJob.join()
        }
    }

    fun setIntent(intent: GenericIntent?) {
        _intent.value = intent
    }

    private val _lastfmConnecting = MutableStateFlow(false)

    /** A Last.fm sign-in is waiting for the user to approve it in the browser. */
    val lastfmConnecting: StateFlow<Boolean> = _lastfmConnecting
    private var lastfmAuthJob: Job? = null

    /**
     * Desktop's Last.fm sign-in: a request token, the approve page opened with it, then a check every
     * 5s for 5 minutes. It lives here rather than on a screen because the user leaves for the browser
     * and the screen they left from may be gone when they come back.
     */
    fun connectLastfm(openUrl: (String) -> Unit) {
        lastfmAuthJob?.cancel()
        lastfmAuthJob =
            viewModelScope.launch {
                _lastfmConnecting.value = true
                try {
                    val token = requestToken()
                    if (token == null) {
                        makeToast(getString(Res.string.lastfm_login_failed))
                        return@launch
                    }
                    openUrl(authorizeUrl(token))
                    repeat(60) {
                        delay(5_000)
                        when (val auth = checkApproval(token)) {
                            is LastfmAuth.Approved -> {
                                dataStoreManager.setLastfmSession(auth.session.sessionKey, auth.session.username)
                                makeToast(getString(Res.string.lastfm_scrobbling_as, auth.session.username))
                                return@launch
                            }
                            is LastfmAuth.Failed -> {
                                makeToast("Last.fm: ${auth.message}")
                                return@launch
                            }
                            LastfmAuth.Pending -> Unit
                        }
                    }
                    makeToast(getString(Res.string.lastfm_connect_timeout))
                } finally {
                    _lastfmConnecting.value = false
                }
            }
    }

    fun cancelLastfmConnect() {
        lastfmAuthJob?.cancel()
    }

    /** Finishes a Discord sign-in from the `limusic://discord-auth` redirect, the way the Last.fm callback used to. */
    fun completeDiscordLogin(
        code: String?,
        state: String?,
        error: String?,
    ) {
        viewModelScope.launch {
            // Discord refused before any code was issued (a scope the app may not ask for, a
            // redirect it doesn't know): its own words are more use than ours.
            if (error != null) {
                Logger.w("Auth", "Discord: authorize refused: $error")
                makeToast("Discord: $error")
                return@launch
            }
            val tokens = code?.let { DiscordOAuth.complete(it, state) }
            if (tokens != null) {
                dataStoreManager.setDiscordTokens(tokens.access, tokens.refresh, tokens.expiresAt)
                dataStoreManager.setRichPresenceEnabled(true)
                makeToast(getString(Res.string.login_success))
            } else {
                makeToast(getString(Res.string.discord_login_failed))
            }
        }
    }

    fun showNotificationPermissionDialog() {
        _showNotificationPermissionDialog.value = true
    }

    fun dismissNotificationPermissionDialog(doNotShowAgain: Boolean) {
        _showNotificationPermissionDialog.value = false
        if (doNotShowAgain) {
            putString("notification_permission_do_not_ask", "true")
        }
    }

    // The hearts read the local row; signed in, this brings it in line with YouTube's like. The
    // same answer names the track's other version, for the player's Song/Video switch.
    private fun getLikeStatus(videoId: String?) {
        if (videoId == null) return
        viewModelScope.launch { songRepository.refreshLikeFromYouTube(videoId)?.let(mediaPlayerHandler::learnTwin) }
    }

    // The song the lyrics chain last came back empty for, so the lyrics page can tell "still
    // looking" (lyricsData null, another id here) from "nothing anywhere".
    private val _lyricsMissFor = MutableStateFlow<String?>(null)
    val lyricsMissFor: StateFlow<String?> = _lyricsMissFor

    fun getLyricsProviders() = dataStoreManager.lyricsProviders

    fun getLyricsTextSize() = dataStoreManager.lyricsTextSize

    fun setLyricsTextSize(scale: Float) {
        viewModelScope.launch { dataStoreManager.setLyricsTextSize(scale) }
    }

    fun getLyricsRomanized() = dataStoreManager.lyricsRomanized

    fun setLyricsRomanized(on: Boolean) {
        viewModelScope.launch { dataStoreManager.setLyricsRomanized(on) }
    }

    private suspend fun lyricsArtist(song: SongEntity): String =
        song.artistName?.firstOrNull()?.takeUnless { it.contains("Various Artists") }
            ?: mediaPlayerHandler.nowPlaying.first()?.metadata?.artist?.toString().orEmpty()

    /**
     * The song's lyrics down the user's source list: Limusic's chain, cached per song. Dropped if
     * another song is playing by the time they arrive.
     */
    private fun loadLyrics(
        song: SongEntity,
        durationSeconds: Int,
    ) {
        // A live broadcast has nothing to sync lyrics to; its "duration" is only the seek window.
        if (LiveStreamRegistry.isLive(song.videoId)) {
            _lyricsMissFor.value = song.videoId
            return
        }
        viewModelScope.launch {
            val entity =
                lyricsCanvasRepository.getLyrics(
                    videoId = song.videoId,
                    title = song.title,
                    artist = lyricsArtist(song),
                    album = song.albumName,
                    durationSeconds = durationSeconds,
                    order = LyricsSource.order(dataStoreManager.lyricsProviders.first()),
                )
            if (_nowPlayingState.value?.songEntity?.videoId != song.videoId) return@launch
            showLyrics(song.videoId, entity)
        }
    }

    private fun showLyrics(
        videoId: String,
        entity: LyricsEntity?,
    ) {
        if (entity == null) {
            _lyricsMissFor.value = videoId
            _nowPlayingScreenData.update { it.copy(lyricsData = null) }
            return
        }
        val lyrics = entity.toLyrics()
        _nowPlayingScreenData.update {
            it.copy(
                lyricsData =
                    NowPlayingScreenData.LyricsData(
                        lyrics = lyrics.copy(lines = lyrics.lines?.map { l -> l.copy(words = decodeHtmlEntities(l.words)) }),
                        lyricsProvider = LyricsSource.fromId(entity.provider),
                        offsetMs = entity.offsetMs,
                        pinned = entity.pinned,
                    ),
            )
        }
    }

    /**
     * The source picker's preview: [source]'s lyrics for the playing song, uncached. [title] and
     * [artist] replace the song's own for a search by title; the album goes with them, the length
     * stays (it is still the recording playing). Null when the source has none; throws when it
     * couldn't be asked.
     */
    suspend fun previewLyrics(
        source: LyricsSource,
        title: String? = null,
        artist: String? = null,
    ): LyricsEntity? {
        val song = _nowPlayingState.value?.songEntity ?: return null
        return lyricsCanvasRepository.previewLyrics(
            videoId = song.videoId,
            source = source,
            title = title ?: song.title,
            artist = artist ?: lyricsArtist(song),
            album = if (title == null) song.albumName else null,
            durationSeconds = (timeline.value.total / 1000).toInt(),
        )
    }

    /** What the cache holds for [videoId]: the lyrics on screen, for the picker to keep as they are. */
    suspend fun savedLyrics(videoId: String): LyricsEntity? = lyricsCanvasRepository.getSavedLyrics(videoId).first()

    /** Keep [entity] for its song whatever the order says, or with null go back to Automatic. */
    fun chooseLyrics(entity: LyricsEntity?) {
        val song = _nowPlayingState.value?.songEntity ?: return
        viewModelScope.launch {
            if (entity != null) {
                val pinned = entity.copy(pinned = true, offsetMs = 0)
                lyricsCanvasRepository.insertLyrics(pinned)
                showLyrics(song.videoId, pinned)
            } else {
                lyricsCanvasRepository.forgetLyrics(song.videoId)
                _lyricsMissFor.value = null
                _nowPlayingScreenData.update { it.copy(lyricsData = null) }
                loadLyrics(song, (timeline.value.total / 1000).toInt())
            }
        }
    }

    /** This song's timing nudge, positive = lyrics later. Pins what it applies to, like desktop. */
    fun setSongLyricsOffset(offsetMs: Int) {
        val videoId = _nowPlayingState.value?.songEntity?.videoId ?: return
        _nowPlayingScreenData.update { data ->
            data.copy(lyricsData = data.lyricsData?.copy(offsetMs = offsetMs, pinned = true))
        }
        viewModelScope.launch {
            val saved = lyricsCanvasRepository.getSavedLyrics(videoId).first() ?: return@launch
            lyricsCanvasRepository.insertLyrics(saved.copy(offsetMs = offsetMs, pinned = true))
        }
    }

    private fun getCanvas(
        videoId: String,
        duration: Int,
    ) {
        Logger.d(tag, "Start getCanvas: $videoId $duration")
//        canvasJob?.cancel()
        viewModelScope.launch {
            if (dataStoreManager.amAnimatedArtwork.first() != TRUE) return@launch
            val sources = listOf(lyricsCanvasRepository.getAMAnimatedArtwork(videoId))

            var resolved = false
            for (source in sources) {
                if (resolved) break
                source.cancellable().collect { response ->
                    val data = response.data
                    if (response is Resource.Success && data != null && nowPlayingState.value?.mediaItem?.mediaId == videoId) {
                        resolved = true
                        _canvas.value = data
                        _nowPlayingScreenData.update {
                            it.copy(
                                canvasData =
                                    NowPlayingScreenData.CanvasData(
                                        isVideo = data.isVideo,
                                        url = data.canvasUrl,
                                        thumbUrl = data.canvasThumbUrl,
                                    ),
                            )
                        }
                        // Save canvas video url
                        if (data.isVideo) lyricsCanvasRepository.updateCanvasUrl(videoId, data.canvasUrl)
                        // Save canvas thumb url
                        data.canvasThumbUrl?.let { lyricsCanvasRepository.updateCanvasThumbUrl(videoId, it) }
                    } else {
                        log("Get canvas miss from a source: ${response.message}")
                    }
                }
            }

            if (!resolved) {
                nowPlayingState.value?.songEntity?.canvasUrl?.let { url ->
                    _nowPlayingScreenData.update {
                        it.copy(
                            canvasData =
                                NowPlayingScreenData.CanvasData(
                                    isVideo = url.isCanvasVideoUrl(),
                                    url = url,
                                    thumbUrl = nowPlayingState.value?.songEntity?.canvasThumbUrl,
                                ),
                        )
                    }
                }
            }
        }
    }

    fun getString(key: String): String? = runBlocking { dataStoreManager.getString(key).first() }

    // A Library tab asked for from outside the Library screen (a deep link); the screen selects it
    // the next time it composes and clears the request.
    private val _libraryTabRequest = MutableStateFlow<LibraryChipType?>(null)
    val libraryTabRequest: StateFlow<LibraryChipType?> = _libraryTabRequest.asStateFlow()

    fun requestLibraryTab(tab: LibraryChipType?) {
        _libraryTabRequest.value = tab
    }

    fun putString(
        key: String,
        value: String,
    ) {
        runBlocking { dataStoreManager.putString(key, value) }
    }

    fun setSleepTimer(minutes: Int) {
        mediaPlayerHandler.sleepStart(minutes)
    }

    fun stopSleepTimer() {
        mediaPlayerHandler.sleepStop()
    }

    private val downloadHandler: DownloadHandler by inject()
    private val libraryDownloader = LibraryDownloader()
    private var libraryDownloadJob: Job? = null
    private val _checkingLibrary = MutableStateFlow(false)

    /** While Keep library downloaded reads the library's lists. */
    val checkingLibrary: StateFlow<Boolean> = _checkingLibrary.asStateFlow()

    val keepLibraryDownloaded: StateFlow<Boolean> =
        dataStoreManager.keepLibraryDownloaded
            .map { it == DataStoreManager.TRUE }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** On reads the whole library now; off stops the daily check and leaves every download where it is. */
    fun setKeepLibraryDownloaded(on: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setKeepLibraryDownloaded(on)
            if (on) syncLibraryDownloads(force = true) else libraryDownloadJob?.cancel()
        }
    }

    private fun syncLibraryDownloads(force: Boolean = false) {
        if (libraryDownloadJob?.isActive == true) return
        libraryDownloadJob =
            viewModelScope.launch {
                _checkingLibrary.value = true
                try {
                    libraryDownloader.runIfDue(force)
                } finally {
                    _checkingLibrary.value = false
                }
            }
    }

    fun checkIsRestoring() {
        downloadHandler.resumeLeftovers()
        syncLibraryDownloads()
        viewModelScope.launch {
            val downloadedCacheKeys = cacheRepository.getAllCacheKeys(DOWNLOAD_CACHE).toSet()
            val missing = songRepository.getDownloadedSongs().first().orEmpty().map { it.videoId }.filterNot { it in downloadedCacheKeys }
            songRepository.updateDownloadStates(missing, DownloadState.STATE_NOT_DOWNLOADED)
        }
    }

    fun loadSharedMediaItem(videoId: String) {
        viewModelScope.launch {
            val localSong = songRepository.getSongById(videoId).firstOrNull()
            if (localSong != null) {
                val track = localSong.toTrack()
                mediaPlayerHandler.setQueueData(
                    QueueData.Data(
                        listTracks = arrayListOf(track),
                        firstPlayedTrack = track,
                        playlistId = "RDAMVM$videoId",
                        playlistName = getString(Res.string.shared),
                        playlistType = PlaylistType.RADIO,
                        continuation = null,
                    ),
                )
                loadMediaItemFromTrack(track, SONG_CLICK)
            } else {
                streamRepository.getFullMetadata(videoId).collectLatest { response ->
                    val track = response.data
                    when (response) {
                        is Resource.Success if (track != null) -> {
                            mediaPlayerHandler.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(track),
                                    firstPlayedTrack = track,
                                    playlistId = "RDAMVM$videoId",
                                    playlistName = getString(Res.string.shared),
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            loadMediaItemFromTrack(track, SONG_CLICK)
                        }

                        else -> {
                            log("Load shared media item error: ${response.message}", LogLevel.WARN)
                            makeToast("${getString(Res.string.error)}: ${response.message}")
                        }
                    }
                }
            }
        }
    }

    fun loadMediaItemFromTrack(
        track: Track,
        type: String,
        index: Int? = null,
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.loadMediaItem(track, type, index)
            songRepository.getSongById(track.videoId).firstOrNull()?.let { _liked.value = it.liked }
        }
    }

    fun onUIEvent(uiEvent: UIEvent) =
        viewModelScope.launch {
            when (uiEvent) {
                UIEvent.Backward -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Backward,
                    )
                }

                UIEvent.Forward -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Forward)
                }

                UIEvent.PlayPause -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.PlayPause,
                    )
                }

                UIEvent.Next -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                }

                UIEvent.Previous -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Previous,
                    )
                }

                UIEvent.SkipToPrevious -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.SkipToPrevious,
                    )
                }

                UIEvent.Stop -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Stop)
                }

                is UIEvent.UpdateProgress -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.UpdateProgress(
                            uiEvent.newProgress,
                        ),
                    )
                }

                UIEvent.Repeat -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Repeat)
                }

                UIEvent.Shuffle -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Shuffle)
                }

                UIEvent.ToggleLike -> {
                    Logger.d(tag, "ToggleLike")
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.ToggleLike)
                }

                is UIEvent.UpdateVolume -> {
                    val newVolume = uiEvent.newVolume
                    // Apply to the player first: persisting to DataStore is a suspending disk write
                    // and must not sit between the user's gesture and the audible change.
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.UpdateVolume(newVolume))
                    dataStoreManager.setPlayerVolume(newVolume)
                }
            }
        }

    override fun onCleared() {
        Logger.d("Check onCleared", "onCleared")
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    private fun getFormat(mediaId: String?) {
        if (mediaId != _format.value?.videoId && !mediaId.isNullOrEmpty()) {
            _format.value = null
            _extractSource.value = streamRepository.getExtractSource(mediaId, null)
            getFormatFlowJob?.cancel()
            getFormatFlowJob =
                viewModelScope.launch {
                    streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Logger.d(tag, "Get format for $mediaId: itag ${f?.itag}, expires ${f?.expiredTime}")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                        // Re-read on every emission: the first one usually lands before the
                        // extractor has finished, so the source is only known on a later pass.
                        _extractSource.value = streamRepository.getExtractSource(mediaId, f?.audioUrl)
                    }
                }
        }
    }

    private var songInfoJob: Job? = null

    fun getSongInfo(mediaId: String?) {
        songInfoJob?.cancel()
        songInfoJob =
            viewModelScope.launch {
                if (mediaId != null) {
                    songRepository.getSongInfo(mediaId).collect { song ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                songInfoData = song,
                            )
                        }
                    }
                }
            }
    }

    fun stopPlayer() {
        _nowPlayingScreenData.value = NowPlayingScreenData.initial()
        _nowPlayingState.value = null
        mediaPlayerHandler.resetSongAndQueue()
        onUIEvent(UIEvent.Stop)
    }

    private var _recreateActivity: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    private val _youTubePlaylistsVersion = MutableStateFlow(0)

    /** Bumped when one of your YouTube playlists is edited or deleted, so Library refetches its list. */
    val youTubePlaylistsVersion: StateFlow<Int> = _youTubePlaylistsVersion

    fun youTubePlaylistsChanged() {
        _youTubePlaylistsVersion.value++
    }

    private val _createdPlaylists = MutableStateFlow<List<PlaylistsResult>>(emptyList())

    /**
     * Playlists made on YouTube from this phone. YouTube's library list leaves a new one out for a
     * few seconds, so Library shows these in front of it until it lists them itself, as desktop does.
     */
    val createdPlaylists: StateFlow<List<PlaylistsResult>> = _createdPlaylists

    fun playlistCreatedOnYouTube(
        title: String,
        playlistId: String,
    ) {
        viewModelScope.launch { listCreated(title, playlistId) }
    }

    private suspend fun listCreated(
        title: String,
        playlistId: String,
    ): PlaylistsResult {
        val created =
            PlaylistsResult(
                author = dataStoreManager.getString("AccountName").first().orEmpty(),
                browseId = "VL" + playlistId.removePrefix("VL"),
                category = "",
                itemCount = "",
                resultType = "",
                thumbnails = emptyList(),
                title = title,
            )
        _createdPlaylists.update { listOf(created) + it }
        return created
    }

    /** Where New playlist puts one, desktop's choice: the account unless the last pick was this phone. */
    val newPlaylistOnYouTube: StateFlow<Boolean> =
        dataStoreManager
            .getString(NEW_PLAYLIST_WHERE)
            .map { it != "device" }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /**
     * New playlist, from Library or from the Add to playlist picker. [onCreated] gets the playlist
     * (a [PlaylistsResult] on the account, a [LocalPlaylistEntity] on the phone), or null when
     * YouTube refused, already said in a toast.
     */
    fun createPlaylist(
        title: String,
        onYouTube: Boolean,
        onCreated: (LibraryPlaylist?) -> Unit,
    ) {
        viewModelScope.launch {
            // Signed out there was no choice to remember.
            if (dataStoreManager.loggedIn.first() == DataStoreManager.TRUE) {
                dataStoreManager.putString(NEW_PLAYLIST_WHERE, if (onYouTube) "youtube" else "device")
            }
            val created: LibraryPlaylist? =
                if (onYouTube) {
                    playlistRepository.createYouTubePlaylist(title)?.let { listCreated(title, it) }
                } else {
                    LocalPlaylistEntity(id = localPlaylistRepository.createLocalPlaylist(title), title = title)
                }
            if (created == null) makeToast(getString(Res.string.youtube_write_failed))
            onCreated(created)
        }
    }

    /** [from] names the album or playlist the tracks came from: shuffle keeps such a block together. */
    fun playListNext(
        listTrack: List<Track>,
        from: String? = null,
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.playNext(listTrack, from)
            makeToast(getString(Res.string.play_next))
        }
    }

    fun addListToQueue(
        listTrack: List<Track>,
        from: String? = null,
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.addToQueue(listTrack, from)
            makeToast(getString(Res.string.added_to_queue))
        }
    }

    /**
     * Desktop's "Play only these, on repeat": the queue becomes [tracks] and loops. Repeat goes on
     * first, or autoplay could add to a short queue before it landed, and the hand-queued tracks are
     * dropped, or the new list would carry them into the loop.
     */
    fun playOnRepeat(
        tracks: List<Track>,
        from: String?,
    ) {
        val first = tracks.firstOrNull() ?: return
        viewModelScope.launch {
            // The toggle runs off -> all -> one -> off, so at most two presses reach all.
            repeat(2) {
                if (mediaPlayerHandler.controlState.value.repeatState != RepeatState.All) {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Repeat)
                }
            }
            mediaPlayerHandler.clearQueued()
            setQueueData(
                QueueData.Data(
                    listTracks = tracks,
                    firstPlayedTrack = first,
                    playlistName = from,
                    playlistType = PlaylistType.PLAYLIST,
                ),
            )
            loadMediaItem(first, PLAYLIST_CLICK, 0)
        }
    }

    // Drives the Mix for you tab: YouTube hands an anonymous session no mixes at all.
    fun getYouTubeLoggedIn() = dataStoreManager.loggedIn

    fun getThemeMode() = dataStoreManager.themeMode

    fun getThemeColorSource() = dataStoreManager.themeColorSource

    fun getCustomThemeColor() = dataStoreManager.customThemeColor

    fun getArtworkColors() = dataStoreManager.artworkColors

    fun getNowPlayingStyle() = dataStoreManager.nowPlayingStyle

    fun getLyricsStyle() = dataStoreManager.lyricsStyle

    fun getLyricsOffsetMs() = dataStoreManager.lyricsOffsetMs

    fun setLyricsOffsetMs(offsetMs: Int) {
        viewModelScope.launch {
            dataStoreManager.setLyricsOffsetMs(offsetMs)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(mode)
        }
    }

    fun setThemeColorSource(source: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeColorSource(source)
        }
    }

    fun setArtworkColors(enable: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setArtworkColors(enable)
        }
    }

    fun setCustomThemeColor(argbHex: String) {
        viewModelScope.launch {
            dataStoreManager.setCustomThemeColor(argbHex)
        }
    }

    fun setNowPlayingStyle(style: String) {
        viewModelScope.launch {
            dataStoreManager.setNowPlayingStyle(style)
        }
    }

    fun setLyricsStyle(style: String) {
        viewModelScope.launch {
            dataStoreManager.setLyricsStyle(style)
        }
    }

    fun getRomanizationLanguages() = dataStoreManager.romanizationLanguages

    fun setRomanizationLanguages(languages: Set<RomanizationLanguage>) {
        viewModelScope.launch {
            // Sorted by name so the stored string is stable: an unsorted Set writes a different
            // value for the same selection depending on iteration order, which makes the DataStore
            // flow emit on a change that did not happen.
            dataStoreManager.setRomanizationLanguages(languages.map { it.name }.sorted().joinToString(","))
        }
    }

    private val _reloadDestination: MutableStateFlow<KClass<*>?> = MutableStateFlow(null)
    val reloadDestination: StateFlow<KClass<*>?> = _reloadDestination.asStateFlow()

    fun reloadDestination(destination: KClass<*>) {
        _reloadDestination.value = destination
    }

    fun reloadDestinationDone() {
        _reloadDestination.value = null
    }

    private var _downloadFileProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.INIT)
    val downloadFileProgress: StateFlow<DownloadProgress> get() = _downloadFileProgress

    fun downloadFile(bitmap: ImageBitmap) {
        val fileName =
            "${nowPlayingScreenData.value.nowPlayingTitle} - ${nowPlayingScreenData.value.artistName}"
                .replace(Regex("""[|\\?*<":>]"""), "")
                .replace(" ", "_")
        val path =
            "${getDownloadFolderPath()}/$fileName"
        viewModelScope.launch {
            nowPlayingState.value?.track?.let { track ->
                val bytesArray = bitmap.toByteArray()
                try {
                    val fileOutputStream = FileOutputStream("$path.jpg")
                    fileOutputStream.write(bytesArray)
                    fileOutputStream.close()
                    Logger.d(tag, "Thumbnail saved to $path.jpg")
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
                songRepository
                    .downloadToFile(
                        track = track,
                        videoId = track.videoId,
                        path = path,
                        isVideo = nowPlayingScreenData.value.isVideo && getVideo.value,
                    ).collectLatest {
                        _downloadFileProgress.value = it
                    }
            }
        }
    }

    fun downloadFileDone() {
        _downloadFileProgress.value = DownloadProgress.INIT
    }

    fun setBitmap(bitmap: ImageBitmap?) {
        _nowPlayingScreenData.update {
            it.copy(bitmap = bitmap)
        }
    }

    fun shouldStopMusicService(): Boolean = runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }

    fun isUserLoggedIn(): Boolean = runBlocking { dataStoreManager.cookie.first().isNotEmpty() }

    fun isCombineFavoriteAndYTLiked(): Boolean = runBlocking { dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE }
}

sealed class UIEvent {
    data object PlayPause : UIEvent()

    data object Backward : UIEvent()

    data object Forward : UIEvent()

    data object Next : UIEvent()

    data object Previous : UIEvent()

    /**
     * Always advances to the previous track — bypasses the 3-second
     * "seek to start of current track" rule used by [Previous]. Used by the
     * NowPlaying artwork pager swipe.
     */
    data object SkipToPrevious : UIEvent()

    data object Stop : UIEvent()

    data object Shuffle : UIEvent()

    data object Repeat : UIEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data class UpdateVolume(
        val newVolume: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

data class NowPlayingScreenData(
    val playlistName: String,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    val songInfoData: SongInfoEntity? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
        // A still of the clip at the clip's own size — Apple Music's animated artwork sends one.
        // The Apple Music player shows it under the clip while that loads, and reads the clip's
        // proportions off it, so the frame is the right shape before the first video frame arrives.
        val thumbUrl: String? = null,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        // ponytail: nothing fills this since AI and SimpMusic translations went; it leaves with the
        // old player styles that still read it.
        val translatedLyrics: Pair<Lyrics, LyricsSource>? = null,
        val lyricsProvider: LyricsSource?,
        /** This song's timing nudge from the source picker, on top of the global offset. */
        val offsetMs: Int = 0,
        /** Source picked by hand or timing nudged. */
        val pinned: Boolean = false,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}

/**
 * Whether a stored canvas url points at something a player should open rather than an image.
 *
 * The column holds whatever the active source wrote: a Spotify canvas is an `.mp4`, while AM
 * animated artwork is an HLS `.m3u8` master playlist. Testing only for `.mp4` — as this did before
 * AM existed — sends every AM artwork down the still-image branch, and because the branch that
 * reads this is the one that restores a *cached* url, the failure only appears from the second play
 * of a track onwards.
 */
private fun String.isCanvasVideoUrl(): Boolean = contains(".mp4") || contains(".m3u8")

private const val NEW_PLAYLIST_WHERE = "new_playlist_where"

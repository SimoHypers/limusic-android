package com.simohypers.limusic.media3.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import com.simohypers.limusic.domain.data.player.AudioOutput
import com.simohypers.limusic.domain.data.player.GenericCastState
import com.simohypers.limusic.domain.data.player.GenericMediaItem
import com.simohypers.limusic.domain.data.player.GenericPlaybackParameters
import com.simohypers.limusic.domain.data.player.LiveStreamRegistry
import com.simohypers.limusic.domain.data.player.PlayerConstants
import com.simohypers.limusic.domain.data.player.PlayerError
import com.simohypers.limusic.domain.extension.isVideo
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.player.MediaPlayerInterface
import com.simohypers.limusic.domain.mediaservice.player.MediaPlayerListener
import com.simohypers.limusic.domain.repository.StreamRepository
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.media3.audio.EqualizerAudioProcessor
import com.simohypers.limusic.media3.audio.EqualizerCurve
import com.simohypers.limusic.media3.audio.SleepFadeAudioProcessor
import com.simohypers.limusic.media3.service.mediasourcefactory.isLiveStreamDetected
import com.simohypers.limusic.media3.service.mediasourcefactory.refusalReason
import com.simohypers.limusic.media3.service.mediasourcefactory.MergingMediaSourceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

private const val TAG = "CrossfadeExoPlayerAdapter"

/** The App log's playback timeline: what the player did and why, one Info line each. */
private const val PLAYBACK = "Playback"

/** A position that has not moved for this long while meant to be playing is logged as a freeze. */
private const val FREEZE_MS = 500L

private fun clock(ms: Long) = "${ms / 60_000}:${(ms / 1000 % 60).toString().padStart(2, '0')}"

/**
 * ExoPlayer implementation of [MediaPlayerInterface] with crossfade support.
 *
 * Architecture mirrors [com.simpmusic.media_jvm.GstreamerPlayerAdapter]:
 * - Internal playlist management (not ExoPlayer's playlist)
 * - Multi-player instance model: each track gets its own ExoPlayer
 * - Precaching system for smooth transitions
 * - Crossfade with listener swap pattern
 * - [DelegatingForwardingPlayer] for MediaSession integration
 *
 * Key difference from GstreamerPlayerAdapter:
 * - Uses [MergingMediaSourceFactory] + ResolvingDataSource for URL resolution
 *   (instead of manually extracting URLs via StreamRepository)
 * - Each ExoPlayer gets a single [MediaItem] and auto-resolves the stream URL
 */
@SuppressLint("UnsafeOptInUsageError")
@OptIn(UnstableApi::class)
internal class CrossfadeExoPlayerAdapter(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val dataStoreManager: DataStoreManager,
    private val mediaSourceFactory: MergingMediaSourceFactory,
    private val audioAttributes: AudioAttributes,
    private val streamRepository: StreamRepository,
) : MediaPlayerInterface {
    // ========== Internal State Enum (same as GstreamerPlayerAdapter) ==========

    private enum class InternalState {
        IDLE, // No media loaded
        PREPARING, // Loading media
        READY, // Ready to play/paused
        PLAYING, // Currently playing
        PAUSED,
        ENDED, // Playback ended
        ERROR, // Error state
    }

    private fun InternalState.isInReadyState(): Boolean = this == InternalState.READY || this == InternalState.PLAYING || this == InternalState.PAUSED

    // ========== Crossfade Settings (loaded from DataStore) ==========

    init {
        coroutineScope.launch {
            dataStoreManager.crossfadeEnabled.collect { enabled ->
                crossfadeEnabled = (enabled == DataStoreManager.TRUE)
                Logger.d(TAG, "Crossfade enabled: $crossfadeEnabled")
            }
        }
        coroutineScope.launch {
            dataStoreManager.crossfadeDuration.collect { duration ->
                crossfadeDurationMs = duration
                Logger.d(TAG, "Crossfade duration: $crossfadeDurationMs ms")
            }
        }
        coroutineScope.launch {
            dataStoreManager.watchVideoInsteadOfPlayingAudio.collect { enabled ->
                watchVideoEnabled = (enabled == DataStoreManager.TRUE)
                Logger.d(TAG, "Watch video enabled: $watchVideoEnabled")
            }
        }
        coroutineScope.launch {
            dataStoreManager.crossfadeSkipAlbum.collect { enabled ->
                skipCrossfadeInAlbum = (enabled == DataStoreManager.TRUE)
                Logger.d(TAG, "Skip crossfade inside album: $skipCrossfadeInAlbum")
            }
        }
    }

    // ========== State Management ==========

    private val listeners = mutableListOf<MediaPlayerListener>()

    @Volatile
    private var currentPlayer: ExoPlayer? = null

    @Volatile
    private var internalState = InternalState.IDLE

    @Volatile
    private var internalPlayWhenReady = true

    @Volatile
    private var internalVolume = 1.0f

    /**
     * Sleep-timer fade attenuation. Applied on a separate volume line — every player's
     * [SleepFadeAudioProcessor] reads this value straight out of here, so writing it once covers
     * both players of a crossfade and nothing on the `volume` line has to be touched.
     */
    @Volatile
    private var internalSleepFadeFactor = 1.0f

    /**
     * The equalizer curve in force. Read the same way as [internalSleepFadeFactor]: every player's
     * [EqualizerAudioProcessor] samples it on every buffer, so one write covers both players of a
     * crossfade and every precached one without any of them having to be found first.
     */
    @Volatile
    private var internalEqualizerCurve: EqualizerCurve = EqualizerCurve.FLAT

    @Volatile
    private var internalRepeatMode = PlayerConstants.REPEAT_MODE_OFF

    @Volatile
    private var internalShuffleModeEnabled = false

    @Volatile
    private var internalPlaybackSpeed = 1.0f

    @Volatile
    private var internalPlaybackPitch = 1.0f

    @Volatile
    private var internalSkipSilence = false

    // Position tracking - updated periodically, not on every query
    @Volatile
    private var cachedPosition = 0L

    @Volatile
    private var cachedDuration = 0L

    @Volatile
    private var cachedBufferedPosition = 0L

    @Volatile
    private var cachedIsLoading = false

    private var positionUpdateJob: Job? = null

    // Active Player.Listener (equivalent to BusListeners in GstreamerPlayerAdapter)
    // Only ONE listener instance, attached to ONE ExoPlayer at a time.
    // Swapped between players during crossfade.
    private var activePlayerListener: Player.Listener? = null

    // ========== Audio Focus (manual, session-scoped) — #2155 ==========
    // The multi-player swap model means audio focus must NOT be tied to any single
    // ExoPlayer: releasing the outgoing player would abandon focus, and the incoming
    // player (built with handleAudioFocus=false) never re-requests it — the root cause
    // of "music stops between tracks" (see androidx/media#2100). Instead we hold one
    // app-level AudioFocusRequest at the adapter level so focus survives every swap.

    private val duckVolumeFactor = 0.2f

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    /**
     * One audio session for every player this adapter builds. Each track gets a new ExoPlayer, and
     * each ExoPlayer would otherwise open its own session, so a system equalizer (Poweramp, Wavelet)
     * had to attach again on every track. Declared above the initial player, which reads it.
     */
    private val sharedAudioSessionId: Int by lazy {
        audioManager?.generateAudioSessionId()?.takeIf { it > 0 } ?: C.AUDIO_SESSION_ID_UNSET
    }

    @Volatile
    private var hasAudioFocus = false

    /** True when focus was lost transiently so playback should auto-resume on regain. */
    @Volatile
    private var resumeOnFocusGain = false

    private val audioFocusListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            // Info, so the App log shows when another app's sound paused or ducked us.
            val what =
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> "back"
                    AudioManager.AUDIOFOCUS_LOSS -> "lost, pausing"
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "lost for a moment, pausing"
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "lost for a moment, ducking"
                    else -> "changed ($focusChange)"
                }
            Logger.i(PLAYBACK, "audio focus $what")
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Don't fight the crossfade ramp; while crossfading it owns the volume.
                    if (!isCrossfading) currentPlayer?.volume = internalVolume
                    if (resumeOnFocusGain) {
                        resumeOnFocusGain = false
                        play()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    // Permanent loss (another app took over): pause and stop tracking focus.
                    resumeOnFocusGain = false
                    hasAudioFocus = false
                    pause()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Temporary loss (e.g. an incoming call): pause and remember to resume.
                    resumeOnFocusGain = internalState == InternalState.PLAYING
                    pause()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // Lower the volume instead of pausing (e.g. a navigation prompt).
                    // Skip during crossfade — the ramp owns volume and would override this.
                    if (!isCrossfading) currentPlayer?.volume = internalVolume * duckVolumeFactor
                }
            }
        }

    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes
                    .Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            ).setOnAudioFocusChangeListener(audioFocusListener)
            .setWillPauseWhenDucked(false)
            .build()
    }

    /** Request app-level audio focus once; idempotent while focus is held. */
    private fun requestAudioFocusInternal(): Boolean {
        val am = audioManager ?: return true
        if (hasAudioFocus) return true
        val granted = am.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasAudioFocus = granted
        Logger.d(TAG, "requestAudioFocus -> granted=$granted")
        return granted
    }

    private fun abandonAudioFocusInternal() {
        val am = audioManager ?: return
        if (!hasAudioFocus) return
        am.abandonAudioFocusRequest(audioFocusRequest)
        hasAudioFocus = false
        resumeOnFocusGain = false
        Logger.d(TAG, "abandonAudioFocus")
    }

    // ========== Precaching System ==========

    private data class PrecachedPlayer(
        val player: ExoPlayer,
        val mediaItem: GenericMediaItem,
    )

    // VideoId -> PrecachedPlayer
    private val precachedPlayers = ConcurrentHashMap<String, PrecachedPlayer>()

    // ========== Audio Output ==========

    /**
     * Where the sound leaves by — see [MediaPlayerInterface.audioOutputs]. The picked device goes to
     * every player this adapter runs, and [createExoPlayerInstance] gives it to each new one, so the
     * next track, a crossfade partner or a precached handle cannot slip back to the old output.
     * Declared ahead of the first player built below, which reads it.
     */
    private val outputRouter = AudioOutputRouter(context) { device -> applyPreferredAudioDevice(device) }

    override val audioOutputs: StateFlow<List<AudioOutput>>
        get() = outputRouter.outputs

    override fun selectAudioOutput(id: String?) = outputRouter.select(id)

    override fun refreshAudioOutputs() = outputRouter.refresh()

    private fun applyPreferredAudioDevice(device: AudioDeviceInfo?) {
        coroutineScope.launch {
            currentPlayer?.setPreferredAudioDevice(device)
            fadingOutPlayer?.setPreferredAudioDevice(device)
            precachedPlayers.values.forEach { it.player.setPreferredAudioDevice(device) }
        }
    }
    private var precacheEnabled = true
    // One track ahead, as the desktop app does: every precached track is a full stream resolution
    // (several YouTube requests), so looking further ahead only spends requests on songs that may
    // never play.
    private val maxPrecacheCount = 1
    private var precacheJob: Job? = null

    // ========== Crossfade System ==========

    @Volatile
    private var crossfadeEnabled = false

    /** See MediaPlayerInterface.crossfadeSuppressed — set while in a Listen Together room. */
    override var crossfadeSuppressed: Boolean = false

    @Volatile
    private var crossfadeDurationMs = 5000

    // Whether video content plays as video (watch-video setting) — the same condition
    // MergingMediaSourceFactory uses to build a merged audio+video source.
    @Volatile
    private var watchVideoEnabled = false

    /** User setting: leave transitions inside an album alone. */
    @Volatile
    private var skipCrossfadeInAlbum = false

    /** Set by the handler when an album is loaded; empty for every other kind of queue. */
    @Volatile
    private var internalAlbumTrackIds: Set<String> = emptySet()

    /** The track going out while a fade runs; [currentPlayer] is already the one coming in. */
    @Volatile
    private var fadingOutPlayer: ExoPlayer? = null

    @Volatile
    private var crossfadeJob: Job? = null

    @Volatile
    private var isCrossfading = false

    // ========== Retry on Source Error ==========
    // Track retry attempts per media item to avoid infinite retry loops
    private var retryCount = 0
    private var retryVideoId: String? = null
    private val maxRetryCount = 2

    /**
     * Update crossfade state and notify listeners when it changes.
     */
    private fun setCrossfading(value: Boolean) {
        if (isCrossfading != value) {
            isCrossfading = value
            listeners.forEach { it.onCrossfadeStateChanged(value) }
        }
    }

    // ========== Playlist Management ==========

    private val playlist = mutableListOf<GenericMediaItem>()
    private var localCurrentMediaItemIndex = -1

    // Shuffle management
    private var shuffleIndices = mutableListOf<Int>()
    private var shuffleOrder = mutableListOf<Int>()

    /**
     * Tracks that keep their place right after the current one when a track is appended under
     * shuffle: one put there with Play next. Held by identity, since a queue can hold the same song
     * twice.
     */
    private val pinnedNextItems: MutableSet<GenericMediaItem> =
        Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap<GenericMediaItem, Boolean>()))

    // Loading management
    private var currentLoadJob: Job? = null

    // ========== ForwardingPlayer for MediaSession ==========

    // Create an initial idle ExoPlayer for MediaSession to hold
    private val initialPlayer = createExoPlayerInstance()

    /**
     * Stable [Player] reference for MediaSession.
     * Delegates all calls to the currently active [ExoPlayer] instance.
     * Updated via [DelegatingForwardingPlayer.swapDelegate] when the active player changes.
     */
    val forwardingPlayer: DelegatingForwardingPlayer = DelegatingForwardingPlayer(initialPlayer)

    init {
        currentPlayer = initialPlayer

        // Wire up playlist navigation so ForwardingPlayer (and thus MediaSession)
        // can see the full playlist state instead of the single-item ExoPlayer state.
        // Only navigation commands are overridden — NOT getMediaItemCount/getCurrentMediaItemIndex
        // which must stay consistent with ExoPlayer's internal Timeline to avoid crashes.
        forwardingPlayer.playlistNavigationProvider =
            object : DelegatingForwardingPlayer.PlaylistNavigationProvider {
                override fun hasNextMediaItem(): Boolean = this@CrossfadeExoPlayerAdapter.hasNextMediaItem()

                override fun hasPreviousMediaItem(): Boolean = this@CrossfadeExoPlayerAdapter.hasPreviousMediaItem()

                override fun seekToNext(): Unit = this@CrossfadeExoPlayerAdapter.seekToNext()

                override fun seekToPrevious(): Unit = this@CrossfadeExoPlayerAdapter.seekToPrevious()

                override fun seekToPreviousMediaItem(): Unit = this@CrossfadeExoPlayerAdapter.seekToPreviousMediaItem()

                override fun play(): Unit = this@CrossfadeExoPlayerAdapter.play()

                override fun pause(): Unit = this@CrossfadeExoPlayerAdapter.pause()

                override fun setPlayWhenReady(playWhenReady: Boolean) {
                    this@CrossfadeExoPlayerAdapter.playWhenReady = playWhenReady
                }
            }
    }

    // ========== Cast Remote Routing ==========

    /**
     * While a Cast session is active this holds the session-level [Player] (the unified
     * CastPlayer wrapping [forwardingPlayer]): transport calls and position/state getters
     * are routed to it, and playback-start requests are handed to [castPlaybackRouter]
     * instead of the local ExoPlayer machinery. The playlist itself stays local — the
     * receiver only ever sees a small resolved-URL window of it.
     */
    @Volatile
    private var castRemotePlayer: Player? = null

    internal val isCastActive: Boolean
        get() = castRemotePlayer != null

    /** Set by CastHandoffManager: (playlistIndex, startPositionMs, playWhenReady) -> load on receiver. */
    internal var castPlaybackRouter: ((Int, Long, Boolean) -> Unit)? = null

    internal fun setCastActive(
        remotePlayer: Player?,
        deviceName: String?,
    ) {
        if (remotePlayer != null) {
            if (castRemotePlayer === remotePlayer) return
            castRemotePlayer = remotePlayer
            Logger.i(TAG, "Cast session active on ${deviceName ?: "unknown device"} — local playback suspended")
            coroutineScope.launch {
                // Kill anything that makes local noise or wastes battery while remote.
                cancelFade()
                cancelPrecaching()
                clearAllPrecacheInternal()
                currentPlayer?.pause()
                stopPositionUpdates()
                abandonAudioFocusInternal()
                notifyEqualizerIntent(false)
            }
            listeners.forEach { it.onCastStateChanged(GenericCastState(isRemote = true, deviceName = deviceName)) }
        } else {
            if (castRemotePlayer == null) return
            castRemotePlayer = null
            Logger.i(TAG, "Cast session ended — back to local playback")
            listeners.forEach { it.onCastStateChanged(GenericCastState.NOT_CASTING) }
        }
    }

    /** Remote queue advanced — keep the local playlist pointer and UI in sync. */
    internal fun notifyRemoteTransition(playlistIndex: Int) {
        if (!isCastActive || playlistIndex !in playlist.indices) return
        if (playlistIndex == localCurrentMediaItemIndex) return
        localCurrentMediaItemIndex = playlistIndex
        unpinCurrent()
        val item = playlist[playlistIndex]
        listeners.forEach {
            it.onMediaItemTransition(item, PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        }
    }

    internal fun notifyRemoteIsPlaying(isPlaying: Boolean) {
        if (!isCastActive) return
        internalPlayWhenReady = isPlaying
        listeners.forEach { it.onIsPlayingChanged(isPlaying) }
    }

    internal fun notifyRemotePlaybackState(playbackState: Int) {
        if (!isCastActive) return
        listeners.forEach { it.onPlaybackStateChanged(playbackState) }
    }

    // ========== ExoPlayer Instance Factory ==========

    /**
     * Create a new ExoPlayer instance.
     *
     * Audio focus is NOT handled per-player: it is managed once at the adapter level
     * (see the Audio Focus section) so it survives every player swap (#2155).
     */
    private fun createExoPlayerInstance(): ExoPlayer {
        val sleepFade = SleepFadeAudioProcessor { internalSleepFadeFactor }
        val equalizer = EqualizerAudioProcessor { internalEqualizerCurve }

        val perPlayerRenderers =
            object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink =
                    DefaultAudioSink
                        .Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                        .setAudioProcessorChain(
                            DefaultAudioSink.DefaultAudioProcessorChain(
                                // Equalizer first, matching the desktop graph, where the ten
                                // `equalizer` entries sit ahead of everything else in mpv's `af`
                                // list — it decides which stage a boost can clip in, and the
                                // preamp that makes room for the boost belongs with it. The
                                // sleep fade stays last, because it is the master attenuation and
                                // has to survive everything above.
                                arrayOf(equalizer, sleepFade),
                                SilenceSkippingAudioProcessor(
                                    2_000_000,
                                    (20_000 / 2_000_000).toFloat(),
                                    2_000_000,
                                    0,
                                    256,
                                ),
                                SonicAudioProcessor(),
                            ),
                        ).build()
            }

        val player =
            ExoPlayer
                .Builder(context)
                .setAudioAttributes(audioAttributes, false)
                .setLoadControl(
                    DefaultLoadControl
                        .Builder()
                        .setBufferDurationsMs(
                            DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * 4,
                            DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 4,
                            0,
                            0,
                        ).build(),
                ).setWakeMode(C.WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .setSeekForwardIncrementMs(5000)
                .setSeekBackIncrementMs(5000)
                .setMediaSourceFactory(mediaSourceFactory)
                .setRenderersFactory(perPlayerRenderers)
                .build()
        if (sharedAudioSessionId != C.AUDIO_SESSION_ID_UNSET) player.setAudioSessionId(sharedAudioSessionId)

        // The output the user picked, if any: a player built for the next track, a crossfade or a
        // precache starts where the others are playing rather than back on the system's choice.
        outputRouter.preferredDevice?.let { player.setPreferredAudioDevice(it) }

        return player
    }

    // ========== Playback Control ==========

    override fun play() {
        Logger.d(TAG, "play() called (state: $internalState, playWhenReady: $internalPlayWhenReady)")
        castRemotePlayer?.let { remote ->
            internalPlayWhenReady = true
            remote.play()
            return
        }
        coroutineScope.launch {
            when (internalState) {
                InternalState.READY, InternalState.ENDED, InternalState.PAUSED -> {
                    currentPlayer?.let { player ->
                        requestAudioFocusInternal()
                        // At the end of the queue `play()` only sets playWhenReady, which does
                        // nothing while the player sits in STATE_ENDED — the press would look
                        // ignored. Rewind first so the last track replays.
                        if (internalState == InternalState.ENDED) {
                            Logger.d(TAG, "Play: replaying from the start after end of queue")
                            player.seekTo(0L)
                        }
                        // A controller's stop (system media controls, after a long pause) reaches the
                        // ExoPlayer through the ForwardingPlayer without passing here: it sits idle at
                        // its position while this adapter still says paused, and play() alone does nothing.
                        if (player.playbackState == Player.STATE_IDLE) player.prepare()
                        player.play()
                        Logger.i(PLAYBACK, "play at ${clock(player.currentPosition)}")
                        transitionToState(InternalState.PLAYING)
                        internalPlayWhenReady = true
                    } ?: Logger.w(TAG, "Play called but currentPlayer is null")
                }

                InternalState.PREPARING -> {
                    internalPlayWhenReady = true
                    Logger.d(TAG, "Play: During PREPARING - will auto-play when ready")
                }

                InternalState.PLAYING -> {
                    internalPlayWhenReady = true
                    cachedIsLoading = false
                }

                // The track failed. Play loads it again from where it stopped, asking for its stream
                // afresh; it used to do nothing here, and only a skip got music going again.
                InternalState.ERROR -> {
                    Logger.i(TAG, "Play after an error: loading the track again at ${cachedPosition}ms")
                    internalPlayWhenReady = true
                    loadAndPlayTrackInternal(localCurrentMediaItemIndex, cachedPosition.coerceAtLeast(0L), shouldPlay = true)
                }

                else -> {
                    Logger.d(TAG, "Play: Called in invalid state: $internalState")
                }
            }
        }
    }

    override fun pause() {
        Logger.d(TAG, "pause() called (state: $internalState, playWhenReady: $internalPlayWhenReady)")
        castRemotePlayer?.let { remote ->
            internalPlayWhenReady = false
            remote.pause()
            // The coroutine below never runs on this path, so the sleep attenuation has to be
            // cleared here too. Left set, it would outlive the cast session: the processor stays in
            // the local chain and would keep multiplying every sample by ~0, leaving the app silent
            // with a full volume slider until the process restarts.
            internalSleepFadeFactor = 1f
            return
        }
        coroutineScope.launch {
            try {
                forwardingPlayer.suppressPlaybackEnded = false
                // Pausing mid-fade stays on the incoming track, the one the UI already shows.
                cancelFade()

                when (internalState) {
                    InternalState.PLAYING, InternalState.READY -> {
                        currentPlayer?.let { player ->
                            player.pause()
                            Logger.i(PLAYBACK, "pause at ${clock(player.currentPosition)}")
                            transitionToState(InternalState.PAUSED)
                            internalPlayWhenReady = false
                        }
                    }

                    InternalState.PREPARING -> {
                        internalPlayWhenReady = false
                    }

                    else -> {
                        Logger.d(TAG, "Pause: Called in invalid state: $internalState")
                    }
                }
            } finally {
                // Playback has stopped, so a sleep-timer attenuation has served its purpose.
                // Cleared here rather than by the caller, which cannot tell when this actually
                // happened — pause() only queues this coroutine.
                //
                // In `finally` because the block above can throw or be cancelled (committing a
                // crossfade joins a job that may be cancelled). Anything that skips this leaves the
                // factor near zero, and since the processor multiplies every sample by it, that is
                // permanent silence with a full volume slider — no code path recovers from it.
                internalSleepFadeFactor = 1f
            }
        }
    }

    override fun stop() {
        castRemotePlayer?.let { remote ->
            remote.stop()
            return
        }
        coroutineScope.launch {
            forwardingPlayer.suppressPlaybackEnded = false
            // Stopping only the current player would leave the outgoing one fading out on its own.
            cancelFade()
            currentPlayer?.let { player ->
                Logger.i(PLAYBACK, "stop at ${clock(player.currentPosition)}")
                player.stop()
                transitionToState(InternalState.IDLE)
                stopPositionUpdates()
                abandonAudioFocusInternal()
                notifyEqualizerIntent(false)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        // A live broadcast is followed at its live edge: its seek bar shows LIVE, not a length.
        if (isCurrentTrackLive()) return
        castRemotePlayer?.let { remote ->
            remote.seekTo(positionMs)
            cachedPosition = positionMs
            return
        }
        // Reflected immediately so the progress bar does not snap back while the seek is queued.
        cachedPosition = positionMs
        coroutineScope.launch {
            // A seek is aimed at the incoming track; the outgoing one has nothing to do with it.
            cancelFade()
            currentPlayer?.let { player ->
                try {
                    Logger.i(PLAYBACK, "seek ${clock(player.currentPosition)} -> ${clock(positionMs)}")
                    player.seekTo(positionMs)
                } catch (e: Exception) {
                    Logger.e(TAG, "Seek exception: ${e.message}", e)
                }
            }
        }
    }

    override fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    ) {
        if (mediaItemIndex !in playlist.indices) return

        coroutineScope.launch {
            val shouldPlay = internalPlayWhenReady

            // Cancel any ongoing load
            currentLoadJob?.cancel()

            // Load the new track
            localCurrentMediaItemIndex = mediaItemIndex
            unpinCurrent()
            loadAndPlayTrackInternal(mediaItemIndex, positionMs, shouldPlay)
        }
    }

    override fun seekBack() {
        val newPosition = (currentPosition - 5000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    override fun seekForward() {
        val end = duration.takeIf { it > 0 } ?: cachedDuration
        val newPosition = (currentPosition + 5000).coerceAtMost(end)
        seekTo(newPosition)
    }

    override fun seekToNext() {
        coroutineScope.launch {
            // Mid-fade the incoming track is already current, so "next" is the one after it. Cut
            // first: with no next track the fade would otherwise carry on under the press.
            cancelFade()
            if (hasNextMediaItem()) {
                seekTo(getNextMediaItemIndex(), 0)
            }
        }
    }

    override fun seekToPrevious() {
        coroutineScope.launch {
            // Mid-fade the incoming track is already current, so the 3-second rule below reads it.
            cancelFade()

            // Standard music player behavior:
            // - Position > 3s  → seek to start of current track
            // - Position <= 3s → go to previous track
            val positionThresholdMs = 3000L
            val position = currentPosition
            // A live broadcast has no start to go back to, so Previous always means the previous track.
            if (position > positionThresholdMs && !isCurrentTrackLive()) {
                Logger.d(TAG, "seekToPrevious: pos=${position}ms > ${positionThresholdMs}ms — seeking to start")
                seekTo(0)
            } else if (hasPreviousMediaItem()) {
                Logger.d(TAG, "seekToPrevious: pos=${position}ms <= ${positionThresholdMs}ms — going to previous track")
                val prevIndex = getPreviousMediaItemIndex()
                seekTo(prevIndex, 0)
            } else {
                Logger.d(TAG, "seekToPrevious: No previous item, seeking to start")
                seekTo(0)
            }
        }
    }

    override fun seekToPreviousMediaItem() {
        coroutineScope.launch {
            cancelFade()

            // Always advance to the previous track regardless of `cachedPosition` —
            // skips the 3-second "seek to start" rule used by seekToPrevious().
            if (hasPreviousMediaItem()) {
                val prevIndex = getPreviousMediaItemIndex()
                Logger.d(TAG, "seekToPreviousMediaItem: jumping to previous index=$prevIndex")
                seekTo(prevIndex, 0)
            } else {
                Logger.d(TAG, "seekToPreviousMediaItem: No previous item — no-op")
            }
        }
    }

    override fun prepare() {
        if (playlist.isNotEmpty() && localCurrentMediaItemIndex >= 0) {
            coroutineScope.launch {
                loadAndPlayTrackInternal(localCurrentMediaItemIndex, 0, false)
            }
        }
    }

    // ========== Media Item Management ==========

    override fun setMediaItem(mediaItem: GenericMediaItem) {
        coroutineScope.launch {
            // Cancel ongoing operations
            currentLoadJob?.cancel()
            cancelPrecaching()

            playlist.clear()
            clearAllPrecacheInternal()
            playlist.add(mediaItem)
            localCurrentMediaItemIndex = 0

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
            loadAndPlayTrackInternal(0, 0, internalPlayWhenReady)
        }
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        playlist.add(mediaItem)

        if (internalShuffleModeEnabled) {
            insertAppendedIntoShuffleOrder()
        }

        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

        if (playlist.size - 1 - currentMediaItemIndex <= maxPrecacheCount) {
            coroutineScope.launch {
                clearPrecacheExceptCurrentInternal()
                triggerPrecachingInternal()
            }
        }
    }

    override fun addMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        if (index in 0..playlist.size) {
            val currentIndexBeforeInsert = localCurrentMediaItemIndex

            playlist.add(index, mediaItem)

            // Adjust current index if needed
            if (index <= localCurrentMediaItemIndex) {
                localCurrentMediaItemIndex++
            }

            // Update shuffle order if enabled
            if (internalShuffleModeEnabled) {
                if (currentIndexBeforeInsert >= 0 && index == currentIndexBeforeInsert + 1) {
                    val currentShufflePos = shuffleIndices.getOrNull(currentIndexBeforeInsert) ?: 0
                    insertIntoShuffleOrder(index, currentShufflePos)
                    pinnedNextItems.add(mediaItem)
                } else {
                    createShuffleOrder()
                }
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            if (index - 1 - currentMediaItemIndex <= maxPrecacheCount) {
                coroutineScope.launch {
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }
            }
        }
    }

    override fun removeMediaItem(index: Int) {
        coroutineScope.launch {
            // Bounds checked INSIDE the launch, on the same queue as removeAt: a removal queued
            // earlier (a radio trimming its history) can shrink the playlist between an outside check
            // and this body — issue #2156's crash shape, fixed the same way in MpvPlayerAdapter.
            if (index !in playlist.indices) return@launch
            // Where the track sat in the shuffled order, read before the order loses it.
            val shuffledPos = if (internalShuffleModeEnabled) shuffleIndices.getOrNull(index) ?: -1 else -1
            val track = playlist.removeAt(index)
            // Only this track leaves the shuffled order, before anything below reads the order.
            // Rebuilding it here (createShuffleOrder) reshuffled the whole queue every time one track
            // was removed with shuffle on.
            if (internalShuffleModeEnabled) removeFromShuffleOrder(index)
            pinnedNextItems.remove(track)

            // Remove from precache
            precachedPlayers.remove(track.mediaId)?.let { cached ->
                cleanupPlayerInternal(cached.player)
            }

            when {
                index < localCurrentMediaItemIndex -> {
                    localCurrentMediaItemIndex--
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }

                index == localCurrentMediaItemIndex -> {
                    if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                        // The track after it in the shuffled order takes over, not the next one in
                        // the playlist: the removal shifted that track into the removed one's slot.
                        localCurrentMediaItemIndex = shuffleOrder[shuffledPos.coerceIn(0, shuffleOrder.lastIndex)]
                    } else if (localCurrentMediaItemIndex >= playlist.size) {
                        localCurrentMediaItemIndex = playlist.size - 1
                    }
                    unpinCurrent()
                    if (localCurrentMediaItemIndex >= 0) {
                        loadAndPlayTrackInternal(localCurrentMediaItemIndex, 0, internalPlayWhenReady)
                    } else {
                        cleanupCurrentPlayerInternal()
                    }
                }

                else -> {
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
        }
    }

    /**
     * Drops `[fromIndex, toIndex)` of already-played tracks in one pass: one precache pass, one
     * timeline notification, instead of repeating both per removed track (the cost shape of #2504).
     *
     * Indices count the UNSHUFFLED playlist, like [removeMediaItem] and [currentMediaItemIndex].
     * Only a range strictly BELOW the current track is accepted; anything else is refused rather
     * than guessed at, since getting it wrong stops playback. [onRemoved] hears back exactly once
     * either way, from inside this block — see [MediaPlayerInterface.removeMediaItems] for why.
     */
    override fun removeMediaItems(
        fromIndex: Int,
        toIndex: Int,
        onRemoved: (removedIds: List<String>) -> Unit,
    ) {
        coroutineScope.launch {
            // Everything is re-checked INSIDE the launch, on the same queue as the mutation: another
            // queued op (clearMediaItems / setMediaItem) can change the playlist between a
            // caller-thread check and this body, which is issue #2156's crash shape.
            val refused =
                fromIndex < 0 ||
                    toIndex <= fromIndex ||
                    toIndex > playlist.size ||
                    toIndex > localCurrentMediaItemIndex ||
                    // With shuffle on, the tracks before the current one in this unshuffled list are
                    // not the played ones — they are part of what is still to come.
                    internalShuffleModeEnabled ||
                    // The receiver's queue window is mapped by absolute playlist positions
                    // (CastHandoffManager.remoteToPlaylist); shifting them sends it the wrong tracks.
                    isCastActive
            if (refused) {
                onRemoved(emptyList())
                return@launch
            }

            val removed = playlist.subList(fromIndex, toIndex).toList()
            playlist.subList(fromIndex, toIndex).clear()
            localCurrentMediaItemIndex -= removed.size
            // Before anything else can run, so the caller cuts its copy of the queue in this same step.
            onRemoved(removed.map { it.mediaId })

            removed.forEach { track ->
                precachedPlayers.remove(track.mediaId)?.let { cached ->
                    cleanupPlayerInternal(cached.player)
                }
            }
            // Everything removed sits behind the current track and precache is keyed by mediaId,
            // so the window ahead needs no rebuild — clearing it here would throw away the handle
            // the next crossfade is about to use. This only tops it back up, which also restores
            // any handle an id repeated in the dropped history took with it.
            triggerPrecachingInternal()

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
        }
    }

    override fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        coroutineScope.launch {
            // Same reason as removeMediaItem (issue #2156).
            if (fromIndex !in playlist.indices || toIndex !in playlist.indices) return@launch
            val item = playlist.removeAt(fromIndex)
            playlist.add(toIndex, item)

            // Update current index
            localCurrentMediaItemIndex =
                when {
                    localCurrentMediaItemIndex == fromIndex -> {
                        toIndex
                    }
                    fromIndex < localCurrentMediaItemIndex && toIndex >= localCurrentMediaItemIndex -> {
                        localCurrentMediaItemIndex - 1
                    }
                    fromIndex > localCurrentMediaItemIndex && toIndex <= localCurrentMediaItemIndex -> {
                        localCurrentMediaItemIndex + 1
                    }
                    else -> {
                        localCurrentMediaItemIndex
                    }
                }

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            clearPrecacheExceptCurrentInternal()
            triggerPrecachingInternal()
        }
    }

    override fun moveShuffledItem(
        fromShuffledIndex: Int,
        toShuffledIndex: Int,
    ) {
        coroutineScope.launch {
            // Same reason as removeMediaItem (issue #2156): checked against the order as it is now.
            if (!internalShuffleModeEnabled) return@launch
            if (fromShuffledIndex !in shuffleOrder.indices || toShuffledIndex !in shuffleOrder.indices) return@launch
            // Only the play order moves. Playlist indices stay where they were, and with them the
            // current index and the position a running crossfade would revert to.
            shuffleOrder.add(toShuffledIndex, shuffleOrder.removeAt(fromShuffledIndex))
            rebuildShuffleIndices()
            // Put right after the current track (Move to play next, or a drag), it keeps that slot
            // the way a Play next track does; moved anywhere else, it no longer holds one.
            playlist.getOrNull(shuffleOrder[toShuffledIndex])?.let { moved ->
                if (toShuffledIndex == (shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: -1) + 1) {
                    pinnedNextItems.add(moved)
                } else {
                    pinnedNextItems.remove(moved)
                }
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            // What plays next may have changed.
            clearPrecacheExceptCurrentInternal()
            triggerPrecachingInternal()
        }
    }

    override fun clearMediaItems() {
        coroutineScope.launch {
            playlist.clear()
            localCurrentMediaItemIndex = -1
            clearShuffleOrder()
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
            cleanupCurrentPlayerInternal()
            clearAllPrecacheInternal()
        }
    }

    override fun setQueue(
        items: List<GenericMediaItem>,
        currentIndex: Int,
        startPositionMs: Long,
        reload: Boolean,
        playWhenReady: Boolean?,
    ) {
        coroutineScope.launch {
            if (currentIndex !in items.indices) return@launch
            val playingId = playlist.getOrNull(localCurrentMediaItemIndex)?.mediaId
            val nextId = playlist.getOrNull(getNextMediaItemIndex())?.mediaId
            playlist.clear()
            playlist.addAll(items)
            localCurrentMediaItemIndex = currentIndex
            pinnedNextItems.clear()
            if (internalShuffleModeEnabled) createShuffleOrder()
            playWhenReady?.let { internalPlayWhenReady = it }
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
            if (reload || items[currentIndex].mediaId != playingId) {
                loadAndPlayTrackInternal(currentIndex, startPositionMs, internalPlayWhenReady)
            } else if (playlist.getOrNull(getNextMediaItemIndex())?.mediaId != nextId) {
                // Same track playing, a different one up next: precache that instead.
                clearPrecacheExceptCurrentInternal()
                triggerPrecachingInternal()
            }
        }
    }

    override fun replaceMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        coroutineScope.launch {
            // Same reason as removeMediaItem (issue #2156).
            if (index !in playlist.indices) return@launch
            playlist[index] = mediaItem

            precachedPlayers.remove(mediaItem.mediaId)?.let { cached ->
                cleanupPlayerInternal(cached.player)
            }

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            if (index == localCurrentMediaItemIndex) {
                loadAndPlayTrackInternal(index, 0, internalPlayWhenReady)
            } else {
                triggerPrecachingInternal()
            }
        }
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? = playlist.getOrNull(index)

    override fun getCurrentMediaTimeLine(): List<GenericMediaItem> =
        if (internalShuffleModeEnabled) {
            shuffleOrder.mapNotNull { shuffledIndex -> playlist.getOrNull(shuffledIndex) }
        } else {
            playlist.toList()
        }

    override fun getUnshuffledIndex(shuffledIndex: Int): Int =
        if (internalShuffleModeEnabled) {
            shuffleOrder.getOrNull(shuffledIndex) ?: -1
        } else {
            shuffledIndex
        }

    // ========== Playback State Properties ==========

    override val isPlaying: Boolean
        get() = castRemotePlayer?.isPlaying ?: (internalState == InternalState.PLAYING)

    override val currentPosition: Long
        get() = castRemotePlayer?.currentPosition ?: cachedPosition

    override val duration: Long
        get() {
            castRemotePlayer?.let { remote ->
                return remote.duration.takeIf { it > 0 } ?: 0L
            }
            return currentPlayer?.duration ?: cachedDuration
        }

    override val bufferedPosition: Long
        get() = castRemotePlayer?.bufferedPosition ?: cachedBufferedPosition

    override val bufferedPercentage: Int
        get() {
            val dur = duration
            if (dur <= 0) return 0
            return ((bufferedPosition * 100) / dur).toInt().coerceIn(0, 100)
        }

    override val currentMediaItem: GenericMediaItem?
        get() = playlist.getOrNull(localCurrentMediaItemIndex)

    override val currentMediaItemIndex: Int
        get() = localCurrentMediaItemIndex

    override val mediaItemCount: Int
        get() = playlist.size

    override val contentPosition: Long
        get() = castRemotePlayer?.contentPosition ?: cachedPosition

    override val playbackState: Int
        get() {
            // Media3 Player.STATE_* values match PlayerConstants 1:1.
            castRemotePlayer?.let { return it.playbackState }
            return when (internalState) {
                InternalState.IDLE -> PlayerConstants.STATE_IDLE
                InternalState.PREPARING -> PlayerConstants.STATE_BUFFERING
                InternalState.READY -> PlayerConstants.STATE_READY
                InternalState.PLAYING -> PlayerConstants.STATE_READY
                InternalState.ENDED -> PlayerConstants.STATE_ENDED
                InternalState.ERROR -> PlayerConstants.STATE_IDLE
                InternalState.PAUSED -> PlayerConstants.STATE_READY
            }
        }

    // ========== Navigation ==========

    override fun hasNextMediaItem(): Boolean =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> true
            PlayerConstants.REPEAT_MODE_ALL -> true
            else ->
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos =
                        shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: -1
                    currentShufflePos >= 0 && currentShufflePos < shuffleOrder.lastIndex
                } else {
                    localCurrentMediaItemIndex < playlist.size - 1
                }
        }

    override fun hasPreviousMediaItem(): Boolean =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> true
            PlayerConstants.REPEAT_MODE_ALL -> true
            else ->
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos =
                        shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: -1
                    currentShufflePos > 0
                } else {
                    localCurrentMediaItemIndex > 0
                }
        }

    private fun getNextMediaItemIndex(): Int =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> {
                localCurrentMediaItemIndex
            }
            PlayerConstants.REPEAT_MODE_ALL -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val nextShufflePos = (currentShufflePos + 1) % shuffleOrder.size
                    shuffleOrder.getOrNull(nextShufflePos) ?: localCurrentMediaItemIndex
                } else {
                    if (localCurrentMediaItemIndex < playlist.size - 1) {
                        localCurrentMediaItemIndex + 1
                    } else {
                        0
                    }
                }
            }
            else -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val nextShufflePos = currentShufflePos + 1
                    if (nextShufflePos < shuffleOrder.size) {
                        shuffleOrder.getOrNull(nextShufflePos) ?: localCurrentMediaItemIndex
                    } else {
                        localCurrentMediaItemIndex
                    }
                } else {
                    (localCurrentMediaItemIndex + 1).coerceAtMost(playlist.size - 1)
                }
            }
        }

    private fun getPreviousMediaItemIndex(): Int =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> {
                localCurrentMediaItemIndex
            }
            PlayerConstants.REPEAT_MODE_ALL -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val prevShufflePos =
                        if (currentShufflePos > 0) {
                            currentShufflePos - 1
                        } else {
                            shuffleOrder.size - 1
                        }
                    shuffleOrder.getOrNull(prevShufflePos) ?: localCurrentMediaItemIndex
                } else {
                    if (localCurrentMediaItemIndex > 0) {
                        localCurrentMediaItemIndex - 1
                    } else {
                        playlist.size - 1
                    }
                }
            }
            else -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val prevShufflePos = currentShufflePos - 1
                    if (prevShufflePos >= 0) {
                        shuffleOrder.getOrNull(prevShufflePos) ?: localCurrentMediaItemIndex
                    } else {
                        localCurrentMediaItemIndex
                    }
                } else {
                    (localCurrentMediaItemIndex - 1).coerceAtLeast(0)
                }
            }
        }

    // ========== Playback Modes ==========

    override var shuffleModeEnabled: Boolean
        get() = internalShuffleModeEnabled
        set(value) {
            if (internalShuffleModeEnabled == value) return

            internalShuffleModeEnabled = value

            if (value) {
                createShuffleOrder()
            } else {
                clearShuffleOrder()
            }

            val mediaItemList = getShuffledMediaItemList()
            listeners.forEach { it.onShuffleModeEnabledChanged(value, mediaItemList) }
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            Logger.d(TAG, "Shuffle mode ${if (value) "enabled" else "disabled"}")
        }

    override var repeatMode: Int
        get() = internalRepeatMode
        set(value) {
            if (internalRepeatMode == value) return
            internalRepeatMode = value
            listeners.forEach { it.onRepeatModeChanged(value) }
        }

    override var playWhenReady: Boolean
        get() = internalPlayWhenReady
        set(value) {
            internalPlayWhenReady = value
            if (value) play() else pause()
        }

    override var playbackParameters: GenericPlaybackParameters
        get() = GenericPlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)
        set(value) {
            internalPlaybackSpeed = value.speed
            internalPlaybackPitch = value.pitch
            val params = PlaybackParameters(value.speed, value.pitch)
            currentPlayer?.playbackParameters = params
            fadingOutPlayer?.playbackParameters = params
            // Receiver support varies (speed only, no pitch) — best-effort while casting.
            castRemotePlayer?.let { remote ->
                runCatching { remote.playbackParameters = params }
            }
        }

    // ========== Audio Settings ==========

    override val audioSessionId: Int
        get() = currentPlayer?.audioSessionId ?: 0

    override var volume: Float
        get() = internalVolume
        set(value) {
            Logger.d(TAG, "Setting volume to $value")
            internalVolume = value.coerceIn(0f, 1f)
            castRemotePlayer?.volume = internalVolume
            // Mid-fade the ramp owns both volumes and scales them by this on its next step. Written
            // here, it put the incoming song at full level for a step at every fade start, where
            // the handler's normalization hook sets the volume.
            if (!isCrossfading) currentPlayer?.volume = internalVolume
            listeners.forEach { it.onVolumeChanged(internalVolume) }
        }

    override var albumTrackIds: Set<String>
        get() = internalAlbumTrackIds
        set(value) {
            internalAlbumTrackIds = value
        }

    override var sleepFadeFactor: Float
        get() = internalSleepFadeFactor
        set(value) {
            // Nothing to push: each player's SleepFadeAudioProcessor samples this on every buffer.
            // That is the whole point of keeping it off the `volume` line, which the crossfade ramp
            // owns and rewrites fifty times per transition.
            //
            // Deliberately not forwarded to castRemotePlayer, unlike `volume` above. While casting
            // the local pipeline produces no audio, so the processor never sees data and the fade
            // would have to be rebuilt as a volume ramp on the receiver. Decided against: the sleep
            // timer simply stops the cast device without fading.
            internalSleepFadeFactor = value.coerceIn(0f, 1f)
        }

    /**
     * Hand the equalizer a new curve.
     *
     * Nothing is pushed anywhere, for the same reason the sleep fade pushes nothing: the value is
     * sampled per buffer. A fresh [EqualizerCurve] is allocated on every call precisely so each
     * processor can decide whether to rebuild its coefficients with one reference comparison,
     * rather than diffing eleven floats on every buffer for the whole of playback.
     *
     * Not forwarded to `castRemotePlayer`: while casting, the audio is decoded on the receiver and
     * never passes through this pipeline at all, which is why the settings screen greys the
     * equalizer out for the duration.
     */
    override fun setEqualizer(
        bandsDb: List<Float>,
        preampDb: Float,
    ) {
        internalEqualizerCurve = EqualizerCurve(bandsDb, preampDb)
    }

    override var skipSilenceEnabled: Boolean
        get() = internalSkipSilence
        set(value) {
            internalSkipSilence = value
            currentPlayer?.skipSilenceEnabled = value
            fadingOutPlayer?.skipSilenceEnabled = value
        }

    // ========== Listener Management ==========

    override fun addListener(listener: MediaPlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MediaPlayerListener) {
        listeners.remove(listener)
    }

    // ========== Release Resources ==========

    override fun release() {
        // Cancel all ongoing jobs
        currentLoadJob?.cancel()
        precacheJob?.cancel()
        positionUpdateJob?.cancel()

        cancelFade()

        abandonAudioFocusInternal()
        coroutineScope.cancel()
        cleanupCurrentPlayerInternal()
        clearAllPrecacheInternal()
        outputRouter.release()
        listeners.clear()
    }

    // ========== Internal: State Transition ==========

    /**
     * State transition helper - mirrors GstreamerPlayerAdapter.transitionToState()
     */
    private fun propagatePlayerError(error: PlaybackException) {
        val genericError =
            PlayerError(
                errorCode =
                    when (error.errorCode) {
                        PlaybackException.ERROR_CODE_TIMEOUT -> PlayerConstants.ERROR_CODE_TIMEOUT
                        else -> error.errorCode
                    },
                errorCodeName = error.errorCodeName,
                message = error.message,
                refusal = error.refusalReason(),
            )
        // Media3's OkHttpDataSource folds every OkHttp failure (DNS, TLS, refused, timeout, EMFILE)
        // into ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, so the code alone says nothing: log the
        // cause chain, and the open fd / thread counts that tell a leak from a bad network.
        val causes = generateSequence(error.cause) { it.cause }.joinToString(" <- ") { "${it.javaClass.simpleName}: ${it.message}" }
        Logger.e(
            TAG,
            "Playback error ${error.errorCodeName}: ${error.message} <- $causes " +
                "(fds ${java.io.File("/proc/self/fd").list()?.size}, threads ${java.io.File("/proc/self/task").list()?.size})",
        )
        listeners.forEach { it.onPlayerError(genericError) }
        transitionToState(InternalState.ERROR)
    }

    private fun transitionToState(newState: InternalState) {
        if (internalState == newState) {
            Logger.d(TAG, "State transition ignored: already in $newState")
            return
        }

        val oldState = internalState
        internalState = newState

        Logger.d(TAG, "State: $oldState -> $newState (playWhenReady=$internalPlayWhenReady)")

        // Update cached duration from player
        currentPlayer?.let {
            val dur = it.duration
            if (dur > 0L) {
                cachedDuration = dur
            }
        }

        // Notify listeners
        when (newState) {
            InternalState.PAUSED -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            InternalState.IDLE -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_IDLE) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            InternalState.PREPARING -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_BUFFERING) }
            }

            InternalState.READY -> {
                if (internalPlayWhenReady) {
                    play()
                } else {
                    listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                    listeners.forEach { it.onIsPlayingChanged(false) }
                }
            }

            InternalState.PLAYING -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                cachedIsLoading = false
                listeners.forEach { it.onIsLoadingChanged(false) }
                listeners.forEach { it.onIsPlayingChanged(true) }
            }

            InternalState.ENDED -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_ENDED) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            // The error itself is reported by whoever failed: propagatePlayerError for the player's
            // own errors, the catch in loadAndPlayTrackInternal otherwise. Reported here as well, every
            // failure reached the listener twice, and the second time as an unexplained ERROR_UNKNOWN.
            InternalState.ERROR -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_IDLE) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }
        }
    }

    // ========== Internal: Load and Play Track ==========

    /**
     * Load and play track - mirrors GstreamerPlayerAdapter.loadAndPlayTrackInternal()
     *
     * Key difference: instead of extracting URL + creating PlayBin,
     * we create an ExoPlayer, set a MediaItem, and let MediaSourceFactory resolve the URL.
     */
    private fun loadAndPlayTrackInternal(
        index: Int,
        startPositionMs: Long,
        shouldPlay: Boolean,
    ) {
        if (index !in playlist.indices) return
        // Whatever was fading is not what was asked for now.
        cancelFade()

        val mediaItem = playlist[index]
        val videoId = mediaItem.mediaId

        // While casting, playback starts on the receiver — never on a local ExoPlayer.
        castPlaybackRouter?.takeIf { isCastActive }?.let { router ->
            currentLoadJob?.cancel()
            listeners.forEach {
                it.onMediaItemTransition(mediaItem, PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_SEEK)
            }
            router(index, startPositionMs, shouldPlay)
            return
        }

        // Cancel previous load
        currentLoadJob?.cancel()

        // The position poll skips PREPARING, so without this the new track would report the old
        // one's position until its audio starts: the seek bar flashes it, and Discord's card would
        // start the new song minutes in.
        cachedPosition = startPositionMs
        currentLoadJob =
            coroutineScope.launch {
                try {
                    transitionToState(InternalState.PREPARING)

                    // Notify media item transition
                    listeners.forEach {
                        it.onMediaItemTransition(
                            mediaItem,
                            PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                        )
                    }

                    // Use precached player if available — unless it already failed. A precache that
                    // ran into a live broadcast holds nothing playable (its progressive source threw
                    // LiveStreamDetectedException while nobody was listening), so it is dropped and
                    // the track is built afresh, which now gives it an HLS source.
                    val cachedPlayerEntry =
                        precachedPlayers.remove(videoId)?.let { cached ->
                            if (cached.player.playerError == null) {
                                cached
                            } else {
                                cached.player.release()
                                null
                            }
                        }
                    val player: ExoPlayer
                    if (cachedPlayerEntry?.player != null) {
                        Logger.d(TAG, "Using precached player for $videoId")
                        player = cachedPlayerEntry.player
                    } else {
                        Logger.d(TAG, "Creating new player for $videoId")
                        player = createExoPlayerInstance()
                        player.setMediaItem(mediaItem.toMedia3MediaItem())
                        player.prepare()
                    }

                    // === CAREFUL ORDER for ForwardingPlayer integration ===

                    // 1. Remove our active listener from old player
                    cleanupPlayerListenerInternal()
                    stopPositionUpdates()
                    // Again here: a fade can have started between the call and this launch running.
                    cancelFade()

                    // 2. Save old player reference
                    val oldPlayer = currentPlayer

                    // 3. Set new player as current
                    currentPlayer = player

                    // 4. Setup our listener on new player
                    setupPlayerListenerInternal(player)

                    // 5. Swap ForwardingPlayer delegate (moves MediaSession's listeners from old to new)
                    forwardingPlayer.swapDelegate(player)

                    // 5b. Notify MediaSession about the new media item
                    // The MediaItem was set before the swap (either during precache or above),
                    // so MediaSession's listener missed the onMediaItemTransition event.
                    // play() below will trigger onIsPlayingChanged which causes MediaSession
                    // to re-query metadata, but this explicit notify is safer and ensures
                    // the notification updates immediately even if play() is delayed.
                    forwardingPlayer.notifyMediaItemChanged()

                    // 6. NOW release old player (it has no listeners anymore)
                    if (oldPlayer != null && oldPlayer !== player) {
                        try {
                            oldPlayer.stop()
                            oldPlayer.release()
                        } catch (e: Exception) {
                            Logger.w(TAG, "Error releasing old player: ${e.message}")
                        }
                    }

                    // Audio focus is held at the adapter level (see Audio Focus section),
                    // not per-player, so it survives this swap (#2155).

                    // Apply settings
                    player.volume = internalVolume
                    player.playbackParameters = PlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)
                    player.skipSilenceEnabled = internalSkipSilence

                    // Seek if needed
                    if (startPositionMs > 0) {
                        player.seekTo(startPositionMs)
                        cachedPosition = startPositionMs
                    }

                    // Auto-play if requested
                    if (shouldPlay) {
                        requestAudioFocusInternal()
                        player.play()
                        transitionToState(InternalState.PLAYING)
                    } else {
                        player.pause()
                        transitionToState(InternalState.READY)
                    }

                    forwardingPlayer.suppressPlaybackEnded = false

                    // Start position updates
                    startPositionUpdates()

                    // Trigger precaching
                    triggerPrecachingInternal()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Logger.e(TAG, "Load track error: ${e.message}", e)
                    forwardingPlayer.suppressPlaybackEnded = false
                    transitionToState(InternalState.ERROR)
                    listeners.forEach {
                        it.onPlayerError(
                            PlayerError(
                                errorCode = 403,
                                errorCodeName = "ERROR_UNKNOWN",
                                message = "Can not extract playable URL or playback error",
                            ),
                        )
                    }
                }
            }
    }

    // ========== Internal: Player Listener Management ==========
    // Equivalent to setupPlayerListenersInternal / cleanupBusListenersInternal

    /**
     * Setup Player.Listener on the given player.
     * First removes any existing listener from the old player (like cleanupBusListenersInternal),
     * then creates and attaches a new listener to the given player.
     *
     * This is the KEY crossfade mechanism:
     * When crossfade starts, the listener is moved from old player to new player.
     * The old player's STATE_ENDED event is then ignored (no listener to handle it).
     */
    private fun setupPlayerListenerInternal(player: ExoPlayer) {
        // Clean up old listener first
        cleanupPlayerListenerInternal()

        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            Logger.d(TAG, "End of stream reached")
                            if (hasNextMediaItem()) {
                                forwardingPlayer.suppressPlaybackEnded = true
                                transitionToState(InternalState.PREPARING)
                            } else {
                                transitionToState(InternalState.ENDED)
                            }
                            handleTrackEndInternal()
                        }

                        Player.STATE_READY -> {
                            // Always clear loading state when ExoPlayer is ready
                            // (handles both initial load AND mid-playback rebuffer)
                            if (cachedIsLoading && player == currentPlayer) {
                                cachedIsLoading = false
                                listeners.forEach { it.onIsLoadingChanged(false) }
                            }
                            // Duration should be available now
                            val dur = player.duration
                            if (dur > 0) cachedDuration = dur
                            // Reset retry counter on successful playback
                            retryCount = 0
                            retryVideoId = null
                        }

                        Player.STATE_BUFFERING -> {
                            // Playback is stalled waiting for data — report buffering
                        }
                    }
                }

                // ExoPlayer pauses itself here (setHandleAudioBecomingNoisy), past pause() and its log line.
                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    // It also fires while already paused (only the reason changes); still PLAYING here means it paused us.
                    if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) {
                        val what = if (internalState == InternalState.PLAYING) "pause at ${clock(player.currentPosition)}" else "already paused"
                        Logger.i(PLAYBACK, "headphones disconnected, $what")
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (player != currentPlayer) {
                        Logger.d(TAG, "Ignoring onPlaybackStateChanged from non-current player")
                        return
                    }
                    if (isPlaying) {
                        if (internalState != InternalState.PLAYING) {
                            transitionToState(InternalState.PLAYING)
                            notifyEqualizerIntent(true)
                        }
                    } else {
                        if (internalState == InternalState.PLAYING) {
                            if (!player.playWhenReady) {
                                transitionToState(InternalState.PAUSED)
                                notifyEqualizerIntent(false)
                            }
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (player != currentPlayer) {
                        Logger.d(TAG, "Ignoring onPlayerError from non-current player")
                        return
                    }

                    // Retry for source errors (expired/invalid stream URL)
                    // ERROR_CODE_PARSING_CONTAINER_MALFORMED (3001) = server returned non-media response (e.g. HTML error page)
                    // ERROR_CODE_IO_BAD_HTTP_STATUS (2004) = HTTP 403/410 from expired URL
                    // ERROR_CODE_IO_NETWORK_CONNECTION_FAILED (2001) = connection refused
                    // ERROR_CODE_IO_FILE_NOT_FOUND (2005) = the resolver served a cache hit as a bare
                    //   media id and the cached spans were evicted (or cleared) mid-read, so
                    //   DefaultDataSource fell through to FileDataSource on a scheme-less URI. Media3
                    //   lists FileNotFoundException as non-retriable, so this layer is the only chance
                    //   to recover; reloading re-runs the resolver, which now sees an incomplete cache
                    //   and resolves a real URL.
                    val isRetryableSourceError =
                        error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND

                    val currentVideoId = playlist.getOrNull(localCurrentMediaItemIndex)?.mediaId
                    // The progressive resolver found this track to be a live broadcast. Loading it
                    // again builds an HLS source instead — see LiveStreamDetectedException. Not a
                    // retry, and not counted as one: it is a different source, not the same one again.
                    if (error.isLiveStreamDetected() && currentVideoId != null) {
                        Logger.i(TAG, "$currentVideoId is a live stream; loading it again as HLS")
                        coroutineScope.launch {
                            precachedPlayers.remove(currentVideoId)?.player?.release()
                            loadAndPlayTrackInternal(localCurrentMediaItemIndex, 0L, shouldPlay = true)
                        }
                        return
                    }
                    if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND) {
                        // Without this the crash report is a bare FileDataSourceException with
                        // nothing tying it back to a cache decision made three layers up.
                        Logger.w(
                            TAG,
                            "Cache disappeared mid-read for $currentVideoId — the resolver had served it " +
                                "as a fully cached bare media id. Retrying to resolve a real URL.",
                        )
                    }
                    if (isRetryableSourceError && currentVideoId != null) {
                        // Reset retry count if this is a different track
                        if (retryVideoId != currentVideoId) {
                            retryVideoId = currentVideoId
                            retryCount = 0
                        }
                        if (retryCount < maxRetryCount) {
                            retryCount++
                            Logger.w(TAG, "Retryable source error (attempt $retryCount/$maxRetryCount) for $currentVideoId: ${error.errorCodeName}")
                            // Snapshot the current position so the retry resumes where playback
                            // failed instead of restarting the track from the beginning.
                            val resumePositionMs = cachedPosition.coerceAtLeast(0L)
                            coroutineScope.launch {
                                try {
                                    // Invalidate cached format so ResolvingDataSource fetches a fresh URL
                                    streamRepository.invalidateFormat(currentVideoId)
                                    streamRepository.invalidateFormat("${com.simohypers.limusic.common.MERGING_DATA_TYPE.VIDEO}$currentVideoId")
                                    // Evict from precache (it may hold a stale player)
                                    precachedPlayers.remove(currentVideoId)?.player?.release()
                                    // Reload the track at the saved position
                                    loadAndPlayTrackInternal(localCurrentMediaItemIndex, resumePositionMs, shouldPlay = true)
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    Logger.e(TAG, "Retry failed: ${e.message}", e)
                                    propagatePlayerError(error)
                                }
                            }
                            return
                        }
                        Logger.e(TAG, "Max retries ($maxRetryCount) exhausted for $currentVideoId")
                    }

                    propagatePlayerError(error)
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    // ExoPlayer reports isLoading=true during normal background buffer refill,
                    // not just when playback is stalled. Only propagate loading=true when
                    // playback is actually stalled (STATE_BUFFERING) AND the player intends
                    // to play. Ignore buffering events while paused to avoid showing
                    // a loading spinner when the user has explicitly paused.
                    val isPlaybackStalled = isLoading && player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
                    val isCurrentPlayer = player == currentPlayer
                    Logger.d(TAG, "onIsLoadingChanged: isLoading=$isLoading, isPlaybackStalled=$isPlaybackStalled, isCurrentPlayer=$isCurrentPlayer")
                    if (cachedIsLoading != isPlaybackStalled && isCurrentPlayer) {
                        cachedIsLoading = isPlaybackStalled
                        listeners.forEach { it.onIsLoadingChanged(isPlaybackStalled) }
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    if (player != currentPlayer) {
                        Logger.d(TAG, "Ignoring onPlaybackStateChanged from non-current player")
                        return
                    }
                    val genericTracks = tracks.toGenericTracks()
                    listeners.forEach { it.onTracksChanged(genericTracks) }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    if (player != currentPlayer) return
                    // Fires for both in-app seeks and MediaSession/notification seeks — both land on the
                    // raw ExoPlayer. seekTo() does no manual notification, so this is the single source.
                    if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                        listeners.forEach { it.onSeeked(newPosition.positionMs) }
                    }
                }

                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    if (player != currentPlayer) {
                        Logger.d(TAG, "Ignoring onPlaybackStateChanged from non-current player")
                        return
                    }
                    val shouldBePlaying =
                        !(player.playbackState == Player.STATE_ENDED || !player.playWhenReady)
                    if (events.containsAny(
                            Player.EVENT_PLAYBACK_STATE_CHANGED,
                            Player.EVENT_PLAY_WHEN_READY_CHANGED,
                            Player.EVENT_IS_PLAYING_CHANGED,
                            Player.EVENT_POSITION_DISCONTINUITY,
                        )
                    ) {
                        if (shouldBePlaying) {
                            listeners.forEach { it.shouldOpenOrCloseEqualizerIntent(true) }
                        } else {
                            listeners.forEach { it.shouldOpenOrCloseEqualizerIntent(false) }
                        }
                    }
                }
            }

        player.addListener(listener)
        activePlayerListener = listener
    }

    /** Clean up the active player listener. It is always on [currentPlayer], the incoming track mid-fade. */
    private fun cleanupPlayerListenerInternal() {
        activePlayerListener?.let { listener ->
            currentPlayer?.removeListener(listener)
        }
        activePlayerListener = null
    }

    // ========== Internal: Player Cleanup ==========

    private fun cleanupPlayerInternal(player: ExoPlayer) {
        try {
            player.stop()
            player.release()
        } catch (e: Exception) {
            Logger.w(TAG, "Error cleaning up player: ${e.message}")
        }
    }

    private fun cleanupCurrentPlayerInternal() {
        stopPositionUpdates()
        cleanupPlayerListenerInternal()

        cancelFade()

        currentPlayer?.let { cleanupPlayerInternal(it) }
        currentPlayer = null
    }

    // ========== Internal: Track End ==========

    /**
     * Crossfade is skipped when the NEXT track will play as a video (video content with
     * the watch-video setting on — the same condition MergingMediaSourceFactory uses to
     * build a merged audio+video source). The merged source resolves two stream URLs and
     * is error-prone to prepare mid-fade, and a video should start from its first frame
     * instead of fading in under the outgoing song — so the transition takes the normal
     * (non-crossfade) path.
     */
    private fun isNextTrackVideo(): Boolean = watchVideoEnabled && playlist.getOrNull(getNextMediaItemIndex())?.isVideo() == true

    /** Same skip rule for the CURRENT track: a video should play out to its last frame instead of fading out under the incoming song. */
    private fun isCurrentTrackVideo(): Boolean = watchVideoEnabled && currentMediaItem?.isVideo() == true

    /** A live broadcast never reaches an end to fade out of, and its "remaining time" is only how far behind the live edge it is. */
    private fun isCurrentTrackLive(): Boolean = currentMediaItem?.mediaId?.let(LiveStreamRegistry::isLive) == true

    /** Nor can a live broadcast fade in: its HLS source is not one a crossfade can prepare ahead of time. */
    private fun isNextTrackLive(): Boolean = playlist.getOrNull(getNextMediaItemIndex())?.mediaId?.let(LiveStreamRegistry::isLive) == true

    /**
     * True when both this track and the next came from the album loaded in the queue, and the user
     * asked for albums to play through uninterrupted.
     *
     * Requiring *both* sides is what keeps the edges intact: the last album track into the first
     * track endless queue appended still crossfades, because that one is not in the set.
     */
    private fun isWithinAlbum(): Boolean {
        if (!skipCrossfadeInAlbum) return false
        val ids = internalAlbumTrackIds
        if (ids.isEmpty()) return false
        val current = currentMediaItem?.mediaId ?: return false
        val next = playlist.getOrNull(getNextMediaItemIndex())?.mediaId ?: return false
        return current in ids && next in ids
    }

    /**
     * Handle track end - mirrors GstreamerPlayerAdapter.handleTrackEndInternal()
     */
    private fun handleTrackEndInternal() {
        // While casting, track transitions are driven by the receiver queue.
        if (isCastActive) return
        // A crossfade starts before the end (maybeStartCrossfade); reaching it means none did.
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> {
                seekTo(localCurrentMediaItemIndex, 0)
            }

            PlayerConstants.REPEAT_MODE_ALL -> {
                if (hasNextMediaItem()) {
                    seekToNext()
                }
            }

            else -> {
                if (hasNextMediaItem()) {
                    seekToNext()
                } else {
                    notifyEqualizerIntent(false)
                }
            }
        }
    }

    // ========== Internal: Crossfade ==========
    //
    // Desktop's two-deck model (Limusic crates/player/src/lib.rs). The next track waits on its
    // precached player, prepared and paused. Close enough to the end of this one, that player
    // becomes [currentPlayer] at once and the outgoing one moves to [fadingOutPlayer], so pause,
    // seek, the position, the length and every listener callback already belong to the track the UI
    // shows. A ramp turns the two volumes over, and [finishFade] is the one teardown, reached when
    // the ramp ends or when [cancelFade] cuts it short. Nothing else touches the outgoing player.

    /**
     * The fade length in ms if one should start at [posMs], else null. Desktop's `fade_due`: never
     * longer than half the track, nor than what is left of it. Wall-clock ms, already divided by the
     * playback speed.
     */
    private fun fadeDueMs(
        posMs: Long,
        durMs: Long,
    ): Long? {
        if (durMs <= 1_000L || posMs < 0L) return null
        val left = durMs - posMs
        val fade = minOf(crossfadeDurationMs.toLong(), durMs / 2)
        return if (left <= fade) left.coerceAtLeast(0L) else null
    }

    /**
     * Called on every position tick. Everything has to line up, as on desktop: crossfade on, not in a
     * Listen Together room, not repeat-one (that track never ends), no fade running, and the next
     * track already prepared on its own player. Without that the track ends and the next one loads
     * the usual way: a fade into a player still opening plays silence over the song going out.
     */
    private fun maybeStartCrossfade(
        player: ExoPlayer,
        posMs: Long,
        durMs: Long,
    ) {
        if (!crossfadeEnabled || crossfadeSuppressed || isCrossfading || isCastActive || !player.isPlaying) return
        if (internalRepeatMode == PlayerConstants.REPEAT_MODE_ONE || !hasNextMediaItem()) return
        if (isCurrentTrackVideo() || isNextTrackVideo() || isCurrentTrackLive() || isNextTrackLive() || isWithinAlbum()) return
        val speed = internalPlaybackSpeed.coerceAtLeast(0.1f)
        val fadeMs = fadeDueMs((posMs / speed).toLong(), (durMs / speed).toLong()) ?: return
        val nextIndex = getNextMediaItemIndex()
        if (nextIndex == localCurrentMediaItemIndex) return
        val nextId = playlist.getOrNull(nextIndex)?.mediaId ?: return
        val incoming =
            precachedPlayers[nextId]?.player?.takeIf { it.playerError == null && it.playbackState == Player.STATE_READY }
                ?: return
        precachedPlayers.remove(nextId)
        startCrossfade(nextIndex, incoming, fadeMs)
    }

    /**
     * Begin the overlap: make the incoming player current, start it silent, and ramp both volumes.
     * The transition goes out now, at the start of the fade, because that is when the new track
     * becomes the one being heard: the queue, the UI, the notification and the scrobbler change over
     * with the audio rather than seconds late.
     */
    private fun startCrossfade(
        nextIndex: Int,
        incoming: ExoPlayer,
        fadeMs: Long,
    ) {
        val outgoing = currentPlayer ?: return
        Logger.i(PLAYBACK, "crossfade into ${playlist.getOrNull(nextIndex)?.mediaId} over ${fadeMs / 100 / 10.0} s")
        // Listener and MediaSession move before play(), as in loadAndPlayTrackInternal, so neither
        // misses the incoming player's first events.
        cleanupPlayerListenerInternal()
        fadingOutPlayer = outgoing
        currentPlayer = incoming
        setupPlayerListenerInternal(incoming)
        forwardingPlayer.swapDelegate(incoming)
        incoming.volume = 0f
        incoming.playbackParameters = PlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)
        incoming.skipSilenceEnabled = internalSkipSilence
        requestAudioFocusInternal()
        incoming.play()
        forwardingPlayer.suppressPlaybackEnded = false
        setCrossfading(true)
        localCurrentMediaItemIndex = nextIndex
        unpinCurrent()
        listeners.forEach {
            it.onMediaItemTransition(playlist[nextIndex], PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        }
        forwardingPlayer.notifyMediaItemChanged()
        // Bounded by the incoming track too: an overlap longer than half of it would still be
        // climbing past its own end.
        val incomingMs = (incoming.duration / internalPlaybackSpeed.coerceAtLeast(0.1f)).toLong()
        val fade = if (incoming.duration > 0) minOf(fadeMs, incomingMs / 2) else fadeMs
        // The track after this one, so the next transition has a player to fade into.
        triggerPrecachingInternal()
        crossfadeJob =
            coroutineScope.launch {
                val steps = (fade / FADE_STEP_MS).coerceAtLeast(1L)
                for (step in 1..steps) {
                    delay(FADE_STEP_MS)
                    // Linear, not desktop's equal power (cos/sin): that held the old song within 3 dB
                    // of full for the first half, so a long fade sounded like both songs at once.
                    // Linear lets it audibly fade from the start, for a ~3 dB dip mid-way.
                    val t = step.toFloat() / steps
                    outgoing.volume = internalVolume * (1f - t)
                    incoming.volume = internalVolume * t
                }
                finishFade()
            }
    }

    /**
     * The one teardown, however the fade ended: stop the outgoing track and put the incoming one at
     * the user's level. A fade cut short would otherwise leave it wherever the ramp had got to.
     */
    private fun finishFade() {
        crossfadeJob = null
        fadingOutPlayer?.let { cleanupPlayerInternal(it) }
        fadingOutPlayer = null
        currentPlayer?.volume = internalVolume
        setCrossfading(false)
    }

    /**
     * Cut a fade short, keeping the incoming track: it is the one the UI already shows. Everything
     * that changes what plays calls this first (pause, seek, skip, a load, stop, a new queue, cast).
     * Synchronous: the adapter runs on the main thread, so once this returns the outgoing track is
     * gone and the ramp writes no further volume.
     */
    private fun cancelFade() {
        if (fadingOutPlayer == null) return
        crossfadeJob?.cancel()
        finishFade()
    }

    companion object {
        /** How often a fade retouches both volumes, as on desktop. */
        private const val FADE_STEP_MS = 50L
    }

    // ========== Internal: Position Updates ==========

    /**
     * Start position updates (periodic background task).
     * Also handles crossfade detection when position approaches end.
     */
    private fun startPositionUpdates() {
        stopPositionUpdates()

        positionUpdateJob =
            coroutineScope.launch {
                while (isActive && currentPlayer != null) {
                    try {
                        currentPlayer?.let { player ->
                            if (internalState == InternalState.PLAYING ||
                                internalState == InternalState.READY ||
                                internalState == InternalState.PAUSED
                            ) {
                                // Mid-fade this is already the incoming track, the one the UI shows.
                                val pos = player.currentPosition
                                val dur = player.duration
                                val buf = player.bufferedPosition

                                if (pos >= 0) cachedPosition = pos
                                if (dur > 0) cachedDuration = dur
                                if (buf >= 0) cachedBufferedPosition = buf

                                // Out of audio and meant to be playing: a load that has not come
                                // through yet, or is waiting for a network. Checked here as well as
                                // in onIsLoadingChanged, which misses a precached player swapped in
                                // mid-load and a play pressed while the load kept retrying.
                                val stalled = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
                                if (stalled != cachedIsLoading) {
                                    cachedIsLoading = stalled
                                    listeners.forEach { it.onIsLoadingChanged(stalled) }
                                }

                                if (dur > 0 && pos > 0) maybeStartCrossfade(player, pos, dur)
                                watchFreeze(player, pos)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore query errors - don't log to avoid spam
                    }

                    delay(50) // Update every 50ms
                }
            }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // Freeze watch, fed by the position loop: where the position last moved, and when.
    private var lastMovedPos = -1L
    private var lastMovedAt = 0L
    private var freezeLogged = false

    /**
     * Logs a position that stopped while the player means to play: waiting for data, or an audio
     * output that stopped taking sound (a route change, another app's sound). Nothing else in the
     * App log shows a stop shorter than the 10 s stuck-player error.
     */
    private fun watchFreeze(
        player: ExoPlayer,
        pos: Long,
    ) {
        val now = SystemClock.elapsedRealtime()
        val meantToPlay = internalState == InternalState.PLAYING && player.playWhenReady
        if (!meantToPlay || pos != lastMovedPos) {
            if (freezeLogged) {
                val ms = now - lastMovedAt
                Logger.i(PLAYBACK, if (meantToPlay) "moving again after $ms ms" else "frozen for $ms ms, then stopped")
            }
            freezeLogged = false
            lastMovedPos = pos
            lastMovedAt = now
            return
        }
        // At 0:00 the track is still loading its start, which is not a freeze.
        if (!freezeLogged && pos > 0 && now - lastMovedAt >= FREEZE_MS) {
            freezeLogged = true
            val why = if (player.playbackState == Player.STATE_BUFFERING) "waiting for data" else "audio output not taking sound"
            Logger.i(PLAYBACK, "frozen at ${clock(pos)} ($why)")
        }
    }

    // ========== Internal: Precaching ==========

    /**
     * Trigger precaching - mirrors GstreamerPlayerAdapter.triggerPrecachingInternal()
     *
     * Creates ExoPlayer instances for upcoming tracks. Each player gets a MediaItem
     * and calls prepare(), which triggers URL resolution and buffering via MediaSourceFactory.
     */
    private fun triggerPrecachingInternal() {
        if (!precacheEnabled || playlist.isEmpty() || isCastActive) return

        cancelPrecaching()
        Logger.d(TAG, "Trigger precache")
        precacheJob =
            coroutineScope.launch {
                try {
                    // The track that will actually play next: follows shuffle and repeat. Repeat-one
                    // and the end of the queue both answer the current index, which precaches nothing.
                    val nextIndex = getNextMediaItemIndex()
                    val nextVideoId = playlist.getOrNull(nextIndex)?.mediaId
                    // A live broadcast is not precached: buffered ahead, it would start behind the
                    // live edge by however long it waited in the queue.
                    val indicesToPrecache =
                        listOfNotNull(
                            nextIndex.takeIf {
                                it != localCurrentMediaItemIndex &&
                                    nextVideoId != null &&
                                    !precachedPlayers.containsKey(nextVideoId) &&
                                    !LiveStreamRegistry.isLive(nextVideoId)
                            },
                        )

                    for (idx in indicesToPrecache) {
                        if (!isActive) break

                        val mediaItem = playlist.getOrNull(idx) ?: continue

                        try {
                            // Before prepare, so it comes ahead of the lookup lines it causes.
                            Logger.i(PLAYBACK, "preloading next: ${mediaItem.mediaId}")
                            val player = createExoPlayerInstance()
                            player.setMediaItem(mediaItem.toMedia3MediaItem())
                            player.prepare()
                            precachedPlayers[mediaItem.mediaId] = PrecachedPlayer(player, mediaItem)
                        } catch (e: Exception) {
                            Logger.e(TAG, "Precaching error for $idx: ${e.message}")
                        }

                        delay(100)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Logger.e(TAG, "Precaching error: ${e.message}")
                    }
                }
            }
    }

    private fun cancelPrecaching() {
        precacheJob?.cancel()
        precacheJob = null
    }

    private fun clearPrecacheExceptCurrentInternal() {
        Logger.d(TAG, "Clearing precache")
        precachedPlayers.entries.removeIf { (videoId, cached) ->
            if (videoId != currentMediaItem?.mediaId) {
                cleanupPlayerInternal(cached.player)
                true
            } else {
                false
            }
        }
    }

    private fun clearAllPrecacheInternal() {
        Logger.d(TAG, "Clearing all precache")
        precachedPlayers.values.forEach { cleanupPlayerInternal(it.player) }
        precachedPlayers.clear()
    }

    // ========== Internal: Notifications ==========

    private fun notifyEqualizerIntent(shouldOpen: Boolean) {
        listeners.forEach { it.shouldOpenOrCloseEqualizerIntent(shouldOpen) }
    }

    // ========== Internal: Shuffle Management ==========
    // Mirrors GstreamerPlayerAdapter shuffle management exactly

    private fun createShuffleOrder() {
        // A rebuilt order puts no track in any particular place, the pinned ones included.
        pinnedNextItems.clear()
        if (playlist.isEmpty()) {
            shuffleIndices.clear()
            shuffleOrder.clear()
            return
        }

        val indices = playlist.indices.toMutableList()

        val currentIndex = localCurrentMediaItemIndex
        if (currentIndex in indices) {
            indices.removeAt(currentIndex)
        }

        indices.shuffle()

        if (currentIndex in playlist.indices) {
            indices.add(0, currentIndex)
        }

        shuffleOrder.clear()
        shuffleOrder.addAll(indices)

        shuffleIndices.clear()
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, originalIndex ->
            shuffleIndices[originalIndex] = shuffledPos
        }

        Logger.d(TAG, "Created shuffle order: $shuffleOrder")
    }

    private fun clearShuffleOrder() {
        shuffleIndices.clear()
        shuffleOrder.clear()
        pinnedNextItems.clear()
        Logger.d(TAG, "Cleared shuffle order")
    }

    private fun insertIntoShuffleOrder(
        insertedOriginalIndex: Int,
        afterShufflePos: Int,
    ) {
        if (playlist.isEmpty() || insertedOriginalIndex !in playlist.indices) {
            return
        }

        for (i in shuffleOrder.indices) {
            if (shuffleOrder[i] >= insertedOriginalIndex) {
                shuffleOrder[i]++
            }
        }

        val insertPos = (afterShufflePos + 1).coerceIn(0, shuffleOrder.size)
        shuffleOrder.add(insertPos, insertedOriginalIndex)

        shuffleIndices.clear()
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }

        Logger.d(
            TAG,
            "Inserted index $insertedOriginalIndex into shuffle at position $insertPos (after shuffle pos $afterShufflePos)",
        )
    }

    /**
     * Gives the track just appended to [playlist] a random place after the current one, leaving
     * every other track where it was. Rebuilding the order here (createShuffleOrder) reshuffled the
     * whole queue on every Add to queue and on every track an endless queue appended, and put tracks
     * already played back in front of the listener.
     *
     * The slots right after the current track that hold [pinnedNextItems] (Play next tracks) are
     * skipped. Every other slot stays open, so a queue built by appends is still a fair shuffle, its
     * second track included.
     *
     * The full rebuild stays as the fallback for an order that no longer covers the playlist. Appends
     * can run on another thread than the one editing the order, and rebuilding is what used to repair
     * such an order on the next append.
     */
    private fun insertAppendedIntoShuffleOrder() {
        val appendedIndex = playlist.lastIndex
        if (!shuffleOrderCovers(appendedIndex)) {
            createShuffleOrder()
            return
        }
        val orderSize = shuffleOrder.size
        val currentShufflePos =
            (shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: -1).coerceIn(-1, orderSize - 1)
        var firstOpenPos = currentShufflePos + 1
        while (firstOpenPos < orderSize) {
            val item = shuffleOrder.getOrNull(firstOpenPos)?.let { playlist.getOrNull(it) } ?: break
            if (item !in pinnedNextItems) break
            firstOpenPos++
        }
        insertIntoShuffleOrder(appendedIndex, Random.nextInt(firstOpenPos, orderSize + 1) - 1)
    }

    /** Whether [shuffleOrder] is a permutation of the playlist indices `0 until size`. */
    private fun shuffleOrderCovers(size: Int): Boolean {
        if (shuffleOrder.size != size || shuffleIndices.size != size) return false
        val seen = BooleanArray(size)
        for (i in 0 until size) {
            val index = shuffleOrder.getOrNull(i) ?: return false
            if (index !in 0 until size || seen[index]) return false
            seen[index] = true
        }
        return true
    }

    /**
     * Releases the pin of the track that just became current. It has had its turn: still pinned, it
     * would hold every later append back once the queue wraps around to it again.
     */
    private fun unpinCurrent() {
        playlist.getOrNull(localCurrentMediaItemIndex)?.let { pinnedNextItems.remove(it) }
    }

    /**
     * Takes [removedOriginalIndex] — already removed from the playlist — out of the shuffled order,
     * renumbering the playlist indices after it, so every other track keeps its place in the order.
     */
    private fun removeFromShuffleOrder(removedOriginalIndex: Int) {
        shuffleOrder.remove(removedOriginalIndex)
        for (i in shuffleOrder.indices) {
            if (shuffleOrder[i] > removedOriginalIndex) {
                shuffleOrder[i]--
            }
        }
        rebuildShuffleIndices()
    }

    /** Re-derives the playlist index → shuffled position map from [shuffleOrder]. */
    private fun rebuildShuffleIndices() {
        shuffleIndices.clear()
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, originalIndex ->
            if (originalIndex in shuffleIndices.indices) {
                shuffleIndices[originalIndex] = shuffledPos
            }
        }
    }

    private fun getShuffledMediaItemList(): List<GenericMediaItem> {
        if (!internalShuffleModeEnabled || shuffleOrder.isEmpty()) {
            return playlist.toList()
        }
        return shuffleOrder.mapNotNull { playlist.getOrNull(it) }
    }

    private fun notifyTimelineChanged(reason: String) {
        val list = getShuffledMediaItemList()
        listeners.forEach { it.onTimelineChanged(list, reason) }
    }
}
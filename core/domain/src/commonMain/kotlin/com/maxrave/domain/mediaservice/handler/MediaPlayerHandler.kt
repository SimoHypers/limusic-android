package com.maxrave.domain.mediaservice.handler

import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.data.player.GenericCommandButton
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.PlayerError
import com.maxrave.domain.mediaservice.player.MediaPlayerInterface
import kotlinx.coroutines.flow.StateFlow

/**
 * Handler interface for managing media playback service operations
 */
interface MediaPlayerHandler {
    // Core player interface
    val player: MediaPlayerInterface

    // State flows
    val simpleMediaState: StateFlow<SimpleMediaState>
    val nowPlaying: StateFlow<GenericMediaItem?>
    val queueData: StateFlow<QueueData?>
    val controlState: StateFlow<ControlState>
    val nowPlayingState: StateFlow<NowPlayingTrackState>
    val sleepTimerState: StateFlow<SleepTimerState>
    val format: StateFlow<NewFormatEntity?>
    val currentSongIndex: StateFlow<Int>
    val castState: StateFlow<GenericCastState>

    // Listeners
    var onUpdateNotification: (List<GenericCommandButton>) -> Unit
    var pushPlayerError: (PlayerError) -> Unit
    var showToast: (ToastType) -> Unit

    // Playback control
    suspend fun onPlayerEvent(playerEvent: PlayerEvent)

    fun toggleRadio()

    fun toggleLike()

    fun like(liked: Boolean)

    fun resetSongAndQueue()

    // Sleep timer
    fun sleepStart(minutes: Int)

    fun sleepStop()

    // Queue (desktop Limusic's model: one list in play order, shuffle reorders it)

    /** Removes the queue row at [position]; the playing one stays. */
    fun removeMediaItem(position: Int)

    /** Plays the queue row at [index]. */
    fun playMediaItemInMediaSource(index: Int)

    /** The playing row's index in [queueData]. */
    fun currentOrderIndex(): Int

    /** Drag to reorder: moves an upcoming row from [from] to [to]. */
    fun moveInQueue(
        from: Int,
        to: Int,
    )

    fun resetCrossfade()

    /** What the next [loadMediaItem] plays: the list, where it comes from, and how. */
    fun setQueueData(queueData: QueueData.Data)

    fun getCurrentMediaItem(): GenericMediaItem?

    /** Moves the queue track at [position] to play right after the current one. */
    suspend fun moveItemToPlayNext(position: Int)

    /** Right after the playing track, behind earlier Play next adds. [from] names the album or playlist. */
    suspend fun playNext(
        tracks: List<Track>,
        from: String? = null,
    )

    /** At the end of what is playing, ahead of autoplay. [from] names the album or playlist. */
    suspend fun addToQueue(
        tracks: List<Track>,
        from: String? = null,
    )

    /** Removes the upcoming tracks added with Play next or Add to queue. */
    fun clearQueued()

    /** Puts back the queue a click replaced, where it was left ("Back to …"). */
    fun backToPrevious()

    /** A Listen Together guest's queue, as the host sent it. */
    fun playRoomQueue(
        tracks: List<Track>,
        playWhenReady: Boolean,
        positionMs: Long,
    )

    /** In a room as a guest: the host drives, so no autoplay and no saving the queue. */
    var followingRoom: Boolean

    suspend fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int? = null,
    )

    // Playback information
    fun getPlayerDuration(): Long

    fun getProgress(): Long

    fun startProgressUpdate()

    fun stopProgressUpdate()

    fun startBufferedUpdate()

    fun stopBufferedUpdate()

    // Settings
    fun mayBeNormalizeVolume()

    fun mayBeSaveRecentSong(runBlocking: Boolean = false)

    fun mayBeSavePlaybackState()

    fun mayBeRestoreQueue()

    /**
     * Restores the persisted queue and starts playing it.
     *
     * This is the background playback resumption path: a `play` command that arrives with an
     * empty player and no UI attached (a Bluetooth or headset button, a Samsung Routine, the
     * system resumption chip). Unlike [mayBeRestoreQueue] it must end with the player actually
     * playing, and it must get there quickly - see the implementation for why.
     *
     * Returns false when there is nothing to resume.
     */
    suspend fun restoreQueueAndPlay(): Boolean

    // Lifecycle
    fun shouldReleaseOnTaskRemoved(): Boolean

    fun release()
}

// State classes and enums - these would need to be defined in domain layer
sealed class RepeatState {
    data object None : RepeatState()

    data object All : RepeatState()

    data object One : RepeatState()
}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()

    data object Backward : PlayerEvent()

    data object Forward : PlayerEvent()

    data object Stop : PlayerEvent()

    data object Next : PlayerEvent()

    data object Previous : PlayerEvent()

    /**
     * Always advances to the previous media item, bypassing the 3-second
     * "seek to start of current track" behaviour of [Previous]. Used by UI
     * affordances like the artwork pager swipe.
     */
    data object SkipToPrevious : PlayerEvent()

    data object Shuffle : PlayerEvent()

    data object Repeat : PlayerEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : PlayerEvent()

    data object ToggleLike : PlayerEvent()

    data class UpdateVolume(
        val newVolume: Float,
    ) : PlayerEvent()
}

sealed class SimpleMediaState {
    data object Initial : SimpleMediaState()

    data object Ended : SimpleMediaState()

    data class Ready(
        val duration: Long,
    ) : SimpleMediaState()

    data class Loading(
        val bufferedPercentage: Int,
        val duration: Long,
    ) : SimpleMediaState()

    data class Progress(
        val progress: Long,
    ) : SimpleMediaState()

    data class Buffering(
        val position: Long,
    ) : SimpleMediaState()
}

data class ControlState(
    val isPlaying: Boolean,
    val isShuffle: Boolean,
    val repeatState: RepeatState,
    val isLiked: Boolean,
    val isNextAvailable: Boolean,
    val isPreviousAvailable: Boolean,
    val isCrossfading: Boolean,
    val volume: Float, // 0f..1f
)

data class NowPlayingTrackState(
    val mediaItem: GenericMediaItem,
    val track: Track?,
    val songEntity: SongEntity?,
) {
    fun isNotEmpty(): Boolean = this != initial()

    companion object {
        fun initial(): NowPlayingTrackState =
            NowPlayingTrackState(
                mediaItem = GenericMediaItem.EMPTY,
                track = null,
                songEntity = null,
            )
    }
}

data class SleepTimerState(
    val isDone: Boolean,
    val timeRemaining: Int,
)

data class QueueData(
    val queueState: StateSource = StateSource.STATE_CREATED,
    val data: Data = Data(),
) {
    data class Data(
        val listTracks: List<Track> = arrayListOf(),
        val firstPlayedTrack: Track? = null,
        val playlistId: String? = null,
        val playlistName: String? = null,
        val playlistType: PlaylistType? = null,
        val continuation: String? = null,
        /** Play the list shuffled (a page's Shuffle button). */
        val shuffle: Boolean = false,
        /** [continuation] is a `next` (watch queue) token, not a playlist page's. */
        val nextContinuation: Boolean = false,
        /** The track "Back to …" returns to, while it can. */
        val prevTrack: String? = null,
    )

    enum class StateSource {
        STATE_CREATED,
        STATE_INITIALIZING,
        STATE_INITIALIZED,
        STATE_ERROR,
    }

    fun isLocalPlaylist(): Boolean = this.data.playlistType == PlaylistType.LOCAL_PLAYLIST

    fun isRadio(): Boolean = this.data.playlistType == PlaylistType.RADIO

    /** True for both plain playlists and albums — an album queue is a playlist that knows its origin. */
    fun isPlaylist(): Boolean =
        this.data.playlistType == PlaylistType.PLAYLIST ||
            this.data.playlistType == PlaylistType.ALBUM
}

enum class PlaylistType {
    PLAYLIST,

    /**
     * A queue loaded from an album. Behaves exactly like [PLAYLIST] everywhere else — it exists so
     * playback can tell that the tracks were sequenced together deliberately, which is what lets
     * crossfade step aside inside an album while still fading into whatever is queued after it.
     */
    ALBUM,
    LOCAL_PLAYLIST,
    RADIO,
}

sealed class ToastType(
    extra: String? = null,
) {
    data object ExplicitContent : ToastType()

    data class PlayerError(
        val error: String,
    ) : ToastType(error)

    /** YouTube refused the video; [reason] is its own sentence for why. */
    data class PlaybackRefused(
        val reason: String,
    ) : ToastType(reason)

}
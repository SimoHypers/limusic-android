package com.maxrave.media3.service

import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.maxrave.domain.data.model.listentogether.DeviceLink
import com.maxrave.domain.data.model.listentogether.LinkRemote
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.repository.DeviceLinkRepository
import com.maxrave.logger.Logger
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The media session's player while this phone is the remote for a linked computer: the
 * notification, the lock screen, headset buttons and Android Auto show the computer's song and
 * queue and control it, and the volume keys move its volume (remote playback). Commands go out as
 * remote actions; the state is whatever the computer last reported, with the last play/pause and
 * volume sent shown until it answers.
 */
@UnstableApi
@OptIn(ExperimentalTime::class)
internal class LinkSessionPlayer(
    looper: Looper,
    private val deviceLink: DeviceLinkRepository,
    /** "Playing on <name>", for the artist line. */
    private val playingOn: (String) -> String,
) : SimpleBasePlayer(looper) {
    private var link = DeviceLink()
    private var sentPlaying: Boolean? = null

    /**
     * The volume last sent and when. It stands until the computer reports that very value, or
     * [VOLUME_SETTLE_MS] passes: a report for an earlier key press must not pull the next press
     * back to an old level.
     */
    private var sentVolume: Int? = null
    private var sentVolumeAt = 0L
    private var volumeBeforeMute = VOLUME_MAX / 2

    /** On the main thread, with every link change. */
    fun update(next: DeviceLink) {
        if (next.updatedAtMs != link.updatedAtMs) sentPlaying = null
        val reported = next.reportedVolume()
        if (reported == sentVolume) sentVolume = null
        if (next.volume != link.volume) Logger.d(TAG, "volume reported $reported, sent $sentVolume")
        link = next
        invalidateState()
    }

    private fun DeviceLink.reportedVolume() = (volume * VOLUME_MAX).roundToInt()

    private fun shownVolume(): Int = sentVolume?.takeIf { SystemClock.elapsedRealtime() - sentVolumeAt < VOLUME_SETTLE_MS } ?: link.reportedVolume()

    override fun getState(): State {
        val (rows, current) = rows()
        val volume = shownVolume()
        val playing = sentPlaying ?: link.isPlaying
        val position = link.livePositionMs(Clock.System.now().toEpochMilliseconds())
        val device = link.player?.name.orEmpty()
        return State
            .Builder()
            .setAvailableCommands(COMMANDS)
            .setPlaylist(
                rows.mapIndexed { i, t ->
                    MediaItemData
                        .Builder("$i:${t.id}")
                        .setMediaItem(t.toMediaItem(if (i == current && device.isNotEmpty()) playingOn(device) else null))
                        .setDurationUs(if (t.durationMs > 0L) t.durationMs * 1000L else C.TIME_UNSET)
                        .setIsSeekable(true)
                        .build()
                },
            ).setCurrentMediaItemIndex(current)
            .setPlaybackState(if (rows.isEmpty()) Player.STATE_IDLE else Player.STATE_READY)
            .setPlayWhenReady(playing, Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .setContentPositionMs(if (playing) PositionSupplier.getExtrapolating(position, 1f) else PositionSupplier.getConstant(position))
            .setDeviceInfo(DEVICE)
            .setDeviceVolume(volume)
            .setIsDeviceMuted(volume == 0)
            .build()
    }

    /** The computer's queue window and its playing row, or just the playing track until it sends one. */
    private fun rows(): Pair<List<RoomTrack>, Int> {
        val q = link.playerQueue
        if (q != null && q.items.isNotEmpty()) return q.items.map { it.track } to q.current.coerceIn(0, q.items.lastIndex)
        return listOfNotNull(link.track) + link.queue.takeIf { link.track != null }.orEmpty() to 0
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        deviceLink.remote(if (playWhenReady) LinkRemote.Play else LinkRemote.Pause)
        sentPlaying = playWhenReady
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> = handleSetPlayWhenReady(false)

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: @Player.Command Int,
    ): ListenableFuture<*> {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> deviceLink.remote(LinkRemote.Next)
            Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> deviceLink.remote(LinkRemote.Previous)
            Player.COMMAND_SEEK_TO_MEDIA_ITEM, Player.COMMAND_SEEK_TO_DEFAULT_POSITION -> {
                val q = link.playerQueue
                val row = q?.items?.getOrNull(mediaItemIndex)
                if (q != null && row != null && mediaItemIndex != q.current) {
                    deviceLink.remote(LinkRemote.PlayIndex(q.offset + mediaItemIndex, row.track.id))
                } else if (positionMs != C.TIME_UNSET) {
                    deviceLink.remote(LinkRemote.Seek(positionMs.coerceAtLeast(0L)))
                }
            }
            else -> if (positionMs != C.TIME_UNSET) deviceLink.remote(LinkRemote.Seek(positionMs.coerceAtLeast(0L)))
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetDeviceVolume(
        deviceVolume: Int,
        flags: Int,
    ): ListenableFuture<*> {
        val v = deviceVolume.coerceIn(0, VOLUME_MAX)
        deviceLink.remote(LinkRemote.SetVolume(v / VOLUME_MAX.toFloat()))
        sentVolume = v
        sentVolumeAt = SystemClock.elapsedRealtime()
        Logger.d(TAG, "volume sent $v")
        return Futures.immediateVoidFuture()
    }

    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> = handleSetDeviceVolume(shownVolume() + VOLUME_STEP, flags)

    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> = handleSetDeviceVolume(shownVolume() - VOLUME_STEP, flags)

    override fun handleSetDeviceMuted(
        muted: Boolean,
        flags: Int,
    ): ListenableFuture<*> {
        if (muted) volumeBeforeMute = shownVolume().takeIf { it > 0 } ?: volumeBeforeMute
        return handleSetDeviceVolume(if (muted) 0 else volumeBeforeMute, flags)
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()

    private fun RoomTrack.toMediaItem(device: String?): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(title)
                    .setArtist(listOfNotNull(artist.takeIf { it.isNotBlank() }, device).joinToString(" · "))
                    .setAlbumTitle(album.takeIf { it.isNotBlank() })
                    .setArtworkUri(cover()?.let(Uri::parse))
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build(),
            ).build()

    /** Same rule as the app's player: the computer's cover, else YouTube's own thumbnail. */
    private fun RoomTrack.cover(): String? =
        when {
            thumbnail.isNotBlank() -> thumbnail.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
            id.isNotBlank() -> "https://i.ytimg.com/vi/$id/hqdefault.jpg"
            else -> null
        }

    private companion object {
        const val TAG = "LinkSessionPlayer"
        const val VOLUME_MAX = 100

        /** How long a sent volume waits for the computer to report it. */
        const val VOLUME_SETTLE_MS = 1_500L

        /** A volume key press moves the computer's volume this much, about the phone's own step. */
        const val VOLUME_STEP = 5

        val DEVICE: DeviceInfo = DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE).setMinVolume(0).setMaxVolume(VOLUME_MAX).build()

        val COMMANDS: Player.Commands =
            Player.Commands
                .Builder()
                .addAll(
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_GET_TIMELINE,
                    Player.COMMAND_GET_METADATA,
                    Player.COMMAND_GET_DEVICE_VOLUME,
                    Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                    Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
                    Player.COMMAND_RELEASE,
                ).build()
    }
}

package com.simohypers.limusic.media3.service.mediasourcefactory

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.simohypers.limusic.common.Connectivity
import com.simohypers.limusic.domain.data.player.UnreachableStreams
import com.simohypers.limusic.logger.Logger
import java.io.IOException

/*
 * Everything Android needs to play a live broadcast. A live stream is an HLS playlist, not a file,
 * so it cannot go through the progressive source every other track uses — and which source to
 * build is decided before any URL is known. So a live track is found out the first time it is
 * resolved (LiveStreamRegistry), its progressive source fails with LiveStreamDetectedException, and
 * the track is built again, this time as an HLS source.
 */

/**
 * Thrown by the progressive stream resolver when the video it was asked for turns out to be a live
 * broadcast. By the time this is thrown [com.simohypers.limusic.domain.data.player.LiveStreamRegistry] knows
 * the video is live, so building the track again gives [MergingMediaSourceFactory] what it needs to
 * pick an HLS source.
 */
class LiveStreamDetectedException(
    val videoId: String,
) : IOException("$videoId is a live stream; it has to be rebuilt as an HLS source")

/** Whether this error, or anything that led to it, is a [LiveStreamDetectedException]. */
internal fun Throwable.isLiveStreamDetected(): Boolean =
    // take(): a cause chain is not supposed to loop, but nothing stops one from doing so.
    generateSequence(this) { it.cause }.take(8).any { it is LiveStreamDetectedException }

/**
 * Thrown by the stream resolver when YouTube's player refused the video
 * ([com.simohypers.limusic.domain.data.player.PlaybackRefusals]); [reason] is YouTube's own sentence.
 */
class StreamRefusedException(
    val videoId: String,
    val reason: String,
) : IOException("YouTube refused $videoId: $reason")

/** YouTube's reason, if this error, or anything that led to it, is a [StreamRefusedException]. */
internal fun Throwable.refusalReason(): String? =
    generateSequence(this) { it.cause }.take(8).filterIsInstance<StreamRefusedException>().firstOrNull()?.reason

/**
 * Thrown by a stream resolver when it could not ask for [videoId] at all: no network, or a lookup
 * that got no answer ([com.simohypers.limusic.domain.data.player.UnreachableStreams]).
 */
class StreamUnreachableException(
    val videoId: String,
) : IOException("Could not reach YouTube for $videoId")

/**
 * Whether this error is the network's and not the stream's: nothing reachable, a dead or reset
 * connection, a timeout. An HTTP status is an answer, so it is never one of these.
 */
internal fun Throwable.isNetworkFailure(): Boolean =
    generateSequence(this) { it.cause }.take(8).any {
        it is StreamUnreachableException ||
            (
                it is HttpDataSource.HttpDataSourceException &&
                    it !is HttpDataSource.InvalidResponseCodeException &&
                    (
                        it.reason == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            it.reason == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                    )
            )
    }

/**
 * How every player load retries.
 *
 * - A [LiveStreamDetectedException] or a [StreamRefusedException] never does: a live track is
 *   rebuilt as HLS, and a refusal was YouTube's answer, which each retry would ask for again while
 *   the listener sits in silence.
 * - A network failure retries for as long as it takes, and never reaches the player: the player
 *   plays what it has buffered, then waits in buffering, and plays on from the same spot by itself
 *   once a load gets through. Offline the resolver fails before asking anything, so every second
 *   costs nothing and the music is back within a second of the network.
 * - Anything else gets the default three retries and then reaches the player, whose own retry
 *   asks for a fresh URL (CrossfadeExoPlayerAdapter.onPlayerError).
 */
@UnstableApi
internal class PlaybackLoadErrorPolicy : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val error = loadErrorInfo.exception
        val tries = loadErrorInfo.errorCount
        return when {
            error.isLiveStreamDetected() || error.refusalReason() != null -> C.TIME_UNSET
            error.isNetworkFailure() -> {
                // The first failure of a run, and then every 30th: enough to see in the App log what
                // the network did without a line a second.
                if (tries % 30 == 1) {
                    val causes = generateSequence<Throwable>(error) { it.cause }.joinToString(" <- ") { "${it.javaClass.simpleName}: ${it.message}" }
                    Logger.w(
                        "Playback",
                        "Load failed on the network (try $tries), retrying: $causes " +
                            "(fds ${java.io.File("/proc/self/fd").list()?.size})",
                    )
                }
                if (!Connectivity.state.value.hasNetwork) 1_000L else (tries * 1_000L).coerceAtMost(5_000L)
            }
            tries > DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT -> C.TIME_UNSET
            else -> super.getRetryDelayMsFor(loadErrorInfo)
        }
    }

    // Only a fatal error (C.TIME_UNSET above) reaches the player; every other one is retried.
    override fun getMinimumLoadableRetryCount(dataType: Int): Int = Int.MAX_VALUE
}

/**
 * The URI a live track's HLS source is built with. The real playlist URL is not known when the
 * source is built, and it expires within hours, so the source asks for this placeholder and
 * [liveStreamDataSourceFactory] swaps in a freshly resolved URL every time the playlist is loaded.
 */
internal object LiveStreamPlaylistUri {
    private const val SCHEME = "limusic-live"

    // The id rides in the path, not the host: hosts may be case-folded, and video ids are case-sensitive.
    fun forVideo(videoId: String): Uri = "$SCHEME://playlist/$videoId".toUri()

    fun videoIdOf(uri: Uri): String? = uri.takeIf { it.scheme == SCHEME }?.lastPathSegment
}

/**
 * Plain HTTP for a live track, with the playlist placeholder resolved on the way. Nothing else is
 * touched: the media playlists and segments an HLS playlist points to are already real URLs. There is
 * no cache layer either — a live stream is never played twice.
 */
@UnstableApi
internal fun liveStreamDataSourceFactory(
    httpDataSourceFactory: DataSource.Factory,
    resolveLiveHlsUrl: (videoId: String) -> String?,
): DataSource.Factory =
    ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec ->
        val videoId = LiveStreamPlaylistUri.videoIdOf(dataSpec.uri) ?: return@Factory dataSpec
        if (!Connectivity.state.value.hasNetwork) throw StreamUnreachableException(videoId)
        val playlistUrl =
            resolveLiveHlsUrl(videoId)
                ?: throw if (videoId in UnreachableStreams) StreamUnreachableException(videoId) else IOException("No live HLS playlist found for $videoId")
        dataSpec.withUri(playlistUrl.toUri())
    }

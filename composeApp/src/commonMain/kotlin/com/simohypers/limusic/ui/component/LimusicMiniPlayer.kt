package com.simohypers.limusic.ui.component

import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.waiting_for_network
import org.jetbrains.compose.resources.stringResource
import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.listentogether.LinkedDevice
import com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler
import com.simohypers.limusic.domain.mediaservice.handler.RepeatState
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.ui.icon.Favorite
import com.simohypers.limusic.ui.icon.FavoriteBorder
import com.simohypers.limusic.ui.icon.Pause
import com.simohypers.limusic.ui.icon.PlayArrow
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.SkipNext
import com.simohypers.limusic.ui.screen.player.ArtworkSeekAction
import com.simohypers.limusic.ui.screen.player.computeSeekAction
import com.simohypers.limusic.ui.screen.player.deriveOrderIndex
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import org.koin.compose.koinInject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

private const val TAG = "LimusicMiniPlayer"

/** How long a swipe's seek may take to move the player before the bar gives up and slides back. */
private const val SEEK_LANDING_TIMEOUT_MS = 3_000L

/** Below this the pager counts as resting on a page rather than parked between two. */
private const val PAGE_OFFSET_EPSILON = 0.01f

/** How far an upward drag has to travel before it opens the full player. */
private val SWIPE_UP = 22.dp

/**
 * The bar above the tabs while something plays. A tap or an upward swipe opens the full player; a
 * sideways swipe pages through the queue, the neighbouring song sliding in with the finger.
 *
 * The paging mirrors the full player's artwork pager (NowPlayingScreenContent), hazards included;
 * see the comments there for the long story behind each guard.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LimusicMiniPlayer(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    sharedViewModel: SharedViewModel = koinInject(),
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
) {
    val nowPlaying by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val control by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timeline by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val waitingForNetwork by sharedViewModel.waitingForNetwork.collectAsStateWithLifecycle()
    val song = nowPlaying?.songEntity
    val remoteDevice = rememberRemoteDevice()
    val progress = if (timeline.total > 0L && timeline.current >= 0L) timeline.current.toFloat() / timeline.total else 0f
    val onBackground = MaterialTheme.colorScheme.onSurface

    // The queue and position the full player's pager reads, but our own PagerState: both pagers can
    // be composed at once, and sharing one would let each drag the other around.
    val queueDataState by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val queue by remember { derivedStateOf { queueDataState?.data?.listTracks ?: emptyList() } }
    // videoId, not mediaItem.mediaId: the latter carries the "Video" prefix for video items.
    val nowPlayingVideoId: String? = nowPlaying?.track?.videoId
    // currentOrderIndex() is not state; the keys are the things that can move it. The handler's index
    // is one: between two copies of a song only it changes, and without it the bar stayed on the old
    // copy and worked a swipe's Next or Previous out from there. See NowPlayingScreenContent for the
    // order the index and the track arrive in, and why the last shown page is passed.
    val playerOrderIndex by mediaPlayerHandler.currentSongIndex.collectAsStateWithLifecycle()
    val shownOrderIndex = remember { intArrayOf(-1) }
    val currentOrderIndex by remember(queue, nowPlayingVideoId, playerOrderIndex) {
        derivedStateOf {
            deriveOrderIndex(queue, nowPlayingVideoId, mediaPlayerHandler.currentOrderIndex(), shownOrderIndex[0])
        }
    }
    SideEffect { shownOrderIndex[0] = currentOrderIndex }
    val pagerState =
        rememberPagerState(
            initialPage = currentOrderIndex.coerceAtLeast(0),
            pageCount = { queue.size.coerceAtLeast(1) },
        )
    var isAnimatingFromPlayer by remember { mutableStateOf(false) }
    var isUserDraggingActive by remember { mutableStateOf(false) }
    // The user moved the pager and hasn't been answered yet. Set when a drag starts, cleared once a seek
    // is sent for where it ended, or when it ended back on the page it started from.
    var pendingUserSwipe by remember { mutableStateOf(false) }
    var dragStartPage by remember { mutableIntStateOf(-1) }
    // The order index a seek was sent from, while it hasn't landed. Until the index moves off it, a
    // second Next or SkipToPrevious would be worked out from a stale position, so new swipes wait.
    var seekFrom by remember { mutableStateOf<Int?>(null) }
    val latestOrderIndex by rememberUpdatedState(currentOrderIndex)
    val latestQueueSize by rememberUpdatedState(queue.size)

    // isScrollInProgress is true for our own scrolls too; isAnimatingFromPlayer tells them apart.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress to isAnimatingFromPlayer }
            .collect { (scrolling, animating) ->
                isUserDraggingActive = scrolling && !animating
                if (isUserDraggingActive && !pendingUserSwipe) {
                    pendingUserSwipe = true
                    dragStartPage = pagerState.settledPage
                }
            }
    }

    // A seek that never moves the player (nothing there, a failed load) must not hold the pager off
    // the playing song for good: give it up, and the reconciler below slides back.
    LaunchedEffect(seekFrom) {
        if (seekFrom != null) {
            delay(SEEK_LANDING_TIMEOUT_MS)
            seekFrom = null
        }
    }

    // Same track, new index (a shuffle, a trimmed radio history): jump in this very frame, so the
    // cover the new order put on the old page never shows. Only when the index moved, not just the
    // pager: between a swipe settling and its seek landing, the pager is ahead of the track on purpose.
    val lastTrackId = remember { arrayOfNulls<String>(1) }
    val lastOrderIndex = remember { intArrayOf(currentOrderIndex) }
    val jumpTarget = currentOrderIndex
    if (!isUserDraggingActive &&
        seekFrom == null &&
        jumpTarget in queue.indices &&
        jumpTarget != pagerState.currentPage &&
        jumpTarget != lastOrderIndex[0] &&
        nowPlayingVideoId != null &&
        nowPlayingVideoId == lastTrackId[0]
    ) {
        SideEffect {
            pendingUserSwipe = false
            pagerState.requestScrollToPage(jumpTarget)
        }
    }
    SideEffect {
        lastTrackId[0] = nowPlayingVideoId
        lastOrderIndex[0] = jumpTarget
    }

    // One reconciler for both directions. Whenever the pager is idle and off the playing song, either
    // the user put it there (send exactly one seek, once any earlier one has landed) or they didn't
    // (slide back). Re-run on every change of what it reads, not once per track change: a one-shot
    // sync gave up when a track change landed mid-drag, a settledPage flow swallowed a return to the
    // page it last reported, and a Next worked out from a stale index undid an earlier seek, each
    // leaving the bar on one song while another played.
    LaunchedEffect(pagerState) {
        snapshotFlow {
            listOf(
                pagerState.isScrollInProgress,
                isAnimatingFromPlayer,
                pagerState.currentPage,
                abs(pagerState.currentPageOffsetFraction) > PAGE_OFFSET_EPSILON,
                latestOrderIndex,
                latestQueueSize,
                pendingUserSwipe,
                seekFrom,
            )
        }.collect {
            if (pagerState.isScrollInProgress || isAnimatingFromPlayer) return@collect
            val orderIndex = latestOrderIndex
            if (orderIndex !in 0 until latestQueueSize) return@collect
            val page = pagerState.currentPage
            val parked = abs(pagerState.currentPageOffsetFraction) > PAGE_OFFSET_EPSILON
            if (seekFrom != null && seekFrom != orderIndex) seekFrom = null
            val seekInFlight = seekFrom != null
            // Ended where it started: nothing was chosen, so nothing is owed.
            if (pendingUserSwipe && page == dragStartPage && !parked) pendingUserSwipe = false
            if (page == orderIndex && !parked) {
                // On the playing song. A swipe made while a seek is landing stays owed until it has.
                if (!seekInFlight) pendingUserSwipe = false
                return@collect
            }
            // Off the playing song. Wait for a seek still landing; it moves the index and re-runs this.
            if (seekInFlight) return@collect
            when {
                pendingUserSwipe && !parked -> {
                    pendingUserSwipe = false
                    seekFrom = orderIndex
                    runCatching {
                        when (val action = computeSeekAction(page, orderIndex)) {
                            ArtworkSeekAction.Next -> sharedViewModel.onUIEvent(UIEvent.Next)
                            // SkipToPrevious, not Previous: a swipe means the previous song, never a restart.
                            ArtworkSeekAction.Previous -> sharedViewModel.onUIEvent(UIEvent.SkipToPrevious)
                            is ArtworkSeekAction.Skip -> mediaPlayerHandler.playMediaItemInMediaSource(action.index)
                            ArtworkSeekAction.NoOp -> Unit
                        }
                    }.onFailure { error ->
                        seekFrom = null
                        Logger.w(TAG, "Mini player seek failed: ${error.message}")
                    }
                }
                else -> {
                    // Not the user's page (a track change from elsewhere, a seek that never landed), or
                    // left between pages by a touch that stopped a slide: back onto the song that plays,
                    // or, when the user had moved it, onto their nearest page so it can be sent above.
                    val target = if (pendingUserSwipe) page else orderIndex
                    isAnimatingFromPlayer = !pendingUserSwipe
                    try {
                        // Slide to a neighbour; a far jump is not worth flinging through every cover between.
                        if (abs(target - page) <= 1) {
                            pagerState.animateScrollToPage(target)
                        } else {
                            pagerState.scrollToPage(target)
                        }
                        // A bar that is not laid out has nothing to slide through; a snap still holds.
                        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
                    } catch (e: CancellationException) {
                        // A touch took the pager over; that drag is handled on its own. Only our own
                        // cancellation ends this loop.
                        currentCoroutineContext().ensureActive()
                    } finally {
                        isAnimatingFromPlayer = false
                    }
                }
            }
            Unit
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(rounded(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onOpen)
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                    },
                    onDragEnd = { if (dragged < -SWIPE_UP.toPx()) onOpen() },
                )
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Reaches the bar's start edge, the gutter inside each page, so a song slides out under
            // the rounded edge instead of being cut off 8dp short of it.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                userScrollEnabled = control.repeatState !is RepeatState.One && queue.size >= 2,
                key = { index -> "mini_${queue.getOrNull(index)?.videoId.orEmpty()}_$index" },
            ) { page ->
                if (page == currentOrderIndex || queue.isEmpty()) {
                    MiniPlayerTrack(
                        song = song,
                        remoteDevice = remoteDevice,
                        isPlaying = control.isPlaying,
                        waitingForNetwork = waitingForNetwork,
                    )
                } else {
                    val track = queue.getOrNull(page)
                    // Mapped the way the playing song's entity is, so the cover keeps its cache key
                    // once it starts playing. toSongEntity calls last() on the thumbnails, hence the guard.
                    val neighbour =
                        remember(track) { track?.let { it.copy(thumbnails = it.thumbnails?.takeIf { t -> t.isNotEmpty() }).toSongEntity() } }
                    MiniPlayerTrack(song = neighbour, remoteDevice = null, isPlaying = false, waitingForNetwork = false)
                }
            }
            IconButton(onClick = { sharedViewModel.onUIEvent(UIEvent.ToggleLike) }) {
                Icon(
                    imageVector = if (control.isLiked) LimusicIcons.Favorite else LimusicIcons.FavoriteBorder,
                    contentDescription = null,
                    tint = if (control.isLiked) MaterialTheme.colorScheme.primary else onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { sharedViewModel.onUIEvent(UIEvent.PlayPause) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (control.isPlaying) LimusicIcons.Pause else LimusicIcons.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = { sharedViewModel.onUIEvent(UIEvent.Next) }, enabled = control.isNextAvailable) {
                Icon(
                    LimusicIcons.SkipNext,
                    contentDescription = null,
                    tint = if (control.isNextAvailable) onBackground else onBackground.copy(alpha = 0.35f),
                )
            }
        }
        // Thin progress line along the bottom edge, as on the desktop player bar.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(onBackground.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/**
 * One page of the mini player: cover, title and artists. Only the playing song's page gets the
 * "Playing on" and waiting-for-network lines; a neighbour passes null and false.
 */
@Composable
private fun MiniPlayerTrack(
    song: SongEntity?,
    remoteDevice: LinkedDevice?,
    isPlaying: Boolean,
    waitingForNetwork: Boolean,
) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(song?.thumbnails)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(song?.thumbnails)
                    .crossfade(300)
                    .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(rounded(6.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song?.title.orEmpty(),
                style = typo().titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (remoteDevice != null) {
                PlayingOnLabel(remoteDevice, playing = isPlaying, style = typo().bodySmall)
            } else if (waitingForNetwork) {
                Text(
                    text = stringResource(Res.string.waiting_for_network),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            } else {
                Text(
                    text = song?.artistName?.joinToString(", ").orEmpty(),
                    style = typo().bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

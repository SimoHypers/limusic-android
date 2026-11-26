@file:OptIn(ExperimentalMaterial3Api::class)

package com.simohypers.limusic.ui.screen.player

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kmpalette.rememberPaletteState
import com.simohypers.limusic.common.Config.MAIN_PLAYER
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.expect.ui.rememberVideoAspectRatio
import com.simohypers.limusic.extension.GradientAngle
import com.simohypers.limusic.extension.GradientOffset
import com.simohypers.limusic.extension.KeepScreenOn
import com.simohypers.limusic.extension.getColorFromPalette
import com.simohypers.limusic.extension.getScreenSizeInfo
import com.simohypers.limusic.extension.rememberIsInPipMode
import com.simohypers.limusic.ui.component.AddToPlaylistModalBottomSheet
import com.simohypers.limusic.ui.component.BusyPill
import com.simohypers.limusic.ui.component.FullscreenLyricsSheet
import com.simohypers.limusic.ui.component.InfoPlayerBottomSheet
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.QueueBottomSheet
import com.simohypers.limusic.ui.component.QueuePosition
import com.simohypers.limusic.ui.component.removeFromQueueIfStillThere
import com.simohypers.limusic.ui.icon.KeyboardArrowDown
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.home.ListenTogetherDestination
import com.simohypers.limusic.ui.navigation.destination.home.LyricsSourcesDestination
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.navigation.destination.player.FullscreenDestination
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentActions
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentImmersive
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentLimusic
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentM3Expressive
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentState
import com.simohypers.limusic.ui.screen.player.content.PlayerBackdropColor
import com.simohypers.limusic.ui.theme.ForceDarkContent
import com.simohypers.limusic.ui.screen.player.content.fullscreenCanvas
import com.simohypers.limusic.ui.screen.player.content.toAudioQualityLabel
import com.simohypers.limusic.viewModel.NowPlayingBottomSheetUIEvent
import com.simohypers.limusic.viewModel.NowPlayingBottomSheetViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

private const val TAG = "NowPlayingScreen"

// Below this the players swap the big cover for a small one (#13): a phone on its side is 340-430.
private const val SHORT_WINDOW_DP = 600

// Material3's own sheet thresholds (BottomSheetDefaults keeps them internal), so a drag during the
// slide-up settles the way a drag on the open sheet does.
private val SHEET_POSITIONAL_THRESHOLD = 56.dp
private val SHEET_VELOCITY_THRESHOLD = 125.dp
private const val SCRIM_ALPHA = .5f

// Of the player's height: well under a pixel, so the spring doesn't stop short and then jump.
private const val ENTRANCE_THRESHOLD = .0005f

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterial3Api
@Composable
fun NowPlayingScreen(
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onDismiss: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    // Created already open. Material3 turns the sheet's drag on only once its state has settled on
    // a visible value, so a sheet sliding up through show() can't be grabbed until it lands. The
    // slide-up is ours instead (entrance below), and it can be held from the first frame.
    // Reported upstream: https://issuetracker.google.com/issues/587973840 — once fixed there,
    // rememberModalBottomSheetState and its own show() can come back.
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Expanded,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    // So a rotation doesn't play the slide-up again.
    var opened by rememberSaveable { mutableStateOf(false) }
    // How far below its place the player is drawn, as a share of its height: 1 = off the screen.
    // Bounded like Material3's anchors: the spring stops at the top instead of overshooting past it.
    val entrance = remember { Animatable(if (opened) 0f else 1f, ENTRANCE_THRESHOLD).apply { updateBounds(0f, 1f) } }
    val entering by remember { derivedStateOf { entrance.value > 0f } }
    // The theme's spatial spring, the one Material3 slides its sheet with, with a threshold that
    // suits a 0..1 value.
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val entranceSpec =
        remember(spatialSpec) {
            (spatialSpec as? SpringSpec<*>)?.let { spring(it.dampingRatio, it.stiffness, ENTRANCE_THRESHOLD) } ?: spatialSpec
        }

    // Where a slide-up the finger let go of goes: back up, or down and away.
    val settleEntrance: suspend (close: Boolean, velocity: Float) -> Unit = { close, velocity ->
        entrance.animateTo(if (close) 1f else 0f, entranceSpec, velocity)
        if (close) {
            currentOnDismiss()
        } else {
            opened = true
        }
    }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.targetValue }.collectLatest { target ->
            if (target == SheetValue.Hidden) {
                // Back, or a tap beside the sheet, while it slides up: Material3 slides the sheet
                // away, so ours holds still and doesn't fight it.
                entrance.stop()
            } else if (entrance.value > 0f && !entrance.isRunning) {
                // The slide-up itself, or picking it up again if Material3's close was undone.
                settleEntrance(false, 0f)
            }
        }
    }

    val hideSheet: () -> Unit = {
        coroutineScope.launch {
            if (entrance.value > 0f) {
                settleEntrance(true, 0f)
            } else {
                sheetState.hide()
                currentOnDismiss()
            }
        }
    }

    val currentHideSheet by rememberUpdatedState(hideSheet)

    ModalBottomSheet(
        modifier =
            Modifier
                .fillMaxHeight(),
        onDismissRequest = {
            onDismiss()
        },
        // The sheet itself sits still and open; the colour is painted by the content that slides.
        containerColor = Color.Transparent,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background),
        dragHandle = {},
        // Material3 fades its scrim on the state's target, which starts out open: it would pop in.
        // It follows the slide instead. Recomposes this small scope per frame, only while sliding.
        scrimColor = Color.Black.copy(alpha = SCRIM_ALPHA * (1f - entrance.value)),
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Before the layer, so the finger is tracked against the screen, not the content it moves.
                .carryWhileEntering(entrance, sheetState, coroutineScope, settleEntrance)
                .graphicsLayer { translationY = entrance.value * size.height }
                .background(MaterialTheme.colorScheme.background),
        ) {
            NowPlayingScreenContent(
                sharedViewModel = sharedViewModel,
                navController = navController,
                // Not before it has slid all the way up, as when Material3 slid it.
                isExpanded = sheetState.currentValue == SheetValue.Expanded && !entering,
                dismissIcon = LimusicIcons.KeyboardArrowDown,
                onDismiss = {
                    currentHideSheet()
                },
            )
            BusyPill(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp))
        }
    }
}

/**
 * Only a gesture that starts while the player slides up is taken here: the finger stops it where
 * it is and a drag carries it either way; let go, it settles up or away like the sheet would. Once
 * it has landed this stands aside, and Material3's drag and the content inside work as before.
 */
private fun Modifier.carryWhileEntering(
    entrance: Animatable<Float, AnimationVector1D>,
    sheetState: SheetState,
    scope: CoroutineScope,
    settle: suspend (close: Boolean, velocity: Float) -> Unit,
): Modifier =
    pointerInput(Unit) {
        val positionalThreshold = SHEET_POSITIONAL_THRESHOLD.toPx()
        val velocityThreshold = SHEET_VELOCITY_THRESHOLD.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (entrance.value <= 0f || sheetState.targetValue == SheetValue.Hidden) return@awaitEachGesture
            val height = size.height.toFloat().coerceAtLeast(1f)
            scope.launch(start = CoroutineStart.UNDISPATCHED) { entrance.stop() }
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var travel = Offset.Zero
            var dragging = false
            try {
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val delta = change.position - change.previousPosition
                    travel += delta
                    if (!dragging && travel.getDistance() > viewConfiguration.touchSlop) dragging = true
                    // Seen before anything inside (Initial pass), so the pager, the lists and the
                    // sheet's own drag all find it taken and stand down. A tap isn't taken: it lands.
                    if (dragging) change.consume()
                    if (!change.pressed) break
                    if (dragging && delta.y != 0f) {
                        val to = entrance.value + delta.y / height
                        scope.launch(start = CoroutineStart.UNDISPATCHED) { entrance.snapTo(to) }
                    }
                }
            } finally {
                val velocity = if (dragging) tracker.calculateVelocity().y else 0f
                val close =
                    when {
                        !dragging -> false
                        velocity > velocityThreshold -> true
                        velocity < -velocityThreshold -> false
                        else -> travel.y > positionalThreshold
                    }
                // Material3 closing it meanwhile (back) has the last word.
                if (sheetState.targetValue != SheetValue.Hidden) scope.launch { settle(close, velocity / height) }
            }
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreenContent(
    sharedViewModel: SharedViewModel = koinInject(),
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
    navController: NavController,
    isExpanded: Boolean,
    dismissIcon: ImageVector,
    onDismiss: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    // ViewModel State
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    // Audio-delay correction, read here and applied ONLY to the lyric line below. The seek bar and
    // the elapsed-time readout keep the raw position: they report where the player is, while a
    // lyric reports what the ear is hearing, and those two are what the offset separates.
    val globalLyricsOffsetMs by sharedViewModel.getLyricsOffsetMs().collectAsStateWithLifecycle(0)
    // Plus this song's own nudge from the source picker.
    val lyricsOffsetMs = globalLyricsOffsetMs + (screenDataState.lyricsData?.offsetMs ?: 0)
    val castState by sharedViewModel.castState.collectAsStateWithLifecycle()
    // Immersive style's quality line under the progress bar — see NowPlayingContentState.toAudioQualityLabel.
    val formatState by sharedViewModel.format.collectAsStateWithLifecycle(initialValue = null)

    val videosOn by sharedViewModel.getVideo.collectAsStateWithLifecycle()
    val videoWanted by sharedViewModel.videoWanted.collectAsStateWithLifecycle()
    val shouldShowVideo = videosOn && videoWanted

    // Which Now Playing style renders the content layer (Settings → Now Playing style).
    val nowPlayingStyle by sharedViewModel
        .getNowPlayingStyle()
        .collectAsStateWithLifecycle(initialValue = DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY)

    // Artwork Pager state — Spotify-style horizontal swipe between queue tracks.
    // The pager wraps the Canvas + Thumbnail layers. Controller layout below stays fixed.
    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val queueDataState by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val artworkQueue by remember {
        derivedStateOf { queueDataState?.data?.listTracks ?: emptyList() }
    }
    // ⚠️ Use track.videoId (already prefix-stripped at MediaServiceHandlerImpl.kt:386).
    // Do NOT use mediaItem.mediaId — it carries the "Video" prefix for video items.
    val nowPlayingVideoId: String? = nowPlayingState?.track?.videoId
    // currentOrderIndex() is a plain getter over the player, NOT Compose state, so it is read
    // inside this remember block, keyed on what can move it: the queue, the track now playing, and
    // the handler's own index. The index key is for a queue holding one song twice: moving from one
    // copy to the other changes neither the track nor (often) the queue's value, and without it the
    // pager, the mini player and Immersive's "up next" stayed on the copy that had stopped.
    // The index moves first on a jump (the handler sets it before the player loads the new track),
    // the track first when a song ends; deriveOrderIndex checks one against the other and, in the
    // gap, keeps the page it last showed rather than sampling the outgoing track.
    val playerOrderIndex by mediaPlayerHandler.currentSongIndex.collectAsStateWithLifecycle()
    val shownOrderIndex = remember { intArrayOf(-1) }
    val currentOrderIndex by remember(artworkQueue, nowPlayingVideoId, playerOrderIndex) {
        derivedStateOf {
            deriveOrderIndex(
                queue = artworkQueue,
                nowPlayingVideoId = nowPlayingVideoId,
                playerOrderIndex = mediaPlayerHandler.currentOrderIndex(),
                previousOrderIndex = shownOrderIndex[0],
            )
        }
    }
    SideEffect { shownOrderIndex[0] = currentOrderIndex }
    // Single PagerState — the unified ArtworkPager renders BOTH the fullscreen canvas
    // background and the centered square thumbnail in each page, so we don't need two
    // pagers + state mirroring.
    val artworkPagerState =
        rememberPagerState(
            initialPage = currentOrderIndex.coerceAtLeast(0),
            pageCount = { artworkQueue.size.coerceAtLeast(1) },
        )
    var isAnimatingFromPlayer by remember { mutableStateOf(false) }
    var isUserDraggingActive by remember { mutableStateOf(false) }
    val latestQueueSize by rememberUpdatedState(artworkQueue.size)

    // ② Pager → Player: when a swipe comes to rest, play the page it rests on. `isScrollInProgress`
    // is true for the user's drag and fling and for ①'s animateScrollToPage; `isAnimatingFromPlayer`,
    // set around ①, tells them apart. Adjacent (±1) → Next/SkipToPrevious, which keep play or pause
    // as it was; a far page → playMediaItemInMediaSource.
    //
    // On the swipe's END, not on each settledPage: a run of quick flicks keeps the scroll going from
    // one to the next while settledPage still moves at every page, and a seek per settle, armed once
    // per drag, took only the first. The cover went on to the page the flicks ended on and the
    // player stayed songs behind.
    //
    // Only after a user scroll: a pager composed again (Immersive's Queue tab, where it is not
    // composed at all) holds a stale settledPage, and taking that for a swipe sent the player back to
    // the song that had just been playing.
    //
    // Against the player's index now, not this screen's, which hears of a track change only once the
    // new song's row is read: from that, Next went a page past the cover.
    LaunchedEffect(artworkPagerState) {
        snapshotFlow {
            artworkPagerState.isScrollInProgress to isAnimatingFromPlayer
        }.collect { (scrolling, animating) ->
            val wasSwiping = isUserDraggingActive
            isUserDraggingActive = scrolling && !animating
            if (!wasSwiping || isUserDraggingActive) return@collect
            val settled = artworkPagerState.settledPage
            val orderIndex = mediaPlayerHandler.currentOrderIndex()
            if (settled !in 0 until latestQueueSize || settled == orderIndex) return@collect
            runCatching {
                when (val action = computeSeekAction(settled, orderIndex)) {
                    ArtworkSeekAction.Next -> sharedViewModel.onUIEvent(UIEvent.Next)
                    // UIEvent.Previous would restart the song once it is past 3 seconds.
                    ArtworkSeekAction.Previous -> sharedViewModel.onUIEvent(UIEvent.SkipToPrevious)
                    is ArtworkSeekAction.Skip -> mediaPlayerHandler.playMediaItemInMediaSource(action.index)
                    ArtworkSeekAction.NoOp -> Unit
                }
            }.onFailure { error ->
                Logger.w(TAG, "ArtworkPager seek failed: ${error.message}")
            }
        }
    }

    // ⓪ Same track, new page: a reorder (shuffle) or a trimmed radio history moves the playing
    // track to another page without changing it. Jump in the SAME frame: ① is a LaunchedEffect and
    // only runs on the next one, so for a frame the pager drew whatever cover the new order had put
    // on its old page, or slid in from it. Plain holders, not state: they only remember what the
    // last composition saw, and writing them must not recompose.
    //
    // Only when the INDEX moved, not the pager: after a swipe settles, the seek takes a few frames
    // to change the track, and in that gap "same track, other page" holds too. Jumping then
    // snapped the pager back to the old cover before it slid forward again.
    val lastTrackId = remember { arrayOfNulls<String>(1) }
    val lastOrderIndex = remember { intArrayOf(currentOrderIndex) }
    val jumpTarget = currentOrderIndex
    if (!isUserDraggingActive &&
        jumpTarget in artworkQueue.indices &&
        jumpTarget != artworkPagerState.currentPage &&
        jumpTarget != lastOrderIndex[0] &&
        nowPlayingVideoId != null &&
        nowPlayingVideoId == lastTrackId[0]
    ) {
        SideEffect { artworkPagerState.requestScrollToPage(jumpTarget) }
    }
    SideEffect {
        lastTrackId[0] = nowPlayingVideoId
        lastOrderIndex[0] = jumpTarget
    }

    // ① Player → Pager: animate to new track when player advances.
    //
    // Keyed on the track ALONE. It used to be keyed on the queue SIZE as well, and a changing key
    // cancels the running effect — including the animateScrollToPage in flight. A cancelled scroll
    // simply stops where it is; nothing snaps it to a page afterwards, because snapping belongs to
    // the gesture path, not to a programmatic scroll. Turning on the radio appends to the queue
    // repeatedly, so the size changed again and again and cut the animation short each time,
    // leaving the pager parked between two pages. The size is still read below — just as a value,
    // not as a trigger; ③ handles the case where the queue shrinks under the current page.
    LaunchedEffect(currentOrderIndex) {
        val target = currentOrderIndex
        if (!isUserDraggingActive &&
            artworkQueue.isNotEmpty() &&
            target in 0 until artworkQueue.size &&
            target != artworkPagerState.currentPage
        ) {
            isAnimatingFromPlayer = true
            try {
                // Animate only a neighbouring page — that is a track change, and the slide IS the
                // feedback. A jump of many pages is not: trimming a radio queue's history drops the
                // current track's index by dozens without changing the track, and animating that
                // flings the pager through dozens of covers to land on the same song.
                if (abs(target - artworkPagerState.currentPage) <= 1) {
                    artworkPagerState.animateScrollToPage(target)
                } else {
                    artworkPagerState.scrollToPage(target)
                }
                // A pager off screen (Classic's Queue or Lyrics view, Immersive's Queue tab) has no
                // layout to slide through, so the slide moves nothing and the page stays on the old
                // track, its cover shown on return. A snap only sets the position, which holds.
                if (artworkPagerState.currentPage != target) artworkPagerState.scrollToPage(target)
            } finally {
                isAnimatingFromPlayer = false
            }
        }
    }

    // ③ Queue mutation guard — when queue shrinks below currentPage, scroll to last index
    // to avoid IndexOutOfBoundsException during recomposition.
    LaunchedEffect(artworkQueue.size) {
        if (artworkQueue.isNotEmpty() && artworkPagerState.currentPage >= artworkQueue.size) {
            runCatching { artworkPagerState.scrollToPage(artworkQueue.lastIndex) }
        }
    }

    // State
    val isInPipMode = rememberIsInPipMode()

    val mainScrollState = rememberScrollState()

    var showHideMiddleLayout by rememberSaveable {
        mutableStateOf(true)
    }

    var showSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showFullscreenLyrics by rememberSaveable {
        mutableStateOf(false)
    }
    val fullscreenLyricsRequested by sharedViewModel.fullscreenLyricsRequest.collectAsStateWithLifecycle()
    LaunchedEffect(fullscreenLyricsRequested) {
        // Classic's and Immersive's lyrics are a view of their own page, so they take the request themselves.
        if (fullscreenLyricsRequested &&
            nowPlayingStyle != DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY &&
            nowPlayingStyle != DataStoreManager.NOW_PLAYING_STYLE_IMMERSIVE
        ) {
            showFullscreenLyrics = true
            sharedViewModel.consumeFullscreenLyricsRequest()
        }
    }

    var showQueueBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }
    // A queue row's ⋯ in the Immersive style, whose queue is a tab of this screen rather than a
    // sheet: its position and the track that was there. The other styles' queue sheet hosts its own.
    var queueMoreFor by remember { mutableStateOf<Pair<Int, Track>?>(null) }

    var showInfoBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    // NEW: Add to Playlist state
    var showAddToPlaylistDirectly by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldShowToolbar by remember {
        mutableStateOf(false)
    }

    // Palette state
    val paletteState = rememberPaletteState()

    val startColor =
        remember {
            Animatable(Color.Black)
        }
    val endColor =
        remember {
            Animatable(Color.Black)
        }
    val gradientOffset by remember {
        mutableStateOf(GradientOffset(GradientAngle.CW135))
    }

    var spotShadowColor by remember {
        mutableStateOf(Color.White)
    }

    LaunchedEffect(screenDataState) {
        Logger.d(TAG, "ScreenDataState: $screenDataState")
        // Only a canvas that fills the page replaces it; animated artwork sits at its top.
        showHideMiddleLayout = screenDataState.fullscreenCanvas() == null
    }

    // Palette generation lives in its own NEVER-restarting effect. Keyed on screenDataState it
    // was cancelled mid-generate every time ANOTHER field of the data class arrived (canvasData,
    // lyrics, songInfo), and kmpalette parks on Loading when generate() is cancelled — palette
    // stays null, startColor stays black, and the M3E style falls back to the app seed (the
    // "canvas songs are always cyan" bug). Canvas songs hit this deterministically because the
    // canvas fetch always lands after the artwork bitmap.
    LaunchedEffect(Unit) {
        snapshotFlow { screenDataState.bitmap }
            .filterNotNull()
            .distinctUntilChanged()
            .collectLatest {
                paletteState.generate(it)
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                spotShadowColor = it.getColorFromPalette()
                startColor.animateTo(it.getColorFromPalette())
                // Lands on the same backdrop colour the fade and the area below the gradient
                // use, so the palette ramp resolves into the surface instead of a black patch.
                endColor.animateTo(PlayerBackdropColor)
            }
    }

    LaunchedEffect(spotShadowColor) {
        Logger.d(TAG, "spotShadowColor: $spotShadowColor")
    }

    var isSliding by rememberSaveable {
        mutableStateOf(false)
    }
    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }
    LaunchedEffect(key1 = timelineState, key2 = isSliding) {
        if (!isSliding) {
            sliderValue =
                if (timelineState.total > 0L) {
                    timelineState.current.toFloat() * 100 / timelineState.total.toFloat()
                } else {
                    0f
                }
        }
    }

    // ponytail: only the unused NowPlayingContentSpotify still reads this. The crossfade rainbow it
    // carried sat on a random pastel for the whole fade (it chased a hue that moved every frame).
    val sliderTrackColor = Color.White

    // Show ControlLayout Or Show Artist Badge
    var showHideControlLayout by rememberSaveable {
        mutableStateOf(true)
    }
    val controlLayoutAlpha: Float by animateFloatAsState(
        targetValue = if (showHideControlLayout) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = 500,
                easing = LinearEasing,
            ),
        label = "ControlLayoutAlpha",
    )

    var showHideJob by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(key1 = showHideJob) {
        if (!showHideJob) {
            delay(5000)
            if (mainScrollState.value == 0) showHideControlLayout = false
            showHideJob = true
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            screenDataState
        }.distinctUntilChangedBy {
            it.canvasData?.url
        }.collectLatest {
            if (it.fullscreenCanvas() != null && mainScrollState.value == 0) {
                showHideJob = false
            } else {
                showHideJob = true
                showHideControlLayout = true
            }
        }
    }

    LaunchedEffect(key1 = showHideControlLayout) {
        if (showHideControlLayout && screenDataState.fullscreenCanvas() != null && mainScrollState.value == 0) {
            showHideJob = false
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { mainScrollState.value }
            .distinctUntilChanged()
            .collect {
                if (it > 0 && !showHideControlLayout && screenDataState.fullscreenCanvas() != null) {
                    showHideJob = true
                    showHideControlLayout = true
                } else if (showHideControlLayout && it == 0 && screenDataState.fullscreenCanvas() != null) {
                    showHideJob = false
                }
            }
    }

    var currentLyricLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }

    // Canvas subtitle sync
    LaunchedEffect(timelineState, screenDataState.lyricsData?.lyrics, lyricsOffsetMs) {
        val lyrics = screenDataState.lyricsData?.lyrics
        if (lyrics == null || lyrics.syncType == "UNSYNCED" || lyrics.syncType == null) {
            currentLyricLineIndex = -1
            return@LaunchedEffect
        }
        val lines = lyrics.lines ?: return@LaunchedEffect
        val translatedLines =
            screenDataState.lyricsData
                ?.translatedLyrics
                ?.first
                ?.lines
        // What the ear is hearing right now, which is what a lyric answers to. Keyed on the offset
        // as well so dragging the setting while paused still moves the line.
        val nowMs = timelineState.current - lyricsOffsetMs
        if (nowMs > 0L) {
            lines.indices.forEach { i ->
                val startTimeMs = lines[i].startTimeMs.toLongOrNull() ?: 0L
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLongOrNull() ?: 0L
                    } else {
                        startTimeMs + 60000
                    }
                if (nowMs in startTimeMs..endTimeMs) {
                    currentLyricLineIndex = i
                }
            }
            if (lines.isNotEmpty() &&
                nowMs in 0..(lines.getOrNull(0)?.startTimeMs?.toLongOrNull() ?: 0L)
            ) {
                currentLyricLineIndex = -1
            }
        } else {
            currentLyricLineIndex = -1
        }
    }

    if (screenDataState.lyricsData != null && controllerState.isPlaying) {
        KeepScreenOn()
    }
    val twin = artworkQueue.getOrNull(currentOrderIndex)?.takeIf { it.videoId == nowPlayingVideoId }?.counterpart
    // ponytail: one height for all three styles, scaled by the font size because their controls
    // grow with it and the cover takes the cut. Raise it if a style still overflows.
    val isShort = getScreenSizeInfo().hDP < SHORT_WINDOW_DP * LocalDensity.current.fontScale
    val state =
        NowPlayingContentState(
            screenData = screenDataState,
            controllerState = controllerState,
            timelineState = timelineState,
            timelineFlow = sharedViewModel.timeline,
            castState = castState,
            shouldShowVideo = shouldShowVideo,
            artworkQueue = artworkQueue,
            currentOrderIndex = currentOrderIndex,
            artworkPagerState = artworkPagerState,
            startColor = startColor,
            endColor = endColor,
            spotShadowColor = spotShadowColor,
            gradientOffset = gradientOffset,
            sliderTrackColor = sliderTrackColor,
            sliderValue = sliderValue,
            currentLyricLineIndex = currentLyricLineIndex,
            // A short window has no room for a canvas, so nothing hides the controls for one.
            showControlLayout = showHideControlLayout || isShort,
            controlLayoutAlpha = controlLayoutAlpha,
            showHideMiddleLayout = showHideMiddleLayout || isShort,
            shouldShowToolbar = shouldShowToolbar,
            isInPipMode = isInPipMode,
            mainScrollState = mainScrollState,
            isExpanded = isExpanded,
            dismissIcon = dismissIcon,
            audioQualityLabel = formatState.toAudioQualityLabel(),
            lyricsOffsetMs = lyricsOffsetMs.toLong(),
            videoAspectRatio = rememberVideoAspectRatio(MAIN_PLAYER) ?: 16f / 9,
            // Like desktop, only while videos are on: with them off a video plays as its sound. A
            // video with no other version gets it too, to show or hide its picture.
            canSwitchVersion = videosOn && (screenDataState.isVideo || twin != null),
            isShort = isShort,
        )
    val actions =
        NowPlayingContentActions(
            onUIEvent = { sharedViewModel.onUIEvent(it) },
            onSeekToQueueIndex = { index ->
                mediaPlayerHandler.playMediaItemInMediaSource(index)
            },
            onArtworkBitmap = { sharedViewModel.setBitmap(it) },
            onSliderChange = { newValue ->
                isSliding = true
                sliderValue = newValue
            },
            onSliderChangeFinished = {
                isSliding = false
                sharedViewModel.onUIEvent(
                    UIEvent.UpdateProgress(sliderValue),
                )
            },
            onToggleControls = {
                showHideJob = true
                showHideControlLayout = !showHideControlLayout
            },
            onNavigateToArtist = {
                val song = sharedViewModel.nowPlayingState.value?.songEntity
                (
                    song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                        ?: screenDataState.songInfoData?.authorId
                )?.let { channelId ->
                    onDismiss()
                    navController.navigate(
                        ArtistDestination(
                            channelId = channelId,
                        ),
                    )
                }
            },
            onNavigateToAlbum = {
                coroutineScope.launch {
                    sharedViewModel.nowPlayingAlbumId()?.let { albumId ->
                        onDismiss()
                        navController.navigate(AlbumDestination(browseId = albumId))
                    }
                }
            },
            onOpenListenTogether = {
                onDismiss()
                navController.navigate(ListenTogetherDestination)
            },
            onShowMoreSheet = { showSheet = true },
            onShowQueue = { showQueueBottomSheet = true },
            onShowInfo = { showInfoBottomSheet = true },
            onShowAddToPlaylist = { showAddToPlaylistDirectly = true },
            onShowFullscreenLyrics = { showFullscreenLyrics = true },
            onShowVoteDialog = {},
            onLyricsOffsetChange = { sharedViewModel.setLyricsOffsetMs(it) },
            onEnterFullscreenVideo = {
                onDismiss()
                navController.navigate(FullscreenDestination)
            },
            onDismiss = onDismiss,
            onToolbarVisibilityChange = { shouldShowToolbar = it },
            onMoveQueueItem = { from, to -> mediaPlayerHandler.moveInQueue(from, to) },
            onRemoveQueueItem = { index, videoId ->
                mediaPlayerHandler.removeFromQueueIfStillThere(index, videoId)
            },
            onQueueItemMore = { index, track -> queueMoreFor = index to track },
            onSwitchVersion = {
                // Desktop's pickVersion: the side tapped is the other one. With no other version, or
                // on the one already playing, only the picture changes.
                val video = !(screenDataState.isVideo && videoWanted)
                if (twin == null || video == screenDataState.isVideo) {
                    sharedViewModel.videoWanted.value = video
                } else {
                    if (video) sharedViewModel.videoWanted.value = true
                    mediaPlayerHandler.switchVersion()
                }
            },
            onOpenLyricsSettings = {
                onDismiss()
                navController.navigate(LyricsSourcesDestination)
            },
        )

    // Below `state`/`actions`: the page is Classic's Lyrics view, which takes the same contract.
    if (showFullscreenLyrics) {
        FullscreenLyricsSheet(state = state, actions = actions) {
            showFullscreenLyrics = false
        }
    }

    // Declared AFTER the lyrics host on purpose. A rotation recreates the activity, so every open
    // sheet re-enters composition in one pass and attaches in declaration order: the lyrics page
    // opens these same sheets (its song header's menu, add to playlist) on top of itself, and
    // declaring them first would bury them under the page after the device is turned.
    if (showSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showSheet = false
            },
            navController = navController,
            onNavigateToOtherScreen = {
                onDismiss()
            },
            song = null, // Auto set now playing
            setSleepTimerEnable = true,
            onShowInfo = { showInfoBottomSheet = true },
        )
    }

    if (showQueueBottomSheet) {
        QueueBottomSheet(
            onDismiss = {
                showQueueBottomSheet = false
            },
            navController = navController,
            onNavigateToOtherScreen = {
                showQueueBottomSheet = false
                onDismiss()
            },
        )
    }

    queueMoreFor?.let { (index, track) ->
        NowPlayingBottomSheet(
            onDismiss = { queueMoreFor = null },
            navController = navController,
            song = track.toSongEntity(),
            queuePosition = QueuePosition(index, track.videoId),
            onNavigateToOtherScreen = { onDismiss() },
        )
    }

    if (showInfoBottomSheet) {
        InfoPlayerBottomSheet(
            onDismiss = {
                showInfoBottomSheet = false
            },
        )
    }

    // NEW: Add to Playlist Bottom Sheet
    if (showAddToPlaylistDirectly) {
        val viewModel: NowPlayingBottomSheetViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.resetPlaylists()
            viewModel.setSongEntity(null) // Uses current playing song
        }

        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = uiState.listLocalPlaylist,
            listYouTubePlaylist = uiState.listYouTubePlaylist,
            onDismiss = { showAddToPlaylistDirectly = false },
            onClick = { playlist ->
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(playlist.id))
                showAddToPlaylistDirectly = false
            },
            onYTPlaylistClick = { playlist ->
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist(playlist.browseId))
                showAddToPlaylistDirectly = false
            },
            videoId = uiState.songUIState.videoId,
        )
    }

    // Classic (the stored value is still SPOTIFY) is the Limusic player; the other two are SimpMusic's.
    // Classic and M3 Expressive follow light/dark; Immersive's white-on-cover look stays dark.
    when (nowPlayingStyle) {
        DataStoreManager.NOW_PLAYING_STYLE_M3_EXPRESSIVE ->
            NowPlayingContentM3Expressive(
                state = state,
                actions = actions,
            )

        DataStoreManager.NOW_PLAYING_STYLE_IMMERSIVE ->
            ForceDarkContent {
                NowPlayingContentImmersive(
                    state = state,
                    actions = actions,
                )
            }

        else ->
            NowPlayingContentLimusic(
                state = state,
                actions = actions,
            )
    }
}

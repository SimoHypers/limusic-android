package com.simohypers.limusic.ui.component

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.data.model.listentogether.LinkRemote
import com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler
import com.simohypers.limusic.domain.repository.DeviceLinkRepository
import com.simohypers.limusic.domain.mediaservice.handler.RepeatState
import com.simohypers.limusic.domain.mediaservice.queue.queueView
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.expect.ui.isLyricsBlurSupported
import com.simohypers.limusic.ui.icon.AllInclusive
import com.simohypers.limusic.ui.icon.Favorite
import com.simohypers.limusic.ui.icon.FavoriteBorder
import com.simohypers.limusic.ui.icon.KeyboardArrowDown
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.icon.PlaylistAdd
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Undo
import com.simohypers.limusic.ui.screen.player.content.FrostedCover
import com.simohypers.limusic.ui.screen.player.content.applemusic.appleMusicFadeBetween
import com.simohypers.limusic.ui.screen.player.content.applemusic.appleMusicVerticalFadeEdges
import com.simohypers.limusic.ui.theme.AppThemeContent
import com.simohypers.limusic.ui.theme.typo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.back_to
import limusic.composeapp.generated.resources.clear_queue
import limusic.composeapp.generated.resources.duration_hours_short
import limusic.composeapp.generated.resources.duration_minutes_short
import limusic.composeapp.generated.resources.empty_queue
import limusic.composeapp.generated.resources.endless_queue
import limusic.composeapp.generated.resources.now_playing
import limusic.composeapp.generated.resources.queue
import limusic.composeapp.generated.resources.queue_ends
import limusic.composeapp.generated.resources.songs_left
import limusic.composeapp.generated.resources.songs_left_one

/**
 * The queue sheet, the mini player's way into the queue: [QueueTopBar], the song playing
 * ([SongHeader]), [QueueActions] and [QueueList] on the frosted cover. The Classic player shows the
 * same parts as its own Queue view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    /** Closes whatever screen hosts this sheet, before a row's sheet opens an artist or album page. */
    onNavigateToOtherScreen: () -> Unit,
    musicServiceHandler: MediaPlayerHandler = koinInject<MediaPlayerHandler>(),
) {
    AppThemeContent {
        val scope = rememberCoroutineScope()
        val colors = rememberSurfaceDarkColors()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val nowPlaying by musicServiceHandler.nowPlaying.collectAsStateWithLifecycle()
        val control by musicServiceHandler.controlState.collectAsStateWithLifecycle()
        var nowPlayingMore by remember { mutableStateOf(false) }
        // The row whose ⋮ was tapped, as its queue position and the track that was there.
        var moreFor by remember { mutableStateOf<Pair<Int, Track>?>(null) }

        if (nowPlayingMore) {
            NowPlayingBottomSheet(
                onDismiss = { nowPlayingMore = false },
                navController = navController,
                song = null, // the song playing, as the player's own menu
                onNavigateToOtherScreen = onNavigateToOtherScreen,
            )
        }
        moreFor?.let { (index, track) ->
            NowPlayingBottomSheet(
                onDismiss = { moreFor = null },
                navController = navController,
                song = track.toSongEntity(),
                queuePosition = QueuePosition(index, track.videoId),
                onNavigateToOtherScreen = onNavigateToOtherScreen,
            )
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = colors.container,
            contentColor = colors.content,
            dragHandle = {},
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight(),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RectangleShape,
        ) {
            Box(Modifier.fillMaxSize()) {
                if (isLyricsBlurSupported()) FrostedCover(nowPlaying?.metadata?.artworkUri, wash = colors.container, washFrom = 0.62f, washTo = 0.85f)
                Column(Modifier.fillMaxSize().padding(WindowInsets.statusBars.asPaddingValues())) {
                    QueueTopBar(onClose = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    })
                    QueueActions()
                    QueueList(
                        onMore = { index, track -> moreFor = index to track },
                        bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        modifier = Modifier.weight(1f),
                    ) {
                        nowPlaying?.takeIf { it.mediaId.isNotEmpty() }?.let { item ->
                            SongHeader(
                                thumbnail = item.metadata.artworkUri,
                                title = item.metadata.title.orEmpty(),
                                artist = item.metadata.artist.orEmpty(),
                                liked = control.isLiked,
                                onLike = musicServiceHandler::toggleLike,
                                onMore = { nowPlayingMore = true },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The chevron ([onClose]), "Queue" over where it plays from and what is left, then the Autoplay switch. */
@Composable
internal fun QueueTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    musicServiceHandler: MediaPlayerHandler = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val colors = rememberSurfaceDarkColors()
    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()
    val current by musicServiceHandler.currentSongIndex.collectAsStateWithLifecycle()
    val ownAutoplay by dataStoreManager.endlessQueue.map { it == DataStoreManager.TRUE }.collectAsStateWithLifecycle(true)
    // The switch is the playing device's: the computer's own while this phone controls it.
    val deviceLink = koinInject<DeviceLinkRepository>()
    val link by deviceLink.link.collectAsStateWithLifecycle()
    val remoteAutoplay = link.playerQueue?.autoplay?.takeIf { link.controlling }
    val autoplay = remoteAutoplay ?: ownAutoplay
    val items = queueData?.data?.listTracks.orEmpty()
    val view = remember(items, current) { queueView(items, current) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(start = 4.dp, end = 8.dp)) {
        IconButton(onClick = onClose) {
            Icon(LimusicIcons.KeyboardArrowDown, contentDescription = null, tint = colors.content)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.queue), style = typo().titleMedium, color = colors.content)
            val summary = queueSummary(queueData?.data?.playlistName?.ifBlank { null }, view.leftCount, view.leftSecs)
            if (summary.isNotEmpty()) {
                Text(summary, style = typo().bodySmall, color = colors.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // A guest follows the host: no switch, no clearing, no reordering of its own.
        if (!musicServiceHandler.followingRoom && items.isNotEmpty()) {
            Text(stringResource(Res.string.endless_queue), style = typo().labelSmall, color = colors.subtitle)
            Switch(
                checked = autoplay,
                onCheckedChange = { on ->
                    if (remoteAutoplay != null) deviceLink.remote(LinkRemote.SetAutoplay(on)) else scope.launch { dataStoreManager.setEndlessQueue(on) }
                },
                modifier = Modifier.scale(0.75f),
            )
        }
    }
}

/**
 * The queue laid out like desktop's queue panel (QueueList.svelte): one list in play order. It opens
 * on the playing row with what comes next under it and what came before a scroll away above;
 * autoplay's upcoming tracks sit under their own divider, the part the switch takes away. The list
 * fades out at both ends. While the playing row is out of sight, [header] (the song playing) fades in
 * over the top of the list, and the rows under it fade out.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun QueueList(
    /** A row's ⋮: its queue position and the track there. */
    onMore: (Int, Track) -> Unit,
    /** Room kept under the list for the navigation bar, when the list runs to the screen's bottom. */
    bottomInset: Dp,
    modifier: Modifier = Modifier,
    musicServiceHandler: MediaPlayerHandler = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
    header: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colors = rememberSurfaceDarkColors()
    val density = LocalDensity.current
    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()
    val current by musicServiceHandler.currentSongIndex.collectAsStateWithLifecycle()
    val repeat by musicServiceHandler.controlState.map { it.repeatState }.collectAsStateWithLifecycle(RepeatState.None)
    val autoplay by dataStoreManager.endlessQueue.map { it == DataStoreManager.TRUE }.collectAsStateWithLifecycle(true)
    val items = queueData?.data?.listTracks.orEmpty()
    val view = remember(items, current) { queueView(items, current) }
    val canEdit = !musicServiceHandler.followingRoom
    val hasAutoplay = view.autoplayFrom < items.size

    // Lazy rows are the queue rows plus the Autoplay divider in front of autoplay's tracks.
    fun lazyIndex(i: Int) = if (hasAutoplay && i >= view.autoplayFrom) i + 1 else i

    // Keyed by track and copy, never by position: history trimmed off the front must not
    // re-key every row, or the list loses its anchor and jumps.
    val keys =
        remember(items) {
            val seen = HashMap<String, Int>(items.size)
            items.map { t ->
                val n = seen.getOrElse(t.videoId) { 0 }
                seen[t.videoId] = n + 1
                "row:${t.videoId}#$n"
            }
        }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = lazyIndex(current.coerceAtLeast(0)))
    val dragDropState =
        rememberDragDropState(listState) { from, to ->
            // Back from list positions to queue positions; the divider counts as autoplay's first row.
            fun queueIndex(i: Int) = if (hasAutoplay && i > view.autoplayFrom) i - 1 else i
            val src = queueIndex(from)
            val dst = if (hasAutoplay && to == view.autoplayFrom && src < to) to - 1 else queueIndex(to)
            musicServiceHandler.moveInQueue(src, dst)
        }
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    // The row just tapped: it is already under the finger, so the list stays put for it.
    var clicked by remember { mutableStateOf<String?>(null) }

    // Follow the play pointer: the playing row keeps its place on screen, as long as it was on
    // screen; a different queue altogether opens on its playing row.
    val playingKey = keys.getOrNull(current)
    var followed by remember { mutableStateOf(playingKey) }
    LaunchedEffect(playingKey) {
        val from = followed
        followed = playingKey
        if (playingKey == null || from == null || from == playingKey) return@LaunchedEffect
        if (clicked == playingKey) {
            clicked = null
            return@LaunchedEffect
        }
        if (from !in keys) {
            listState.scrollToItem(lazyIndex(current))
            return@LaunchedEffect
        }
        withFrameNanos { } // this frame's layout first, so the offsets are the new list's
        val visible = listState.layoutInfo.visibleItemsInfo
        val old = visible.firstOrNull { it.key == from } ?: return@LaunchedEffect
        val new = visible.firstOrNull { it.key == playingKey }
        if (new != null) listState.animateScrollBy((new.offset - old.offset).toFloat()) else listState.scrollToItem(lazyIndex(current))
    }
    val playingLazy by rememberUpdatedState(lazyIndex(current))
    val currentPlayingKey by rememberUpdatedState(playingKey)
    val away by remember {
        derivedStateOf {
            val key = currentPlayingKey
            val visible = listState.layoutInfo.visibleItemsInfo
            // Not laid out yet isn't away: the list opens on the playing row.
            key != null && visible.isNotEmpty() && dragDropState.currentIndexOfDraggedItem == null &&
                visible.none { it.key == key }
        }
    }
    val awayAbove by remember { derivedStateOf { listState.firstVisibleItemIndex > playingLazy } }
    var headerHeight by remember { mutableStateOf(72.dp) }
    // How far down the rows are cleared for the header: they fade back in just under it.
    val clear by animateDpAsState(if (away) headerHeight else 0.dp, label = "queueHeaderClear")

    Box(modifier) {
        LazyColumn(
            state = listState,
            // The top padding matches the top fade, so the playing row it opens on is clear
            // of it, with a faded glimpse of what played before above.
            contentPadding = PaddingValues(top = TOP_FADE, bottom = 72.dp + bottomInset),
            modifier =
                Modifier
                    .fillMaxSize()
                    .appleMusicVerticalFadeEdges(topFade = 0.dp, bottomFade = BOTTOM_FADE)
                    .appleMusicFadeBetween(transparentAt = clear, opaqueAt = clear + TOP_FADE)
                    .pointerInput(canEdit, current, hasAutoplay) {
                        if (!canEdit) return@pointerInput
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                // Only upcoming rows move: the playing track and the history stay put.
                                val hit =
                                    listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        offset.y.toInt() in it.offset..(it.offset + it.size)
                                    }
                                val key = hit?.key as? String
                                val index = key?.let { keys.indexOf(it) } ?: -1
                                if (index > current) dragDropState.onDragStart(offset)
                            },
                            onDrag = { change, offset ->
                                change.consume()
                                dragDropState.onDrag(offset = offset)
                                if (overscrollJob?.isActive == true) return@detectDragGesturesAfterLongPress
                                dragDropState
                                    .checkForOverScroll()
                                    .takeIf { it != 0f }
                                    ?.let {
                                        overscrollJob =
                                            scope.launch {
                                                dragDropState.state.animateScrollBy(it * 1.3f, tween(easing = FastOutLinearInEasing))
                                            }
                                    } ?: run { overscrollJob?.cancel() }
                            },
                            onDragEnd = {
                                dragDropState.onDragInterrupted(true)
                                overscrollJob?.cancel()
                            },
                            onDragCancel = {
                                dragDropState.onDragInterrupted()
                                overscrollJob?.cancel()
                            },
                        )
                    },
        ) {
            if (items.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.empty_queue),
                        style = typo().bodyMedium,
                        color = colors.subtitle,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            itemsIndexed(items.take(view.autoplayFrom), key = { i, _ -> keys[i] }) { i, track ->
                QueueRow(i, track, i == current, dragDropState, onPlay = {
                    clicked = keys[i]
                    musicServiceHandler.playMediaItemInMediaSource(i)
                }, onMore = { onMore(i, track) })
            }
            // Said once, where the list stops, rather than leaving it to look unfinished.
            if (canEdit && items.isNotEmpty() && view.leftCount == 0 && !hasAutoplay && !autoplay && repeat == RepeatState.None) {
                item(key = "ends") {
                    Text(
                        stringResource(Res.string.queue_ends),
                        style = typo().bodySmall,
                        color = colors.subtitle,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            if (hasAutoplay) {
                item(key = "autoplay") {
                    Column(Modifier.animateItem()) {
                        HorizontalDivider(color = colors.handle.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Icon(LimusicIcons.AllInclusive, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.endless_queue), style = typo().labelMedium, color = colors.subtitle)
                        }
                    }
                }
                itemsIndexed(items.drop(view.autoplayFrom), key = { j, _ -> keys[view.autoplayFrom + j] }) { j, track ->
                    val i = view.autoplayFrom + j
                    QueueRow(i, track, i == current, dragDropState, lazy = i + 1, onPlay = {
                        clicked = keys[i]
                        musicServiceHandler.playMediaItemInMediaSource(i)
                    }, onMore = { onMore(i, track) })
                }
            }
        }
        AnimatedVisibility(visible = away, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            Box(Modifier.fillMaxWidth().onSizeChanged { headerHeight = with(density) { it.height.toDp() } }) { header() }
        }
        // The way back to the playing row once the list has scrolled it off screen.
        val pillAlpha by animateFloatAsState(if (away) 1f else 0f)
        if (pillAlpha > 0f) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp + bottomInset)
                        .graphicsLayer { alpha = pillAlpha },
                onClick = { scope.launch { listState.animateScrollToItem(playingLazy) } },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 6.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        LimusicIcons.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).rotate(if (awayAbove) 180f else 0f),
                    )
                    Text(stringResource(Res.string.now_playing), style = typo().labelLarge)
                }
            }
        }
    }
}

private val TOP_FADE = 24.dp
private val BOTTOM_FADE = 64.dp

/** The song playing: cover, title and artist, then add to playlist (when given), like and its menu. */
@Composable
internal fun SongHeader(
    thumbnail: String?,
    title: String,
    artist: String,
    liked: Boolean,
    onLike: () -> Unit,
    onMore: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
) {
    val colors = rememberSurfaceDarkColors()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)) {
        AsyncImage(
            model = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            modifier = Modifier.size(56.dp).clip(rounded(8.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = typo().titleSmall, fontWeight = FontWeight.SemiBold, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, style = typo().bodySmall, color = colors.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onAddToPlaylist != null) {
            IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(40.dp)) {
                Icon(LimusicIcons.PlaylistAdd, contentDescription = null, tint = colors.content)
            }
        }
        IconButton(onClick = onLike, modifier = Modifier.size(40.dp)) {
            Icon(if (liked) LimusicIcons.Favorite else LimusicIcons.FavoriteBorder, contentDescription = null, tint = colors.content)
        }
        IconButton(onClick = onMore, modifier = Modifier.size(40.dp)) {
            Icon(LimusicIcons.MoreVert, contentDescription = null, tint = colors.content)
        }
    }
}

/** Where the queue plays from and what is left of it, as one line: "Liked songs · 12 songs left · 48 min". */
@Composable
private fun queueSummary(
    sourceName: String?,
    leftCount: Int,
    leftSecs: Int,
): String {
    val parts = mutableListOf<String>()
    sourceName?.let { parts += it }
    if (leftCount > 0) {
        parts += if (leftCount == 1) stringResource(Res.string.songs_left_one) else stringResource(Res.string.songs_left, leftCount)
        val mins = (leftSecs + 30) / 60
        val hours = mins / 60
        val time =
            listOfNotNull(
                if (hours > 0) stringResource(Res.string.duration_hours_short, hours) else null,
                if (mins % 60 > 0) stringResource(Res.string.duration_minutes_short, mins % 60) else null,
            ).joinToString(" ")
        if (time.isNotEmpty()) parts += time
    }
    return parts.joinToString(" · ")
}

/** "Back to …" and Clear queue, only when one of them applies. */
@Composable
internal fun QueueActions(musicServiceHandler: MediaPlayerHandler = koinInject()) {
    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()
    val current by musicServiceHandler.currentSongIndex.collectAsStateWithLifecycle()
    val items = queueData?.data?.listTracks.orEmpty()
    val queued = remember(items, current) { queueView(items, current).queued }
    val prevTrack = queueData?.data?.prevTrack
    // Only what was added by hand: Play next and Add to queue, not the playlist.
    val canClear = !musicServiceHandler.followingRoom && queued > 0
    if (prevTrack == null && !canClear) return
    val colors = rememberSurfaceDarkColors()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
        // Clicking a song throws the queue away; the one just played is in the queue kept.
        if (prevTrack != null) {
            TextButton(onClick = musicServiceHandler::backToPrevious, modifier = Modifier.weight(1f, fill = false)) {
                Icon(LimusicIcons.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.subtitle)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(Res.string.back_to, prevTrack),
                    style = typo().labelLarge,
                    color = colors.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (canClear) {
            TextButton(onClick = musicServiceHandler::clearQueued) {
                Text(stringResource(Res.string.clear_queue), style = typo().labelLarge, color = colors.subtitle)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.QueueRow(
    index: Int,
    track: Track,
    playing: Boolean,
    dragDropState: DragDropState,
    lazy: Int = index,
    onPlay: () -> Unit,
    onMore: () -> Unit,
) {
    DraggableItem(dragDropState = dragDropState, index = lazy, modifier = Modifier) {
        // The tint goes on a wrapper: the row applies the modifier it is given twice, so padding
        // and a background passed in there come out doubled, with strips down the sides.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(rounded(8.dp))
                .then(if (playing) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) else Modifier),
        ) {
            SongFullWidthItems(
                track = track,
                isPlaying = playing,
                onClickListener = { onPlay() },
                onMoreClickListener = { onMore() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

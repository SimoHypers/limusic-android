package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.domain.mediaservice.queue.queueView
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.ui.icon.AllInclusive
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Undo
import com.maxrave.simpmusic.ui.theme.AppThemeContent
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.back_to
import simpmusic.composeapp.generated.resources.clear_queue
import simpmusic.composeapp.generated.resources.duration_hours_short
import simpmusic.composeapp.generated.resources.duration_minutes_short
import simpmusic.composeapp.generated.resources.empty_queue
import simpmusic.composeapp.generated.resources.endless_queue
import simpmusic.composeapp.generated.resources.now_playing
import simpmusic.composeapp.generated.resources.playing_from
import simpmusic.composeapp.generated.resources.queue
import simpmusic.composeapp.generated.resources.queue_ends
import simpmusic.composeapp.generated.resources.songs_left
import simpmusic.composeapp.generated.resources.songs_left_one

/**
 * The queue, laid out like desktop's queue panel (QueueList.svelte): where it plays from, what is
 * left and the Autoplay switch on top, then one list in play order. It opens on the playing row with
 * what comes next under it and what came before a scroll away above; autoplay's upcoming tracks sit
 * under their own divider, the part the switch takes away.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    /** Closes whatever screen hosts this sheet, before a row's sheet opens an artist or album page. */
    onNavigateToOtherScreen: () -> Unit,
    musicServiceHandler: MediaPlayerHandler = koinInject<MediaPlayerHandler>(),
    dataStoreManager: DataStoreManager = koinInject(),
) {
    AppThemeContent {
        val scope = rememberCoroutineScope()
        val colors = rememberSurfaceDarkColors()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()
        val current by musicServiceHandler.currentSongIndex.collectAsStateWithLifecycle()
        val repeat by musicServiceHandler.controlState.map { it.repeatState }.collectAsStateWithLifecycle(RepeatState.None)
        val autoplay by dataStoreManager.endlessQueue.map { it == DataStoreManager.TRUE }.collectAsStateWithLifecycle(true)
        val items = queueData?.data?.listTracks.orEmpty()
        val view = remember(items, current) { queueView(items, current) }
        // A guest follows the host: no switch, no clearing, no reordering of its own.
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
        // The row whose ⋮ was tapped, as its queue position and the track that was there.
        var moreFor by remember { mutableStateOf<Pair<Int, Track>?>(null) }
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
                key != null && dragDropState.currentIndexOfDraggedItem == null &&
                    listState.layoutInfo.visibleItemsInfo.none { it.key == key }
            }
        }
        val awayAbove by remember { derivedStateOf { listState.firstVisibleItemIndex > playingLazy } }

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
            Column(Modifier.fillMaxSize().padding(WindowInsets.statusBars.asPaddingValues())) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    IconButton(onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }) {
                        Icon(SimpIcons.KeyboardArrowDown, contentDescription = null, tint = colors.content)
                    }
                    Text(stringResource(Res.string.queue), style = typo().titleMedium, color = colors.content)
                }
                if (items.isNotEmpty()) {
                    QueueHeader(
                        sourceName = queueData?.data?.playlistName?.ifBlank { null },
                        leftCount = view.leftCount,
                        leftSecs = view.leftSecs,
                        autoplay = autoplay,
                        prevTrack = queueData?.data?.prevTrack,
                        queued = view.queued,
                        canEdit = canEdit,
                        onAutoplay = { on -> scope.launch { dataStoreManager.setEndlessQueue(on) } },
                        onBack = musicServiceHandler::backToPrevious,
                        onClear = musicServiceHandler::clearQueued,
                    )
                }
                HorizontalDivider(color = colors.handle.copy(alpha = 0.5f))
                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 4.dp, bottom = 72.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                        modifier =
                            Modifier.fillMaxSize().pointerInput(canEdit, current, hasAutoplay) {
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
                            }, onMore = { moreFor = i to track })
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
                                        Icon(SimpIcons.AllInclusive, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(16.dp))
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
                                }, onMore = { moreFor = i to track })
                            }
                        }
                    }
                    // The way back to the playing row once the list has scrolled it off screen.
                    val pillAlpha by animateFloatAsState(if (away) 1f else 0f)
                    if (pillAlpha > 0f) {
                        Surface(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
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
                                    SimpIcons.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp).rotate(if (awayAbove) 180f else 0f),
                                )
                                Text(stringResource(Res.string.now_playing), style = typo().labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Playing from", what is left, the Autoplay switch; then "Back to …" and Clear queue when they apply. */
@Composable
private fun QueueHeader(
    sourceName: String?,
    leftCount: Int,
    leftSecs: Int,
    autoplay: Boolean,
    prevTrack: String?,
    queued: Int,
    canEdit: Boolean,
    onAutoplay: (Boolean) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    val colors = rememberSurfaceDarkColors()
    Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (sourceName != null) {
                    Text(stringResource(Res.string.playing_from), style = typo().labelSmall, color = colors.subtitle)
                    Text(
                        sourceName,
                        style = typo().titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (leftCount > 0) {
                    val count = if (leftCount == 1) stringResource(Res.string.songs_left_one) else stringResource(Res.string.songs_left, leftCount)
                    val mins = (leftSecs + 30) / 60
                    val hours = mins / 60
                    val time =
                        listOfNotNull(
                            if (hours > 0) stringResource(Res.string.duration_hours_short, hours) else null,
                            if (mins % 60 > 0) stringResource(Res.string.duration_minutes_short, mins % 60) else null,
                        ).joinToString(" ")
                    Text(
                        listOf(count, time).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = typo().bodySmall,
                        color = colors.subtitle,
                        maxLines = 1,
                    )
                }
            }
            if (canEdit) {
                Text(stringResource(Res.string.endless_queue), style = typo().labelMedium, color = colors.subtitle)
                Spacer(Modifier.width(8.dp))
                Switch(checked = autoplay, onCheckedChange = onAutoplay)
            }
        }
        if (prevTrack != null || (canEdit && queued > 0)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                // Clicking a song throws the queue away; the one just played is in the queue kept.
                if (prevTrack != null) {
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f, fill = false)) {
                        Icon(SimpIcons.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.subtitle)
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
                // Only what was added by hand: Play next and Add to queue, not the playlist.
                if (canEdit && queued > 0) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(Res.string.clear_queue), style = typo().labelLarge, color = colors.subtitle)
                    }
                }
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
                .clip(RoundedCornerShape(8.dp))
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

package com.simohypers.limusic.ui.screen.library

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.PaddingValues
import org.jetbrains.compose.resources.pluralStringResource
import com.simohypers.limusic.ui.component.CollectionAuthorLine
import com.simohypers.limusic.ui.component.CollectionHero
import com.simohypers.limusic.ui.component.CollectionTitle
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.playlist
import limusic.composeapp.generated.resources.new_playlist_where_device
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.simohypers.limusic.common.LOCAL_PLAYLIST_ID
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.utils.PlaylistSort
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.Platform
import com.simohypers.limusic.extension.barBlurStyle
import com.simohypers.limusic.getPlatform
import com.simohypers.limusic.ui.component.CollectionDownloadButton
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.DraggableItem
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.LoadingDialog
import com.simohypers.limusic.ui.component.LocalPlaylistMenu
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.PlaylistSortSheet
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.SearchBarEnter
import com.simohypers.limusic.ui.component.SearchBarExit
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.SuggestItems
import com.simohypers.limusic.ui.component.painterPlaylistThumbnail
import com.simohypers.limusic.ui.component.pillSurface
import com.simohypers.limusic.ui.component.rememberDragDropState
import com.simohypers.limusic.ui.component.selection.TrackSelectionBar
import com.simohypers.limusic.ui.component.selection.keyedRows
import com.simohypers.limusic.ui.component.selection.rememberTrackSelection
import com.simohypers.limusic.ui.component.selection.rowKey
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Sort
import com.simohypers.limusic.ui.icon.TipsAndUpdates
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.viewModel.ListSort
import com.simohypers.limusic.viewModel.LocalPlaylistUIEvent
import com.simohypers.limusic.viewModel.LocalPlaylistViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.custom_order
import limusic.composeapp.generated.resources.reload
import limusic.composeapp.generated.resources.search
import limusic.composeapp.generated.resources.sort_by
import limusic.composeapp.generated.resources.suggest
import limusic.composeapp.generated.resources.your_playlist

private const val TAG = "LocalPlaylistScreen"

@ExperimentalFoundationApi
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoroutinesApi::class,
)
@Composable
fun LocalPlaylistScreen(
    id: Long,
    sharedViewModel: SharedViewModel = koinInject(),
    viewModel: LocalPlaylistViewModel = koinViewModel(),
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val aiPainter = rememberVectorPainter(LimusicIcons.TipsAndUpdates)
    val limit = 1.5f
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progressAnimated by transition.animateFloat(
        initialValue = -limit,
        targetValue = limit,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmer",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation",
    )

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    val download by viewModel.download.collectAsStateWithLifecycle()
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestions by rememberSaveable { mutableStateOf(false) }

    val selection = rememberTrackSelection()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    var shouldShowSuggestButton by rememberSaveable { mutableStateOf(false) }

    val playingTrack by sharedViewModel.nowPlayingState
        .mapLatest {
            it?.songEntity
        }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val suggestedTracks by viewModel.uiState.map { it.suggestions?.songs ?: emptyList() }.collectAsState(initial = emptyList())
    val suggestionsLoading by viewModel.loading.collectAsStateWithLifecycle()
    var firstTimeGetLocalPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var currentItem by remember {
        mutableStateOf<SongEntity?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var menuRequest by remember { mutableIntStateOf(0) }

    var sortBottomSheetShow by remember {
        mutableStateOf(false)
    }

    var changingOrder by remember {
        mutableStateOf(false)
    }
    // Dragging rows and picking them would fight over the same touches.
    LaunchedEffect(changingOrder) { if (changingOrder) selection.exit() }
    // Dragging moves the stored order, so it ends when another sort is picked.
    LaunchedEffect(uiState.sort) { if (!uiState.sort.isDefault) changingOrder = false }

    DisposableEffect(Unit) {
        onDispose {
            firstTimeGetLocalPlaylist = true
        }
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            LocalPlaylistUIEvent.ItemClick(
                videoId = videoId,
            ),
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = tracks?.findLast { it.first.videoId == videoId }?.first
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        menuRequest++
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = shouldShowSuggestions) {
        if (suggestedTracks.isEmpty() && uiState.syncState != LocalPlaylistEntity.YouTubeSyncState.NotSynced) {
            viewModel.getSuggestions(uiState.id)
        }
    }

    LaunchedEffect(key1 = id) {
        if (id != uiState.id) {
            Logger.d("PlaylistScreen", "new id: $id")
            viewModel.removeListSuggestion()
            viewModel.updatePlaylistState(id, true)
            delay(100)
            firstTimeGetLocalPlaylist = true
        }
    }
    LaunchedEffect(key1 = uiState) {
        shouldShowSuggestButton =
            !uiState.ytPlaylistId.isNullOrEmpty() &&
            uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val hazeState = rememberHazeState()
    val background = MaterialTheme.colorScheme.background
    val isThisPlaying = isPlaying && playingPlaylistId == LOCAL_PLAYLIST_ID + uiState.id

    // Loading dialog
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    if (showLoadingDialog.first) {
        LoadingDialog(
            true,
            showLoadingDialog.second,
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val dragDropState =
        rememberDragDropState(lazyState) { from, to ->
            coroutineScope.launch {
                Logger.d(TAG, "onMove from $from to $to")
                viewModel.changeLocalPlaylistItemPosition(from - 1, to - 1)
            }
        }
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
//    Box {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(background)
                .hazeSource(hazeState)
                .pointerInput(changingOrder) {
                    if (!changingOrder) return@pointerInput
                    val onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, offset: Offset) -> Unit =
                        { change, offset ->
                            Logger.d(TAG, "onDrag $offset")
                            change.consume()
                            dragDropState.onDrag(offset = offset)

                            if (overscrollJob?.isActive != true) {
                                dragDropState
                                    .checkForOverScroll()
                                    .takeIf { it != 0f }
                                    ?.let {
                                        overscrollJob =
                                            coroutineScope.launch {
                                                dragDropState.state.animateScrollBy(
                                                    it * 1.3f,
                                                    tween(easing = FastOutLinearInEasing),
                                                )
                                            }
                                    }
                                    ?: run { overscrollJob?.cancel() }
                            }
                        }
                    val onDragStart: (Offset) -> Unit = { offset ->
                        Logger.d(TAG, "onDragStart $offset")
                        dragDropState.onDragStart(offset)
                    }
                    val onDragEnd: () -> Unit = {
                        Logger.d(TAG, "onDragEnd")
                        dragDropState.onDragInterrupted(true)
                        overscrollJob?.cancel()
                    }
                    val onDragCancel: () -> Unit = {
                        Logger.d(TAG, "onDragCancel")
                        dragDropState.onDragInterrupted()
                        overscrollJob?.cancel()
                    }
                    if (getPlatform() == Platform.Desktop) {
                        // Desktop: normal drag (click-and-drag), no long press needed
                        detectDragGestures(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    } else {
                        // Android: long press then drag (touch-friendly)
                        detectDragGesturesAfterLongPress(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                },
        state = lazyState,
    ) {
        item(contentType = "header") {
            Column {
                CollectionHero(
                    thumbnail = uiState.thumbnail,
                    onBack = { navController.navigateUp() },
                    placeholder = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                    actions = {
                        Row(
                            modifier = Modifier.height(48.dp).pillSurface(rounded(24.dp)).padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // YouTube's suggestions, only while synced with YouTube.
                            if (shouldShowSuggestButton) {
                                IconButton(
                                    onClick = { shouldShowSuggestions = !shouldShowSuggestions },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(24.dp)
                                                .graphicsLayer {
                                                    compositingStrategy = CompositingStrategy.Offscreen
                                                }.drawWithContent {
                                                    val width = size.width
                                                    val height = size.height
                                                    val offsetDraw = width * progressAnimated
                                                    val brush =
                                                        Brush.linearGradient(
                                                            colors =
                                                                listOf(
                                                                    Color(0xFF4C82EF),
                                                                    Color(0xFFD96570),
                                                                ),
                                                            start = Offset(offsetDraw, 0f),
                                                            end =
                                                                Offset(
                                                                    offsetDraw + width,
                                                                    height,
                                                                ),
                                                        )
                                                    with(aiPainter) {
                                                        draw(size = Size(width, height))
                                                    }
                                                    drawRect(
                                                        brush = brush,
                                                        blendMode = BlendMode.SrcIn,
                                                    )
                                                },
                                    )
                                }
                            }
                            IconButton(onClick = { showSearchBar = true }, modifier = Modifier.size(40.dp)) {
                                Icon(LimusicIcons.Search, "Search in playlist", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { sortBottomSheetShow = true }, modifier = Modifier.size(40.dp)) {
                                // Accent while the list is in anything but Custom order.
                                Icon(
                                    LimusicIcons.Sort,
                                    stringResource(Res.string.sort_by),
                                    tint = if (uiState.sort.isDefault) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                )
                            }
                            CollectionDownloadButton(
                                download = download,
                                onDownload = viewModel::downloadFullPlaylist,
                                onRemove = viewModel::removeDownload,
                                onDownloadMissing = viewModel::downloadMissing,
                            )
                            IconButton(onClick = onPlaylistMoreClick, modifier = Modifier.size(40.dp)) {
                                Icon(LimusicIcons.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                ) {
                    CollectionTitle(
                        kicker = stringResource(Res.string.playlist) + " • " + stringResource(Res.string.new_playlist_where_device),
                        title = uiState.title,
                        playing = isThisPlaying,
                        onPlay = {
                            if (isThisPlaying) {
                                sharedViewModel.onUIEvent(UIEvent.PlayPause)
                            } else {
                                viewModel.onUIEvent(LocalPlaylistUIEvent.PlayClick)
                            }
                        },
                        onShuffle = { viewModel.onUIEvent(LocalPlaylistUIEvent.ShuffleClick) },
                    )
                    CollectionAuthorLine(
                        name = stringResource(Res.string.your_playlist),
                        thumbnail = null,
                        meta = listOf(pluralStringResource(Res.plurals.liked_songs_count, uiState.trackCount, uiState.trackCount)),
                        onClick = null,
                    )
                }
                Column(Modifier.padding(horizontal = 16.dp)) {
                    AnimatedVisibility(visible = shouldShowSuggestions) {
                        Column(
                            modifier = Modifier.animateContentSize(),
                        ) {
                            Spacer(modifier = Modifier.size(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(Res.string.suggest),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier =
                                        Modifier
                                            .padding(vertical = 8.dp)
                                            .weight(1f),
                                )
                                TextButton(
                                    onClick = { viewModel.reloadSuggestion() },
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                ) {
                                    Text(text = stringResource(Res.string.reload))
                                }
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Crossfade(targetState = suggestionsLoading) {
                                if (it) {
                                    CenterLoadingBox(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                    )
                                } else {
                                    Column {
                                        suggestedTracks.forEach { track ->
                                            SuggestItems(
                                                track = track,
                                                isPlaying = playingTrack?.videoId == track.videoId,
                                                onAddClickListener = {
                                                    viewModel.addSuggestTrackToListTrack(
                                                        track,
                                                    )
                                                },
                                                onClickListener = {
                                                    viewModel.onUIEvent(LocalPlaylistUIEvent.SuggestionsItemClick(track.videoId))
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.size(12.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                    // Dragging is for the stored order: only on Custom order, and never reversed.
                    // Other sorts get the YouTube playlist page's gap instead.
                    if (uiState.sort.sort != PlaylistSort.DEFAULT) {
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    if (!changingOrder && uiState.sort.desc) viewModel.setSort(ListSort())
                                    changingOrder = !changingOrder
                                },
                            ) {
                                Text(
                                    text = if (changingOrder) "Done" else "Change order",
                                    style = typo().bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
        // The row id stays with a song through a re-sort or a drag, so rows animate to their place.
        itemsIndexed(tracks.orEmpty(), key = { _, row -> row.second.id }) { index, row ->
            val item = row.first
            val content = @Composable { mod: Modifier ->
                SongFullWidthItems(
                    isPlaying = playingTrack?.videoId == item.videoId && isPlaying,
                    shouldShowDragHandle = changingOrder,
                    songEntity = item,
                    onMoreClickListener = { onItemMoreClick(it) },
                    onClickListener = { onPlaylistItemClick(it) },
                    onAddToQueue = {
                        sharedViewModel.addListToQueue(
                            arrayListOf(item.toTrack()),
                        )
                    },
                    selectionMode = selection.active,
                    isSelected = selection.has(rowKey(item.videoId)),
                    // No long press while reordering: that gesture already belongs to drag.
                    onLongClick = if (changingOrder) null else ({ selection.start(rowKey(item.videoId), item.toTrack()) }),
                    onSelectToggle = { selection.toggle(rowKey(item.videoId), item.toTrack()) },
                    modifier = mod,
                    padding = PaddingValues(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                )
            }
            if (changingOrder) {
                DraggableItem(
                    dragDropState,
                    index + 1,
                    Modifier.animateItem(),
                ) {
                    content(Modifier)
                }
            } else {
                Column(modifier = Modifier.animateItem()) {
                    content(Modifier)
                }
            }
        }
        item {
            AnimatedVisibility(tracks == null) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(15.dp))
                    CenterLoadingBox(modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(15.dp))
                }
            }
        }
        item {
            EndOfPage()
        }
    }
    if (itemBottomSheetShow && currentItem != null) {
        val track = currentItem ?: return
        NowPlayingBottomSheet(
            onDelete = { viewModel.deleteItem(uiState.id, track) },
            onDismiss = {
                itemBottomSheetShow = false
                currentItem = null
            },
            navController = navController,
            song = track,
        )
    }
    if (playlistBottomSheetShow) {
        key(menuRequest) {
            LocalPlaylistMenu(
                viewModel = viewModel,
                uiState = uiState,
                onClose = { playlistBottomSheetShow = false },
                onDeleted = { navController.navigateUp() },
                // Straight to the playlist where it lives now, in place of this page.
                onMoved = { youTubeId ->
                    navController.navigateUp()
                    navController.navigate(PlaylistDestination("VL$youTubeId", isYourYouTubePlaylist = true))
                },
                onSelectTracks = selection::enter,
            )
        }
    }
    // Drawn over the list rather than inside it, so the list keeps its drag and scroll state.
    AnimatedVisibility(
        visible = showSearchBar,
        enter = SearchBarEnter,
        exit = SearchBarExit,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(background),
        ) {
            // Same bar as the one on Playlist and Album: blurred over the content behind it,
            // back on the left, clear on the right.
            Box(
                Modifier
                    .fillMaxWidth()
                    .hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .windowInsetsPadding(WindowInsets.statusBars),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RippleIconButton(
                        imageVector = LimusicIcons.ArrowBackIosNew,
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigateUp()
                    }
                    SearchBar(
                        modifier =
                            Modifier
                                .height(50.dp)
                                .padding(horizontal = 12.dp)
                                .weight(1f),
                        colors =
                            SearchBarDefaults.colors().copy(
                                containerColor = Color.Transparent,
                            ),
                        inputField = {
                            CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                                SearchBarDefaults.InputField(
                                    query = searchQuery,
                                    onQueryChange = { viewModel.setSearchQuery(it) },
                                    onSearch = {},
                                    expanded = showSearchBar,
                                    onExpandedChange = { showSearchBar = it },
                                    placeholder = {
                                        Text(
                                            stringResource(Res.string.search),
                                            style = typo().bodyMedium,
                                        )
                                    },
                                )
                            }
                        },
                        expanded = false,
                        onExpandedChange = {},
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                    }
                    IconButton(
                        onClick = {
                            showSearchBar = false
                            viewModel.setSearchQuery("")
                        },
                    ) {
                        Icon(LimusicIcons.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    searchResults,
                    key = { (song, pair) -> song.videoId + "_" + pair.position },
                ) { (song, _) ->
                    SongFullWidthItems(
                        isPlaying = playingTrack?.videoId == song.videoId && isPlaying,
                        songEntity = song,
                        onMoreClickListener = { onItemMoreClick(it) },
                        onClickListener = { onPlaylistItemClick(it) },
                        onAddToQueue = { sharedViewModel.addListToQueue(arrayListOf(song.toTrack())) },
                        selectionMode = selection.active,
                        isSelected = selection.has(rowKey(song.videoId)),
                        onLongClick = { selection.start(rowKey(song.videoId), song.toTrack()) },
                        onSelectToggle = { selection.toggle(rowKey(song.videoId), song.toTrack()) },
                        // Required, no default: the list passes its own through `mod`.
                        modifier = Modifier,
                        padding = PaddingValues(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }
    // After the search, so it floats over the results too.
    val rows = remember(tracks) { keyedRows(tracks.orEmpty().map { it.first.toTrack() }) }
    val searchRows = remember(searchResults) { keyedRows(searchResults.map { it.first.toTrack() }) }
    TrackSelectionBar(
        selection = selection,
        bottomPadding = innerPadding.calculateBottomPadding(),
        rows = rows,
        shown = if (showSearchBar) searchRows else rows,
        from = uiState.title,
        onRemove = { picked, done -> viewModel.deleteItems(picked, done) },
        exceptLocalId = uiState.id,
    )
    if (sortBottomSheetShow) {
        PlaylistSortSheet(
            sort = uiState.sort.sort,
            desc = uiState.sort.desc,
            onSort = {
                viewModel.setSort(uiState.sort.copy(sort = it))
                sortBottomSheetShow = false
            },
            onReverse = { viewModel.setSort(uiState.sort.copy(desc = it)) },
            onDismiss = { sortBottomSheetShow = false },
            defaultLabel = Res.string.custom_order,
        )
    }
    AnimatedVisibility(
        visible = shouldHideTopBar,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        TopAppBar(
            windowInsets =
                TopAppBarDefaults.windowInsets.exclude(
                    TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
                ),
            title = {
                Text(
                    text = uiState.title,
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        LimusicIcons.ArrowBackIosNew,
                        Modifier
                            .size(32.dp),
                        true,
                        MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            modifier =
                Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
        )
    }
}
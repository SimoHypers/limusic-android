package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.PaddingValues
import org.jetbrains.compose.resources.pluralStringResource
import com.maxrave.simpmusic.ui.component.CollectionAuthorLine
import com.maxrave.simpmusic.ui.component.CollectionHero
import com.maxrave.simpmusic.ui.component.CollectionTitle
import simpmusic.composeapp.generated.resources.liked_songs_count
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.new_playlist_where_device
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.maxrave.common.LOCAL_PLAYLIST_ID
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PairSongLocalPlaylist
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.extension.displayNameRes
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.CollectionDownloadButton
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DraggableItem
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LoadingDialog
import com.maxrave.simpmusic.ui.component.LocalPlaylistMenu
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.SortPlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.SuggestItems
import com.maxrave.simpmusic.ui.component.painterPlaylistThumbnail
import com.maxrave.simpmusic.ui.component.pillSurface
import com.maxrave.simpmusic.ui.component.rememberDragDropState
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionAction
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Sort
import com.maxrave.simpmusic.ui.icon.TipsAndUpdates
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.viewModel.LocalPlaylistUIEvent
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
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
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.reload
import simpmusic.composeapp.generated.resources.remove_from_playlist
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.sort_by
import simpmusic.composeapp.generated.resources.suggest
import simpmusic.composeapp.generated.resources.your_playlist

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
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val aiPainter = rememberVectorPainter(SimpIcons.TipsAndUpdates)
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

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
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

    val trackPagingItems: LazyPagingItems<Pair<SongEntity, PairSongLocalPlaylist>> = viewModel.tracksPagingState.collectAsLazyPagingItems()
    LaunchedEffect(Unit) {
        snapshotFlow {
            trackPagingItems.loadState
        }.collectLatest {
            Logger.d("PlaylistScreen", "loadState: ${trackPagingItems.loadState}")
            viewModel.setLazyTrackPagingItems(trackPagingItems)
        }
    }

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
        currentItem = trackPagingItems.itemSnapshotList.findLast { it?.first?.videoId == videoId }?.first
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
            viewModel.setOffset(0)
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
                trackPagingItems.refresh()
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
                            modifier = Modifier.height(48.dp).pillSurface(RoundedCornerShape(24.dp)).padding(horizontal = 4.dp),
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
                                Icon(SimpIcons.Search, "Search in playlist", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            CollectionDownloadButton(
                                download = download,
                                onDownload = viewModel::downloadFullPlaylist,
                                onRemove = viewModel::removeDownload,
                                onDownloadMissing = viewModel::downloadMissing,
                            )
                            IconButton(onClick = onPlaylistMoreClick, modifier = Modifier.size(40.dp)) {
                                Icon(SimpIcons.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface)
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
                    Row(
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable {
                                        sortBottomSheetShow = true
                                    }.padding(vertical = 8.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = SimpIcons.Sort,
                                contentDescription = "Sort playlist",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text =
                                    stringResource(Res.string.sort_by) + ": " +
                                        stringResource(
                                            uiState.filterState.displayNameRes(),
                                        ),
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        AnimatedVisibility(
                            visible =
                                uiState.filterState == FilterState.CustomOrder,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            TextButton(
                                onClick = {
                                    changingOrder = !changingOrder
                                },
                            ) {
                                Text(
                                    text =
                                        if (changingOrder) {
                                            "Done"
                                        } else {
                                            "Change order"
                                        },
                                    style = typo().bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
        items(count = trackPagingItems.itemCount, key = { index ->
            val item = trackPagingItems[index]
            (item?.first?.videoId ?: "") + "item_$index" + item?.second?.inPlaylist + item?.second?.position
        }) { index ->
            val item = trackPagingItems[index]?.first
            if (item != null) {
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
                        selectionMode = selectionState.isActive,
                        isSelected = selectionState.isSelected(item.videoId),
                        // No long press while reordering: that gesture already belongs to drag.
                        onLongClick = if (changingOrder) null else ({ selectionState.start(it) }),
                        onSelectToggle = { selectionState.toggle(it) },
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
        }
        trackPagingItems.apply {
            item {
                AnimatedVisibility(loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(15.dp))
                        CenterLoadingBox(modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(15.dp))
                    }
                }
            }
        }
        item {
            EndOfPage()
        }
    }
    AnimatedVisibility(
        visible = selectionState.isActive,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        SongSelectionTopAppBar(
            state = selectionState,
            onSelectAll = {
                // peek() rather than get(): reading an item is what asks Paging to load its page,
                // and select-all must not drag the whole playlist into memory.
                selectionState.toggleSelectAll(
                    (0 until trackPagingItems.itemCount).mapNotNull {
                        trackPagingItems.peek(it)?.first?.videoId
                    },
                )
            },
            onOpenActions = { showSelectionSheet = true },
            modifier =
                Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
        )
    }
    if (showSelectionSheet) {
        val selectedIds = selectionState.selected.toList()
        val removeLabel = stringResource(Res.string.remove_from_playlist)
        SelectedSongsBottomSheet(
            count = selectedIds.size,
            onDismiss = { showSelectionSheet = false },
            onPlayNext = {
                selectionViewModel.playNext(selectedIds)
                selectionState.exit()
            },
            onAddToQueue = {
                selectionViewModel.addToQueue(selectedIds)
                selectionState.exit()
            },
            onAddToPlaylist = { showSelectionAddToPlaylist = true },
            onDownload = {
                selectionViewModel.download(selectedIds)
                selectionState.exit()
            },
            onAddToFavorite = {
                selectionViewModel.addToFavorite(selectedIds)
                selectionState.exit()
            },
            extraActions =
                listOf(
                    SongSelectionAction(
                        icon = SimpIcons.Delete,
                        label = removeLabel,
                        tint = Color.Red,
                    ) {
                        selectionViewModel.removeFromLocalPlaylist(uiState.id, selectedIds)
                        selectionState.exit()
                    },
                ),
        )
    }
    if (showSelectionAddToPlaylist) {
        val selectedIds = selectionState.selected.toList()
        val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = localPlaylists,
            listYouTubePlaylist = emptyList(),
            onDismiss = { showSelectionAddToPlaylist = false },
            onClick = { playlist ->
                selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                selectionState.exit()
            },
            onYTPlaylistClick = {},
        )
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
            )
        }
    }
    // A sibling of the paged list, not a branch inside it. The paged reader carries drag
    // reordering, in-place removal and its own scroll state; searching has none of that, and
    // threading a second data source through it would put all of that at risk for a feature that
    // only needs a flat list. This draws over it instead and leaves it untouched.
    //
    // Results come from LocalPlaylistViewModel.searchResults, which queries the database —
    // filtering trackPagingItems would only ever search the pages already scrolled through.
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
                        imageVector = SimpIcons.ArrowBackIosNew,
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
                        Icon(SimpIcons.Close, null, tint = MaterialTheme.colorScheme.onSurface)
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
                        selectionMode = selectionState.isActive,
                        isSelected = selectionState.isSelected(song.videoId),
                        onLongClick = { selectionState.start(it) },
                        onSelectToggle = { selectionState.toggle(it) },
                        // Required, no default — the paged list passes it down through `mod`.
                        modifier = Modifier,
                        padding = PaddingValues(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }
    if (sortBottomSheetShow) {
        SortPlaylistBottomSheet(
            selectedState = uiState.filterState,
            onDismiss = { sortBottomSheetShow = false },
            onSortChanged = {
                viewModel.onUIEvent(LocalPlaylistUIEvent.ChangeFilter(it))
                sortBottomSheetShow = false
            },
        )
    }
    AnimatedVisibility(
        visible = shouldHideTopBar && !selectionState.isActive,
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
                        SimpIcons.ArrowBackIosNew,
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
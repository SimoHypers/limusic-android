package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.pluralStringResource
import com.maxrave.simpmusic.ui.component.CollectionAuthorLine
import com.maxrave.simpmusic.ui.component.CollectionHero
import com.maxrave.simpmusic.ui.component.CollectionTitle
import com.maxrave.simpmusic.ui.component.LibrarySectionHeader
import simpmusic.composeapp.generated.resources.liked_songs_count
import androidx.compose.material3.IconButton
import com.maxrave.simpmusic.ui.component.pillSurface
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.ui.component.CollectionDownloadButton
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.AlbumMenu
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.LocalPlaylistState
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.other_version

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    browseId: String,
    navController: NavController,
    viewModel: AlbumViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val uriHandler = LocalUriHandler.current

    val playingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val download by viewModel.download.collectAsStateWithLifecycle()

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var albumBottomSheetShow by rememberSaveable { mutableStateOf(false) }
    var chosenSong: Track? by remember { mutableStateOf(null) }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }


    LaunchedEffect(browseId) {
        viewModel.updateBrowseId(browseId)
    }

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val hazeState = rememberHazeState()
    val background = MaterialTheme.colorScheme.background
    val isThisPlaying = playingVideoId.isNotEmpty() && playingPlaylistId == browseId.replaceFirst("VL", "")

    Crossfade(uiState.loadState) {
        when (it) {
            LocalPlaylistState.PlaylistLoadState.Success -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(background)
                            .hazeSource(hazeState),
                    state = lazyState,
                ) {
                    item(contentType = "header") {
                        Column {
                            CollectionHero(
                                thumbnail = uiState.thumbnail,
                                onBack = { navController.navigateUp() },
                                actions = {
                                    Row(
                                        modifier = Modifier.height(48.dp).pillSurface(RoundedCornerShape(24.dp)).padding(horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CollectionDownloadButton(
                                            download = download,
                                            onDownload = viewModel::downloadFullAlbum,
                                            onRemove = viewModel::removeDownload,
                                            onDownloadMissing = viewModel::downloadMissing,
                                        )
                                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                            HeartCheckBox(
                                                size = 28,
                                                checked = uiState.liked,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                onStateChange = viewModel::setAlbumLike,
                                            )
                                        }
                                        IconButton(onClick = { albumBottomSheetShow = true }, modifier = Modifier.size(40.dp)) {
                                            Icon(SimpIcons.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                },
                            ) {
                                CollectionTitle(
                                    // The parser writes a missing year as "null".
                                    kicker =
                                        listOfNotNull(
                                            uiState.kind ?: stringResource(Res.string.album),
                                            uiState.year.takeIf { year -> year.toIntOrNull() != null },
                                        ).joinToString(" • "),
                                    title = uiState.title,
                                    playing = isThisPlaying,
                                    onPlay = {
                                        if (isThisPlaying) {
                                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                        } else {
                                            uiState.listTrack.firstOrNull()?.let { track -> viewModel.playTrack(track) }
                                        }
                                    },
                                    onShuffle = viewModel::shuffle,
                                )
                                CollectionAuthorLine(
                                    name = uiState.artist.name,
                                    thumbnail = uiState.artistThumbnail,
                                    meta =
                                        listOf(
                                            pluralStringResource(Res.plurals.liked_songs_count, uiState.trackCount, uiState.trackCount),
                                            uiState.length,
                                        ),
                                    onClick =
                                        uiState.artist.id?.let { channelId ->
                                            { navController.navigate(ArtistDestination(channelId = channelId)) }
                                        },
                                )
                            }
                            uiState.description?.takeIf { text -> text.isNotBlank() }?.let { text ->
                                DescriptionView(
                                    text = text,
                                    limitLine = 2,
                                    onTimeClicked = {},
                                    onURLClicked = { url -> uriHandler.openUri(url) },
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    items(count = uiState.trackCount, key = { index ->
                        val item = uiState.listTrack.getOrNull(index)
                        item?.videoId + "item_$index"
                    }) { index ->
                        val item = uiState.listTrack.getOrNull(index)
                        if (item != null) {
                            Column(modifier = Modifier.animateItem()) {
                                SongFullWidthItems(
                                    isPlaying = item.videoId == playingVideoId,
                                    index = index,
                                    track = item,
                                    onMoreClickListener = {
                                        chosenSong = item
                                        showBottomSheet = true
                                    },
                                    onClickListener = {
                                        viewModel.playTrack(item)
                                    },
                                    onAddToQueue = {
                                        sharedViewModel.addListToQueue(
                                            arrayListOf(item),
                                        )
                                    },
                                    selectionMode = selectionState.isActive,
                                    isSelected = selectionState.isSelected(item.videoId),
                                    onLongClick = { selectionState.start(it) },
                                    onSelectToggle = { selectionState.toggle(it) },
                                    modifier = Modifier,
                                    padding = PaddingValues(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                )
                            }
                        }
                    }
                    item(contentType = "other_version") {
                        AnimatedVisibility(uiState.otherVersion.isNotEmpty()) {
                            Column {
                                Spacer(Modifier.height(16.dp))
                                LibrarySectionHeader(icon = null, title = stringResource(Res.string.other_version))
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    items(uiState.otherVersion) { album ->
                                        HomeItemContentPlaylist(
                                            onClick = {
                                                navController.navigate(
                                                    AlbumDestination(
                                                        browseId = album.browseId,
                                                    ),
                                                )
                                            },
                                            data = album,
                                            thumbSize = 180.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        EndOfPage()
                    }
                }
                AnimatedVisibility(
                    visible = shouldHideTopBar && !selectionState.isActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = uiState.title,
                                style = typo().titleMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(
                                            align = Alignment.CenterVertically,
                                        ).basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
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
                AnimatedVisibility(
                    visible = selectionState.isActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    SongSelectionTopAppBar(
                        state = selectionState,
                        onSelectAll = {
                            selectionState.toggleSelectAll(uiState.listTrack.map { it.videoId })
                        },
                        onOpenActions = { showSelectionSheet = true },
                        modifier =
                            Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
                    )
                }
                if (showSelectionSheet) {
                    val selectedIds = selectionState.selected.toList()
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
                        // Deliberately does not exit yet: the playlist picker opens next and
                        // still needs the selection alive.
                        onAddToPlaylist = { showSelectionAddToPlaylist = true },
                        onDownload = {
                            selectionViewModel.download(selectedIds)
                            selectionState.exit()
                        },
                        onAddToFavorite = {
                            selectionViewModel.addToFavorite(selectedIds)
                            selectionState.exit()
                        },
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
                    )
                }
                if (showBottomSheet) {
                    NowPlayingBottomSheet(
                        onDismiss = {
                            showBottomSheet = false
                            chosenSong = null
                        },
                        navController = navController,
                        song = chosenSong?.toSongEntity(),
                    )
                }
                if (albumBottomSheetShow) {
                    AlbumMenu(viewModel, uiState, browseId, onClose = { albumBottomSheetShow = false })
                }
            }

            LocalPlaylistState.PlaylistLoadState.Error -> {
                navController.navigateUp()
            }

            LocalPlaylistState.PlaylistLoadState.Loading -> {
                CenterLoadingBox(
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
package com.simohypers.limusic.ui.screen.other

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.pluralStringResource
import com.simohypers.limusic.ui.component.CollectionAuthorLine
import com.simohypers.limusic.ui.component.CollectionHero
import com.simohypers.limusic.ui.component.CollectionTitle
import com.simohypers.limusic.ui.component.LibrarySectionHeader
import limusic.composeapp.generated.resources.liked_songs_count
import androidx.compose.material3.IconButton
import com.simohypers.limusic.ui.component.pillSurface
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
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.extension.barBlurStyle
import com.simohypers.limusic.ui.component.CollectionDownloadButton
import com.simohypers.limusic.ui.component.AlbumMenu
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.DescriptionView
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.HeartCheckBox
import com.simohypers.limusic.ui.component.HomeItemContentPlaylist
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.selection.TrackSelectionBar
import com.simohypers.limusic.ui.component.selection.keyedRows
import com.simohypers.limusic.ui.component.selection.rememberTrackSelection
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.AlbumViewModel
import com.simohypers.limusic.viewModel.LocalPlaylistState
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.album
import limusic.composeapp.generated.resources.other_version

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    browseId: String,
    navController: NavController,
    viewModel: AlbumViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    innerPadding: PaddingValues = PaddingValues(),
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

    val selection = rememberTrackSelection()
    val rows = remember(uiState.listTrack) { keyedRows(uiState.listTrack) }


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
                                            Icon(LimusicIcons.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface)
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
                        val row = rows.getOrNull(index)
                        if (row != null) {
                            val (key, item) = row
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
                                    selectionMode = selection.active,
                                    isSelected = selection.has(key),
                                    onLongClick = { selection.start(key, item) },
                                    onSelectToggle = { selection.toggle(key, item) },
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
                    visible = shouldHideTopBar,
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
                TrackSelectionBar(
                    selection = selection,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    rows = rows,
                    from = uiState.title,
                )
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
                    AlbumMenu(viewModel, uiState, browseId, onClose = { albumBottomSheetShow = false }, onSelectTracks = selection::enter)
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
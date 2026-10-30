package com.simohypers.limusic.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.entities.AlbumEntity
import com.simohypers.limusic.domain.data.entities.ArtistEntity
import com.simohypers.limusic.domain.data.entities.PlaylistEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.extension.ultraThinBarStyle
import com.simohypers.limusic.ui.component.ArtistFullWidthItems
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.PlaylistFullWidthItems
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.selection.TrackSelectionBar
import com.simohypers.limusic.ui.component.selection.keyedRows
import com.simohypers.limusic.ui.component.selection.rememberTrackSelection
import com.simohypers.limusic.ui.component.selection.rowKey
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.RecentlySongsViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.error
import limusic.composeapp.generated.resources.recently_added

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlySongsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: RecentlySongsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val hazeState = rememberHazeState()

    val selection = rememberTrackSelection()

    val recentlyItems = viewModel.recentlySongs.collectAsLazyPagingItems()
    val playingTrack by sharedViewModel.nowPlayingState.map { it?.songEntity }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
        ) {
            item {
                Spacer(
                    Modifier.size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
                )
            }

            items(
                count = recentlyItems.itemCount,
                key = { index ->
                    val item = recentlyItems[index]
                    when (item) {
                        is SongEntity -> "song_${item.videoId}_$index"
                        is AlbumEntity -> "album_${item.browseId}_$index"
                        is PlaylistEntity -> "playlist_${item.id}_$index"
                        is ArtistEntity -> "artist_${item.channelId}_$index"
                        else -> index.toString()
                    }
                },
            ) { index ->
                val item = recentlyItems[index]
                when (item) {
                    is SongEntity -> {
                        val recentlyAddedString = stringResource(Res.string.recently_added)
                        SongFullWidthItems(
                            songEntity = item,
                            isPlaying = playingTrack?.videoId == item.videoId && isPlaying,
                            onClickListener = { videoId ->
                                val firstQueue = item.toTrack()
                                viewModel.setQueueData(
                                    QueueData.Data(
                                        listTracks = arrayListOf(firstQueue),
                                        firstPlayedTrack = firstQueue,
                                        playlistId = "RDAMVM$videoId",
                                        playlistName = recentlyAddedString,
                                        playlistType = PlaylistType.RADIO,
                                        continuation = null,
                                    ),
                                )
                                viewModel.loadMediaItem(
                                    firstQueue,
                                    Config.SONG_CLICK,
                                    0,
                                )
                            },
                            onAddToQueue = {
                                sharedViewModel.addListToQueue(
                                    arrayListOf(item.toTrack()),
                                )
                            },
                            selectionMode = selection.active,
                            isSelected = selection.has(rowKey(item.videoId)),
                            onLongClick = { selection.start(rowKey(item.videoId), item.toTrack()) },
                            onSelectToggle = { selection.toggle(rowKey(item.videoId), item.toTrack()) },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    is AlbumEntity -> {
                        PlaylistFullWidthItems(
                            data = item,
                            onClickListener = {
                                navController.navigate(
                                    AlbumDestination(
                                        item.browseId,
                                    ),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    is PlaylistEntity -> {
                        PlaylistFullWidthItems(
                            data = item,
                            onClickListener = {
                                navController.navigate(
                                    PlaylistDestination(
                                        item.id,
                                    ),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    is ArtistEntity -> {
                        ArtistFullWidthItems(
                            data = item,
                            onClickListener = {
                                navController.navigate(
                                    ArtistDestination(
                                        item.channelId,
                                    ),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            // Loading state
            recentlyItems.apply {
                when {
                    loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> {
                        item {
                            CenterLoadingBox(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                            )
                        }
                    }

                    loadState.refresh is LoadState.Error || loadState.append is LoadState.Error -> {
                        item {
                            val error =
                                (loadState.refresh as? LoadState.Error)?.error?.message
                                    ?: (loadState.append as? LoadState.Error)?.error?.message
                                    ?: stringResource(Res.string.error)
                            LaunchedEffect(error) {
                                viewModel.makeToast(error)
                            }
                        }
                    }
                }
            }

            item {
                EndOfPage()
            }
        }

        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()),
            title = {
                Text(
                    text = stringResource(Res.string.recently_added),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        LimusicIcons.ArrowBackIosNew,
                        Modifier.size(32.dp),
                        true,
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            actions = {},
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
        // The history is paged and runs long: Select all takes the songs loaded so far.
        val songs = recentlyItems.itemSnapshotList.items
        val rows = remember(songs) { keyedRows(songs.filterIsInstance<SongEntity>().distinctBy { it.videoId }.map { it.toTrack() }) }
        TrackSelectionBar(selection = selection, rows = rows, bottomPadding = innerPadding.calculateBottomPadding())
    }
}

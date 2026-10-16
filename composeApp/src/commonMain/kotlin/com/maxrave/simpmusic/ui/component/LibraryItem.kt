package com.maxrave.simpmusic.ui.component

import com.maxrave.simpmusic.extension.shimmer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Update
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.component.selection.SongSelectionState
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.recently_added
import com.maxrave.domain.mediaservice.handler.PlaylistType as DomainPlaylistType

/**
 * The Recently added list under the All tab's shelves: songs, albums, playlists and artists
 * newest first, each opening as it would anywhere else.
 */
@Composable
fun RecentlyAddedList(
    data: List<RecentlyType>,
    playingVideoId: String,
    isLoading: Boolean,
    navController: NavController,
    viewModel: LibraryViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    // Null means these rows do not take part in multi-selection; the selection bar lives on the
    // hosting screen, so only a screen that shows one passes a state down.
    selectionState: SongSelectionState? = null,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var songEntity by remember { mutableStateOf<SongEntity?>(null) }
    Box {
        if (showBottomSheet) {
            NowPlayingBottomSheet(
                onDismiss = {
                    showBottomSheet = false
                    songEntity = null
                },
                navController = navController,
                song = songEntity ?: return,
                onLibraryDelete = {
                    songEntity?.videoId?.let { viewModel.deleteSong(it) }
                },
            )
        }
        Column {
            LibrarySectionHeader(
                icon = SimpIcons.Update,
                title = stringResource(Res.string.recently_added),
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            Crossfade(targetState = isLoading, label = "Loading") { isLoading ->
                if (!isLoading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        data.forEach { item ->
                            when (item.objectType()) {
                                RecentlyType.Type.SONG -> {
                                    SongFullWidthItems(
                                        songEntity = item as SongEntity,
                                        isPlaying = item.videoId == playingVideoId,
                                        modifier = Modifier,
                                        onMoreClickListener = {
                                            songEntity = item
                                            showBottomSheet = true
                                        },
                                        onClickListener = {
                                            viewModel.setQueueData(
                                                QueueData.Data(
                                                    listTracks = arrayListOf(item.toTrack()),
                                                    firstPlayedTrack = item.toTrack(),
                                                    playlistId = "RDAMVM${item.videoId}",
                                                    playlistName = item.title,
                                                    playlistType = DomainPlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            viewModel.loadMediaItem(
                                                item,
                                                type = Config.SONG_CLICK,
                                                index = 0,
                                            )
                                        },
                                        selectionMode = selectionState?.isActive == true,
                                        isSelected = selectionState?.isSelected(item.videoId) == true,
                                        onLongClick =
                                            selectionState?.let { sel -> { id: String -> sel.start(id) } },
                                        onSelectToggle =
                                            selectionState?.let { sel -> { id: String -> sel.toggle(id) } },
                                        onAddToQueue = { videoId ->
                                            sharedViewModel.addListToQueue(
                                                arrayListOf(item.toTrack()),
                                            )
                                        },
                                    )
                                }

                                RecentlyType.Type.ARTIST -> {
                                    ArtistFullWidthItems(
                                        data = item as? ArtistEntity ?: return@forEach,
                                        onClickListener = {
                                            navController.navigate(
                                                ArtistDestination(
                                                    channelId = item.channelId,
                                                ),
                                            )
                                        },
                                    )
                                }

                                else -> {
                                    if (item is PlaylistType) {
                                        PlaylistFullWidthItems(
                                            data = item,
                                            onClickListener = { navController.openLibraryItem(item) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column { repeat(5) { SongRowSkeleton() } }
                }
            }
        }
    }
}

/** A song row still loading: artwork and two lines, sized like [SongFullWidthItems]. */
@Composable
private fun SongRowSkeleton() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).shimmer())
        Column(Modifier.padding(start = 12.dp).fillMaxWidth()) {
            Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(50)).shimmer())
            Box(Modifier.padding(top = 8.dp).fillMaxWidth(0.35f).height(11.dp).clip(RoundedCornerShape(50)).shimmer())
        }
    }
}

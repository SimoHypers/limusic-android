package com.simohypers.limusic.ui.component

import com.simohypers.limusic.extension.shimmer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.height
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simohypers.limusic.domain.data.entities.AlbumEntity
import com.simohypers.limusic.domain.data.model.searchResult.albums.AlbumsResult
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.PlaylistEntity
import com.simohypers.limusic.domain.data.entities.PodcastsEntity
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.type.PlaylistType
import com.simohypers.limusic.domain.utils.LocalResource
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.extension.isScrollingUp
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PodcastDestination
import com.simohypers.limusic.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Columns for every grid built on [GridLibraryPlaylist] — the Library tabs, Wrapped and Mix for you.
 *
 * The count is whichever brings the artwork closest to 150dp, so every cell on a page is the same
 * size and a wider window adds columns instead of stretching them. `Adaptive(minSize = 120.dp)`
 * treated 120 as a floor instead, which on a 1404dp window meant eleven columns of ~108dp artwork.
 *
 * Never fewer than three: the column count must follow the window, not the device's density, and a
 * 360–412dp phone would otherwise round down to two. Below 480dp this gives exactly the three columns
 * `Adaptive(120.dp)` did, so phones keep the grid they have.
 */
internal object LibraryGridCells : GridCells {
    // The artwork plus the 10dp start + 10dp end padding every item in these grids carries.
    private val TargetCellWidth = 150.dp + 20.dp

    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int,
    ): List<Int> {
        val target = TargetCellWidth.roundToPx() + spacing
        val count = ((availableSize + spacing).toFloat() / target).roundToInt().coerceAtLeast(3)
        // The same split GridCells.Fixed makes: leftover pixels go one each to the first columns.
        val usable = availableSize - spacing * (count - 1)
        return List(count) { usable / count + if (it < usable % count) 1 else 0 }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal inline fun <reified T> GridLibraryPlaylist(
    navController: NavController,
    contentPadding: PaddingValues,
    data: LocalResource<List<T>>,
    emptyText: StringResource,
    // Hoisted so a caller can read the scroll position itself — Mix for you derives its
    // top-bar frost from `index == 0 && offset == 0`, which the coarse onScrolling below
    // cannot say.
    state: LazyGridState = rememberLazyGridState(),
    noinline onScrolling: (onTop: Boolean) -> Unit = { _ -> },
    // A full-width block above the tiles, as a real grid item spanning every column — the same
    // mechanism the create tile and the chart button below already use. A caller drawing it in a
    // Box over the grid instead has to reserve its height in contentPadding and translate it by
    // the scroll offset by hand, which is what left a screen-tall hole above the Wrapped tab.
    noinline header: (@Composable () -> Unit)? = null,
    noinline createNewPlaylist: (() -> Unit)? = null,
    // A line under a tile's title in place of its own, for the items it returns non-null.
    noinline subtitleOf: @Composable (PlaylistType) -> String? = { null },
    noinline onReload: () -> Unit,
) {
    Logger.d("GridLibraryPlaylist", "Generic Type: ${T::class.simpleName}")
    val isScrollingUp by state.isScrollingUp()

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }
    // The last list that landed stays on screen while a refresh runs, so pulling to refresh never
    // blanks the grid; skeleton tiles stand in only before anything has landed at all.
    var last by remember { mutableStateOf<List<T>?>(null) }
    LaunchedEffect(data) { if (data is LocalResource.Success) last = data.data }
    val list: List<T>? = if (data is LocalResource.Success) data.data else last
    val skeleton = list == null && data is LocalResource.Loading
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        onRefresh = onReload,
        isRefreshing = data is LocalResource.Loading && !skeleton,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = data is LocalResource.Loading && !skeleton,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top = contentPadding.calculateTopPadding(),
                        ),
                containerColor = PullToRefreshDefaults.indicatorContainerColor,
                color = PullToRefreshDefaults.indicatorColor,
                maxDistance = PullToRefreshDefaults.PositionalThreshold,
            )
        },
    ) {
        val empty = !skeleton && list.isNullOrEmpty() && createNewPlaylist == null && header == null
        Crossfade(targetState = empty) { empty ->
            if (!empty) {
                LazyVerticalGrid(
                    columns = LibraryGridCells,
                    contentPadding = contentPadding,
                    state = state,
                ) {
                    if (header != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            header()
                        }
                    }
                    if (createNewPlaylist != null) {
                        item {
                            NewPlaylistTile(createNewPlaylist, Modifier.fillMaxWidth())
                        }
                    }
                    if (skeleton) {
                        items(12) { PlaylistTileSkeleton(Modifier.fillMaxWidth()) }
                    }
                    items(list.orEmpty()) { item ->
                        if (item !is PlaylistType) {
                            return@items
                        }
                        HomeItemContentPlaylist(
                            onClick = { navController.openLibraryItem(item) },
                            data = item,
                            thumbSize = 132.dp,
                            // The cell decides the width now, so the artwork follows it instead of
                            // being pinned to thumbSize and cropped by a narrower cell.
                            fillMaxWidth = true,
                            subtitle = subtitleOf(item),
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EndOfPage()
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(emptyText),
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Opens whatever a Library grid or shelf holds on its own page. */
fun NavController.openLibraryItem(item: PlaylistType) {
    when (item) {
        is LocalPlaylistEntity -> navigate(LocalPlaylistDestination(item.id))
        is PlaylistsResult -> navigate(PlaylistDestination(item.browseId, isYourYouTubePlaylist = true))
        is AlbumEntity -> navigate(AlbumDestination(item.browseId))
        is AlbumsResult -> navigate(AlbumDestination(item.browseId))
        is PlaylistEntity -> navigate(PlaylistDestination(item.id))
        is PodcastsEntity -> navigate(PodcastDestination(podcastId = item.podcastId))
    }
}

/** A playlist tile still loading: the artwork square and a title line, padded like the real card. */
@Composable
internal fun PlaylistTileSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).shimmer())
        Box(Modifier.padding(top = 10.dp).fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(50)).shimmer())
        Box(Modifier.padding(top = 6.dp).fillMaxWidth(0.45f).height(11.dp).clip(RoundedCornerShape(50)).shimmer())
    }
}

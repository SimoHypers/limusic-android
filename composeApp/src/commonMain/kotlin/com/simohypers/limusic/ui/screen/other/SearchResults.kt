package com.simohypers.limusic.ui.screen.other

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.searchResult.albums.AlbumsResult
import com.simohypers.limusic.domain.data.model.searchResult.artists.ArtistsResult
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.model.searchResult.songs.SongsResult
import com.simohypers.limusic.domain.data.model.searchResult.videos.VideosResult
import com.simohypers.limusic.domain.data.type.SearchResultType
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.utils.connectArtists
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.expect.ui.isLyricsBlurSupported
import com.simohypers.limusic.extension.getStringBlocking
import com.simohypers.limusic.ui.component.AlbumMenu
import com.simohypers.limusic.ui.component.HomeItemContentPlaylist
import com.simohypers.limusic.ui.component.LibrarySectionHeader
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.ui.component.selection.TrackSelection
import com.simohypers.limusic.ui.component.selection.rowKey
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.icon.PlayArrow
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PodcastDestination
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.AlbumViewModel
import com.simohypers.limusic.viewModel.LocalPlaylistState
import com.simohypers.limusic.viewModel.SearchScreenState
import com.simohypers.limusic.viewModel.SearchType
import com.simohypers.limusic.viewModel.SearchViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.albums
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.in_search
import limusic.composeapp.generated.resources.playlists
import limusic.composeapp.generated.resources.songs
import limusic.composeapp.generated.resources.videos

/** A search hit played like the app always has: the song, then its radio. */
internal fun SearchViewModel.playFromSearch(
    track: Track,
    query: String,
    clickType: String = Config.SONG_CLICK,
) {
    setQueueData(
        QueueData.Data(
            listTracks = arrayListOf(track),
            firstPlayedTrack = track,
            playlistId = "RDAMVM${track.videoId}",
            playlistName = "\"$query\" ${getStringBlocking(Res.string.in_search)}",
            playlistType = PlaylistType.RADIO,
            continuation = null,
        ),
    )
    loadMediaItem(track, clickType)
}

/** Whether the chip on screen has anything to show. */
internal fun SearchScreenState.hasResults(): Boolean =
    when (searchType) {
        SearchType.ALL ->
            top != null ||
                listOf(searchSongsResult, searchVideosResult, searchAlbumsResult, searchArtistsResult, searchPlaylistsResult).any { it.isNotEmpty() }
        SearchType.SONGS -> searchSongsResult.isNotEmpty()
        SearchType.VIDEOS -> searchVideosResult.isNotEmpty()
        SearchType.ALBUMS -> searchAlbumsResult.isNotEmpty()
        SearchType.ARTISTS -> searchArtistsResult.isNotEmpty()
        SearchType.PLAYLISTS -> searchPlaylistsResult.isNotEmpty()
        SearchType.PODCASTS -> searchPodcastsResult.isNotEmpty()
    }

/** The songs on screen as rows, which select mode acts on. */
internal fun SearchScreenState.rowTracks(): List<Track> =
    when (searchType) {
        SearchType.ALL -> (topSongs + searchSongsResult.take(ALL_SONGS)).map { it.toTrack() }
        SearchType.SONGS -> searchSongsResult.map { it.toTrack() }
        SearchType.VIDEOS -> searchVideosResult.map { it.toTrack() }
        else -> emptyList()
    }

/** How many columns a chip's grid takes; rows and All's sections span them all. */
internal fun SearchType.gridColumns(wide: Boolean): Int =
    when (this) {
        SearchType.ALBUMS, SearchType.PLAYLISTS, SearchType.PODCASTS -> if (wide) 4 else 2
        SearchType.ARTISTS -> if (wide) 5 else 3
        else -> 1
    }

private const val ALL_SONGS = 5
private val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

/**
 * Desktop's results: on All the top result, then a section per kind (Songs, Albums, Artists,
 * Videos, Playlists) whose "See all" opens its chip; on any other chip that kind's whole list.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyGridScope.searchResults(
    state: SearchScreenState,
    query: String,
    playingVideoId: String?,
    selection: TrackSelection,
    navController: NavController,
    viewModel: SearchViewModel,
    sharedViewModel: SharedViewModel,
    onSongMenu: (Track) -> Unit,
    onSeeAll: (SearchType) -> Unit,
) {
    val songRow: @Composable (Track, String, PaddingValues) -> Unit = { track, clickType, padding ->
        SongFullWidthItems(
            track = track,
            isPlaying = track.videoId == playingVideoId,
            modifier = Modifier,
            padding = padding,
            onMoreClickListener = { onSongMenu(track) },
            onClickListener = { viewModel.playFromSearch(track, query, clickType) },
            onAddToQueue = { sharedViewModel.addListToQueue(arrayListOf(track)) },
            selectionMode = selection.active,
            isSelected = selection.has(rowKey(track.videoId)),
            onLongClick = { selection.start(rowKey(track.videoId), track) },
            onSelectToggle = { selection.toggle(rowKey(track.videoId), track) },
        )
    }
    val openAlbum = { album: AlbumsResult -> navController.navigate(AlbumDestination(album.browseId)) }
    val openArtist = { artist: ArtistsResult -> navController.navigate(ArtistDestination(artist.browseId)) }
    val openPlaylist = { playlist: PlaylistsResult ->
        if (playlist.resultType == "Podcast") {
            navController.navigate(PodcastDestination(playlist.browseId))
        } else {
            navController.navigate(PlaylistDestination(playlist.browseId))
        }
    }

    fun header(
        title: StringResource,
        more: SearchType,
    ) = item(span = fullSpan) {
        LibrarySectionHeader(
            icon = null,
            title = stringResource(title),
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            onMore = { onSeeAll(more) },
        )
    }

    fun <T> shelf(
        title: StringResource,
        more: SearchType,
        list: List<T>,
        card: @Composable (T) -> Unit,
    ) {
        if (list.isEmpty()) return
        header(title, more)
        item(span = fullSpan) {
            // Home's rows: the cards carry their own 10dp, so 6dp puts the art at 16dp.
            LazyRow(contentPadding = PaddingValues(horizontal = 6.dp)) {
                items(list) { card(it) }
            }
        }
    }

    when (state.searchType) {
        SearchType.ALL -> {
            state.top?.let { top ->
                item(span = fullSpan) {
                    TopResultCard(
                        top = top,
                        subtitle = state.topSubtitle,
                        songs = state.topSongs.map { it.toTrack() },
                        query = query,
                        navController = navController,
                        viewModel = viewModel,
                        onSongMenu = onSongMenu,
                        songRow = { songRow(it, Config.SONG_CLICK, PaddingValues(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) },
                    )
                }
            }
            val songs = state.searchSongsResult.take(ALL_SONGS)
            if (songs.isNotEmpty()) {
                header(Res.string.songs, SearchType.SONGS)
                items(songs, span = { GridItemSpan(maxLineSpan) }) { songRow(it.toTrack(), Config.SONG_CLICK, RowPadding) }
            }
            shelf(Res.string.albums, SearchType.ALBUMS, state.searchAlbumsResult) {
                HomeItemContentPlaylist(onClick = { openAlbum(it) }, data = it, subtitle = it.subtitle())
            }
            shelf(Res.string.artists, SearchType.ARTISTS, state.searchArtistsResult) {
                ArtistCircle(it, Modifier.width(136.dp)) { openArtist(it) }
            }
            shelf(Res.string.videos, SearchType.VIDEOS, state.searchVideosResult) {
                val track = it.toTrack()
                VideoCard(it, onClick = { viewModel.playFromSearch(track, query, Config.VIDEO_CLICK) }, onLongClick = { onSongMenu(track) })
            }
            shelf(Res.string.playlists, SearchType.PLAYLISTS, state.searchPlaylistsResult) {
                HomeItemContentPlaylist(onClick = { openPlaylist(it) }, data = it)
            }
        }

        SearchType.SONGS -> items(state.searchSongsResult, span = { GridItemSpan(maxLineSpan) }) { songRow(it.toTrack(), Config.SONG_CLICK, RowPadding) }

        SearchType.VIDEOS -> items(state.searchVideosResult, span = { GridItemSpan(maxLineSpan) }) { songRow(it.toTrack(), Config.VIDEO_CLICK, RowPadding) }

        SearchType.ALBUMS ->
            items(state.searchAlbumsResult) {
                HomeItemContentPlaylist(onClick = { openAlbum(it) }, data = it, fillMaxWidth = true, subtitle = it.subtitle())
            }

        SearchType.ARTISTS -> items(state.searchArtistsResult) { ArtistCircle(it, Modifier.fillMaxWidth()) { openArtist(it) } }

        SearchType.PLAYLISTS -> items(state.searchPlaylistsResult) { HomeItemContentPlaylist(onClick = { openPlaylist(it) }, data = it, fillMaxWidth = true) }

        SearchType.PODCASTS -> items(state.searchPodcastsResult) { HomeItemContentPlaylist(onClick = { openPlaylist(it) }, data = it, fillMaxWidth = true) }
    }
}

// The collection pages' tighter rows, with the art at the headers' 16dp.
private val RowPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)

/** Desktop's "Album • Drake • 2026" line, minus the kind the section title already says. */
private fun AlbumsResult.subtitle() = listOf(artists.map { it.name }.connectArtists(), year).filter { it.isNotBlank() }.joinToString(" • ")

/**
 * Desktop's TopResult: the answer rather than one card among many, lit by its own artwork, the
 * cover large, the title at display size, a play button that does the right thing for the kind
 * (an artist's radio), and the songs YouTube pairs with it.
 */
@Composable
private fun TopResultCard(
    top: SearchResultType,
    subtitle: String,
    songs: List<Track>,
    query: String,
    navController: NavController,
    viewModel: SearchViewModel,
    onSongMenu: (Track) -> Unit,
    songRow: @Composable (Track) -> Unit,
) {
    val title: String
    val thumbnail: String?
    when (top) {
        is SongsResult -> {
            title = top.title.orEmpty()
            thumbnail = top.thumbnails?.lastOrNull()?.url
        }
        is AlbumsResult -> {
            title = top.title
            thumbnail = top.thumbnails.lastOrNull()?.url
        }
        is ArtistsResult -> {
            title = top.artist
            thumbnail = top.thumbnails.lastOrNull()?.url
        }
        else -> return
    }
    // An album plays and opens its menu through the album page's own view model, as Home's shortcuts do.
    val album = top as? AlbumsResult
    val albumViewModel: AlbumViewModel? = album?.let { koinViewModel(key = "search-top:${it.browseId}") }
    var albumMenu by remember { mutableStateOf(false) }
    val loadAlbum = {
        if (album != null && albumViewModel != null) {
            val ui = albumViewModel.uiState.value
            if (ui.browseId != album.browseId || ui.loadState != LocalPlaylistState.PlaylistLoadState.Success) albumViewModel.updateBrowseId(album.browseId)
        }
    }
    val open: () -> Unit = {
        when (top) {
            is SongsResult -> viewModel.playFromSearch(top.toTrack(), query)
            is AlbumsResult -> navController.navigate(AlbumDestination(top.browseId))
            is ArtistsResult -> navController.navigate(ArtistDestination(top.browseId))
            else -> {}
        }
    }
    val play: () -> Unit = {
        when (top) {
            is SongsResult -> viewModel.playFromSearch(top.toTrack(), query)
            is AlbumsResult -> {
                loadAlbum()
                albumViewModel?.let { vm -> vm.afterLoad { loaded -> loaded.listTrack.firstOrNull()?.let(vm::playTrack) } }
            }
            is ArtistsResult -> viewModel.startArtistRadio(top)
            else -> {}
        }
    }
    // Artists have no menu sheet outside their page; their card is play and open only.
    val menu: (() -> Unit)? =
        when (top) {
            is SongsResult -> ({ onSongMenu(top.toTrack()) })
            is AlbumsResult -> ({
                loadAlbum()
                albumMenu = true
            })
            else -> null
        }
    if (albumMenu && album != null && albumViewModel != null) {
        val uiState by albumViewModel.uiState.collectAsStateWithLifecycle()
        AlbumMenu(albumViewModel, uiState, album.browseId, onClose = { albumMenu = false })
    }

    val card = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = rounded(20.dp)
    Box(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 4.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), shape),
    ) {
        // Lit by its own artwork, blurred past recognition (a small decode is enough), fading into
        // the card. Without blur (Android 11 and older) the plain card is the design.
        if (thumbnail != null && isLyricsBlurSupported()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(thumbnail).size(96).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = 1.25f
                            scaleY = 1.25f
                            alpha = 0.6f
                        }.blur(40.dp),
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to card.copy(alpha = 0.3f), 0.5f to card.copy(alpha = 0.75f), 1f to card)))
        }
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = open).padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val coverShape = if (top is ArtistsResult) CircleShape else rounded(12.dp)
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current).data(thumbnail).crossfade(true).build(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp).shadow(12.dp, coverShape).clip(coverShape),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = typo().headlineLarge.copy(fontSize = if (title.length > 28) 19.sp else 24.sp, lineHeight = if (title.length > 28) 22.sp else 27.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = play),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LimusicIcons.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                if (menu != null) {
                    IconButton(onClick = menu) {
                        Icon(LimusicIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.width(12.dp))
                }
            }
            if (songs.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column(Modifier.padding(vertical = 4.dp)) { songs.forEach { songRow(it) } }
            }
        }
    }
}

@Composable
private fun ArtistCircle(
    artist: ArtistsResult,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.clip(rounded(8.dp)).clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current).data(artist.thumbnails.lastOrNull()?.url).crossfade(true).build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape),
        )
        Text(
            text = artist.artist,
            style = typo().titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** A music video at its own shape, 16:9, so the Videos row reads differently from the song rows. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoCard(
    video: VideosResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(Modifier.width(240.dp).clip(rounded(8.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(10.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current).data(video.thumbnails?.lastOrNull()?.url).crossfade(true).build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = null,
            // YouTube's hqdefault is 4:3 with black bands; the crop drops them.
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(rounded(10.dp)),
        )
        Text(
            text = video.title,
            style = typo().titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = video.artists?.map { it.name }?.connectArtists().orEmpty(),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

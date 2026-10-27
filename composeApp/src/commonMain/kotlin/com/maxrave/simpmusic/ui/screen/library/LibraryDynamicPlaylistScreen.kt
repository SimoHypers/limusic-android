package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.ArtistFullWidthItems
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.PlayCircle
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album_length
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.favorite
import simpmusic.composeapp.generated.resources.followed
import simpmusic.composeapp.generated.resources.liked_songs
import simpmusic.composeapp.generated.resources.liked_songs_count
import simpmusic.composeapp.generated.resources.most_played
import simpmusic.composeapp.generated.resources.search

@Composable
@ExperimentalMaterial3Api
fun LibraryDynamicPlaylistScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    type: String,
    viewModel: LibraryDynamicPlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val nowPlayingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    var chosenSong: SongEntity? by remember { mutableStateOf(null) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }

    val favorite by viewModel.listFavoriteSong.collectAsStateWithLifecycle()
    var tempFavorite by remember { mutableStateOf(emptyList<SongEntity>()) }
    val followed by viewModel.listFollowedArtist.collectAsStateWithLifecycle()
    var tempFollowed by remember { mutableStateOf(emptyList<ArtistEntity>()) }
    val mostPlayed by viewModel.listMostPlayedSong.collectAsStateWithLifecycle()
    var tempMostPlayed by remember { mutableStateOf(emptyList<SongEntity>()) }
    val downloaded by viewModel.listDownloadedSong.collectAsStateWithLifecycle()
    var tempDownloaded by remember { mutableStateOf(emptyList<SongEntity>()) }
    val artistLiked by viewModel.listArtistLikedSong.collectAsStateWithLifecycle()
    val artistLikedName by viewModel.artistLikedName.collectAsStateWithLifecycle()
    var tempArtistLiked by remember { mutableStateOf(emptyList<SongEntity>()) }
    val hazeState =
        rememberHazeState()

    // The other lists are observed from the database and are already loaded by the time this
    // screen opens; an artist's liked songs need the route's channel id first.
    LaunchedEffect(type) {
        val parsed = LibraryDynamicPlaylistType.toType(type)
        if (parsed is LibraryDynamicPlaylistType.ArtistLiked) {
            viewModel.getArtistLikedSong(parsed)
        }
    }

    LaunchedEffect(query) {
        tempFavorite = favorite.filter { it.matches(query) }
        tempFollowed = followed.filter { it.name.contains(query, ignoreCase = true) }
        tempMostPlayed = mostPlayed.filter { it.matches(query) }
        tempDownloaded = downloaded.filter { it.matches(query) }
        tempArtistLiked = artistLiked.filter { it.matches(query) }
    }

    LazyColumn(
        modifier = Modifier.hazeSource(hazeState),
        contentPadding = innerPadding,
    ) {
        item {
            Spacer(Modifier.height(64.dp))
        }
        item {
            AnimatedVisibility(showSearchBar) {
                Spacer(Modifier.height(55.dp))
            }
        }
        val type = LibraryDynamicPlaylistType.toType(type)
        if (type == LibraryDynamicPlaylistType.Followed) {
            items(
                if (query.isNotEmpty() && showSearchBar) {
                    tempFollowed
                } else {
                    followed
                },
                key = { it.channelId },
            ) { artist ->
                ArtistFullWidthItems(
                    artist,
                    onClickListener = {
                        navController.navigate(
                            ArtistDestination(
                                channelId = artist.channelId,
                            ),
                        )
                    },
                )
            }
        } else {
            items(
                when (type) {
                    LibraryDynamicPlaylistType.Downloaded -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempDownloaded
                        } else {
                            downloaded
                        }
                    }

                    LibraryDynamicPlaylistType.Favorite -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempFavorite
                        } else {
                            favorite
                        }
                    }

                    LibraryDynamicPlaylistType.MostPlayed -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempMostPlayed
                        } else {
                            mostPlayed
                        }
                    }

                    is LibraryDynamicPlaylistType.ArtistLiked -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempArtistLiked
                        } else {
                            artistLiked
                        }
                    }
                },
                key = { it.hashCode() },
            ) { song ->
                SongFullWidthItems(
                    songEntity = song,
                    isPlaying = song.videoId == nowPlayingVideoId,
                    modifier = Modifier.fillMaxWidth(),
                    onMoreClickListener = {
                        chosenSong = song
                        showBottomSheet = true
                    },
                    onClickListener = { videoId ->
                        viewModel.playSong(videoId, type = type)
                    },
                    onAddToQueue = {
                        sharedViewModel.addListToQueue(
                            arrayListOf(song.toTrack()),
                        )
                    },
                    selectionMode = selectionState.isActive,
                    isSelected = selectionState.isSelected(song.videoId),
                    onLongClick = { selectionState.start(it) },
                    onSelectToggle = { selectionState.toggle(it) },
                )
            }
        }
        item {
            EndOfPage()
        }
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
            song = chosenSong ?: return,
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val type = LibraryDynamicPlaylistType.toType(type)
        val isSongType = type != LibraryDynamicPlaylistType.Followed
        // Counts always come from the unfiltered lists, so the subtitle keeps reporting the
        // library total while the user is typing in the search bar.
        val subtitle =
            when (type) {
                LibraryDynamicPlaylistType.Favorite ->
                    stringResource(Res.string.album_length, favorite.size.toString(), "")
                LibraryDynamicPlaylistType.MostPlayed ->
                    stringResource(Res.string.album_length, mostPlayed.size.toString(), "")
                LibraryDynamicPlaylistType.Downloaded ->
                    stringResource(Res.string.album_length, downloaded.size.toString(), "")
                LibraryDynamicPlaylistType.Followed ->
                    "${followed.size} ${stringResource(Res.string.artists)}"
                is LibraryDynamicPlaylistType.ArtistLiked ->
                    listOfNotNull(
                        pluralStringResource(Res.plurals.liked_songs_count, artistLiked.size, artistLiked.size),
                        artistLikedName,
                    ).joinToString(" · ")
                else -> null
            }
        Box {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = type.title(),
                            style = typo().titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            SimpIcons.ArrowBackIosNew,
                            Modifier
                                .size(32.dp),
                            true,
                            tint = MaterialTheme.colorScheme.onBackground,
                        ) {
                            navController.navigateUp()
                        }
                    }
                },
                actions = {
                    if (isSongType) {
                        RippleIconButton(
                            SimpIcons.PlayCircle,
                            Modifier
                                .size(48.dp),
                            fillMaxSize = true,
                            tint = MaterialTheme.colorScheme.onBackground,
                        ) {
                            viewModel.playAll(type)
                        }
                        RippleIconButton(
                            SimpIcons.Shuffle,
                            Modifier.size(32.dp),
                            true,
                            tint = MaterialTheme.colorScheme.onBackground,
                        ) {
                            viewModel.shuffle(type)
                        }
                    }
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            if (showSearchBar) SimpIcons.Close else SimpIcons.Search,
                            Modifier
                                .size(32.dp),
                            true,
                            tint = MaterialTheme.colorScheme.onBackground,
                        ) {
                            showSearchBar = !showSearchBar
                        }
                    }
                },
                modifier =
                    Modifier
                        .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()),
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
            // Drawn last inside the same Box, with an opaque colour, so it covers the normal bar
            // instead of pushing it around — the search bar below keeps its position either way.
            if (selectionState.isActive) {
                SongSelectionTopAppBar(
                    state = selectionState,
                    onSelectAll = {
                        val visible =
                            when (type) {
                                LibraryDynamicPlaylistType.Downloaded ->
                                    (if (query.isNotEmpty() && showSearchBar) tempDownloaded else downloaded)
                                        .map { it.videoId }

                                LibraryDynamicPlaylistType.Favorite ->
                                    (if (query.isNotEmpty() && showSearchBar) tempFavorite else favorite)
                                        .map { it.videoId }

                                LibraryDynamicPlaylistType.MostPlayed ->
                                    (if (query.isNotEmpty() && showSearchBar) tempMostPlayed else mostPlayed)
                                        .map { it.videoId }

                                is LibraryDynamicPlaylistType.ArtistLiked ->
                                    (if (query.isNotEmpty() && showSearchBar) tempArtistLiked else artistLiked)
                                        .map { it.videoId }

                                else -> emptyList()
                            }
                        selectionState.toggleSelectAll(visible)
                    },
                    onOpenActions = { showSelectionSheet = true },
                    containerColor = Color.Black,
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showSearchBar,
            enter = SearchBarEnter,
            exit = SearchBarExit,
        ) {
            SearchBar(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .padding(horizontal = 12.dp),
                inputField = {
                    CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { showSearchBar = false },
                            expanded = showSearchBar,
                            onExpandedChange = { showSearchBar = it },
                            placeholder = {
                                Text(
                                    stringResource(Res.string.search),
                                    style = typo().bodySmall,
                                )
                            },
                            leadingIcon = { Icon(SimpIcons.Search, contentDescription = null) },
                        )
                    }
                },
                expanded = false,
                onExpandedChange = {},
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
            }
        }
    }
}

sealed class LibraryDynamicPlaylistType {
    data object Favorite : LibraryDynamicPlaylistType()

    data object Followed : LibraryDynamicPlaylistType()

    data object MostPlayed : LibraryDynamicPlaylistType()

    data object Downloaded : LibraryDynamicPlaylistType()

    /**
     * Liked songs crediting one artist — opened from the "Liked songs" row on that artist's page
     * (issue #2524).
     *
     * Carries only the channel id: the header's artist name is looked up by the view model, since a
     * name can hold any character and would have to survive the route string.
     */
    data class ArtistLiked(
        val channelId: String,
    ) : LibraryDynamicPlaylistType()

    /** The fixed name, for the callers that resolve a resource without composition. */
    fun name(): StringResource =
        when (this) {
            Favorite -> Res.string.favorite
            Followed -> Res.string.followed
            MostPlayed -> Res.string.most_played
            Downloaded -> Res.string.downloaded
            is ArtistLiked -> Res.string.liked_songs
        }

    /** What the header shows. */
    @Composable
    fun title(): String = stringResource(name())

    // For serialization and navigation
    fun toStringParams(): String =
        when (this) {
            Favorite -> "favorite"
            Followed -> "followed"
            MostPlayed -> "most_played"
            Downloaded -> "downloaded"
            is ArtistLiked -> ARTIST_LIKED_PREFIX + channelId
        }

    companion object {
        private const val ARTIST_LIKED_PREFIX = "artist_liked_"

        fun toType(input: String): LibraryDynamicPlaylistType =
            when (input) {
                "favorite" -> Favorite
                "followed" -> Followed
                "most_played" -> MostPlayed
                "downloaded" -> Downloaded
                else ->
                    parseArtistLiked(input)
                        ?: throw IllegalArgumentException("Unknown type: $input")
            }

        /**
         * `artist_liked_UC…` back into an [ArtistLiked], or null. The remainder is taken whole —
         * channel ids contain `_` and `-` — and only has to be non-blank: an id matching no song
         * just opens an empty list.
         */
        private fun parseArtistLiked(input: String): ArtistLiked? =
            input
                .takeIf { it.startsWith(ARTIST_LIKED_PREFIX) }
                ?.removePrefix(ARTIST_LIKED_PREFIX)
                ?.takeIf { it.isNotBlank() }
                ?.let(::ArtistLiked)
    }
}

/**
 * Whether a title or any of its artists contains [query].
 *
 * Artist names count as well as the title: people look for "everything by X" at least as often as
 * for one track, and typing an artist into a search box that only reads titles looks broken.
 *
 * [artists] is a converted `List<String>`, so every entry is checked — a featured artist matches
 * as readily as the lead.
 */
private fun matchesQuery(
    title: String,
    artists: List<String>?,
    query: String,
): Boolean =
    title.contains(query, ignoreCase = true) ||
        artists?.any { it.contains(query, ignoreCase = true) } == true

private fun SongEntity.matches(query: String): Boolean = matchesQuery(title, artistName, query)

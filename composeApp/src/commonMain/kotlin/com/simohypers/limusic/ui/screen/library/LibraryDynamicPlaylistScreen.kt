package com.simohypers.limusic.ui.screen.library

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
import com.simohypers.limusic.domain.data.entities.ArtistEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.extension.ultraThinBarStyle
import com.simohypers.limusic.ui.component.SearchBarExit
import com.simohypers.limusic.ui.component.SearchBarEnter
import com.simohypers.limusic.ui.component.ArtistFullWidthItems
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.selection.TrackSelectionBar
import com.simohypers.limusic.ui.component.selection.keyedRows
import com.simohypers.limusic.ui.component.selection.rememberTrackSelection
import com.simohypers.limusic.ui.component.selection.rowKey
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.PlayCircle
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.Shuffle
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.LibraryDynamicPlaylistViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.album_length
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.downloaded
import limusic.composeapp.generated.resources.favorite
import limusic.composeapp.generated.resources.followed
import limusic.composeapp.generated.resources.liked_songs
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.most_played
import limusic.composeapp.generated.resources.search

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

    val selection = rememberTrackSelection()

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
                    selectionMode = selection.active,
                    isSelected = selection.has(rowKey(song.videoId)),
                    onLongClick = { selection.start(rowKey(song.videoId), song.toTrack()) },
                    onSelectToggle = { selection.toggle(rowKey(song.videoId), song.toTrack()) },
                )
            }
        }
        item {
            EndOfPage()
        }
    }
    // Each list but Followed (artists) is songs, the whole list already loaded from the database.
    val searching = query.isNotEmpty() && showSearchBar
    val (allSongs, shownSongs) =
        when (LibraryDynamicPlaylistType.toType(type)) {
            LibraryDynamicPlaylistType.Downloaded -> downloaded to tempDownloaded
            LibraryDynamicPlaylistType.Favorite -> favorite to tempFavorite
            LibraryDynamicPlaylistType.MostPlayed -> mostPlayed to tempMostPlayed
            is LibraryDynamicPlaylistType.ArtistLiked -> artistLiked to tempArtistLiked
            else -> emptyList<SongEntity>() to emptyList()
        }
    val rows = remember(allSongs) { keyedRows(allSongs.map { it.toTrack() }) }
    val searchRows = remember(shownSongs) { keyedRows(shownSongs.map { it.toTrack() }) }
    TrackSelectionBar(
        selection = selection,
        rows = rows,
        bottomPadding = innerPadding.calculateBottomPadding(),
        shown = if (searching) searchRows else rows,
    )
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
                            LimusicIcons.ArrowBackIosNew,
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
                            LimusicIcons.PlayCircle,
                            Modifier
                                .size(48.dp),
                            fillMaxSize = true,
                            tint = MaterialTheme.colorScheme.onBackground,
                        ) {
                            viewModel.playAll(type)
                        }
                        RippleIconButton(
                            LimusicIcons.Shuffle,
                            Modifier.size(32.dp),
                            true,
                            tint = MaterialTheme.colorScheme.onBackground,
                        ) {
                            viewModel.shuffle(type)
                        }
                    }
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            if (showSearchBar) LimusicIcons.Close else LimusicIcons.Search,
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
                            leadingIcon = { Icon(LimusicIcons.Search, contentDescription = null) },
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

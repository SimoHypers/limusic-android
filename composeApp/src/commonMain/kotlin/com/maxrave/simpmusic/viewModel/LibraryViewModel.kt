package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.library.LibraryOverview
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.isRadioPlaylistId
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.library_synced_all
import simpmusic.composeapp.generated.resources.library_synced_none
import simpmusic.composeapp.generated.resources.library_synced_partial
import simpmusic.composeapp.generated.resources.library_syncing
import simpmusic.composeapp.generated.resources.playlist_created
import simpmusic.composeapp.generated.resources.youtube_liked_music
import simpmusic.composeapp.generated.resources.youtube_write_failed

class LibraryViewModel(
    private val dataStoreManager: DataStoreManager,
    private val songRepository: SongRepository,
    private val commonRepository: CommonRepository,
    private val playlistRepository: PlaylistRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val podcastRepository: PodcastRepository,
    private val artistRepository: ArtistRepository,
) : BaseViewModel() {
    private val _currentScreen: MutableStateFlow<LibraryChipType> = MutableStateFlow(LibraryChipType.YOUR_LIBRARY)
    val currentScreen: StateFlow<LibraryChipType> get() = _currentScreen.asStateFlow()

    // A chip was picked (a tap, or a deep link) before the one saved last time finished loading.
    private var screenChosen = false
    private val _recentlyAdded: MutableStateFlow<LocalResource<List<RecentlyType>>> =
        MutableStateFlow(LocalResource.Loading())
    val recentlyAdded: StateFlow<LocalResource<List<RecentlyType>>> get() = _recentlyAdded.asStateFlow()

    private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist.asStateFlow()

    private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist.asStateFlow()

    private val _youTubeMixForYou: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubeMixForYou: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubeMixForYou.asStateFlow()

    private val _favoritePlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePlaylist.asStateFlow()

    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePodcasts.asStateFlow()

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist.asStateFlow()

    private val _accountThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val accountThumbnail: StateFlow<String?> get() = _accountThumbnail.asStateFlow()

    /** Counts and newest artwork for the four All tab tiles; null until the first read lands. */
    private val _libraryOverview: MutableStateFlow<LibraryOverview?> = MutableStateFlow(null)
    val libraryOverview: StateFlow<LibraryOverview?> get() = _libraryOverview.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

    /** What the Sync button would push; zero while signed out, which hides it. */
    val unsyncedCount: StateFlow<Int> =
        combine(commonRepository.unsyncedLibrary(), youtubeLoggedIn) { keys, signedIn -> if (signedIn) keys.size else 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> get() = _syncing.asStateFlow()

    init {
        viewModelScope.launch {
            val currentScreenJob =
                launch {
                    dataStoreManager.getString("library_current_screen").first()?.let { chipType ->
                        LibraryChipType.fromStringValue(chipType)?.let {
                            // The chip picked meanwhile wins over the one saved last time.
                            if (!screenChosen) _currentScreen.value = it
                        }
                    }
                }
            val cookieJob =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collect {
                        _accountThumbnail.value = dataStoreManager.getString("AccountThumbUrl").first().takeIf { !it.isNullOrEmpty() }
                    }
                }
            currentScreenJob.join()
            cookieJob.join()
        }
        viewModelScope.launch {
            commonRepository.getLibraryOverview().collect { _libraryOverview.value = it }
        }
    }

    fun setCurrentScreen(chipType: LibraryChipType) {
        screenChosen = true
        _currentScreen.value = chipType
        viewModelScope.launch {
            dataStoreManager.putString("library_current_screen", chipType.toStringValue())
        }
    }

    fun getRecentlyAdded() {
        viewModelScope.launch {
            commonRepository.getAllRecentData().collectLatest { data ->
                val temp: MutableList<RecentlyType> = mutableListOf()
                temp.addAll(data)
                temp
                    .find {
                        it is PlaylistEntity && it.id.isRadioPlaylistId()
                    }.let {
                        temp.remove(it)
                    }
                temp.removeIf { it is SongEntity && it.inLibrary == Config.REMOVED_SONG_DATE_TIME }
                if (dataStoreManager.loggedIn.first() == DataStoreManager.TRUE) {
                    temp.removeIf { it is PlaylistEntity && it.id == "LM" }
                    temp.add(
                        PlaylistEntity(
                            title = getString(Res.string.youtube_liked_music),
                            author = "YouTube Music",
                            id = "LM",
                            description = "PIN",
                            thumbnails = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-songs-delhi-1200.png",
                        ),
                    )
                }
                temp.reverse()
                _recentlyAdded.value = LocalResource.Success(temp.toImmutableList())
            }
        }
    }

    fun getYouTubePlaylist() {
        _youTubePlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getLibraryPlaylist().collect { data ->
                _youTubePlaylist.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeMixedForYou() {
        _youTubeMixForYou.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getMixedForYou().collect { data ->
                _youTubeMixForYou.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeLoggedIn(): Boolean = runBlocking { dataStoreManager.loggedIn.first() } == DataStoreManager.TRUE

    fun getPlaylistFavorite() {
        viewModelScope.launch {
            albumRepository.getLikedAlbums().collect { album ->
                val temp: MutableList<PlaylistType> = mutableListOf()
                temp.addAll(album)
                playlistRepository.getLikedPlaylists().collect { playlist ->
                    temp.addAll(playlist)
                    val sortedList =
                        temp.sortedWith<PlaylistType>(
                            Comparator { p0, p1 ->
                                val timeP0: LocalDateTime? =
                                    when (p0) {
                                        is AlbumEntity -> p0.favoriteAt ?: p0.inLibrary
                                        is PlaylistEntity -> p0.favoriteAt ?: p0.inLibrary
                                        else -> null
                                    }
                                val timeP1: LocalDateTime? =
                                    when (p1) {
                                        is AlbumEntity -> p1.favoriteAt ?: p1.inLibrary
                                        is PlaylistEntity -> p1.favoriteAt ?: p1.inLibrary
                                        else -> null
                                    }
                                if (timeP0 == null || timeP1 == null) {
                                    return@Comparator if (timeP0 == null && timeP1 == null) {
                                        0
                                    } else if (timeP0 == null) {
                                        -1
                                    } else {
                                        1
                                    }
                                }
                                timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                            },
                        )
                    _favoritePlaylist.value = LocalResource.Success(sortedList)
                }
            }
        }
    }

    fun getFavoritePodcasts() {
        viewModelScope.launch {
            podcastRepository.getFavoritePodcasts().collectLatest { podcasts ->
                val sortedList = podcasts.sortedByDescending { it.favoriteTime }
                _favoritePodcasts.value = LocalResource.Success(sortedList)
            }
        }
    }

    // No reset to Loading: Library re-reads this on every visit, and a re-read must not blank the
    // list it is about to replace.
    fun getLocalPlaylist() {
        viewModelScope.launch {
            localPlaylistRepository.getAllLocalPlaylists().collect { values ->
//                    _listLocalPlaylist.postValue(values)
                _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
            }
        }
    }

    fun getDownloadedPlaylist() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadedPlaylist().collect { values ->
                _downloadedPlaylist.value = LocalResource.Success(values)
            }
        }
    }

    /**
     * Desktop's Sync button: put everything saved on this phone onto the YouTube account, through
     * the same writes a tap makes. One at a time, because these are writes to someone's account
     * and a burst is what gets a session rate-limited. What fails keeps its key, so pressing
     * again retries exactly what is left.
     */
    fun syncToYouTube() {
        if (_syncing.value) return
        _syncing.value = true
        viewModelScope.launch {
            val keys = commonRepository.unsyncedLibrary().first()
            makeToast(getString(Res.string.library_syncing, keys.size))
            // A playlist the account library already lists needs no write; your own refuse one.
            val known =
                if (keys.any { it.startsWith("playlist:") }) {
                    playlistRepository.getLibraryPlaylist().first().orEmpty().map { it.browseId.removePrefix("VL") }.toSet()
                } else {
                    emptySet()
                }
            var failed = 0
            for (key in keys) {
                val id = key.substringAfter(':')
                val synced =
                    when (key.substringBefore(':')) {
                        "song" -> songRepository.updateLikeStatus(id, 1)
                        "album" -> albumRepository.updateAlbumLiked(id, 1)
                        "artist" -> artistRepository.updateFollowedStatus(id, 1)
                        "playlist" if id in known -> true.also { commonRepository.markSynced(key) }
                        "playlist" -> playlistRepository.updatePlaylistLiked(id, 1)
                        else -> true.also { commonRepository.markSynced(key) }
                    }
                if (!synced) failed++
                delay(SYNC_GAP_MS)
            }
            val done = keys.size - failed
            makeToast(
                when {
                    failed == 0 -> getString(Res.string.library_synced_all, done)
                    done == 0 -> getString(Res.string.library_synced_none, failed)
                    else -> getString(Res.string.library_synced_partial, done, keys.size, failed)
                },
            )
            _syncing.value = false
        }
    }

    /** Where New playlist puts one, desktop's choice: the account unless the last pick was this phone. */
    val newPlaylistOnYouTube: StateFlow<Boolean> =
        dataStoreManager
            .getString(NEW_PLAYLIST_WHERE)
            .map { it != "device" }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** [onCreated] gets the YouTube playlist's id, or null for one made on this phone. */
    fun createPlaylist(
        title: String,
        onYouTube: Boolean,
        onResult: (Boolean) -> Unit,
        onCreated: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            // Signed out there was no choice to remember.
            if (dataStoreManager.loggedIn.first() == DataStoreManager.TRUE) {
                dataStoreManager.putString(NEW_PLAYLIST_WHERE, if (onYouTube) "youtube" else "device")
            }
            if (onYouTube) {
                val id = playlistRepository.createYouTubePlaylist(title)
                if (id == null) {
                    makeToast(getString(Res.string.youtube_write_failed))
                    onResult(false)
                    return@launch
                }
                onCreated(id)
            } else {
                localPlaylistRepository.insertLocalPlaylist(LocalPlaylistEntity(title = title), "").lastOrNull()
                getLocalPlaylist()
                onCreated(null)
            }
            makeToast(getString(Res.string.playlist_created, title))
            onResult(true)
        }
    }

    fun deleteSong(videoId: String) {
        _recentlyAdded.value = LocalResource.Loading()
        viewModelScope.launch {
            songRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
            songRepository.resetTotalPlayTime(videoId)
            delay(500) // Wait for the database to update
            getRecentlyAdded()
        }
    }
}

// ponytail: a fixed gap between Sync's writes, not a rate limiter; enough to keep a few hundred
// likes from leaving as one burst.
private const val SYNC_GAP_MS = 250L

private const val NEW_PLAYLIST_WHERE = "new_playlist_where"

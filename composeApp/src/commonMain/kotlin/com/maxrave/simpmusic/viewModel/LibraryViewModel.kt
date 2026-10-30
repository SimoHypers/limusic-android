package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.download.DownloadQueue
import com.maxrave.domain.data.model.library.LibraryOverview
import com.maxrave.domain.data.player.PlaybackRefusals
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.isRadioPlaylistId
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.type.ArtistType
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.PlaylistType as QueuePlaylistType
import com.maxrave.domain.utils.toTrack
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.library_synced_all
import simpmusic.composeapp.generated.resources.library_synced_none
import simpmusic.composeapp.generated.resources.library_synced_partial
import simpmusic.composeapp.generated.resources.library_syncing
import simpmusic.composeapp.generated.resources.playlist_created
import simpmusic.composeapp.generated.resources.songs
import simpmusic.composeapp.generated.resources.youtube_liked_music

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

    private val _savedPlaylists: MutableStateFlow<List<PlaylistEntity>> = MutableStateFlow(emptyList())

    /** Other people's playlists saved on this phone that the account's list doesn't carry. */
    val savedPlaylists: StateFlow<List<PlaylistEntity>> get() = _savedPlaylists.asStateFlow()

    private val _albums: MutableStateFlow<LocalResource<List<PlaylistType>>> = MutableStateFlow(LocalResource.Loading())
    val albums: StateFlow<LocalResource<List<PlaylistType>>> get() = _albums.asStateFlow()

    private val _artists: MutableStateFlow<LocalResource<List<ArtistType>>> = MutableStateFlow(LocalResource.Loading())

    /** The artists behind the songs in the account's library (signed in only). */
    val artists: StateFlow<LocalResource<List<ArtistType>>> get() = _artists.asStateFlow()

    private val _subscriptions: MutableStateFlow<LocalResource<List<ArtistType>>> = MutableStateFlow(LocalResource.Loading())

    /** The account's subscriptions plus follows still waiting for Sync; signed out, the phone's follows. */
    val subscriptions: StateFlow<LocalResource<List<ArtistType>>> get() = _subscriptions.asStateFlow()

    private val _songs: MutableStateFlow<LocalResource<List<Track>>> = MutableStateFlow(LocalResource.Loading())

    /** Library ▸ Songs signed in, the phone's liked songs signed out. */
    val songs: StateFlow<LocalResource<List<Track>>> get() = _songs.asStateFlow()
    private var songsContinuation: String? = null
    private var songsLoadingMore = false

    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePodcasts.asStateFlow()

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist.asStateFlow()

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

    private val downloadHandler: DownloadHandler by inject()
    private val cacheRepository: CacheRepository by inject()

    val downloadQueue: StateFlow<DownloadQueue> get() = downloadHandler.queue

    /** Every downloaded song: the Downloads tiles count theirs against it. */
    val downloadedIds: StateFlow<Set<String>> =
        songRepository.downloadedVideoIds().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** The space downloads take, read again whenever the downloaded count moves. */
    val downloadedBytes: StateFlow<Long> =
        libraryOverview
            .map { it?.downloaded?.count }
            .distinctUntilChanged()
            .map { cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /** Songs YouTube refused, this run or for an earlier download: the Downloads counts leave them out. */
    val refusedIds: StateFlow<Set<String>> =
        PlaybackRefusals.refusedIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Songs of the downloaded playlists and albums that are not on the phone, minus those YouTube refused. */
    val missingDownloads: StateFlow<List<String>> =
        combine(downloadedPlaylist, downloadedIds, refusedIds) { lists, downloaded, refused ->
            lists.data
                .orEmpty()
                .flatMap { it.trackIds() }
                .distinct()
                .filter { it !in downloaded && it !in refused }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Asks again for the refused songs of these playlists and albums too. */
    fun retryMissingDownloads() {
        val kept = downloadedPlaylist.value.data.orEmpty().flatMapTo(HashSet()) { it.trackIds() }
        val ids = missingDownloads.value + PlaybackRefusals.forgetDownloadRefusals().filter { it in kept }
        viewModelScope.launch { downloadHandler.downloadTracks(songRepository.getSongsByListVideoId(ids).first()) }
    }

    fun setDownloadsPaused(paused: Boolean) = downloadHandler.setPaused(paused)

    init {
        viewModelScope.launch {
            dataStoreManager.getString("library_current_screen").first()?.let { chipType ->
                LibraryChipType.fromStringValue(chipType)?.let {
                    // The chip picked meanwhile wins over the one saved last time.
                    if (!screenChosen) _currentScreen.value = it
                }
            }
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

    private suspend fun signedIn() = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE

    /**
     * What a tab merges in from the phone, like desktop's `mergeSaved`: signed out the phone's own
     * saves are the whole library; signed in only those still waiting for Sync, because the
     * account's list is the truth for the rest.
     */
    private suspend fun <T> phoneSaves(
        all: List<T>,
        key: (T) -> String,
    ): List<T> {
        if (!signedIn()) return all
        val unsynced = commonRepository.unsyncedLibrary().first()
        return all.filter { key(it) in unsynced }
    }

    fun getSavedPlaylists() {
        viewModelScope.launch {
            val liked =
                playlistRepository
                    .getLikedPlaylists()
                    .first()
                    .filter { it.id.removePrefix("VL") != "LM" && !it.id.isRadioPlaylistId() }
            _savedPlaylists.value = phoneSaves(liked) { "playlist:${it.id.removePrefix("VL")}" }
        }
    }

    fun getAlbums() {
        _albums.value = LocalResource.Loading()
        viewModelScope.launch {
            val youTube = if (signedIn()) albumRepository.getLibraryAlbums() else null
            val listed = youTube.orEmpty().mapTo(HashSet()) { it.browseId }
            val local = phoneSaves(albumRepository.getLikedAlbums().first()) { "album:${it.browseId}" }
            _albums.value = LocalResource.Success(local.filter { it.browseId !in listed } + youTube.orEmpty())
        }
    }

    fun getArtists() {
        _subscriptions.value = LocalResource.Loading()
        viewModelScope.launch {
            val followed = artistRepository.getFollowedArtists().first()
            if (!signedIn()) {
                _subscriptions.value = LocalResource.Success(followed)
                _artists.value = LocalResource.Success(emptyList())
                return@launch
            }
            val youTube = artistRepository.getLibraryArtists(subscriptions = true)
            val listed = youTube.orEmpty().mapTo(HashSet()) { it.browseId }
            // The account could not be asked: the phone's follows are the best answer there is.
            val local = if (youTube == null) followed else phoneSaves(followed) { "artist:${it.channelId}" }
            _subscriptions.value = LocalResource.Success(local.filter { it.channelId !in listed } + youTube.orEmpty())
            _artists.value = LocalResource.Success(artistRepository.getLibraryArtists(subscriptions = false).orEmpty())
        }
    }

    fun getSongs() {
        _songs.value = LocalResource.Loading()
        songsContinuation = null
        viewModelScope.launch {
            if (!signedIn()) {
                _songs.value = LocalResource.Success(songRepository.getLikedSongs().first().map { it.toTrack() })
                return@launch
            }
            val page = songRepository.getLibrarySongs(null)
            songsContinuation = page?.second
            _songs.value = page?.let { LocalResource.Success(it.first.distinctBy { track -> track.videoId }) } ?: LocalResource.Error("")
        }
    }

    /** The next page of Library ▸ Songs, when the list is scrolled to its end. */
    fun getMoreSongs() {
        val token = songsContinuation ?: return
        if (songsLoadingMore) return
        songsLoadingMore = true
        viewModelScope.launch {
            val page = songRepository.getLibrarySongs(token)
            if (page != null) {
                songsContinuation = page.second
                // Keyed by videoId on screen, so a song two pages both carry must not show twice.
                _songs.value = LocalResource.Success((_songs.value.data.orEmpty() + page.first).distinctBy { it.videoId })
            }
            songsLoadingMore = false
        }
    }

    // ponytail: the queue is the songs loaded so far; it does not page further on its own.
    fun playSong(videoId: String) {
        val list = _songs.value.data ?: return
        val track = list.find { it.videoId == videoId } ?: return
        setQueueData(
            QueueData.Data(
                listTracks = ArrayList(list),
                firstPlayedTrack = track,
                playlistId = null,
                playlistName = getString(Res.string.songs),
                playlistType = QueuePlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(track, Config.PLAYLIST_CLICK, list.indexOf(track))
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

    /** After New playlist made one: say so, and list it if it lives on the phone. */
    fun playlistCreated(
        title: String,
        onPhone: Boolean,
    ) {
        viewModelScope.launch {
            if (onPhone) getLocalPlaylist()
            makeToast(getString(Res.string.playlist_created, title))
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

/** The songs a downloaded playlist, album or local playlist was asked to hold. */
fun PlaylistType.trackIds(): List<String> =
    when (this) {
        is PlaylistEntity -> tracks
        is AlbumEntity -> tracks
        is LocalPlaylistEntity -> tracks
        else -> null
    }.orEmpty()

package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.SELECTED_LANGUAGE
import com.simohypers.limusic.domain.data.entities.SearchHistory
import com.simohypers.limusic.domain.data.model.searchResult.albums.AlbumsResult
import com.simohypers.limusic.domain.data.model.searchResult.artists.ArtistsResult
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.model.searchResult.songs.SongsResult
import com.simohypers.limusic.domain.data.model.mood.Mood
import com.simohypers.limusic.domain.data.model.searchResult.videos.VideosResult
import com.simohypers.limusic.domain.data.type.SearchResultType
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.repository.HomeRepository
import com.simohypers.limusic.domain.repository.SearchRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import limusic.composeapp.generated.resources.starting_radio
import org.jetbrains.compose.resources.getString
import limusic.composeapp.generated.resources.error
import limusic.composeapp.generated.resources.radio
import com.simohypers.limusic.domain.utils.Resource
import com.simohypers.limusic.domain.utils.toQueryList
import com.simohypers.limusic.logger.LogLevel
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.albums
import limusic.composeapp.generated.resources.all
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.playlists
import limusic.composeapp.generated.resources.podcasts
import limusic.composeapp.generated.resources.songs
import limusic.composeapp.generated.resources.videos

// State cho tìm kiếm
data class SearchScreenState(
    val searchType: SearchType = SearchType.ALL,
    /** YouTube's top result, its own line and up to three of its songs (All only). */
    val top: SearchResultType? = null,
    val topSubtitle: String = "",
    val topSongs: List<SongsResult> = emptyList(),
    val searchSongsResult: List<SongsResult> = emptyList(),
    val searchVideosResult: List<VideosResult> = emptyList(),
    val searchAlbumsResult: List<AlbumsResult> = emptyList(),
    val searchArtistsResult: List<ArtistsResult> = emptyList(),
    val searchPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchPodcastsResult: List<PlaylistsResult> = emptyList(),
    val suggestQueries: List<String> = emptyList(),
    val suggestYTItems: List<SearchResultType> = emptyList(),
)

// Desktop's chips, plus Podcasts.
enum class SearchType {
    ALL,
    SONGS,
    VIDEOS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    PODCASTS,
}

fun SearchType.toStringRes(): StringResource =
    when (this) {
        SearchType.ALL -> Res.string.all
        SearchType.SONGS -> Res.string.songs
        SearchType.VIDEOS -> Res.string.videos
        SearchType.ALBUMS -> Res.string.albums
        SearchType.ARTISTS -> Res.string.artists
        SearchType.PLAYLISTS -> Res.string.playlists
        SearchType.PODCASTS -> Res.string.podcasts
    }

// UI state cho tìm kiếm
sealed class SearchScreenUIState {
    object Empty : SearchScreenUIState()

    object Loading : SearchScreenUIState()

    object Success : SearchScreenUIState()

    object Error : SearchScreenUIState()
}

class SearchViewModel(
    private val dataStoreManager: DataStoreManager,
    private val searchRepository: SearchRepository,
    private val homeRepository: HomeRepository,
    private val songRepository: SongRepository,
) : BaseViewModel() {
    private val _searchScreenUIState = MutableStateFlow<SearchScreenUIState>(SearchScreenUIState.Empty)
    val searchScreenUIState: StateFlow<SearchScreenUIState> get() = _searchScreenUIState.asStateFlow()

    private val _searchScreenState = MutableStateFlow(SearchScreenState())
    val searchScreenState: StateFlow<SearchScreenState> get() = _searchScreenState.asStateFlow()

    private val _searchHistory: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val searchHistory: StateFlow<List<String>> get() = _searchHistory.asStateFlow()

    /** Browse categories shown on the empty search screen, the way YouTube Music itself does. */
    private val _moodAndGenres: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val moodAndGenres: StateFlow<Mood?> get() = _moodAndGenres.asStateFlow()

    /** Cover art per category params, filled in as tiles scroll into view. */
    private val _moodArtwork: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
    val moodArtwork: StateFlow<Map<String, String>> get() = _moodArtwork.asStateFlow()

    private val requestedArtwork = mutableSetOf<String>()

    var regionCode: String? = null
    var language: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
        getSearchHistory()
        getMoodAndGenres()
    }

    /**
     * Fetched once for the app's lifetime — this ViewModel is a Koin `single`, and the category
     * list changes about as often as YouTube ships a new mood, so re-fetching on every visit to
     * the search tab would be pure waste.
     */
    private fun getMoodAndGenres() {
        if (_moodAndGenres.value != null) return
        viewModelScope.launch {
            homeRepository.getMoodAndMomentsData().collect { resource ->
                if (resource is Resource.Success) {
                    _moodAndGenres.value = resource.data
                }
            }
        }
    }

    /**
     * Resolve the cover for one category. Called from the tile itself, so only categories the user
     * actually scrolls to cost a request — [requestedArtwork] keeps a tile scrolling in and out of
     * view from firing it again.
     */
    fun loadMoodArtwork(params: String) {
        if (!requestedArtwork.add(params)) return
        viewModelScope.launch {
            homeRepository.getMoodCategoryArtwork(params).collect { url ->
                if (url != null) {
                    _moodArtwork.update { it + (params to url) }
                }
            }
        }
    }

    private fun getSearchHistory() {
        viewModelScope.launch {
            searchRepository.getSearchHistory().collect { values ->
                if (values.isNotEmpty()) {
                    values.toQueryList().reversed().let { list ->
                        _searchHistory.value = list
                        log("Search history updated: $list")
                    }
                } else {
                    _searchHistory.value = emptyList()
                    log("Search history is empty")
                }
            }
        }
    }

    fun insertSearchHistory(query: String) {
        viewModelScope.launch {
            searchRepository.insertSearchHistory(SearchHistory(query = query)).collectLatest {
                Logger.d(tag, "Inserted search history: $query, $it")
                getSearchHistory()
            }
        }
    }

    fun deleteSearchHistory() {
        _searchHistory.value = emptyList()
        viewModelScope.launch { searchRepository.deleteSearchHistory() }
    }

    /** One recent search, gone at once from the row; the database follows. */
    fun forgetSearch(query: String) {
        _searchHistory.update { it - query }
        viewModelScope.launch { searchRepository.deleteSearchQuery(query) }
    }

    private var query = ""
    private var searchJob: Job? = null
    private var cardsJob: Job? = null

    // Chips whose full list is its own filtered search, and which of them hold this query's list.
    private val cardTypes = setOf(SearchType.ALBUMS, SearchType.ARTISTS, SearchType.PLAYLISTS, SearchType.PODCASTS)
    private val loaded = mutableSetOf<SearchType>()

    /**
     * Desktop's search: the unfiltered search (top result, albums, artists, playlists) and the
     * songs and videos searches, in parallel, three requests in all. The other chips fetch their
     * own list when opened (see [setSearchType]); the chip stays where it was, like desktop.
     */
    fun search(query: String) {
        this.query = query
        // A newer search supersedes one still in flight.
        searchJob?.cancel()
        cardsJob?.cancel()
        loaded.clear()
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                val all = async { searchRepository.getSearchDataAll(query).lastOrNull() }
                val songs = async { searchRepository.getSearchDataSong(query).lastOrNull() }
                val videos = async { searchRepository.getSearchDataVideo(query).lastOrNull() }
                val a = all.await()?.data
                val s = songs.await()?.data
                val v = videos.await()?.data
                if (a == null && s == null && v == null) {
                    _searchScreenUIState.value = SearchScreenUIState.Error
                    reloadWhenOnline({ this@SearchViewModel.query == query && _searchScreenUIState.value == SearchScreenUIState.Error }) { search(query) }
                    return@launch
                }
                _searchScreenState.update {
                    it.copy(
                        top = a?.top,
                        topSubtitle = a?.topSubtitle.orEmpty(),
                        topSongs = a?.topSongs.orEmpty(),
                        searchSongsResult = s.orEmpty(),
                        searchVideosResult = v.orEmpty(),
                        searchAlbumsResult = a?.albums.orEmpty(),
                        searchArtistsResult = a?.artists.orEmpty(),
                        searchPlaylistsResult = a?.playlists.orEmpty(),
                        searchPodcastsResult = emptyList(),
                    )
                }
                val type = _searchScreenState.value.searchType
                if (type in cardTypes) loadCards(type) else _searchScreenUIState.value = SearchScreenUIState.Success
            }
    }

    fun setSearchType(searchType: SearchType) {
        _searchScreenState.update { it.copy(searchType = searchType) }
        // The search in flight opens whichever chip is current when it lands.
        if (searchJob?.isActive == true) return
        cardsJob?.cancel()
        if (searchType in cardTypes && searchType !in loaded && query.isNotEmpty()) {
            loadCards(searchType)
        } else if (_searchScreenUIState.value == SearchScreenUIState.Loading) {
            // Left a chip mid-load: what this one shows is already here.
            _searchScreenUIState.value = SearchScreenUIState.Success
        }
    }

    private fun loadCards(type: SearchType) {
        val query = query
        _searchScreenUIState.value = SearchScreenUIState.Loading
        cardsJob =
            viewModelScope.launch {
                val ok =
                    when (type) {
                        SearchType.ALBUMS ->
                            searchRepository.getSearchDataAlbum(query).lastOrNull()?.data?.also { list ->
                                _searchScreenState.update { it.copy(searchAlbumsResult = list) }
                            }
                        SearchType.ARTISTS ->
                            searchRepository.getSearchDataArtist(query).lastOrNull()?.data?.also { list ->
                                _searchScreenState.update { it.copy(searchArtistsResult = list) }
                            }
                        SearchType.PLAYLISTS ->
                            searchRepository.getSearchDataPlaylist(query).lastOrNull()?.data?.also { list ->
                                _searchScreenState.update { it.copy(searchPlaylistsResult = list) }
                            }
                        else ->
                            searchRepository.getSearchDataPodcast(query).lastOrNull()?.data?.also { list ->
                                _searchScreenState.update { it.copy(searchPodcastsResult = list) }
                            }
                    } != null
                if (ok) loaded += type
                _searchScreenUIState.value = if (ok) SearchScreenUIState.Success else SearchScreenUIState.Error
                if (!ok) {
                    reloadWhenOnline({ this@SearchViewModel.query == query && _searchScreenState.value.searchType == type && type !in loaded }) {
                        loadCards(type)
                    }
                }
            }
    }

    /** What the top result's play button means for an artist, as on desktop: its radio. */
    fun startArtistRadio(artist: ArtistsResult) {
        if (artist.radioId.isEmpty()) return
        viewModelScope.launch {
            val (tracks, continuation) =
                songRepository
                    .getRadioFromEndpoint(YouTubeWatchEndpoint(playlistId = artist.radioId))
                    .busy(Res.string.starting_radio)
                    .lastOrNull()
                    ?.data
                    ?.takeIf { it.first.isNotEmpty() }
                    ?: return@launch makeToast(getString(Res.string.error))
            setQueueData(
                QueueData.Data(
                    listTracks = tracks,
                    firstPlayedTrack = tracks.first(),
                    playlistId = artist.radioId,
                    playlistName = "${artist.artist} ${getString(Res.string.radio)}",
                    playlistType = PlaylistType.RADIO,
                    continuation = continuation,
                ),
            )
            loadMediaItem(tracks.first(), Config.PLAYLIST_CLICK, 0)
        }
    }

    fun suggestQuery(query: String) {
        viewModelScope.launch {
            searchRepository.getSuggestQuery(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { suggestData ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    suggestQueries = suggestData.queries,
                                    suggestYTItems = suggestData.recommendedItems,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        // Không cần xử lý lỗi đặc biệt cho gợi ý
                        log("Error fetching suggest queries: ${values.message}", LogLevel.ERROR)
                    }
                }
            }
        }
    }

}
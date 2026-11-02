package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.common.Config.REMOVED_SONG_DATE_TIME
import com.simohypers.limusic.domain.data.entities.ArtistEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.repository.ArtistRepository
import com.simohypers.limusic.domain.repository.CacheRepository
import com.simohypers.limusic.domain.repository.CommonRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.utils.PlaylistSort
import com.simohypers.limusic.domain.utils.toArrayListTrack
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.ui.screen.library.LibraryDynamicPlaylistType
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/** The Downloaded songs order, kept on the phone like a playlist's local sort. */
data class DownloadSort(
    val sort: PlaylistSort = PlaylistSort.DEFAULT,
    val desc: Boolean = false,
) {
    val isDefault get() = sort == PlaylistSort.DEFAULT && !desc
}

/** What Downloaded songs offers: its own newest-first order, Title and Artist, each reversible. */
val DOWNLOAD_SORTS = listOf(PlaylistSort.DEFAULT, PlaylistSort.TITLE, PlaylistSort.ARTIST)

/**
 * Liked songs, Downloaded songs, Most played, Followed and one artist's liked songs. The screen says
 * which with [open]; only that list is read, and it follows the database while the page is open.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryDynamicPlaylistViewModel(
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
) : BaseViewModel() {
    private val commonRepository: CommonRepository by inject()
    private val cacheRepository: CacheRepository by inject()
    private val dataStoreManager: DataStoreManager by inject()

    private val type = MutableStateFlow<LibraryDynamicPlaylistType?>(null)

    fun open(type: LibraryDynamicPlaylistType) {
        this.type.value = type
    }

    // The liked and followed reads are one-shot (paged through the whole table); the Library
    // cards' counts and newest covers are observed, so a like, unlike or follow anywhere in the
    // app moves them, and each move reads the list again.
    private val overview = commonRepository.getLibraryOverview().shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val downloadSort: StateFlow<DownloadSort> =
        dataStoreManager
            .getString(DOWNLOAD_SORT_KEY)
            .map { stored ->
                val (name, dir) = stored?.split(":")?.let { it[0] to it.getOrNull(1) } ?: return@map DownloadSort()
                PlaylistSort.entries.firstOrNull { it.name == name && it in DOWNLOAD_SORTS }?.let { DownloadSort(it, dir == "desc") } ?: DownloadSort()
            }.stateIn(viewModelScope, SharingStarted.Eagerly, DownloadSort())

    fun setDownloadSort(sort: DownloadSort) {
        viewModelScope.launch {
            dataStoreManager.putString(DOWNLOAD_SORT_KEY, if (sort.isDefault) "" else sort.sort.name + if (sort.desc) ":desc" else "")
        }
    }

    /** Null until the first read lands, so the page can tell "loading" from "empty". */
    val songs: StateFlow<List<SongEntity>?> =
        type
            .filterNotNull()
            .flatMapLatest { t ->
                when (t) {
                    LibraryDynamicPlaylistType.Favorite ->
                        reread(overview.map { it.favorite }) { songRepository.getLikedSongs() }.map { it.newestLikedFirst() }
                    LibraryDynamicPlaylistType.Downloaded ->
                        combine(
                            reread(songRepository.downloadedVideoIds()) { songRepository.getDownloadedSongs() },
                            downloadSort,
                        ) { list, sort -> list.orEmpty().sortedByDescending { it.downloadedAt ?: REMOVED_SONG_DATE_TIME }.sortedBy(sort) }
                    LibraryDynamicPlaylistType.MostPlayed ->
                        songRepository.getMostPlayedSongs().map { list -> list.sortedByDescending { it.totalPlayTime } }
                    is LibraryDynamicPlaylistType.ArtistLiked ->
                        songRepository.getLikedSongsByArtist(t.channelId).map { it.newestLikedFirst() }
                    LibraryDynamicPlaylistType.Followed -> flowOf(emptyList())
                }
            }.flowOn(Dispatchers.Default)
            // Room re-emits on any change to the song table (download progress every 500 ms); an
            // equal list is no news.
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val artists: StateFlow<List<ArtistEntity>?> =
        type
            .filterNotNull()
            .flatMapLatest { t ->
                if (t != LibraryDynamicPlaylistType.Followed) {
                    flowOf(null)
                } else {
                    reread(overview.map { it.followed }) { artistRepository.getFollowedArtists() }
                        .map { list -> list.sortedByDescending { it.followedAt ?: REMOVED_SONG_DATE_TIME } }
                }
            }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** The artist behind an artist's liked songs: name and photo for the header. */
    val artist: StateFlow<ArtistEntity?> =
        type
            .filterIsInstance<LibraryDynamicPlaylistType.ArtistLiked>()
            .flatMapLatest { artistRepository.getArtistById(it.channelId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** The space downloads take, read again whenever a download lands or goes. */
    val downloadedBytes: StateFlow<Long> =
        type
            .filterNotNull()
            .flatMapLatest { t ->
                if (t != LibraryDynamicPlaylistType.Downloaded) {
                    flowOf(0L)
                } else {
                    songRepository.downloadedVideoIds().distinctUntilChanged().map { cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE) }
                }
            }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private fun <K, T> reread(
        signal: Flow<K>,
        read: () -> Flow<T>,
    ): Flow<T> = signal.distinctUntilChanged().flatMapLatest { read() }

    private fun List<SongEntity>.newestLikedFirst() = sortedByDescending { it.favoriteAt ?: REMOVED_SONG_DATE_TIME }

    private fun List<SongEntity>.sortedBy(sort: DownloadSort): List<SongEntity> {
        if (sort.isDefault) return this
        val byId = associateBy { it.videoId }
        return sortTracks(map { it.toTrack() }, sort.sort, baseNewestFirst = true, desc = sort.desc).mapNotNull { byId[it.videoId] }
    }

    /**
     * What the Now Playing screen calls the queue this page started; the page also compares it
     * with the playing queue's name to show Pause.
     */
    fun queueName(type: LibraryDynamicPlaylistType): String {
        val name = getString(type.name())
        return if (type is LibraryDynamicPlaylistType.ArtistLiked) listOfNotNull(name, artist.value?.name).joinToString(" · ") else name
    }

    fun playSong(
        videoId: String,
        type: LibraryDynamicPlaylistType,
    ) {
        val list = songs.value.orEmpty()
        val index = list.indexOfFirst { it.videoId == videoId }
        if (index < 0) return
        play(type, list, index)
    }

    fun playAll(type: LibraryDynamicPlaylistType) {
        val list = songs.value.orEmpty()
        if (list.isNotEmpty()) play(type, list, 0)
    }

    fun shuffle(type: LibraryDynamicPlaylistType) {
        val list = songs.value.orEmpty()
        if (list.isEmpty()) return
        setQueueData(
            QueueData.Data(
                listTracks = list.toArrayListTrack(),
                playlistId = null,
                playlistName = queueName(type),
                playlistType = PlaylistType.RADIO,
                shuffle = true,
            ),
        )
        loadMediaItem(list.first().toTrack(), Config.PLAYLIST_CLICK, null)
    }

    private fun play(
        type: LibraryDynamicPlaylistType,
        list: List<SongEntity>,
        index: Int,
    ) {
        val first = list[index].toTrack()
        setQueueData(
            QueueData.Data(
                listTracks = list.toArrayListTrack(),
                firstPlayedTrack = first,
                playlistId = null,
                playlistName = queueName(type),
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(first, Config.PLAYLIST_CLICK, index)
    }

    private companion object {
        const val DOWNLOAD_SORT_KEY = "playlist_sort_downloaded"
    }
}

package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.ui.component.DateBucket
import com.simohypers.limusic.ui.component.dateBucket
import com.simohypers.limusic.ui.component.selection.keyedRows
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.history
import org.koin.core.component.inject

enum class HistorySource { YOUTUBE, PHONE }

/**
 * One heading of the history and its rows, each row with its selection key. YouTube names its own
 * days ([title]); the phone's are worked out from when each song last played ([bucket]).
 */
data class HistoryGroup(
    val title: String?,
    val bucket: DateBucket?,
    val rows: List<Pair<String, Track>>,
)

sealed interface HistoryState {
    data object Loading : HistoryState

    data object Failed : HistoryState

    data class Ready(
        val groups: List<HistoryGroup>,
    ) : HistoryState
}

/**
 * Desktop's History page: the account's YouTube Music history, or what this phone played. YouTube
 * is asked once per visit (and on Retry); the phone's list is read each time the page shows.
 */
class RecentlySongsViewModel(
    private val songRepository: SongRepository,
) : BaseViewModel() {
    private val dataStoreManager: DataStoreManager by inject()

    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    /** Null until sign-in is known, so the page doesn't flash the phone's list first. */
    private val _source = MutableStateFlow<HistorySource?>(null)
    val source: StateFlow<HistorySource?> = _source.asStateFlow()

    private val _youtube = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val youtube: StateFlow<HistoryState> = _youtube.asStateFlow()

    private val _phone = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val phone: StateFlow<HistoryState> = _phone.asStateFlow()

    private var youtubeJob: Job? = null

    init {
        viewModelScope.launch {
            val signedIn = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE
            _signedIn.value = signedIn
            _source.value = if (signedIn) HistorySource.YOUTUBE else HistorySource.PHONE
            if (signedIn) loadYouTube(fallBack = true)
        }
    }

    fun setSource(source: HistorySource) {
        _source.value = source
        if (source == HistorySource.YOUTUBE && _youtube.value == HistoryState.Failed) loadYouTube(fallBack = false)
    }

    fun retryYouTube() = loadYouTube(fallBack = false)

    /** [fallBack]: the first try, which hands the page to the phone's list if YouTube can't answer. */
    private fun loadYouTube(fallBack: Boolean) {
        if (youtubeJob?.isActive == true) return
        _youtube.value = HistoryState.Loading
        youtubeJob =
            viewModelScope.launch {
                val days = songRepository.getHistory()
                if (days == null) {
                    _youtube.value = HistoryState.Failed
                    if (fallBack && _source.value == HistorySource.YOUTUBE) _source.value = HistorySource.PHONE
                } else {
                    _youtube.value = HistoryState.Ready(keyed(days.map { Triple(it.title, null, it.tracks) }))
                }
            }
    }

    fun refreshPhone() {
        viewModelScope.launch {
            val songs = songRepository.getPlayedSongs(PHONE_LIMIT)
            val today = now().date
            val groups = mutableListOf<Triple<String?, DateBucket?, List<Track>>>()
            var start = 0
            // Last played first, so each heading's songs sit together.
            while (start < songs.size) {
                val bucket = dateBucket(songs[start].inLibrary, today, days = true)
                var end = start + 1
                while (end < songs.size && dateBucket(songs[end].inLibrary, today, days = true) == bucket) end++
                groups += Triple(null, bucket, songs.subList(start, end).map { it.toTrack() })
                start = end
            }
            _phone.value = HistoryState.Ready(keyed(groups))
        }
    }

    /** Keys over the whole history, so a song played on two days is two rows, picked apart. */
    private fun keyed(groups: List<Triple<String?, DateBucket?, List<Track>>>): List<HistoryGroup> {
        val rows = keyedRows(groups.flatMap { it.third })
        var at = 0
        return groups.map { (title, bucket, tracks) ->
            HistoryGroup(title, bucket, rows.subList(at, at + tracks.size)).also { at += tracks.size }
        }
    }

    /** Desktop's rule: the whole history in order from [start], never the filtered view. */
    fun play(
        tracks: List<Track>,
        start: Int,
    ) {
        val first = tracks.getOrNull(start) ?: return
        setQueueData(
            QueueData.Data(
                listTracks = ArrayList(tracks),
                firstPlayedTrack = first,
                playlistId = null,
                playlistName = getString(Res.string.history),
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(first, Config.PLAYLIST_CLICK, start)
    }

    fun shuffle(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        setQueueData(
            QueueData.Data(
                listTracks = ArrayList(tracks),
                playlistId = null,
                playlistName = getString(Res.string.history),
                playlistType = PlaylistType.RADIO,
                shuffle = true,
            ),
        )
        loadMediaItem(tracks.first(), Config.PLAYLIST_CLICK, null)
    }

    private companion object {
        // ponytail: the newest 500 plays; a year of listening is more rows than anyone scrolls.
        const val PHONE_LIMIT = 500
    }
}

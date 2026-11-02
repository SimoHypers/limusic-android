package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.domain.data.entities.NotificationEntity
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.repository.CommonRepository
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.core.component.inject

/** One album or single the release check found, with the artist it came from. */
data class Release(
    val browseId: String,
    val title: String,
    val thumbnail: String?,
    val isAlbum: Boolean,
    val artist: String,
    val channelId: String,
    val artistThumbnail: String?,
    val foundAt: LocalDateTime,
)

/** When New releases was last opened, as ISO text: Library's bell shows a dot for anything newer. */
const val RELEASES_SEEN_KEY = "releases_seen_at"

suspend fun DataStoreManager.releasesSeenAt(): LocalDateTime? =
    getString(RELEASES_SEEN_KEY).first()?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

/**
 * New releases: every album and single the release check (NotifyWork) has found, newest first, one
 * entry each. Opening the page marks them seen; [seenBefore] is the mark from before this visit, so
 * the page can still tag what is new to you now.
 */
class NotificationViewModel(
    private val commonRepository: CommonRepository,
) : BaseViewModel() {
    private val dataStoreManager: DataStoreManager by inject()

    private val _releases = MutableStateFlow<List<Release>?>(null)
    val releases: StateFlow<List<Release>?> = _releases.asStateFlow()

    private val _seenBefore = MutableStateFlow<LocalDateTime?>(null)
    val seenBefore: StateFlow<LocalDateTime?> = _seenBefore.asStateFlow()

    init {
        viewModelScope.launch {
            _seenBefore.value = dataStoreManager.releasesSeenAt()
            dataStoreManager.putString(RELEASES_SEEN_KEY, now().toString())
            _releases.value =
                commonRepository
                    .getAllNotifications()
                    .first()
                    .orEmpty()
                    .filter { it.type != NotificationEntity.TYPE_BLOG }
                    .flatMap { n -> n.album.mapNotNull { n.release(it, isAlbum = true) } + n.single.mapNotNull { n.release(it, isAlbum = false) } }
                    .sortedByDescending { it.foundAt }
                    // A release can be found twice (YouTube moving an EP between grids); it shows once.
                    .distinctBy { it.browseId }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            commonRepository.clearNotifications()
            _releases.value = emptyList()
        }
    }

    private fun NotificationEntity.release(
        item: Map<String, String>,
        isAlbum: Boolean,
    ): Release? {
        val browseId = item["browseId"]?.takeIf { it.isNotBlank() } ?: return null
        return Release(
            browseId = browseId,
            title = item["title"].orEmpty(),
            thumbnail = item["thumbnails"]?.takeIf { it.isNotBlank() },
            isAlbum = isAlbum,
            artist = name,
            channelId = channelId,
            artistThumbnail = thumbnail,
            foundAt = time,
        )
    }
}

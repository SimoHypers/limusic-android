package com.maxrave.domain.data.model.browse.playlist

import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint

data class PlaylistState(
    val id: String,
    val title: String,
    val isRadio: Boolean,
    val author: Author,
    val thumbnail: String? = null,
    val description: String? = null,
    val year: String,
    val trackCount: Int = 0,
    val radioEndpoint: YouTubeWatchEndpoint? = null,
    val shuffleEndpoint: YouTubeWatchEndpoint? = null,
    /** `PUBLIC`, `UNLISTED` or `PRIVATE`; only read for playlists you own. */
    val privacy: String? = null,
    val owned: Boolean = false,
)
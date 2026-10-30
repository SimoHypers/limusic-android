package com.simohypers.limusic.domain.data.model.browse.playlist

import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.searchResult.songs.Thumbnail
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint
import com.simohypers.limusic.domain.utils.PlaylistSortMenu

data class PlaylistBrowse(
    val author: Author,
    val description: String?,
    val duration: String,
    val durationSeconds: Int,
    val id: String,
    val privacy: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val trackCount: Int,
    val tracks: List<Track>,
    val year: String,
    val shuffleEndpoint: YouTubeWatchEndpoint? = null,
    val radioEndpoint: YouTubeWatchEndpoint? = null,
    /** Yours to edit: the page came with YouTube's edit header. */
    val owned: Boolean = false,
    /** Saved to the YouTube account's library; null when the page did not say (your own playlist). */
    val saved: Boolean? = null,
    /** The author's round picture: the header's strapline thumbnail or its facepile. */
    val authorThumbnail: String? = null,
    val sortMenu: PlaylistSortMenu? = null,
)
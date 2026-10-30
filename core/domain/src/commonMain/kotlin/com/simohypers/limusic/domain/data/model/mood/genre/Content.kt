package com.simohypers.limusic.domain.data.model.mood.genre

import com.simohypers.limusic.domain.data.model.searchResult.songs.Thumbnail
import com.simohypers.limusic.domain.data.type.HomeContentType

data class Content(
    val playlistBrowseId: String,
    val thumbnail: List<Thumbnail>?,
    val title: Title,
) : HomeContentType
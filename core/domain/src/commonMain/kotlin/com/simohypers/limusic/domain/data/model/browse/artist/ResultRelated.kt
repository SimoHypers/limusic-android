package com.simohypers.limusic.domain.data.model.browse.artist

import com.simohypers.limusic.domain.data.model.searchResult.songs.Thumbnail

data class ResultRelated(
    val browseId: String,
    val subscribers: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
)
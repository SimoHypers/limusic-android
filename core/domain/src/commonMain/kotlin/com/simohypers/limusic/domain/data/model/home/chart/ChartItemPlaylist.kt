package com.simohypers.limusic.domain.data.model.home.chart

import com.simohypers.limusic.domain.data.model.browse.artist.ResultPlaylist

data class ChartItemPlaylist(
    val title: String,
    val playlists: List<ResultPlaylist>,
)
package com.simohypers.limusic.domain.data.model.library

import com.simohypers.limusic.domain.data.model.browse.album.Track

/** One day bucket of the account's history, named as YouTube names it ("Today", "Last week"). */
data class HistoryDay(
    val title: String,
    val tracks: List<Track>,
)

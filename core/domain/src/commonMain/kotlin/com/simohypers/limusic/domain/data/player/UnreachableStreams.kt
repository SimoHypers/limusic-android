package com.simohypers.limusic.domain.data.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Videos whose last stream lookup got no answer at all: no network, or a network that died on the
 * way. The opposite of [PlaybackRefusals], where YouTube did answer, with a no. The stream resolvers
 * read it to tell the player "try again when you can" instead of "this song cannot play".
 */
object UnreachableStreams {
    private val ids = MutableStateFlow<Set<String>>(emptySet())

    fun mark(videoId: String) = ids.update { it + videoId }

    fun clear(videoId: String) = ids.update { it - videoId }

    operator fun contains(videoId: String) = videoId in ids.value
}

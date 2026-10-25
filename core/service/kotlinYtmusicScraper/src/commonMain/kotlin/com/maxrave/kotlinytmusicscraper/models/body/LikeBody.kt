package com.maxrave.kotlinytmusicscraper.models.body

import com.maxrave.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class LikeBody(
    val context: Context,
    val target: Target,
) {
    /** One of the two: a track, or a playlist (an album saves through its `OLAK5uy_` playlist). */
    @Serializable
    data class Target(
        val videoId: String? = null,
        val playlistId: String? = null,
    )
}
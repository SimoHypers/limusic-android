package com.simohypers.limusic.ui.navigation.destination.list

import kotlinx.serialization.Serializable

@Serializable
data class MoreAlbumsDestination(
    val id: String,
    val type: String = ALBUM_TYPE,
    // The artist page's name and photo, for the page's hero.
    val artist: String? = null,
    val artwork: String? = null,
) {
    companion object {
        const val ALBUM_TYPE = "album"
        const val SINGLE_TYPE = "single"
    }
}
package com.maxrave.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MusicPlaylistShelfRenderer(
    val playlistId: String?,
    val contents: List<MusicShelfRenderer.Content>? = null,
    val collapsedItemCount: Int,
    val continuations: List<Continuation>?,
    /** Holds the sort menu (parser/PlaylistSort.kt); kept as JSON and searched, like desktop does. */
    val header: JsonElement? = null,
)

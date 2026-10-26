package com.maxrave.kotlinytmusicscraper.parser

import com.maxrave.domain.utils.PlaylistSort
import com.maxrave.domain.utils.PlaylistSortMenu
import com.maxrave.kotlinytmusicscraper.models.body.EditPlaylistBody
import io.ktor.http.decodeURLQueryComponent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

// Desktop's PlaylistSort and SortMenu (crates/innertube models/browse.rs, context/08).

/**
 * The `browse` params that return a list in this order, or null for [PlaylistSort.PLAYS], which
 * YouTube knows nothing about. Every list with a sort menu honours these in both directions, and
 * asking writes nothing (Liked Music aside: YouTube remembers whatever it was last asked for).
 *
 * A protobuf literal, field 139 wrapping {1: order, 2: kind}: order 1 ascending / 2 descending,
 * kind 3 date / 4 top voted / 5 title / 6 artist / 7 album, empty for the stored order. Copied from
 * YouTube's own menu rather than encoded, because these eleven are all of them.
 */
fun PlaylistSort.params(desc: Boolean): String? =
    when (this) {
        PlaylistSort.DEFAULT -> "2ggA"
        // Newest is already a descending date sort, so reversing it is Oldest.
        PlaylistSort.NEWEST -> if (desc) "2ggECAEQAw==" else "2ggECAIQAw=="
        PlaylistSort.OLDEST -> if (desc) "2ggECAIQAw==" else "2ggECAEQAw=="
        PlaylistSort.TOP -> if (desc) "2ggECAEQBA==" else "2ggECAIQBA=="
        PlaylistSort.TITLE -> if (desc) "2ggECAIQBQ==" else "2ggECAEQBQ=="
        PlaylistSort.ARTIST -> if (desc) "2ggECAIQBg==" else "2ggECAEQBg=="
        PlaylistSort.ALBUM -> if (desc) "2ggECAIQBw==" else "2ggECAEQBw=="
        PlaylistSort.PLAYS -> null
    }

/**
 * The `browse/edit_playlist` action that stores this order on a playlist you own.
 *
 * There is deliberately no descending Title/Artist/Album: `playlistDynamicSortPreference` takes
 * 1..3 and nothing else. Anything outside answers HTTP 500 and leaves the playlist serving an
 * order its own menu then calls manual, so never widen this to express a reversed sort.
 */
fun PlaylistSort.editAction(): EditPlaylistBody.Action? =
    when (this) {
        PlaylistSort.DEFAULT -> videoOrder(0)
        PlaylistSort.NEWEST -> videoOrder(1)
        PlaylistSort.OLDEST -> videoOrder(2)
        PlaylistSort.TOP -> videoOrder(6)
        PlaylistSort.TITLE -> dynamicSort(1)
        PlaylistSort.ARTIST -> dynamicSort(2)
        PlaylistSort.ALBUM -> dynamicSort(3)
        PlaylistSort.PLAYS -> null
    }

private fun videoOrder(n: Int) = EditPlaylistBody.Action(action = "ACTION_SET_PLAYLIST_VIDEO_ORDER", playlistVideoOrder = n)

private fun dynamicSort(n: Int) =
    EditPlaylistBody.Action(action = "ACTION_SET_PLAYLIST_DYNAMIC_SORT_PREFERENCE", playlistDynamicSortPreference = n)

/**
 * The sort menu in a playlist shelf's header, or null on lists YouTube will not reorder (albums,
 * its radio mixes). Options are read by their payload, never their label, which YouTube localises.
 */
fun JsonElement.playlistSortMenu(): PlaylistSortMenu? {
    val items =
        (findAll("sortFilterSubMenuRenderer").firstOrNull() as? JsonObject)
            ?.get("subMenuItems") as? JsonArray ?: return null
    return PlaylistSortMenu(
        selected =
            items
                .firstOrNull { ((it as? JsonObject)?.get("selected") as? JsonPrimitive)?.booleanOrNull == true }
                ?.let(::sortOf),
        editable = items.any { it.endpoint()?.containsKey("playlistEditEndpoint") == true },
    )
}

private fun JsonElement.endpoint() = (this as? JsonObject)?.get("serviceEndpoint") as? JsonObject

/** Owned playlists carry edit actions; Liked Music and other people's carry browse params. */
private fun sortOf(item: JsonElement): PlaylistSort? {
    val endpoint = item.endpoint() ?: return null
    (endpoint["playlistEditEndpoint"] as? JsonObject)?.let { edit ->
        val action = (edit["actions"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
        fun int(key: String) = (action[key] as? JsonPrimitive)?.intOrNull
        val order = int("playlistVideoOrder")
        val pref = int("playlistDynamicSortPreference")
        return PlaylistSort.entries.firstOrNull { s ->
            s.editAction()?.let { it.playlistVideoOrder == order && it.playlistDynamicSortPreference == pref } == true
        }
    }
    // The browse flavour hands its params back percent-encoded ("2ggECAEQBQ%3D%3D").
    val params = (endpoint["browseEndpoint"] ?: return null).stringAt("params")?.decodeURLQueryComponent() ?: return null
    return PlaylistSort.entries.firstOrNull { it.params(false) == params || it.params(true) == params }
}

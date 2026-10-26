package com.maxrave.domain.utils

/**
 * Desktop's playlist sorts (`ui/src/lib/sort.ts`). Every one but [PLAYS] is an order YouTube can put
 * a playlist in itself; [PLAYS] ranks by this phone's own play counts.
 */
enum class PlaylistSort {
    DEFAULT,
    NEWEST,
    OLDEST,
    TITLE,
    ARTIST,
    ALBUM,

    /** YouTube's "Top voted". Not offered, but a list can already be in it, so it round-trips. */
    TOP,
    PLAYS,
}

/** The sort menu on a playlist's track shelf, when YouTube offers one. Albums and mixes have none. */
data class PlaylistSortMenu(
    /** The order the list is in right now, when it is one we have a name for. */
    val selected: PlaylistSort?,
    /**
     * The options write to the playlist, so every client on the account follows the choice. Your
     * own playlists only; elsewhere the menu only reads (Liked Music remembers the last one anyway).
     */
    val editable: Boolean,
)
